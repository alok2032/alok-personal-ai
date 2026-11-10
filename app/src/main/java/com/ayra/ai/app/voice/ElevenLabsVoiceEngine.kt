package com.ayra.ai.app.voice

import android.content.Context
import com.ayra.ai.app.data.AyraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Production ElevenLabs Neural Voice Engine.
 * Allows ultra-realistic AI voice synthesis with low latency.
 * Stores key securely in Android Keystore AES-256.
 */
class ElevenLabsVoiceEngine(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Default Rachel voice ID
    companion object {
        const val DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM"
    }

    /**
     * Synthesizes text to speech audio bytes (MP3) using ElevenLabs API.
     */
    suspend fun synthesizeSpeech(
        text: String,
        voiceId: String = DEFAULT_VOICE_ID
    ): ByteArray = withContext(Dispatchers.IO) {
        val apiKey = AyraPreferences.getElevenLabsKey(context).trim()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("No ElevenLabs API key configured.")
        }

        val jsonPayload = JSONObject().apply {
            put("text", text.take(1500))
            put("model_id", "eleven_multilingual_v2")
            put("voice_settings", JSONObject().apply {
                put("stability", 0.5)
                put("similarity_boost", 0.75)
            })
        }

        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
            .header("xi-api-key", apiKey)
            .header("Accept", "audio/mpeg")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { resp ->
            if (resp.code == 401) {
                throw IOException("Invalid ElevenLabs API key (HTTP 401).")
            }
            if (resp.code == 429) {
                throw IOException("ElevenLabs character quota exceeded.")
            }
            if (!resp.isSuccessful) {
                throw IOException("ElevenLabs error: HTTP ${resp.code}")
            }
            resp.body?.bytes() ?: throw IOException("Empty audio response from ElevenLabs.")
        }
    }

    /**
     * Real connection test against ElevenLabs user profile endpoint.
     * Returns latency in milliseconds or throws descriptive error.
     */
    suspend fun testConnection(customKey: String? = null): Long = withContext(Dispatchers.IO) {
        val apiKey = (customKey ?: AyraPreferences.getElevenLabsKey(context)).trim()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("ElevenLabs API key is empty.")
        }

        val start = System.currentTimeMillis()
        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/user")
            .header("xi-api-key", apiKey)
            .header("Accept", "application/json")
            .get()
            .build()

        httpClient.newCall(request).execute().use { resp ->
            if (resp.code == 401) {
                throw IOException("Invalid ElevenLabs API key (HTTP 401)")
            }
            if (!resp.isSuccessful) {
                throw IOException("ElevenLabs ping failed: HTTP ${resp.code}")
            }
            System.currentTimeMillis() - start
        }
    }
}
