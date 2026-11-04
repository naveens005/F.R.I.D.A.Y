package com.friday.assistant.ui.screens.voice

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.friday.assistant.data.preferences.PreferencesManager
import com.friday.assistant.services.voice.TextToSpeechManager
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.FridayBgDark
import com.friday.assistant.ui.theme.FridayCyan
import com.friday.assistant.ui.theme.FridayGlassBorder
import com.friday.assistant.ui.theme.FridaySurface
import com.friday.assistant.ui.theme.FridayTypography
import com.friday.assistant.ui.theme.TextPrimary
import com.friday.assistant.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val tts = remember { TextToSpeechManager(context) }

    val currentPitch by prefs.voicePitch.collectAsState(initial = 1.0f)
    val currentRate by prefs.voiceRate.collectAsState(initial = 1.0f)
    val currentLang by prefs.voiceLanguage.collectAsState(initial = "en-US")
    val continuousListening by prefs.continuousListening.collectAsState(initial = false)
    val wakeWordEnabled by prefs.wakeWordEnabled.collectAsState(initial = true)

    var pitchSlider by remember { mutableStateOf(currentPitch) }
    var rateSlider by remember { mutableStateOf(currentRate) }

    LaunchedEffect(currentPitch, currentRate) {
        pitchSlider = currentPitch
        rateSlider = currentRate
    }

    Scaffold(
        containerColor = FridayBgDark,
        topBar = {
            TopAppBar(
                title = { Text("Voice & Speech Engine", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = FridayCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FridayBgDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Voice Persona Information
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("ASSISTANT PERSONA", style = FridayTypography.labelSmall, color = FridayCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("F.R.I.D.A.Y. Female Voice Profile", style = FridayTypography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Composed, elegant, and intelligent female neural voice inspired by Marvel's Iron Man operating assistant.",
                        style = FridayTypography.bodyMedium,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            tts.setLanguage(currentLang)
                            tts.setPitch(pitchSlider)
                            tts.setSpeechRate(rateSlider)
                            val sample = if (currentLang.startsWith("ta")) {
                                "வணக்கம் பாஸ். அனைத்து அமைப்புகளும் தயார் நிலையில் உள்ளன."
                            } else {
                                "Good day, Boss. All propulsion, navigation, and security protocols are active and nominal."
                            }
                            tts.speak(sample)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FridayCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Sample", tint = FridayBgDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Voice Synthesis", color = FridayBgDark, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Language Profile Selection (English / Tamil)
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("LANGUAGE PROFILE & DIALECT", style = FridayTypography.labelSmall, color = FridayCyan)

                    val languages = listOf(
                        Triple("en-US", "Siri / Studio Neural (US English)", "Ultra-smooth, natural, breathy Siri-style female voice"),
                        Triple("en-GB", "F.R.I.D.A.Y. Authentic (UK/Irish)", "Kerry Condon inspired Marvel Stark AI female voice"),
                        Triple("en-IN", "English (India)", "Indian English natural female voice"),
                        Triple("ta-IN", "Tamil (தமிழ்)", "Tamil natural female speech synthesis")
                    )

                    languages.forEach { (code, title, desc) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (currentLang == code) FridayCyan.copy(alpha = 0.15f) else FridaySurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (currentLang == code) FridayCyan else FridayGlassBorder.copy(alpha = 0.3f)
                            ),
                            onClick = {
                                scope.launch {
                                    prefs.setVoiceLanguage(code)
                                    tts.setLanguage(code)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = currentLang == code,
                                    onClick = {
                                        scope.launch {
                                            prefs.setVoiceLanguage(code)
                                            tts.setLanguage(code)
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = FridayCyan)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(title, style = FridayTypography.titleSmall, color = TextPrimary)
                                    Text(desc, style = FridayTypography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Pitch & Rate Controls
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("SYNTHESIS PARAMETERS", style = FridayTypography.labelSmall, color = FridayCyan)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Voice Pitch", color = TextPrimary, fontSize = 13.sp)
                        Text(String.format("%.2fx", pitchSlider), color = FridayCyan, fontSize = 13.sp)
                    }

                    Slider(
                        value = pitchSlider,
                        onValueChange = {
                            pitchSlider = it
                            tts.setPitch(it)
                            scope.launch { prefs.setVoicePitch(it) }
                        },
                        valueRange = 0.7f..1.5f,
                        colors = SliderDefaults.colors(thumbColor = FridayCyan, activeTrackColor = FridayCyan)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Speech Rate", color = TextPrimary, fontSize = 13.sp)
                        Text(String.format("%.2fx", rateSlider), color = FridayCyan, fontSize = 13.sp)
                    }

                    Slider(
                        value = rateSlider,
                        onValueChange = {
                            rateSlider = it
                            tts.setSpeechRate(it)
                            scope.launch { prefs.setVoiceRate(it) }
                        },
                        valueRange = 0.7f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = FridayCyan, activeTrackColor = FridayCyan)
                    )
                }
            }

            // Background Interaction Settings
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("VOICE INTERACTION MODES", style = FridayTypography.labelSmall, color = FridayCyan)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Conversation", color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("Automatically listens for follow-up prompts after answering.", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = continuousListening,
                            onCheckedChange = { scope.launch { prefs.setContinuousListening(it) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = FridayCyan)
                        )
                    }

                    HorizontalDivider(color = FridayGlassBorder.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Wake Word Detection", color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text("Responds when user says 'Friday' or 'Hey Friday'.", color = TextSecondary, fontSize = 11.sp)
                        }
                        Switch(
                            checked = wakeWordEnabled,
                            onCheckedChange = { scope.launch { prefs.setWakeWordEnabled(it) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = FridayCyan)
                        )
                    }
                }
            }
        }
    }
}
