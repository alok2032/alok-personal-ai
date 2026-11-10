package com.ayra.ai.app.search

import android.content.Context
import com.ayra.ai.app.data.AyraPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Production Tavily Live Web Search Engine.
 * Delivers real, citation-grounded live search results from across the web.
 * Uses on-device AES-256 encrypted API key.
 */
class TavilySearchEngine(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    data class SearchResult(
        val title: String,
        val url: String,
        val content: String,
        val score: Double = 0.0
    )

    data class SearchResponse(
        val query: String,
        val results: List<SearchResult>,
        val formattedSummary: String,
        val latencyMs: Long
    )

    /**
     * Executes real web search query via Tavily API.
     * Returns structured results and formatted citation block.
     */
    suspend fun search(
        query: String,
        maxResults: Int = 4,
        searchDepth: String = "basic"
    ): SearchResponse = withContext(Dispatchers.IO) {
        val apiKey = AyraPreferences.getTavilySearchKey(context).trim()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("No Tavily Search API key configured. Please add one in AI Settings.")
        }

        val start = System.currentTimeMillis()

        val jsonPayload = JSONObject().apply {
            put("api_key", apiKey)
            put("query", query.trim())
            put("search_depth", searchDepth)
            put("include_answer", true)
            put("max_results", maxResults.coerceIn(1, 10))
        }

        val request = Request.Builder()
            .url("https://api.tavily.com/search")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        response.use { resp ->
            if (resp.code == 401 || resp.code == 403) {
                throw IOException("Invalid Tavily API Key (HTTP ${resp.code}). Please verify your key at tavily.com.")
            }
            if (resp.code == 429) {
                throw IOException("Tavily search rate limit or quota exceeded.")
            }
            if (!resp.isSuccessful) {
                throw IOException("Tavily Search API error: HTTP ${resp.code} ${resp.message}")
            }

            val body = resp.body?.string() ?: throw IOException("Empty response from Tavily search.")
            val json = JSONObject(body)

            val resultsArray = json.optJSONArray("results") ?: JSONArray()
            val searchResults = mutableListOf<SearchResult>()

            for (i in 0 until resultsArray.length()) {
                val item = resultsArray.getJSONObject(i)
                searchResults.add(
                    SearchResult(
                        title = item.optString("title", "Untitled Source"),
                        url = item.optString("url", ""),
                        content = item.optString("content", "").take(600),
                        score = item.optDouble("score", 0.0)
                    )
                )
            }

            val directAnswer = json.optString("answer", "")
            val sb = StringBuilder()
            if (directAnswer.isNotBlank()) {
                sb.append("### 🌐 Live Web Search Overview\n")
                sb.append(directAnswer.trim()).append("\n\n")
            }

            if (searchResults.isNotEmpty()) {
                sb.append("### 📚 Grounded Web Sources & Citations\n")
                searchResults.forEachIndexed { index, res ->
                    sb.append("**[${index + 1}] [${res.title}](${res.url})**\n")
                    sb.append("> ${res.content.trim()}\n\n")
                }
            }

            SearchResponse(
                query = query,
                results = searchResults,
                formattedSummary = sb.toString(),
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    /**
     * Real connection test against Tavily API.
     * Returns latency in milliseconds or throws descriptive exception.
     */
    suspend fun testConnection(customKey: String? = null): Long = withContext(Dispatchers.IO) {
        val apiKey = (customKey ?: AyraPreferences.getTavilySearchKey(context)).trim()
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Tavily API key is empty.")
        }

        val start = System.currentTimeMillis()
        val jsonPayload = JSONObject().apply {
            put("api_key", apiKey)
            put("query", "ping test")
            put("search_depth", "basic")
            put("max_results", 1)
        }

        val request = Request.Builder()
            .url("https://api.tavily.com/search")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .build()

        httpClient.newCall(request).execute().use { resp ->
            if (resp.code == 401 || resp.code == 403) {
                throw IOException("Invalid Tavily API key (HTTP ${resp.code})")
            }
            if (!resp.isSuccessful) {
                throw IOException("Tavily ping error: HTTP ${resp.code}")
            }
            System.currentTimeMillis() - start
        }
    }
}
