package com.ayra.ai.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayra.ai.app.ui.theme.GlassBorder
import com.ayra.ai.app.ui.theme.GlassSurfaceDark
import com.ayra.ai.app.ui.theme.NeonCyan
import com.ayra.ai.app.ui.theme.NeonPurple
import com.ayra.ai.app.ui.theme.NeonRose
import com.ayra.ai.app.ui.theme.TextPrimary
import com.ayra.ai.app.ui.theme.TextSecondary
import org.json.JSONObject
import kotlin.math.sin

@Composable
fun SongPlayerCard(
    jsonPayload: String,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var expandedLyrics by remember { mutableStateOf(false) }

    val json = remember(jsonPayload) {
        try {
            JSONObject(jsonPayload)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    val title = json.optString("title", "Cosmic Composition")
    val genre = json.optString("genre", "Electronic Synth")
    val tempo = json.optString("tempo", "120 BPM")
    val chords = json.optString("chords", "Am - F - C - G")
    val lyrics = json.optString("lyrics", "")

    val infiniteTransition = rememberInfiniteTransition(label = "WaveformPulse")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassSurfaceDark)
            .border(
                1.2.dp,
                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.7f), NeonPurple.copy(alpha = 0.5f), NeonRose.copy(alpha = 0.6f))),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(NeonCyan, NeonPurple))
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = {
                    if (isPlaying) {
                        com.ayra.ai.app.audio.AyraAudioSynthesizer.stop()
                        isPlaying = false
                    } else {
                        isPlaying = true
                        com.ayra.ai.app.audio.AyraAudioSynthesizer.playSynthSong { active ->
                            isPlaying = active
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "$genre • $tempo",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Visual Audio Waveform
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF090E1B))
                .padding(horizontal = 6.dp)
        ) {
            val barCount = 32
            val spacing = size.width / barCount
            for (i in 0 until barCount) {
                val amplitude = if (isPlaying) {
                    (0.3f + 0.65f * kotlin.math.abs(sin(wavePhase + i * 0.35f))).toFloat()
                } else {
                    0.25f
                }
                val barHeight = size.height * amplitude
                val x = i * spacing + spacing / 2
                val top = (size.height - barHeight) / 2
                drawLine(
                    brush = Brush.verticalGradient(listOf(NeonCyan, NeonPurple)),
                    start = Offset(x, top),
                    end = Offset(x, top + barHeight),
                    strokeWidth = spacing * 0.6f
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chords Guide
        if (chords.isNotBlank()) {
            Text(
                text = "Chord Progression: $chords",
                style = MaterialTheme.typography.labelSmall,
                color = NeonCyan,
                fontSize = 11.sp
            )
        }

        if (lyrics.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (expandedLyrics) "Hide Lyrics ▲" else "View Full Lyrics ▼",
                color = NeonRose,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { expandedLyrics = !expandedLyrics }
            )
            if (expandedLyrics) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lyrics,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
