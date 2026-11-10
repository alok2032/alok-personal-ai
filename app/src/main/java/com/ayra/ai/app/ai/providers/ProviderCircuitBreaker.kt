package com.ayra.ai.app.ai.providers

import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Production Circuit Breaker & LRU Cache for AI Providers (F12).
 * Prevents cascading timeouts and quota exhaustion by short-circuiting failing providers.
 * Bounded thread-safe LRU cache provides 0ms responses for duplicate prompts.
 */
class ProviderCircuitBreaker(
    private val failureThreshold: Int = 3,
    private val cooldownPeriodMs: Long = 60_000L,
    cacheCapacity: Int = 100
) {

    enum class State {
        CLOSED,     // Healthy, normal execution
        OPEN,       // Tripped, fast-fail without network call
        HALF_OPEN   // Testing single probe request
    }

    private data class ProviderStatus(
        var state: State = State.CLOSED,
        var consecutiveFailures: Int = 0,
        var lastFailureTime: Long = 0L
    )

    private val providerStates = ConcurrentHashMap<AiProviderType, ProviderStatus>()

    // Thread-safe Bounded LRU Cache (works across Android device and JVM Unit Tests)
    private val responseCache: MutableMap<String, String> = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(cacheCapacity, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
                return size > cacheCapacity
            }
        }
    )

    /**
     * Checks if provider is allowed to execute a request.
     * Returns true if provider is CLOSED or cooldown has elapsed (HALF_OPEN).
     */
    fun canExecute(provider: AiProviderType): Boolean {
        val status = providerStates.computeIfAbsent(provider) { ProviderStatus() }
        val now = System.currentTimeMillis()

        return when (status.state) {
            State.CLOSED -> true
            State.OPEN -> {
                if (now - status.lastFailureTime > cooldownPeriodMs) {
                    status.state = State.HALF_OPEN
                    true
                } else {
                    false
                }
            }
            State.HALF_OPEN -> true
        }
    }

    /**
     * Records a successful execution. Resets failure count and closes circuit.
     */
    fun recordSuccess(provider: AiProviderType) {
        val status = providerStates.computeIfAbsent(provider) { ProviderStatus() }
        status.state = State.CLOSED
        status.consecutiveFailures = 0
    }

    /**
     * Records a failure. If consecutive failures reach threshold, trips circuit to OPEN.
     */
    fun recordFailure(provider: AiProviderType) {
        val status = providerStates.computeIfAbsent(provider) { ProviderStatus() }
        status.consecutiveFailures++
        status.lastFailureTime = System.currentTimeMillis()

        if (status.consecutiveFailures >= failureThreshold) {
            status.state = State.OPEN
        }
    }

    fun getCachedResponse(cacheKey: String): String? = responseCache[cacheKey]

    fun putCachedResponse(cacheKey: String, response: String) {
        if (response.isNotBlank()) {
            responseCache[cacheKey] = response
        }
    }

    fun clearCache() {
        responseCache.clear()
    }

    fun resetAll() {
        providerStates.clear()
        responseCache.clear()
    }

    fun getState(provider: AiProviderType): State {
        return providerStates[provider]?.state ?: State.CLOSED
    }
}
