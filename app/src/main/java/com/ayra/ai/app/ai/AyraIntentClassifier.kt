package com.ayra.ai.app.ai

import java.util.regex.Pattern

enum class AyraToolIntent {
    CHAT,
    IMAGE,
    PDF,
    TERMINAL,
    DEEP_RESEARCH
}

/**
 * Section 2.1: Robust intent classifier replacing substring routers.
 * Principles:
 * - Default = CHAT.
 * - Explicit slash / prefix commands take priority (/image, /pdf, /terminal, /research).
 * - Multi-word boundary regex with both required verb AND noun in strict sequence.
 * - ZERO false positives on ordinary questions containing tokens like "pic", "ping", "cmd", "document", "music", etc.
 */
object AyraIntentClassifier {

    // Explicit prefix patterns (case-insensitive)
    private val IMAGE_PREFIX_PATTERN = Pattern.compile("^(/(image|img|draw|paint)|image:|photo:|ছবি:|আঁকো:)\\s+.+", Pattern.CASE_INSENSITIVE)
    private val PDF_PREFIX_PATTERN = Pattern.compile("^(/(pdf|document)|pdf:|doc:|পিডিএফ:)\\s+.+", Pattern.CASE_INSENSITIVE)
    private val TERMINAL_PREFIX_PATTERN = Pattern.compile("^(/(terminal|term|cmd|shell|exec)|terminal:|টার্মিনাল:)\\s+.+", Pattern.CASE_INSENSITIVE)
    private val RESEARCH_PREFIX_PATTERN = Pattern.compile("^(/(research|deepresearch)|research:|গবেষণা:)\\s+.+", Pattern.CASE_INSENSITIVE)

    // Strict regexes with word boundaries requiring both an explicit action verb AND target noun
    private val EXPLICIT_IMAGE_REGEX = Pattern.compile(
        "\\b(draw|paint|sketch|generate|render)\\b.{1,40}\\b(an?\\s+image|a\\s+photo|a\\s+picture|a\\s+portrait|a\\s+wallpaper)\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val EXPLICIT_IMAGE_BN_REGEX = Pattern.compile(
        "(একটি|একটা|সুন্দর)?\\s*(ছবি|চিত্র|ওয়ালপেপার)\\s*(এঁকে দাও|আঁকো|বানিয়ে দাও|তৈরি করো)",
        Pattern.CASE_INSENSITIVE
    )

    private val EXPLICIT_PDF_REGEX = Pattern.compile(
        "\\b(export|compile|save|convert)\\b.{1,30}\\b(as|to|into)\\s+(a\\s+)?pdf\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val EXPLICIT_PDF_BN_REGEX = Pattern.compile(
        "(পিডিএফ|pdf)\\s*(ফাইল|ডকুমেন্ট)?\\s*(হিসেবে\\s*)?(সেভ করো|ডাউনলোড করো|এক্সপোর্ট করো|তৈরি করো)",
        Pattern.CASE_INSENSITIVE
    )

    private val EXPLICIT_TERMINAL_REGEX = Pattern.compile(
        "^\\s*(ping|traceroute|nslookup|dig|whois|curl|nmap)\\s+([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}|\\d{1,3}(\\.\\d{1,3}){3})\\s*$",
        Pattern.CASE_INSENSITIVE
    )

    fun classifyIntent(prompt: String): AyraToolIntent {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return AyraToolIntent.CHAT

        // 1. Slash / prefix commands
        if (IMAGE_PREFIX_PATTERN.matcher(trimmed).find()) return AyraToolIntent.IMAGE
        if (PDF_PREFIX_PATTERN.matcher(trimmed).find()) return AyraToolIntent.PDF
        if (TERMINAL_PREFIX_PATTERN.matcher(trimmed).find()) return AyraToolIntent.TERMINAL
        if (RESEARCH_PREFIX_PATTERN.matcher(trimmed).find()) return AyraToolIntent.DEEP_RESEARCH

        // 2. Direct diagnostic terminal command (e.g. "ping google.com", "dig cloudflare.com")
        if (EXPLICIT_TERMINAL_REGEX.matcher(trimmed).find()) {
            return AyraToolIntent.TERMINAL
        }

        // 3. Strict explicit action regexes (with verb + noun boundaries)
        if (EXPLICIT_IMAGE_REGEX.matcher(trimmed).find() || EXPLICIT_IMAGE_BN_REGEX.matcher(trimmed).find()) {
            return AyraToolIntent.IMAGE
        }

        if (EXPLICIT_PDF_REGEX.matcher(trimmed).find() || EXPLICIT_PDF_BN_REGEX.matcher(trimmed).find()) {
            return AyraToolIntent.PDF
        }

        // Default: EVERYTHING ELSE IS CHAT
        return AyraToolIntent.CHAT
    }
}
