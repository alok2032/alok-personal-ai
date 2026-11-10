package com.ayra.ai.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Autonomous Voice Announcer for background tasks, reminders, and scheduled call message announcements.
 */
object AyraVoiceAnnouncer {

    fun playAlarmChime(context: Context) {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, alertUri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    fun speakAloud(context: Context, textToSpeak: String, onComplete: (() -> Unit)? = null) {
        val appContext = context.applicationContext
        val isBengali = textToSpeak.any { it in '\u0980'..'\u09FF' }
        val targetLocale = if (isBengali) Locale("bn", "BD") else Locale.US

        var ttsInstance: TextToSpeech? = null
        val handler = Handler(Looper.getMainLooper())

        handler.post {
            ttsInstance = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsInstance?.let { engine ->
                        engine.language = targetLocale
                        engine.setPitch(1.05f)
                        engine.setSpeechRate(0.95f)

                        // Route through stream notification or alarm
                        val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                        engine.setAudioAttributes(audioAttributes)

                        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {}

                            override fun onDone(utteranceId: String?) {
                                onComplete?.invoke()
                                handler.postDelayed({
                                    try {
                                        engine.shutdown()
                                    } catch (_: Exception) {}
                                }, 1000)
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                onComplete?.invoke()
                                handler.postDelayed({
                                    try {
                                        engine.shutdown()
                                    } catch (_: Exception) {}
                                }, 1000)
                            }
                        })

                        engine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "ayra_scheduled_announcement")
                    }
                }
            }
        }
    }
}
