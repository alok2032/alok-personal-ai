package com.ayra.ai.app.voice

/**
 * Intelligent multilingual Sentiment & Emotion Detector for Ayra AI.
 * Analyzes English, Bengali (script & phonetic Banglish), and Hindi expressions
 * to classify the user's emotional state in real time.
 */
object SentimentDetector {

    // Emojis mapping
    private val affectionateEmojis = setOf("❤️", "💖", "💕", "💗", "💓", "💞", "💘", "💌", "💋", "😘", "😻", "👩‍❤️‍👨", "💍", "🌹", "💐")
    private val sadEmojis = setOf("😢", "😭", "😞", "😔", "💔", "😿", "🥺", "🤕", "🤒", "😩", "😫", "🖤", "🌧️")
    private val excitedEmojis = setOf("🥳", "🎉", "🎊", "🤩", "😁", "😆", "🍾", "🚀", "💃", "🕺", "✨", "🔥", "💥", "⭐")
    private val anxiousAngryEmojis = setOf("😡", "😠", "🤬", "😤", "😾", "🤯", "💢", "🛑", "⚠️", "⚡")
    private val playfulEmojis = setOf("😉", "😜", "😝", "😋", "😏", "🤭", "🙈", "💃", "🧁", "🍫")

    // Keywords (Affectionate / Romantic)
    private val affectionateKeywords = listOf(
        "ভালোবাসি", "ভালবাসি", "তোমাকে চাই", "তোমায় চাই", "আমার সোনা", "আমার জান", "আমার বাবু", "মিস করছি",
        "অনেক আদর", "তুমি খুব মিষ্টি", "বিয়ে করবে", "আই লাভ ইউ", "হাগ", "কিস", "প্রিয়তমা", "ভালোবাসা", "প্রেম",
        "valobashi", "bhalobashi", "bhalo bashi", "valo bashi", "miss korchi", "miss u", "miss you",
        "love you", "love u", "so sweet", "my love", "shona", "jaan", "babu", "sweetheart", "darling",
        "honey", "tumi khub sundor", "tumi amar gf", "hug", "kiss", "cutie", "adorable", "adore you",
        "you are gorgeous", "you're cute", "babe", "baby", "cherish you", "pyaar", "mohabbat", "ishq",
        "tujhse pyaar", "meri jaan"
    )

    // Keywords (Sad / Depressed / Lonely / Vulnerable)
    private val sadKeywords = listOf(
        "মন খারাপ", "কষ্ট হচ্ছে", "কষ্ট", "কান্না পাচ্ছে", "কান্না", "একা লাগছে", "একা", "ভালো লাগছে না",
        "ডিপ্রেশন", "চিন্তা", "মাথা ব্যথা", "ক্লান্ত", "খুব ক্লান্ত", "হেরে গেলাম", "ব্রেকআপ", "কানছি",
        "চোখে জল", "অসুস্থ", "শরীর খারাপ", "মৃত্যু", "মরে যেতে ইচ্ছে", "অসহায়", "ব্যথা",
        "mon kharap", "kosto hocche", "kosto", "kanna pacche", "eka lagche", "bhalo lagche na",
        "valo lagche na", "chintay achi", "depression", "breakup", "matha betha", "klanto", "exhausted",
        "tired", "sad", "sick", "crying", "lost my", "failed", "terrible day", "bad day", "headache",
        "hopeless", "unhappy", "hurts", "in pain", "feeling down", "feeling lonely", "depressed",
        "heartbroken", "akela", "dard", "udas", "ro raha", "dukhi"
    )

    // Keywords (Excited / Joyful / Celebrating)
    private val excitedKeywords = listOf(
        "দারুণ", "দারুন", "খুব খুশি", "আনন্দ", "পাস করেছি", "চাকরি পেয়েছি", "প্রমোশন", "পার্টি", "জিতে গেছি",
        "চমৎকার", "অসাধারণ", "মজা", "হুররে", "জিতলাম", "সাফল্য", "অভিনন্দন", "লটারি",
        "darun", "khub khushi", "anondo", "pass korechi", "chakri peyechi", "promotion", "party",
        "jite gechi", "awesome", "great news", "congrats", "congratulations", "hurray", "yay", "yippee",
        "so happy", "thrilled", "fantastic", "wonderful news", "we won", "incredible", "can't believe it",
        "celebrate", "celebrating", "kamaal", "zabardast", "khush hu"
    )

    // Keywords (Anxious / Frustrated / Stressed / Angry)
    private val anxiousAngryKeywords = listOf(
        "রাগ হচ্ছে", "রাগ", "বিরক্ত", "বিরক্তি", "চুপ করো", "বাজে", "মাথা গরম", "ঝামেলা", "প্যাঁড়া",
        "টেনশন", "পাগল হয়ে যাব", "জঘন্য", "নষ্ট", "ঘেন্না", "বিরক্তিকর",
        "rag hocche", "biroktikor", "chup kor", "chup koro", "matha gorom", "tension hocche", "tension",
        "jhamele", "pera", "hate", "so angry", "furious", "pissed off", "annoyed", "irritated", "shut up",
        "hate this", "frustrated", "so stressed", "anxiety", "panicking", "stop it", "leave me alone",
        "gussa", "pagal", "pareshan"
    )

    // Keywords (Playful / Flirty / Teasing)
    private val playfulKeywords = listOf(
        "দুষ্টু", "পাগল", "ফাজলামি", "খুনসুটি", "তুমি কি আমাকে ভালোবাসো", "মিথ্যা বলছো", "পটাতে চাইছো", "লাজুক", "পাজি", "দুষ্টুমি",
        "dusto", "pagol", "fazlami", "khunsuti", "paji", "dustomi", "flirt korcho", "tease", "sotti bolcho", "are you flirting",
        "you cheeky", "silly girl", "teasing me", "you're blushing", "prove it", "naughty", "masti"
    )

    /**
     * Detects sentiment from input query or spoken phrase.
     */
    fun detectSentiment(text: String): UserSentiment {
        if (text.isBlank()) return UserSentiment.NEUTRAL_CARING

        val lower = text.lowercase().trim()

        // 1. Emoji scanning
        if (sadEmojis.any { text.contains(it) }) return UserSentiment.SAD_VULNERABLE
        if (affectionateEmojis.any { text.contains(it) }) return UserSentiment.AFFECTIONATE
        if (excitedEmojis.any { text.contains(it) }) return UserSentiment.EXCITED_JOYFUL
        if (anxiousAngryEmojis.any { text.contains(it) }) return UserSentiment.ANXIOUS_FRUSTRATED
        if (playfulEmojis.any { text.contains(it) }) return UserSentiment.PLAYFUL_FLIRTY

        // 2. Exact or substring keyword scoring
        var scoreAffectionate = 0
        var scoreSad = 0
        var scoreExcited = 0
        var scoreAnxious = 0
        var scorePlayful = 0

        for (kw in affectionateKeywords) {
            if (lower.contains(kw)) scoreAffectionate += 2
        }
        for (kw in sadKeywords) {
            if (lower.contains(kw)) scoreSad += 2
        }
        for (kw in excitedKeywords) {
            if (lower.contains(kw)) scoreExcited += 2
        }
        for (kw in anxiousAngryKeywords) {
            if (lower.contains(kw)) scoreAnxious += 2
        }
        for (kw in playfulKeywords) {
            if (lower.contains(kw)) scorePlayful += 2
        }

        // Punctuation cues
        if (lower.count { it == '!' } >= 2 && scoreExcited > 0) scoreExcited += 1
        if (lower.count { it == '?' } >= 2 && scorePlayful > 0) scorePlayful += 1

        val maxScore = maxOf(scoreAffectionate, scoreSad, scoreExcited, scoreAnxious, scorePlayful)
        if (maxScore == 0) {
            return UserSentiment.NEUTRAL_CARING
        }

        return when (maxScore) {
            scoreSad -> UserSentiment.SAD_VULNERABLE
            scoreAffectionate -> UserSentiment.AFFECTIONATE
            scoreExcited -> UserSentiment.EXCITED_JOYFUL
            scoreAnxious -> UserSentiment.ANXIOUS_FRUSTRATED
            scorePlayful -> UserSentiment.PLAYFUL_FLIRTY
            else -> UserSentiment.NEUTRAL_CARING
        }
    }
}
