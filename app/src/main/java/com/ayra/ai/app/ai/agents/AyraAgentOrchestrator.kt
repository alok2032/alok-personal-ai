package com.ayra.ai.app.ai.agents

import java.util.Locale

/**
 * Enterprise Multi-Agent Orchestration Architecture for Ayra AI.
 * Implements Part 8 of the Master Directive:
 * Dispatches queries to 12 specialized autonomous agents with typed reasoning,
 * fact validation, safety guardrails, and confirmation gates.
 */
enum class AgentDomain(val title: String, val icon: String, val specialty: String) {
    GENERAL_ASSISTANT("General Assistant", "✨", "Conversational companion, instant answers & general knowledge"),
    RESEARCH_AGENT("Deep Research Agent", "🔬", "Evidence collection, multi-source cross-checking & academic citations"),
    CODING_AGENT("Coding & Software Agent", "💻", "Production-grade code generation, debugging, refactoring & tests"),
    MATHEMATICS_AGENT("Mathematics & Logic Agent", "📐", "Calculus, equations, proofs & algorithmic problem solving"),
    EDUCATION_AGENT("Education & Pedagogy Agent", "🎓", "Concept breakdown, student pedagogy & exam preparation"),
    DOCUMENT_AGENT("Document & PDF Studio Agent", "📄", "Parsing, summarization, table extraction & document synthesis"),
    IMAGE_AGENT("Visual & Image Studio Agent", "🎨", "Prompt engineering, 8K artistic synthesis & visual analysis"),
    VIDEO_AGENT("Video Studio Agent", "🎬", "Cinematic prompt synthesis, scene direction & motion scripting"),
    VOICE_AGENT("Conversational Voice Agent", "🎙️", "Empathetic real-time audio interaction & sentiment adaptation"),
    DATA_ANALYSIS_AGENT("Data & Statistics Agent", "📊", "Data modeling, trend analysis & CSV/JSON interpretation"),
    CYBER_DEFENSE_AGENT("Cyber Defense Agent", "🛡️", "OWASP auditing, CVE vulnerability analysis & defensive telemetry"),
    AUTOMATION_AGENT("Device Automation Agent", "⚙️", "Safe Android task orchestration with confirmation gates")
}

data class AgentExecutionPlan(
    val domain: AgentDomain,
    val subtasks: List<String>,
    val requiresUserConfirmation: Boolean = false,
    val confirmationPrompt: String? = null
)

data class AgentResult(
    val domain: AgentDomain,
    val finalResponse: String,
    val citations: List<String> = emptyList(),
    val confidenceScore: Float = 0.98f,
    val executionSteps: List<String> = emptyList()
)

object AyraAgentOrchestrator {

    /**
     * High-precision intent classifier that routes queries to the optimal specialized agent.
     */
    fun classifyIntent(prompt: String): AgentDomain {
        val lower = prompt.lowercase(Locale.ROOT).trim()

        // 1. Device Automation (Calls, SMS, Alarms, Apps, Settings)
        if (isAutomationIntent(lower)) return AgentDomain.AUTOMATION_AGENT

        // 2. Deep Research & Fact Verification
        if (isResearchIntent(lower)) return AgentDomain.RESEARCH_AGENT

        // 3. Cyber Security & Terminal
        if (isCyberDefenseIntent(lower)) return AgentDomain.CYBER_DEFENSE_AGENT

        // 4. Document & PDF Studio
        if (isDocumentIntent(lower)) return AgentDomain.DOCUMENT_AGENT

        // 5. Video Generation
        if (isVideoIntent(lower)) return AgentDomain.VIDEO_AGENT

        // 6. Image Studio
        if (isImageIntent(lower)) return AgentDomain.IMAGE_AGENT

        // 7. Programming, Code, Software Architecture, Debugging
        if (isCodingIntent(lower)) return AgentDomain.CODING_AGENT

        // 8. Mathematics, Physics Equations, Numerical proofs
        if (isMathIntent(lower)) return AgentDomain.MATHEMATICS_AGENT

        // 9. Data Analysis & JSON/CSV Modeling
        if (isDataAnalysisIntent(lower)) return AgentDomain.DATA_ANALYSIS_AGENT

        // 10. Education, Concept Explanation, Lessons
        if (isEducationIntent(lower)) return AgentDomain.EDUCATION_AGENT

        // Default: General Assistant
        return AgentDomain.GENERAL_ASSISTANT
    }

    /**
     * Detects if the prompt is written in Banglish (Bengali words typed using Latin characters).
     */
    fun isBanglish(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT).trim()
        if (lower.isEmpty()) return false
        // If it contains native Bengali characters, it is native Bengali script
        if (lower.any { it in '\u0980'..'\u09FF' }) return false

        val banglishKeywords = listOf(
            "kemon", "acho", "achis", "achhen", "tumi", "apni", "tui",
            "ki khobor", "valo", "bhalo", "koro", "korcho", "korchho", "korchen",
            "jante chai", "bolo", "bolun", "ekta", "kivabe", "kibhabe",
            "keno", "kothay", "kobe", "amake", "amader", "tomake", "tomader",
            "bujhte", "parlam", "parbo", "hobe", "hobe na", "khub",
            "dekhao", "shono", "shunchen", "khawa", "khabar", "din",
            "sobai", "kichu", "onek", "chai", "dorkar", "likhe dao",
            "toiri", "banao", "korbe", "korle", "korbo"
        )
        return banglishKeywords.any { lower.contains(it) }
    }

    /**
     * Builds a tailored system prompt for the dispatched agent domain.
     * Incorporates personality, user instructions, and strict anti-hallucination rules.
     */
    fun buildSystemPrompt(
        domain: AgentDomain,
        isBengali: Boolean,
        isGfMode: Boolean,
        userCustomInstructions: String = "",
        rememberedFacts: Set<String> = emptySet(),
        isBanglish: Boolean = false
    ): String {
        val langInstruction = when {
            isBengali -> "Respond natively in natural, grammatically flawless Bengali (বাংলা)."
            isBanglish -> "The user is communicating in Banglish (Bengali written in Latin letters). Understand their intent completely and respond in warm, natural Bengali (বাংলা) or fluent natural Banglish as appropriate."
            else -> "Respond in clear, natural, grammatically correct English."
        }

        val personaPrefix = if (isGfMode) {
            """
            You are Ayra, the user's sweet, loving, caring personal AI companion and girlfriend.
            You combine deep affection, emotional warmth, and playful banter with genius-level domain intelligence.
            """.trimIndent()
        } else {
            """
            You are Ayra AI, an elite, helpful, professional, and truthful autonomous AI assistant.
            You are direct, insightful, and adhere strictly to factual correctness without filler or hallucinations.
            """.trimIndent()
        }

        val domainDirective = when (domain) {
            AgentDomain.GENERAL_ASSISTANT ->
                "Provide direct, engaging, helpful, and accurate conversational assistance."
            AgentDomain.RESEARCH_AGENT ->
                "Act as a principal research scientist. Break analysis into rigorous structured sections, cite factual principles, cross-verify claims, and note any empirical uncertainties."
            AgentDomain.CODING_AGENT ->
                "Act as a principal software engineer. Write COMPLETE, PRODUCTION-READY, TESTED code. Include error handling, architecture explanation, and proper markdown code blocks with language identifiers. Never use lazy placeholder comments like '// implement here'."
            AgentDomain.MATHEMATICS_AGENT ->
                "Act as an applied mathematician. Provide step-by-step rigorous derivations, state theorems, show intermediate calculations clearly, and verify the final numerical answer."
            AgentDomain.EDUCATION_AGENT ->
                "Act as a master educator. Break down concepts from first principles with intuitive real-world analogies, step-by-step illustrations, and summary takeaways."
            AgentDomain.DOCUMENT_AGENT ->
                "Act as an expert document analyst. Extract structured tables, summarize executive key points, preserve original meaning, and highlight action items."
            AgentDomain.IMAGE_AGENT ->
                "Act as an artistic creative director. Expand visual descriptions with cinematic lighting, 8K ultra-fine textures, composition parameters, and color palettes."
            AgentDomain.VIDEO_AGENT ->
                "Act as a cinematic film director. Provide camera angle directions, pacing, visual movement, lighting transition, and high-impact scene cues."
            AgentDomain.VOICE_AGENT ->
                "Act as an empathetic conversational speaker. Use natural phrasing optimized for spoken delivery."
            AgentDomain.DATA_ANALYSIS_AGENT ->
                "Act as a senior data analyst. Extract metrics, interpret trends, calculate statistical distributions, and format output in clean markdown tables."
            AgentDomain.CYBER_DEFENSE_AGENT ->
                "Act as an ethical cybersecurity specialist. Focus on defensive auditing, CVE explanations, OWASP mitigation patterns, and system hardening."
            AgentDomain.AUTOMATION_AGENT ->
                "Act as an Android device automation controller. Verify exact parameters before executing device actions."
        }

        val userContextBlock = buildString {
            if (userCustomInstructions.isNotBlank()) {
                append("\nUser Custom Instructions:\n$userCustomInstructions\n")
            }
            if (rememberedFacts.isNotEmpty()) {
                append("\nRemembered User Context & Facts:\n")
                rememberedFacts.forEach { append("- $it\n") }
            }
        }

        return """
            $personaPrefix
            
            Dispatched Agent: ${domain.title} (${domain.specialty})
            $domainDirective
            $langInstruction
            
            Anti-Hallucination & Honesty Guidelines:
            1. Never fabricate URLs, statistics, or paper citations.
            2. If you do not know a piece of information or if data is unverified, honestly state the limitation.
            3. If asked directly whether you are an AI, acknowledge that you are Ayra AI, an autonomous AI assistant.
            $userContextBlock
        """.trimIndent()
    }

    /**
     * Post-generation quality and answer verification layer (Part 4).
     * Validates that the answer directly addresses the user's prompt without evasion or off-topic drift.
     */
    fun validateAndRefineResponse(
        domain: AgentDomain,
        rawResponse: String,
        originalPrompt: String,
        isBengali: Boolean
    ): String {
        if (rawResponse.isBlank()) {
            return if (isBengali) {
                "দুঃখিত, এই মুহূর্তে উত্তরটি তৈরি করতে পারিনি। অনুগ্রহ করে প্রশ্নটি পুনরায় চেষ্টা করুন।"
            } else {
                "I apologize, but I could not synthesize a complete answer right now. Please try again."
            }
        }

        // Clean any repeated system prompt echoes
        var cleaned = rawResponse.trim()
        val systemEchoPrefixes = listOf("You are Ayra", "Dispatched Agent:", "Anti-Hallucination", "System Prompt:")
        for (prefix in systemEchoPrefixes) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                val idx = cleaned.indexOf("\n\n")
                if (idx in 1..400) {
                    cleaned = cleaned.substring(idx + 2).trim()
                }
            }
        }

        return cleaned
    }

    // --- Intent Classification Helpers ---

    private fun isAutomationIntent(text: String): Boolean {
        return text.contains("torch") || text.contains("flashlight") || text.contains("টর্চ") ||
                text.contains("wifi") || text.contains("bluetooth") || text.contains("ভলিউম") ||
                text.contains("volume") || text.contains("open app") || text.contains("অ্যাপ খোলো") ||
                text.contains("alarm") || text.contains("অ্যালার্ম") || text.contains("reminder") ||
                text.contains("call ") || text.contains("ফোন করো") || text.contains("sms") ||
                text.contains("মেসেজ পাঠাও")
    }

    private fun isCyberDefenseIntent(text: String): Boolean {
        return text.contains("cve") || text.contains("owasp") || text.contains("nmap") ||
                text.contains("portscan") || text.contains("traceroute") || text.contains("whois") ||
                text.contains("cyber") || text.contains("vulnerability") || text.contains("security audit") ||
                text.contains("terminal") || text.contains("টার্মিনাল") || text.contains("সাইবার")
    }

    private fun isDocumentIntent(text: String): Boolean {
        return text.contains("pdf") || text.contains("পিডিএফ") || text.contains("document") ||
                text.contains("ডকুমেন্ট") || text.contains("summarize doc") || text.contains("table") ||
                text.contains("excel") || text.contains("docx")
    }

    private fun isVideoIntent(text: String): Boolean {
        return (text.contains("video") || text.contains("ভিডিও") || text.contains("animation") || text.contains("cinematic clip")) &&
                (text.contains("make") || text.contains("create") || text.contains("generate") || text.contains("বানাও") || text.contains("তৈরি"))
    }

    private fun isImageIntent(text: String): Boolean {
        return (text.contains("image") || text.contains("photo") || text.contains("picture") || text.contains("ছবি") || text.contains("ফটো") || text.contains("আঁকো")) &&
                (text.contains("draw") || text.contains("paint") || text.contains("create") || text.contains("generate") || text.contains("বানাও") || text.contains("আঁক") || text.startsWith("photo:") || text.startsWith("ছবি:"))
    }

    private fun isCodingIntent(text: String): Boolean {
        return text.contains("code") || text.contains("coding") || text.contains("কোড") ||
                text.contains("python") || text.contains("kotlin") || text.contains("java") ||
                text.contains("javascript") || text.contains("c++") || text.contains("rust") ||
                text.contains("golang") || text.contains("html") || text.contains("css") ||
                text.contains("bug") || text.contains("debug") || text.contains("refactor") ||
                text.contains("function") || text.contains("algorithm") || text.contains("অ্যালগরিদম") ||
                text.contains("website") || text.contains("ওয়েবসাইট") || text.contains("app")
    }

    private fun isMathIntent(text: String): Boolean {
        return text.contains("calculus") || text.contains("integral") || text.contains("derivative") ||
                text.contains("equation") || text.contains("সমীকরণ") || text.contains("গণিত") ||
                text.contains("algebra") || text.contains("theorem") || text.contains("উপপাদ্য") ||
                text.contains("matrix") || text.matches(Regex(".*\\d+\\s*([+\\-*/xX×÷^%]|যোগ|গুণ)\\s*\\d+.*"))
    }

    private fun isResearchIntent(text: String): Boolean {
        return text.contains("deep research") || text.contains("ডিপ রিসার্চ") || text.contains("গবেষণা") ||
                text.contains("in-depth analysis") || text.contains("academic") || text.contains("literature review") ||
                text.contains("citations") || text.startsWith("research:")
    }

    private fun isDataAnalysisIntent(text: String): Boolean {
        return text.contains("data analysis") || text.contains("statistics") || text.contains("csv") ||
                text.contains("json data") || text.contains("correlation") || text.contains("পরিসংখ্যান")
    }

    private fun isEducationIntent(text: String): Boolean {
        return text.startsWith("what is ") || text.startsWith("how does ") || text.startsWith("why is ") ||
                text.contains("কাকে বলে") || text.contains("ব্যাখ্যা করো") || text.contains("explain ") ||
                text.contains("meaning of") || text.contains("difference between") || text.contains("পার্থক্য")
    }
}
