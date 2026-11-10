package com.ayra.ai.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Real-time software audio synthesizer for generating harmonious melodic music,
 * chords, and electronic synth tracks through the device speaker.
 */
object AyraAudioSynthesizer {

    private var audioTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    // Musical note frequencies (Hz) for chord progressions (C major / A minor synthwave)
    private val melodyNotes = doubleArrayOf(
        261.63, // C4
        329.63, // E4
        392.00, // G4
        523.25, // C5
        440.00, // A4
        349.23, // F4
        392.00, // G4
        293.66, // D4
        329.63, // E4
        392.00, // G4
        493.88, // B4
        587.33  // D5
    )

    fun isPlaying(): Boolean = audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

    fun playSynthSong(onPlaybackStateChange: ((Boolean) -> Unit)? = null) {
        stop()

        synthJob = scope.launch {
            val sampleRate = 44100
            val minBufSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufSize.coerceAtLeast(8192))
                .build()

            audioTrack = track
            track.play()
            onPlaybackStateChange?.invoke(true)

            try {
                // Play 16 bars of arpeggiated harmonic music (~15 seconds)
                val noteDurationSamples = (sampleRate * 0.32).toInt() // ~320ms per note
                val buffer = ShortArray(noteDurationSamples)

                for (bar in 0 until 48) {
                    if (!isActive) break

                    val baseFreq = melodyNotes[bar % melodyNotes.size]
                    val bassFreq = baseFreq / 2.0 // Warm sub-bass octave

                    for (i in 0 until noteDurationSamples) {
                        val t = i.toDouble() / sampleRate
                        // Envelope ADSR: smooth attack and decay
                        val progress = i.toDouble() / noteDurationSamples
                        val envelope = if (progress < 0.1) progress / 0.1 else (1.0 - progress) * 0.9 + 0.1

                        // Dual oscillator: Warm sine + soft harmonic overtone + sub-bass
                        val sampleValue = (
                            0.55 * sin(2.0 * PI * baseFreq * t) +
                            0.25 * sin(2.0 * PI * (baseFreq * 2.0) * t) +
                            0.20 * sin(2.0 * PI * bassFreq * t)
                        ) * envelope

                        buffer[i] = (sampleValue * Short.MAX_VALUE * 0.65).toInt().toShort()
                    }

                    track.write(buffer, 0, buffer.size)
                }
            } catch (_: Exception) {
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
                audioTrack = null
                onPlaybackStateChange?.invoke(false)
            }
        }
    }

    fun stop() {
        synthJob?.cancel()
        synthJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
