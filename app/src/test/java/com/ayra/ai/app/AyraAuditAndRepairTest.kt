package com.ayra.ai.app

import com.ayra.ai.app.ai.agents.AgentDomain
import com.ayra.ai.app.ai.agents.AyraAgentOrchestrator
import com.ayra.ai.app.ai.providers.AiProviderType
import com.ayra.ai.app.ai.providers.ExternalAiProviders
import com.ayra.ai.app.ai.providers.ProviderCircuitBreaker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rigorous Unit Verification for Ayra AI Audit & Full Functionality Repair:
 * - Gemini HTTP 520 / 404 Prevention: Dynamic Model Resolution mapping
 * - Banglish & Bilingual Natural Language Routing
 * - Provider Circuit Breaker recovery and per-provider reset
 */
class AyraAuditAndRepairTest {

    @Test
    fun testResolveGeminiModelResolution() {
        // Flash variants must resolve to stable gemini-2.0-flash to prevent HTTP 520 / 404
        assertEquals("gemini-2.0-flash", ExternalAiProviders.resolveGeminiModel("gemini-2.5-flash"))
        assertEquals("gemini-2.0-flash", ExternalAiProviders.resolveGeminiModel("gemini_flash"))
        assertEquals("gemini-2.0-flash", ExternalAiProviders.resolveGeminiModel("gemini-flash"))
        assertEquals("gemini-2.0-flash", ExternalAiProviders.resolveGeminiModel("gemini-2.0-flash"))
        assertEquals("gemini-2.0-flash", ExternalAiProviders.resolveGeminiModel("models/gemini-2.0-flash"))

        // Pro variants must resolve to stable gemini-1.5-pro
        assertEquals("gemini-1.5-pro", ExternalAiProviders.resolveGeminiModel("gemini-2.5-pro"))
        assertEquals("gemini-1.5-pro", ExternalAiProviders.resolveGeminiModel("gemini_pro"))
        assertEquals("gemini-1.5-pro", ExternalAiProviders.resolveGeminiModel("gemini-pro"))
        assertEquals("gemini-1.5-pro", ExternalAiProviders.resolveGeminiModel("gemini-1.5-pro"))

        // Flash-Lite and 1.5-Flash
        assertEquals("gemini-2.0-flash-lite", ExternalAiProviders.resolveGeminiModel("gemini-2.0-flash-lite"))
        assertEquals("gemini-1.5-flash", ExternalAiProviders.resolveGeminiModel("gemini-1.5-flash"))

        // Blank falls back to gemini-2.0-flash
        assertEquals("gemini-2.0-flash", ExternalAiProviders.resolveGeminiModel(""))
    }

    @Test
    fun testBanglishDetection() {
        // Banglish expressions (Bengali in Latin characters)
        assertTrue(AyraAgentOrchestrator.isBanglish("kemon acho tumi?"))
        assertTrue(AyraAgentOrchestrator.isBanglish("ki khobor bondhu?"))
        assertTrue(AyraAgentOrchestrator.isBanglish("amake ekta website banao"))
        assertTrue(AyraAgentOrchestrator.isBanglish("kivabe ami android app toiri korbo"))
        assertTrue(AyraAgentOrchestrator.isBanglish("bhalo achi tumi kemon"))
        assertTrue(AyraAgentOrchestrator.isBanglish("kichu bujhte parlam na"))

        // Native Bengali Unicode script must NOT be treated as Latin Banglish
        assertFalse(AyraAgentOrchestrator.isBanglish("কেমন আছো তুমি?"))
        assertFalse(AyraAgentOrchestrator.isBanglish("একটি অ্যান্ড্রয়েড অ্যাপ বানাও"))

        // English must NOT be treated as Banglish
        assertFalse(AyraAgentOrchestrator.isBanglish("What is the capital of Bangladesh?"))
        assertFalse(AyraAgentOrchestrator.isBanglish("Write a python script to parse CSV files"))
    }

    @Test
    fun testBuildSystemPromptWithBanglish() {
        val banglishPrompt = AyraAgentOrchestrator.buildSystemPrompt(
            domain = AgentDomain.GENERAL_ASSISTANT,
            isBengali = false,
            isGfMode = false,
            userCustomInstructions = "",
            isBanglish = true
        )
        assertTrue(banglishPrompt.contains("Banglish"))
        assertTrue(banglishPrompt.contains("বাংলা"))

        val bengaliPrompt = AyraAgentOrchestrator.buildSystemPrompt(
            domain = AgentDomain.GENERAL_ASSISTANT,
            isBengali = true,
            isGfMode = false,
            userCustomInstructions = "",
            isBanglish = false
        )
        assertTrue(bengaliPrompt.contains("Bengali (বাংলা)"))

        val englishPrompt = AyraAgentOrchestrator.buildSystemPrompt(
            domain = AgentDomain.GENERAL_ASSISTANT,
            isBengali = false,
            isGfMode = false,
            userCustomInstructions = "",
            isBanglish = false
        )
        assertTrue(englishPrompt.contains("English"))
    }

    @Test
    fun testCircuitBreakerResetProvider() {
        val breaker = ProviderCircuitBreaker(failureThreshold = 2, cooldownPeriodMs = 60_000L)
        val provider = AiProviderType.GEMINI

        assertTrue(breaker.canExecute(provider))

        // Trip the circuit to OPEN
        breaker.recordFailure(provider)
        breaker.recordFailure(provider)
        assertFalse("Circuit should be open after 2 failures", breaker.canExecute(provider))

        // Calling resetProvider should immediately restore provider to healthy execution
        breaker.resetProvider(provider)
        assertTrue("Provider should be executable immediately after resetProvider", breaker.canExecute(provider))
    }
}
