package com.ayra.ai.app

import com.ayra.ai.app.ai.superpowers.AyraSuperpowersEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verification test suite for obra/superpowers integration in Ayra AI.
 */
class AyraSuperpowersEngineTest {

    @Test
    fun testSuperpowerExplicitIntentDetection() {
        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.SYSTEMATIC_DEBUGGING,
            AyraSuperpowersEngine.detectSuperpower("superpower: debug my Kotlin class")
        )

        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.TEST_DRIVEN_DEVELOPMENT,
            AyraSuperpowersEngine.detectSuperpower("superpower: tdd user authentication service")
        )

        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.BRAINSTORMING,
            AyraSuperpowersEngine.detectSuperpower("superpower: brainstorm real-time chat architecture")
        )

        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.SUBAGENT_DISPATCHER,
            AyraSuperpowersEngine.detectSuperpower("superpower: subagent parallel task orchestration")
        )

        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.CODE_REVIEW_GATE,
            AyraSuperpowersEngine.detectSuperpower("superpower: review security audit of PR")
        )
    }

    @Test
    fun testSuperpowerNaturalLanguageIntentDetection() {
        // Debugging / Crash / Exception
        val debugQuery = "Fix this bug: crash with NullPointerException in UserSession.kt { fun logout() }"
        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.SYSTEMATIC_DEBUGGING,
            AyraSuperpowersEngine.detectSuperpower(debugQuery)
        )

        // TDD / Unit Tests
        val tddQuery = "Write unit tests for PaymentProcessor with full mocking and edge cases"
        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.TEST_DRIVEN_DEVELOPMENT,
            AyraSuperpowersEngine.detectSuperpower(tddQuery)
        )

        // Code Review & Security Audit
        val reviewQuery = "Please perform a code review and security audit on this code snippet"
        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.CODE_REVIEW_GATE,
            AyraSuperpowersEngine.detectSuperpower(reviewQuery)
        )

        // Architecture & Brainstorming
        val planQuery = "Let's brainstorm the system design and architecture design for our streaming service"
        assertEquals(
            AyraSuperpowersEngine.SuperpowerType.BRAINSTORMING,
            AyraSuperpowersEngine.detectSuperpower(planQuery)
        )

        // Unrelated query should return null
        assertNull(AyraSuperpowersEngine.detectSuperpower("What is the capital of Bangladesh?"))
    }

    @Test
    fun testSuperpowerBengaliOfflineSynthesis() {
        val response = AyraSuperpowersEngine.generateOfflineSuperpowerResponse(
            AyraSuperpowersEngine.SuperpowerType.SYSTEMATIC_DEBUGGING,
            "অ্যাপ ক্র্যাশ করছে সমাধান দাও",
            isBengali = true
        )

        assertNotNull(response)
        assertTrue("Bengali response should mention সুপারপাওয়ার", response.contains("সুপারপাওয়ার ইঞ্জিন"))
        assertTrue("Bengali response should cite obra/superpowers protocol", response.contains("obra/superpowers"))
        assertTrue("Must include 4-phase checklist", response.contains("এক্সিকিউশন চেকলিস্ট"))
    }

    @Test
    fun testSuperpowerEnglishOfflineSynthesis() {
        val response = AyraSuperpowersEngine.generateOfflineSuperpowerResponse(
            AyraSuperpowersEngine.SuperpowerType.TEST_DRIVEN_DEVELOPMENT,
            "Create comprehensive unit tests",
            isBengali = false
        )

        assertNotNull(response)
        assertTrue("English response should mention Superpower Engine", response.contains("Superpower Engine"))
        assertTrue("English response should cite obra/superpowers protocol", response.contains("obra/superpowers"))
        assertTrue("Must include 4-phase checklist", response.contains("Superpowers Execution Checklist"))
    }
}
