package com.ayra.ai.app.ai

import com.ayra.ai.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object AiMediaPromptSynthesizer {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val bengaliToEnglishSubjectMap = mapOf(
        "বিড়াল" to "cute fluffy domestic cat",
        "বিড়ালের" to "cute fluffy domestic cat",
        "কুকুর" to "playful golden retriever dog",
        "রাজকন্যা" to "fairytale royal princess with ornate tiara and gown",
        "রাজকন্যার" to "fairytale royal princess with ornate tiara and gown",
        "গাড়ি" to "futuristic hypercar sleek sports car",
        "গাড়ির" to "futuristic hypercar sleek sports car",
        "মহাকাশ" to "deep space with luminous nebulae and galaxies",
        "চাঁদ" to "realistic glowing moon in dark cosmic night sky",
        "চাঁদে" to "astronaut on the lunar surface with earth in background",
        "সূর্য" to "blazing golden sun with solar flares in space",
        "বাঘ" to "majestic royal bengal tiger in lush green jungle",
        "সিংহ" to "noble male lion with golden mane in savannah",
        "ফুল" to "vibrant blooming flowers in a sunlit garden",
        "গোলাপ" to "ultra-detailed red roses with delicate dewdrops",
        "সমুদ্র" to "tropical azure ocean with gentle waves and coral reef",
        "সৈকত" to "peaceful sandy tropical beach at sunset",
        "পাহাড়" to "snow-capped majestic alpine mountain peaks",
        "সূর্যাস্ত" to "dramatic vibrant golden sunset over horizon",
        "মেয়ে" to "beautiful cinematic portrait of an elegant woman",
        "ছেলে" to "portrait of a handsome young man",
        "রোবট" to "sentient humanoid cyber android with neon circuitry",
        "বাড়ি" to "luxurious modern architectural glass villa with infinity pool",
        "পাখি" to "vivid colorful exotic bird with iridescent feathers",
        "শহর" to "futuristic cyberpunk neon metropolis skyline",
        "নদী" to "crystal clear winding river reflecting starlight",
        "অ্যানিমে" to "stunning Makoto Shinkai style anime character landscape",
        "কার্টুন" to "delightful Pixar 3D animated character",
        "ড্রাগন" to "majestic fire-breathing dragon over ancient castle",
        "ঘোড়া" to "powerful wild black stallion galloping on beach",
        "বৃষ্টি" to "romantic rainy city street with neon reflections",
        "বাচ্চা" to "happy adorable baby with sparkling eyes",
        "মা" to "loving affectionate mother holding child portrait"
    )

    /**
     * Translates and refines any user prompt into a high-fidelity DALL-E / Flux English prompt.
     */
    fun synthesizeVisualPrompt(rawPrompt: String, type: String = "IMAGE", context: android.content.Context? = null): Pair<String, String> {
        val cleanSubject = extractCleanSubject(rawPrompt)
        val geminiKey = context?.let { com.ayra.ai.app.data.AyraPreferences.getEffectiveGeminiKey(it) }?.ifBlank { BuildConfig.GEMINI_API_KEY } ?: BuildConfig.GEMINI_API_KEY

        // 1. Try Gemini API prompt expansion if key available
        if (geminiKey.isNotBlank()) {
            val expanded = expandViaGemini(cleanSubject, geminiKey, type)
            if (expanded.isNotBlank()) {
                return Pair(cleanSubject, expanded)
            }
        }

        // 2. Intelligent Offline Bilingual Translation & Photographic Expansion
        val translatedSubject = translateBengaliVisualTerms(cleanSubject)
        val styleTags = if (type == "VIDEO") {
            "4k 60fps cinematic tracking motion shot, dynamic perspective, dramatic volumetric studio lighting, hyper-realistic photorealism, Unreal Engine 5 render style"
        } else {
            "masterpiece, photorealistic 8k, award-winning photography, shot on 35mm lens, f/1.8, ray traced reflections, dramatic volumetric lighting, cinematic color grading, trending on ArtStation"
        }

        val expanded = "$translatedSubject, $styleTags"
        return Pair(cleanSubject, expanded)
    }

    fun buildImageUrl(prompt: String, seed: Long = (100000..999999).random().toLong()): String {
        val (_, expandedPrompt) = synthesizeVisualPrompt(prompt, "IMAGE")
        val encoded = URLEncoder.encode(expandedPrompt, "UTF-8")
        return "https://image.pollinations.ai/prompt/$encoded?model=flux&width=1024&height=1024&seed=$seed&nologo=true&enhance=true"
    }

    fun buildVideoPayload(prompt: String, seed: Long = (100000..999999).random().toLong()): JSONObject {
        val (cleanSubject, expandedPrompt) = synthesizeVisualPrompt(prompt, "VIDEO")
        val encoded = URLEncoder.encode(expandedPrompt, "UTF-8")
        val previewUrl = "https://image.pollinations.ai/prompt/$encoded?model=flux&width=1280&height=720&seed=$seed&nologo=true&enhance=true"

        return JSONObject().apply {
            put("title", cleanSubject.replaceFirstChar { it.uppercase() })
            put("prompt", cleanSubject)
            put("previewUrl", previewUrl)
            put("duration", "0:15")
        }
    }

    private fun extractCleanSubject(prompt: String): String {
        var clean = prompt
            .replace(Regex("(?i)^(please\\s+)?(can\\s+you\\s+)?(make|create|generate|draw|paint|render|show\\s+me|give\\s+me)\\s*(a|an|the)?\\s*"), "")
            .replace(Regex("(?i)^(photo|image|picture|pic|wallpaper|video|animation|clip|chobi|ছবি|ফটো|পিক|ভিডিও)\\s*(of|about|for|er|এর)?\\s*"), "")
            .replace(Regex("(?i)(photo|image|picture|pic|video|animation|chobi|ছবি|ফটো|পিক|ভিডিও)\\s*(banao|toiri\\s+koro|akho|enke\\s+dao|banai\\s+dao|banie\\s+dao|বানাও|আঁকো|তৈরি\\s+করো|এঁকে\\s+দাও|বানিয়ে\\s+দাও)"), "")
            .replace(Regex("(?i)^(ekta|akta|একটি|একটা|একটি\\s+সুন্দর|একটা\\s+সুন্দর)\\s*"), "")
            .replace(Regex("^(ছবি:|ফটো:|পিক:|ভিডিও:|image:|photo:|pic:|video:)"), "")
            .trim()

        if (clean.isBlank()) {
            clean = "Stunning futuristic cyberpunk neon city 8k"
        }
        return clean
    }

    private fun translateBengaliVisualTerms(text: String): String {
        var result = text
        for ((bn, en) in bengaliToEnglishSubjectMap) {
            if (result.contains(bn)) {
                result = result.replace(bn, en)
            }
        }
        return result
    }

    private fun expandViaGemini(subject: String, apiKey: String, type: String): String {
        return try {
            val systemInstruction = if (type == "VIDEO") {
                "You are an expert prompt engineer for Sora and Gen-2. Convert this visual request into a single sentence vivid cinematic 4k video motion prompt describing dynamic movement, lighting, and camera tracking. Output ONLY the English prompt in one line, no markdown, no quotes."
            } else {
                "You are an expert prompt engineer for DALL-E 3 and Midjourney v6. Convert this visual request into a single sentence master photographic English prompt with subject details, lens, lighting, and textures. Output ONLY the English prompt in one line, no markdown, no quotes."
            }

            val encodedKey = if (apiKey.isNotBlank()) URLEncoder.encode(apiKey, "UTF-8") else ""
            val url = if (encodedKey.isNotBlank()) {
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$encodedKey"
            } else {
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
            }
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", "$systemInstruction\nUser request: $subject")
                    }))
                }))
            }

            val reqBuilder = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("x-goog-api-key", apiKey)
            }

            val request = reqBuilder.build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    val answer = parts?.optJSONObject(0)?.optString("text", "")?.trim() ?: ""
                    if (answer.isNotBlank()) return answer
                }
            }
            ""
        } catch (_: Exception) {
            ""
        }
    }
}
