package com.ayra.ai.app.ai.deepseek

import android.content.Context
import com.ayra.ai.app.ai.AiResponse
import com.ayra.ai.app.ai.providers.AiProviderType
import com.ayra.ai.app.ai.providers.ExternalAiProviders
import com.ayra.ai.app.data.AyraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeepSeekHarnessResponse(
    val reasoningChain: String,
    val finalAnswer: String,
    val modelUsed: String
)

/**
 * DeepSeek-R1 Harness Engine.
 * Formats multi-step chain-of-thought prompts, parses <think> blocks,
 * and extracts validated reasoning chains for deep mathematics, logic, and code.
 */
class DeepSeekHarnessEngine(private val context: Context? = null) {

    private val externalProviders = ExternalAiProviders(context)

    suspend fun executeHarness(prompt: String): AiResponse = withContext(Dispatchers.IO) {
        val parsed = queryDeepSeek(prompt)
        AiResponse(
            text = if (parsed.reasoningChain.isNotBlank()) {
                "🧠 **DeepSeek-R1 Chain-of-Thought Reasoning**:\n> ${parsed.reasoningChain.replace("\n", "\n> ")}\n\n${parsed.finalAnswer}"
            } else {
                parsed.finalAnswer
            },
            modelUsed = parsed.modelUsed
        )
    }

    suspend fun queryDeepSeek(prompt: String): DeepSeekHarnessResponse = withContext(Dispatchers.IO) {
        val systemPrompt = "You are DeepSeek-R1. Use rigorous step-by-step chain-of-thought reasoning before arriving at conclusions. Wrap internal thinking in <think>...</think> tags, followed by your final answer in the user's language."

        val resp = externalProviders.generateChatResponse(
            prompt = prompt,
            systemPrompt = systemPrompt,
            preferredProvider = AiProviderType.DEEPSEEK,
            preferredModelId = "deepseek-reasoner",
            withSearchGrounding = false
        )

        val rawText = resp.text
        val thinkRegex = Regex("<think>([\\s\\S]*?)</think>", RegexOption.IGNORE_CASE)
        val match = thinkRegex.find(rawText)

        val reasoning = match?.groupValues?.get(1)?.trim() ?: ""
        val answer = rawText.replace(thinkRegex, "").trim()

        DeepSeekHarnessResponse(
            reasoningChain = reasoning,
            finalAnswer = if (answer.isNotBlank()) answer else rawText,
            modelUsed = "DeepSeek-R1 Chain-of-Thought 🔬"
        )
    }
}
