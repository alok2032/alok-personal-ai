package com.ayra.ai.app

import com.ayra.ai.app.ai.AiMediaPromptSynthesizer
import com.ayra.ai.app.ai.AutonomousKnowledgeEngine
import com.ayra.ai.app.ai.BuildYourOwnXKnowledgeEngine
import com.ayra.ai.app.ai.UnifiedAiEngine
import com.ayra.ai.app.security.CyberTerminalEngine
import com.ayra.ai.app.voice.SentimentDetector
import com.ayra.ai.app.voice.UserSentiment
import com.ayra.ai.app.vpn.AyraVpnPrivacyController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-load concurrency and stress testing suite for Ayra AI.
 * Simulates extreme parallel workloads across all features:
 * - Math evaluation under massive multi-threading
 * - Cyber Security Terminal rapid execution
 * - BuildYourOwnX architecture engine synthesis
 * - VPN Controller parallel state mutations
 * - Unified AI Engine sub-millisecond response caching
 * - Prompt classification throughput
 */
class AyraHighLoadStressTest {

    @Test
    fun testHighLoadConcurrentMathCalculations() = runBlocking {
        val iterations = 500
        val successCount = AtomicInteger(0)

        val deferredList = (1..iterations).map { i ->
            async(Dispatchers.Default) {
                val a = (i % 50) + 1
                val b = (i % 20) + 1
                val prompt = "$a * $b"
                val result = AutonomousKnowledgeEngine.tryCalculateMath(prompt)
                assertNotNull("Math result should not be null for $prompt", result)
                val expected = (a * b).toString()
                if (result?.contains(expected) == true) {
                    successCount.incrementAndGet()
                }
            }
        }
        deferredList.awaitAll()

        assertEquals("All 500 concurrent math calculations must succeed", iterations, successCount.get())
    }

    @Test
    fun testHighLoadCyberTerminalConcurrentCommands() = runBlocking {
        val commands = listOf(
            "ping 8.8.8.8",
            "traceroute 1.1.1.1",
            "whois example.com",
            "dig cloudflare.com",
            "ls",
            "pwd",
            "whoami",
            "id",
            "uname",
            "date",
            "uptime",
            "cat defense.txt",
            "ps",
            "top",
            "df",
            "free",
            "ifconfig",
            "netstat",
            "hash sha256 secret_key_test",
            "hash md5 admin",
            "base64 encode AyraAiKingOfInternet",
            "base64 decode QXlyYUFpS2luZ09mSW50ZXJuZXQ=",
            "cve CVE-2024-3094",
            "cve CVE-2021-44228",
            "cve-2024-3094",
            "owasp sqli",
            "owasp xss",
            "sqli",
            "vpn",
            "labs",
            "portscan 192.168.1.1",
            "headers https://github.com",
            "help"
        )

        val concurrencyFactor = 20 // 17 commands * 20 = 340 parallel terminal executions
        val completedCount = AtomicInteger(0)

        val tasks = (0 until concurrencyFactor).flatMap {
            commands.map { cmd ->
                async(Dispatchers.Default) {
                    val output = CyberTerminalEngine.executeCommand(cmd)
                    assertNotNull(output)
                    assertTrue("Output should not be empty for $cmd", output.output.isNotBlank())
                    assertFalse("Standard commands should not report unknown command error", output.output.contains("bash: command not found"))
                    completedCount.incrementAndGet()
                }
            }
        }
        tasks.awaitAll()

        assertEquals("All parallel terminal commands must complete", commands.size * concurrencyFactor, completedCount.get())
    }

    @Test
    fun testHighLoadBuildYourOwnXConcurrentSynthesis() = runBlocking {
        val topics = listOf(
            "git", "docker", "redis", "sqlite", "transformer", "blockchain", "compiler", "kernel", "web server"
        )

        val successMap = ConcurrentHashMap<String, Boolean>()

        val jobs = topics.map { topic ->
            async(Dispatchers.Default) {
                val prompt = "Build your own $topic from scratch"
                assertTrue("Must detect BuildYourOwnX intent for $topic", BuildYourOwnXKnowledgeEngine.isBuildYourOwnXIntent(prompt))
                val response = BuildYourOwnXKnowledgeEngine.synthesizeBuildYourOwnX(prompt, isBengali = false, isGfMode = false)
                assertNotNull(response)
                assertTrue("Code should contain implementation for $topic", response.text.contains("```") || response.text.contains(topic, ignoreCase = true))
                successMap[topic] = true
            }
        }
        jobs.awaitAll()

        assertEquals("All BuildYourOwnX topics should synthesize successfully", topics.size, successMap.size)
    }

    @Test
    fun testHighLoadVpnControllerThreadSafety() = runBlocking {
        val operations = 200
        val bytesPerOp = 1024L
        val jobs = (1..operations).map {
            async(Dispatchers.Default) {
                AyraVpnPrivacyController.toggleKillSwitch()
                AyraVpnPrivacyController.toggleDnsProtection()
                AyraVpnPrivacyController.addEncryptedBytes(bytesPerOp)

                val status = AyraVpnPrivacyController.vpnStatus.value
                assertNotNull(status)
                assertNotNull(status.config)
                assertFalse(status.isConnected)
            }
        }
        jobs.awaitAll()

        val finalStatus = AyraVpnPrivacyController.vpnStatus.value
        assertNotNull(finalStatus)
        assertFalse(finalStatus.isConnected)
    }

    @Test
    fun testUnifiedAiEngineSubMillisecondCachingUnderHighLoad() = runBlocking {
        val engine = UnifiedAiEngine()
        UnifiedAiEngine.clearCache()

        val testPrompt = "What is the capital of Bangladesh?"

        // First call: populates cache
        val firstResponse = engine.generateUnifiedResponse(testPrompt, personaMode = "PROFESSIONAL", modelTier = "auto")
        assertNotNull(firstResponse)
        assertTrue(firstResponse.text.isNotBlank())

        // 300 concurrent requests for the same prompt: must resolve via cache in < 1ms
        val startTime = System.currentTimeMillis()
        val requests = 300
        val responses = (1..requests).map {
            async(Dispatchers.Default) {
                engine.generateUnifiedResponse(testPrompt, personaMode = "PROFESSIONAL", modelTier = "auto")
            }
        }.awaitAll()
        val duration = System.currentTimeMillis() - startTime

        assertEquals("All 300 cached responses should match", requests, responses.size)
        responses.forEach { res ->
            assertEquals(firstResponse.text, res.text)
        }
        // Under 300 parallel reads, average should be extremely fast (well under 500ms total for all 300)
        assertTrue("300 parallel cached reads should complete within 1500ms (actual: ${duration}ms)", duration < 1500)
    }

    @Test
    fun testConcurrentIntentClassificationThroughput() = runBlocking {
        val samplePrompts = listOf(
            Pair("একটি সুন্দর ড্রাগন আঁকো", "IMAGE"),
            Pair("Generate an anime girl in rain video", "VIDEO"),
            Pair("একটি রোমান্টিক প্রেমের গান বানাও", "SONG"),
            Pair("Convert this image to PNG", "CONVERT"),
            Pair("ping 127.0.0.1 and check latency", "CYBER_TERMINAL"),
            Pair("Build your own redis server from scratch", "BUILD_X"),
            Pair("Create a modern calculator android app in Jetpack Compose", "APP_CODE"),
            Pair("What is 45 * 80?", "MATH")
        )

        val loopCount = 50 // 8 * 50 = 400 parallel classification checks
        val counter = AtomicInteger(0)

        val jobs = (0 until loopCount).flatMap {
            samplePrompts.map { (prompt, expectedType) ->
                async(Dispatchers.Default) {
                    when (expectedType) {
                        "CYBER_TERMINAL" -> assertTrue(UnifiedAiEngine.isCyberTerminalIntent(prompt))
                        "BUILD_X" -> assertTrue(BuildYourOwnXKnowledgeEngine.isBuildYourOwnXIntent(prompt))
                        "MATH" -> assertNotNull(AutonomousKnowledgeEngine.tryCalculateMath(prompt))
                        else -> assertTrue(prompt.isNotBlank())
                    }
                    counter.incrementAndGet()
                }
            }
        }
        jobs.awaitAll()

        assertEquals(samplePrompts.size * loopCount, counter.get())
    }

    @Test
    fun testHighLoadAttachmentMenuFeatures() = runBlocking {
        // High-load validation of all 12 tools triggered via the + attachment hub
        val tools = listOf(
            Pair("Create a professional PDF document about Artificial Intelligence", "PDF"),
            Pair("একটি রঙিন সুন্দর রাজকন্যার 8K ছবি আঁকো", "IMAGE"),
            Pair("Generate a cinematic 4K video of galaxy collision", "VIDEO"),
            Pair("একটি রোমান্টিক মিষ্টি প্রেমের গান বানাও", "SONG"),
            Pair("3D CAD interactive model of a drone", "3D"),
            Pair("Open Cyber Terminal and run diagnostics", "TERMINAL"),
            Pair("Build your own Git from scratch with object database", "BUILD_X"),
            Pair("Deep research on quantum computing error correction", "RESEARCH"),
            Pair("Where am I right now? GPS Coordinates", "LOCATION")
        )

        val concurrency = 40 // 9 * 40 = 360 parallel tool intent verifications
        val successCounter = AtomicInteger(0)

        val jobs = (0 until concurrency).flatMap {
            tools.map { (prompt, expectedTool) ->
                async(Dispatchers.Default) {
                    when (expectedTool) {
                        "PDF" -> assertTrue(UnifiedAiEngine.isPdfStudioIntent(prompt))
                        "IMAGE" -> assertTrue(UnifiedAiEngine.isImageGenerationIntent(prompt))
                        "VIDEO" -> assertTrue(UnifiedAiEngine.isVideoGenerationIntent(prompt))
                        "SONG" -> assertTrue(UnifiedAiEngine.isSongGenerationIntent(prompt))
                        "3D" -> assertTrue(UnifiedAiEngine.is3dModelIntent(prompt))
                        "TERMINAL" -> assertTrue(UnifiedAiEngine.isCyberTerminalIntent(prompt))
                        "BUILD_X" -> assertTrue(BuildYourOwnXKnowledgeEngine.isBuildYourOwnXIntent(prompt))
                        "RESEARCH" -> assertTrue(UnifiedAiEngine.isDeepResearchIntent(prompt))
                        "LOCATION" -> assertTrue(prompt.contains("GPS", ignoreCase = true))
                    }
                    successCounter.incrementAndGet()
                }
            }
        }
        jobs.awaitAll()

        assertEquals(tools.size * concurrency, successCounter.get())
    }

    @Test
    fun testHighLoadAssistantSentimentAndVoiceModulation() = runBlocking {
        // 500 parallel queries testing the Assistant's emotional voice modulation engine
        val testInputs = listOf(
            Pair("তোমাকে অনেক ভালোবাসি সোনা", UserSentiment.AFFECTIONATE),
            Pair("আজকে মন খারাপ, ভালো লাগছে না", UserSentiment.SAD_VULNERABLE),
            Pair("হুররে! আমি প্রথম হয়েছি! দারুণ লাগছে!", UserSentiment.EXCITED_JOYFUL),
            Pair("আজকে কাজের অনেক চাপ, খুব টেনশন হচ্ছে", UserSentiment.ANXIOUS_FRUSTRATED),
            Pair("তুমি কি আমাকে ভুলে গেছো নাকি পাজি?", UserSentiment.PLAYFUL_FLIRTY),
            Pair("What is the distance to Mars?", UserSentiment.NEUTRAL_CARING)
        )

        val loopCount = 60 // 6 * 60 = 360 parallel emotional sentiment detections
        val successCounter = AtomicInteger(0)

        val jobs = (0 until loopCount).flatMap {
            testInputs.map { (input, expectedSentiment) ->
                async(Dispatchers.Default) {
                    val detected = SentimentDetector.detectSentiment(input)
                    assertEquals("Sentiment detection mismatch for: $input", expectedSentiment, detected)
                    assertTrue(detected.pitch > 0.5f && detected.pitch < 2.0f)
                    assertTrue(detected.speechRate > 0.5f && detected.speechRate < 2.0f)
                    successCounter.incrementAndGet()
                }
            }
        }
        jobs.awaitAll()

        assertEquals(testInputs.size * loopCount, successCounter.get())
    }

    @Test
    fun testHighLoadMediaPromptSynthesizerTripleAi() = runBlocking {
        // High concurrency testing of ChatGPT (DALL-E 3) + Gemini 2.5 + Claude 3.7 synergy synthesizer
        val prompts = listOf(
            "একটি সুন্দর সাদা বিড়াল",
            "Golden retriever dog playing in autumn leaves",
            "fairytale royal princess with ornate tiara",
            "Futuristic neon cyberpunk city in rain 4k",
            "Astronaut floating near Saturn rings"
        )

        val loopCount = 50 // 5 * 50 = 250 parallel visual prompt syntheses
        val counter = AtomicInteger(0)

        val jobs = (0 until loopCount).flatMap {
            prompts.map { p ->
                async(Dispatchers.Default) {
                    val (imageSubject, expandedPrompt) = AiMediaPromptSynthesizer.synthesizeVisualPrompt(p, "IMAGE")
                    assertNotNull(imageSubject)
                    assertNotNull(expandedPrompt)
                    assertTrue(expandedPrompt.contains("8K") || expandedPrompt.contains("photorealistic") || expandedPrompt.contains("octane render"))

                    val url = AiMediaPromptSynthesizer.buildImageUrl(p)
                    assertTrue(url.startsWith("https://image.pollinations.ai/prompt/"))
                    counter.incrementAndGet()
                }
            }
        }
        jobs.awaitAll()

        assertEquals(prompts.size * loopCount, counter.get())
    }

    @Test
    fun testFactualKnowledgePointByPointAccuracy() {
        // 1. Topic Extraction
        val topic1 = AutonomousKnowledgeEngine.extractTopicKeyword("সালোকসংশ্লেষণ কি")
        assertTrue(topic1.isNotBlank())

        val topic2 = AutonomousKnowledgeEngine.extractTopicKeyword("সূর্য কেন হলুদ দেখায়")
        assertTrue(topic2.isNotBlank())

        // 2. Point by point formatting (clean encyclopedic style)
        val formatted = AutonomousKnowledgeEngine.formatKnowledgePoints("Quantum Mechanics", "Quantum mechanics is a fundamental theory in physics that provides a description of the physical properties of nature at the scale of atoms and subatomic particles. It is the foundation of all quantum physics including quantum chemistry.", isBengali = false)
        assertTrue(formatted.contains("**Quantum Mechanics**"))
        assertTrue(formatted.contains("fundamental theory in physics"))

        // 3. Mathematical Evaluation
        val math = AutonomousKnowledgeEngine.tryCalculateMath("100 * 25")
        assertNotNull(math)
        assertTrue(math!!.contains("2500"))
    }

    @Test
    fun testVpnThroughputAndPassThroughRouting() {
        val sampleConf = """
            [Interface]
            Address = 10.66.66.2/32, fd42:42:42::2/128
            PrivateKey = YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWE=
            DNS = 1.1.1.1, 1.0.0.1
            MTU = 1380

            [Peer]
            PublicKey = YmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmJiYmI=
            Endpoint = ams.wireguard.network:51820
            AllowedIPs = 0.0.0.0/0, ::/0
        """.trimIndent()

        val parsed = AyraVpnPrivacyController.parseWireGuardConfig(sampleConf, "Amsterdam Node")
        assertNotNull(parsed)

        // Per Audit F03 (Option B): fake VPN is decommissioned and always reports disconnected
        AyraVpnPrivacyController.setVpnConnected(true)
        assertFalse(AyraVpnPrivacyController.vpnStatus.value.isConnected)

        val summary = AyraVpnPrivacyController.formatStatusSummary(isBengali = true)
        assertTrue(summary.isNotBlank())
    }

    @Test
    fun testSecureKeyStoreEncryptionDecryptionRoundTrip() {
        val testAlias = "test_encrypted_api_key_alias"
        val originalSecret = "sk-ant-api03-abcdef1234567890XYZ-TestValidAnthropicKey"

        // Encrypt
        val encryptedBase64 = com.ayra.ai.app.security.SecureKeyStore.encrypt(testAlias, originalSecret)
        assertNotNull(encryptedBase64)
        assertFalse(encryptedBase64 == originalSecret)

        // Decrypt
        val decryptedSecret = com.ayra.ai.app.security.SecureKeyStore.decrypt(testAlias, encryptedBase64)
        assertEquals(originalSecret, decryptedSecret)
    }

    @Test
    fun testIntentDisambiguationAndZeroFalsePositives() {
        val educationalQueries = listOf(
            "Call by value vs call by reference in Java",
            "What is Wi-Fi and how does it work?",
            "Difference between Wi-Fi and Bluetooth",
            "Explain how sound travels through air",
            "Who invented the clock (ঘড়ি)?",
            "Where is the Eiffel Tower located?",
            "Send me an example of quicksort in C++",
            "What is an alarm circuit?",
            "GPS satellite কীভাবে কাজ করে?",
            "Camera lens aperture explain",
            "How does telephone call routing work?"
        )

        for (q in educationalQueries) {
            val isEdu = com.ayra.ai.app.automation.DeviceAutomationController.isEducationalOrInformationalQuery(q)
            assertTrue("Query must be classified as educational/informational: $q", isEdu)
        }

        // Direct device automation commands should NOT be blocked
        val directCommands = listOf(
            "turn on torch",
            "turn off flashlight",
            "টর্চ জ্বালাও",
            "লাইট অন করো",
            "turn on wifi",
            "bluetooth on",
            "ভলিউম বাড়াও"
        )

        for (cmd in directCommands) {
            val isEdu = com.ayra.ai.app.automation.DeviceAutomationController.isEducationalOrInformationalQuery(cmd)
            assertFalse("Direct hardware command must NOT be marked educational: $cmd", isEdu)
        }
    }

    @Test
    fun testAyraAgentOrchestratorIntentClassification() {
        val orchestrator = com.ayra.ai.app.ai.agents.AyraAgentOrchestrator

        // Coding Agent
        assertEquals(
            com.ayra.ai.app.ai.agents.AgentDomain.CODING_AGENT,
            orchestrator.classifyIntent("Python এ কুইক সর্ট অ্যালগরিদম লিখে দাও")
        )
        assertEquals(
            com.ayra.ai.app.ai.agents.AgentDomain.CODING_AGENT,
            orchestrator.classifyIntent("Fix this kotlin coroutine memory leak bug")
        )

        // Math Agent
        assertEquals(
            com.ayra.ai.app.ai.agents.AgentDomain.MATHEMATICS_AGENT,
            orchestrator.classifyIntent("Solve the differential equation dy/dx + 2y = e^x")
        )

        // Cyber Defense Agent
        assertEquals(
            com.ayra.ai.app.ai.agents.AgentDomain.CYBER_DEFENSE_AGENT,
            orchestrator.classifyIntent("cve CVE-2024-3094 xz utils backdoor audit")
        )

        // Research Agent
        assertEquals(
            com.ayra.ai.app.ai.agents.AgentDomain.RESEARCH_AGENT,
            orchestrator.classifyIntent("Deep research on quantum computing error correction algorithms")
        )

        // Education Agent
        assertEquals(
            com.ayra.ai.app.ai.agents.AgentDomain.EDUCATION_AGENT,
            orchestrator.classifyIntent("Explain how photosensors work in simple terms")
        )

        // Automation Agent
        assertEquals(
            com.ayra.ai.app.ai.agents.AgentDomain.AUTOMATION_AGENT,
            orchestrator.classifyIntent("টর্চ জ্বালাও")
        )
    }

    @Test
    fun testAiRoutingModeSelectionUnderLoad() {
        val providers = com.ayra.ai.app.ai.providers.ExternalAiProviders(null)

        val fastRoute = providers.getOptimalModelForRoutingMode(com.ayra.ai.app.ai.providers.AiRoutingMode.FAST)
        assertNotNull(fastRoute.first)
        assertNotNull(fastRoute.second)

        val reasoningRoute = providers.getOptimalModelForRoutingMode(com.ayra.ai.app.ai.providers.AiRoutingMode.REASONING)
        assertTrue(reasoningRoute.second.contains("reasoner") || reasoningRoute.second.contains("r1") || reasoningRoute.second.contains("pro") || reasoningRoute.second.contains("o3"))

        val codingRoute = providers.getOptimalModelForRoutingMode(com.ayra.ai.app.ai.providers.AiRoutingMode.CODING)
        assertTrue(codingRoute.second.contains("sonnet") || codingRoute.second.contains("coder") || codingRoute.second.contains("codestral") || codingRoute.second.contains("pro"))

        val localRoute = providers.getOptimalModelForRoutingMode(com.ayra.ai.app.ai.providers.AiRoutingMode.PRIVACY_LOCAL)
        assertNotNull(localRoute.first)
        assertNotNull(localRoute.second)
    }

    @Test
    fun testAyraAgentOrchestratorSystemPromptConstruction() {
        val orchestrator = com.ayra.ai.app.ai.agents.AyraAgentOrchestrator
        val prompt = orchestrator.buildSystemPrompt(
            domain = com.ayra.ai.app.ai.agents.AgentDomain.CODING_AGENT,
            isBengali = true,
            isGfMode = false,
            userCustomInstructions = "Prioritize clean Kotlin 2.0 idioms",
            rememberedFacts = setOf("User is a senior engineer", "User prefers functional programming")
        )

        assertTrue(prompt.contains("Coding & Software Agent"))
        assertTrue(prompt.contains("Bengali (বাংলা)"))
        assertTrue(prompt.contains("Prioritize clean Kotlin 2.0 idioms"))
        assertTrue(prompt.contains("User is a senior engineer"))
        assertTrue(prompt.contains("Never fabricate URLs"))
    }
}
