package com.friday.assistant.domain.orchestrator

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import com.friday.assistant.core.security.FridaySecurityGuard
import com.friday.assistant.core.security.SecurityBlocklist
import com.friday.assistant.core.state.AssistantOrbState
import com.friday.assistant.core.state.MessageSender
import com.friday.assistant.core.state.UiMessage
import com.friday.assistant.data.local.FridayDatabase
import com.friday.assistant.data.local.entity.AuditLogEntity
import com.friday.assistant.data.local.entity.AutomationEntity
import com.friday.assistant.data.local.entity.ConversationEntity
import com.friday.assistant.data.preferences.PreferencesManager
import com.friday.assistant.data.remote.models.ApiMessageItem
import com.friday.assistant.domain.ai.FridayAIModelRouter
import com.friday.assistant.domain.ai.FridayChatbotBrain
import com.friday.assistant.domain.apps.AppControlEngine
import com.friday.assistant.domain.apps.InstalledAppsManager
import com.friday.assistant.domain.browser.FridayBrowserAgent
import com.friday.assistant.domain.knowledge.FridayKnowledgeEngine
import com.friday.assistant.domain.memory.FridayMemoryManager
import com.friday.assistant.domain.protocols.StarkProtocolEngine
import com.friday.assistant.domain.telephony.TelephonyEngine
import com.friday.assistant.services.overlay.FridayOverlayService
import com.friday.assistant.services.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

data class PendingConfirmation(
    val title: String,
    val description: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

class TaskOrchestrator(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val database = FridayDatabase.getInstance(context)
    val preferencesManager = PreferencesManager(context)
    val securityGuard = FridaySecurityGuard(context, scope)
    val modelRouter = FridayAIModelRouter(context, scope)
    val memoryManager = FridayMemoryManager(context)
    val appControlEngine = AppControlEngine(context)
    val browserEngine = FridayBrowserAgent(context)
    val installedAppsManager = InstalledAppsManager(context)
    val telephonyEngine = TelephonyEngine(context)
    val knowledgeEngine = FridayKnowledgeEngine(context)
    val chatbotBrain = FridayChatbotBrain(context)
    val protocolEngine = StarkProtocolEngine(context, memoryManager, appControlEngine)
    val vaultManager = com.friday.assistant.core.security.StarkVaultManager(context)
    val calendarEngine = com.friday.assistant.domain.calendar.StarkCalendarEngine(context)
    val geofenceEngine = com.friday.assistant.domain.location.StarkGeofenceEngine(context, memoryManager)
    val iotEngine = com.friday.assistant.domain.iot.StarkIoTEngine(context)
    val integrityGuard = com.friday.assistant.core.security.StarkIntegrityGuard(context)
    private var ttsManager: TextToSpeechManager? = null

    private val _orbState = MutableStateFlow(AssistantOrbState.IDLE)
    val orbState: StateFlow<AssistantOrbState> = _orbState.asStateFlow()

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingConfirmation?> = _pendingConfirmation.asStateFlow()

    fun setTtsManager(tts: TextToSpeechManager) {
        this.ttsManager = tts
    }

    /**
     * Central Orchestration Pipeline:
     * Input -> Fail-Closed Security -> Persistent Memory -> AI Router -> Execution -> Verification -> Voice
     */
    fun processCommand(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        // 1. Record User Message
        val userMsg = UiMessage(sender = MessageSender.USER, text = trimmed)
        appendMessage(userMsg)

        scope.launch {
            // Save to Room DB
            withContext(Dispatchers.IO) {
                database.conversationDao().insertMessage(
                    ConversationEntity(sender = "USER", content = trimmed)
                )
                database.auditLogDao().insertLog(
                    AuditLogEntity(eventType = "COMMAND", details = trimmed, status = "RECEIVED")
                )
            }

            // 2. Strict Security Clearance Check (Fail-Closed)
            val clearance = securityGuard.evaluateClearance(userQuery = trimmed)
            if (!clearance.isPermitted) {
                _orbState.value = AssistantOrbState.BLOCKED
                speakAndDisplayRefusal(clearance.refusalSpeech, trimmed)
                return@launch
            }

            // 3. Floating Arc Reactor Overlay Directives
            val qLower = trimmed.lowercase()
            if (qLower.contains("enable floating") || qLower.contains("start floating orb") || qLower.contains("start overlay") || qLower.contains("floating arc reactor")) {
                FridayOverlayService.start(context)
                val reply = "Floating Arc Reactor overlay deployed, Boss. You can now access me from any screen."
                respondLocally(reply, reply)
                return@launch
            }
            if (qLower.contains("disable floating") || qLower.contains("stop floating orb") || qLower.contains("stop overlay") || qLower.contains("remove overlay")) {
                FridayOverlayService.stop(context)
                val reply = "Floating Arc Reactor overlay recalled, Boss."
                respondLocally(reply, reply)
                return@launch
            }

            // 4. Autonomous Stark Protocols (Workout, Focus, Morning Briefing, WhatsApp Dispatch)
            val protocolResult = protocolEngine.tryExecuteProtocol(trimmed)
            if (protocolResult != null) {
                _orbState.value = AssistantOrbState.SPEAKING
                val msg = UiMessage(
                    sender = MessageSender.FRIDAY,
                    text = "**${protocolResult.protocolName}**\n\n" + protocolResult.displayDetails.joinToString("\n• ", prefix = "• "),
                    intentType = "STARK_PROTOCOL"
                )
                appendMessage(msg)
                withContext(Dispatchers.IO) {
                    database.conversationDao().insertMessage(
                        ConversationEntity(sender = "FRIDAY", content = protocolResult.speechResponse, intentType = "STARK_PROTOCOL")
                    )
                }
                ttsManager?.speak(protocolResult.speechResponse) { _orbState.value = AssistantOrbState.IDLE }
                return@launch
            }

            // 5. Encrypted Stark Biometric Vault Directives
            if (qLower.startsWith("vault encrypt ") || qLower.startsWith("save secret ") || qLower.startsWith("encrypt note ")) {
                val secretText = trimmed.substringAfter(" ").substringAfter(" ").trim()
                if (secretText.isNotBlank()) {
                    val encrypted = vaultManager.encrypt(secretText)
                    memoryManager.saveNote("🔐 Encrypted Vault Item", encrypted)
                    com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
                    val reply = "Secret encrypted via AES-256-GCM and stored in the Stark Security Vault, Boss."
                    respondLocally(reply, "🔐 **Stark Vault Secured**\n\nEncrypted Payload: `$encrypted`\n\n*Stored securely with hardware Keystore protection.*")
                    return@launch
                }
            }

            // 6. Geolocation & Position Triangulation Directives
            if (qLower == "where am i" || qLower.contains("what is my location") || qLower.contains("current location")) {
                val locMsg = getLocationSummary()
                com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
                respondLocally(locMsg, "📍 $locMsg")
                return@launch
            }

            // 7. Stark Calendar & Event Scheduling
            val calResult = calendarEngine.tryScheduleEvent(trimmed)
            if (calResult != null) {
                com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
                respondLocally(calResult.message, "📅 **Google Calendar Synchronized**\n\nEvent: \"${calResult.title}\"\nTime: ${calResult.formattedTime}\n*Created successfully via Stark Protocol.*")
                return@launch
            }

            // 8. Stark Geofence Arrival / Departure Protocols
            val geoResult = geofenceEngine.handleLocationDirective(trimmed)
            if (geoResult != null) {
                com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
                val display = "**${geoResult.locationName}**\n\n" + geoResult.details.joinToString("\n• ", prefix = "• ")
                respondLocally(geoResult.speechResponse, display)
                return@launch
            }

            // 9. Smart Home & IoT Automation
            val iotResult = iotEngine.tryHandleIoTCommand(trimmed)
            if (iotResult != null) {
                com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
                respondLocally(iotResult.speechResponse, "⚡ **Smart Home Telemetry**\n\nDevice: ${iotResult.device}\nAction: ${iotResult.action}\n*Matter / Home Assistant protocol executed.*")
                return@launch
            }

            // 10. Stark Zero-Trust Security Posture Audit
            if (qLower.contains("audit security") || qLower.contains("security posture") || qLower.contains("check integrity")) {
                val report = integrityGuard.auditSecurityPosture()
                val speech = if (report.isSecure) {
                    "Defensive posture verified, Boss. Hardware storage is encrypted, zero mock spoofing detected, and perimeter shields are 100 percent nominal."
                } else {
                    "Attention Boss: Minor telemetry warnings detected: ${report.threatSummary.joinToString(", ")}."
                }
                val details = if (report.threatSummary.isEmpty()) "• Zero active perimeter threats detected\n• Hardware encryption: Active\n• Mock GPS: Clean" else report.threatSummary.joinToString("\n• ", prefix = "• ")
                respondLocally(speech, "🛡️ **Stark Defensive Audit**\n\nStatus: ${if (report.isSecure) "NOMINAL (100% SECURE)" else "WARNING"}\n\n$details")
                return@launch
            }

            // 11. Encrypted Backup Generation
            if (qLower.contains("export backup") || qLower.contains("generate backup") || qLower.contains("backup data")) {
                val encryptedBackup = memoryManager.generateEncryptedBackup(vaultManager)
                com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
                val speech = "Encrypted intelligence backup generated successfully using hardware Keystore, Boss."
                respondLocally(speech, "💾 **Encrypted Backup Vault**\n\nEncrypted Payload: `$encryptedBackup`\n\n*Securely signed with AES-256-GCM.*")
                return@launch
            }

            // 12. Offline Personal Knowledge Base RAG Search
            val ragResult = memoryManager.queryPersonalKnowledgeBase(trimmed)
            if (ragResult != null && (qLower.startsWith("what ") || qLower.startsWith("where ") || qLower.startsWith("who "))) {
                com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
                respondLocally(ragResult, ragResult)
                return@launch
            }

            // 13. Persistent Memory & Notes Check
            val memoryResponse = memoryManager.tryHandleMemoryCommand(trimmed)
            if (memoryResponse != null) {
                _orbState.value = AssistantOrbState.SPEAKING
                val msg = UiMessage(sender = MessageSender.FRIDAY, text = memoryResponse, intentType = "MEMORY")
                appendMessage(msg)
                withContext(Dispatchers.IO) {
                    database.conversationDao().insertMessage(
                        ConversationEntity(sender = "FRIDAY", content = memoryResponse, intentType = "MEMORY")
                    )
                }
                ttsManager?.speak(memoryResponse) { _orbState.value = AssistantOrbState.IDLE }
                return@launch
            }

            // 4. AI Reasoning & Router Phase (Online Cloud vs Offline On-Device)
            _orbState.value = AssistantOrbState.THINKING

            val installedPackages = withContext(Dispatchers.IO) {
                try {
                    installedAppsManager.getInstalledLaunchableApps().map { it.packageName }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val recentTurnEntities = withContext(Dispatchers.IO) {
                try {
                    database.conversationDao().getRecentMessages(6)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val historyItems = recentTurnEntities.reversed().map {
                ApiMessageItem(role = if (it.sender == "USER") "user" else "assistant", content = it.content)
            }

            // Route via FridayAIModelRouter
            val routed = modelRouter.routeRequest(trimmed, historyItems, installedPackages)
            if (routed.isCloudExecuted && routed.response != null) {
                handleAssistantResponse(routed.response, trimmed)
                return@launch
            }

            // Execute Rich Local Engine (100% offline functionality)
            handleLocalExecution(trimmed)
        }
    }

    private suspend fun handleAssistantResponse(
        resp: com.friday.assistant.data.remote.models.AssistantChatResponse,
        originalQuery: String
    ) {
        if (resp.isSecurityBlocked) {
            _orbState.value = AssistantOrbState.BLOCKED
            speakAndDisplayRefusal(resp.speechResponse, originalQuery)
            return
        }

        val action = resp.action
        if (action != null) {
            // Check for Sensitive Action requiring User Confirmation
            if (isSensitiveAction(action.type, action.actionType)) {
                _orbState.value = AssistantOrbState.THINKING
                _pendingConfirmation.value = PendingConfirmation(
                    title = "Confirm Consequential Operation",
                    description = "F.R.I.D.A.Y. is requesting permission to execute: ${resp.speechResponse}",
                    onConfirm = {
                        _pendingConfirmation.value = null
                        executeActionAndReport(action, resp)
                    },
                    onCancel = {
                        _pendingConfirmation.value = null
                        _orbState.value = AssistantOrbState.IDLE
                        val cancelMsg = UiMessage(
                            sender = MessageSender.FRIDAY,
                            text = "Operation cancelled by user."
                        )
                        appendMessage(cancelMsg)
                        ttsManager?.speak("Operation cancelled.")
                    }
                )
                return
            }

            // Normal Execution
            executeActionAndReport(action, resp)
        } else {
            // Conversational or Math Result
            _orbState.value = AssistantOrbState.SPEAKING
            val mathSteps = resp.mathDetails?.steps ?: emptyList()
            val fridayMsg = UiMessage(
                sender = MessageSender.FRIDAY,
                text = resp.displayText,
                intentType = resp.intent,
                mathSteps = mathSteps
            )
            appendMessage(fridayMsg)

            // Save conversation
            withContext(Dispatchers.IO) {
                database.conversationDao().insertMessage(
                    ConversationEntity(sender = "FRIDAY", content = resp.displayText, intentType = resp.intent)
                )
            }

            ttsManager?.speak(resp.speechResponse) {
                _orbState.value = AssistantOrbState.IDLE
            }
        }
    }

    private fun executeActionAndReport(
        action: com.friday.assistant.data.remote.models.ActionPayloadDto,
        resp: com.friday.assistant.data.remote.models.AssistantChatResponse
    ) {
        scope.launch {
            // Validate target package against SecurityGuard
            val clearance = securityGuard.evaluateClearance(
                targetPackage = action.packageName,
                targetUrl = action.url
            )
            if (!clearance.isPermitted) {
                _orbState.value = AssistantOrbState.BLOCKED
                speakAndDisplayRefusal(clearance.refusalSpeech, action.appName ?: "Unknown")
                return@launch
            }

            _orbState.value = AssistantOrbState.EXECUTING

            val execResult = when (action.type) {
                "APP_LAUNCH", "APP_DEEP_LINK" -> {
                    appControlEngine.executeAppAction(
                        packageName = action.packageName,
                        appName = action.appName,
                        actionType = action.actionType,
                        query = action.query
                    )
                }
                "CHROME_SEARCH" -> {
                    browserEngine.openSearch(action.query ?: "")
                }
                "OPEN_URL" -> {
                    browserEngine.openUrl(action.url ?: "")
                }
                else -> {
                    com.friday.assistant.domain.apps.ExecutionResult(false, "Unknown action type: ${action.type}")
                }
            }

            // Record in Automation History
            withContext(Dispatchers.IO) {
                database.automationDao().insertAutomation(
                    AutomationEntity(
                        targetApp = action.appName ?: "Browser",
                        packageName = action.packageName ?: "com.android.chrome",
                        actionType = action.type,
                        parameters = action.query ?: action.url ?: "",
                        executionResult = if (execResult.success) "VERIFIED" else "FAILED"
                    )
                )
            }

            // Announce Result
            _orbState.value = AssistantOrbState.SPEAKING
            val speechOutput = if (execResult.success) {
                resp.speechResponse
            } else {
                "Unable to complete action: ${execResult.message}"
            }

            val msg = UiMessage(
                sender = MessageSender.FRIDAY,
                text = "${resp.displayText}\n\n*${execResult.message}*",
                intentType = resp.intent,
                actionDetails = "Action: ${action.type} -> ${execResult.message}"
            )
            appendMessage(msg)

            ttsManager?.speak(speechOutput) {
                _orbState.value = AssistantOrbState.IDLE
            }
        }
    }

    /**
     * Rich Offline Intelligence Engine (100% on-device functionality):
     * Handles phone calling & speakerphone, app discovery and launching, web searches,
     * deterministic mathematics, hardware toggles, device telemetry, general Q&A, and conversational replies.
     */
    private suspend fun handleLocalExecution(query: String) {
        val qLower = query.lowercase().trim()

        // 1. Phone Calling Directives with Speakerphone Support
        val callDirective = parseCallDirective(query)
        if (callDirective != null) {
            val (target, putOnSpeaker) = callDirective
            _orbState.value = AssistantOrbState.EXECUTING

            val res = telephonyEngine.makeCall(target, putOnSpeaker)
            _orbState.value = AssistantOrbState.SPEAKING
            appendMessage(
                UiMessage(
                    sender = MessageSender.FRIDAY,
                    text = res.message,
                    actionDetails = "Action: Call -> $target${if (putOnSpeaker) " (Speaker ON)" else ""}"
                )
            )
            ttsManager?.speak(res.message) { _orbState.value = AssistantOrbState.IDLE }
            return
        }

        // 2. Device Hardware & Telemetry Controls
        // Flashlight / Torch
        if (qLower.contains("flashlight on") || qLower.contains("torch on") || qLower.contains("turn on flashlight") || qLower.contains("turn on torch")) {
            setTorch(true)
            respondLocally("Flashlight activated, Boss.", "Flashlight turned ON.")
            return
        }
        if (qLower.contains("flashlight off") || qLower.contains("torch off") || qLower.contains("turn off flashlight") || qLower.contains("turn off torch")) {
            setTorch(false)
            respondLocally("Flashlight deactivated.", "Flashlight turned OFF.")
            return
        }

        // Battery Status
        if (qLower.contains("battery") || qLower.contains("power status") || qLower.contains("battery percentage")) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val batLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val isCharging = bm?.isCharging == true
            val batMsg = if (batLevel >= 0) {
                "Battery is at $batLevel%${if (isCharging) ", charging" else ""}. Power reserves are stable, Boss."
            } else {
                "Power levels nominal. Battery telemetry is active."
            }
            respondLocally(batMsg, "⚡ $batMsg")
            return
        }

        // Time & Date
        if (qLower == "time" || qLower.contains("what is the time") || qLower.contains("what's the time") || qLower.contains("current time")) {
            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            respondLocally("The current time is $timeStr.", "Current Time: $timeStr")
            return
        }
        if (qLower == "date" || qLower.contains("what is the date") || qLower.contains("what's the date") || qLower.contains("today's date")) {
            val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
            respondLocally("Today is $dateStr.", "Date: $dateStr")
            return
        }

        // 3. Application Launch & Compound Intents
        if (qLower.startsWith("open ") || qLower.startsWith("launch ") || qLower.startsWith("start ")) {
            val rawAppTarget = qLower.removePrefix("open ").removePrefix("launch ").removePrefix("start ").trim()

            // Check for compound command: "open [app] and search for [query]"
            val (appName, appQuery) = parseCompoundAppTarget(rawAppTarget)

            // Validate against SecurityGuard
            val clearance = securityGuard.evaluateClearance(targetPackage = appName, userQuery = query)
            if (!clearance.isPermitted) {
                _orbState.value = AssistantOrbState.BLOCKED
                speakAndDisplayRefusal(clearance.refusalSpeech, appName)
                return
            }

            val result = appControlEngine.executeAppAction(
                packageName = null,
                appName = appName,
                query = appQuery ?: rawAppTarget
            )

            _orbState.value = AssistantOrbState.SPEAKING
            val speech = if (result.success) result.message else "Unable to open $appName: ${result.message}"
            appendMessage(UiMessage(sender = MessageSender.FRIDAY, text = result.message, actionDetails = "Action: Launch -> $appName"))
            ttsManager?.speak(speech) { _orbState.value = AssistantOrbState.IDLE }
            return
        }

        // 4. Web & Browser Search Intents
        if (qLower.startsWith("search ") || qLower.startsWith("google ") || qLower.startsWith("find ")) {
            val searchParam = qLower
                .removePrefix("search for ")
                .removePrefix("search google for ")
                .removePrefix("search chrome for ")
                .removePrefix("search ")
                .removePrefix("google ")
                .removePrefix("find ")
                .trim()

            if (searchParam.isNotBlank()) {
                val result = browserEngine.openSearch(searchParam)
                _orbState.value = AssistantOrbState.SPEAKING
                val speech = "Searching Google for $searchParam."
                appendMessage(UiMessage(sender = MessageSender.FRIDAY, text = "Opened search for '$searchParam' in Chrome."))
                ttsManager?.speak(speech) { _orbState.value = AssistantOrbState.IDLE }
                return
            }
        }

        // 5. Deterministic Mathematics Evaluation
        val mathResult = knowledgeEngine.evaluateMath(query)
        if (mathResult != null) {
            _orbState.value = AssistantOrbState.SPEAKING
            val speech = "The answer is ${mathResult.formattedResult}."
            appendMessage(
                UiMessage(
                    sender = MessageSender.FRIDAY,
                    text = "Calculation Result:\n\n**${mathResult.expression} = ${mathResult.formattedResult}**",
                    intentType = "MATHEMATICS",
                    mathSteps = mathResult.steps
                )
            )
            ttsManager?.speak(speech) { _orbState.value = AssistantOrbState.IDLE }
            return
        }

        // 6. Conversational AI Chatbot Brain (Like ChatGPT / Gemini)
        _orbState.value = AssistantOrbState.SPEAKING
        val apiKey = preferencesManager.geminiApiKey.first()
        val historyTurns = withContext(Dispatchers.IO) {
            try {
                database.conversationDao().getRecentMessages(6).reversed()
                    .map { Pair(it.sender.lowercase(), it.content) }
            } catch (e: Exception) {
                emptyList()
            }
        }

        val chatbotResponse = chatbotBrain.generateResponse(query, apiKey, historyTurns)

        appendMessage(
            UiMessage(
                sender = MessageSender.FRIDAY,
                text = chatbotResponse,
                intentType = "AI_CHATBOT"
            )
        )

        withContext(Dispatchers.IO) {
            try {
                database.conversationDao().insertMessage(
                    ConversationEntity(
                        sender = "FRIDAY",
                        content = chatbotResponse,
                        intentType = "AI_CHATBOT"
                    )
                )
            } catch (e: Exception) {
                // Ignore DB insertion error
            }
        }

        ttsManager?.speak(chatbotResponse) { _orbState.value = AssistantOrbState.IDLE }
    }

    private fun parseCallDirective(query: String): Pair<String, Boolean>? {
        val q = query.lowercase().trim()
        val isCall = q.startsWith("call ") || q.startsWith("dial ") || q.startsWith("make a call to ") || q.startsWith("phone ")
        if (!isCall) return null

        val putOnSpeaker = q.contains("speaker") || q.contains("speakerphone") || q.contains("on speaker")

        var target = query.trim()
        val prefixes = listOf("make a call to ", "make a call ", "call to ", "call ", "dial ", "phone ")
        for (p in prefixes) {
            if (target.startsWith(p, ignoreCase = true)) {
                target = target.substring(p.length).trim()
                break
            }
        }

        val speakerPhrases = listOf(
            "and put the phone on speaker",
            "and put phone on speaker",
            "and put on speaker",
            "and put on speakerphone",
            "and turn on speaker",
            "and put it on speaker",
            "on speakerphone",
            "on speaker",
            "speakerphone",
            "speaker"
        )
        for (phrase in speakerPhrases) {
            if (target.contains(phrase, ignoreCase = true)) {
                target = target.replace(phrase, "", ignoreCase = true).trim()
            }
        }
        target = target.removeSuffix("and").trim()

        return if (target.isNotBlank()) Pair(target, putOnSpeaker) else null
    }

    private fun parseCompoundAppTarget(raw: String): Pair<String, String?> {
        val delimiters = listOf(" and search for ", " and search ", " and play ", " and text ", " and ")
        for (delim in delimiters) {
            if (raw.contains(delim)) {
                val app = raw.substringBefore(delim).trim()
                val q = raw.substringAfter(delim).trim()
                return Pair(app, q)
            }
        }
        return Pair(raw, null)
    }

    private fun setTorch(enable: Boolean) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager?.cameraIdList?.firstOrNull()

            if (cameraId != null && cameraManager != null) {
                cameraManager.setTorchMode(cameraId, enable)
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun respondLocally(speech: String, display: String) {
        _orbState.value = AssistantOrbState.SPEAKING
        appendMessage(UiMessage(sender = MessageSender.FRIDAY, text = display))
        ttsManager?.speak(speech) { _orbState.value = AssistantOrbState.IDLE }
    }

    private fun getLocationSummary(): String {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
            val providers = lm?.getProviders(true) ?: emptyList()
            var bestLoc: android.location.Location? = null
            for (p in providers) {
                val l = try { lm?.getLastKnownLocation(p) } catch (e: SecurityException) { null }
                if (l != null && (bestLoc == null || l.accuracy < bestLoc.accuracy)) {
                    bestLoc = l
                }
            }
            if (bestLoc != null) {
                val lat = String.format(Locale.US, "%.4f", bestLoc.latitude)
                val lon = String.format(Locale.US, "%.4f", bestLoc.longitude)
                return "Current coordinates: Latitude $lat, Longitude $lon. Geolocation telemetry is verified, Boss."
            }
        } catch (e: Exception) {}
        return "Position triangulation unavailable. Please ensure location sensors are enabled, Boss."
    }

    private suspend fun speakAndDisplayRefusal(refusalMsg: String, query: String) {
        com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.SECURITY_BLOCKED)
        withContext(Dispatchers.IO) {
            database.auditLogDao().insertLog(
                AuditLogEntity(
                    eventType = "SECURITY_BLOCK",
                    details = "Blocked prohibited command: $query",
                    status = "BLOCKED"
                )
            )
        }

        appendMessage(
            UiMessage(
                sender = MessageSender.FRIDAY,
                text = refusalMsg,
                isSecurityBlocked = true
            )
        )

        ttsManager?.speak(refusalMsg) {
            _orbState.value = AssistantOrbState.IDLE
        }
    }

    private fun isSensitiveAction(type: String, actionType: String?): Boolean {
        val t = (actionType ?: type).lowercase()
        return t.contains("send") || t.contains("delete") || t.contains("post") || t.contains("submit")
    }

    private fun appendMessage(msg: UiMessage) {
        _messages.value = _messages.value + msg
    }

    fun setOrbState(state: AssistantOrbState) {
        _orbState.value = state
    }
}
