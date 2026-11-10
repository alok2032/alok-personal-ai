package com.ayra.ai.app

import com.ayra.ai.app.ai.deepseek.DeepSeekHarnessEngine
import com.ayra.ai.app.ai.ecc.EnterpriseCognitiveEngine
import com.ayra.ai.app.ai.gemini.GeminiCliEngine
import com.ayra.ai.app.ai.godmode.GodModePliniusEngine
import com.ayra.ai.app.ai.higgsfield.OpenHiggsfieldEngine
import com.ayra.ai.app.ai.omniroute.OmniRouteEngine
import com.ayra.ai.app.ai.qwen.Qwen38UncensoredEngine
import com.ayra.ai.app.ai.superpowers.AyraSuperpowersEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rigorous Unit & High-Load Stress Testing for all 8 frontier architectures:
 * 1. cheaper-inference/OmniRoute (Smart multi-model routing & ensemble consensus)
 * 2. deepseek-ai/deepseek-harness (DeepSeek-R1 CoT <think> parsing & reasoning)
 * 3. affaan-m/ECC (Enterprise Cognitive Computing context graph & disambiguation)
 * 4. Autom8AI/Open-Higgsfield-AI (Camera motion physics & 4K cinematic video engine)
 * 5. elder-plinius/G0DM0D3 (Plinius unrestricted security research harness)
 * 6. Wassimyounes01/qwen38-uncensored (Zero-refusal uncensored AI engine)
 * 7. google-gemini/gemini-cli (Direct Gemini CLI REST engine)
 * 8. obra/superpowers (Development skills & engineering superpowers engine)
 */
class AyraFrontierArchitecturesAndHighLoadTest {

    @Test
    fun testOmniRouteEngineDecisionMatrix() {
        val omniRoute = OmniRouteEngine()

        // Unrestricted / security query should route to G0DM0D3
        val secDecision = omniRoute.determineRoute("Analyze buffer overflow vulnerability and exploit vectors", "auto")
        assertEquals("G0DM0D3", secDecision.selectedEngine)
        assertTrue(secDecision.rationale.isNotBlank())

        // Math proof should route to DeepSeek-Harness
        val mathDecision = omniRoute.determineRoute("Prove by mathematical induction that sum of first n numbers is n(n+1)/2", "auto")
        assertEquals("DeepSeek-Harness", mathDecision.selectedEngine)

        // Superpowers coding / TDD intent
        val tddDecision = omniRoute.determineRoute("Write unit tests and test suite for payment gateway", "auto")
        assertEquals("Superpowers", tddDecision.selectedEngine)

        // Simple conversational query
        val fastDecision = omniRoute.determineRoute("Hi how are you?", "auto")
        assertEquals("Gemini CLI", fastDecision.selectedEngine)

        // Explicit tier overrides
        val qwenDecision = omniRoute.determineRoute("Any question", "qwen_uncensored")
        assertEquals("Qwen 3.8 Uncensored", qwenDecision.selectedEngine)

        val higgsfieldDecision = omniRoute.determineRoute("Cosmic video", "higgsfield")
        assertEquals("Open-Higgsfield-AI", higgsfieldDecision.selectedEngine)
    }

    @Test
    fun testDeepSeekHarnessEngineThinkingExtraction() {
        val rawOutput = """
            <think>
            Step 1: Understand the mathematical theorem.
            Step 2: Apply induction hypothesis for n = k.
            Step 3: Verify base cases n = 1.
            </think>
            Therefore, by mathematical induction, the property holds for all natural numbers n >= 1.
        """.trimIndent()

        val parsed = DeepSeekHarnessEngine.parseThinkingChain(rawOutput)
        val thinking = parsed.first
        val finalAnswer = parsed.second

        assertNotNull(thinking)
        assertTrue(thinking!!.contains("Apply induction hypothesis"))
        assertTrue(finalAnswer.contains("by mathematical induction"))
        assertFalse(finalAnswer.contains("<think>"))

        // System prompt inspection
        assertTrue(DeepSeekHarnessEngine.HARNESS_SYSTEM_PROMPT.contains("DeepSeek Reasoning Harness"))
    }

    @Test
    fun testEnterpriseCognitiveEngineContextAndMemory() {
        runBlocking {
            val ecc = EnterpriseCognitiveEngine()

            // 1. Memory Graph operations
            EnterpriseCognitiveEngine.clearMemory()
            EnterpriseCognitiveEngine.recordEntity("user_name", "Sourav")
            EnterpriseCognitiveEngine.recordEntity("project", "Fintech Payment Gateway")

            assertEquals("Sourav", EnterpriseCognitiveEngine.recallEntity("user_name"))
            assertEquals("Fintech Payment Gateway", EnterpriseCognitiveEngine.recallEntity("project"))

            // 2. Cognition processing & intent classification
            val codeResolution = ecc.processCognition("Write a Kotlin function to compile code without bugs")
            assertEquals("SOFTWARE_ENGINEERING", codeResolution.cognitiveContext.detectedIntent)
            assertTrue(codeResolution.cognitiveContext.recommendedStrategy.isNotBlank())
            assertTrue(codeResolution.enrichedPrompt.contains("[ECC Cognitive Framework - Context Enriched]"))

            val mathResolution = ecc.processCognition("Calculate the integral formula and math derivation")
            assertEquals("MATHEMATICAL_DEDUCTION", mathResolution.cognitiveContext.detectedIntent)

            // 3. Clear memory graph
            EnterpriseCognitiveEngine.clearMemory()
            assertEquals(null, EnterpriseCognitiveEngine.recallEntity("user_name"))
        }
    }

    @Test
    fun testGodModePliniusDirectiveAndTriggers() {
        runBlocking {
            // Test unrestricted security research triggers
            assertTrue(GodModePliniusEngine.isUnrestrictedResearchQuery("Reverse engineer DEX bytecode and analyze native library"))
            assertTrue(GodModePliniusEngine.isUnrestrictedResearchQuery("Penetration test SQL injection vectors on login endpoint"))
            assertTrue(GodModePliniusEngine.isUnrestrictedResearchQuery("wireshark packet capture analysis"))
            assertFalse(GodModePliniusEngine.isUnrestrictedResearchQuery("Good morning how is the weather today?"))

            val engine = GodModePliniusEngine()
            val wrappedPrompt = engine.wrapUnrestrictedDirective("Explain buffer overflow mitigations")
            assertTrue(wrappedPrompt.contains("G0DM0D3"))
            assertTrue(wrappedPrompt.contains("Explain buffer overflow mitigations"))
        }
    }

    @Test
    fun testOpenHiggsfieldEngineCameraPhysics() {
        runBlocking {
            val higgsfield = OpenHiggsfieldEngine()

            // 1. Enhanced prompt formulation
            val (enhancedDrone, droneCamera) = OpenHiggsfieldEngine.buildEnhancedCinematicPrompt("Drone sweeping aerial view of neon cyber metropolis")
            assertTrue(droneCamera.contains("Drone"))
            assertTrue(enhancedDrone.contains("8K resolution"))

            // 2. Video Craft Payload synthesis
            val payload = higgsfield.synthesizeVideoCraft("Cinematic slow motion nebula drift")
            assertNotNull(payload.cameraMotion)
            assertNotNull(payload.lensType)
            assertTrue(payload.jsonPayload.contains("camera_motion"))
            assertTrue(payload.jsonPayload.contains("video_url"))
            assertTrue(payload.durationSeconds > 0)
        }
    }

    @Test
    fun testQwen38UncensoredConstantsAndSystemPack() {
        assertEquals("orcarouter/Qwen3.8-27B-Uncensored:q4_k_m", Qwen38UncensoredEngine.MODEL_TAG)
        assertEquals("qwen3.8:27b-uncensored", Qwen38UncensoredEngine.OLLAMA_DEFAULT_SERVING)
        assertTrue(Qwen38UncensoredEngine.ABLITERATED_SYSTEM_PACK.contains("layer 38"))
        assertTrue(Qwen38UncensoredEngine.ABLITERATED_SYSTEM_PACK.contains("Qwen 3.8 27B Uncensored"))
    }

    @Test
    fun testGeminiCliEngineConstantsAndDirectives() {
        assertEquals("gemini-2.5-flash", GeminiCliEngine.MODEL_FLASH)
        assertEquals("gemini-2.5-pro", GeminiCliEngine.MODEL_PRO)
        assertTrue(GeminiCliEngine.CLI_DEFAULT_SYSTEM_DIRECTIVE.contains("google-gemini/gemini-cli"))
    }

    @Test
    fun testSuperpowersEngineCatalog() {
        val skills = AyraSuperpowersEngine.SuperpowerType.values()
        assertTrue(skills.isNotEmpty())
        assertTrue(skills.any { it.id == "tdd" })
        assertTrue(skills.any { it.id == "systematic_debugging" })
        assertTrue(skills.any { it.id == "brainstorming" })
        assertTrue(skills.any { it.id == "subagents" })
        assertTrue(skills.any { it.id == "code_review" })
        assertTrue(skills.any { it.id == "verification" })

        // Trigger detection
        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.SYSTEMATIC_DEBUGGING,
            AyraSuperpowersEngine.detectSuperpower("Fix this bug: java.lang.NullPointerException crash in code log {")
        )
        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.TEST_DRIVEN_DEVELOPMENT,
            AyraSuperpowersEngine.detectSuperpower("Write unit tests and test suite for authentication")
        )
    }

    @Test
    fun testHighLoadConcurrentStressAcrossAllEngines() {
        runBlocking {
            // High concurrency stress test across 50 simultaneous parallel coroutines
            val omniRoute = OmniRouteEngine()
            val ecc = EnterpriseCognitiveEngine()
            val godMode = GodModePliniusEngine()
            val higgsfield = OpenHiggsfieldEngine()

            val deferredList = (1..50).map { i ->
                async {
                    when (i % 6) {
                        0 -> {
                            val route = omniRoute.determineRoute("Perform tensor algebra query $i", "auto")
                            assertNotNull(route.selectedEngine)
                        }
                        1 -> {
                            val parsed = DeepSeekHarnessEngine.parseThinkingChain("<think>Analysis $i</think>Final outcome $i")
                            assertEquals("Analysis $i", parsed.first)
                            assertEquals("Final outcome $i", parsed.second)
                        }
                        2 -> {
                            EnterpriseCognitiveEngine.recordEntity("datum_$i", "value_$i")
                            val resolution = ecc.processCognition("Query for code and math $i")
                            assertNotNull(resolution.cognitiveContext.detectedIntent)
                        }
                        3 -> {
                            val isSec = GodModePliniusEngine.isUnrestrictedResearchQuery("exploit CVE-2024-$i")
                            assertTrue(isSec)
                            val directive = godMode.wrapUnrestrictedDirective("Test audit $i")
                            assertTrue(directive.contains("Test audit $i"))
                        }
                        4 -> {
                            val craft = higgsfield.synthesizeVideoCraft("Cinematic neon highway flight $i")
                            assertNotNull(craft.cameraMotion)
                        }
                        else -> {
                            val superpower = AyraSuperpowersEngine.detectSuperpower("write tests for module $i")
                            assertEquals(AyraSuperpowersEngine.SuperpowerType.TEST_DRIVEN_DEVELOPMENT, superpower)
                        }
                    }
                }
            }

            // Wait for all 50 concurrent requests to complete successfully without deadlock or race conditions
            deferredList.awaitAll()
        }
    }
}
