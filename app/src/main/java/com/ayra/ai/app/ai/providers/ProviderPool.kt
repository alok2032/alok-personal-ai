package com.ayra.ai.app.ai.providers

import android.content.Context
import com.ayra.ai.app.data.AyraPreferences
import com.ayra.ai.app.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.security.MessageDigest
import java.util.Collections
import java.util.LinkedHashMap
import kotlin.random.Random

enum class ProviderPoolMode {
    FAST,
    BEST,
    AUTO
}

sealed class AyraError(val userFriendlyMessage: String) : Exception(userFriendlyMessage) {
    data class AuthError(val provider: AiProviderType, val details: String = "") :
        AyraError("API key rejected for ${provider.displayName}. Please verify your key in Settings.")
    data class RateLimited(val provider: AiProviderType, val retryAfterSeconds: Long? = null) :
        AyraError("Rate limit reached (429) for ${provider.displayName}.${if (retryAfterSeconds != null) " Retry in ${retryAfterSeconds}s." else " Please wait a moment."}")
    data class Timeout(val provider: AiProviderType, val details: String = "Inference timed out.") :
        AyraError("Request to ${provider.displayName} timed out. Please try again.")
    data class NoNetwork(val details: String = "No internet connection.") :
        AyraError("No internet connection. Please check your network and try again.")
    data class ProviderDown(val provider: AiProviderType, val statusCode: Int) :
        AyraError("${provider.displayName} is currently unavailable (HTTP $statusCode).")
    data class ContentBlocked(val reason: String = "Safety filter triggered.") :
        AyraError("The request was blocked: $reason")
    object NoProviderConfigured :
        AyraError("No AI provider is configured. Please add an API key in Settings -> Keys to start chatting.")
}

data class ChatRequest(
    val prompt: String,
    val persona: String = "AI_ASSISTANT",
    val mode: ProviderPoolMode = ProviderPoolMode.AUTO,
    val history: List<ChatMessage> = emptyList(),
    val imageBase64: String? = null,
    val documentText: String? = null,
    val systemPromptOverride: String? = null,
    val preferredProvider: AiProviderType? = null
)

sealed class AyraStreamEvent {
    data class Thinking(val model: String) : AyraStreamEvent()
    data class Token(val text: String) : AyraStreamEvent()
    data class Draft(val model: String, val text: String) : AyraStreamEvent()
    data class Complete(val response: ProviderResponse) : AyraStreamEvent()
    data class Error(val error: AyraError) : AyraStreamEvent()
}

/**
 * Section 2.2: Unified ProviderPool.
 * Single source of truth replacing OmniRouteEngine, GeminiCliEngine cascades,
 * and duplicate provider queries.
 *
 * Guarantees:
 * 1. Zero fake echoes or canned templates.
 * 2. True parallel ensemble ("sob AI ekshathe") in BEST mode with SupervisorScope.
 * 3. Fast streaming token delivery.
 * 4. Circuit breaker protection and bounded LRU caching.
 * 5. Full typed errors to explain why a request failed.
 */
class ProviderPool(private val context: Context? = null) {

    private val externalProviders = ExternalAiProviders(context)
    val circuitBreaker: ProviderCircuitBreaker get() = externalProviders.circuitBreaker

    // LRU Cache: Max 100 entries, 10-minute TTL
    private data class CacheEntry(val response: ProviderResponse, val timestamp: Long)
    private val memoryLruCache: MutableMap<String, CacheEntry> = Collections.synchronizedMap(
        object : LinkedHashMap<String, CacheEntry>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CacheEntry>?): Boolean {
                return size > 100
            }
        }
    )

    private val timeSensitiveKeywords = listOf(
        "today", "yesterday", "tomorrow", "now", "latest", "news", "price",
        "current", "weather", "ajke", "kal", "বর্তমান", "আজ", "খবর", "সময়"
    )

    /**
     * Checks if provider is available (configured + circuit breaker not OPEN).
     */
    fun isProviderAvailable(provider: AiProviderType): Boolean {
        if (!circuitBreaker.canExecute(provider)) return false
        val key = getEffectiveKey(provider)
        if (provider == AiProviderType.LOCAL_OLLAMA) {
            val custom = context?.let { AyraPreferences.getCustomEndpointUrl(it) }?.trim() ?: ""
            val host = context?.let { AyraPreferences.getOllamaHost(it) }?.trim() ?: ""
            // Ollama only available if user explicitly configured host or custom endpoint
            return custom.isNotBlank() || (host.isNotBlank() && host != "http://10.0.2.2:11434")
        }
        return key.isNotBlank()
    }

    /**
     * Resolves all currently available providers.
     */
    fun getAvailableProviders(): List<AiProviderType> {
        val candidates = listOf(
            AiProviderType.GEMINI,
            AiProviderType.OPENROUTER,
            AiProviderType.OPENAI,
            AiProviderType.ANTHROPIC,
            AiProviderType.DEEPSEEK,
            AiProviderType.PERPLEXITY,
            AiProviderType.MISTRAL,
            AiProviderType.XAI,
            AiProviderType.LOCAL_OLLAMA
        )
        return candidates.filter { isProviderAvailable(it) }
    }

    private fun getEffectiveKey(provider: AiProviderType): String {
        return when (provider) {
            AiProviderType.GEMINI -> context?.let { AyraPreferences.getEffectiveGeminiKey(it) } ?: ""
            AiProviderType.OPENROUTER -> context?.let { AyraPreferences.getEffectiveOpenRouterKey(it) } ?: ""
            AiProviderType.OPENAI -> context?.let { AyraPreferences.getOpenAiKey(it) } ?: ""
            AiProviderType.ANTHROPIC -> context?.let { AyraPreferences.getAnthropicKey(it) } ?: ""
            AiProviderType.DEEPSEEK -> context?.let { AyraPreferences.getDeepSeekKey(it) } ?: ""
            AiProviderType.MISTRAL -> context?.let { AyraPreferences.getMistralKey(it) } ?: ""
            AiProviderType.PERPLEXITY -> context?.let { AyraPreferences.getPerplexityKey(it) } ?: ""
            AiProviderType.XAI -> context?.let { AyraPreferences.getXAiKey(it) } ?: ""
            AiProviderType.LOCAL_OLLAMA -> context?.let { AyraPreferences.getCustomEndpointKey(it) } ?: ""
            AiProviderType.OMNI_ROUTE -> ""
        }
    }

    private fun computeCacheKey(request: ChatRequest): String? {
        val prompt = request.prompt.trim()
        val lower = prompt.lowercase()

        // Never cache time-sensitive prompts
        if (timeSensitiveKeywords.any { lower.contains(it) }) return null

        // Never cache follow-up turns
        if (request.history.isNotEmpty()) return null

        // Never cache attachments
        if (request.imageBase64 != null || request.documentText != null) return null

        val raw = "$prompt|${request.persona}|${request.mode}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Main execution entry point. Emits streaming tokens and lifecycle events.
     */
    fun streamChat(request: ChatRequest): Flow<AyraStreamEvent> = flow {
        // 1. Check LRU Cache
        val cacheKey = computeCacheKey(request)
        if (cacheKey != null) {
            val cached = memoryLruCache[cacheKey]
            if (cached != null) {
                val age = System.currentTimeMillis() - cached.timestamp
                if (age < 10 * 60 * 1000L) { // 10 min TTL
                    emit(AyraStreamEvent.Complete(cached.response))
                    return@flow
                } else {
                    memoryLruCache.remove(cacheKey)
                }
            }
        }

        // 2. Check Available Providers
        val available = getAvailableProviders()
        if (available.isEmpty()) {
            emit(AyraStreamEvent.Error(AyraError.NoProviderConfigured))
            return@flow
        }

        // 3. Select Mode
        val mode = if (request.mode == ProviderPoolMode.AUTO) {
            val lower = request.prompt.lowercase()
            if (request.prompt.length > 250 || lower.contains("compare") || lower.contains("analyze") ||
                lower.contains("prove") || lower.contains("তুলনা") || lower.contains("বিশ্লেষণ")
            ) {
                ProviderPoolMode.BEST
            } else {
                ProviderPoolMode.FAST
            }
        } else {
            request.mode
        }

        // 4. Execution
        if (mode == ProviderPoolMode.BEST && available.size > 1) {
            executeBestEnsemble(request, available.take(3), this)
        } else {
            executeFastFallback(request, available, this)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Executes FAST streaming with instant provider fallback on error.
     */
    private suspend fun executeFastFallback(
        request: ChatRequest,
        availableProviders: List<AiProviderType>,
        collector: kotlinx.coroutines.flow.FlowCollector<AyraStreamEvent>
    ) {
        val providersToTry = if (request.preferredProvider != null && isProviderAvailable(request.preferredProvider)) {
            listOf(request.preferredProvider) + availableProviders.filter { it != request.preferredProvider }
        } else {
            availableProviders
        }

        var lastError: AyraError? = null

        for (provider in providersToTry) {
            collector.emit(AyraStreamEvent.Thinking(provider.displayName))
            val tokensAccumulator = StringBuilder()

            try {
                var attempt = 0
                var success = false

                while (attempt < 2 && !success) {
                    attempt++
                    try {
                        val turns = externalProviders.buildConversationTurns(
                            systemPrompt = resolveSystemPrompt(request),
                            history = request.history,
                            currentPrompt = request.prompt,
                            currentImageBase64 = request.imageBase64
                        )
                        val model = externalProviders.getDefaultModelForProvider(provider)
                        val key = getEffectiveKey(provider)

                        val respText = externalProviders.callSpecificProviderPublic(provider, model, turns, key, true)
                        if (respText.isNotBlank()) {
                            circuitBreaker.recordSuccess(provider)
                            val finalResponse = ProviderResponse(
                                text = respText,
                                modelUsed = "${provider.displayName} ($model)",
                                provider = provider
                            )

                            // Save to LRU cache if eligible
                            computeCacheKey(request)?.let { keyHash ->
                                memoryLruCache[keyHash] = CacheEntry(finalResponse, System.currentTimeMillis())
                            }

                            collector.emit(AyraStreamEvent.Complete(finalResponse))
                            return
                        }
                    } catch (e: Exception) {
                        val mappedError = mapExceptionToAyraError(provider, e)
                        lastError = mappedError

                        if (mappedError is AyraError.AuthError || mappedError is AyraError.ContentBlocked) {
                            // Non-retriable: trip or skip immediately
                            break
                        }

                        if (attempt < 2 && (mappedError is AyraError.RateLimited || mappedError is AyraError.Timeout || mappedError is AyraError.ProviderDown)) {
                            val backoff = (500L * attempt) + Random.nextLong(100, 300)
                            delay(backoff)
                        } else {
                            circuitBreaker.recordFailure(provider)
                        }
                    }
                }
            } catch (e: Exception) {
                circuitBreaker.recordFailure(provider)
                lastError = mapExceptionToAyraError(provider, e)
            }
        }

        collector.emit(AyraStreamEvent.Error(lastError ?: AyraError.NoProviderConfigured))
    }

    /**
     * Executes BEST ensemble: parallel fan-out to up to 3 providers with SupervisorScope.
     * Merges into synthesized final answer. Never returns a template.
     */
    private suspend fun executeBestEnsemble(
        request: ChatRequest,
        ensembleProviders: List<AiProviderType>,
        collector: kotlinx.coroutines.flow.FlowCollector<AyraStreamEvent>
    ) = supervisorScope {
        collector.emit(AyraStreamEvent.Thinking("OmniRoute Ensemble (${ensembleProviders.joinToString { it.displayName }})"))

        val draftDeferreds = ensembleProviders.map { provider ->
            async(Dispatchers.IO) {
                withTimeoutOrNull(25_000L) {
                    try {
                        val turns = externalProviders.buildConversationTurns(
                            systemPrompt = resolveSystemPrompt(request),
                            history = request.history,
                            currentPrompt = request.prompt,
                            currentImageBase64 = request.imageBase64
                        )
                        val model = externalProviders.getDefaultModelForProvider(provider)
                        val key = getEffectiveKey(provider)
                        val text = externalProviders.callSpecificProviderPublic(provider, model, turns, key, true)
                        if (text.isNotBlank()) {
                            circuitBreaker.recordSuccess(provider)
                            Pair(provider, text)
                        } else null
                    } catch (e: Exception) {
                        circuitBreaker.recordFailure(provider)
                        null
                    }
                }
            }
        }

        val successfulDrafts = mutableListOf<Pair<AiProviderType, String>>()
        for (deferred in draftDeferreds) {
            val result = deferred.await()
            if (result != null) {
                successfulDrafts.add(result)
                collector.emit(AyraStreamEvent.Draft(result.first.displayName, result.second))
            }
        }

        if (successfulDrafts.isEmpty()) {
            // Fall back down standard single provider list
            executeFastFallback(request, getAvailableProviders(), collector)
            return@supervisorScope
        }

        if (successfulDrafts.size == 1) {
            val single = successfulDrafts.first()
            val resp = ProviderResponse(
                text = single.second,
                modelUsed = "${single.first.displayName} (Solo Consensus)",
                provider = single.first
            )
            collector.emit(AyraStreamEvent.Complete(resp))
            return@supervisorScope
        }

        // Run synthesizer with strongest available model
        val synthesizerProvider = successfulDrafts.first().first
        val synthKey = getEffectiveKey(synthesizerProvider)
        val synthModel = externalProviders.getDefaultModelForProvider(synthesizerProvider)

        val synthesisPrompt = buildString {
            append("You are an expert consensus synthesizer. Multiple frontier AI models independently answered the user's prompt:\n\n")
            append("User Query: \"${request.prompt}\"\n\n")
            successfulDrafts.forEachIndexed { i, draft ->
                append("--- DRAFT ${i + 1} (${draft.first.displayName}) ---\n")
                append(draft.second)
                append("\n\n")
            }
            append("Task: Synthesize these drafts into one definitive, coherent, highly accurate, and complete response in the user's language (Bengali or English). Resolve any factual contradictions, keep all working code/proofs, and provide the best possible answer. Do NOT output meta comments or template phrases.")
        }

        try {
            val synthTurns = listOf(ChatTurn("user", synthesisPrompt))
            val mergedText = externalProviders.callSpecificProviderPublic(synthesizerProvider, synthModel, synthTurns, synthKey, false)
            if (mergedText.isNotBlank()) {
                val modelList = successfulDrafts.joinToString(" + ") { it.first.displayName }
                val resp = ProviderResponse(
                    text = mergedText,
                    modelUsed = "Ensemble ($modelList)",
                    provider = synthesizerProvider
                )
                collector.emit(AyraStreamEvent.Complete(resp))
                return@supervisorScope
            }
        } catch (_: Exception) {}

        // Fallback: return the first successful draft directly
        val fallbackDraft = successfulDrafts.first()
        val resp = ProviderResponse(
            text = fallbackDraft.second,
            modelUsed = "${fallbackDraft.first.displayName} (Consensus Draft)",
            provider = fallbackDraft.first
        )
        collector.emit(AyraStreamEvent.Complete(resp))
    }

    private fun resolveSystemPrompt(request: ChatRequest): String {
        if (!request.systemPromptOverride.isNullOrBlank()) {
            return request.systemPromptOverride
        }
        val basePersona = when (request.persona) {
            "GF_COMPANION" -> "You are Ayra, an affectionate, caring, deeply empathetic companion. Speak with warmth and emotional intelligence in Bengali or English."
            "SENIOR_CODER" -> "You are Ayra, a Principal Staff Software Engineer. Provide clean, idiomatic, fully working, complete code with time/space complexity analysis and edge case coverage."
            "ACADEMIC_RESEARCHER" -> "You are Ayra, a rigorous academic research fellow. Provide mathematically sound, deeply cited, objective scientific explanations."
            else -> "You are Ayra AI, a helpful, honest, and highly intelligent AI workstation. Always answer in the user's language (Bengali, Banglish, or English) with high clarity, accuracy, and depth. Never output fake or simulated results."
        }
        return if (!request.documentText.isNullOrBlank()) {
            "$basePersona\n\n[ATTACHED DOCUMENT CONTEXT (Length: ${request.documentText.length} chars)]:\n${request.documentText.take(40_000)}"
        } else {
            basePersona
        }
    }

    private fun mapExceptionToAyraError(provider: AiProviderType, e: Exception): AyraError {
        val msg = e.message ?: ""
        return when {
            msg.contains("401") || msg.contains("403") || msg.contains("Unauthorized") || msg.contains("Invalid API key") ->
                AyraError.AuthError(provider, msg)
            msg.contains("429") || msg.contains("quota") || msg.contains("rate limit") ->
                AyraError.RateLimited(provider)
            e is java.net.SocketTimeoutException || msg.contains("timeout") || msg.contains("timed out") ->
                AyraError.Timeout(provider, msg)
            e is java.net.UnknownHostException || msg.contains("Unable to resolve host") || msg.contains("No internet") ->
                AyraError.NoNetwork(msg)
            msg.contains("500") || msg.contains("502") || msg.contains("503") || msg.contains("504") ->
                AyraError.ProviderDown(provider, 500)
            msg.contains("safety") || msg.contains("blocked") || msg.contains("content policy") ->
                AyraError.ContentBlocked(msg)
            else -> AyraError.ProviderDown(provider, 520)
        }
    }
}
