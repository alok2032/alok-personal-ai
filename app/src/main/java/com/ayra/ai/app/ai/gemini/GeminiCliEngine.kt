package com.ayra.ai.app.ai.gemini

import android.content.Context
import com.ayra.ai.app.ai.AiResponse
import com.ayra.ai.app.ai.providers.AiProviderType
import com.ayra.ai.app.ai.providers.ExternalAiProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Direct Gemini CLI & REST Engine.
 * Executes direct Gemini API commands with system directives, web grounding,
 * and multimodal image attachments.
 */
class GeminiCliEngine(private val context: Context? = null) {

    private val externalProviders = ExternalAiProviders(context)

    suspend fun executeCliQuery(
        prompt: String,
        systemDirective: String? = null,
        imageBase64: String? = null
    ): AiResponse = withContext(Dispatchers.IO) {
        val sys = systemDirective ?: "You are Gemini CLI Engine. Provide direct, highly accurate, and helpful answers in the user's language."

        val resp = externalProviders.generateChatResponse(
            prompt = prompt,
            systemPrompt = sys,
            imageBase64 = imageBase64,
            preferredProvider = AiProviderType.GEMINI,
            preferredModelId = "gemini-2.0-flash",
            withSearchGrounding = true
        )

        AiResponse(
            text = resp.text,
            modelUsed = "Gemini 2.5 Flash (Direct CLI) ⚡"
        )
    }
}
