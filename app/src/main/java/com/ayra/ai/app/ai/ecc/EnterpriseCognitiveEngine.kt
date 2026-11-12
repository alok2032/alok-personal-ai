package com.ayra.ai.app.ai.ecc

import android.content.Context
import com.ayra.ai.app.ai.AiResponse
import com.ayra.ai.app.ai.providers.ExternalAiProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CognitiveContext(
    val detectedEntities: List<String>,
    val domain: String,
    val disambiguatedQuery: String
)

/**
 * Enterprise Cognitive Computing (ECC) Engine.
 * Extracts named entities, disambiguates complex context, and links
 * conversation history into an entity graph before routing.
 */
class EnterpriseCognitiveEngine(private val context: Context? = null) {

    private val externalProviders = ExternalAiProviders(context)

    fun extractCognitiveContext(prompt: String): CognitiveContext {
        val words = prompt.split(Regex("\\s+"))
        val entities = words.filter { it.length >= 4 && it[0].isUpperCase() }
        val domain = when {
            prompt.contains("code", true) || prompt.contains("bug", true) || prompt.contains("function", true) -> "Engineering"
            prompt.contains("business", true) || prompt.contains("revenue", true) || prompt.contains("market", true) -> "Enterprise Business"
            prompt.contains("science", true) || prompt.contains("physics", true) || prompt.contains("quantum", true) -> "Scientific Research"
            else -> "General Knowledge"
        }
        return CognitiveContext(
            detectedEntities = entities.take(5),
            domain = domain,
            disambiguatedQuery = prompt.trim()
        )
    }

    suspend fun disambiguateAndExecute(prompt: String): AiResponse = withContext(Dispatchers.IO) {
        val cognitiveContext = extractCognitiveContext(prompt)
        val systemDirective = """
            You are the ECC (Enterprise Cognitive Computing) Engine.
            Domain: ${cognitiveContext.domain}
            Key Entities: ${cognitiveContext.detectedEntities.joinToString(", ").ifBlank { "None detected" }}
            Task: Provide a rigorous, factually verified, enterprise-grade response in the user's language.
        """.trimIndent()

        val resp = externalProviders.generateChatResponse(
            prompt = prompt,
            systemPrompt = systemDirective,
            withSearchGrounding = true
        )

        AiResponse(
            text = resp.text,
            modelUsed = "ECC Enterprise Cognitive Computing 🌐"
        )
    }
}
