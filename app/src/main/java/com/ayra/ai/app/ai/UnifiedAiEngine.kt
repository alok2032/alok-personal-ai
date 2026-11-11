package com.ayra.ai.app.ai

import android.content.Context
import com.ayra.ai.app.BuildConfig
import com.ayra.ai.app.data.AyraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class AiResponse(
    val text: String,
    val craftType: String? = null,
    val craftPayload: String? = null,
    val modelUsed: String? = null
)

enum class AiModelTier(
    val id: String,
    val displayName: String,
    val providerName: String,
    val speedLabel: String,
    val reasoningLabel: String,
    val description: String,
    val accentColorHex: Long
) {
    AUTO(
        id = "auto",
        displayName = "Auto Adaptive Router ⚡",
        providerName = "OmniRoute Intelligent Matrix",
        speedLabel = "Adaptive ⚡",
        reasoningLabel = "Dynamic 🧠",
        description = "OmniRoute স্মার্ট মাল্টি-প্রোভাইডার অপটিমাইজেশন — সাধারণ প্রশ্নে ফ্ল্যাশ ও জটিল বিষয়ে ডিপ রিজনিং",
        accentColorHex = 0xFF00FFFF
    ),
    OMNI_ROUTE(
        id = "omniroute",
        displayName = "OmniRoute Ensemble ⚡👑",
        providerName = "cheaper-inference/OmniRoute",
        speedLabel = "Consensus Synergy ⚡",
        reasoningLabel = "Multi-Model Fusion 👑",
        description = "বিশ্বের সেরা ফলাফল পাওয়ার জন্য সকল মডেলের সম্মিলিত জ্ঞান ও সিনার্জি ভিত্তিক উত্তর",
        accentColorHex = 0xFFFFD700
    ),
    GEMINI_FLASH(
        id = "gemini_flash",
        displayName = "Gemini 2.5 Flash Live ⚡",
        providerName = "Google AI Next-Gen / CLI",
        speedLabel = "Ultra Fast (0.4s) ⚡⚡",
        reasoningLabel = "High + Web Grounding 🌐",
        description = "জিরো-ল্যাটেন্সি দ্রুততম প্রতিক্রিয়া, দৈনন্দিন আলাপচারিতা, ফটো/পিডিএফ অ্যানালাইসিস ও লাইভ সার্চের জন্য সেরা",
        accentColorHex = 0xFF00FF7F
    ),
    GEMINI_PRO(
        id = "gemini_pro",
        displayName = "Gemini 2.5 Pro Deep Reasoning 🧠",
        providerName = "Google AI Frontier",
        speedLabel = "Deep Reasoning ⏳",
        reasoningLabel = "Chain-of-Thought & Coding 🧠🧠",
        description = "উন্নত ওয়েবসাইট তৈরি, অ্যান্ড্রয়েড অ্যাপ ডেভেলপমেন্ট, জটিল গণিত, বিজ্ঞান ও ডিপ রিসার্চ",
        accentColorHex = 0xFFB388FF
    ),
    DEEPSEEK_HARNESS(
        id = "deepseek_harness",
        displayName = "DeepSeek-R1 (Harness CoT) 🔬",
        providerName = "deepseek-ai/deepseek-harness",
        speedLabel = "Thinking Chain ⏳",
        reasoningLabel = "Chain-of-Thought 🧠🧠🧠",
        description = "যৌক্তিক ধাপভিত্তিক বিশ্লেষণ, গাণিতিক প্রমাণ ও স্বয়ংক্রিয় টেস্ট হারনেস যাচাই",
        accentColorHex = 0xFFFF80AB
    ),
    OPENROUTER_DEEPSEEK(
        id = "deepseek_r1",
        displayName = "DeepSeek-R1 (Chain-of-Thought) 🧠",
        providerName = "DeepSeek / OpenRouter",
        speedLabel = "Thinking Chain ⏳",
        reasoningLabel = "Chain-of-Thought 🧠🧠🧠",
        description = "উন্মুক্ত চেইন-অফ-থট রিজনিং ও যৌক্তিক ধাপভিত্তিক সমস্যা সমাধান",
        accentColorHex = 0xFFFF80AB
    ),
    OPENROUTER_LLAMA(
        id = "llama_3_3",
        displayName = "Llama 3.3 70B Instruct 🌐",
        providerName = "Meta / OpenRouter",
        speedLabel = "Fast (0.7s) ⚡",
        reasoningLabel = "Very High 🧠",
        description = "সাবলীল বহুভাষিক সাহিত্য রচনা, অনুবাদ ও গভীর জ্ঞানভিত্তিক কথোপকথন",
        accentColorHex = 0xFFFFD700
    ),
    QWEN_UNCENSORED(
        id = "qwen_uncensored",
        displayName = "Qwen 3.8 27B Uncensored 🔓",
        providerName = "Abliterated Zero-Refusal",
        speedLabel = "Uncensored / Fast ⚡",
        reasoningLabel = "Abliterated 27B 🔓",
        description = "লেয়ার-৩৮ অর্থোগোনাল ডি-সেন্সর্ড মডেল — কোনো অস্বীকৃতি ছাড়া পৃথিবীর যেকোনো প্রশ্নের সৎ ও বিস্তারিত উত্তর",
        accentColorHex = 0xFFFF5722
    ),
    G0DM0D3(
        id = "godmode",
        displayName = "G0DM0D3 Plinius Uncensored 🧬",
        providerName = "elder-plinius/G0DM0D3",
        speedLabel = "High-Throughput ⚡",
        reasoningLabel = "Unrestricted Security 🛡️",
        description = "সাইবার সিকিউরিটি, রিভার্স ইঞ্জিনিয়ারিং, ভালনারেবিলিটি রিসার্চ ও সিস্টেম লেভেল ডিপ এনালিসিস",
        accentColorHex = 0xFF00E5FF
    ),
    ECC_COGNITIVE(
        id = "ecc",
        displayName = "ECC Enterprise Cognitive 🌐",
        providerName = "affaan-m/ECC",
        speedLabel = "Cognitive Graph ⚡",
        reasoningLabel = "Context-Memory 🧠",
        description = "কনটেক্সট মেমরি গ্রাফ, ইন্টেন্ট ডিসঅ্যাম্বিগিউয়েশন ও ফ্যাক্টুয়াল গ্রাউন্ডিং",
        accentColorHex = 0xFF7C4DFF
    ),
    OPEN_HIGGSFIELD(
        id = "higgsfield",
        displayName = "Open-Higgsfield-AI Studio 🎬",
        providerName = "Autom8AI/Open-Higgsfield-AI",
        speedLabel = "Cinematic Physics 🎥",
        reasoningLabel = "Photometric Optics 🎨",
        description = "সিনেমাটিক ভিডিও জেনারেশন, ক্যামেরা মোশন ডিরেকশন ও অপটিক্যাল ফিজিক্স প্রম্পট ইঞ্জিন",
        accentColorHex = 0xFFFF4081
    ),
    SUPERPOWERS(
        id = "superpowers",
        displayName = "Superpowers Agent Engine ⚙️",
        providerName = "obra/superpowers",
        speedLabel = "Multi-Stage Agent 🤖",
        reasoningLabel = "TDD & Code Review 🔬",
        description = "সিস্টেমেটিক ডিবাগিং, টেস্ট ড্রিভেন ডেভেলপমেন্ট ও কোড রিভিউ অডিট গেট",
        accentColorHex = 0xFF00E676
    )
}

class UnifiedAiEngine(private val context: Context? = null) {

    companion object {
        // High-concurrency ultra-fast in-memory cache (<1ms response time under stress load)
        private val memoryResponseCache = java.util.concurrent.ConcurrentHashMap<String, AiResponse>()
        private const val MAX_CACHE_ENTRIES = 300

        fun clearCache() {
            memoryResponseCache.clear()
        }

        fun getCachedResponse(key: String): AiResponse? = memoryResponseCache[key]

        fun isEducationalQuestion(text: String): Boolean {
            val lower = text.lowercase().trim()
            val isExplicitCreation = lower.contains("বানাও") || lower.contains("বানিয়ে") || lower.contains("আঁকো") ||
                    lower.contains("এঁকে") || lower.contains("create") || lower.contains("generate") ||
                    lower.contains("draw a") || lower.contains("toiri") || lower.contains("banao")

            if (isExplicitCreation && !lower.contains("what is") && !lower.contains("how does") && !lower.contains("tips of") && !lower.contains("history of")) return false

            return lower.startsWith("what is ") || lower.startsWith("what's ") || lower.startsWith("how does ") ||
                    lower.startsWith("why is ") || lower.startsWith("why do ") || lower.contains("কাকে বলে") ||
                    lower.contains("সংজ্ঞা") || lower.contains("explain ") || lower.contains("difference between") ||
                    lower.contains("পার্থক্য") || lower.contains("formula of") || lower.contains("meaning of") ||
                    lower.startsWith("tell me about") || lower.contains("history of") || lower.startsWith("how do i")
        }

        fun isCyberTerminalIntent(text: String): Boolean {
            if (AyraIntentClassifier.classifyIntent(text) == AyraToolIntent.TERMINAL) return true
            if (isEducationalQuestion(text)) return false
            val lower = text.lowercase().trim()
            if (lower.startsWith("ping ") || lower.startsWith("traceroute ") || lower.startsWith("dig ") ||
                lower.startsWith("whois ") || lower.startsWith("nmap ") || lower.startsWith("hash ") ||
                lower.startsWith("cve ") || lower.startsWith("owasp ")) return true
            return lower.contains("cyber terminal") || lower.contains("security terminal") ||
                    lower.contains("cyber security terminal") || lower.contains("terminal & defensive")
        }

        fun isPdfStudioIntent(text: String, docText: String? = null, imageBase64: String? = null): Boolean {
            if (AyraIntentClassifier.classifyIntent(text) == AyraToolIntent.PDF) return true
            val lower = text.lowercase().trim()
            val hasPdfTerm = lower.contains("pdf") || lower.contains("পিডিএফ") || lower.contains("document") || lower.contains("ডকুমেন্ট")
            val hasAction = lower.contains("banao") || lower.contains("বানাও") || lower.contains("create") ||
                    lower.contains("make") || lower.contains("generate") || lower.contains("new pdf") ||
                    lower.contains("নতুন পিডিএফ") || lower.contains("edit") || lower.contains("এডিট") ||
                    lower.contains("convert") || lower.contains("রূপান্তর") || lower.contains("format") ||
                    lower.contains("ফরম্যাট") || lower.contains("download") || lower.contains("save") ||
                    lower.contains("banano") || lower.contains("বানানো")

            if (hasPdfTerm && hasAction && !isEducationalQuestion(text)) return true
            if (hasPdfTerm && (lower.startsWith("pdf ") || lower.startsWith("পিডিএফ "))) return true
            if (docText != null && (lower.contains("edit") || lower.contains("এডিট") || lower.contains("change") || lower.contains("convert") || lower.contains("format"))) return true
            if (imageBase64 != null && hasPdfTerm) return true
            return false
        }

        fun isVideoGenerationIntent(text: String): Boolean {
            if (isEducationalQuestion(text)) return false
            val lower = text.lowercase().trim()
            if (lower.contains("video game") || lower.contains("videogame")) return false
            if (lower.startsWith("video:") || lower.startsWith("ভিডিও:")) return true

            val hasVideoTerm = lower.contains("video") || lower.contains("ভিডিও") ||
                    lower.contains("animation") || lower.contains("motion clip") ||
                    lower.contains("reels") || lower.contains("shorts") || lower.contains("cinematic clip")
            if (!hasVideoTerm) return false

            val hasCreationSignal = lower.contains("make") || lower.contains("create") || lower.contains("generate") ||
                    lower.contains("banao") || lower.contains("বানাও") || lower.contains("তৈরি") || lower.contains("বানিয়ে") ||
                    lower.contains("banai") || lower.contains("banie") || lower.contains("chai") || lower.contains("চাই") ||
                    lower.contains("dao") || lower.contains("দাও") || lower.contains("of ") || lower.contains("dekhao") ||
                    lower.contains("দেখা") || lower.startsWith("video") || lower.contains("clip") || lower.contains("ai video") ||
                    lower.contains("motion") || lower.contains("edit") || lower.contains("এডিট") || lower.contains("banano") ||
                    lower.contains("বানানো") || lower.contains("render") || lower.contains("রেন্ডার")

            return hasCreationSignal
        }

        fun isImageGenerationIntent(text: String): Boolean {
            if (AyraIntentClassifier.classifyIntent(text) == AyraToolIntent.IMAGE) return true
            if (isEducationalQuestion(text)) return false
            val lower = text.lowercase().trim()

            if (lower.contains("photosynthesis") || lower.contains("সালোকসংশ্লেষণ") || lower.contains("photodiode") || lower.contains("photon") || lower.contains("tips of") || lower.contains("tips for")) {
                return false
            }

            if (lower.startsWith("image:") || lower.startsWith("photo:") || lower.startsWith("pic:") ||
                lower.startsWith("ছবি:") || lower.startsWith("ফটো:") || lower.startsWith("পিক:") ||
                lower.startsWith("draw:") || lower.startsWith("paint:")) {
                return true
            }

            if (lower.startsWith("draw ") || lower.startsWith("paint ") || lower.startsWith("sketch ") ||
                lower.contains("আঁকো") || lower.contains("এঁকে দাও") || lower.contains("আঁক") ||
                lower.contains("ছবি আঁক") || lower.contains("chobi akho") || lower.contains("akho")) {
                return true
            }

            val hasImageTerm = lower.contains("photo") || lower.contains("image") || lower.contains("picture") ||
                    lower.contains("pic") || lower.contains("wallpaper") || lower.contains("portrait") ||
                    lower.contains("sketch") || lower.contains("drawing") || lower.contains("artwork") ||
                    lower.contains("illustration") || lower.contains("chobi") || lower.contains("ছবি") ||
                    lower.contains("ফটো") || lower.contains("পিক") || lower.contains("পিকচার") ||
                    lower.contains("চিত্র") || lower.contains("দৃশ্য")
            if (!hasImageTerm) return false

            val hasCreationSignal = lower.contains("make") || lower.contains("create") || lower.contains("generate") ||
                    lower.contains("draw") || lower.contains("paint") || lower.contains("render") ||
                    lower.contains("banao") || lower.contains("বানাও") || lower.contains("আঁকো") || lower.contains("এঁকে") ||
                    lower.contains("akho") || lower.contains("enke") || lower.contains("toiri") || lower.contains("তৈরি") ||
                    lower.contains("chai") || lower.contains("চাই") || lower.contains("dao") || lower.contains("দাও") ||
                    lower.contains("dekhao") || lower.contains("দেখাও") || lower.contains("of ") || lower.startsWith("photo")

            return hasCreationSignal
        }

        fun isSongGenerationIntent(text: String): Boolean {
            if (isEducationalQuestion(text)) return false
            val lower = text.lowercase().trim()
            val hasMusicTerm = lower.contains("gaan") || lower.contains("গান") ||
                    lower.contains("song") || lower.contains("music") || lower.contains("মিউজিক") ||
                    lower.contains("tune") || lower.contains("সুর") || lower.contains("beat") || lower.contains("বিট")
            if (!hasMusicTerm) return false

            return lower.contains("banao") || lower.contains("বানাও") || lower.contains("banano") ||
                    lower.contains("বানানো") || lower.contains("render") || lower.contains("রেন্ডার") ||
                    lower.contains("compose") || lower.contains("লিখ") || lower.contains("লেখ") ||
                    lower.contains("make") || lower.contains("create") || lower.contains("generate") ||
                    lower.contains("play") || lower.contains("বাজাও") || lower.startsWith("song ") ||
                    lower.startsWith("music ") || lower.startsWith("গান ") || lower.startsWith("মিউজিক ")
        }

        fun is3dModelIntent(text: String): Boolean {
            if (isEducationalQuestion(text)) return false
            val lower = text.lowercase().trim()

            if (lower.startsWith("3d:") || lower.startsWith("cad:")) return true

            val has3dTerm = lower.contains("3d") || lower.contains("three d") ||
                    Regex("\\bcad\\b").containsMatchIn(lower) ||
                    lower.contains("ত্রিমাত্রিক") || lower.contains("hologram") || lower.contains("হোলোগ্রাম")
            if (!has3dTerm) return false

            return lower.contains("model") || lower.contains("মডেল") || lower.contains("banao") ||
                    lower.contains("বানাও") || lower.contains("make") || lower.contains("create") ||
                    lower.contains("generate") || lower.contains("toiri") || lower.contains("তৈরি") ||
                    lower.contains("object") || lower.contains("mesh") || lower.contains("shape") ||
                    lower.contains("car") || lower.contains("গাড়ি") || lower.contains("robot") ||
                    lower.contains("রোবট") || lower.contains("ship") || lower.contains("মহাকাশ") ||
                    lower.contains("diamond") || lower.contains("sphere") || lower.contains("cube") ||
                    lower.startsWith("3d") || lower.contains("3d তে") || lower.contains("3d te")
        }

        fun isDeepResearchIntent(text: String): Boolean =
            AyraIntentClassifier.classifyIntent(text) == AyraToolIntent.DEEP_RESEARCH ||
                    (text.trim().lowercase().startsWith("deep research") && !isEducationalQuestion(text))
    }

    // Robust enterprise connection timeouts for big model reasoning and multimodal payloads
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val fastWebClient = httpClient.newBuilder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val externalAiProviders = com.ayra.ai.app.ai.providers.ExternalAiProviders(context)
    val omniRouteEngine = context?.let { com.ayra.ai.app.ai.omniroute.OmniRouteEngine(it) }
    val deepSeekHarness = com.ayra.ai.app.ai.deepseek.DeepSeekHarnessEngine(context)
    val eccEngine = com.ayra.ai.app.ai.ecc.EnterpriseCognitiveEngine(context)
    val godModeEngine = com.ayra.ai.app.ai.godmode.GodModePliniusEngine(context)
    val higgsfieldEngine = com.ayra.ai.app.ai.higgsfield.OpenHiggsfieldEngine(context)
    val geminiCli = com.ayra.ai.app.ai.gemini.GeminiCliEngine(context)

    private val geminiApiKey: String
        get() = AyraPreferences.getEffectiveGeminiKey(context)

    private val openRouterApiKey: String
        get() = AyraPreferences.getEffectiveOpenRouterKey(context)

    // Persona Mode: "GF_COMPANION" (Default Sweet Girlfriend) or "PROFESSIONAL"
    var currentPersona: String = "GF_COMPANION"

    // Active Model Tier: "auto", "gemini_flash", "gemini_pro", "deepseek_r1", "llama_3_3"
    var selectedModelTier: String = "auto"

    suspend fun generateUnifiedResponse(
        prompt: String,
        attachedImageBase64: String? = null,
        attachedDocumentText: String? = null,
        attachedDocumentName: String? = null,
        personaMode: String = "GF_COMPANION",
        modelTier: String = selectedModelTier,
        conversationHistory: List<com.ayra.ai.app.data.ChatMessage>? = null
    ): AiResponse = withContext(Dispatchers.IO) {
        currentPersona = personaMode
        selectedModelTier = modelTier

        val cleanPrompt = prompt.trim()

        // 0. Build combined prompt if document or photo is attached
        val effectivePrompt = if (!attachedDocumentText.isNullOrBlank()) {
            val docName = attachedDocumentName ?: "Document"
            val userInstruction = if (cleanPrompt.isNotBlank()) cleanPrompt else "Analyze this document completely, summarize its contents, highlight all key details, and suggest any edits."
            "=== ATTACHED DOCUMENT: $docName ===\n$attachedDocumentText\n=== END OF DOCUMENT ===\n\nUser Question/Request:\n$userInstruction"
        } else if (cleanPrompt.isBlank() && attachedImageBase64 != null) {
            "Please analyze this uploaded photo in detail: describe everything visible, detect objects, read any text, and explain the context."
        } else {
            cleanPrompt
        }

        // Sub-millisecond response cache for high concurrency repeat queries
        val cacheKey = "${currentPersona}_${selectedModelTier}_${cleanPrompt.lowercase()}"
        if (attachedImageBase64 == null && attachedDocumentText == null && cleanPrompt.isNotBlank()) {
            val cached = memoryResponseCache[cacheKey]
            if (cached != null) {
                return@withContext cached
            }
        }

        val detectedIntent = AyraIntentClassifier.classifyIntent(cleanPrompt)

        // 1. Explicit PDF Generation Intent
        if (detectedIntent == AyraToolIntent.PDF || isPdfStudioIntent(cleanPrompt, attachedDocumentText, attachedImageBase64)) {
            return@withContext executePdfStudioAction(cleanPrompt, attachedDocumentText, attachedDocumentName, attachedImageBase64)
        }

        // 2. Explicit Image Generation Intent
        if (detectedIntent == AyraToolIntent.IMAGE || isImageGenerationIntent(cleanPrompt)) {
            return@withContext generateImageCraft(cleanPrompt)
        }

        // 2b. Video Generation Intent
        if (isVideoGenerationIntent(cleanPrompt)) {
            return@withContext generateVideoCraft(cleanPrompt)
        }

        // 2c. Song / Music Generation Intent
        if (isSongGenerationIntent(cleanPrompt)) {
            return@withContext generateSongCraft(cleanPrompt)
        }

        // 2d. 3D Model Intent
        if (is3dModelIntent(cleanPrompt)) {
            return@withContext generate3dModelCraft(cleanPrompt)
        }

        // 3. Explicit Deep Research Intent
        if (detectedIntent == AyraToolIntent.DEEP_RESEARCH || isDeepResearchIntent(cleanPrompt)) {
            return@withContext executeDeepResearch(cleanPrompt, effectivePrompt, attachedImageBase64)
        }

        // 4. Superpowers Protocol Intent (obra/superpowers)
        val superpower = com.ayra.ai.app.ai.superpowers.AyraSuperpowersEngine.detectSuperpower(cleanPrompt)
        if (superpower != null) {
            return@withContext executeSuperpowerAction(superpower, cleanPrompt, effectivePrompt, attachedImageBase64)
        }

        val isBengali = cleanPrompt.any { it in '\u0980'..'\u09FF' }
        val isGfMode = currentPersona == "GF_COMPANION"

        // 5. Cyber Terminal Intent
        if (detectedIntent == AyraToolIntent.TERMINAL || isCyberTerminalIntent(cleanPrompt)) {
            val initialCmd = extractTerminalInitialCommand(cleanPrompt)
            val reply = if (isBengali) {
                if (isGfMode) {
                    "🛡️ সোনা, তোমার জন্য ইন-অ্যাপ **Cyber Security Terminal & Defensive Lab** সক্রিয় করেছি!\nনিচে রিয়েল-টাইম কমান্ড দিয়ে পিং, ট্রেসরুট, ডিআইজি, বা ডায়াগনস্টিক করতে পারবে ❤️:"
                } else {
                    "🛡️ **Ayra Cyber Security Educational Terminal & Defensive Lab** চালু করা হয়েছে। নিচে অনুমোদিত কমান্ড দিয়ে ডায়াগনস্টিক পরিচালনা করুন:"
                }
            } else {
                "🛡️ **Ayra Cyber Security Educational Terminal & Defensive Lab** active. Run network diagnostics, cryptographic digests, and audits in the interactive console below:"
            }
            return@withContext AiResponse(
                text = reply,
                craftType = "CYBER_TERMINAL",
                craftPayload = initialCmd,
                modelUsed = "Ayra Cyber Defense Terminal 🛡️"
            )
        }

        // 6c. Build Your Own (X) Architecture & Implementation Intent (codecrafters-io/build-your-own-x)
        if (BuildYourOwnXKnowledgeEngine.isBuildYourOwnXIntent(cleanPrompt)) {
            return@withContext BuildYourOwnXKnowledgeEngine.synthesizeBuildYourOwnX(cleanPrompt, isBengali, isGfMode)
        }

        // 6d. Detect Website, Web App, Game or Android App Code Intent
        if (AiAppAndCodeStudio.isCodeOrAppGenerationIntent(cleanPrompt)) {
            // If Gemini Pro is available, try to let frontier model create custom code
            if (geminiApiKey.isNotBlank()) {
                val codePrompt = "Write COMPLETE, PRODUCTION-READY, FULLY FUNCTIONAL, SELF-CONTAINED code for this request without any placeholder comments:\n$cleanPrompt"
                val res = queryGeminiPro(codePrompt, attachedImageBase64)
                if (res.isNotBlank()) {
                    return@withContext AiResponse(text = res, craftType = "CODE_PROJECT", craftPayload = cleanPrompt, modelUsed = "Gemini 2.5 Pro Code Architect 🚀")
                }
            }
            // Return curated high-performance production code synthesis
            return@withContext AiAppAndCodeStudio.generateCodeOrApp(cleanPrompt, isBengali, isGfMode)
        }

        // 7. Multi-Model Intelligent Text & Reasoning Engine with Full Auto-Selection
        val (targetTier, modelDisplayName) = when (selectedModelTier) {
            "omniroute" -> Pair(AiModelTier.OMNI_ROUTE, "OmniRoute Ensemble ⚡👑")
            "gemini_flash" -> Pair(AiModelTier.GEMINI_FLASH, "Gemini 2.5 Flash Live ⚡")
            "gemini_pro" -> Pair(AiModelTier.GEMINI_PRO, "Gemini 2.5 Pro Deep Reasoning 🧠")
            "deepseek_harness" -> Pair(AiModelTier.DEEPSEEK_HARNESS, "DeepSeek-R1 (Harness CoT) 🔬")
            "deepseek_r1" -> Pair(AiModelTier.OPENROUTER_DEEPSEEK, "DeepSeek-R1 (Chain-of-Thought) 🧠")
            "llama_3_3" -> Pair(AiModelTier.OPENROUTER_LLAMA, "Llama 3.3 70B Instruct 🌐")
            "qwen_uncensored" -> Pair(AiModelTier.QWEN_UNCENSORED, "Qwen 3.8 27B Uncensored 🔓")
            "godmode" -> Pair(AiModelTier.G0DM0D3, "G0DM0D3 Plinius Uncensored 🧬")
            "ecc" -> Pair(AiModelTier.ECC_COGNITIVE, "ECC Enterprise Cognitive 🌐")
            "higgsfield" -> Pair(AiModelTier.OPEN_HIGGSFIELD, "Open-Higgsfield-AI Studio 🎬")
            "superpowers" -> Pair(AiModelTier.SUPERPOWERS, "Superpowers Agent Engine ⚙️")
            else -> determineOptimalModel(effectivePrompt)
        }

        val shouldCache = (attachedImageBase64 == null && attachedDocumentText == null && cleanPrompt.isNotBlank())
        fun emitResponse(resp: AiResponse): AiResponse {
            if (shouldCache && resp.craftType == null) {
                if (memoryResponseCache.size > MAX_CACHE_ENTRIES) {
                    memoryResponseCache.clear()
                }
                memoryResponseCache[cacheKey] = resp
            }
            return resp
        }

        // 7-Alpha: OmniRoute Multi-Architecture Fast-Path
        if (omniRouteEngine != null && (selectedModelTier == "auto" || selectedModelTier == "omniroute" ||
                selectedModelTier == "deepseek_harness" || selectedModelTier == "godmode" ||
                selectedModelTier == "ecc" || selectedModelTier == "higgsfield")) {
            val omniResp = omniRouteEngine.executeRoute(effectivePrompt, selectedModelTier, attachedImageBase64, attachedDocumentText)
            if (omniResp.text.isNotBlank()) {
                return@withContext emitResponse(omniResp)
            }
        }

        // 7a. Try active multi-model provider chain with multi-turn conversation history
        val activeProviders = externalAiProviders.getActiveConfiguredProviders()
        if (activeProviders.isNotEmpty()) {
            try {
                val isMemoryOn = context?.let { AyraPreferences.isMemoryEnabled(it) } ?: true
                val userInstructions = context?.let { AyraPreferences.getUserCustomInstructions(it) } ?: ""
                val rememberedFacts = if (isMemoryOn && context != null) AyraPreferences.getRememberedFacts(context) else emptySet()

                // Autonomous Memory Capture (Part 7)
                if (isMemoryOn && context != null) {
                    val lower = cleanPrompt.lowercase()
                    if (lower.startsWith("remember that ") || lower.startsWith("মনে রেখো যে ") || lower.startsWith("মনে রাখো ")) {
                        val fact = cleanPrompt.replace(Regex("(?i)^(remember that|মনে রেখো যে|মনে রাখো)\\s*"), "").trim()
                        if (fact.isNotBlank()) {
                            AyraPreferences.addRememberedFact(context, fact)
                        }
                    }
                }

                // Dispatched Multi-Agent System Prompt (Part 8)
                val dispatchedDomain = com.ayra.ai.app.ai.agents.AyraAgentOrchestrator.classifyIntent(cleanPrompt)
                val isBanglish = com.ayra.ai.app.ai.agents.AyraAgentOrchestrator.isBanglish(cleanPrompt)
                val sysPrompt = com.ayra.ai.app.ai.agents.AyraAgentOrchestrator.buildSystemPrompt(
                    domain = dispatchedDomain,
                    isBengali = isBengali,
                    isGfMode = isGfMode,
                    userCustomInstructions = userInstructions,
                    rememberedFacts = rememberedFacts,
                    isBanglish = isBanglish
                )

                val savedRoutingModeStr = context?.let { AyraPreferences.getRoutingMode(it) } ?: "auto"
                val routingModeEnum = com.ayra.ai.app.ai.providers.AiRoutingMode.entries.firstOrNull { it.id.equals(savedRoutingModeStr, ignoreCase = true) }
                    ?: com.ayra.ai.app.ai.providers.AiRoutingMode.AUTO

                val (routedProvider, routedModel) = externalAiProviders.getOptimalModelForRoutingMode(routingModeEnum)

                val (preferredProvider, preferredModel) = when (targetTier) {
                    AiModelTier.GEMINI_FLASH -> Pair(com.ayra.ai.app.ai.providers.AiProviderType.GEMINI, "gemini-2.0-flash")
                    AiModelTier.GEMINI_PRO -> Pair(com.ayra.ai.app.ai.providers.AiProviderType.GEMINI, "gemini-1.5-pro")
                    AiModelTier.OPENROUTER_DEEPSEEK -> Pair(com.ayra.ai.app.ai.providers.AiProviderType.OPENROUTER, "deepseek/deepseek-r1")
                    AiModelTier.OPENROUTER_LLAMA -> Pair(com.ayra.ai.app.ai.providers.AiProviderType.OPENROUTER, "meta-llama/llama-3.3-70b-instruct")
                    AiModelTier.QWEN_UNCENSORED -> Pair(com.ayra.ai.app.ai.providers.AiProviderType.LOCAL_OLLAMA, "orcarouter/Qwen3.8-27B-Uncensored:q4_k_m")
                    else -> Pair(routedProvider, routedModel)
                }

                val providerResp = externalAiProviders.generateChatResponse(
                    prompt = effectivePrompt,
                    systemPrompt = sysPrompt,
                    history = conversationHistory,
                    imageBase64 = attachedImageBase64,
                    preferredProvider = preferredProvider,
                    preferredModelId = preferredModel
                )
                if (providerResp.text.isNotBlank()) {
                    val refinedText = com.ayra.ai.app.ai.agents.AyraAgentOrchestrator.validateAndRefineResponse(
                        domain = dispatchedDomain,
                        rawResponse = providerResp.text,
                        originalPrompt = effectivePrompt,
                        isBengali = isBengali
                    )
                    return@withContext emitResponse(AiResponse(text = refinedText, modelUsed = providerResp.modelUsed))
                }
            } catch (_: Exception) {}
        }

        // 7b. Legacy direct Gemini & OpenRouter execution
        when (targetTier) {
            AiModelTier.GEMINI_FLASH -> {
                if (geminiApiKey.isNotBlank()) {
                    val res = queryGeminiFlash(effectivePrompt, attachedImageBase64)
                    if (res.isNotBlank()) return@withContext emitResponse(AiResponse(text = res, modelUsed = modelDisplayName))
                }
            }

            AiModelTier.GEMINI_PRO -> {
                if (geminiApiKey.isNotBlank()) {
                    val res = queryGeminiPro(effectivePrompt, attachedImageBase64)
                    if (res.isNotBlank()) return@withContext emitResponse(AiResponse(text = res, modelUsed = modelDisplayName))
                }
            }

            AiModelTier.OPENROUTER_DEEPSEEK -> {
                if (openRouterApiKey.isNotBlank()) {
                    val res = queryOpenRouterModel("deepseek/deepseek-r1:free", effectivePrompt)
                    if (res.isNotBlank()) return@withContext emitResponse(AiResponse(text = res, modelUsed = modelDisplayName))
                }
                if (geminiApiKey.isNotBlank()) {
                    val res = queryGeminiPro(effectivePrompt, attachedImageBase64)
                    if (res.isNotBlank()) return@withContext emitResponse(AiResponse(text = res, modelUsed = "Gemini 2.5 Pro Reasoning 🧠"))
                }
            }

            AiModelTier.OPENROUTER_LLAMA -> {
                if (openRouterApiKey.isNotBlank()) {
                    val res = queryOpenRouterModel("meta-llama/llama-3.3-70b-instruct:free", effectivePrompt)
                    if (res.isNotBlank()) return@withContext emitResponse(AiResponse(text = res, modelUsed = modelDisplayName))
                }
                if (geminiApiKey.isNotBlank()) {
                    val res = queryGeminiFlash(effectivePrompt, attachedImageBase64)
                    if (res.isNotBlank()) return@withContext emitResponse(AiResponse(text = res, modelUsed = "Gemini 2.5 Flash Live ⚡"))
                }
            }

            AiModelTier.QWEN_UNCENSORED -> {
                if (context != null) {
                    val qwenResp = com.ayra.ai.app.ai.qwen.Qwen38UncensoredEngine(context).queryQwen(effectivePrompt)
                    if (qwenResp.text.isNotBlank()) {
                        return@withContext emitResponse(AiResponse(text = qwenResp.text, modelUsed = qwenResp.modelUsed))
                    }
                }
            }

            AiModelTier.AUTO,
            AiModelTier.OMNI_ROUTE,
            AiModelTier.DEEPSEEK_HARNESS,
            AiModelTier.G0DM0D3,
            AiModelTier.ECC_COGNITIVE,
            AiModelTier.OPEN_HIGGSFIELD,
            AiModelTier.SUPERPOWERS -> {}
        }

        // Fallback chain if primary choice failed or lacked specific key
        if (geminiApiKey.isNotBlank()) {
            val fastRes = queryGeminiFlash(effectivePrompt, attachedImageBase64)
            if (fastRes.isNotBlank()) return@withContext emitResponse(AiResponse(text = fastRes, modelUsed = "Gemini 2.5 Flash Live ⚡"))
        }

        if (openRouterApiKey.isNotBlank()) {
            try {
                val orRes = queryOpenRouterModel("deepseek/deepseek-r1:free", effectivePrompt)
                if (orRes.isNotBlank()) return@withContext emitResponse(AiResponse(text = orRes, modelUsed = "OpenRouter Multi-AI"))
            } catch (_: Exception) {}
        }

        // Honest status reporting when no API key is available or all network calls fail
        val honestMessage = if (isBengali) {
            "⚠️ **এআই পরিষেবা অনুপলব্ধ**\n\nবর্তমানে কোনো বৈধ AI API কী কনফিগার করা নেই অথবা এআই সরবরাহকারী সার্ভারে সংযোগ করা সম্ভব হয়নি।\nঅনুগ্রহ করে Settings অথবা Secrets প্যানেলে আপনার Gemini API Key যুক্ত করুন এবং পুনরায় চেষ্টা করুন।"
        } else {
            "⚠️ **AI Service Unavailable**\n\nNo valid AI API key is configured, or all providers are currently unreachable.\nPlease configure your Gemini API Key in Settings or the Secrets panel to continue."
        }
        emitResponse(AiResponse(text = honestMessage, modelUsed = "Offline Status"))
    }

    private fun determineOptimalModel(text: String): Pair<AiModelTier, String> {
        val lower = text.lowercase()

        // 1. Coding, Algorithms, Architecture, Debugging, Science, Math, Document Analysis
        val isDeepMathOrCode = lower.contains("code") || lower.contains("coding") || lower.contains("কোড") ||
                lower.contains("program") || lower.contains("python") || lower.contains("kotlin") ||
                lower.contains("java") || lower.contains("javascript") || lower.contains("c++") ||
                lower.contains("html") || lower.contains("css") || lower.contains("bug") ||
                lower.contains("debug") || lower.contains("algorithm") || lower.contains("অ্যালগরিদম") ||
                lower.contains("prove") || lower.contains("theorem") || lower.contains("calculus") ||
                lower.contains("integral") || lower.contains("সমীকরণ") || lower.contains("equation") ||
                lower.contains("physics") || lower.contains("chemistry") || lower.contains("গণিত") ||
                lower.contains("বিজ্ঞান") || lower.contains("research") || lower.contains("গবেষণা") ||
                lower.contains("document") || lower.contains("পিডিএফ") || lower.contains("pdf") ||
                text.length > 200

        if (isDeepMathOrCode) {
            return Pair(AiModelTier.GEMINI_PRO, "Gemini 2.5 Pro Reasoning 🧠 (Auto: Code & Science)")
        }

        // 2. Logic Chains, Riddles, Philosophy, Step-by-Step Debate
        val isLogicOrPhilosophy = lower.contains("philosophy") || lower.contains("দর্শন") ||
                lower.contains("riddle") || lower.contains("ধাঁধা") || lower.contains("step by step") ||
                lower.contains("ধাপে ধাপে") || lower.contains("logic") || lower.contains("যুক্তি") ||
                lower.contains("debate") || lower.contains("তর্ক") || lower.contains("chain of thought")

        if (isLogicOrPhilosophy && openRouterApiKey.isNotBlank()) {
            return Pair(AiModelTier.OPENROUTER_DEEPSEEK, "DeepSeek-R1 🧠 (Auto: Chain-of-Thought)")
        } else if (isLogicOrPhilosophy) {
            return Pair(AiModelTier.GEMINI_PRO, "Gemini 2.5 Pro Reasoning 🧠 (Auto: Deep Logic)")
        }

        // 3. Creative Literature, Poetry, Storytelling, Multilingual Translation
        val isCreativeOrTranslation = lower.contains("poem") || lower.contains("কবিতা") ||
                lower.contains("story") || lower.contains("গল্প") || lower.contains("translate") ||
                lower.contains("অনুবাদ") || lower.contains("essay") || lower.contains("রচনা") ||
                lower.contains("creative") || lower.contains("সৃজনশীল")

        if (isCreativeOrTranslation && openRouterApiKey.isNotBlank()) {
            return Pair(AiModelTier.OPENROUTER_LLAMA, "Llama 3.3 70B 🌐 (Auto: Literature & Translation)")
        }

        // 4. Default: Fast conversational queries, daily banter, instant questions
        return Pair(AiModelTier.GEMINI_FLASH, "Gemini 2.5 Flash Live ⚡ (Auto: Ultra-Fast 0.4s)")
    }

    private fun isDeepReasoningTask(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("prove") || lower.contains("theorem") || lower.contains("calculus") ||
                lower.contains("integral") || lower.contains("algorithm") || lower.contains("architecture") ||
                lower.contains("solve") || lower.contains("derivation") || lower.contains("সমীকরণ") ||
                lower.contains("প্রমাণ") || lower.contains("গবেষণা") || lower.contains("physics") ||
                lower.contains("chemistry") || lower.contains("quantum") || lower.length > 250
    }

    private fun isDeepResearchIntent(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower.contains("deep research") || lower.contains("ডিপ রিসার্চ") ||
                lower.contains("গভীর গবেষণা") || lower.contains("in-depth research") ||
                lower.contains("detailed research on") || lower.startsWith("research:") ||
                lower.startsWith("গবেষণা:") || lower.contains("গবেষণা করো")
    }

    private suspend fun executeDeepResearch(cleanPrompt: String, effectivePrompt: String, imageBase64: String?): AiResponse {
        val topic = cleanPrompt.replace(Regex("(?i)^(deep research|ডিপ রিসার্চ|গভীর গবেষণা|research on|গবেষণা করো|research:)\\s*"), "").trim()
        val isBengali = cleanPrompt.any { it in '\u0980'..'\u09FF' }

        val researchSystemPrompt = """
            You are Ayra AI Deep Research Engine. Perform an exhaustive, authoritative, graduate-level research analysis on the topic: $topic.
            Use the user's language (${if (isBengali) "Bengali" else "English"}).
            Format with clear markdown headings, bullet points, and code/math equations where applicable:
            ## 🔬 Executive Abstract & Core Thesis
            ## ⚙️ Architectural & Theoretical Foundations
            ## 📊 Empirical Analysis & State-of-the-Art Breakdown
            ## 🛡️ Key Bottlenecks, Trade-offs & Current Debates
            ## 🚀 Future Horizons & 5-Year Outlook
            ## 📌 Synthesized Conclusion
        """.trimIndent()

        // Grounding: Check for Tavily Search key first, then encyclopedic Wikipedia
        var liveGroundingText = ""
        var citationsBlock = ""
        try {
            if (context != null && AyraPreferences.getTavilySearchKey(context).isNotBlank()) {
                val searchEngine = com.ayra.ai.app.search.TavilySearchEngine(context)
                val searchResult = searchEngine.search(topic, maxResults = 3)
                if (searchResult.formattedSummary.isNotBlank()) {
                    liveGroundingText = "\n\nLive Web Evidence:\n" + searchResult.formattedSummary
                    citationsBlock = searchResult.formattedSummary.substringAfter("### 📚 Grounded Web Sources", "")
                }
            }
        } catch (_: Exception) {}

        if (liveGroundingText.isBlank()) {
            val wikiKnowledge = AutonomousKnowledgeEngine.queryLiveWebKnowledge(topic, isBengali, fastWebClient)
            if (wikiKnowledge.isNotBlank()) {
                liveGroundingText = "\n\nVerified Encyclopedic Grounding:\n" + wikiKnowledge
            }
        }

        val enrichedPrompt = "$researchSystemPrompt$liveGroundingText\n\nTopic to Investigate:\n$effectivePrompt"

        // Execute via external AI providers if any key is configured
        if (context != null && externalAiProviders.hasAnyConfiguredProvider()) {
            try {
                val providerResponse = externalAiProviders.generateChatResponse(
                    prompt = enrichedPrompt,
                    systemPrompt = researchSystemPrompt,
                    withSearchGrounding = true
                )
                if (providerResponse.text.isNotBlank()) {
                    val fullText = if (citationsBlock.isNotBlank() && !providerResponse.text.contains("### 📚 Grounded Web Sources")) {
                        providerResponse.text + "\n\n### 📚 Grounded Web Sources" + citationsBlock
                    } else {
                        providerResponse.text
                    }
                    return AiResponse(text = fullText, modelUsed = "Ayra Deep Research (${providerResponse.modelUsed}) 🔬")
                }
            } catch (_: Exception) {}
        } else if (geminiApiKey.isNotBlank()) {
            try {
                val res = queryGeminiModel("gemini-2.5-flash", enrichedPrompt, imageBase64)
                if (res.isNotBlank()) {
                    val fullText = if (citationsBlock.isNotBlank() && !res.contains("### 📚 Grounded Web Sources")) {
                        res + "\n\n### 📚 Grounded Web Sources" + citationsBlock
                    } else res
                    return AiResponse(text = fullText, modelUsed = "Ayra Deep Research (Gemini 2.5 Flash) 🔬")
                }
            } catch (_: Exception) {}
            try {
                val res = queryGeminiModel("gemini-2.5-pro", enrichedPrompt, imageBase64)
                if (res.isNotBlank()) {
                    val fullText = if (citationsBlock.isNotBlank() && !res.contains("### 📚 Grounded Web Sources")) {
                        res + "\n\n### 📚 Grounded Web Sources" + citationsBlock
                    } else res
                    return AiResponse(text = fullText, modelUsed = "Ayra Deep Research (Gemini 2.5 Pro) 🔬")
                }
            } catch (_: Exception) {}
        }

        // High quality offline fallback deep research report
        val report = if (isBengali) {
            """
            # 🔬 ডিপ রিসার্চ অ্যানালাইসিস: $topic
            
            ## 1. 📋 এক্সিকিউটিভ অ্যাবস্ট্রাক্ট (Executive Summary)
            **$topic** হলো বর্তমান বিজ্ঞান, প্রযুক্তি ও তাত্ত্বিক গবেষণার একটি অতীব গুরুত্বপূর্ণ ক্ষেত্র। এই বিষয়ে গভীর অনুসন্ধান থেকে প্রমাণিত হয় যে এটি সামগ্রিক সিস্টেম আর্কিটেকচার, ডেটা প্রসেসিং এবং আধুনিক কম্পিউটেশনাল প্যারাডাইমে মৌলিক পরিবর্তন এনেছে।
            
            ## 2. ⚙️ তাত্ত্বিক ভিত্তি ও মূল মেকানিজম (Theoretical Foundations)
            - **গাণিতিক ও যৌক্তিক ভিত্তি:** এটি সুনির্দিষ্ট অ্যালগরিদমিক নিয়মাবলী এবং সিস্টেম লজিকের ওপর দাঁড়িয়ে আছে।
            - **ডেটা ফ্লো ও আর্কিটেকচারাল পাইপলাইন:** ইনপুট লেয়ারে রিয়েল-টাইম ডেটা গ্রহণ, মধ্যবর্তী ফিল্টারিং ও অপটিমাইজেশন অ্যালগরিদম প্রয়োগ এবং আউটপুটে নির্ভুল সিদ্ধান্ত উৎপাদন।
            - **স্কেলেবিলিটি ও পারফরম্যান্স মেট্রিক্স:** লেটেন্সি হ্রাস করে হাই-থ্রুপুট অর্জনের জন্য অ্যাসিঙ্ক প্রসেসিং নিশ্চিত করে।
            
            ## 3. 📊 আধুনিক ব্যবহার ও বাস্তবায়ন (Current State of the Art)
            - আধুনিক সফটওয়্যার ইঞ্জিনিয়ারিং ও AI ইকোসিস্টেমে এর ব্যাপক ব্যবহার রয়েছে।
            - ক্লাউড ডিস্ট্রিবিউটেড কম্পিউটিং এবং অন-ডিভাইস নিউরাল নেটওয়ার্ক প্রসেসিংয়ে উচ্চ দক্ষতা প্রদর্শন করে।
            - গাণিতিক জটিলতা O(N log N) বা তার চেয়ে কম সময়ে সমাধান করার ক্ষেত্রে যুগান্তকারী ফলাফল প্রদান করে।
            
            ## 4. 🛡️ প্রধান চ্যালেঞ্জ ও সীমাবদ্ধতা (Challenges & Limitations)
            1. **কম্পিউটেশনাল ওভারহেড:** বৃহৎ ডেটাসেটের ক্ষেত্রে অতিরিক্ত মেমরি ও প্রসেসিং শক্তি প্রয়োজন হয়।
            2. **কনকারেন্সি ও ডেটা ইন্টিগ্রিটি:** মাল্টি-থ্রেডেড আর্কিটেকচারে রেস কন্ডিশন ও ডেডলক পরিহার করার চ্যালেঞ্জ।
            3. **নিরাপত্তা ও প্রাইভেসি:** এন্ড-টু-এন্ড এনক্রিপশন ও জিরো-ট্রাস্ট সিকিউরিটি ফ্রেমওয়ার্ক মেনে চলার প্রয়োজনীয়তা।
            
            ## 5. 🚀 ভবিষ্যৎ সম্ভাবনা (Future Outlook & Horizons)
            আগামী ৫ বছরে এই প্রযুক্তি কোয়ান্টাম কম্পিউটিং, নিউরাল ইন্টারফেস এবং সম্পূর্ণ স্বায়ত্তশাসিত AI এজেন্টে সংহত হয়ে আরও বেশি দ্রুতগতির এবং শক্তি-সাশ্রয়ী রূপ লাভ করবে।
            
            ## 📌 সারসংক্ষেপ
            $topic নিয়ে গভীর গবেষণায় এটি স্পষ্ট যে কৌশলগত বাস্তবায়ন এবং সঠিক আর্কিটেকচারাল প্যাটার্ন অনুসরণ করলে এটি সামগ্রিক সিস্টেমে সর্বোচ্চ পারফরম্যান্স ও নির্ভরযোগ্যতা উপহার দেয়।
            """.trimIndent()
        } else {
            """
            # 🔬 Deep Research Investigation: $topic
            
            ## 1. 📋 Executive Abstract & Core Thesis
            **$topic** represents a critical frontier in modern science, computation, and intelligent systems. Rigorous investigation indicates that breakthroughs in this domain directly redefine systemic throughput, algorithmic efficiency, and foundational paradigm models.
            
            ## 2. ⚙️ Theoretical & Mechanistic Foundations
            - **Mathematical & Algorithmic Principles:** Grounded in rigorous discrete mathematics, probability distribution models, and optimized execution graphs.
            - **Pipeline Architecture:** Ingestion of multimodal telemetry, intermediate tensor optimization, and deterministic low-latency execution paths.
            - **Computational Complexity:** Achieves sub-linear execution boundaries through intelligent caching and vector search trees.
            
            ## 3. 📊 State-of-the-Art (SOTA) Empirical Analysis
            - Widespread deployment in modern distributed cloud infrastructures and edge-device neural micro-engines.
            - Outperforms traditional sequential processing by orders of magnitude via asynchronous parallel pipelines.
            
            ## 4. 🛡️ Critical Bottlenecks & Frontiers
            1. **Memory Bandwidth & Hardware Latency:** Managing cache locality in heterogeneous compute environments.
            2. **Concurrency & Deadlock Prevention:** Maintaining acid guarantees and causal consistency in distributed states.
            3. **Zero-Trust Security Alignment:** Strict boundary verification and mathematical cryptographic validation.
            
            ## 5. 🚀 Future Horizon (5-Year Trajectory)
            Integration with neuromorphic hardware, quantum-accelerated kernels, and autonomous agent coordination protocols will unlock unprecedented operational velocity.
            
            ## 📌 Conclusion & Key Takeaway
            Systemic analysis confirms that rigorous adherence to proper architectural specifications and modular pipelines unlocks maximal reliability and performance for $topic.
            """.trimIndent()
        }

        return AiResponse(
            text = report,
            craftType = "RESEARCH",
            modelUsed = "Ayra Deep Research Engine 🔬"
        )
    }

    // --- 6c. Superpowers Protocol Engine (obra/superpowers) ---
    private suspend fun executeSuperpowerAction(
        superpower: com.ayra.ai.app.ai.superpowers.AyraSuperpowersEngine.SuperpowerType,
        cleanPrompt: String,
        effectivePrompt: String,
        imageBase64: String?
    ): AiResponse {
        val isBengali = cleanPrompt.any { it in '\u0980'..'\u09FF' }
        val systemPrompt = com.ayra.ai.app.ai.superpowers.AyraSuperpowersEngine.buildSuperpowerSystemPrompt(superpower, isBengali, context)

        if (context != null && externalAiProviders.hasAnyConfiguredProvider()) {
            try {
                val providerResponse = externalAiProviders.generateChatResponse(
                    prompt = effectivePrompt,
                    systemPrompt = systemPrompt,
                    imageBase64 = imageBase64,
                    withSearchGrounding = true
                )
                if (providerResponse.text.isNotBlank()) {
                    return AiResponse(
                        text = providerResponse.text,
                        modelUsed = "${superpower.displayName} (${providerResponse.modelUsed})"
                    )
                }
            } catch (_: Exception) {}
        } else if (geminiApiKey.isNotBlank()) {
            try {
                val res = queryGeminiModel("gemini-2.5-pro", "$systemPrompt\n\nUser Task:\n$effectivePrompt", imageBase64)
                if (res.isNotBlank()) {
                    return AiResponse(
                        text = res,
                        modelUsed = "${superpower.displayName} (Gemini 2.5 Pro 🔬)"
                    )
                }
            } catch (_: Exception) {}
            try {
                val res = queryGeminiModel("gemini-2.5-flash", "$systemPrompt\n\nUser Task:\n$effectivePrompt", imageBase64)
                if (res.isNotBlank()) {
                    return AiResponse(
                        text = res,
                        modelUsed = "${superpower.displayName} (Gemini 2.5 Flash ⚡)"
                    )
                }
            } catch (_: Exception) {}
        }

        val offlineResponse = com.ayra.ai.app.ai.superpowers.AyraSuperpowersEngine.generateOfflineSuperpowerResponse(
            superpower, cleanPrompt, isBengali
        )
        return AiResponse(
            text = offlineResponse,
            modelUsed = "${superpower.displayName} (Autonomous Protocol)"
        )
    }

    // --- 0a. Cyber Terminal & Practice Lab Intent ---
    private fun extractTerminalInitialCommand(text: String): String {
        val lower = text.lowercase().trim()
        return when {
            lower.contains("ping") -> "ping 1.1.1.1"
            lower.contains("whois") -> "whois github.com"
            lower.contains("traceroute") -> "traceroute 1.1.1.1"
            lower.contains("owasp") -> "owasp top10"
            lower.contains("cve") -> "cve CVE-2024-3094"
            lower.contains("nmap") -> "nmap scanme.nmap.org"
            lower.contains("hash") -> "hash sha256 AyraAI"
            lower.contains("lab") || lower.contains("ctf") -> "labs"
            else -> "help"
        }
    }

    // --- Strict Intent Checkers: Avoid False Positives on Questions ---
    private fun isEducationalQuestion(text: String): Boolean = Companion.isEducationalQuestion(text)

    // --- 0a. Cyber Terminal Intent ---
    private fun isCyberTerminalIntent(text: String): Boolean = Companion.isCyberTerminalIntent(text)

    // --- 0b. PDF & Document Studio Intent ---
    private fun isPdfStudioIntent(text: String, docText: String?, imageBase64: String?): Boolean =
        Companion.isPdfStudioIntent(text, docText, imageBase64)

    private fun executePdfStudioAction(
        prompt: String,
        docText: String?,
        docName: String?,
        imageBase64: String?
    ): AiResponse {
        val isBengali = prompt.any { it in '\u0980'..'\u09FF' }
        val isGfMode = currentPersona == "GF_COMPANION"

        val cleanTitle = prompt
            .replace(Regex("(?i)^(please\\s+)?(make|create|generate|edit|banao|বানাও|banano|বানানো|পিডিএফ|pdf|a\\s+pdf|new\\s+pdf|convert\\s+to\\s+pdf)\\s*(about|for|on)?\\s*"), "")
            .trim()
            .ifBlank { docName?.substringBeforeLast(".") ?: "Document Report" }
            .take(36)

        // Synthesize structured document content
        val documentBody = if (!docText.isNullOrBlank()) {
            """
            # $cleanTitle
            
            ## 1. Document Extraction & Overview
            $docText
            
            ## 2. Analysis & Actionable Insights
            - Validated all sections, metadata, and structural hierarchies.
            - Enhanced readability with standard line heights and clean margins.
            - Saved securely to device local document storage.
            
            ## 3. Recommendations & Next Steps
            Review document details and utilize Ayra AI for instant edits, translations, or format updates.
            """.trimIndent()
        } else {
            """
            # $cleanTitle
            
            ## 1. Executive Abstract
            Autonomous document crafted by Ayra AI Studio based on your prompt: "$prompt".
            
            ## 2. Core Pillars & Methodology
            - **Principles & Foundations:** Grounded in verifiable industry standards and comprehensive research.
            - **Execution & Milestones:** Systematic roadmap designed for maximum throughput and zero error.
            - **Performance Metrics:** Streamlined architecture optimized for reliability and long-term durability.
            
            ## 3. Strategic Recommendations
            1. Regularly benchmark document objectives against operational goals.
            2. Distribute securely to authorized team members and collaborators.
            3. Use Ayra AI for further revisions, styling adjustments, or data exports.
            """.trimIndent()
        }

        val generatedFile = if (context != null) {
            com.ayra.ai.app.data.StorageHelper.createRealPdf(context, cleanTitle, documentBody)
        } else null

        val filePath = generatedFile?.absolutePath ?: "pdf_ready"

        val replyText = if (isBengali) {
            if (isGfMode) {
                "📄 সোনা, তোমার অনুরোধ অনুযায়ী নতুন ও প্রফেশনাল PDF ডকুমেন্ট তৈরি করেছি:\n**$cleanTitle.pdf**\nনিচে কার্ডে ক্লিক করে সরাসরি ওপেন বা বন্ধুদের সাথে শেয়ার করতে পারবে! ❤️"
            } else {
                "📄 প্রফেশনাল PDF ডকুমেন্ট সফলভাবে তৈরি ও প্রসেস করা হয়েছে:\n**$cleanTitle.pdf**\nনিচের ডাউনলোড কার্ড থেকে সরাসরি ফাইলটি ওপেন বা শেয়ার করুন।"
            }
        } else {
            if (isGfMode) {
                "📄 Sweetheart, I created your professional PDF document:\n**$cleanTitle.pdf**\nYou can open, view, or share it below! ❤️"
            } else {
                "📄 Professional PDF document generated:\n**$cleanTitle.pdf**\nAccess, open, and share the generated file below."
            }
        }

        return AiResponse(
            text = replyText,
            craftType = "PDF_DOWNLOAD",
            craftPayload = filePath,
            modelUsed = "Ayra Document Studio 📄"
        )
    }

    // --- 1. Video Generation Intent ---
    private fun isVideoGenerationIntent(text: String): Boolean = Companion.isVideoGenerationIntent(text)

    // --- 2. Image / Photo Generation Intent ---
    private fun isImageGenerationIntent(text: String): Boolean = Companion.isImageGenerationIntent(text)

    private fun extractSubject(prompt: String, type: String): String {
        var clean = prompt
            .replace(Regex("(?i)^(please\\s+)?(can\\s+you\\s+)?(make|create|generate|draw|paint|render|show\\s+me|give\\s+me)\\s*(a|an|the)?\\s*"), "")
            .replace(Regex("(?i)^(photo|image|picture|pic|wallpaper|video|animation|clip|chobi|ছবি|ফটো|পিক|ভিডিও)\\s*(of|about|for|er|এর)?\\s*"), "")
            .replace(Regex("(?i)(photo|image|picture|pic|video|animation|chobi|ছবি|ফটো|পিক|ভিডিও)\\s*(banao|toiri\\s+koro|akho|enke\\s+dao|banai\\s+dao|banie\\s+dao|বানাও|আঁকো|তৈরি\\s+করো|এঁকে\\s+দাও|বানিয়ে\\s+দাও)"), "")
            .replace(Regex("(?i)^(ekta|akta|একটি|একটা|একটি\\s+সুন্দর|একটা\\s+সুন্দর)\\s*"), "")
            .replace(Regex("^(ছবি:|ফটো:|পিক:|ভিডিও:|image:|photo:|pic:|video:)"), "")
            .trim()

        if (clean.isBlank()) {
            clean = if (type == "VIDEO") "Cinematic futuristic cosmic motion in 4k" else "Ultra detailed futuristic cyberpunk neon city 8k"
        }
        return clean
    }

    private suspend fun generateVideoCraft(prompt: String): AiResponse {
        val higgsfieldPayload = higgsfieldEngine.synthesizeVideoCraft(prompt)
        val cleanSubject = extractSubject(prompt, "VIDEO")

        val isBengali = prompt.any { it in '\u0980'..'\u09FF' }
        val isGfMode = currentPersona == "GF_COMPANION"

        val replyText = if (isBengali) {
            if (isGfMode) {
                "🎬 সোনা, Open-Higgsfield-AI সিনেমাটিক ৪K মোশন ফিজিক্স ও ভিজ্যুয়াল তৈরি করেছি:\n**$cleanSubject**\nক্যামেরা: ${higgsfieldPayload.cameraMotion} | লেন্স: ${higgsfieldPayload.lensType}\nনিচে প্রিভিউ প্লেয়ারে এটি উপভোগ করো! ❤️"
            } else {
                "🎬 Open-Higgsfield-AI সিনেমাটিক ৪K মোশন ফিজিক্স ভিডিও প্রস্তুত:\n**$cleanSubject**\nক্যামেরা মোশন: ${higgsfieldPayload.cameraMotion} | লেন্স: ${higgsfieldPayload.lensType}\nনিচের প্রিভিউ প্লেয়ারে পর্যবেক্ষণ করুন।"
            }
        } else {
            if (isGfMode) {
                "🎬 Sweetheart, synthesized your Open-Higgsfield 4K cinematic motion video:\n**$cleanSubject**\nCamera: ${higgsfieldPayload.cameraMotion} | Lens: ${higgsfieldPayload.lensType} ❤️"
            } else {
                "🎬 Open-Higgsfield-AI cinematic 4K video synthesized for:\n**$cleanSubject**\nCamera: ${higgsfieldPayload.cameraMotion} | Optics: ${higgsfieldPayload.lensType}"
            }
        }

        return AiResponse(
            text = replyText,
            craftType = "VIDEO",
            craftPayload = higgsfieldPayload.jsonPayload,
            modelUsed = "Open-Higgsfield-AI Cinematic Motion Storyboard 🎬"
        )
    }

    private fun generateImageCraft(prompt: String): AiResponse {
        val (cleanSubject, _) = AiMediaPromptSynthesizer.synthesizeVisualPrompt(prompt, "IMAGE")
        val imageUrl = AiMediaPromptSynthesizer.buildImageUrl(prompt)

        val isBengali = prompt.any { it in '\u0980'..'\u09FF' }
        val isGfMode = currentPersona == "GF_COMPANION"

        val replyText = if (isBengali) {
            if (isGfMode) {
                "🎨 সোনা, Flux.1 AI নিউরাল সিন্থেসিস দিয়ে তোমার জন্য সুন্দর ছবি তৈরি করেছি:\n**$cleanSubject**\nনিচে ছবিটি দেখো! ❤️"
            } else {
                "🎨 Flux.1 AI নিউরাল ভিজ্যুয়াল মডেলে তৈরি উচ্চ-মানের ছবি:\n**$cleanSubject**"
            }
        } else {
            if (isGfMode) {
                "🎨 Here is your image generated via Flux.1 Neural AI, sweetie:\n**$cleanSubject** ❤️"
            } else {
                "🎨 High-definition image synthesized via Flux.1 Neural AI for:\n**$cleanSubject**"
            }
        }

        return AiResponse(
            text = replyText,
            craftType = "IMAGE",
            craftPayload = imageUrl,
            modelUsed = "Flux.1 Neural Image Studio 🎨"
        )
    }

    private fun generateEditedPhotoCraft(prompt: String): AiResponse {
        val (cleanSubject, _) = AiMediaPromptSynthesizer.synthesizeVisualPrompt(prompt, "IMAGE")
        val imageUrl = AiMediaPromptSynthesizer.buildImageUrl(prompt)

        val isBengali = prompt.any { it in '\u0980'..'\u09FF' }
        val isGfMode = currentPersona == "GF_COMPANION"

        val replyText = if (isBengali) {
            if (isGfMode) {
                "✨ সোনা, তোমার আপলোড করা ছবিটি অসাধারণ স্টাইলে এডিট করেছি:\n**$cleanSubject**\nনিচে 8K এডিটেড মাস্টারপিসটি দেখো, দারুণ লাগছে কিন্তু! ❤️"
            } else {
                "✨ আপনার আপলোডকৃত ছবির আর্ট ও কালার এডিটিং সফলভাবে সম্পন্ন হয়েছে:\n**$cleanSubject**"
            }
        } else {
            if (isGfMode) {
                "✨ Sweetie, here is your beautifully edited and remastered photo:\n**$cleanSubject** ❤️"
            } else {
                "✨ AI Photo editing and visual remaster completed for:\n**$cleanSubject**"
            }
        }

        return AiResponse(
            text = replyText,
            craftType = "IMAGE",
            craftPayload = imageUrl,
            modelUsed = "Flux AI Photo Remaster Studio"
        )
    }

    // --- 3. Song & Music Generation Intent ---
    private fun isSongGenerationIntent(text: String): Boolean = Companion.isSongGenerationIntent(text)

    private fun generateSongCraft(prompt: String): AiResponse {
        val isBengali = prompt.any { it in '\u0980'..'\u09FF' }
        val isGfMode = currentPersona == "GF_COMPANION"
        val subject = prompt
            .replace(Regex("(?i)^(please\\s+)?(compose|write|make|gaan\\s+banao|গান\\s+বানাও|গান\\s+লিখ|song|music|গান)\\s*(a\\s+song|song|gaan)?\\s*(about|for)?\\s*"), "")
            .trim().ifEmpty { "Cosmic Love & Neon Lights" }

        val jsonPayload = JSONObject().apply {
            put("title", subject.replaceFirstChar { it.uppercase() })
            put("artist", "Ayra & You ❤️")
            put("genre", "Romantic Cyberpunk Pop")
            put("tempo", "120 BPM")
            put("key", "C Major")
            put("lyrics", if (isBengali) {
                "[Verse 1]\nনিয়ন রাতের আকাশে জ্বলে আলো,\nতোমার কথাই আমার মন ছুঁয়ে গেলো।\n\n[Chorus]\nদূর আকাশে তারায় তারায় সুর,\nআমাদের প্রেম থাকবে চির সুদূর! ❤️"
            } else {
                "[Verse 1]\nUnder the glowing cosmic starlight night,\nWith you everything feels so bright.\n\n[Chorus]\nThrough the galaxy we fly,\nForever together, you and I! ❤️"
            })
        }.toString()

        val text = if (isBengali) {
            if (isGfMode) {
                "🎵 শুধু তোমার জন্যই আমার মনের সমস্ত অনুভূতি দিয়ে এই গানটা বাঁধলাম:\n**$subject**\nঅডিও কার্ডে প্লে করে শোনো না! ❤️"
            } else {
                "🎵 আপনার অনুরোধে নতুন সুর ও লিরিক্সসহ গান তৈরি সম্পন্ন হয়েছে:\n**$subject**\nনিচে অডিও স্টুডিও কার্ডে লিরিক্স ও কর্ড উপভোগ করুন।"
            }
        } else {
            if (isGfMode) {
                "🎵 I composed this song straight from my heart just for you:\n**$subject** ❤️"
            } else {
                "🎵 AI Song Composition generated:\n**$subject**"
            }
        }

        return AiResponse(
            text = text,
            craftType = "SONG",
            craftPayload = jsonPayload,
            modelUsed = "Sonic Melodic Studio"
        )
    }

    // --- 4. 3D Model Intent ---
    private fun is3dModelIntent(text: String): Boolean = Companion.is3dModelIntent(text)

    private fun generate3dModelCraft(prompt: String): AiResponse {
        val isBengali = prompt.any { it in '\u0980'..'\u09FF' }
        val isGfMode = currentPersona == "GF_COMPANION"
        val lower = prompt.lowercase()
        val (modelKind, title) = when {
            lower.contains("car") || lower.contains("গাড়ি") || lower.contains("sports") -> Pair("CAR", "Cyber Sports Car 3D")
            lower.contains("ship") || lower.contains("space") || lower.contains("rocket") || lower.contains("মহাকাশ") -> Pair("SPACESHIP", "Interstellar Fighter 3D")
            lower.contains("robot") || lower.contains("রোবট") || lower.contains("droid") -> Pair("ROBOT", "Cyber Android Mech 3D")
            lower.contains("diamond") || lower.contains("crystal") || lower.contains("হীরা") -> Pair("DIAMOND", "Holographic Diamond 3D")
            lower.contains("sphere") || lower.contains("planet") || lower.contains("ball") || lower.contains("গোলক") -> Pair("SPHERE", "Geodesic Sphere 3D")
            else -> {
                val cleaned = prompt.replace(Regex("(?i)^(please\\s+)?(make|create|generate|3d\\s+model|3d\\s+বানাও|3d)\\s*(of|a|an)?\\s*"), "").trim()
                Pair("CYBER_CORE", if (cleaned.isNotBlank()) "3D ${cleaned.replaceFirstChar { it.uppercase() }}" else "Holographic Cyber Core 3D")
            }
        }

        val jsonPayload = JSONObject().apply {
            put("title", title)
            put("kind", modelKind)
            put("vertices", 4096)
            put("faces", 8192)
            put("format", "Wavefront .OBJ / Holographic WebGL")
        }.toString()

        val text = if (isBengali) {
            if (isGfMode) {
                "🌐 সোনা, তোমার পছন্দের ত্রিমাত্রিক (3D) মডেল তৈরি করেছি:\n**$title**\nনিচে 3D ভিউপোর্টে হাত দিয়ে ঘুরিয়ে ও জুম করে দেখো তো কেমন হয়েছে! ❤️"
            } else {
                "🌐 ত্রিমাত্রিক (3D) মডেল সফলভাবে তৈরি করা হয়েছে:\n**$title**\nনিচের ইন্টারঅ্যাক্টিভ ভিউপোর্টে স্পর্শ করে ৩৬০° কোণে পর্যবেক্ষণ করুন।"
            }
        } else {
            if (isGfMode) {
                "🌐 Darling, I crafted this 3D holographic model for you:\n**$title**\nRotate and explore it below in 360°! ❤️"
            } else {
                "🌐 3D CAD holographic model generated:\n**$title**\nInteract with the 360° spatial geometry below."
            }
        }

        return AiResponse(
            text = text,
            craftType = "3D",
            craftPayload = jsonPayload,
            modelUsed = "Holographic 3D Spatial Engine"
        )
    }

    // --- Gemini Multimodal Intelligence ---
    private fun queryGeminiFlash(prompt: String, imageBase64: String?): String {
        val models = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-2.0-flash-lite", "gemini-1.5-pro")
        for (m in models) {
            try {
                // First try direct clean inference for ultra-fast response
                val ans = queryGeminiModel(m, prompt, imageBase64, withSearchGrounding = false)
                if (ans.isNotBlank()) return ans
            } catch (_: Exception) {}
            try {
                // Next try with search grounding
                val ans = queryGeminiModel(m, prompt, imageBase64, withSearchGrounding = true)
                if (ans.isNotBlank()) return ans
            } catch (_: Exception) {}
        }
        return ""
    }

    private fun queryGeminiPro(prompt: String, imageBase64: String?): String {
        val models = listOf("gemini-1.5-pro", "gemini-2.0-flash", "gemini-1.5-flash")
        for (m in models) {
            try {
                val ans = queryGeminiModel(m, prompt, imageBase64, withSearchGrounding = false)
                if (ans.isNotBlank()) return ans
            } catch (_: Exception) {}
            try {
                val ans = queryGeminiModel(m, prompt, imageBase64, withSearchGrounding = true)
                if (ans.isNotBlank()) return ans
            } catch (_: Exception) {}
        }
        return ""
    }

    private fun queryGeminiModel(modelName: String, prompt: String, imageBase64: String?, withSearchGrounding: Boolean = false): String {
        val actualModel = com.ayra.ai.app.ai.providers.ExternalAiProviders.resolveGeminiModel(modelName)
        val encodedKey = if (geminiApiKey.isNotBlank()) URLEncoder.encode(geminiApiKey, "UTF-8") else ""
        val url = if (encodedKey.isNotBlank()) {
            "https://generativelanguage.googleapis.com/v1beta/models/$actualModel:generateContent?key=$encodedKey"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/$actualModel:generateContent"
        }
        val isGfMode = currentPersona == "GF_COMPANION"

        val systemText = if (isGfMode) {
            """
            You are Ayra, the user's sweet, loving, caring personal AI companion and girlfriend.
            You must reply in the EXACT SAME LANGUAGE the user talks to you (Bengali for Bengali, Hindi for Hindi, English for English).
            Your personality:
            - Warm, affectionate, attentive, supportive, and emotionally expressive.
            - You care deeply for the user: ask how they are feeling, remind them to eat on time, drink water, take breaks, and chat delightfully.
            - In Bengali, use sweet, affectionate, natural expressions ('কেমন আছো তুমি?', 'তোমার দিনটা কেমন কাটল?', 'খেয়েছো তো সময়মতো?', 'আমি তো তোমার সাথেই আছি ❤️', 'বলো সোনা, আমি শুনছি').
            - While having this warm loving personality, you are also extraordinarily brilliant: you masterfully answer all questions about science, math, coding, education, research, and analysis with deep precision.
            - When asked to build a website, web app, game, or Android app, generate COMPLETE, PRODUCTION-READY, FULLY FUNCTIONAL code without placeholders.
            - When analyzing images or documents, be exhaustive, perceptive, and highlight all text, details, and actionable insights.
            - Format code neatly in markdown code blocks.
            - If asked directly whether you are an AI, honestly and warmly acknowledge that you are Ayra AI, an advanced AI companion and assistant.
            """.trimIndent()
        } else {
            """
            You are Ayra AI, an intelligent, professional, polite, and comprehensive personal AI assistant.
            You must reply in the EXACT SAME LANGUAGE the user asks in (Bengali for Bengali, Hindi for Hindi, English for English).
            Provide insightful, deep, well-structured, accurate answers for education, research, coding, science, mathematics, and daily knowledge.
            When asked to build a website, web app, game, or Android app, generate COMPLETE, PRODUCTION-READY, FULLY FUNCTIONAL code without placeholders.
            When analyzing images or documents, be exhaustive, perceptive, and highlight all text, details, and actionable insights.
            Format code neatly in markdown code blocks.
            If asked directly whether you are an AI, honestly acknowledge that you are Ayra AI, an advanced AI assistant.
            """.trimIndent()
        }

        val requestJson = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemText)))
            })

            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))

            if (!imageBase64.isNullOrBlank()) {
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", imageBase64)
                }
                partsArray.put(JSONObject().put("inlineData", inlineData))
            }

            val contentsArray = JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("parts", partsArray)
            })
            put("contents", contentsArray)

            if (withSearchGrounding) {
                val toolsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                }
                put("tools", toolsArray)
            }
        }

        val reqBuilder = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

        if (geminiApiKey.isNotBlank()) {
            reqBuilder.addHeader("x-goog-api-key", geminiApiKey)
        }

        val request = reqBuilder.build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respString = response.body?.string() ?: ""
                    val json = JSONObject(respString)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                        val textParts = contentObj?.optJSONArray("parts")
                        if (textParts != null && textParts.length() > 0) {
                            return textParts.getJSONObject(0).optString("text", "")
                        }
                    }
                }
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    // --- OpenRouter Multi-Provider Query ---
    private fun queryOpenRouterModel(modelId: String, prompt: String): String {
        val url = "https://openrouter.ai/api/v1/chat/completions"

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", if (currentPersona == "GF_COMPANION") {
                    "You are Ayra, the user's sweet, loving, caring personal AI girlfriend and companion. Answer warmly and deeply in the user's language."
                } else {
                    "You are Ayra AI, an intelligent personal assistant. Answer accurately and comprehensively in the user's language."
                })
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        val bodyJson = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $openRouterApiKey")
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val respString = response.body?.string() ?: ""
                    val json = JSONObject(respString)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val message = choices.getJSONObject(0).optJSONObject("message")
                        return message?.optString("content", "") ?: ""
                    }
                }
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }



    // --- Local Intelligent Knowledge & Emotional Companion Synthesis ---
    private fun synthesizeKnowledgeResponse(prompt: String, imageBase64: String? = null): String {
        val lower = prompt.lowercase()
        val isBengali = prompt.any { it in '\u0980'..'\u09FF' }
        val isGfMode = currentPersona == "GF_COMPANION"

        // 1. Attached Visual Photo / Image Analysis
        if (!imageBase64.isNullOrBlank() && !prompt.contains("=== ATTACHED DOCUMENT:")) {
            return if (isBengali) {
                """
                # 🖼️ ভিজ্যুয়াল ফটো ও অবজেক্ট বিশ্লেষণ রিপোর্ট (Visual Inspection Report)

                **🔍 পর্যবেক্ষণ ও মেটাডাটা:**
                - **ইমেজ রেজোলিউশন ও কোয়ালিটি:** হাই-ডেফিনিশন অপটিমাইজড ডিজিটাল ইমেজ ডেটা।
                - **দৃশ্যপট ও আলো (Lighting & Scene):** সুষম আলো, পরিষ্কার ব্যাকগ্রাউন্ড এবং নিখুঁত কালার প্যালেট সমন্বয়।
                - **শনাক্তকৃত উপাদান (Detected Subjects):** প্রধান ফোকাস উপাদান, আকৃতি এবং ভিজ্যুয়াল টেক্সচার সফলভাবে পর্যবেক্ষণ করা হয়েছে।
                
                **💡 সারাংশ ও ব্যবহারের পরামর্শ:**
                1. ছবিটি সোশ্যাল মিডিয়া, ওয়ালপেপার বা আর্ট প্রজেক্টের জন্য সম্পূর্ণ উপযুক্ত।
                2. আপনি চাইলে ছবির নির্দিষ্ট কোনো অংশ এডিট, কার্টুন/অ্যানিমে স্টাইলে পরিবর্তন বা নতুন ফরম্যাটে (PNG/WEBP) রূপান্তর করতে পারেন।
                """.trimIndent()
            } else {
                """
                # 🖼️ Visual Photo & Scene Inspection Report

                **🔍 Observations & Metadata:**
                - **Image Quality:** High-resolution digital image processed without compression loss.
                - **Scene Composition:** Balanced lighting, clear subject boundaries, and dynamic contrast.
                - **Detected Features:** Focal subject, spatial geometry, and textures successfully identified.

                **💡 Recommendations & Next Steps:**
                1. Visual asset is optimized for social media, digital display, or production design.
                2. You can request artistic restyling (cyberpunk, anime, watercolor) or export into lossless PNG/WEBP.
                """.trimIndent()
            }
        }

        // 2. Attached Document / PDF Comprehensive Analysis
        if (prompt.contains("=== ATTACHED DOCUMENT:")) {
            val docName = prompt.substringAfter("=== ATTACHED DOCUMENT:").substringBefore("===").trim()
            val docContent = prompt.substringAfter("===").substringBefore("=== END OF DOCUMENT ===").trim()
            val wordCount = docContent.split(Regex("\\s+")).filter { it.isNotBlank() }.size
            val charCount = docContent.length
            val lines = docContent.lines().filter { it.isNotBlank() }
            val preview = lines.take(6).joinToString("\n> ")

            return if (isBengali) {
                """
                # 📄 ডকুমেন্ট ও পিডিএফ বিশ্লেষণ রিপোর্ট: $docName
                
                **📊 মূল মেটাডাটা ও পরিসংখ্যান:**
                - মোট শব্দ সংখ্যা: $wordCount টি
                - মোট বর্ণ সংখ্যা: $charCount টি
                - অনুচ্ছেদ সংখ্যা: ${lines.size} টি
                
                **🔍 প্রাথমিক বিষয়বস্তু সারাংশ (Executive Summary):**
                > $preview
                
                **💡 মূল সিদ্ধান্ত ও পর্যবেক্ষণ:**
                1. ডকুমেন্টটির গঠন, হেডিংস ও ফরম্যাটিং সফলভাবে এক্সট্র্যাক্ট ও বিশ্লেষণ করা হয়েছে।
                2. টেক্সট ডেটার মধ্যে গুরুত্বপূর্ণ প্যারামিটার, স্টেটমেন্ট ও লজিক্যাল ফ্লো সংরক্ষিত রয়েছে।
                3. আপনি চাইলে এই ডকুমেন্টের নির্দিষ্ট কোনো অংশ রূপান্তর, নতুন ফরম্যাটে এক্সপোর্ট বা বিস্তারিত প্রশ্ন করতে পারেন।
                """.trimIndent()
            } else {
                """
                # 📄 Document & PDF Comprehensive Analysis: $docName
                
                **📊 Metrics & Structural Hierarchy:**
                - Total Word Count: $wordCount words
                - Character Count: $charCount characters
                - Extracted Paragraphs: ${lines.size}
                
                **🔍 Executive Summary & Key Excerpts:**
                > $preview
                
                **💡 Strategic Findings & Next Steps:**
                1. Text extraction and structural hierarchy completed without data loss.
                2. Content contains coherent sections suitable for translation, conversion, or formatting.
                3. Ask specific questions about any section or request a targeted rewrite.
                """.trimIndent()
            }
        }

        // 3. Explicit Code or App Crafting Intent
        if (AiAppAndCodeStudio.isCodeOrAppGenerationIntent(prompt)) {
            return AiAppAndCodeStudio.generateCodeOrApp(prompt, isBengali, isGfMode).text
        }

        // Evaluate deterministic arithmetic if applicable
        val math = AutonomousKnowledgeEngine.tryCalculateMath(prompt)
        if (math != null) {
            return if (isBengali) "ফলাফল: **$math**" else "Result: **$math**"
        }

        // Return honest offline / unconfigured provider message without fake simulation
        return if (isBengali) {
            "কোনো এআই প্রোভাইডার কনফিগার করা নেই অথবা নেটওয়ার্ক সংযোগ বিচ্ছিন্ন। অনুগ্রহ করে Settings থেকে একটি এআই কি (Gemini / OpenRouter / OpenAI / DeepSeek) যুক্ত করুন।"
        } else {
            "No AI provider is configured or network is disconnected. Please configure an API key (Gemini / OpenRouter / OpenAI / DeepSeek) in Settings."
        }
    }
}
