package com.ayra.ai.app.ai.providers

import android.content.Context
import com.ayra.ai.app.data.AyraPreferences
import com.ayra.ai.app.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

enum class AiProviderType(val id: String, val displayName: String, val defaultBaseUrl: String) {
    GEMINI("gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta/"),
    OPENAI("openai", "OpenAI", "https://api.openai.com/v1/"),
    ANTHROPIC("anthropic", "Anthropic Claude", "https://api.anthropic.com/v1/"),
    DEEPSEEK("deepseek", "DeepSeek", "https://api.deepseek.com/"),
    XAI("xai", "xAI Grok", "https://api.x.ai/v1/"),
    MISTRAL("mistral", "Mistral AI", "https://api.mistral.ai/v1/"),
    PERPLEXITY("perplexity", "Perplexity AI", "https://api.perplexity.ai/"),
    OPENROUTER("openrouter", "OpenRouter", "https://openrouter.ai/api/v1/"),
    LOCAL_OLLAMA("local_ollama", "Local Ollama / LM Studio", "http://10.0.2.2:11434/v1/"),
    KEYLESS_COMMUNITY("keyless", "Keyless Community Fallback", "https://text.pollinations.ai/")
}

data class AiModelInfo(
    val id: String,
    val displayName: String,
    val provider: AiProviderType,
    val description: String,
    val contextWindow: String,
    val supportsVision: Boolean = true,
    val isReasoning: Boolean = false,
    val isDefault: Boolean = false
)

enum class AiRoutingMode(val id: String, val title: String, val icon: String, val description: String) {
    AUTO("auto", "Auto Adaptive", "⚡", "Intelligently routes based on complexity, language, and math"),
    BEST_QUALITY("quality", "Best Quality", "👑", "Routes to highest benchmark reasoning models (Gemini 2.5 Pro, GPT-4o, Claude 3.7)"),
    FAST("fast", "Ultra Fast", "🚀", "Sub-second low latency responses (Gemini 2.5 Flash, GPT-4o Mini)"),
    REASONING("reasoning", "Deep Reasoning", "🧠", "Chain-of-thought verification (DeepSeek R1, o3-mini, Gemini 2.5 Pro)"),
    CODING("coding", "Software & Coding", "💻", "Architectural coding specialist (Claude 3.7 Sonnet, Codestral, Qwen 2.5 Coder)"),
    PRIVACY_LOCAL("local", "Private / Local LAN", "🏠", "Zero-cloud private local LLM running on LAN/Ollama"),
    COST_EFFICIENT("cost", "Cost Efficient / Free", "🌱", "Zero-cost or lowest token consumption routes"),
    QWEN_UNCENSORED("qwen_uncensored", "Qwen 3.8 Uncensored", "🔓", "Abliterated 27B weights — zero refusal answers to any question in the world")
}

data class ProviderResponse(
    val text: String,
    val modelUsed: String,
    val provider: AiProviderType,
    val finishReason: String = "stop",
    val latencyMs: Long = 0
)

sealed class ProviderError(val displayMessage: String) : Exception(displayMessage) {
    class NoKeyConfigured(provider: AiProviderType) :
        ProviderError("No API key configured for ${provider.displayName}. Tap Settings to configure your key.")
    class RateLimited(message: String) :
        ProviderError("Provider rate limit reached (HTTP 429). Falling back to alternate model...")
    class Unauthorized(provider: AiProviderType) :
        ProviderError("Invalid API key for ${provider.displayName}. Please verify your key in Settings.")
    class ServerError(code: Int, message: String) :
        ProviderError("Remote server error (HTTP $code): $message")
    class Timeout(message: String = "Request timed out while waiting for model inference.") :
        ProviderError(message)
    class NetworkFailure(message: String) :
        ProviderError("Network connection failed: $message")
}

data class ChatTurn(
    val role: String, // "system", "user", "assistant"
    val content: String,
    val imageBase64: String? = null
)

/**
 * Enterprise-grade multi-model neural AI engine for Ayra AI.
 * Handles request construction, streaming token delivery, multi-turn history trimming,
 * and intelligent automatic failover across 8 major AI providers.
 */
class ExternalAiProviders(private val context: Context? = null) {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    val circuitBreaker = ProviderCircuitBreaker()

    companion object {
        private val dynamicModelCache = java.util.concurrent.ConcurrentHashMap<AiProviderType, List<AiModelInfo>>()

        val CURATED_MODELS = listOf(
            // Google Gemini (Latest 2.5 and 2.0 series)
            AiModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash ⚡", AiProviderType.GEMINI, "World's fastest flagship multimodal intelligence", "1M tokens", true, false, true),
            AiModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro 🔬", AiProviderType.GEMINI, "Frontier deep reasoning, code generation & synthesis", "2M tokens", true, true, false),
            AiModelInfo("gemini-2.0-flash", "Gemini 2.0 Flash", AiProviderType.GEMINI, "Fast reliable multimodal conversational engine", "1M tokens", true, false, false),
            AiModelInfo("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite", AiProviderType.GEMINI, "Cost-efficient lightweight responses", "1M tokens", true, false, false),

            // OpenAI (Omni & Reasoning series)
            AiModelInfo("gpt-4o", "GPT-4o Omni 🧠", AiProviderType.OPENAI, "OpenAI flagship multimodal intelligence", "128k tokens", true, false, false),
            AiModelInfo("gpt-4o-mini", "GPT-4o Mini ⚡", AiProviderType.OPENAI, "Fast, lightweight daily conversational model", "128k tokens", true, false, false),
            AiModelInfo("o3-mini", "o3-mini (Reasoning) 🔬", AiProviderType.OPENAI, "State-of-the-art STEM, math & coding reasoning", "200k tokens", false, true, false),
            AiModelInfo("o1", "o1 Frontier 🧬", AiProviderType.OPENAI, "Full deliberate chain-of-thought problem solver", "200k tokens", true, true, false),

            // Anthropic Claude
            AiModelInfo("claude-3-7-sonnet-20250219", "Claude 3.7 Sonnet 🎭", AiProviderType.ANTHROPIC, "Hybrid fast reasoning and coding benchmark", "200k tokens", true, true, false),
            AiModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet 👑", AiProviderType.ANTHROPIC, "Industry-leading software architecture and coding", "200k tokens", true, false, false),
            AiModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku ⚡", AiProviderType.ANTHROPIC, "Near-instant responses for chat & editing", "200k tokens", false, false, false),

            // DeepSeek
            AiModelInfo("deepseek-chat", "DeepSeek V3 🚀", AiProviderType.DEEPSEEK, "671B MoE top-tier open conversational intelligence", "64k tokens", false, false, false),
            AiModelInfo("deepseek-reasoner", "DeepSeek R1 (Chain-of-Thought) 🔬", AiProviderType.DEEPSEEK, "World-class open mathematical proof & CoT", "64k tokens", false, true, false),

            // xAI Grok
            AiModelInfo("grok-2-latest", "Grok 2 Latest 🤖", AiProviderType.XAI, "Latest frontier reasoning by xAI", "128k tokens", true, false, false),
            AiModelInfo("grok-beta", "Grok Beta ⚡", AiProviderType.XAI, "High-speed reasoning channel", "128k tokens", true, false, false),

            // Mistral AI
            AiModelInfo("mistral-large-latest", "Mistral Large 🌐", AiProviderType.MISTRAL, "Top-tier European multilingual intelligence", "128k tokens", false, false, false),
            AiModelInfo("codestral-latest", "Codestral 25.01 💻", AiProviderType.MISTRAL, "Dedicated programming & refactoring model", "256k tokens", false, false, false),
            AiModelInfo("pixtral-large-latest", "Pixtral Large 👁️", AiProviderType.MISTRAL, "124B parameter frontier multimodal vision", "128k tokens", true, false, false),

            // Perplexity AI (Web-grounded search reasoning)
            AiModelInfo("sonar-reasoning", "Perplexity Sonar Reasoning 🔎", AiProviderType.PERPLEXITY, "Live web-grounded research with chain-of-thought", "128k tokens", false, true, false),
            AiModelInfo("sonar", "Perplexity Sonar 🌐", AiProviderType.PERPLEXITY, "Real-time cited factual web intelligence", "128k tokens", false, false, false),

            // OpenRouter (Universal Multi-Model Gateway)
            AiModelInfo("openrouter/auto", "OpenRouter Auto-Router 🔀", AiProviderType.OPENROUTER, "Dynamically routes to the best available model", "128k tokens", true, false, false),
            AiModelInfo("deepseek/deepseek-r1", "DeepSeek R1 via OpenRouter 🔬", AiProviderType.OPENROUTER, "OpenRouter hosted DeepSeek R1 reasoning", "64k tokens", false, true, false),
            AiModelInfo("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B Instruct 🦙", AiProviderType.OPENROUTER, "Meta flagship open-weights model", "128k tokens", false, false, false),
            AiModelInfo("qwen/qwen-2.5-72b-instruct", "Qwen 2.5 72B Instruct 🐉", AiProviderType.OPENROUTER, "Top multilingual, STEM & reasoning model", "128k tokens", false, false, false),
            AiModelInfo("qwen/qwen-2.5-coder-32b-instruct", "Qwen 2.5 Coder 32B 💻", AiProviderType.OPENROUTER, "Elite coding and bug-fixing specialist", "128k tokens", false, false, false),

            // Local Ollama & Qwen 3.8 Uncensored (from Wassimyounes01/qwen38-uncensored)
            AiModelInfo("orcarouter/Qwen3.8-27B-Uncensored:q4_k_m", "Qwen 3.8 27B Uncensored 🔓", AiProviderType.LOCAL_OLLAMA, "Abliterated 27B weights — zero refusal answers to any question", "262k tokens", true, true, true),
            AiModelInfo("qwen3.8:27b-uncensored", "Qwen 3.8 Uncensored (Ollama Tag) 🔓", AiProviderType.LOCAL_OLLAMA, "Abliterated Qwen 3.8 27B serving tag on Ollama", "8k tokens", false, true, false),
            AiModelInfo("llama3.3", "Local Ollama / LM Studio 🏠", AiProviderType.LOCAL_OLLAMA, "Zero-cloud private local LLM running on your device/LAN", "8k+ tokens", false, false, false)
        )
    }

    /**
     * Converts Room ChatMessage history into token-budgeted multi-turn conversation turns.
     * Prevents context explosion while preserving context across back-and-forth chats.
     */
    fun buildConversationTurns(
        systemPrompt: String,
        history: List<ChatMessage>?,
        currentPrompt: String,
        currentImageBase64: String? = null,
        maxPreviousTurns: Int = 12
    ): List<ChatTurn> {
        val turns = mutableListOf<ChatTurn>()

        // 1. System Prompt
        turns.add(ChatTurn(role = "system", content = systemPrompt))

        // 2. Recent History (take last N messages, excluding empty or system messages)
        if (!history.isNullOrEmpty()) {
            val relevantHistory = history
                .filter { it.content.isNotBlank() && (it.role == "user" || it.role == "assistant") }
                .takeLast(maxPreviousTurns)

            for (msg in relevantHistory) {
                val role = if (msg.role == "assistant") "assistant" else "user"
                turns.add(ChatTurn(role = role, content = msg.content))
            }
        }

        // 3. Current User Turn
        turns.add(ChatTurn(role = "user", content = currentPrompt, imageBase64 = currentImageBase64))
        return turns
    }

    /**
     * Executes synchronous / unary model inference with automatic provider fallback.
     */
    suspend fun generateChatResponse(
        prompt: String,
        systemPrompt: String,
        history: List<ChatMessage>? = null,
        imageBase64: String? = null,
        preferredProvider: AiProviderType? = null,
        preferredModelId: String? = null,
        withSearchGrounding: Boolean = true
    ): ProviderResponse = withContext(Dispatchers.IO) {
        val turns = buildConversationTurns(systemPrompt, history, prompt, imageBase64)
        val startTime = System.currentTimeMillis()

        // 0. Check LRU Cache for instant idempotent response
        val cacheKey = "$prompt:$systemPrompt"
        circuitBreaker.getCachedResponse(cacheKey)?.let { cached ->
            return@withContext ProviderResponse(
                text = cached,
                modelUsed = "Ayra Instant Memory Cache ⚡",
                provider = preferredProvider ?: getBestAvailableProvider(),
                latencyMs = 0L
            )
        }

        // 1. If explicit provider preferred and configured, try it first
        if (preferredProvider != null && preferredProvider != AiProviderType.KEYLESS_COMMUNITY) {
            val key = getProviderKey(preferredProvider)
            if ((key.isNotBlank() || preferredProvider == AiProviderType.LOCAL_OLLAMA) && circuitBreaker.canExecute(preferredProvider)) {
                try {
                    val model = preferredModelId ?: getDefaultModelForProvider(preferredProvider)
                    val resultText = callSpecificProvider(preferredProvider, model, turns, key, withSearchGrounding)
                    if (resultText.isNotBlank()) {
                        circuitBreaker.recordSuccess(preferredProvider)
                        circuitBreaker.putCachedResponse(cacheKey, resultText)
                        return@withContext ProviderResponse(
                            text = resultText,
                            modelUsed = "${preferredProvider.displayName} ($model)",
                            provider = preferredProvider,
                            latencyMs = System.currentTimeMillis() - startTime
                        )
                    }
                } catch (e: Exception) {
                    circuitBreaker.recordFailure(preferredProvider)
                    // Log and fall through to auto-router
                }
            }
        }

        // 2. Auto-Router: Identify best active provider with configured key
        val activeProviders = getActiveConfiguredProviders()
        for (provider in activeProviders) {
            if (!circuitBreaker.canExecute(provider)) continue
            val key = getProviderKey(provider)
            val model = getDefaultModelForProvider(provider)
            try {
                val resultText = callSpecificProvider(provider, model, turns, key, withSearchGrounding)
                if (resultText.isNotBlank()) {
                    circuitBreaker.recordSuccess(provider)
                    circuitBreaker.putCachedResponse(cacheKey, resultText)
                    return@withContext ProviderResponse(
                        text = resultText,
                        modelUsed = "${provider.displayName} ($model) ⚡",
                        provider = provider,
                        latencyMs = System.currentTimeMillis() - startTime
                    )
                }
            } catch (e: Exception) {
                circuitBreaker.recordFailure(provider)
                continue // Try next active provider in fallback chain
            }
        }

        // 3. Fallback: Keyless Community endpoint (clearly labelled)
        try {
            val fallbackText = queryPollinationsFallback(prompt, systemPrompt)
            if (fallbackText.isNotBlank()) {
                return@withContext ProviderResponse(
                    text = fallbackText,
                    modelUsed = "Keyless Free Community Model 🌐",
                    provider = AiProviderType.KEYLESS_COMMUNITY,
                    latencyMs = System.currentTimeMillis() - startTime
                )
            }
        } catch (_: Exception) {}

        throw ProviderError.NetworkFailure("All available AI model endpoints are currently unreachable. Please check network connectivity or add an API key in Settings.")
    }

    /**
     * Streams tokens in real-time via Kotlin Flow.
     */
    fun streamChatResponse(
        prompt: String,
        systemPrompt: String,
        history: List<ChatMessage>? = null,
        preferredProvider: AiProviderType? = null,
        preferredModelId: String? = null
    ): Flow<String> = flow {
        val turns = buildConversationTurns(systemPrompt, history, prompt)
        val provider = preferredProvider ?: getBestAvailableProvider()
        val key = getProviderKey(provider)
        val model = preferredModelId ?: getDefaultModelForProvider(provider)

        when (provider) {
            AiProviderType.GEMINI -> streamGemini(turns, model, key, this)
            AiProviderType.OPENAI, AiProviderType.DEEPSEEK, AiProviderType.XAI,
            AiProviderType.MISTRAL, AiProviderType.PERPLEXITY, AiProviderType.OPENROUTER, AiProviderType.LOCAL_OLLAMA -> {
                streamOpenAiCompatible(provider, turns, model, key, this)
            }
            AiProviderType.ANTHROPIC -> streamAnthropic(turns, model, key, this)
            else -> {
                // Keyless fallback doesn't support SSE; emit full text
                val fullText = queryPollinationsFallback(prompt, systemPrompt)
                emit(fullText)
            }
        }
    }.flowOn(Dispatchers.IO)

    // --- Provider Call Dispatchers ---

    private fun callSpecificProvider(
        provider: AiProviderType,
        model: String,
        turns: List<ChatTurn>,
        apiKey: String,
        withSearchGrounding: Boolean
    ): String {
        return when (provider) {
            AiProviderType.GEMINI -> callGemini(turns, model, apiKey, withSearchGrounding)
            AiProviderType.OPENAI, AiProviderType.DEEPSEEK, AiProviderType.XAI,
            AiProviderType.MISTRAL, AiProviderType.PERPLEXITY, AiProviderType.OPENROUTER, AiProviderType.LOCAL_OLLAMA -> {
                callOpenAiCompatible(provider, turns, model, apiKey)
            }
            AiProviderType.ANTHROPIC -> callAnthropic(turns, model, apiKey)
            AiProviderType.KEYLESS_COMMUNITY -> {
                val lastPrompt = turns.lastOrNull { it.role == "user" }?.content ?: ""
                val sys = turns.firstOrNull { it.role == "system" }?.content ?: ""
                queryPollinationsFallback(lastPrompt, sys)
            }
        }
    }

    // --- 1. Google Gemini Implementation ---

    private fun callGemini(turns: List<ChatTurn>, model: String, apiKey: String, withSearchGrounding: Boolean): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val requestJson = JSONObject().apply {
            val systemTurn = turns.firstOrNull { it.role == "system" }
            if (systemTurn != null && systemTurn.content.isNotBlank()) {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemTurn.content)))
                })
            }

            val contentsArray = JSONArray()
            val conversationTurns = turns.filter { it.role != "system" }

            for (turn in conversationTurns) {
                val geminiRole = if (turn.role == "assistant") "model" else "user"
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", turn.content))

                if (!turn.imageBase64.isNullOrBlank()) {
                    partsArray.put(JSONObject().apply {
                        put("inline_data", JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", turn.imageBase64)
                        })
                    })
                }

                contentsArray.put(JSONObject().apply {
                    put("role", geminiRole)
                    put("parts", partsArray)
                })
            }

            put("contents", contentsArray)

            if (withSearchGrounding) {
                put("tools", JSONArray().put(JSONObject().apply {
                    put("googleSearch", JSONObject())
                }))
            }
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("x-goog-api-key", apiKey)
        }

        val request = reqBuilder.build()

        return httpClient.newCall(request).execute().use { response ->
            if (response.code == 404 && model != "gemini-2.5-flash") {
                // Dynamic fallback from unlisted/retired model to latest stable flash model
                return callGemini(turns, "gemini-2.5-flash", apiKey, withSearchGrounding)
            }
            if (response.code == 429) throw ProviderError.RateLimited("Gemini quota exceeded.")
            if (response.code == 400 || response.code == 403) throw ProviderError.Unauthorized(AiProviderType.GEMINI)
            if (!response.isSuccessful) throw ProviderError.ServerError(response.code, response.message)

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return parts.getJSONObject(0).optString("text", "")
                }
            }
            ""
        }
    }

    private suspend fun streamGemini(turns: List<ChatTurn>, model: String, apiKey: String, collector: kotlinx.coroutines.flow.FlowCollector<String>) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse"
        val requestJson = JSONObject().apply {
            val systemTurn = turns.firstOrNull { it.role == "system" }
            if (systemTurn != null) {
                put("system_instruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemTurn.content)))
                })
            }
            val contentsArray = JSONArray()
            for (turn in turns.filter { it.role != "system" }) {
                contentsArray.put(JSONObject().apply {
                    put("role", if (turn.role == "assistant") "model" else "user")
                    put("parts", JSONArray().put(JSONObject().put("text", turn.content)))
                })
            }
            put("contents", contentsArray)
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("x-goog-api-key", apiKey)
        }

        val request = reqBuilder.build()

        httpClient.newCall(request).execute().use { response ->
            val reader = BufferedReader(InputStreamReader(response.body?.byteStream() ?: return, StandardCharsets.UTF_8))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.startsWith("data:")) {
                    val jsonStr = l.substring(5).trim()
                    if (jsonStr.isNotBlank()) {
                        try {
                            val json = JSONObject(jsonStr)
                            val candidates = json.optJSONArray("candidates")
                            if (candidates != null && candidates.length() > 0) {
                                val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                                if (parts != null && parts.length() > 0) {
                                    val token = parts.getJSONObject(0).optString("text", "")
                                    if (token.isNotEmpty()) collector.emit(token)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    // --- 2. OpenAI-Compatible Implementation (OpenAI, DeepSeek, xAI, Mistral, OpenRouter, Ollama) ---

    private fun getBaseUrlForProvider(provider: AiProviderType): String {
        return when (provider) {
            AiProviderType.OPENAI -> "https://api.openai.com/v1/"
            AiProviderType.DEEPSEEK -> "https://api.deepseek.com/"
            AiProviderType.XAI -> "https://api.x.ai/v1/"
            AiProviderType.MISTRAL -> "https://api.mistral.ai/v1/"
            AiProviderType.PERPLEXITY -> "https://api.perplexity.ai/"
            AiProviderType.OPENROUTER -> "https://openrouter.ai/api/v1/"
            AiProviderType.LOCAL_OLLAMA -> {
                val custom = context?.let { AyraPreferences.getCustomEndpointUrl(it) } ?: ""
                if (custom.isNotBlank()) {
                    if (custom.endsWith("/")) custom else "$custom/"
                } else {
                    val host = context?.let { AyraPreferences.getOllamaHost(it) } ?: "http://10.0.2.2:11434"
                    if (host.endsWith("/")) "${host}v1/" else "$host/v1/"
                }
            }
            else -> "https://api.openai.com/v1/"
        }
    }

    private fun callOpenAiCompatible(provider: AiProviderType, turns: List<ChatTurn>, model: String, apiKey: String): String {
        val baseUrl = getBaseUrlForProvider(provider)
        val endpoint = "${baseUrl}chat/completions"

        val messagesArray = JSONArray()
        for (turn in turns) {
            messagesArray.put(JSONObject().apply {
                put("role", turn.role)
                if (turn.imageBase64 != null && provider == AiProviderType.OPENAI) {
                    val contentParts = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", turn.content)
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,${turn.imageBase64}")
                            })
                        })
                    }
                    put("content", contentParts)
                } else {
                    put("content", turn.content)
                }
            })
        }

        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            if (model.contains("reasoner") || model.contains("o1") || model.contains("o3")) {
                // Reasoning models do not accept standard temperature
            } else {
                put("temperature", 0.7)
            }
        }

        val reqBuilder = Request.Builder()
            .url(endpoint)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

        if (apiKey.isNotBlank()) {
            reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        }
        if (provider == AiProviderType.OPENROUTER) {
            reqBuilder.addHeader("HTTP-Referer", "https://ayra.ai")
            reqBuilder.addHeader("X-Title", "Ayra AI Platform")
        }

        return httpClient.newCall(reqBuilder.build()).execute().use { response ->
            if (response.code == 429) throw ProviderError.RateLimited("${provider.displayName} rate limit reached.")
            if (response.code == 401 || response.code == 403) throw ProviderError.Unauthorized(provider)
            if (!response.isSuccessful) throw ProviderError.ServerError(response.code, response.message)

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val message = choices.getJSONObject(0).optJSONObject("message")
                return message?.optString("content", "") ?: ""
            }
            ""
        }
    }

    private suspend fun streamOpenAiCompatible(
        provider: AiProviderType,
        turns: List<ChatTurn>,
        model: String,
        apiKey: String,
        collector: kotlinx.coroutines.flow.FlowCollector<String>
    ) {
        val baseUrl = getBaseUrlForProvider(provider)
        val endpoint = "${baseUrl}chat/completions"

        val messagesArray = JSONArray()
        for (turn in turns) {
            messagesArray.put(JSONObject().apply {
                put("role", turn.role)
                put("content", turn.content)
            })
        }

        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            put("stream", true)
        }

        val reqBuilder = Request.Builder()
            .url(endpoint)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

        if (apiKey.isNotBlank()) reqBuilder.addHeader("Authorization", "Bearer $apiKey")
        if (provider == AiProviderType.OPENROUTER) {
            reqBuilder.addHeader("HTTP-Referer", "https://ayra.ai")
            reqBuilder.addHeader("X-Title", "Ayra AI Platform")
        }

        httpClient.newCall(reqBuilder.build()).execute().use { response ->
            val reader = BufferedReader(InputStreamReader(response.body?.byteStream() ?: return, StandardCharsets.UTF_8))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.startsWith("data:")) {
                    val data = l.substring(5).trim()
                    if (data == "[DONE]") break
                    if (data.isNotBlank()) {
                        try {
                            val json = JSONObject(data)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                val text = delta?.optString("content", "") ?: ""
                                if (text.isNotEmpty()) collector.emit(text)
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    // --- 3. Anthropic Claude Implementation ---

    private fun callAnthropic(turns: List<ChatTurn>, model: String, apiKey: String): String {
        val url = "https://api.anthropic.com/v1/messages"
        val systemTurn = turns.firstOrNull { it.role == "system" }

        val messagesArray = JSONArray()
        for (turn in turns.filter { it.role != "system" }) {
            messagesArray.put(JSONObject().apply {
                put("role", if (turn.role == "assistant") "assistant" else "user")
                if (turn.imageBase64 != null) {
                    val contentArr = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "image")
                            put("source", JSONObject().apply {
                                put("type", "base64")
                                put("media_type", "image/jpeg")
                                put("data", turn.imageBase64)
                            })
                        })
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", turn.content)
                        })
                    }
                    put("content", contentArr)
                } else {
                    put("content", turn.content)
                }
            })
        }

        val requestJson = JSONObject().apply {
            put("model", model)
            put("max_tokens", 4096)
            if (systemTurn != null) put("system", systemTurn.content)
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return httpClient.newCall(request).execute().use { response ->
            if (response.code == 429) throw ProviderError.RateLimited("Anthropic Claude rate limit reached.")
            if (response.code == 401 || response.code == 403) throw ProviderError.Unauthorized(AiProviderType.ANTHROPIC)
            if (!response.isSuccessful) throw ProviderError.ServerError(response.code, response.message)

            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val contentArr = json.optJSONArray("content")
            if (contentArr != null && contentArr.length() > 0) {
                return contentArr.getJSONObject(0).optString("text", "")
            }
            ""
        }
    }

    private suspend fun streamAnthropic(
        turns: List<ChatTurn>,
        model: String,
        apiKey: String,
        collector: kotlinx.coroutines.flow.FlowCollector<String>
    ) {
        val url = "https://api.anthropic.com/v1/messages"
        val systemTurn = turns.firstOrNull { it.role == "system" }

        val messagesArray = JSONArray()
        for (turn in turns.filter { it.role != "system" }) {
            messagesArray.put(JSONObject().apply {
                put("role", if (turn.role == "assistant") "assistant" else "user")
                put("content", turn.content)
            })
        }

        val requestJson = JSONObject().apply {
            put("model", model)
            put("max_tokens", 4096)
            put("stream", true)
            if (systemTurn != null) put("system", systemTurn.content)
            put("messages", messagesArray)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val reader = BufferedReader(InputStreamReader(response.body?.byteStream() ?: return, StandardCharsets.UTF_8))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.startsWith("data:")) {
                    val data = l.substring(5).trim()
                    try {
                        val json = JSONObject(data)
                        if (json.optString("type") == "content_block_delta") {
                            val delta = json.optJSONObject("delta")
                            val text = delta?.optString("text", "") ?: ""
                            if (text.isNotEmpty()) collector.emit(text)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // --- 4. Free Keyless Fallback (Pollinations with honest attribution) ---

    private fun queryPollinationsFallback(prompt: String, systemPrompt: String): String {
        val models = listOf("openai", "gemini", "claude")
        for (m in models) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("messages", JSONArray().apply {
                        if (systemPrompt.isNotBlank()) {
                            put(JSONObject().apply {
                                put("role", "system")
                                put("content", systemPrompt)
                            })
                        }
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("model", m)
                }

                val req = Request.Builder()
                    .url("https://text.pollinations.ai/")
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .header("User-Agent", "AyraAiEnterprise/3.0")
                    .build()

                val result = httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val text = resp.body?.string()?.trim() ?: ""
                        if (text.length >= 10 && !text.contains("error code", ignoreCase = true)) text else ""
                    } else ""
                }
                if (result.isNotBlank()) return result
            } catch (_: Exception) {}
        }
        return ""
    }

    // --- Provider Helpers & Status Checkers ---

    fun getProviderKey(provider: AiProviderType): String {
        val ctx = context ?: return ""
        return when (provider) {
            AiProviderType.GEMINI -> AyraPreferences.getEffectiveGeminiKey(ctx)
            AiProviderType.OPENAI -> AyraPreferences.getOpenAiKey(ctx)
            AiProviderType.ANTHROPIC -> AyraPreferences.getAnthropicKey(ctx)
            AiProviderType.DEEPSEEK -> AyraPreferences.getDeepSeekKey(ctx)
            AiProviderType.XAI -> AyraPreferences.getXAiKey(ctx)
            AiProviderType.MISTRAL -> AyraPreferences.getMistralKey(ctx)
            AiProviderType.PERPLEXITY -> AyraPreferences.getPerplexityKey(ctx)
            AiProviderType.OPENROUTER -> AyraPreferences.getEffectiveOpenRouterKey(ctx)
            AiProviderType.LOCAL_OLLAMA -> {
                val url = AyraPreferences.getCustomEndpointUrl(ctx)
                if (url.isNotBlank()) url else AyraPreferences.getCustomEndpointKey(ctx)
            }
            AiProviderType.KEYLESS_COMMUNITY -> ""
        }
    }

    fun isProviderConfigured(provider: AiProviderType): Boolean {
        if (provider == AiProviderType.KEYLESS_COMMUNITY) return true
        if (provider == AiProviderType.LOCAL_OLLAMA) return context?.let { AyraPreferences.getCustomEndpointUrl(it).isNotBlank() } ?: false
        return getProviderKey(provider).isNotBlank()
    }

    fun getActiveConfiguredProviders(): List<AiProviderType> {
        val list = mutableListOf<AiProviderType>()
        // Prioritize Gemini if key exists
        if (isProviderConfigured(AiProviderType.GEMINI)) list.add(AiProviderType.GEMINI)
        if (isProviderConfigured(AiProviderType.OPENAI)) list.add(AiProviderType.OPENAI)
        if (isProviderConfigured(AiProviderType.ANTHROPIC)) list.add(AiProviderType.ANTHROPIC)
        if (isProviderConfigured(AiProviderType.DEEPSEEK)) list.add(AiProviderType.DEEPSEEK)
        if (isProviderConfigured(AiProviderType.PERPLEXITY)) list.add(AiProviderType.PERPLEXITY)
        if (isProviderConfigured(AiProviderType.XAI)) list.add(AiProviderType.XAI)
        if (isProviderConfigured(AiProviderType.MISTRAL)) list.add(AiProviderType.MISTRAL)
        if (isProviderConfigured(AiProviderType.OPENROUTER)) list.add(AiProviderType.OPENROUTER)
        if (isProviderConfigured(AiProviderType.LOCAL_OLLAMA)) list.add(AiProviderType.LOCAL_OLLAMA)
        return list
    }

    fun hasAnyConfiguredProvider(): Boolean = getActiveConfiguredProviders().isNotEmpty()

    fun getBestAvailableProvider(): AiProviderType {
        return getActiveConfiguredProviders().firstOrNull() ?: AiProviderType.KEYLESS_COMMUNITY
    }

    fun getDefaultModelForProvider(provider: AiProviderType): String {
        return when (provider) {
            AiProviderType.GEMINI -> "gemini-2.5-flash"
            AiProviderType.OPENAI -> "gpt-4o"
            AiProviderType.ANTHROPIC -> "claude-3-7-sonnet-20250219"
            AiProviderType.DEEPSEEK -> "deepseek-chat"
            AiProviderType.XAI -> "grok-2-latest"
            AiProviderType.MISTRAL -> "mistral-large-latest"
            AiProviderType.PERPLEXITY -> "sonar-reasoning"
            AiProviderType.OPENROUTER -> "openrouter/auto"
            AiProviderType.LOCAL_OLLAMA -> "llama3.3"
            AiProviderType.KEYLESS_COMMUNITY -> "openai"
        }
    }

    /**
     * One-tap connection test for any provider.
     * Returns latency in milliseconds, or throws [ProviderError].
     */
    suspend fun testProviderConnection(provider: AiProviderType, customKey: String? = null): Long = withContext(Dispatchers.IO) {
        val key = customKey ?: getProviderKey(provider)
        if (key.isBlank() && provider != AiProviderType.LOCAL_OLLAMA && provider != AiProviderType.KEYLESS_COMMUNITY) {
            throw ProviderError.NoKeyConfigured(provider)
        }

        val start = System.currentTimeMillis()
        val testTurns = listOf(
            ChatTurn("user", "Hello! Return the single word 'READY'.")
        )

        val model = getDefaultModelForProvider(provider)
        val response = callSpecificProvider(provider, model, testTurns, key, withSearchGrounding = false)
        if (response.isBlank()) throw ProviderError.NetworkFailure("Empty response received from ${provider.displayName}")
        System.currentTimeMillis() - start
    }

    /**
     * Intelligent Router (Part 3): Selects optimal provider and model based on user routing mode.
     */
    fun getOptimalModelForRoutingMode(mode: AiRoutingMode): Pair<AiProviderType, String> {
        val configured = getActiveConfiguredProviders()
        return when (mode) {
            AiRoutingMode.FAST -> {
                if (configured.contains(AiProviderType.GEMINI)) Pair(AiProviderType.GEMINI, "gemini-2.5-flash")
                else if (configured.contains(AiProviderType.OPENAI)) Pair(AiProviderType.OPENAI, "gpt-4o-mini")
                else if (configured.contains(AiProviderType.DEEPSEEK)) Pair(AiProviderType.DEEPSEEK, "deepseek-chat")
                else Pair(AiProviderType.KEYLESS_COMMUNITY, "openai")
            }
            AiRoutingMode.BEST_QUALITY -> {
                if (configured.contains(AiProviderType.GEMINI)) Pair(AiProviderType.GEMINI, "gemini-2.5-pro")
                else if (configured.contains(AiProviderType.ANTHROPIC)) Pair(AiProviderType.ANTHROPIC, "claude-3-7-sonnet-20250219")
                else if (configured.contains(AiProviderType.OPENAI)) Pair(AiProviderType.OPENAI, "gpt-4o")
                else if (configured.contains(AiProviderType.DEEPSEEK)) Pair(AiProviderType.DEEPSEEK, "deepseek-reasoner")
                else Pair(AiProviderType.GEMINI, "gemini-2.5-flash")
            }
            AiRoutingMode.REASONING -> {
                if (configured.contains(AiProviderType.DEEPSEEK)) Pair(AiProviderType.DEEPSEEK, "deepseek-reasoner")
                else if (configured.contains(AiProviderType.OPENAI)) Pair(AiProviderType.OPENAI, "o3-mini")
                else if (configured.contains(AiProviderType.GEMINI)) Pair(AiProviderType.GEMINI, "gemini-2.5-pro")
                else if (configured.contains(AiProviderType.PERPLEXITY)) Pair(AiProviderType.PERPLEXITY, "sonar-reasoning")
                else Pair(AiProviderType.OPENROUTER, "deepseek/deepseek-r1")
            }
            AiRoutingMode.CODING -> {
                if (configured.contains(AiProviderType.ANTHROPIC)) Pair(AiProviderType.ANTHROPIC, "claude-3-7-sonnet-20250219")
                else if (configured.contains(AiProviderType.MISTRAL)) Pair(AiProviderType.MISTRAL, "codestral-latest")
                else if (configured.contains(AiProviderType.GEMINI)) Pair(AiProviderType.GEMINI, "gemini-2.5-pro")
                else if (configured.contains(AiProviderType.OPENAI)) Pair(AiProviderType.OPENAI, "gpt-4o")
                else Pair(AiProviderType.OPENROUTER, "qwen/qwen-2.5-coder-32b-instruct")
            }
            AiRoutingMode.PRIVACY_LOCAL -> {
                Pair(AiProviderType.LOCAL_OLLAMA, "llama3.3")
            }
            AiRoutingMode.COST_EFFICIENT -> {
                if (configured.contains(AiProviderType.GEMINI)) Pair(AiProviderType.GEMINI, "gemini-2.5-flash")
                else if (configured.contains(AiProviderType.OPENAI)) Pair(AiProviderType.OPENAI, "gpt-4o-mini")
                else Pair(AiProviderType.KEYLESS_COMMUNITY, "openai")
            }
            AiRoutingMode.QWEN_UNCENSORED -> {
                if (configured.contains(AiProviderType.LOCAL_OLLAMA)) Pair(AiProviderType.LOCAL_OLLAMA, "orcarouter/Qwen3.8-27B-Uncensored:q4_k_m")
                else if (configured.contains(AiProviderType.OPENROUTER)) Pair(AiProviderType.OPENROUTER, "qwen/qwen-2.5-72b-instruct")
                else Pair(AiProviderType.LOCAL_OLLAMA, "qwen3.8:27b-uncensored")
            }
            AiRoutingMode.AUTO -> {
                val best = getBestAvailableProvider()
                Pair(best, getDefaultModelForProvider(best))
            }
        }
    }

    /**
     * Dynamic Model Discovery (Part 3):
     * Dynamically queries live provider catalogues, detects vision/reasoning/coding capabilities,
     * and caches results. Falls back to verified curated models if offline or unauthenticated.
     */
    suspend fun discoverDynamicModels(provider: AiProviderType): List<AiModelInfo> = withContext(Dispatchers.IO) {
        dynamicModelCache[provider]?.let { if (it.isNotEmpty()) return@withContext it }
        val key = getProviderKey(provider)
        val discovered = mutableListOf<AiModelInfo>()
        try {
            when (provider) {
                AiProviderType.GEMINI -> {
                    if (key.isNotBlank()) {
                        val url = "https://generativelanguage.googleapis.com/v1beta/models"
                        val req = Request.Builder().url(url).header("x-goog-api-key", key).get().build()
                        httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val root = JSONObject(resp.body?.string() ?: "")
                                val arr = root.optJSONArray("models") ?: JSONArray()
                                for (i in 0 until arr.length()) {
                                    val obj = arr.getJSONObject(i)
                                    val name = obj.optString("name", "").removePrefix("models/")
                                    val displayName = obj.optString("displayName", name)
                                    val desc = obj.optString("description", "")
                                    val inputLimit = obj.optInt("inputTokenLimit", 0)
                                    if (name.contains("gemini")) {
                                        discovered.add(
                                            AiModelInfo(
                                                id = name,
                                                displayName = displayName,
                                                provider = AiProviderType.GEMINI,
                                                description = desc.ifBlank { "Google Gemini official model" },
                                                contextWindow = if (inputLimit > 0) "${inputLimit / 1000}k tokens" else "1M tokens",
                                                supportsVision = true,
                                                isReasoning = name.contains("pro") || name.contains("thinking"),
                                                isDefault = name == "gemini-2.5-flash"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                AiProviderType.OPENAI, AiProviderType.DEEPSEEK, AiProviderType.XAI,
                AiProviderType.MISTRAL, AiProviderType.PERPLEXITY, AiProviderType.OPENROUTER, AiProviderType.LOCAL_OLLAMA -> {
                    val baseUrl = getBaseUrlForProvider(provider)
                    val url = if (provider == AiProviderType.LOCAL_OLLAMA && !baseUrl.contains("/v1/")) "${baseUrl}api/tags" else "${baseUrl}models"
                    val reqBuilder = Request.Builder().url(url)
                    if (key.isNotBlank()) {
                        reqBuilder.addHeader("Authorization", "Bearer $key")
                    }
                    val req = reqBuilder.get().build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val root = JSONObject(resp.body?.string() ?: "")
                            val data = root.optJSONArray("data") ?: root.optJSONArray("models") ?: JSONArray()
                            for (i in 0 until data.length()) {
                                val obj = data.getJSONObject(i)
                                val modelId = obj.optString("id", obj.optString("name", ""))
                                if (modelId.isNotBlank()) {
                                    val isVision = modelId.contains("vision") || modelId.contains("4o") || modelId.contains("pixtral")
                                    val isReasoning = modelId.contains("r1") || modelId.contains("o1") || modelId.contains("o3") || modelId.contains("reasoner") || modelId.contains("sonar-reasoning")
                                    discovered.add(
                                        AiModelInfo(
                                            id = modelId,
                                            displayName = modelId,
                                            provider = provider,
                                            description = "${provider.displayName} dynamically discovered model",
                                            contextWindow = "Adaptive",
                                            supportsVision = isVision,
                                            isReasoning = isReasoning,
                                            isDefault = false
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                AiProviderType.ANTHROPIC -> {
                    if (key.isNotBlank()) {
                        val req = Request.Builder()
                            .url("https://api.anthropic.com/v1/models")
                            .header("x-api-key", key)
                            .header("anthropic-version", "2023-06-01")
                            .get()
                            .build()
                        httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                val root = JSONObject(resp.body?.string() ?: "")
                                val data = root.optJSONArray("data") ?: JSONArray()
                                for (i in 0 until data.length()) {
                                    val obj = data.getJSONObject(i)
                                    val modelId = obj.optString("id", "")
                                    val displayName = obj.optString("display_name", modelId)
                                    if (modelId.isNotBlank()) {
                                        discovered.add(
                                            AiModelInfo(
                                                id = modelId,
                                                displayName = displayName,
                                                provider = AiProviderType.ANTHROPIC,
                                                description = "Anthropic Claude dynamic model",
                                                contextWindow = "200k tokens",
                                                supportsVision = true,
                                                isReasoning = modelId.contains("3-7") || modelId.contains("sonnet"),
                                                isDefault = modelId.contains("3-7-sonnet")
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        } catch (_: Exception) {}

        if (discovered.isNotEmpty()) {
            dynamicModelCache[provider] = discovered
            discovered
        } else {
            CURATED_MODELS.filter { it.provider == provider }
        }
    }
}
