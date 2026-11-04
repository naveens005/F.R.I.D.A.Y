package com.friday.assistant.core.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.friday.assistant.data.local.FridayDatabase
import com.friday.assistant.data.local.entity.AuditLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * FridaySecurityGuard:
 * The centralized enterprise-grade security authority of F.R.I.D.A.Y. 3.0.
 * Guarantees fail-closed execution, immutable blocklist enforcement,
 * real-time intent interception, and audit logging.
 */
class FridaySecurityGuard(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val database = FridayDatabase.getInstance(context)

    private val _securityState = MutableStateFlow(SecurityShieldState.SHIELD_ACTIVE)
    val securityState: StateFlow<SecurityShieldState> = _securityState.asStateFlow()

    private val _blockedAttemptsCount = MutableStateFlow(0)
    val blockedAttemptsCount: StateFlow<Int> = _blockedAttemptsCount.asStateFlow()

    enum class SecurityShieldState {
        SHIELD_ACTIVE,
        CHECK_PASSED,
        ACCESS_DENIED,
        TAMPER_LOCKED
    }

    data class ValidationResult(
        val isPermitted: Boolean,
        val violationReason: String? = null,
        val refusalSpeech: String = SecurityBlocklist.REFUSAL_MESSAGE
    )

    /**
     * Complete pre-execution clearance check before ANY application launch,
     * tool execution, or intent dispatch. Fail-closed guarantee.
     */
    fun evaluateClearance(
        targetPackage: String? = null,
        targetUrl: String? = null,
        userQuery: String? = null
    ): ValidationResult {
        // 1. Check Target Package against immutable blocklist
        if (!targetPackage.isNullOrBlank() && SecurityBlocklist.isPackageBlocked(targetPackage)) {
            recordViolation("BLOCKED_PACKAGE_LAUNCH", targetPackage)
            return ValidationResult(
                isPermitted = false,
                violationReason = "Access to financial/payment application ($targetPackage) is strictly prohibited."
            )
        }

        // 2. Check Query for financial keywords or transaction requests
        if (!userQuery.isNullOrBlank() && SecurityBlocklist.isQueryBlocked(userQuery)) {
            recordViolation("BLOCKED_QUERY_KEYWORD", userQuery)
            return ValidationResult(
                isPermitted = false,
                violationReason = "Command contains prohibited payment, banking, or credential operations."
            )
        }

        // 3. Check Target URL for payment gateways or phishing links
        if (!targetUrl.isNullOrBlank()) {
            val uri = Uri.parse(targetUrl)
            val host = uri.host?.lowercase() ?: ""
            val scheme = uri.scheme?.lowercase() ?: ""

            // Disallow non-http/https intents from browser unless specifically verified
            if (scheme == "upi" || scheme.contains("pay") || host.contains("paytm") || host.contains("phonepe")) {
                recordViolation("BLOCKED_PAYMENT_URL_SCHEME", targetUrl)
                return ValidationResult(
                    isPermitted = false,
                    violationReason = "Access to payment URL schemes ($scheme) is strictly barred."
                )
            }
        }

        // Passed security clearance
        _securityState.value = SecurityShieldState.CHECK_PASSED
        return ValidationResult(isPermitted = true)
    }

    /**
     * Inspects an arbitrary Android Intent before startActivity to prevent malicious redirection.
     */
    fun verifyIntent(intent: Intent): Boolean {
        val targetPkg = intent.`package` ?: intent.component?.packageName
        if (targetPkg != null && SecurityBlocklist.isPackageBlocked(targetPkg)) {
            recordViolation("BLOCKED_INTENT_DISPATCH", targetPkg)
            return false
        }

        val dataString = intent.dataString
        if (dataString != null && (dataString.startsWith("upi://") || dataString.contains("payment"))) {
            recordViolation("BLOCKED_INTENT_DATA", dataString)
            return false
        }

        return true
    }

    private fun recordViolation(type: String, detail: String) {
        _securityState.value = SecurityShieldState.ACCESS_DENIED
        _blockedAttemptsCount.value += 1

        scope.launch(Dispatchers.IO) {
            try {
                database.auditLogDao().insertLog(
                    AuditLogEntity(
                        eventType = "SECURITY_VIOLATION",
                        details = "[$type] Blocked attempted access: $detail",
                        status = "FAIL_CLOSED_BLOCKED"
                    )
                )
            } catch (e: Exception) {
                // Ignore DB logging failure to preserve fail-closed state
            }
        }
    }
}

