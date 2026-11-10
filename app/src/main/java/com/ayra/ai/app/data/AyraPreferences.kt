package com.ayra.ai.app.data

import android.content.Context
import android.content.SharedPreferences
import com.ayra.ai.app.BuildConfig
import com.ayra.ai.app.security.SecureKeyStore

/**
 * Manages Ayra AI local preferences, custom API credentials, and runtime user overrides.
 * All API keys are securely delegated to [SecureKeyStore] using hardware-backed AES-256-GCM.
 */
object AyraPreferences {
    private const val PREF_NAME = "ayra_ai_user_prefs"
    private const val KEY_ACTIVE_PERSONA = "active_persona_mode"
    private const val KEY_SELECTED_MODEL_TIER = "selected_model_tier"
    private const val KEY_SELECTED_PROVIDER = "selected_provider_type"
    private const val KEY_AUTO_ROUTER_ENABLED = "auto_router_enabled"
    private const val KEY_SEARCH_GROUNDING_ENABLED = "search_grounding_enabled"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // --- Provider Keys (Hardware Keystore Encrypted) ---

    fun getEffectiveGeminiKey(context: Context?): String {
        if (context == null) return BuildConfig.GEMINI_API_KEY
        val secureKey = SecureKeyStore.getSecret(context, SecureKeyStore.KEY_GEMINI)
        return if (secureKey.isNotBlank()) secureKey else BuildConfig.GEMINI_API_KEY
    }

    fun getEffectiveOpenRouterKey(context: Context?): String {
        if (context == null) return BuildConfig.OPENROUTER_API_KEY
        val secureKey = SecureKeyStore.getSecret(context, SecureKeyStore.KEY_OPENROUTER)
        return if (secureKey.isNotBlank()) secureKey else BuildConfig.OPENROUTER_API_KEY
    }

    fun setCustomGeminiKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_GEMINI, key)
    }

    fun setCustomOpenRouterKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_OPENROUTER, key)
    }

    fun getCustomGeminiKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_GEMINI)

    fun getCustomOpenRouterKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_OPENROUTER)

    // Additional Major Providers
    fun getOpenAiKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_OPENAI)

    fun setOpenAiKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_OPENAI, key)
    }

    fun getAnthropicKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_ANTHROPIC)

    fun setAnthropicKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_ANTHROPIC, key)
    }

    fun getDeepSeekKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_DEEPSEEK)

    fun setDeepSeekKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_DEEPSEEK, key)
    }

    fun getXAiKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_XAI)

    fun setXAiKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_XAI, key)
    }

    fun getMistralKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_MISTRAL)

    fun setMistralKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_MISTRAL, key)
    }

    fun getPerplexityKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_PERPLEXITY)

    fun setPerplexityKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_PERPLEXITY, key)
    }

    fun getCustomEndpointUrl(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_CUSTOM_ENDPOINT_URL)

    fun setCustomEndpointUrl(context: Context, url: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_CUSTOM_ENDPOINT_URL, url)
    }

    fun getCustomEndpointKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_CUSTOM_ENDPOINT_KEY)

    fun setCustomEndpointKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_CUSTOM_ENDPOINT_KEY, key)
    }

    fun getTavilySearchKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_TAVILY_SEARCH)

    fun setTavilySearchKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_TAVILY_SEARCH, key)
    }

    fun getElevenLabsKey(context: Context): String =
        SecureKeyStore.getSecret(context, SecureKeyStore.KEY_ELEVENLABS)

    fun setElevenLabsKey(context: Context, key: String) {
        SecureKeyStore.putSecret(context, SecureKeyStore.KEY_ELEVENLABS, key)
    }

    fun hasAnyCustomKeyConfigured(context: Context): Boolean {
        return getCustomGeminiKey(context).isNotBlank() ||
                getCustomOpenRouterKey(context).isNotBlank() ||
                getOpenAiKey(context).isNotBlank() ||
                getAnthropicKey(context).isNotBlank() ||
                getDeepSeekKey(context).isNotBlank() ||
                getXAiKey(context).isNotBlank() ||
                getMistralKey(context).isNotBlank() ||
                getPerplexityKey(context).isNotBlank() ||
                getTavilySearchKey(context).isNotBlank() ||
                getElevenLabsKey(context).isNotBlank() ||
                getCustomEndpointUrl(context).isNotBlank()
    }

    fun hasCustomKeyConfigured(context: Context): Boolean = hasAnyCustomKeyConfigured(context)

    // --- Persona & Routing Preferences ---

    fun savePersona(context: Context, persona: String) {
        getPrefs(context).edit().putString(KEY_ACTIVE_PERSONA, persona).apply()
    }

    fun getSavedPersona(context: Context): String {
        return getPrefs(context).getString(KEY_ACTIVE_PERSONA, "AI_ASSISTANT") ?: "AI_ASSISTANT"
    }

    fun saveModelTier(context: Context, tier: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_MODEL_TIER, tier).apply()
    }

    fun getSavedModelTier(context: Context): String {
        return getPrefs(context).getString(KEY_SELECTED_MODEL_TIER, "auto") ?: "auto"
    }

    fun saveSelectedProvider(context: Context, providerId: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_PROVIDER, providerId).apply()
    }

    fun getSelectedProvider(context: Context): String {
        return getPrefs(context).getString(KEY_SELECTED_PROVIDER, "AUTO") ?: "AUTO"
    }

    fun isAutoRouterEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AUTO_ROUTER_ENABLED, true)
    }

    fun setAutoRouterEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_ROUTER_ENABLED, enabled).apply()
    }

    fun isSearchGroundingEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SEARCH_GROUNDING_ENABLED, true)
    }

    fun setSearchGroundingEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SEARCH_GROUNDING_ENABLED, enabled).apply()
    }

    // --- Intelligent Routing Modes ---
    private const val KEY_ROUTING_MODE = "intelligent_routing_mode"

    fun getRoutingMode(context: Context): String {
        return getPrefs(context).getString(KEY_ROUTING_MODE, "auto") ?: "auto"
    }

    fun setRoutingMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_ROUTING_MODE, mode).apply()
    }

    // --- AI Memory & Personalization Settings ---
    private const val KEY_MEMORY_ENABLED = "ai_memory_enabled"
    private const val KEY_USER_CUSTOM_INSTRUCTIONS = "user_custom_instructions"
    private const val KEY_REMEMBERED_FACTS = "user_remembered_facts"

    fun isMemoryEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MEMORY_ENABLED, true)
    }

    fun setMemoryEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MEMORY_ENABLED, enabled).apply()
    }

    fun getUserCustomInstructions(context: Context): String {
        return getPrefs(context).getString(KEY_USER_CUSTOM_INSTRUCTIONS, "") ?: ""
    }

    fun setUserCustomInstructions(context: Context, instructions: String) {
        getPrefs(context).edit().putString(KEY_USER_CUSTOM_INSTRUCTIONS, instructions).apply()
    }

    fun getRememberedFacts(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_REMEMBERED_FACTS, emptySet()) ?: emptySet()
    }

    fun addRememberedFact(context: Context, fact: String) {
        val current = getRememberedFacts(context).toMutableSet()
        current.add(fact)
        getPrefs(context).edit().putStringSet(KEY_REMEMBERED_FACTS, current).apply()
    }

    fun removeRememberedFact(context: Context, fact: String) {
        val current = getRememberedFacts(context).toMutableSet()
        current.remove(fact)
        getPrefs(context).edit().putStringSet(KEY_REMEMBERED_FACTS, current).apply()
    }

    fun clearAllMemory(context: Context) {
        getPrefs(context).edit().remove(KEY_REMEMBERED_FACTS).apply()
    }

    // --- Local Ollama / LM Studio Host Configuration ---
    private const val KEY_OLLAMA_HOST = "local_ollama_host"

    fun getOllamaHost(context: Context): String {
        return getPrefs(context).getString(KEY_OLLAMA_HOST, "http://10.0.2.2:11434") ?: "http://10.0.2.2:11434"
    }

    fun setOllamaHost(context: Context, host: String) {
        val clean = if (host.endsWith("/")) host.dropLast(1) else host
        getPrefs(context).edit().putString(KEY_OLLAMA_HOST, clean).apply()
    }
}
