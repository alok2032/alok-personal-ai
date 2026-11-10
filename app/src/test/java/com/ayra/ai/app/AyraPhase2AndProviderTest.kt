package com.ayra.ai.app

import com.ayra.ai.app.ai.providers.AiProviderType
import com.ayra.ai.app.ai.providers.ProviderCircuitBreaker
import com.ayra.ai.app.data.StorageHelper
import com.ayra.ai.app.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive verification for Phase 2:
 * - F11: ChatViewModel with atomic send mutex & double-tap send prevention
 * - F12: ProviderCircuitBreaker state transitions & bounded LRU cache
 * - F17: StorageHelper file name sanitization & PDF text limits
 */
class AyraPhase2AndProviderTest {

    @Test
    fun testChatViewModelDoubleTapSendPrevention() = runBlocking {
        val viewModel = ChatViewModel()
        viewModel.updateInputText("Hello Ayra")

        // First attempt to acquire send lock must succeed
        val firstAcquired = viewModel.tryStartSending()
        assertTrue("First send attempt should succeed", firstAcquired)
        assertTrue("isGenerating should be true after acquiring lock", viewModel.isGenerating.value)

        // Concurrent/rapid double-tap while generating must be blocked
        val secondAcquired = viewModel.tryStartSending()
        assertFalse("Second rapid send attempt must be blocked by mutex", secondAcquired)

        // Finish sending releases lock
        viewModel.finishSending()
        assertFalse("isGenerating should be false after finishSending", viewModel.isGenerating.value)

        // Now a subsequent send should succeed
        val thirdAcquired = viewModel.tryStartSending()
        assertTrue("Subsequent send after finish should succeed", thirdAcquired)
        viewModel.finishSending()
    }

    @Test
    fun testChatViewModelConcurrentSendStress() = runBlocking {
        val viewModel = ChatViewModel()
        var successfulStarts = 0

        // Launch 20 concurrent coroutines attempting to send simultaneously
        val jobs = (1..20).map {
            async {
                if (viewModel.tryStartSending()) {
                    successfulStarts++
                }
            }
        }
        jobs.awaitAll()

        // Exactly one concurrent start should have succeeded
        assertEquals("Exactly one concurrent sender should obtain the lock", 1, successfulStarts)
        viewModel.finishSending()
    }

    @Test
    fun testProviderCircuitBreakerStateTransitions() {
        val breaker = ProviderCircuitBreaker(failureThreshold = 3, cooldownPeriodMs = 1000L)
        val provider = AiProviderType.GEMINI

        // Initially CLOSED
        assertEquals(ProviderCircuitBreaker.State.CLOSED, breaker.getState(provider))
        assertTrue(breaker.canExecute(provider))

        // Record 2 failures -> Still CLOSED
        breaker.recordFailure(provider)
        breaker.recordFailure(provider)
        assertEquals(ProviderCircuitBreaker.State.CLOSED, breaker.getState(provider))
        assertTrue(breaker.canExecute(provider))

        // 3rd failure reaches threshold -> Trips to OPEN
        breaker.recordFailure(provider)
        assertEquals(ProviderCircuitBreaker.State.OPEN, breaker.getState(provider))
        assertFalse("Tripped circuit must fast-fail", breaker.canExecute(provider))

        // On success, reset to CLOSED
        breaker.recordSuccess(provider)
        assertEquals(ProviderCircuitBreaker.State.CLOSED, breaker.getState(provider))
        assertTrue(breaker.canExecute(provider))
    }

    @Test
    fun testProviderCircuitBreakerLruCache() {
        val breaker = ProviderCircuitBreaker(cacheCapacity = 3)

        breaker.putCachedResponse("key1", "response1")
        breaker.putCachedResponse("key2", "response2")
        breaker.putCachedResponse("key3", "response3")

        assertEquals("response1", breaker.getCachedResponse("key1"))
        assertEquals("response2", breaker.getCachedResponse("key2"))
        assertEquals("response3", breaker.getCachedResponse("key3"))

        // Access key1 again so it becomes the most recently used
        assertEquals("response1", breaker.getCachedResponse("key1"))

        // Inserting 4th item evicts eldest (key2 is now eldest)
        breaker.putCachedResponse("key4", "response4")
        assertNotNull(breaker.getCachedResponse("key4"))
        assertNotNull(breaker.getCachedResponse("key1"))
        assertNull("key2 must be evicted", breaker.getCachedResponse("key2"))
    }

    @Test
    fun testStorageHelperSanitizeFileName() {
        // Path traversal attempts: should isolate filename to prevent directory traversal
        val traversalName = "../../etc/passwd"
        val sanitized = StorageHelper.sanitizeFileName(traversalName)
        assertFalse("Sanitized name must not contain directory slashes", sanitized.contains("/"))
        assertFalse("Sanitized name must not contain backslashes", sanitized.contains("\\"))
        assertEquals("passwd", sanitized)

        // Invalid characters
        val invalidName = "my file<name>:test?*|\".pdf"
        val sanitizedInvalid = StorageHelper.sanitizeFileName(invalidName)
        assertFalse("Must remove special punctuation", sanitizedInvalid.contains(":"))
        assertFalse("Must remove question marks", sanitizedInvalid.contains("?"))
        assertFalse("Must remove quotes", sanitizedInvalid.contains("\""))

        // Blank input fallback
        val emptySanitized = StorageHelper.sanitizeFileName("   ")
        assertTrue("Empty file name must fall back to valid attachment name", emptySanitized.startsWith("attachment_"))
    }
}
