package com.ayra.ai.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Advanced Voice Assistant Manager for Ayra AI.
 * Features:
 * - Dynamic Voice Modulation: alters pitch, speech rate, and emotional tone based on detected user sentiment in GF Mode.
 * - Always-on background wake-word listening ("Ayra" / "আয়রা").
 * - Multi-language speech recognition and natural female TTS (Bengali, Hindi, English).
 */
class VoiceAssistantManager(
    private val context: Context,
    private val onCommandReceived: (String) -> Unit
) : TextToSpeech.OnInitListener, RecognitionListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _liveSpokenText = MutableStateFlow("")
    val liveSpokenText: StateFlow<String> = _liveSpokenText.asStateFlow()

    // Dynamic Voice Modulation & Sentiment State
    private val _currentSentiment = MutableStateFlow(UserSentiment.NEUTRAL_CARING)
    val currentSentiment: StateFlow<UserSentiment> = _currentSentiment.asStateFlow()

    var isDynamicModulationEnabled: Boolean = true
    var manualSentimentLock: UserSentiment? = null

    var onWakeWordTriggered: ((greeting: String) -> Unit)? = null
    var onSpeechTranscribed: ((text: String, isFinal: Boolean) -> Unit)? = null

    var personaMode: String = "GF_COMPANION" // "GF_COMPANION" or "PROFESSIONAL"
    var isAlwaysOnWakeWordEnabled: Boolean = false
    var isManualSession: Boolean = false

    private var isTtsReady = false
    private var isDestroyed = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
        initRecognizer()
    }

    private fun initRecognizer() {
        if (isDestroyed) return
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceAssistantManager)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            setupVoiceModulation(Locale.US, _currentSentiment.value)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    // Once done speaking, immediately re-listen if wake word is active
                    if (isAlwaysOnWakeWordEnabled && !isDestroyed) {
                        mainHandler.postDelayed({
                            startListening()
                        }, 400)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    if (isAlwaysOnWakeWordEnabled && !isDestroyed) {
                        mainHandler.postDelayed({
                            startListening()
                        }, 500)
                    }
                }
            })
        }
    }

    /**
     * Dynamically configures the TTS engine pitch, speech rate, and female voice
     * according to the persona and detected user sentiment.
     */
    private fun setupVoiceModulation(targetLocale: Locale, sentiment: UserSentiment) {
        tts?.let { engine ->
            engine.language = targetLocale

            if (personaMode == "GF_COMPANION") {
                // Apply dynamic emotional modulation
                val activeSentiment = manualSentimentLock ?: sentiment
                engine.setPitch(activeSentiment.pitch)
                engine.setSpeechRate(activeSentiment.speechRate)
            } else {
                // Professional neutral voice
                engine.setPitch(1.00f)
                engine.setSpeechRate(1.00f)
            }

            try {
                val voices = engine.voices
                if (!voices.isNullOrEmpty()) {
                    val sweetFemaleVoice = voices.find { v ->
                        v.locale.language == targetLocale.language &&
                                (v.name.contains("female", ignoreCase = true) ||
                                        v.name.contains("woman", ignoreCase = true) ||
                                        v.name.contains("#female", ignoreCase = true) ||
                                        v.name.contains("sweet", ignoreCase = true) ||
                                        v.quality == Voice.QUALITY_VERY_HIGH)
                    } ?: voices.find { it.locale.language == targetLocale.language }
                    if (sweetFemaleVoice != null) {
                        engine.voice = sweetFemaleVoice
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Analyzes user prompt and updates active emotional state.
     */
    fun detectAndApplySentiment(userPrompt: String): UserSentiment {
        if (!isDynamicModulationEnabled || manualSentimentLock != null) {
            return manualSentimentLock ?: _currentSentiment.value
        }
        val detected = SentimentDetector.detectSentiment(userPrompt)
        _currentSentiment.value = detected
        return detected
    }

    /**
     * Speaks text using dynamic sentiment-modulated pitch and rate.
     */
    fun speak(text: String, overrideSentiment: UserSentiment? = null) {
        if (!isTtsReady || text.isBlank()) return
        stopListening()

        // Determine active sentiment profile
        val targetSentiment = when {
            manualSentimentLock != null -> manualSentimentLock!!
            overrideSentiment != null -> overrideSentiment
            isDynamicModulationEnabled -> {
                val detected = SentimentDetector.detectSentiment(text)
                if (detected != UserSentiment.NEUTRAL_CARING) detected else _currentSentiment.value
            }
            else -> _currentSentiment.value
        }
        _currentSentiment.value = targetSentiment

        // Clean markdown tags & symbols for natural voice
        val cleaned = text
            .replace(Regex("```[\\s\\S]*?```"), "Code snippet provided in chat.")
            .replace(Regex("[*#`_~>|\\[\\]()]"), " ")
            .trim()

        // Detect language script
        val isBengali = cleaned.any { it in '\u0980'..'\u09FF' }
        val isHindi = cleaned.any { it in '\u0900'..'\u097F' }

        val locale = when {
            isBengali -> Locale("bn", "IN")
            isHindi -> Locale("hi", "IN")
            else -> Locale.US
        }

        setupVoiceModulation(locale, targetSentiment)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "AyraSweetVoice_${System.currentTimeMillis()}")
        }
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, params, "AyraSweetVoice")
    }

    /**
     * Triggers a live demonstration of any sentiment tone.
     */
    fun testSentimentVoice(sentiment: UserSentiment, isBengali: Boolean = true) {
        val sample = if (isBengali) sentiment.sampleGreetingBn else sentiment.sampleGreetingEn
        speak(sample, overrideSentiment = sentiment)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun startManualListening() {
        isManualSession = true
        startListening()
    }

    fun startListening() {
        if (isDestroyed || _isSpeaking.value) return
        if (speechRecognizer == null) initRecognizer()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Multi-language support: Bengali, Hindi, English
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-IN", "hi-IN", "en-US"))
        }

        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (_: Exception) {
            _isListening.value = false
            if (!isManualSession) scheduleWakeWordRestart(1200)
        }
    }

    fun stopListening() {
        isManualSession = false
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
    }

    private fun scheduleWakeWordRestart(delayMs: Long = 800) {
        if (isAlwaysOnWakeWordEnabled && !isDestroyed && !_isSpeaking.value && !isManualSession) {
            mainHandler.removeCallbacksAndMessages(null)
            mainHandler.postDelayed({
                if (isAlwaysOnWakeWordEnabled && !isDestroyed && !_isSpeaking.value && !isManualSession) {
                    startListening()
                }
            }, delayMs)
        }
    }

    fun destroy() {
        isDestroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
    }

    // RecognitionListener Callbacks
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
        val wasManual = isManualSession
        _isListening.value = false
        isManualSession = false
        if (!wasManual) {
            scheduleWakeWordRestart(1200)
        }
    }

    override fun onResults(results: Bundle?) {
        val wasManual = isManualSession
        isManualSession = false
        _isListening.value = false

        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spoken = matches[0].trim()
            _liveSpokenText.value = spoken

            if (wasManual) {
                // User explicitly pressed the Mic Button: Transcribe directly to chat!
                onSpeechTranscribed?.invoke(spoken, true)
                onCommandReceived(spoken)
                return
            }

            // BACKGROUND WAKE-WORD MODE: STRICT FILTERING
            // User did not press mic button; assistant MUST NOT respond unless "Ayra" was explicitly spoken!
            val lower = spoken.lowercase()
                .replace(Regex("[.,?!।]"), "")
                .trim()

            val hasWakeWord = lower.contains("ayra") || lower.contains("aira") || lower.contains("eyra") ||
                    lower.contains("আয়রা") || lower.contains("আইরা") || lower.contains("আয়রা") ||
                    lower.contains("आयरा") || lower.contains("ऐরা")

            if (!hasWakeWord) {
                // Background conversation or ambient TV/noise: IGNORE completely!
                scheduleWakeWordRestart(800)
                return
            }

            val isWakeWordOnly = lower in listOf(
                "ayra", "hey ayra", "hi ayra", "hello ayra", "ok ayra", "okay ayra",
                "aira", "hey aira", "hi aira", "hello aira",
                "ayraa", "hey ayraa", "eyra", "hey eyra",
                "আয়রা", "আইরা", "আয়রা", "হেই আয়রা", "হেই আইরা", "হেই আয়রা",
                "এই আয়রা", "এই আইরা", "এই আয়রা", "হ্যালো আয়রা", "হ্যালো আইরা",
                "ওগো আয়রা", "আরে আয়রা",
                "आयरा", "ऐरा", "हे आयरा", "अरे आयरा"
            ) || ((lower.startsWith("ayra") || lower.startsWith("aira") || lower.startsWith("আয়রা") || lower.startsWith("আইরা") || lower.startsWith("আয়রা")) && lower.length <= 8)

            if (isWakeWordOnly) {
                // Wake word detected! Mode-specific friendly greeting
                val isBengali = spoken.any { it in '\u0980'..'\u09FF' }
                val isHindi = spoken.any { it in '\u0900'..'\u097F' }

                val greeting = if (personaMode == "GF_COMPANION") {
                    when {
                        isBengali -> "হ্যাঁ সোনা, বলো শুনছি... তোমার জন্যই তো আছি ❤️"
                        isHindi -> "हाँ जानू, बोलो मैं सुन रही हूँ... ❤️"
                        else -> "Hey sweetie! I'm right here listening to you ❤️"
                    }
                } else {
                    when {
                        isBengali -> "হ্যাঁ, বলুন। আমি আপনাকে কীভাবে সাহায্য করতে পারি?"
                        isHindi -> "हाँ, बताइए। मैं आपकी क्या सहायता कर सकता हूँ?"
                        else -> "Yes, I am listening. How can I assist you today?"
                    }
                }

                val targetSentiment = if (personaMode == "GF_COMPANION") UserSentiment.AFFECTIONATE else UserSentiment.NEUTRAL_CARING
                speak(greeting, overrideSentiment = targetSentiment)
                onWakeWordTriggered?.invoke(greeting)
                return
            }

            // Clean wake word prefix from explicit command (e.g. "Ayra open YouTube" -> "open YouTube")
            val cleanCommand = spoken
                .replace(Regex("(?i)^(hey\\s+|hi\\s+|hello\\s+|ok\\s+|okay\\s+)?(ayra|aira|eyra)(\\s+ai)?[,\\s]*"), "")
                .replace(Regex("^(হেই\\s+|এই\\s+|ওগো\\s+|হ্যালো\\s+)?(আয়রা|আইরা|আয়রা)[,\\s]*"), "")
                .trim()

            val commandToRun = if (cleanCommand.isNotBlank()) cleanCommand else spoken
            onCommandReceived(commandToRun)
        } else {
            if (!wasManual) {
                scheduleWakeWordRestart(600)
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val partial = matches[0]
            _liveSpokenText.value = partial
            if (isManualSession) {
                onSpeechTranscribed?.invoke(partial, false)
            }
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}
}
