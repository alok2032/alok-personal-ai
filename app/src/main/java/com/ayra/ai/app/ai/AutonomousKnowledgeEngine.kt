package com.ayra.ai.app.ai

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Real-time encyclopedic research and calculation utility.
 * Strictly performs genuine HTTP Wikipedia / live factual lookup and deterministic arithmetic.
 * All fake offline canned-answer engines have been permanently removed.
 */
object AutonomousKnowledgeEngine {

    // Thread-safe knowledge cache
    private val knowledgeCache = ConcurrentHashMap<String, String>()

    private val STOP_WORDS_EN_REGEX = Regex("(?i)^(why does|why is|why do|what is|what are|how does|how to|who is|where is|when did|explain|tell me about|meaning of|definition of)\\s+")
    private val STOP_WORDS_BN_REGEX = Regex("\\b(কেন|কীভাবে|কিভাবে|কোথায়|কখন|কেমন|কার|কোনটি|কী|কি|বলুন|বলো তো|বলো|সম্পর্কে|জানাও|ব্যাখ্যা করো|দেখা যায়|দেখায়|হয়|হলো|কাকে বলে|অর্থ কি|নাম কি|কারণে)\\b")
    private val PUNCTUATION_REGEX = Regex("[?।!.,:;]")
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val MATH_REGEX = Regex("^(\\d+(?:\\.\\d+)?)\\s*([+\\-*/xX×÷^%]|যোগ|বিয়োগ|গুণ|ভাগ)\\s*(\\d+(?:\\.\\d+)?)$")

    /**
     * Queries Wikipedia open API for real-time encyclopedic extracts in Bengali or English.
     */
    fun queryLiveWebKnowledge(prompt: String, isBengali: Boolean, httpClient: OkHttpClient): String {
        val topic = extractTopicKeyword(prompt)
        val queryTerm = if (topic.isNotBlank()) topic else prompt.trim()
        if (queryTerm.isBlank()) return ""
        val cacheKey = "wiki:$queryTerm"

        knowledgeCache[cacheKey]?.let { return it }

        val userAgent = "AyraAiAssistant/2.0 (Linux; Android 14; Mobile; https://ayra.ai/assistant; contact@ayra.ai)"

        fun fetchWikiGeneratorSearch(lang: String, term: String): Pair<String, String>? {
            return try {
                val encoded = URLEncoder.encode(term, "UTF-8")
                val searchUrl = "https://$lang.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$encoded&gsrlimit=1&prop=extracts&exintro=1&explaintext=1&format=json"
                val req = Request.Builder()
                    .url(searchUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return null
                    val body = resp.body?.string() ?: ""
                    val root = JSONObject(body)
                    val pages = root.optJSONObject("query")?.optJSONObject("pages") ?: return null
                    val firstKey = pages.keys().asSequence().firstOrNull() ?: return null
                    if (firstKey == "-1") return null
                    val pageObj = pages.getJSONObject(firstKey)
                    val title = pageObj.optString("title", term)
                    val extract = pageObj.optString("extract", "").trim()
                    if (extract.length >= 30) Pair(title, extract) else null
                }
            } catch (_: Exception) { null }
        }

        fun fetchWikiSummary(lang: String, term: String): Pair<String, String>? {
            return try {
                val encoded = URLEncoder.encode(term.replace(" ", "_"), "UTF-8")
                val summaryUrl = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encoded"
                val req = Request.Builder()
                    .url(summaryUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return null
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    val extract = json.optString("extract", "").trim()
                    val title = json.optString("title", term)
                    if (extract.length >= 30) Pair(title, extract) else null
                }
            } catch (_: Exception) { null }
        }

        if (isBengali) {
            val bnGen = fetchWikiGeneratorSearch("bn", queryTerm) ?: fetchWikiGeneratorSearch("bn", prompt.trim())
            if (bnGen != null) {
                val formatted = formatKnowledgePoints(bnGen.first, bnGen.second, isBengali = true)
                knowledgeCache[cacheKey] = formatted
                return formatted
            }
            val bnSummary = fetchWikiSummary("bn", queryTerm)
            if (bnSummary != null) {
                val formatted = formatKnowledgePoints(bnSummary.first, bnSummary.second, isBengali = true)
                knowledgeCache[cacheKey] = formatted
                return formatted
            }
        }

        val enGen = fetchWikiGeneratorSearch("en", queryTerm) ?: fetchWikiGeneratorSearch("en", prompt.trim())
        if (enGen != null) {
            val formatted = formatKnowledgePoints(enGen.first, enGen.second, isBengali = isBengali)
            knowledgeCache[cacheKey] = formatted
            return formatted
        }

        val enSummary = fetchWikiSummary("en", queryTerm)
        if (enSummary != null) {
            val formatted = formatKnowledgePoints(enSummary.first, enSummary.second, isBengali = isBengali)
            knowledgeCache[cacheKey] = formatted
            return formatted
        }

        return ""
    }

    fun formatKnowledgePoints(title: String, rawExtract: String, isBengali: Boolean): String {
        val cleanExtract = rawExtract.replace(Regex("\\s+"), " ").trim()
        val sentences = cleanExtract.split(Regex("(?<=[.!?।])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 8 }

        val overview = sentences.take(2).joinToString(" ")
        val bulletPoints = sentences.drop(2).take(6)

        return buildString {
            append("**$title** (উইকিপিডিয়া রেফারেন্স)\n\n")
            append(overview).append("\n\n")
            if (bulletPoints.isNotEmpty()) {
                bulletPoints.forEach { point ->
                    append("• $point\n")
                }
            }
        }.trimEnd()
    }

    /**
     * Extracts salient topic keywords by stripping question affixes.
     */
    fun extractTopicKeyword(prompt: String): String {
        var clean = prompt.trim()
        clean = STOP_WORDS_EN_REGEX.replace(clean, "")
        clean = STOP_WORDS_BN_REGEX.replace(clean, " ")
        clean = PUNCTUATION_REGEX.replace(clean, " ").trim()
        val words = WHITESPACE_REGEX.split(clean).filter { it.isNotBlank() && it.length > 1 }
        return if (words.isNotEmpty()) words.take(4).joinToString(" ") else clean
    }

    /**
     * Evaluates arithmetic expressions deterministically.
     */
    fun tryCalculateMath(prompt: String): String? {
        val clean = prompt
            .replace(Regex("(?i)^(what is|calculate|solve|find|কত|মান কত|বলুন|বলো)\\s+"), "")
            .replace("=", "").replace("?", "").replace("।", "").trim()
        val match = MATH_REGEX.find(clean) ?: return null
        val num1 = match.groupValues[1].toDoubleOrNull() ?: return null
        val op = match.groupValues[2]
        val num2 = match.groupValues[3].toDoubleOrNull() ?: return null

        val (opSymbol, result) = when (op) {
            "+", "যোগ" -> Pair("+", num1 + num2)
            "-", "বিয়োগ" -> Pair("-", num1 - num2)
            "*", "x", "X", "×", "গুণ" -> Pair("×", num1 * num2)
            "/", "÷", "ভাগ" -> Pair("÷", if (num2 != 0.0) num1 / num2 else Double.NaN)
            "^" -> Pair("^", Math.pow(num1, num2))
            "%" -> Pair("%", num1 % num2)
            else -> return null
        }
        val formattedResult = if (result.isNaN()) "অসংজ্ঞায়িত (Undefined - Division by Zero)"
        else if (result % 1.0 == 0.0) result.toLong().toString()
        else String.format(Locale.US, "%.4f", result)

        return "$num1 $opSymbol $num2 = $formattedResult"
    }
}
