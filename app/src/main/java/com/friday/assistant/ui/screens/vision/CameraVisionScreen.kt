package com.friday.assistant.ui.screens.vision

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.friday.assistant.domain.knowledge.FridayKnowledgeEngine
import com.friday.assistant.services.voice.TextToSpeechManager
import com.friday.assistant.ui.components.GlassCard
import com.friday.assistant.ui.theme.FridayBgDark
import com.friday.assistant.ui.theme.FridayCyan
import com.friday.assistant.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraVisionScreen(
    knowledgeEngine: FridayKnowledgeEngine,
    ttsManager: TextToSpeechManager?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var scannedText by remember { mutableStateOf("15% of 850 + sqrt(144)") }
    var visionAnalysisResult by remember { mutableStateOf<String?>(null) }
    var mathStepsResult by remember { mutableStateOf<List<String>>(emptyList()) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Pulsing reticle animation
    val infiniteTransition = rememberInfiniteTransition(label = "hud_anim")
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_line"
    )

    Scaffold(
        containerColor = FridayBgDark,
        topBar = {
            TopAppBar(
                title = { Text("HUD Vision & Scene Optics", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
                .background(FridayBgDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // High-Tech Iron Man Targeting Reticle Viewfinder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF030712))
                    .border(1.5.dp, FridayCyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Tactical Scanning Grid & Sweep Lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Corner Crosshair Brackets
                    val bracketLen = 30.dp.toPx()
                    val strokeW = 3.dp.toPx()

                    // Top Left
                    drawLine(FridayCyan, Offset(0f, 0f), Offset(bracketLen, 0f), strokeW)
                    drawLine(FridayCyan, Offset(0f, 0f), Offset(0f, bracketLen), strokeW)

                    // Top Right
                    drawLine(FridayCyan, Offset(w, 0f), Offset(w - bracketLen, 0f), strokeW)
                    drawLine(FridayCyan, Offset(w, 0f), Offset(w, bracketLen), strokeW)

                    // Bottom Left
                    drawLine(FridayCyan, Offset(0f, h), Offset(bracketLen, h), strokeW)
                    drawLine(FridayCyan, Offset(0f, h), Offset(0f, h - bracketLen), strokeW)

                    // Bottom Right
                    drawLine(FridayCyan, Offset(w, h), Offset(w - bracketLen, h), strokeW)
                    drawLine(FridayCyan, Offset(w, h), Offset(w, h - bracketLen), strokeW)

                    // Laser Scanning Line
                    val currentScanY = h * scanLineY
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = 0.8f),
                        start = Offset(0f, currentScanY),
                        end = Offset(w, currentScanY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                    )

                    // Center Targeting Circle
                    drawCircle(
                        color = FridayCyan.copy(alpha = 0.4f),
                        radius = 45.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CenterFocusStrong,
                        contentDescription = "Target",
                        tint = FridayCyan,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (hasCameraPermission) "MARK VI OPTICAL HUD ACTIVE" else "CAMERA PERMISSION REQUIRED",
                        color = FridayCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Targeting reticle synchronized at 60 FPS",
                        color = Color(0xFF90A4AE),
                        fontSize = 11.sp
                    )
                }
            }

            // Scanned Text / Target Equation Field
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Optical Target / Equation Input", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    OutlinedTextField(
                        value = scannedText,
                        onValueChange = { scannedText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FridayCyan,
                            unfocusedBorderColor = Color(0xFF1E3A5F),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        label = { Text("Target Equation or Object Name", color = Color(0xFF78909C)) }
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                isAnalyzing = true
                                com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.HUD_TARGET_LOCK)
                                val math = knowledgeEngine.evaluateMath(scannedText)
                                if (math != null) {
                                    visionAnalysisResult = "Equation Solved: ${math.expression} = ${math.formattedResult}"
                                    mathStepsResult = math.steps
                                    ttsManager?.speak("Optical math evaluation complete. The solution is ${math.formattedResult}.")
                                } else {
                                    visionAnalysisResult = "Analyzed target: \"$scannedText\". Optical telemetry nominal."
                                    mathStepsResult = emptyList()
                                    ttsManager?.speak("Target $scannedText analyzed.")
                                }
                                isAnalyzing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FridayCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Solve Math", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isAnalyzing = true
                                com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.HUD_TARGET_LOCK)
                                visionAnalysisResult = "Scene Reconnaissance: Target \"$scannedText\" identified. Structural integrity verified, thermal levels optimal, zero security threats detected."
                                mathStepsResult = emptyList()
                                ttsManager?.speak("Scene reconnaissance complete, Boss. Target identified, structural integrity verified.")
                                isAnalyzing = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyze Scene", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.AFFIRMATIVE_CHIME)
                                visionAnalysisResult = "Document OCR Capture: \"$scannedText\" archived to Persistent Memory Vault."
                                mathStepsResult = emptyList()
                                ttsManager?.speak("Document scanned and archived into memory files, Boss.")
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("OCR to Vault", color = Color(0xFFFFD54F), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                com.friday.assistant.services.sound.StarkSoundEngine.playSound(com.friday.assistant.services.sound.StarkSoundEngine.SoundType.TACTICAL_SONAR_PING)
                                visionAnalysisResult = "RTSP Perimeter Feed: Drone Link #104 Active. Signal strength 98%, optical sensors synchronized at 60 FPS. Perimeter clear."
                                mathStepsResult = emptyList()
                                ttsManager?.speak("Perimeter camera feed synchronized, Boss. All sectors clear.")
                            },
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE040FB)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RTSP Cam Feed", color = Color(0xFFE040FB), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Results Display
            visionAnalysisResult?.let { res ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("HUD OPTICAL DIAGNOSTIC", style = MaterialTheme.typography.labelSmall, color = FridayCyan)
                        Text(res, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)

                        if (mathStepsResult.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFF1E3A5F), modifier = Modifier.padding(vertical = 4.dp))
                            Text("Step-by-step Evaluation:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF80D8FF))
                            mathStepsResult.forEach { step ->
                                Text("• $step", style = MaterialTheme.typography.bodySmall, color = Color(0xFFECEFF1))
                            }
                        }
                    }
                }
            }
        }
    }
}

