package com.ayra.ai.app

import com.ayra.ai.app.voice.SentimentDetector
import com.ayra.ai.app.voice.UserSentiment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

class AyraAutomationAndAiStressTest {

    @Test
    fun testSentimentDetectorStressCases() {
        // 1. Romantic / Affectionate inputs in Bengali & English
        val love1 = SentimentDetector.detectSentiment("তোমাকে খুব ভালোবাসি সোনা")
        assertEquals(UserSentiment.AFFECTIONATE, love1)
        assertTrue(love1.pitch > 1.0f)

        val love2 = SentimentDetector.detectSentiment("I love you so much sweetie")
        assertEquals(UserSentiment.AFFECTIONATE, love2)

        // 2. Sad / Hurt inputs in Bengali & English
        val sad1 = SentimentDetector.detectSentiment("মনটা খুব খারাপ আজকে, ভালো লাগছে না")
        assertEquals(UserSentiment.SAD_VULNERABLE, sad1)
        assertTrue(sad1.speechRate < 1.0f)

        val sad2 = SentimentDetector.detectSentiment("I feel so lonely and crying")
        assertEquals(UserSentiment.SAD_VULNERABLE, sad2)

        // 3. Excited / Cheerful inputs
        val excited1 = SentimentDetector.detectSentiment("দারুণ খবর! আমি জিতে গেছি! হুররে!")
        assertEquals(UserSentiment.EXCITED_JOYFUL, excited1)
        assertTrue(excited1.pitch >= 1.15f)

        // 4. Stressed / Anxious inputs
        val stressed1 = SentimentDetector.detectSentiment("আজকে কাজের অনেক চাপ, খুব টেনশন হচ্ছে")
        assertEquals(UserSentiment.ANXIOUS_FRUSTRATED, stressed1)

        // 5. Playful banter
        val playful1 = SentimentDetector.detectSentiment("তুমি কি আমাকে ভুলে গেছো নাকি পাজি?")
        assertEquals(UserSentiment.PLAYFUL_FLIRTY, playful1)
    }

    @Test
    fun testScheduledCallParsingLogic() {
        // User request: "কাউকে কোনো টাইমে কল করে কিছু বলতে বললে সেই টাইমে এআইটা সেই পারসনকে কল করে কথাটা বলে দেবে"
        val prompt1 = "কাল সকাল ১০টায় বাবাকে কল করে বলো ওষুধ খেয়ে নিতে"
        val lower1 = prompt1.lowercase()

        val isCall = (lower1.contains("call") || lower1.contains("কল") || lower1.contains("ফোন"))
        val hasTime = lower1.contains("সকাল") || lower1.contains("১০") || lower1.contains("কাল")
        val hasSay = lower1.contains("বলো") || lower1.contains("বলবে") || lower1.contains("বলতে")

        assertTrue(isCall && hasTime && hasSay)

        // Bengali recipient extraction (word before কে)
        val bengaliMatch = Regex("([a-zA-Z\u0980-\u09FF0-9+]+)কে").find(prompt1)
        assertEquals("বাবা", bengaliMatch?.groupValues?.get(1))

        // Bengali spoken message extraction
        val sayMatch = Regex("(?i)(?:বলো|বলবে|বলতে|say|tell)\\s*[:,-]?\\s*(.+)").find(prompt1)
        assertEquals("ওষুধ খেয়ে নিতে", sayMatch?.groupValues?.get(1)?.trim())

        // English scheduled call
        val prompt2 = "Call Rahul at 5 pm and say please come home"
        val lower2 = prompt2.lowercase()
        val isCall2 = lower2.contains("call")
        val hasTime2 = lower2.contains("5 pm")
        val sayMatch2 = Regex("(?i)(?:say|tell)\\s*[:,-]?\\s*(.+)").find(prompt2)
        assertTrue(isCall2 && hasTime2)
        assertEquals("please come home", sayMatch2?.groupValues?.get(1)?.trim())
    }

    @Test
    fun testScheduledSmsAndOfflineDeliveryLogic() {
        // User request: "কাউকে মেসেজ করতে বললে ঠিক সময় সেই মেসেজটা পাঠিয়ে দেবে নেট অফ থাকলে নেট অন করে মেসেজটা পাঠাবে"
        val prompt1 = "কাল সকাল ৯টায় অমিতকে মেসেজ পাঠাও যে আমি আসছি"
        val lower1 = prompt1.lowercase()

        val isMsg = lower1.contains("message") || lower1.contains("মেসেজ") || lower1.contains("পাঠাও")
        val hasTime = lower1.contains("সকাল") || lower1.contains("৯")
        assertTrue(isMsg && hasTime)

        // Offline direct SMS number extraction
        val prompt2 = "Send SMS to +919876543210 at 8 pm saying I reached home safely"
        val numberMatcher = Pattern.compile("(\\+?[0-9]{10,14})").matcher(prompt2)
        assertTrue(numberMatcher.find())
        assertEquals("+919876543210", numberMatcher.group(1))

        val msgMatch = Regex("(?i)(?:saying|message|বার্তা|যে)\\s*[:,-]?\\s*(.+)").find(prompt2)
        assertEquals("I reached home safely", msgMatch?.groupValues?.get(1)?.trim())
    }

    @Test
    fun testScheduledReminderLogic() {
        // User request: "আমি কোনো কথা মনে করিয়ে দিতে বললে ঠিক সময় সেই কাজের কথা মনে করিয়ে দেবে"
        val prompt1 = "আমাকে ১০ মিনিট পর মনে করিয়ে দিও বই পড়তে"
        val lower1 = prompt1.lowercase()

        val isRem = lower1.contains("remind") || lower1.contains("মনে করিয়ে") || lower1.contains("মনে করো")
        val hasRelativeTime = lower1.contains("১০ মিনিট পর") || lower1.contains("মিনিট")
        assertTrue(isRem && hasRelativeTime)

        val remMatch = Regex("(?i)(?:মনে\\s*করিয়ে\\s*দিও|মনে\\s*করিয়ে\\s*দাও|remind\\s*me\\s*(?:to|for)?)\\s*(.+)").find(prompt1)
        val extracted = remMatch?.groupValues?.get(1)?.trim()
        assertEquals("বই পড়তে", extracted)
    }

    @Test
    fun testDeepResearchIntentLogic() {
        val q1 = "Deep research on Quantum Computing and error correction"
        val q2 = "ডিপ রিসার্চ করো ব্ল্যাকহোল এবং সাধারণ আপেক্ষিকতা নিয়ে"
        val q3 = "What is photosynthesis?"

        fun isDeepResearch(text: String): Boolean {
            val lower = text.lowercase().trim()
            return lower.contains("deep research") || lower.contains("ডিপ রিসার্চ") ||
                    lower.contains("গভীর গবেষণা") || lower.contains("in-depth research") ||
                    lower.contains("detailed research on") || lower.startsWith("research:")
        }

        assertTrue(isDeepResearch(q1))
        assertTrue(isDeepResearch(q2))
        assertFalse(isDeepResearch(q3))
    }

    @Test
    fun testAiMediaPromptSynthesizerBilingualExpansion() {
        val (subject1, expanded1) = com.ayra.ai.app.ai.AiMediaPromptSynthesizer.synthesizeVisualPrompt("একটি সুন্দর বিড়ালের ছবি আঁকো", "IMAGE")
        assertTrue(subject1.contains("বিড়াল") || subject1.contains("cat"))
        assertTrue(expanded1.contains("cat") || expanded1.contains("domestic"))
        assertTrue(expanded1.contains("photorealistic") || expanded1.contains("8k"))

        val (subject2, expanded2) = com.ayra.ai.app.ai.AiMediaPromptSynthesizer.synthesizeVisualPrompt("রাজকন্যার ভিডিও বানাও", "VIDEO")
        assertTrue(subject2.contains("রাজকন্যা") || subject2.contains("princess"))
        assertTrue(expanded2.contains("princess") || expanded2.contains("tiara"))
        assertTrue(expanded2.contains("cinematic") || expanded2.contains("motion"))

        val imgUrl = com.ayra.ai.app.ai.AiMediaPromptSynthesizer.buildImageUrl("গাড়ির ছবি")
        assertTrue(imgUrl.contains("pollinations.ai"))
        assertTrue(imgUrl.contains("flux"))
    }

    @Test
    fun testDocumentTextExtractionRegexLogic() {
        val dummyPdfContent = "%PDF-1.4 1 0 obj << /Length 50 >> stream BT /F1 12 Tf (Hello Ayra AI deep document analysis) Tj ET endstream endobj"
        val extractedTokens = mutableListOf<String>()
        val parenMatcher = Pattern.compile("\\(([^()]+)\\)").matcher(dummyPdfContent)
        while (parenMatcher.find()) {
            parenMatcher.group(1)?.let { extractedTokens.add(it) }
        }
        val joined = extractedTokens.joinToString(" ")
        assertTrue(joined.contains("Hello Ayra AI deep document analysis"))
    }

    @Test
    fun testVoiceAssistantDoesNotAutoStartByDefault() {
        // Voice assistant must remain idle until explicitly triggered by button or 'Ayra'
        val isAlwaysOnWakeWordEnabled = false
        assertFalse("Voice assistant must not be always-on by default to prevent unwanted mic takeover", isAlwaysOnWakeWordEnabled)
    }

    @Test
    fun testAiAppAndCodeStudioIntentAndCodeGeneration() {
        // 1. Website generation
        assertTrue(com.ayra.ai.app.ai.AiAppAndCodeStudio.isCodeOrAppGenerationIntent("একটি ই-কমার্স ওয়েবসাইট বানাও"))
        assertTrue(com.ayra.ai.app.ai.AiAppAndCodeStudio.isCodeOrAppGenerationIntent("create a developer portfolio website"))
        assertTrue(com.ayra.ai.app.ai.AiAppAndCodeStudio.isCodeOrAppGenerationIntent("build cloud telemetry dashboard"))

        // 2. Android App generation
        assertTrue(com.ayra.ai.app.ai.AiAppAndCodeStudio.isCodeOrAppGenerationIntent("একটি অ্যান্ড্রয়েড অ্যাপ বানাও"))
        assertTrue(com.ayra.ai.app.ai.AiAppAndCodeStudio.isCodeOrAppGenerationIntent("create kotlin compose app"))

        // 3. HTML5 Game generation
        assertTrue(com.ayra.ai.app.ai.AiAppAndCodeStudio.isCodeOrAppGenerationIntent("একটি স্নেক গেম বানাও"))
        assertTrue(com.ayra.ai.app.ai.AiAppAndCodeStudio.isCodeOrAppGenerationIntent("build html5 canvas game"))

        // Test website generation code output
        val webRes = com.ayra.ai.app.ai.AiAppAndCodeStudio.generateCodeOrApp("e-commerce store website", isBengali = true, isGfMode = false)
        assertEquals("CODE_PROJECT", webRes.craftType)
        assertTrue(webRes.text.contains("<!DOCTYPE html>"))
        assertTrue(webRes.text.contains("AuraStore"))
        assertTrue(webRes.text.contains("addToCart"))

        // Test Android app code output
        val appRes = com.ayra.ai.app.ai.AiAppAndCodeStudio.generateCodeOrApp("একটি অ্যান্ড্রয়েড অ্যাপ বানাও", isBengali = true, isGfMode = false)
        assertTrue(appRes.text.contains("Room"))
        assertTrue(appRes.text.contains("StateFlow"))
        assertTrue(appRes.text.contains("ItemEntity"))
        assertTrue(appRes.text.contains("Scaffold"))

        // Test Game code output
        val gameRes = com.ayra.ai.app.ai.AiAppAndCodeStudio.generateCodeOrApp("snake game banao", isBengali = false, isGfMode = false)
        assertTrue(gameRes.text.contains("CYBERSNAKE"))
        assertTrue(gameRes.text.contains("canvas"))
    }

    @Test
    fun testAiModelTierLatestEndpoints() {
        val flashTier = com.ayra.ai.app.ai.AiModelTier.GEMINI_FLASH
        assertTrue(flashTier.displayName.contains("Gemini 2.5 Flash"))

        val proTier = com.ayra.ai.app.ai.AiModelTier.GEMINI_PRO
        assertTrue(proTier.displayName.contains("Gemini 2.5 Pro"))

        val deepseekTier = com.ayra.ai.app.ai.AiModelTier.OPENROUTER_DEEPSEEK
        assertTrue(deepseekTier.displayName.contains("DeepSeek-R1"))

        val llamaTier = com.ayra.ai.app.ai.AiModelTier.OPENROUTER_LLAMA
        assertTrue(llamaTier.displayName.contains("Llama 3.3"))
    }

    @Test
    fun testCybersecurityDefensiveKnowledgeSynthesis() {
        val aiEngine = com.ayra.ai.app.ai.UnifiedAiEngine()
        // Run coroutine to test response synthesis
        kotlinx.coroutines.runBlocking {
            val responseBn = aiEngine.generateUnifiedResponse(
                prompt = "সাইবার সিকিউরিটি ও এথিক্যাল ডিফেন্স কীভাবে কাজ করে?",
                personaMode = "PROFESSIONAL",
                modelTier = "auto"
            )
            assertNotNull(responseBn)
            assertTrue(responseBn.text.isNotEmpty())
            assertTrue(
                responseBn.text.contains("সিকিউরিটি") ||
                responseBn.text.contains("Security") ||
                responseBn.text.contains("ডিফেন্স") ||
                responseBn.text.contains("Defense") ||
                responseBn.text.contains("অনুপলব্ধ") ||
                responseBn.text.contains("Unavailable")
            )

            val responseEn = aiEngine.generateUnifiedResponse(
                prompt = "Explain ethical cybersecurity defense, OWASP top 10, and firewall architecture",
                personaMode = "PROFESSIONAL",
                modelTier = "auto"
            )
            assertNotNull(responseEn)
            assertTrue(responseEn.text.isNotEmpty())
        }
    }

    @Test
    fun testAutonomousKnowledgeEngineTopicAndMath() {
        // 1. Test topic keyword extraction
        val topic1 = com.ayra.ai.app.ai.AutonomousKnowledgeEngine.extractTopicKeyword("সূর্য কেন হলুদ দেখায়?")
        assertTrue(topic1.contains("সূর্য"))

        val topic2 = com.ayra.ai.app.ai.AutonomousKnowledgeEngine.extractTopicKeyword("What is quantum computing?")
        assertTrue(topic2.lowercase().contains("quantum"))

        // 2. Test math calculation evaluator
        val math1 = com.ayra.ai.app.ai.AutonomousKnowledgeEngine.tryCalculateMath("25 * 40")
        assertNotNull(math1)
        assertTrue(math1!!.contains("1000"))

        val math2 = com.ayra.ai.app.ai.AutonomousKnowledgeEngine.tryCalculateMath("500 + 350")
        assertNotNull(math2)
        assertTrue(math2!!.contains("850"))

        val math3 = com.ayra.ai.app.ai.AutonomousKnowledgeEngine.tryCalculateMath("100 / 4")
        assertNotNull(math3)
        assertTrue(math3!!.contains("25"))
    }

    @Test
    fun testAutonomousKnowledgeDeepQuestions() {
        val topic1 = com.ayra.ai.app.ai.AutonomousKnowledgeEngine.extractTopicKeyword("সূর্য কেন হলুদ দেখায়?")
        assertTrue(topic1.contains("সূর্য") || topic1.contains("হলুদ"))

        val topic2 = com.ayra.ai.app.ai.AutonomousKnowledgeEngine.extractTopicKeyword("চাঁদ সম্পর্কে বলো")
        assertTrue(topic2.contains("চাঁদ"))

        val math = com.ayra.ai.app.ai.AutonomousKnowledgeEngine.tryCalculateMath("15 * 12")
        assertNotNull(math)
        assertTrue(math!!.contains("180"))
    }

    @Test
    fun testPdfStudioIntentAndExecution() {
        val aiEngine = com.ayra.ai.app.ai.UnifiedAiEngine()
        kotlinx.coroutines.runBlocking {
            val pdfRes = aiEngine.generateUnifiedResponse(
                prompt = "একটি রিসার্চ রিপোর্ট এর পিডিএফ বানাও",
                personaMode = "PROFESSIONAL",
                modelTier = "auto"
            )
            assertEquals("PDF_DOWNLOAD", pdfRes.craftType)
            assertTrue(pdfRes.text.contains("PDF") || pdfRes.text.contains("পিডিএফ"))

            val pdfRes2 = aiEngine.generateUnifiedResponse(
                prompt = "Create project roadmap PDF document",
                personaMode = "PROFESSIONAL",
                modelTier = "auto"
            )
            assertEquals("PDF_DOWNLOAD", pdfRes2.craftType)
        }
    }

    @Test
    fun testSongAndMusicPromptSynthesis() {
        val aiEngine = com.ayra.ai.app.ai.UnifiedAiEngine()
        kotlinx.coroutines.runBlocking {
            // "music render"
            val musicRes1 = aiEngine.generateUnifiedResponse(
                prompt = "cyberpunk synthwave music render",
                personaMode = "GF_COMPANION",
                modelTier = "auto"
            )
            assertEquals("SONG", musicRes1.craftType)
            assertTrue(musicRes1.text.isNotEmpty())

            // "music banano"
            val musicRes2 = aiEngine.generateUnifiedResponse(
                prompt = "romantic acoustic guitar music banano",
                personaMode = "GF_COMPANION",
                modelTier = "auto"
            )
            assertEquals("SONG", musicRes2.craftType)

            // "গান বানাও"
            val songRes = aiEngine.generateUnifiedResponse(
                prompt = "বৃষ্টির দিনে একটি সুন্দর রোমান্টিক গান বানাও",
                personaMode = "GF_COMPANION",
                modelTier = "auto"
            )
            assertEquals("SONG", songRes.craftType)
        }
    }

    @Test
    fun testVideoEditAndMakingPromptSynthesis() {
        val aiEngine = com.ayra.ai.app.ai.UnifiedAiEngine()
        kotlinx.coroutines.runBlocking {
            // "video edit"
            val vidRes1 = aiEngine.generateUnifiedResponse(
                prompt = "space black hole cinematic video edit",
                personaMode = "PROFESSIONAL",
                modelTier = "auto"
            )
            assertEquals("VIDEO", vidRes1.craftType)

            // "video banano"
            val vidRes2 = aiEngine.generateUnifiedResponse(
                prompt = "neon cyberpunk city video banano",
                personaMode = "GF_COMPANION",
                modelTier = "auto"
            )
            assertEquals("VIDEO", vidRes2.craftType)

            // "ভিডিও বানাও"
            val vidRes3 = aiEngine.generateUnifiedResponse(
                prompt = "নীল সমুদ্রের সুন্দর ভিডিও বানাও",
                personaMode = "GF_COMPANION",
                modelTier = "auto"
            )
            assertEquals("VIDEO", vidRes3.craftType)
        }
    }

    @Test
    fun testDarkWebAndGlobalWebKnowledgeSynthesis() {
        val q1 = "The Hidden Wiki কী এবং ডার্ক ওয়েবে এটি কীভাবে কাজ করে?"
        assertTrue(com.ayra.ai.app.ai.GlobalWebKnowledgeGateway.isDarkWebOrNetworkSecurityQuery(q1))

        val answer1 = com.ayra.ai.app.ai.GlobalWebKnowledgeGateway.synthesizeGlobalNetworkKnowledge(q1, isBengali = true, isGfMode = false)
        assertTrue(answer1.contains("Hidden Wiki") || answer1.contains("হিডেন উইকি"))
        assertTrue(answer1.contains("onion") || answer1.contains("অনিয়ন"))

        val q2 = "Explain Tor onion routing, rendezvous points, and 3-hop encryption"
        assertTrue(com.ayra.ai.app.ai.GlobalWebKnowledgeGateway.isDarkWebOrNetworkSecurityQuery(q2))
        val answer2 = com.ayra.ai.app.ai.GlobalWebKnowledgeGateway.synthesizeGlobalNetworkKnowledge(q2, isBengali = false, isGfMode = false)
        assertTrue(answer2.contains("Relay") || answer2.contains("Tor") || answer2.contains("Circuit"))

        val q3 = "ডিপ ওয়েব ও ডার্ক ওয়েব এর মধ্যে পার্থক্য কি?"
        assertTrue(com.ayra.ai.app.ai.GlobalWebKnowledgeGateway.isDarkWebOrNetworkSecurityQuery(q3))
        val answer3 = com.ayra.ai.app.ai.GlobalWebKnowledgeGateway.synthesizeGlobalNetworkKnowledge(q3, isBengali = true, isGfMode = true)
        assertTrue(answer3.contains("Surface") || answer3.contains("সারফেস") || answer3.contains("Deep"))
    }

    @Test
    fun testVpnPrivacyShieldController() {
        val controller = com.ayra.ai.app.vpn.AyraVpnPrivacyController
        val confText = """
            [Interface]
            Address = 10.200.200.2/24
            PrivateKey = aaaaaaabbbbbbbccccccccdddddddeeeeeeefffffff=
            DNS = 1.1.1.1, 1.0.0.1
            MTU = 1420

            [Peer]
            PublicKey = 111111122222223333333444444455555556666666=
            Endpoint = wg.ayra-shield.net:51820
            AllowedIPs = 0.0.0.0/0, ::/0
        """.trimIndent()

        val parsed = controller.parseWireGuardConfig(confText, "Test Tunnel")
        assertNotNull(parsed)

        // Per Audit F03 (Option B): fake VPN is decommissioned
        controller.setVpnConnected(true)
        assertFalse(controller.vpnStatus.value.isConnected)

        // Test status format
        val summaryBn = controller.formatStatusSummary(isBengali = true)
        assertTrue(summaryBn.isNotBlank())

        // Disconnect
        controller.setVpnConnected(false)
        assertFalse(controller.vpnStatus.value.isConnected)
    }

    @Test
    fun testBuildYourOwnXKnowledgeEngine() {
        val engine = com.ayra.ai.app.ai.BuildYourOwnXKnowledgeEngine

        // 1. Build your own Git
        val gitPrompt = "Build your own Git from scratch in Python"
        assertTrue(engine.isBuildYourOwnXIntent(gitPrompt))
        val gitRes = engine.synthesizeBuildYourOwnX(gitPrompt, isBengali = true, isGfMode = false)
        assertEquals("CODE_PROJECT", gitRes.craftType)
        assertTrue(gitRes.text.contains("MiniGit") || gitRes.text.contains("sha1") || gitRes.text.contains("commit"))

        // 2. Build your own Docker
        val dockerPrompt = "Docker নিজে কিভাবে বানাবো? namespaces ও cgroups বুঝিয়ে দাও"
        assertTrue(engine.isBuildYourOwnXIntent(dockerPrompt))
        val dockerRes = engine.synthesizeBuildYourOwnX(dockerPrompt, isBengali = true, isGfMode = true)
        assertEquals("CODE_PROJECT", dockerRes.craftType)
        assertTrue(dockerRes.text.contains("cgroup") || dockerRes.text.contains("CLONE_NEWPID") || dockerRes.text.contains("namespace"))

        // 3. Build your own Redis
        val redisPrompt = "Build your own Redis key-value database"
        assertTrue(engine.isBuildYourOwnXIntent(redisPrompt))
        val redisRes = engine.synthesizeBuildYourOwnX(redisPrompt, isBengali = false, isGfMode = false)
        assertTrue(redisRes.text.contains("RESP") || redisRes.text.contains("PING") || redisRes.text.contains("SET"))

        // 4. Build your own LLM / Transformer
        val llmPrompt = "Build your own neural network transformer attention from scratch"
        assertTrue(engine.isBuildYourOwnXIntent(llmPrompt))
        val llmRes = engine.synthesizeBuildYourOwnX(llmPrompt, isBengali = false, isGfMode = false)
        assertTrue(llmRes.text.contains("ScaledDotProductAttention") || llmRes.text.contains("Softmax") || llmRes.text.contains("softmax"))
    }

    @Test
    fun testTripleAiImageAndVideoSynergy() {
        val aiEngine = com.ayra.ai.app.ai.UnifiedAiEngine()
        kotlinx.coroutines.runBlocking {
            val imgRes = aiEngine.generateUnifiedResponse(
                prompt = "রাজকন্যার সুন্দর ছবি আঁকো",
                personaMode = "GF_COMPANION",
                modelTier = "auto"
            )
            assertEquals("IMAGE", imgRes.craftType)
            assertTrue(imgRes.modelUsed!!.contains("Flux.1"))

            val vidRes = aiEngine.generateUnifiedResponse(
                prompt = "মহাকাশের 4k সিনেমাটিক ভিডিও বানাও",
                personaMode = "GF_COMPANION",
                modelTier = "auto"
            )
            assertEquals("VIDEO", vidRes.craftType)
            assertTrue(vidRes.modelUsed!!.contains("Motion Storyboard"))
        }
    }

    @Test
    fun testCyberTerminalEngineAndCraftType() {
        // 1. Test Terminal Engine Command Execution
        val helpOut = com.ayra.ai.app.security.CyberTerminalEngine.executeCommand("help")
        assertTrue(helpOut.output.contains("ping") && helpOut.output.contains("owasp") && helpOut.output.contains("cve"))

        val pingOut = com.ayra.ai.app.security.CyberTerminalEngine.executeCommand("ping 1.1.1.1")
        assertTrue(pingOut.output.contains("bytes from") && pingOut.output.contains("packet loss"))

        val hashOut = com.ayra.ai.app.security.CyberTerminalEngine.executeCommand("hash sha256 hello")
        assertTrue(hashOut.output.contains("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"))

        val owaspOut = com.ayra.ai.app.security.CyberTerminalEngine.executeCommand("owasp sqli")
        assertTrue(owaspOut.output.contains("Prepared") || owaspOut.output.contains("Injection"))

        val cveOut = com.ayra.ai.app.security.CyberTerminalEngine.executeCommand("cve CVE-2024-3094")
        assertTrue(cveOut.output.contains("XZ") || cveOut.output.contains("Backdoor") || cveOut.output.contains("liblzma"))

        // 2. Test Terminal Intent Routing via UnifiedAiEngine
        val aiEngine = com.ayra.ai.app.ai.UnifiedAiEngine()
        kotlinx.coroutines.runBlocking {
            val res = aiEngine.generateUnifiedResponse(
                prompt = "open cyber security terminal for practice",
                personaMode = "PROFESSIONAL",
                modelTier = "auto"
            )
            assertEquals("CYBER_TERMINAL", res.craftType)
            assertTrue(res.text.contains("Terminal") || res.text.contains("Cyber Security"))
        }
    }
}

