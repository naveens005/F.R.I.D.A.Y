package com.friday.assistant.domain.telephony

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.friday.assistant.domain.apps.ExecutionResult
import kotlinx.coroutines.*

class TelephonyEngine(private val context: Context) {

    data class ContactMatch(val displayName: String, val phoneNumber: String)

    /**
     * Resolves contact name or phone number, initiates the call, and optionally toggles speakerphone.
     */
    fun makeCall(target: String, putOnSpeaker: Boolean): ExecutionResult {
        val cleanTarget = target.trim()
        if (cleanTarget.isBlank()) {
            return ExecutionResult(false, "No contact name or phone number provided.")
        }

        // 1. Check if target is a raw phone number (contains digits and optional +, -, spaces)
        val digitsOnly = cleanTarget.filter { it.isDigit() }
        val isRawNumber = digitsOnly.length >= 3 && cleanTarget.all { it.isDigit() || it in "+- ()" }

        val resolvedContact = if (isRawNumber) {
            ContactMatch(displayName = cleanTarget, phoneNumber = cleanTarget.replace(" ", ""))
        } else {
            findContactByName(cleanTarget)
        }

        if (resolvedContact == null) {
            // If contact not found, still allow opening dialer with the query
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanTarget")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            return ExecutionResult(
                success = true,
                message = "Contact '$cleanTarget' not found in address book. Opened dialer."
            )
        }

        val phoneNumber = resolvedContact.phoneNumber
        val displayName = resolvedContact.displayName

        // 2. Execute Call Intent
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        try {
            context.startActivity(intent)

            // 3. Manage Speakerphone if requested
            if (putOnSpeaker) {
                activateSpeakerphoneWithRetries()
            }

            val speakerNotice = if (putOnSpeaker) " on speakerphone" else ""
            return ExecutionResult(
                success = true,
                message = "Calling $displayName$speakerNotice."
            )
        } catch (e: Exception) {
            return ExecutionResult(false, "Failed to initiate call: ${e.localizedMessage}")
        }
    }

    /**
     * Searches device contacts for matching name.
     */
    private fun findContactByName(query: String): ContactMatch? {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasReadPermission) return null

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val lowerQuery = query.lowercase().trim()

        // Query all phone numbers
        var bestMatch: ContactMatch? = null
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex) ?: continue
                    val number = cursor.getString(numberIndex) ?: continue
                    val lowerName = name.lowercase()

                    // Exact match takes priority
                    if (lowerName == lowerQuery) {
                        return ContactMatch(name, number)
                    }

                    // Partial match
                    if (bestMatch == null && (lowerName.contains(lowerQuery) || lowerQuery.contains(lowerName))) {
                        bestMatch = ContactMatch(name, number)
                    }
                }
            }
        } catch (e: Exception) {
            // Contact read handled safely
        }

        return bestMatch
    }

    /**
     * Engages audio speaker mode and reaffirms after delays to ensure hardware stays routed to speaker.
     */
    private fun activateSpeakerphoneWithRetries() {
        CoroutineScope(Dispatchers.IO).launch {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return@launch
            val delays = listOf(500L, 1500L, 3000L, 4500L)
            for (d in delays) {
                delay(d)
                try {
                    audioManager.mode = AudioManager.MODE_IN_CALL
                    audioManager.isSpeakerphoneOn = true
                } catch (e: Exception) {
                    // Audio route handled gracefully
                }
            }
        }
    }
}

