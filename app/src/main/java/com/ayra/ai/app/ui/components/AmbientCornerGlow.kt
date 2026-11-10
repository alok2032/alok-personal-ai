package com.ayra.ai.app.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

@Composable
fun AmbientCornerGlow(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CornerGlowMorph")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GlowPhase"
    )

    // Continuously shifting ambient colors
    val colorTopLeft by infiniteTransition.animateColor(
        initialValue = Color(0xFF00E5FF),
        targetValue = Color(0xFF8B5CF6),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ColorTopLeft"
    )

    val colorTopRight by infiniteTransition.animateColor(
        initialValue = Color(0xFFD946EF),
        targetValue = Color(0xFF06B6D4),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ColorTopRight"
    )

    val colorBottomLeft by infiniteTransition.animateColor(
        initialValue = Color(0xFFF43F5E),
        targetValue = Color(0xFFF59E0B),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ColorBottomLeft"
    )

    val colorBottomRight by infiniteTransition.animateColor(
        initialValue = Color(0xFF10B981),
        targetValue = Color(0xFF6366F1),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ColorBottomRight"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val pulse1 = (0.24f + 0.12f * sin(phase)).coerceIn(0.12f, 0.40f)
        val pulse2 = (0.24f + 0.12f * sin(phase + 1.57f)).coerceIn(0.12f, 0.40f)
        val pulse3 = (0.24f + 0.12f * sin(phase + 3.14f)).coerceIn(0.12f, 0.40f)
        val pulse4 = (0.24f + 0.12f * sin(phase + 4.71f)).coerceIn(0.12f, 0.40f)

        val glowRadius = w * 0.46f

        // Top-Left Glowing Ambient light
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorTopLeft.copy(alpha = pulse1), Color.Transparent),
                center = Offset(0f, 0f),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(0f, 0f)
        )

        // Top-Right Glowing Ambient light
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorTopRight.copy(alpha = pulse2), Color.Transparent),
                center = Offset(w, 0f),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(w, 0f)
        )

        // Bottom-Left Glowing Ambient light
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorBottomLeft.copy(alpha = pulse3), Color.Transparent),
                center = Offset(0f, h),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(0f, h)
        )

        // Bottom-Right Glowing Ambient light
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colorBottomRight.copy(alpha = pulse4), Color.Transparent),
                center = Offset(w, h),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(w, h)
        )
    }
}
