package com.ayra.ai.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Section 5.1 & 6.1: NoFakeOutputTest.
 * Fails the build if forbidden fake/simulation/marketing buzzwords or canned fake echoes exist in main source.
 */
class NoFakeOutputTest {

    private val forbiddenPhrases = listOf(
        "### ⚡ OmniRoute Optimized Response",
        "Layer-38 abliterated weights",
        "100% success",
        "zero rate-limit",
        "the world's best",
        "world's best ai app",
        "Open-Higgsfield 4K cinematic video synthesized",
        "ছবির ফরম্যাট সফলভাবে",
        "Ayra VPN Privacy Shield is now ACTIVE"
    )

    @Test
    fun testNoFakeOutputInMainSource() {
        val mainSrcDir = File("src/main/java")
        val targetDir = if (mainSrcDir.exists()) mainSrcDir else File("app/src/main/java")
        assertTrue("Main source directory should exist at ${targetDir.absolutePath}", targetDir.exists())

        val violations = mutableListOf<String>()

        targetDir.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .forEach { file ->
                val lines = file.readLines()
                lines.forEachIndexed { index, line ->
                    // Skip comments or test classes
                    val trimmed = line.trim()
                    if (trimmed.startsWith("//") || trimmed.startsWith("*")) return@forEachIndexed

                    for (phrase in forbiddenPhrases) {
                        if (line.contains(phrase, ignoreCase = false)) {
                            violations.add("${file.name}:${index + 1} contains forbidden phrase: \"$phrase\"")
                        }
                    }
                }
            }

        assertTrue(
            "Found fake/simulation/echo artifacts in main source code:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}
