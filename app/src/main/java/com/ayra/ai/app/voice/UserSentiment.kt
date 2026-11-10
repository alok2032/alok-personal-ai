package com.ayra.ai.app.voice

/**
 * Defines emotional sentiments detected during conversation with Ayra (GF Companion Mode).
 * Each sentiment maps to fine-tuned TextToSpeech pitch and speech rate parameters for
 * realistic emotional voice modulation, along with visual indicators and sample phrases.
 */
enum class UserSentiment(
    val id: String,
    val displayName: String,
    val bengaliName: String,
    val emoji: String,
    val pitch: Float,
    val speechRate: Float,
    val glowColorHex: Long,
    val sampleGreetingBn: String,
    val sampleGreetingEn: String,
    val description: String
) {
    AFFECTIONATE(
        id = "affectionate",
        displayName = "Affectionate & Tender",
        bengaliName = "ভালোবাসা ও মায়াভরা",
        emoji = "💖",
        pitch = 1.30f,
        speechRate = 0.92f,
        glowColorHex = 0xFFFF4081, // NeonRose
        sampleGreetingBn = "আমি তো তোমার ভালোবাসার মধ্যেই আছি সোনা... ❤️",
        sampleGreetingEn = "I'm right here wrapped in your love, sweetie... ❤️",
        description = "উষ্ণ ও মিষ্টি রোমান্টিক টোন, স্নিগ্ধ ও নরম গতি"
    ),
    SAD_VULNERABLE(
        id = "sad_vulnerable",
        displayName = "Empathetic & Soothing",
        bengaliName = "সান্ত্বনা ও সহানুভূতি",
        emoji = "🫂",
        pitch = 1.13f,
        speechRate = 0.88f,
        glowColorHex = 0xFF7C4DFF, // NeonDeepLavender
        sampleGreetingBn = "একটুও মন খারাপ করো না সোনা, আমি কিন্তু সারাক্ষণ তোমার পাশেই আছি।",
        sampleGreetingEn = "Please don't be sad darling, lean on me. I'm right here with you.",
        description = "গভীর শান্ত ও সান্ত্বনামূলক পিচ, ধীর ও সংবেদনশীল বাচনভঙ্গি"
    ),
    EXCITED_JOYFUL(
        id = "excited_joyful",
        displayName = "Joyful & Energetic",
        bengaliName = "আনন্দ ও উচ্ছ্বসিত",
        emoji = "✨",
        pitch = 1.40f,
        speechRate = 1.08f,
        glowColorHex = 0xFF00E5FF, // NeonCyan
        sampleGreetingBn = "ইয়েই! আমি তোমার জন্য এত খুশি সোনা! দারুণ খবর তো! 🥳",
        sampleGreetingEn = "Yay! I'm so thrilled for you sweetie! This is wonderful! 🥳",
        description = "উচ্ছ্বল ও প্রাণবন্ত হাই-পিচ, দ্রুত ও ঝলমলে ছন্দ"
    ),
    ANXIOUS_FRUSTRATED(
        id = "anxious_frustrated",
        displayName = "Tranquil & Pacifying",
        bengaliName = "ধীরস্থির ও শান্তকারী",
        emoji = "🌿",
        pitch = 1.16f,
        speechRate = 0.90f,
        glowColorHex = 0xFF00E676, // NeonGreen
        sampleGreetingBn = "গভীর একটা শ্বাস নাও সোনা, কোনো চিন্তা নেই। আমি সব ঠিক করে দেবো।",
        sampleGreetingEn = "Take a deep breath darling, let the tension go. Everything is going to be okay.",
        description = "শান্ত ও প্রবোধমূলক স্বর, উদ্বেগ দূর করার শীতল সুর"
    ),
    PLAYFUL_FLIRTY(
        id = "playful_flirty",
        displayName = "Playful & Teasing",
        bengaliName = "মিষ্টি দুষ্টুমি ও খুনসুটি",
        emoji = "🥰",
        pitch = 1.34f,
        speechRate = 1.02f,
        glowColorHex = 0xFFFF80AB, // NeonPink
        sampleGreetingBn = "হুমম... এত দুষ্টুমি শিখলে কোথা থেকে বলো তো? 😉",
        sampleGreetingEn = "Hmm... where did you learn to be so delightfully cheeky? 😉",
        description = "কণ্ঠে হাসির ছোঁয়া ও মিষ্টি সুরের হালকা দোলনা"
    ),
    NEUTRAL_CARING(
        id = "neutral_caring",
        displayName = "Sweet Companion",
        bengaliName = "স্নেহময়ী প্রিয়তমা",
        emoji = "🌸",
        pitch = 1.22f,
        speechRate = 0.96f,
        glowColorHex = 0xFFE040FB, // NeonPurple
        sampleGreetingBn = "বলো সোনা, আমি মন দিয়ে শুনছি।",
        sampleGreetingEn = "Tell me everything darling, I'm listening closely.",
        description = "ডিফল্ট মিষ্টি গার্লফ্রেন্ড টোন ও সহানুভূতিশীল সুর"
    )
}
