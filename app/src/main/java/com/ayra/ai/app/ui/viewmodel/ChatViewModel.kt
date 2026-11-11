package com.ayra.ai.app.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayra.ai.app.automation.ProposedAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production ViewModel for Ayra Chat.
 * Guarantees lifecycle safety across screen rotations and hardware configuration changes.
 * Enforces atomic send locks to completely prevent double-tap race conditions.
 * Supports active cancellation via Stop button and streaming token accumulation.
 */
class ChatViewModel : ViewModel() {

    private val sendMutex = Mutex()
    private var inFlightJob: Job? = null

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _thinkingStatus = MutableStateFlow<String?>(null)
    val thinkingStatus: StateFlow<String?> = _thinkingStatus.asStateFlow()

    private val _streamingAnswer = MutableStateFlow<String?>(null)
    val streamingAnswer: StateFlow<String?> = _streamingAnswer.asStateFlow()

    private val _showKeyOnboarding = MutableStateFlow(false)
    val showKeyOnboarding: StateFlow<Boolean> = _showKeyOnboarding.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _selectedDocumentUri = MutableStateFlow<Uri?>(null)
    val selectedDocumentUri: StateFlow<Uri?> = _selectedDocumentUri.asStateFlow()

    private val _selectedDocumentName = MutableStateFlow<String?>(null)
    val selectedDocumentName: StateFlow<String?> = _selectedDocumentName.asStateFlow()

    private val _selectedDocumentText = MutableStateFlow<String?>(null)
    val selectedDocumentText: StateFlow<String?> = _selectedDocumentText.asStateFlow()

    private val _pendingAction = MutableStateFlow<ProposedAction?>(null)
    val pendingAction: StateFlow<ProposedAction?> = _pendingAction.asStateFlow()

    private val _currentPersona = MutableStateFlow("AI_ASSISTANT")
    val currentPersona: StateFlow<String> = _currentPersona.asStateFlow()

    private val _selectedModelTier = MutableStateFlow("auto")
    val selectedModelTier: StateFlow<String> = _selectedModelTier.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun clearInput() {
        _inputText.value = ""
    }

    fun setPersona(persona: String) {
        _currentPersona.value = persona
    }

    fun setModelTier(tier: String) {
        _selectedModelTier.value = tier
    }

    fun setPendingAction(action: ProposedAction?) {
        _pendingAction.value = action
    }

    fun attachImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun attachDocument(uri: Uri?, name: String?, text: String?) {
        _selectedDocumentUri.value = uri
        _selectedDocumentName.value = name
        _selectedDocumentText.value = text
    }

    fun clearAttachments() {
        _selectedImageUri.value = null
        _selectedDocumentUri.value = null
        _selectedDocumentName.value = null
        _selectedDocumentText.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setShowKeyOnboarding(show: Boolean) {
        _showKeyOnboarding.value = show
    }

    fun setThinkingStatus(status: String?) {
        _thinkingStatus.value = status
    }

    fun setStreamingAnswer(answer: String?) {
        _streamingAnswer.value = answer
    }

    fun appendStreamingToken(token: String) {
        val current = _streamingAnswer.value ?: ""
        _streamingAnswer.value = current + token
    }

    fun setInFlightJob(job: Job?) {
        inFlightJob = job
    }

    /**
     * Cancels active inference and resets generation state.
     */
    fun stopGeneration() {
        inFlightJob?.cancel()
        inFlightJob = null
        _isGenerating.value = false
        _thinkingStatus.value = null
        _streamingAnswer.value = null
    }

    /**
     * Atomically locks and checks if a new send operation can begin.
     * Prevents duplicate requests from rapid user double-taps.
     */
    suspend fun tryStartSending(): Boolean = sendMutex.withLock {
        if (_isGenerating.value) {
            return false
        }
        _isGenerating.value = true
        _errorMessage.value = null
        _streamingAnswer.value = null
        _thinkingStatus.value = "Thinking..."
        return true
    }

    /**
     * Marks generation as finished and releases send lock.
     */
    fun finishSending(error: String? = null) {
        _isGenerating.value = false
        _thinkingStatus.value = null
        _streamingAnswer.value = null
        _errorMessage.value = error
        inFlightJob = null
    }
}
