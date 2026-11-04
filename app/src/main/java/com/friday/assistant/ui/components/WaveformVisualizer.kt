package com.friday.assistant.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.friday.assistant.ui.theme.FridayCyan
import com.friday.assistant.ui.theme.FridayElectricBlue
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    isListening: Boolean,
    audioRmsDb: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveformAnim")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val barCount = 28
        val spacing = size.width / barCount
        val barWidth = spacing * 0.55f
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val normalizedX = (i.toFloat() / barCount)
            val wave = sin((normalizedX * 10f) + phase).toFloat()

            val baseHeight = if (isListening) {
                val amplitude = (audioRmsDb * 3.5f).coerceIn(4f, 22f)
                8.dp.toPx() + (amplitude * (0.6f + 0.4f * wave))
            } else {
                3.dp.toPx() + (2.dp.toPx() * wave)
            }

            val top = centerY - (baseHeight / 2f)
            val left = i * spacing + (spacing - barWidth) / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        FridayCyan,
                        FridayElectricBlue
                    ),
                    startY = top,
                    endY = top + baseHeight
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, baseHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

