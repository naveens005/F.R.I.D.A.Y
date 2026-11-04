package com.friday.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.friday.assistant.core.state.AssistantOrbState
import com.friday.assistant.ui.theme.*
import kotlin.math.*

/**
 * Authentic Iron Man Mark VI Arc Reactor Canvas Component
 * Faithfully matches Tony Stark's iconic triangular chest piece:
 * - 10 Radial Transformer Coils with glowing cyan radiation channels
 * - Concentric Inverted Equilateral Triangle Core (▼) with blinding white-hot fusion center
 * - Dark titanium support brackets and radial HUD calibration ticks
 * - Organic energy pulsation, mechanical rotation, and acoustic soundwave reactivity
 */
@Composable
fun FridayOrb(
    state: AssistantOrbState,
    audioLevel: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorTransition")

    // 1. Organic Core Breathing & Fusion Pulse
    val coreBreathing by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantOrbState.THINKING -> 800
                    AssistantOrbState.LISTENING -> 1200
                    else -> 2400
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreBreathing"
    )

    // 2. Coil Segment Ring Rotation (Clockwise)
    val coilRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantOrbState.THINKING -> 4000
                    AssistantOrbState.EXECUTING -> 6000
                    else -> 24000
                },
                easing = LinearEasing
            )
        ),
        label = "coilRotation"
    )

    // 3. Inner HUD Calibration Track Counter-Rotation
    val hudCounterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantOrbState.THINKING -> 3000
                    else -> 16000
                },
                easing = LinearEasing
            )
        ),
        label = "hudCounterRotation"
    )

    // 4. Acoustic Wave Shockwave
    val shockwaveProgress by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shockwaveProgress"
    )

    // State Color Grading
    val (coreGlowColor, plasmaAccentColor, coreWhite) = when (state) {
        AssistantOrbState.IDLE -> Triple(Color(0xFF00E5FF), Color(0xFF00B4D8), Color(0xFFFFFFFF))
        AssistantOrbState.LISTENING -> Triple(Color(0xFF00FFFF), Color(0xFF38EF7D), Color(0xFFFFFFFF))
        AssistantOrbState.THINKING -> Triple(Color(0xFFFFD700), Color(0xFF00E5FF), Color(0xFFFFFFFF))
        AssistantOrbState.SPEAKING -> Triple(Color(0xFF00E5FF), Color(0xFF0077B6), Color(0xFFFFFFFF))
        AssistantOrbState.EXECUTING -> Triple(Color(0xFF00F0FF), Color(0xFF7209B7), Color(0xFFFFFFFF))
        AssistantOrbState.BLOCKED -> Triple(Color(0xFFFF2A2A), Color(0xFF8B0000), Color(0xFFFFD2D2))
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val outerRadius = (size.toPx() / 2f) * 0.94f

            // Dynamic scale influenced by audio energy
            val audioBoost = (audioLevel * 0.035f).coerceIn(0f, 0.25f)
            val effectiveScale = coreBreathing + audioBoost

            // ==========================================
            // LAYER 1: Deep Atmospheric Cyan Lens Flare
            // ==========================================
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreGlowColor.copy(alpha = 0.55f * effectiveScale),
                        plasmaAccentColor.copy(alpha = 0.22f),
                        Color(0xFF001F3F).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerRadius * 1.15f
                ),
                radius = outerRadius * 1.15f,
                center = center
            )

            // ==========================================
            // LAYER 2: Acoustic Shockwave Rings
            // ==========================================
            if (state == AssistantOrbState.LISTENING || state == AssistantOrbState.SPEAKING) {
                val waveAlpha = (1.45f - shockwaveProgress).coerceIn(0f, 0.5f)
                drawCircle(
                    color = coreGlowColor.copy(alpha = waveAlpha),
                    radius = outerRadius * 0.72f * shockwaveProgress,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // ==========================================
            // LAYER 3: Outer Bezel & Calibration Ticks
            // ==========================================
            // Outer Titanium Rim
            drawCircle(
                color = Color(0xFF141F2B),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )
            // Cyan Hairline Ring
            drawCircle(
                color = coreGlowColor.copy(alpha = 0.65f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 40 Precision Radial Calibrations
            val tickCount = 40
            for (i in 0 until tickCount) {
                val angleDeg = i * (360f / tickCount)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajor = i % 4 == 0
                val tickLength = if (isMajor) 8.dp.toPx() else 4.dp.toPx()

                val innerP = Offset(
                    (center.x + (outerRadius - tickLength) * cos(angleRad)).toFloat(),
                    (center.y + (outerRadius - tickLength) * sin(angleRad)).toFloat()
                )
                val outerP = Offset(
                    (center.x + outerRadius * cos(angleRad)).toFloat(),
                    (center.y + outerRadius * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = if (isMajor) coreGlowColor else coreGlowColor.copy(alpha = 0.35f),
                    start = innerP,
                    end = outerP,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // ==========================================
            // LAYER 4: 10 Radial Transformer Coils (Mark VI)
            // ==========================================
            val rCoilOuter = outerRadius * 0.90f
            val rCoilInner = outerRadius * 0.58f

            // Background luminous glow behind coils
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreGlowColor.copy(alpha = 0.85f),
                        plasmaAccentColor.copy(alpha = 0.40f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = rCoilOuter
                ),
                radius = rCoilOuter,
                center = center
            )

            // Draw the 10 rotating metallic coil blocks
            rotate(coilRotation, center) {
                val numCoils = 10
                val stepDeg = 360f / numCoils // 36 degrees per coil sector
                val coilWidthDeg = 24f        // 24 deg solid copper block, 12 deg cyan glow slit

                for (c in 0 until numCoils) {
                    val startAngle = (c * stepDeg) - (coilWidthDeg / 2f)

                    // Draw the dark metallic trapezoidal coil block
                    val coilPath = Path().apply {
                        val startRad = Math.toRadians(startAngle.toDouble())
                        val endRad = Math.toRadians((startAngle + coilWidthDeg).toDouble())

                        val p1 = Offset(
                            (center.x + rCoilInner * cos(startRad)).toFloat(),
                            (center.y + rCoilInner * sin(startRad)).toFloat()
                        )
                        val p2 = Offset(
                            (center.x + rCoilOuter * cos(startRad)).toFloat(),
                            (center.y + rCoilOuter * sin(startRad)).toFloat()
                        )
                        val p3 = Offset(
                            (center.x + rCoilOuter * cos(endRad)).toFloat(),
                            (center.y + rCoilOuter * sin(endRad)).toFloat()
                        )
                        val p4 = Offset(
                            (center.x + rCoilInner * cos(endRad)).toFloat(),
                            (center.y + rCoilInner * sin(endRad)).toFloat()
                        )

                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        lineTo(p4.x, p4.y)
                        close()
                    }

                    // Metallic Coil Face (Copper/Titanium Dark Gradient)
                    drawPath(
                        path = coilPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0F1A24),
                                Color(0xFF1B2E3D),
                                Color(0xFF0F1A24)
                            )
                        )
                    )

                    // Coil metallic border with subtle copper highlight
                    drawPath(
                        path = coilPath,
                        color = Color(0xFF28485E),
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // Central coil division line
                    val midRad = Math.toRadians((startAngle + (coilWidthDeg / 2f)).toDouble())
                    val m1 = Offset(
                        (center.x + (rCoilInner + 2.dp.toPx()) * cos(midRad)).toFloat(),
                        (center.y + (rCoilInner + 2.dp.toPx()) * sin(midRad)).toFloat()
                    )
                    val m2 = Offset(
                        (center.x + (rCoilOuter - 2.dp.toPx()) * cos(midRad)).toFloat(),
                        (center.y + (rCoilOuter - 2.dp.toPx()) * sin(midRad)).toFloat()
                    )
                    drawLine(
                        color = coreGlowColor.copy(alpha = 0.5f),
                        start = m1,
                        end = m2,
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            // ==========================================
            // LAYER 5: Inner Retaining Ring & HUD Dashes
            // ==========================================
            drawCircle(
                color = Color(0xFF09141F),
                radius = rCoilInner,
                center = center,
                style = Fill
            )
            drawCircle(
                color = coreGlowColor,
                radius = rCoilInner,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Rotating HUD calibration dashed arc
            rotate(hudCounterRotation, center) {
                drawCircle(
                    color = coreGlowColor.copy(alpha = 0.5f),
                    radius = rCoilInner * 0.90f,
                    center = center,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                )
            }

            // ==========================================
            // LAYER 6: 3 Corner Structural Brackets
            // ==========================================
            // Locking the inverted triangle (vertices at 90°, 210°, 330°) to the retaining ring
            val bracketAngles = listOf(90.0, 210.0, 330.0)
            for (bAngle in bracketAngles) {
                val bRad = Math.toRadians(bAngle)
                val pOuter = Offset(
                    (center.x + rCoilInner * cos(bRad)).toFloat(),
                    (center.y + rCoilInner * sin(bRad)).toFloat()
                )
                val pInner = Offset(
                    (center.x + (rCoilInner * 0.55f) * cos(bRad)).toFloat(),
                    (center.y + (rCoilInner * 0.55f) * sin(bRad)).toFloat()
                )
                drawLine(
                    color = Color(0xFF0A1926),
                    start = pOuter,
                    end = pInner,
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = coreGlowColor.copy(alpha = 0.75f),
                    start = pOuter,
                    end = pInner,
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // ==========================================
            // LAYER 7: Inverted Equilateral Triangle Core (▼)
            // ==========================================
            // Mark VI Core Geometry: Inverted triangle pointing straight down (vertex 1 at 90°)
            val triBaseRadius = rCoilInner * 0.88f * effectiveScale

            // 7A: Outer Inverted Triangle (Backlit Chamber)
            val outerTriPath = createInvertedTrianglePath(center, triBaseRadius)
            drawPath(
                path = outerTriPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreGlowColor.copy(alpha = 0.95f),
                        plasmaAccentColor.copy(alpha = 0.65f),
                        Color(0xFF002A4A)
                    ),
                    center = center,
                    radius = triBaseRadius
                )
            )
            drawPath(
                path = outerTriPath,
                color = coreGlowColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 7B: Intermediate Structural Inverted Triangle
            val midTriRadius = triBaseRadius * 0.68f
            val midTriPath = createInvertedTrianglePath(center, midTriRadius)
            drawPath(
                path = midTriPath,
                color = Color(0xFF061420),
                style = Fill
            )
            drawPath(
                path = midTriPath,
                color = coreGlowColor.copy(alpha = 0.90f),
                style = Stroke(width = 2.dp.toPx())
            )

            // 7C: Innermost White-Hot Fusion Core (▼)
            val coreTriRadius = triBaseRadius * 0.46f
            val innerTriPath = createInvertedTrianglePath(center, coreTriRadius)

            // High-intensity white-cyan plasma gradient
            drawPath(
                path = innerTriPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreWhite,
                        coreGlowColor,
                        plasmaAccentColor
                    ),
                    center = center,
                    radius = coreTriRadius * 1.1f
                )
            )
            drawPath(
                path = innerTriPath,
                color = Color.White,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // White Star Flare in the exact core center
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        coreGlowColor.copy(alpha = 0.8f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreTriRadius * 0.6f
                ),
                radius = coreTriRadius * 0.6f,
                center = center
            )
        }
    }
}

/**
 * Generates an Inverted Equilateral Triangle (▼) Path centered at (cx, cy).
 * Vertices:
 * - Top-Left at 210°
 * - Top-Right at 330°
 * - Bottom Tip at 90° (pointing straight downwards)
 */
private fun createInvertedTrianglePath(center: Offset, radius: Float): Path {
    val angles = listOf(210.0, 330.0, 90.0)
    val path = Path()

    angles.forEachIndexed { index, angleDeg ->
        val rad = Math.toRadians(angleDeg)
        val x = (center.x + radius * cos(rad)).toFloat()
        val y = (center.y + radius * sin(rad)).toFloat()

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}
