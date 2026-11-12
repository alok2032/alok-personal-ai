package com.ayra.ai.app.ai.omniroute

import android.content.Context
import com.ayra.ai.app.ai.AiResponse
import com.ayra.ai.app.ai.providers.AiProviderType
import com.ayra.ai.app.ai.providers.ExternalAiProviders
import com.ayra.ai.app.data.AyraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class RouteDecision(
    val selectedEngine: String,
    val rationale: String,
    val candidateModels: List<String>
)

/**
 * OmniRoute Multi-Architecture Ensemble Engine.
 * Intelligently fans out inference across all active AI models in parallel,
 * compares candidate drafts, and synthesizes the best consensus answer.
 */
class OmniRouteEngine(private val context: Context? = null) {

    private val externalProviders = ExternalAiProviders(context)

    fun determineRoute(prompt: String, selectedTier: String): RouteDecision {
        val lower = prompt.lowercase()
        return when {
            lower.contains("vulnerability") || lower.contains("exploit") || lower.contains("reverse engineer") -> {
                RouteDecision("G0DM0D3", "Cybersecurity and system-level vulnerability analysis requested", listOf("godmode", "deepseek_r1", "gemini_pro"))
            }
            lower.contains("video") || lower.contains("camera") || lower.contains("cinematic") || lower.contains("4k") -> {
                RouteDecision("HIGGSFIELD", "Visual camera motion physics and video craft required", listOf("higgsfield", "flux", "gemini_flash"))
            }
            lower.contains("reasoning") || lower.contains("proof") || lower.contains("calculate") || lower.contains("step by step") -> {
                RouteDecision("DEEPSEEK_HARNESS", "Complex deductive reasoning or mathematical proof required", listOf("deepseek_r1", "gemini_pro", "claude_sonnet"))
            }
            lower.contains("disambiguate") || lower.contains("context") || lower.contains("enterprise") -> {
                RouteDecision("ECC", "Enterprise cognitive entity disambiguation and context memory required", listOf("ecc", "gemini_pro"))
            }
            lower.contains("uncensored") || lower.contains("unrestricted") -> {
                RouteDecision("QWEN_UNCENSORED", "Unrestricted global knowledge exploration requested", listOf("qwen_uncensored", "deepseek_r1"))
            }
            else -> {
                RouteDecision("OMNI_ENSEMBLE", "Multi-model consensus synergy across active providers", listOf("gemini_flash", "gemini_pro", "openrouter"))
            }
        }
    }

    suspend fun executeRoute(
        prompt: String,
        selectedTier: String = "auto",
        attachedImageBase64: String? = null,
        attachedDocumentText: String? = null
    ): AiResponse = withContext(Dispatchers.IO) {
        val configured = externalProviders.getActiveConfiguredProviders()

        // 1. If multiple providers are configured, execute parallel multi-model consensus
        if (configured.size >= 2 || selectedTier == "omniroute") {
            val ensembleResp = executeMultiModelEnsemble(prompt, configured, attachedImageBase64, attachedDocumentText)
            if (ensembleResp != null && ensembleResp.text.isNotBlank()) {
                return@withContext ensembleResp
            }
        }

        // 2. Fall back to best available single provider
        try {
            val providerResp = externalProviders.generateChatResponse(
                prompt = prompt,
                systemPrompt = "You are Ayra AI OmniRoute. Provide an accurate, comprehensive, and well-structured answer in the user's language.",
                imageBase64 = attachedImageBase64,
                withSearchGrounding = true
            )
            if (providerResp.text.isNotBlank()) {
                return@withContext AiResponse(
                    text = providerResp.text,
                    modelUsed = "OmniRoute Adaptive (${providerResp.modelUsed}) ⚡"
                )
            }
        } catch (_: Exception) {}

        AiResponse(text = "", modelUsed = "OmniRoute")
    }

    private suspend fun executeMultiModelEnsemble(
        prompt: String,
        providers: List<AiProviderType>,
        attachedImageBase64: String?,
        attachedDocumentText: String?
    ): AiResponse? = coroutineScope {
        val targetProviders = providers.take(3)
        val drafts = targetProviders.map { provider ->
            async {
                withTimeoutOrNull(20_000L) {
                    try {
                        val key = externalProviders.getProviderKey(provider)
                        if (key.isBlank()) return@withTimeoutOrNull null
                        val model = externalProviders.getDefaultModelForProvider(provider)
                        val turns = externalProviders.buildConversationTurns(
                            systemPrompt = "You are an expert AI model contributing to an ensemble answer. Answer directly, thoroughly, and accurately in the user's language.",
                            history = null,
                            currentPrompt = prompt,
                            currentImageBase64 = attachedImageBase64
                        )
                        val text = externalProviders.callSpecificProviderPublic(provider, model, turns, key, withSearchGrounding = true)
                        if (text.isNotBlank()) Pair(provider, text) else null
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }.mapNotNull { it.await() }

        if (drafts.isEmpty()) return@coroutineScope null

        if (drafts.size == 1) {
            val single = drafts.first()
            return@coroutineScope AiResponse(
                text = single.second,
                modelUsed = "OmniRoute Ensemble (${single.first.displayName}) ⚡👑"
            )
        }

        // Merge drafts into synthesized consensus answer
        val primary = drafts.first()
        val synthKey = externalProviders.getProviderKey(primary.first)
        val synthModel = externalProviders.getDefaultModelForProvider(primary.first)

        val synthesisPrompt = buildString {
            append("User Query: \"$prompt\"\n\n")
            append("Multiple AI models provided the following answers. Combine them into one coherent, highly accurate, and comprehensive master response in the user's language (Bengali, Banglish, or English). Resolve contradictions, preserve all valid details, code, and explanations:\n\n")
            drafts.forEachIndexed { i, draft ->
                append("=== DRAFT ${i + 1} (${draft.first.displayName}) ===\n")
                append(draft.second)
                append("\n\n")
            }
        }

        try {
            val synthTurns = listOf(com.ayra.ai.app.ai.providers.ChatTurn("user", synthesisPrompt))
            val synthesized = externalProviders.callSpecificProviderPublic(primary.first, synthModel, synthTurns, synthKey, withSearchGrounding = false)
            if (synthesized.isNotBlank()) {
                val contributing = drafts.joinToString(" + ") { it.first.displayName }
                return@coroutineScope AiResponse(
                    text = synthesized,
                    modelUsed = "OmniRoute Ensemble ($contributing) ⚡👑"
                )
            }
        } catch (_: Exception) {}

        // Fallback to highest length draft
        val bestDraft = drafts.maxByOrNull { it.second.length } ?: drafts.first()
        AiResponse(
            text = bestDraft.second,
            modelUsed = "OmniRoute Ensemble (${bestDraft.first.displayName}) ⚡👑"
        )
    }
}
