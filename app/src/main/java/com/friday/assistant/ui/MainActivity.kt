package com.friday.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.friday.assistant.domain.sensors.StarkMotionManager
import com.friday.assistant.services.foreground.FridayVoiceService
import com.friday.assistant.ui.navigation.FridayNavHost
import com.friday.assistant.ui.screens.main.DashboardViewModel
import com.friday.assistant.ui.theme.FridayTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private var motionManager: StarkMotionManager? = null
    private var shouldAutoListenOnStart = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force stop and dismiss any lingering background foreground services from previous versions
        FridayVoiceService.stop(this)

        initMotionManager()
        handleIncomingIntent(intent)
        checkAndRequestPermissions()

        setContent {
            FridayTheme {
                FridayNavHost(
                    shouldAutoListen = shouldAutoListenOnStart,
                    viewModel = viewModel
                )
            }
        }
    }

    private fun initMotionManager() {
        motionManager = StarkMotionManager(
            context = this,
            onShakeTriggered = {
                runOnUiThread {
                    viewModel.startVoiceListening()
                }
            },
            onFaceDownDetected = {
                runOnUiThread {
                    viewModel.stopSpeaking()
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        motionManager?.startListening()
    }

    override fun onPause() {
        super.onPause()
        motionManager?.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action == Intent.ACTION_ASSIST ||
            intent.action == Intent.ACTION_VOICE_COMMAND ||
            intent.getBooleanExtra("AUTO_LISTEN", false)
        ) {
            shouldAutoListenOnStart = true
            viewModel.startVoiceListening()
        }

        // Handle shared screen text or articles (Screen Summarization Directive)
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val query = "Summarize this: $sharedText"
                viewModel.submitTextCommand(query)
            }
        }

        // Handle remote protocol execution
        val protocol = intent.getStringExtra("EXECUTE_PROTOCOL")
        if (!protocol.isNullOrBlank()) {
            viewModel.submitTextCommand(protocol)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        motionManager?.stopListening()
        FridayVoiceService.stop(this)
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )

        val missingPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
}
