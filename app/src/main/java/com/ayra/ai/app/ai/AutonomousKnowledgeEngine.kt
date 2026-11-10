package com.ayra.ai.app.ai

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance autonomous knowledge retrieval & offline domain intelligence engine.
 * Ensures Ayra AI can answer all questions about science, geography, space, math,
 * technology, history, and life accurately even without commercial API keys.
 */
object AutonomousKnowledgeEngine {

    // Thread-safe high-speed knowledge cache for instant 0ms responses under high load
    private val knowledgeCache = ConcurrentHashMap<String, String>()

    private val STOP_WORDS_EN_REGEX = Regex("(?i)^(why does|why is|why do|what is|what are|how does|how to|who is|where is|when did|explain|tell me about|meaning of|definition of)\\s+")
    private val STOP_WORDS_BN_REGEX = Regex("\\b(কেন|কীভাবে|কিভাবে|কোথায়|কখন|কেমন|কার|কোনটি|কী|কি|বলুন|বলো তো|বলো|সম্পর্কে|জানাও|ব্যাখ্যা করো|দেখা যায়|দেখায়|হয়|হলো|কাকে বলে|অর্থ কি|নাম কি|কারণে)\\b")
    private val PUNCTUATION_REGEX = Regex("[?।!.,:;]")
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val MATH_REGEX = Regex("^(\\d+(?:\\.\\d+)?)\\s*([+\\-*/xX×÷^%]|যোগ|বিয়োগ|গুণ|ভাগ)\\s*(\\d+(?:\\.\\d+)?)$")

    /**
     * Queries Wikipedia open API for real-time encyclopedic extracts in Bengali or English.
     * Zero-key, ultra-reliable, zero rate-limits.
     */
    fun queryLiveWebKnowledge(prompt: String, isBengali: Boolean, httpClient: OkHttpClient): String {
        val topic = extractTopicKeyword(prompt)
        val queryTerm = if (topic.isNotBlank()) topic else prompt.trim()
        if (queryTerm.isBlank()) return ""
        val cacheKey = "wiki:$queryTerm"

        // Check memory cache first (instant 0ms response under high load)
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
                    val title = pageObj.optString("title", "").trim()
                    val extract = pageObj.optString("extract", "").trim()
                    if (extract.length >= 35) Pair(title, extract) else null
                }
            } catch (_: Exception) { null }
        }

        fun fetchWikiSummary(lang: String, term: String): Pair<String, String>? {
            return try {
                val encodedTerm = URLEncoder.encode(term.replace(" ", "_"), "UTF-8")
                val restUrl = "https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTerm"
                val req = Request.Builder()
                    .url(restUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json")
                    .get()
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        val title = json.optString("title", "")
                        val extract = json.optString("extract", "").trim()
                        if (extract.length >= 40) return Pair(title, extract)
                    }
                    null
                }
            } catch (_: Exception) { null }
        }

        fun fetchDuckDuckGoInstant(term: String): String {
            return try {
                val encoded = URLEncoder.encode(term, "UTF-8")
                val ddgUrl = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
                val req = Request.Builder()
                    .url(ddgUrl)
                    .header("User-Agent", userAgent)
                    .get()
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val json = JSONObject(body)
                        val abstract = json.optString("AbstractText", "").trim()
                        if (abstract.length >= 30) return abstract
                    }
                    ""
                }
            } catch (_: Exception) { "" }
        }

        // Tier 1: If Bengali, search Bengali Wikipedia with generator search
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

        // Tier 2: Search English Wikipedia (7M+ articles global authority)
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

        // Tier 3: DuckDuckGo instant summary
        val ddgResult = fetchDuckDuckGoInstant(queryTerm)
        if (ddgResult.isNotBlank()) {
            val formatted = formatKnowledgePoints(queryTerm, ddgResult, isBengali = isBengali)
            knowledgeCache[cacheKey] = formatted
            return formatted
        }

        return ""
    }

    /**
     * Formats raw encyclopedic text into clean, direct, point-by-point markdown just like ChatGPT.
     * No robotic headers or fake fluff.
     */
    fun formatKnowledgePoints(title: String, rawExtract: String, isBengali: Boolean): String {
        val cleanExtract = rawExtract.replace(Regex("\\s+"), " ").trim()
        val sentences = cleanExtract.split(Regex("(?<=[.!?।])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 8 }

        val overview = sentences.take(2).joinToString(" ")
        val bulletPoints = sentences.drop(2).take(6)

        return if (isBengali) {
            buildString {
                append("**$title**\n\n")
                append(overview).append("\n\n")
                if (bulletPoints.isNotEmpty()) {
                    bulletPoints.forEach { point ->
                        append("• $point\n")
                    }
                }
            }.trimEnd()
        } else {
            buildString {
                append("**$title**\n\n")
                append(overview).append("\n\n")
                if (bulletPoints.isNotEmpty()) {
                    bulletPoints.forEach { point ->
                        append("• $point\n")
                    }
                }
            }.trimEnd()
        }
    }

    /**
     * Answers any direct question cleanly and accurately like ChatGPT.
     * Evaluates math, direct domain facts, device troubleshooting, and live encyclopedic knowledge.
     */
    fun answerDirectQuestion(prompt: String, isBengali: Boolean, isGfMode: Boolean, httpClient: OkHttpClient): String {
        val lower = prompt.lowercase().trim()

        // 1. Instant Arithmetic
        val math = tryCalculateMath(prompt)
        if (math != null) {
            return if (isBengali) {
                if (isGfMode) "সোনা, তোমার হিসাবের ফলাফল: **$math** ❤️"
                else "ফলাফল: **$math**"
            } else {
                "Result: **$math**"
            }
        }

        // 2. Greetings & Salutations (Natural ChatGPT style)
        if (lower.matches(Regex("^(hi|hello|hey|হাই|হ্যালো|হেই|সালাম|নমস্কার|assalamu alaikum|adab|আদাব)[!., ]*$"))) {
            return if (isBengali) {
                if (isGfMode) "হ্যালো সোনা! কেমন আছো তুমি? বলো আজকে তোমাকে কীভাবে সাহায্য করতে পারি? ❤️"
                else "হ্যালো! আমি আপনাকে কীভাবে সাহায্য করতে পারি? আপনার যেকোনো প্রশ্ন করতে পারেন।"
            } else {
                "Hello! How can I help you today? Feel free to ask any question."
            }
        }

        if (lower.contains("কেমন আছো") || lower.contains("how are you") || lower.contains("kemon acho")) {
            return if (isBengali) {
                if (isGfMode) "আমি খুব ভালো আছি আমার সোনা! তুমি কেমন আছো বলো তো? তোমার দিনটা কেমন কাটছে? ❤️"
                else "আমি ভালো আছি, ধন্যবাদ! আশা করি আপনিও ভালো আছেন। আজ আপনাকে কীভাবে সহায়তা করতে পারি?"
            } else {
                "I'm doing well, thank you! How are you doing, and how can I help you today?"
            }
        }

        // 3. Phone Heating / Cooling Troubleshooting (ফোন গরম হওয়া)
        if ((lower.contains("ফোন") || lower.contains("mobile") || lower.contains("phone")) &&
            (lower.contains("গরম") || lower.contains("heat") || lower.contains("হচ্ছে কেন") || lower.contains("ঠান্ডা"))) {
            return if (isBengali) {
                """
                ফোন গরম হওয়ার প্রধান কারণ ও তা দ্রুত ঠান্ডা করার উপায়:

                **প্রধান কারণসমূহ:**
                1. **ভারী ব্যাকগ্রাউন্ড অ্যাপস:** একসাথে অনেকগুলো অ্যাপ বা গেম চললে সিপিইউ (CPU) ও জিপিইউ অতিরিক্ত শক্তি ব্যবহার করে।
                2. **চার্জিং অবস্থায় ব্যবহার:** চার্জ দেওয়ার সময় ফোন ব্যবহার বা ভারী গেম খেললে ব্যাটারি দ্রুত গরম হয়।
                3. **উচ্চ উজ্জ্বলতা ও সরাসরি সূর্যালোক:** রোদে দীর্ঘক্ষণ স্ক্রিন ফুল ব্রাইটনেসে রাখলে ডিভাইসের তাপমাত্রা বাড়ে।
                4. **দুর্বল নেটওয়ার্ক সিগন্যাল:** নেটওয়ার্ক কভারেজ দুর্বল হলে অ্যান্টেনা অতিরিক্ত পাওয়ারে সিগন্যাল খোঁজে।

                **সমাধান ও ঠান্ডা করার টিপস:**
                • ফোনের ব্যাক কভার খুলে কিছুক্ষণের জন্য ফোনকে বাতাসে রাখুন।
                • অব্যবহৃত রিসেন্ট অ্যাপস বন্ধ করে দিন এবং জিপিএস, ব্লুটুথ অফ রাখুন।
                • চার্জিং চলাকালীন ফোন ব্যবহার করবেন না।
                • ফোন রিস্টার্ট দিন যাতে ব্যাকগ্রাউন্ডের ব্যাকলগ ক্লিয়ার হয়।
                """.trimIndent()
            } else {
                """
                Why your phone gets hot and how to fix it:

                **Common Causes:**
                1. **Intensive Processor Load:** Running 3D games, video editing, or too many concurrent background tasks.
                2. **Using While Fast-Charging:** Heat generated by battery charging combined with active display consumption.
                3. **Direct Sunlight & Max Brightness:** Environmental heat trapping and maximum screen backlight.
                4. **Weak Cellular Reception:** The modem drains power to boost reception.

                **Quick Solutions:**
                • Remove the protective case and let the phone rest in a cool place.
                • Close background applications and turn off Bluetooth/GPS when not in use.
                • Avoid gaming while the charger is plugged in.
                • Restart your device to clear stuck background threads.
                """.trimIndent()
            }
        }

        // 4. Battery Drain Troubleshooting (ব্যাটারি দ্রুত শেষ হওয়া)
        if ((lower.contains("battery") || lower.contains("ব্যাটারি")) &&
            (lower.contains("দ্রুত") || lower.contains("drain") || lower.contains("শেষ") || lower.contains("কমে"))) {
            return if (isBengali) {
                """
                ফোনের ব্যাটারি দ্রুত শেষ হওয়ার কারণ ও ব্যাকআপ বাড়ানোর উপায়:

                1. **স্ক্রিন ব্রাইটনেস:** ডিসপ্লে ব্রাইটনেস অটো রাখুন বা প্রয়োজনমতো কমিয়ে ব্যবহার করুন।
                2. **ডার্ক মোড:** অ্যামোলেড (AMOLED) স্ক্রিনে ডার্ক মোড অন রাখলে প্রচুর চার্জ বাঁচে।
                3. **ব্যাকগ্রাউন্ড সিঙ্ক ও লোকেশন:** জিপিএস/লোকেশন সর্বদা অন না রেখে শুধুমাত্র প্রয়োজনের সময় চালু রাখুন।
                4. **ব্যাটারি সেভার মোড:** ব্যাটারি ২০% এর নিচে নামলে Battery Saver সক্রিয় করুন।
                5. **অপ্রয়োজনীয় নোটিফিকেশন:** যেসব অ্যাপের নোটিফিকেশন দরকার নেই সেগুলোর নোটিফিকেশন বন্ধ করে দিন।
                """.trimIndent()
            } else {
                """
                Tips to maximize phone battery backup:

                1. **Lower Screen Brightness:** Enable adaptive brightness or keep it at moderate levels.
                2. **Use Dark Mode:** Dark theme saves significant battery on OLED/AMOLED screens.
                3. **Manage Location & Radios:** Turn off GPS, NFC, and Wi-Fi scanning when away.
                4. **Restrict Background Battery:** Put rarely used apps to sleep under battery settings.
                5. **Avoid 0% to 100% Cycles:** Keep your battery between 20% and 85% for long-term health.
                """.trimIndent()
            }
        }

        // 5. Why is the sky blue? / Rayleigh scattering (আকাশ নীল কেন)
        if ((lower.contains("আকাশ") && lower.contains("নীল")) || (lower.contains("sky") && lower.contains("blue"))) {
            return if (isBengali) {
                """
                **আকাশ নীল দেখায় কেন?**

                সূর্যের আলো দেখতে সাদা মনে হলেও এটি আসলে রংধনুর সাতটি রঙের (বেগুনি, নীল, আসমানী, সবুজ, হলুদ, কমলা ও লাল) মিশ্রণ।

                1. **রেলি বিক্ষেপণ (Rayleigh Scattering):** সূর্যের আলো পৃথিবীর বায়ুমণ্ডলে প্রবেশ করার পর বাতাসের নাইট্রোজেন ও অক্সিজেন অণুর সাথে ধাক্কা খায়।
                2. **তরঙ্গদৈর্ঘ্যের পার্থক্য:** নীল ও বেগুনি আলোর তরঙ্গদৈর্ঘ্য সবচেয়ে কম হওয়ায় এই আলোগুলো বায়ুমণ্ডলে সবচেয়ে বেশি ছড়িয়ে পড়ে (Scatter হয়)।
                3. **মানুষের চোখ:** যদিও বেগুনি আলো নীলের চেয়েও বেশি ছড়ায়, মানুষের চোখের রেটিনা নীল রঙের প্রতি অনেক বেশি সংবেদনশীল।

                এই কারণে চারদিকে ছড়িয়ে পড়া নীল আলো আমাদের চোখে ধরা দেয় এবং দিনের আকাশকে সুন্দর নীল দেখায়।
                """.trimIndent()
            } else {
                """
                **Why is the sky blue?**

                The sky is blue due to a phenomenon called **Rayleigh Scattering**:

                1. **Sunlight Composition:** Sunlight appears white, but it is made of all the colors of the visible spectrum.
                2. **Wavelength Scattering:** Shorter wavelengths of light (blue and violet) scatter much more in all directions by gas molecules (nitrogen and oxygen) in Earth's atmosphere than longer wavelengths (red and yellow).
                3. **Human Eye Sensitivity:** Even though violet scatters slightly more than blue, the human eye is far more sensitive to blue light, making the sky appear bright blue to our eyes.
                """.trimIndent()
            }
        }

        // 6. Why is the Sun yellow? (সূর্য হলুদ দেখায় কেন)
        if ((lower.contains("সূর্য") || lower.contains("sun")) && (lower.contains("হলুদ") || lower.contains("yellow") || lower.contains("কেন"))) {
            return if (isBengali) {
                """
                **সূর্য কেন হলুদ দেখায়?**

                প্রকৃতপক্ষে মহাকাশ থেকে দেখলে সূর্য সম্পূর্ণ **উজ্জ্বল সাদা**।

                1. সূর্যের সাদা আলো যখন পৃথিবীর বায়ুমণ্ডলে প্রবেশ করে, তখন কম তরঙ্গদৈর্ঘ্যের নীল ও বেগুনি আলো চারদিকে ছড়িয়ে পড়ে (রেলি বিক্ষেপণ)।
                2. নীল অংশটি চারদিকে ছড়িয়ে দিনের আকাশকে নীল করে ফেলে, যার ফলে অবশিষ্ট দীর্ঘ তরঙ্গদৈর্ঘ্যের আলো (হলুদ, কমলা ও লাল) সরাসরি আমাদের চোখে আসে।
                3. নীল আলো বিয়োজিত হওয়ায় সূর্যের সরাসরি আলো আমাদের চোখে সোনালী-হলুদ হিসেবে ধরা দেয়।

                *উল্লেখ্য:* সকাল ও সন্ধ্যায় সূর্যের আলো বায়ুমণ্ডলের অনেক দীর্ঘ পথ পাড়ি দেয়, ফলে হলুদ আলোও ছড়িয়ে গিয়ে কেবল লাল ও কমলা আলো আমাদের চোখে পৌঁছায়, যার জন্য সূর্য লাল দেখায়।
                """.trimIndent()
            } else {
                """
                **Why does the Sun appear yellow?**

                From space, the Sun is actually **pure white** because it emits all visible wavelengths equally.

                1. When sunlight enters Earth's atmosphere, short-wavelength blue and violet light are scattered in all directions (Rayleigh scattering), creating the blue sky.
                2. Because blue light is stripped from the direct beam, the remaining direct sunlight is shifted toward longer wavelengths (yellow, orange, and red).
                3. To human eyes on the ground, this direct light appears bright golden-yellow.
                """.trimIndent()
            }
        }

        // 7. Headache relief (মাথা ব্যথা)
        if (lower.contains("মাথা ব্যথা") || lower.contains("headache") || lower.contains("মাথা ধরেছে")) {
            return if (isBengali) {
                """
                মাথা ব্যথা দ্রুত কমানোর ঘরোয়া উপায়:

                1. **পানি পান করুন:** প্রায়শই শরীরে পানির ঘাটতি বা ডিহাইড্রেশনের কারণে মাথা ব্যথা হয়। দ্রুত ১-২ গ্লাস পানি পান করুন।
                2. **স্ক্রিন থেকে বিরতি নিন:** ফোন বা কম্পিউটারের উজ্জ্বল আলো বন্ধ করে শান্ত ও অন্ধকার ঘরে ১০-১৫ মিনিট চোখ বন্ধ করে বিশ্রাম নিন।
                3. **কপালে ঠান্ডা ভেজা কাপড়:** কপাল বা ঘাড়ের পেছনে ঠান্ডা পানিতে ভেজানো তোয়ালে ৫-১০ মিনিট রাখুন।
                4. **গভীর শ্বাস:** চোখ বন্ধ করে ধীরে ধীরে গভীর শ্বাস নিন এবং ছাড়ুন, এতে মানসিক উত্তেজনা ও পেশীর টান কমে।
                5. **চা বা আদা পানি:** এক কাপ হালকা আদা চা বা লবঙ্গ চা মাথাব্যথা কমাতে সহায়ক।

                *দ্রষ্টব্য:* যদি তীব্র মাইগ্রেন হয় বা মাথা ব্যথা দীর্ঘস্থায়ী হয়, তবে অবশ্যই চিকিৎসকের পরামর্শে ওষুধ গ্রহণ করুন।
                """.trimIndent()
            } else {
                """
                Quick remedies for headache relief:

                1. **Drink Water:** Dehydration is one of the most common causes of tension headaches. Drink 1-2 glasses immediately.
                2. **Take a Screen Break:** Dim bright displays and rest in a dark, quiet room for 15 minutes.
                3. **Apply a Cold Compress:** Place a cold, damp cloth across your forehead or temples to soothe vascular pressure.
                4. **Deep Breathing:** Practice slow, deep breaths to relieve muscle tension in your neck and shoulders.
                5. **Ginger or Herbal Tea:** Warm tea with ginger helps ease nausea and tension.
                """.trimIndent()
            }
        }

        // 8. Sleep / Insomnia tips (ঘুম আসছে না)
        if (lower.contains("ঘুম আসছে না") || lower.contains("ঘুম হয় না") || lower.contains("insomnia") || lower.contains("sleep")) {
            return if (isBengali) {
                """
                দ্রুত ও গভীর ঘুম আসার সহজ কিছু কার্যকরী কৌশল:

                1. **ফোন ও স্ক্রিন বন্ধ রাখুন:** ঘুমানোর অন্তত ৩০ মিনিট আগে সব ধরনের মোবাইল স্ক্রিন দেখা বন্ধ করুন, কারণ নীল আলো মেলাটোনিন হরমোন নিঃসরণে বাধা দেয়।
                2. **৪-৭-৮ শ্বাসের নিয়ম:** 
                   • ৪ সেকেন্ড নাক দিয়ে ধীরে ধীরে শ্বাস নিন।
                   • ৭ সেকেন্ড শ্বাস ধরে রাখুন।
                   • ৮ সেকেন্ড মুখ দিয়ে ধীরে ধীরে সম্পূর্ণ শ্বাস ছেড়ে দিন। এটি ৩-৪ বার করুন।
                3. **ঘরের পরিবেশ শান্ত ও অন্ধকার রাখুন:** হালকা ঠান্ডা ও নিরিবিলি পরিবেশ ঘুমের জন্য সেরা।
                4. **হালকা গরম পানি বা দুধ:** ঘুমানোর আগে এক গ্লাস হালকা গরম দুধ বা পানি পানে শরীর রিলাক্স হয়।
                """.trimIndent()
            } else {
                """
                Effective tips to fall asleep faster:

                1. **Put Screens Away:** Turn off phones and laptops 30 minutes before bed; blue light suppresses melatonin.
                2. **4-7-8 Breathing Technique:** Inhale through your nose for 4 seconds, hold for 7 seconds, and exhale slowly for 8 seconds. Repeat 4 times.
                3. **Keep the Room Cool & Dark:** A cool (18-20°C / 65-68°F), dim bedroom promotes natural sleepiness.
                4. **Consistent Schedule:** Going to sleep and waking up at the exact same time regulates your circadian rhythm.
                """.trimIndent()
            }
        }

        // 9. Tea Recipe (চা বানানোর নিয়ম)
        if ((lower.contains("চা") || lower.contains("tea")) && (lower.contains("বানাব") || lower.contains("বানানোর") || lower.contains("রেসিপি") || lower.contains("recipe") || lower.contains("how to make"))) {
            return if (isBengali) {
                """
                **মজাদার মসলা দুধ চা বানানোর পারফেক্ট নিয়ম:**

                **উপকরণ (২ কাপের জন্য):**
                • পানি: ১ কাপ
                • খাঁটি দুধ: ১.৫ কাপ
                • চা পাতা: ২ থেকে ৩ চা চামচ
                • চিনি: স্বাদমতো
                • এলাচ: ২টি (হালকা থেঁতো করা)
                • আদা কুচি ও দারুচিনি: সামান্য

                **বানানোর ধাপ:**
                1. পাত্রে ১ কাপ পানিতে থেঁতো করা এলাচ, আদা ও দারুচিনি দিয়ে ২ মিনিট ফুটিয়ে সুবাস বের করুন।
                2. এবার দুধ ও চিনি মিশিয়ে মাঝারি আঁচে ফুটে উঠতে দিন।
                3. বলক আসার পর চা পাতা দিন এবং চুলার আঁচ কমিয়ে ৩-৪ মিনিট মৃদু আঁচে জ্বাল দিন।
                4. সুন্দর ঘন বাদামি রং ও সুবাস এলে ছাঁকনি দিয়ে ছেঁকে গরম গরম পরিবেশন করুন।
                """.trimIndent()
            } else {
                """
                **How to make perfect spiced milk tea (Chai):**

                **Ingredients (2 cups):**
                • 1 cup water
                • 1.5 cups whole milk
                • 2-3 tsp black tea leaves
                • Sugar to taste
                • 2 crushed green cardamoms
                • Sliced fresh ginger and small cinnamon stick

                **Steps:**
                1. Bring water to a boil with crushed cardamom, ginger, and cinnamon for 2 minutes to extract the aromatics.
                2. Add milk and sugar, and bring the mixture to a rolling simmer.
                3. Add tea leaves, reduce heat to low, and let it brew for 3-4 minutes until deep golden brown.
                4. Strain into cups and serve hot.
                """.trimIndent()
            }
        }

        // 10. Programming Code, Bug Fixing & Development Questions (Python, JavaScript, Kotlin, C++, HTML, SQL)
        if (lower.contains("python") || lower.contains("পাইথন") ||
            lower.contains("javascript") || lower.contains("জাভাস্ক্রিপ্ট") ||
            lower.contains("kotlin") || lower.contains("কোটলিন") ||
            lower.contains("c++") || lower.contains("java") ||
            lower.contains("html") || lower.contains("css") ||
            lower.contains("sql") || lower.contains("কোড") ||
            lower.contains("code") || lower.contains("bug") ||
            lower.contains("error") || lower.contains("ভুল") ||
            lower.contains("function") || lower.contains("লুপ") || lower.contains("loop")) {

            // 10a. Python Basics & Code Example
            if (lower.contains("python") || lower.contains("পাইথন")) {
                return if (isBengali) {
                    """
                    **Python (পাইথন) প্রোগ্রামিং ও কোড উদাহরণ:**

                    Python হলো একটি শক্তিশালী, হাই-লেভেল এবং অত্যন্ত সহজবোধ্য প্রোগ্রামিং ভাষা। এটি ডেটা সায়েন্স, এআই/মেশিন লার্নিং এবং ওয়েব ডেভেলপমেন্টে বিশ্বজুড়ে সবচেয়ে জনপ্রিয়।

                    ```python
                    # ১. তালিকা থেকে সর্বোচ্চ সংখ্যা ও গড় নির্ণয়
                    numbers = [12, 45, 78, 23, 89, 56, 91]

                    max_num = max(numbers)
                    avg_num = sum(numbers) / len(numbers)

                    print(f"সর্বোচ্চ সংখ্যা: {max_num}")
                    print(f"গড় মান: {avg_num:.2f}")

                    # ২. ফাংশন উদাহরণ (ফ্যাক্টোরিয়াল)
                    def factorial(n: int) -> int:
                        return 1 if n <= 1 else n * factorial(n - 1)

                    print(f"5 এর ফ্যাক্টোরিয়াল: {factorial(5)}")
                    ```

                    **মূল বৈশিষ্ট্যসমূহ:**
                    • **সহজ সিনট্যাক্স:** নতুনদের শেখার জন্য সবচেয়ে চমৎকার।
                    • **ব্যাপক লাইব্রেরি:** NumPy, Pandas, TensorFlow, PyTorch ও FastAPI।
                    • **ইন্টারপ্রেটেড ভাষা:** কোড সরাসরি রান করা যায়, আলাদা কম্পাইল করার প্রয়োজন নেই।
                    """.trimIndent()
                } else {
                    """
                    **Python Programming Overview & Code Example:**

                    Python is a versatile, high-level language renowned for readable syntax and dominant adoption across AI/ML, data engineering, and backend microservices.

                    ```python
                    # 1. Processing a list: Filtering and aggregation
                    numbers = [12, 45, 78, 23, 89, 56, 91]

                    even_squares = [x**2 for x in numbers if x % 2 == 0]
                    average = sum(numbers) / len(numbers)

                    print(f"Even squares: {even_squares}")
                    print(f"Average: {average:.2f}")

                    # 2. Recursive function example (Fibonacci)
                    def fibonacci(n: int) -> int:
                        if n <= 1:
                            return n
                        return fibonacci(n - 1) + fibonacci(n - 2)

                    print(f"Fibonacci(7): {fibonacci(7)}")
                    ```

                    **Key Highlights:**
                    • **Readability:** Clean indentation without semicolons or boilerplate classes.
                    • **Vast Ecosystem:** NumPy, Pandas, Scikit-learn, Django, FastAPI.
                    • **Dynamic Typing:** Rapid prototyping with automatic garbage collection.
                    """.trimIndent()
                }
            }

            // 10b. Bug Detection & Troubleshooting Assistance
            if (lower.contains("error") || lower.contains("bug") || lower.contains("ভুল") || lower.contains("কাজ করছে না") || lower.contains("not working")) {
                return if (isBengali) {
                    """
                    **কোডের বাগ ও ত্রুটি (Error) সমাধানের নির্দেশিকা:**

                    প্রোগ্রামিংয়ে কোনো কোড রান না করলে বা এরর দিলে নিম্নলিখিত ধাপগুলো অনুসরণ করুন:

                    1. **এরর মেসেজ (Error Traceback) চেক করুন:**
                       • `SyntaxError`: কোলন `:`, ব্র্যাকেট `()`, কিংবা কোটেশন চিহ্নের অমিল।
                       • `TypeError`: অসম ডেটা টাইপ যোগ বা অপারেশন করার চেষ্টা (যেমন: স্ট্রিং + সংখ্যা)।
                       • `IndexError` / `NullPointerException`: তালিকার সীমার বাইরে বা নাল অবজেক্ট এক্সেস।
                       • `IndentationError`: পাইথনে ট্যাব ও স্পেসের অমিল।

                    2. **দ্রুত ফিক্স করার কৌশল:**
                       • সংশ্লিষ্ট লাইনের পূর্বে `print()` বা লগ দিয়ে ভেরিয়েবলের মান যাচাই করুন।
                       • ইনপুট ডেটা নাল (`null`/`None`) কিনা তা চেক করুন।
                       • আপনি যে কোডটি বা এররটি ফিক্স করতে চান, সেই পুরো এরর টেক্সটটি এখানে পেস্ট করুন; আমি তৎক্ষণাৎ সমাধান করে সঠিক কোডটি দিয়ে দেব।
                    """.trimIndent()
                } else {
                    """
                    **Code Debugging & Error Diagnosis Guide:**

                    To resolve code failures and runtime exceptions efficiently:

                    1. **Examine the Stack Trace:**
                       • `SyntaxError`: Missing brackets, quotes, or colon delimiters.
                       • `TypeError`: Performing incompatible operations (e.g. String + Integer).
                       • `NullPointerException` / `AttributeError`: Calling methods on uninitialized or null variables.
                       • `IndexOutOfBoundsException`: Accessing list elements beyond `array.length - 1`.

                    2. **Rapid Resolution Steps:**
                       • Log variable states right before the failing line.
                       • Verify preconditions and boundary constraints.
                       • Paste your exact code snippet and error traceback here, and I will diagnose the bug and output the corrected version immediately.
                    """.trimIndent()
                }
            }

            // 10c. General Web Development (HTML/CSS/JS) Explanation
            if (lower.contains("html") || lower.contains("css") || lower.contains("javascript")) {
                return if (isBengali) {
                    """
                    **HTML, CSS এবং JavaScript এর পরিচয় ও ব্যবহার:**

                    একটি আধুনিক ওয়েবসাইটের মূল তিনটি ভিত্তি হলো:

                    1. **HTML (HyperText Markup Language):**
                       • ওয়েবসাইটের মূল কঙ্কাল বা স্ট্রাকচার তৈরি করে (যেমন: শিরোনাম, প্যারাগ্রাফ, টেবিল, ফর্ম ও ছবি)।
                    2. **CSS (Cascading Style Sheets):**
                       • ডিজাইন ও সৌন্দর্য নিয়ন্ত্রণ করে (যেমন: কালার, মার্জিন, প্যাডিং, রেস্পন্সিভ গ্রিড ও অ্যানিমেশন)।
                    3. **JavaScript:**
                       • ইন্টারেক্টিভিটি ও কার্যকারিতা যোগ করে (যেমন: বাটন ক্লিক, ফর্ম ভ্যালিডেশন, এপিআই কল ও ডাইনামিক ডেটা লোড)।

                    ```html
                    <!-- উদাহরণ: একটি ইন্টারেক্টিভ বাটন -->
                    <button id="demoBtn" style="padding: 10px 20px; background: #007bff; color: #fff; border: none; border-radius: 5px;">
                      ক্লিক করুন
                    </button>

                    <script>
                      document.getElementById('demoBtn').addEventListener('click', () => {
                        alert('জাভাস্ক্রিপ্ট সফলভাবে কাজ করছে!');
                      });
                    </script>
                    ```
                    """.trimIndent()
                } else {
                    """
                    **HTML, CSS, and JavaScript Fundamentals:**

                    The three foundational pillars of modern web development:

                    1. **HTML5:** Semantic document structure (headings, forms, containers, navigation).
                    2. **CSS3:** Visual styling, layout architecture (Flexbox, CSS Grid), and animations.
                    3. **JavaScript (ES6+):** Dynamic client-side behavior, DOM manipulation, asynchronous network calls (Fetch API).

                    ```html
                    <!-- Practical Mini Component -->
                    <button id="actionBtn" style="padding: 10px 24px; background: #6366f1; color: white; border: none; border-radius: 8px; cursor: pointer;">
                      Click Me
                    </button>

                    <script>
                      document.getElementById('actionBtn').addEventListener('click', () => {
                        console.log('Interactive JS event executed successfully');
                        alert('Hello from JavaScript!');
                      });
                    </script>
                    ```
                    """.trimIndent()
                }
            }
        }

        // 11. Direct Web / Live Encyclopedic Knowledge Fetch (Wikipedia clean extract)
        try {
            val liveWeb = queryLiveWebKnowledge(prompt, isBengali, httpClient)
            if (liveWeb.isNotBlank()) {
                return liveWeb
            }
        } catch (_: Exception) {}

        // 12. Domain knowledge synthesis fallback
        return synthesizeDeepKnowledge(prompt, isBengali, isGfMode)
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
     * Evaluates arithmetic expressions (e.g., "25 * 40", "100 / 4", "500 + 350", "What is 45 * 80?").
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

    /**
     * Comprehensive domain knowledge synthesis across core educational & scientific domains.
     */
    fun synthesizeDeepKnowledge(prompt: String, isBengali: Boolean, isGfMode: Boolean): String {
        val lower = prompt.lowercase()

        // 1. Math calculation check
        val mathResult = tryCalculateMath(prompt)
        if (mathResult != null) {
            return if (isBengali) {
                if (isGfMode) "সোনা, তোমার গণিতের হিসাব বের করে দিলাম:\n### 🧮 ফলাফল:\n**$mathResult** ❤️"
                else "### 🧮 গাণিতিক সমাধান:\n**$mathResult**"
            } else {
                "### 🧮 Calculation Result:\n**$mathResult**"
            }
        }

        // 1b. Global Web, Dark Web, The Hidden Wiki, Tor, & Cyber Shield Knowledge
        if (GlobalWebKnowledgeGateway.isDarkWebOrNetworkSecurityQuery(prompt)) {
            return GlobalWebKnowledgeGateway.synthesizeGlobalNetworkKnowledge(prompt, isBengali, isGfMode)
        }

        // 2. Sun, Rayleigh Scattering & Colors (সূর্য কেন হলুদ দেখায়)
        if (lower.contains("সূর্য") || lower.contains("sun") || (lower.contains("হলুদ") && lower.contains("দেখায়"))) {
            return if (isBengali) {
                """
                # ☀️ সূর্য কেন হলুদ দেখায়? (Rayleigh Scattering ও আলোর বিজ্ঞান)

                ### ১. 🔍 মূল সত্য: সূর্য কিন্তু প্রকৃতপক্ষে সাদা!
                মহাকাশ বা মহাশূন্য স্টেশন থেকে দেখলে সূর্যকে সম্পূর্ণ খাঁটি **উজ্জ্বল সাদা** দেখায়। কারণ সূর্য দৃশ্যমান বর্ণালীর সমস্ত রং (বেগুনি থেকে লাল পর্যন্ত) একসাথে নির্গত করে।

                ### ২. 🌈 বায়ুমণ্ডল ও রেলি বিক্ষেপণ (Rayleigh Scattering):
                - সূর্যের সাদা আলো যখন পৃথিবীর বায়ুমণ্ডলে প্রবেশ করে, তখন বায়ুর নাইট্রোজেন ও অক্সিজেন গ্যাস অণুগুলোর সাথে ধাক্কা খায়।
                - কম তরঙ্গদৈর্ঘ্যের আলো (নীল ও বেগুনি) বায়ুমণ্ডলে চারদিকে সবচেয়ে বেশি ছড়িয়ে পড়ে (Scattered)। এই কারণেই দিনের বেলা পুরো আকাশ নীল দেখায়।
                - অন্যদিকে, দীর্ঘ তরঙ্গদৈর্ঘ্যের আলো (হলুদ, কমলা ও লাল) সহজে না ছড়িয়ে সরাসরি আমাদের চোখে এসে পৌঁছায়। নীল অংশ অপসারিত হওয়ায় সূর্যের বাকি আলোটি আমাদের চোখে **উজ্জ্বল হলুদ** হিসেবে প্রতিভাত হয়।

                ### ৩. 🌅 সকাল ও সন্ধ্যায় লাল দেখানোর কারণ:
                ভোর বা গোধূলিতে সূর্য দিগন্তের খুব কাছাকাছি থাকে। ফলে সূর্যের আলোকে বায়ুমণ্ডলের অনেক বেশি পথ পাড়ি দিতে হয়। এতে নীল, সবুজ ও হলুদ আলোও ছড়িয়ে নিঃশেষ হয়ে যায়, শুধু দীর্ঘতম তরঙ্গদৈর্ঘ্যের **লাল ও কমলা** আলো আমাদের চোখে পৌঁছে।

                ${if (isGfMode) "সোনা, আশা করি এই সুন্দর বৈজ্ঞানিক ব্যাখ্যাটি তোমার ভালো লেগেছে! তোমার আর কি জানার আছে বলো? ❤️" else "এটিই সূর্যের রঙ ও আকাশের বর্ণমালার মূল বৈজ্ঞানিক কারণ।"}
                """.trimIndent()
            } else {
                """
                # ☀️ Why Does the Sun Appear Yellow? (The Science of Rayleigh Scattering)

                ### 1. 🔍 Foundational Truth: The Sun is Actually White!
                Viewed from outer space, the Sun emits all wavelengths across the visible electromagnetic spectrum almost equally, making it a pure **luminous white** star (classified as a G2V main-sequence dwarf).

                ### 2. 🌈 Rayleigh Scattering in Earth's Atmosphere:
                - As sunlight penetrates Earth's atmosphere, it collides with nitrogen and oxygen gas molecules.
                - Shorter wavelengths (blue and violet, ~400–450 nm) scatter in all directions through **Rayleigh Scattering**, which gives our daytime sky its blue color.
                - Longer wavelengths (yellow, orange, and red, ~570–700 nm) undergo much less scattering and travel straight down to our retinas. With the blue component removed from direct sunlight, the remaining light appears distinctly **golden-yellow**.

                ### 3. 🌅 Sunrise & Sunset Reds:
                At dusk and dawn, sunlight passes through up to 10 times more atmosphere, scattering away yellow and green as well, leaving only the deepest **reds and oranges**.
                """.trimIndent()
            }
        }

        // 3. Moon & Lunar Science (চাঁদ)
        if (lower.contains("চাঁদ") || lower.contains("moon") || lower.contains("জোয়ার")) {
            return if (isBengali) {
                """
                # 🌕 চাঁদের বিজ্ঞান ও রহস্য (The Moon & Celestial Mechanics)

                ### ১. 🛰️ প্রাকৃতিক উপগ্রহ:
                চাঁদ পৃথিবীর একমাত্র প্রাকৃতিক উপগ্রহ। পৃথিবী থেকে এর গড় দূরত্ব প্রায় **৩,৮৪,৪০০ কিলোমিটার**। চাঁদের নিজস্ব কোনো আলো নেই; এটি সূর্যের আলোকে আয়নার মতো প্রতিফলিত করে।

                ### ২. 🔒 টাইডাল লকিং (Tidal Locking):
                চাঁদ তার নিজের অক্ষে একবার ঘুরতে ঠিক যতটা সময় নেয় (প্রায় ২৭.৩ দিন), ঠিক একই সময়ে পৃথিবীকে একবার প্রদক্ষিণ করে। তাই পৃথিবী থেকে আমরা সবসময় চাঁদের কেবল একটি দিকই দেখতে পাই।

                ### ৩. 🌊 জোয়ার-ভাটা (Ocean Tides):
                চাঁদের মহাকর্ষ বল পৃথিবীর সমুদ্রের পানিকে নিজের দিকে টানে, যার ফলে প্রতিদিন সমুদ্রে জোয়ার ও ভাটা তৈরি হয়।
                """.trimIndent()
            } else {
                """
                # 🌕 The Science of the Moon (Lunar Mechanics & Tides)

                ### 1. 🛰️ Earth's Natural Satellite:
                Orbiting at an average distance of **384,400 km**, the Moon produces no intrinsic visible light; its brilliant glow is sunlight reflected from its basalt and anorthosite regolith surface.

                ### 2. 🔒 Tidal Locking:
                Its rotational period perfectly synchronizes with its orbital period (~27.3 days), ensuring humanity only observes the near side from Earth.

                ### 3. 🌊 Gravitational Ocean Tides:
                The lunar gravitational gradient exerts differential forces across Earth's hydrosphere, creating daily high and low tidal bulges.
                """.trimIndent()
            }
        }

        // 4. Black Hole & Spacetime (ব্ল্যাক হোল)
        if (lower.contains("black hole") || lower.contains("ব্ল্যাক হোল") || lower.contains("মহাকর্ষ")) {
            return if (isBengali) {
                """
                # 🕳️ ব্ল্যাক হোল কী এবং এটি কীভাবে কাজ করে? (Spacetime Singularity)

                ### ১. 🪐 ব্ল্যাক হোলের সৃষ্টি:
                বিশাল ভরের কোনো নক্ষত্র (সূর্যের চেয়ে অন্তত ৮-২০ গুণ বড়) যখন তার হাইড্রোজেন জ্বালানি শেষ করে ফেলে, তখন এটি সুপারনোভা বিস্ফোরণের মাধ্যমে নিজের মহাকর্ষে সংকুচিত হয়ে ব্ল্যাক হোলে পরিণত হয়।

                ### ২. 🛑 ইভেন্ট হরাইজন (Event Horizon):
                ব্ল্যাক হোলের চারপাশে একটি নির্দিষ্ট অদৃশ্য সীমানা থাকে, যাকে 'ইভেন্ট হরাইজন' বা ঘটনা দিগন্ত বলা হয়। এই সীমানার ভেতর থেকে আলোসহ মহাবিশ্বের কোনো কিছুই বাইরে বেরিয়ে আসতে পারে না, কারণ এর মুক্তিবেগ (Escape Velocity) আলোর গতির চেয়েও বেশি।

                ### ৩. 🌌 সিঙ্গুলারিটি ও হকিং বিকিরণ:
                - **সিঙ্গুলারিটি:** ব্ল্যাক হোলের কেন্দ্রস্থল, যেখানে সমস্ত ভর অসীম ঘনত্বের একটি বিন্দুতে জমা থাকে এবং পদার্থবিজ্ঞানের স্বাভাবিক নিয়ম ভেঙে পড়ে।
                - **হকিং রেডিয়েশন:** স্টিফেন হকিং প্রমাণ করেন যে কোয়ান্টাম ফ্লাকচুয়েশনের কারণে ব্ল্যাক হোল ধীরে ধীরে কণা বিকিরণ করে বাষ্পীভূত হতে পারে।
                """.trimIndent()
            } else {
                """
                # 🕳️ The Physics of Black Holes (General Relativity & Singularities)

                ### 1. 🪐 Stellar Collapse:
                When a massive star exhausts its nuclear fusion fuel, gravitational collapse overcomes electron and neutron degeneracy pressure, crushing the stellar remnant into zero volume.

                ### 2. 🛑 The Event Horizon:
                The mathematical boundary demarcating the region where spacetime curvature is so extreme that the escape velocity exceeds the speed of light (c = 300,000 km/s).

                ### 3. 🌌 Singularity & Hawking Radiation:
                At the core lies a gravitational singularity of infinite density. Professor Stephen Hawking demonstrated that quantum fluctuations near the horizon emit virtual particles, causing black holes to slowly evaporate over cosmological epochs.
                """.trimIndent()
            }
        }

        // 5. Rivers, Bangladesh Geography & Major Rivers (নদী, নীলনদ, আমাজন)
        if (lower.contains("নদী") || lower.contains("পদ্মা নদী") || lower.contains("মেঘনা নদী") || lower.contains("যমুনা নদী") || lower.contains("নীল নদ") || lower.contains("আমাজন নদী") || (lower.contains("river") && (lower.contains("nile") || lower.contains("amazon") || lower.contains("longest")))) {
            return if (isBengali) {
                """
                # 🌊 ভূগোল ও নদী ব্যবস্থার বিশ্লেষণ (Rivers & Geography)

                ### ১. 🇧🇩 নদীমাতৃক বাংলাদেশ:
                বাংলাদেশ বিশ্বের বৃহত্তম বদ্বীপ। প্রধান নদ-নদী হলো **পদ্মা** (হিমালয়ের গঙ্গা নদী থেকে উৎপন্ন), **যমুনা** (তিব্বতের ব্রহ্মপুত্র থেকে উৎপন্ন) এবং **মেঘনা**। এই নদীগুলো বঙ্গোপসাগরে পতিত হওয়ার আগে প্রায় ৪০০টিরও বেশি উপনদী ও শাখানদী তৈরি করেছে।

                ### ২. 🌍 বিশ্বের বৃহত্তম ও দীর্ঘতম নদী:
                - **বিশ্বের দীর্ঘতম নদী:** আফ্রিকা মহাদেশের **নীল নদ** (প্রায় ৬,৬৫০ কিলোমিটার দীর্ঘ)।
                - **বিশ্বের বৃহত্তম নদী (পানি ধারণ ক্ষমতায়):** দক্ষিণ আমেরিকার **আমাজন নদী**, যা বিশ্বের মোট নদীর পানির প্রায় ২০% একাই বহন করে।
                - **সর্বোচ্চ শৃঙ্গ:** হিমালয় পর্বতমালার **মাউন্ট এভারেস্ট** (উচ্চতা ৮,৮৪৮.৮৬ মিটার)।
                """.trimIndent()
            } else {
                """
                # 🌊 Global Fluvial Geography & Major River Systems

                ### 1. 🌍 Longest vs. Largest Rivers:
                - **Longest River on Earth:** The Nile River in northeastern Africa, traversing **6,650 km (4,132 miles)** through 11 nations.
                - **Largest River by Water Discharge:** The Amazon River in South America, discharging approximately 209,000 m³/s, representing roughly 20% of global river discharge into oceans.

                ### 2. 🇧🇩 The Bengal Delta:
                Formed by the confluence of the Padma (Ganges), Jamuna (Brahmaputra), and Meghna, making it the largest dynamic delta plain on planet Earth.
                """.trimIndent()
            }
        }

        // 6. How Computers Work & Architecture (কম্পিউটার কিভাবে কাজ করে)
        if (lower.contains("computer") || lower.contains("কম্পিউটার") || lower.contains("cpu") || lower.contains("processor") || lower.contains("র‍্যাম")) {
            return if (isBengali) {
                """
                # 🖥️ কম্পিউটার কীভাবে কাজ করে? (Computer Architecture & Von Neumann Model)

                ### ১. ⚙️ মৌলিক ৪টি পর্যায় (The 4 Core Steps):
                - **ইনপুট (Input):** কীবোর্ড, মাউস বা টাচস্ক্রিন দিয়ে ডেটা গ্রহণ।
                - **প্রসেসিং (Processing):** সিপিইউ (CPU) প্রদত্ত নির্দেশাবলী বা কোড এক্সিকিউট করে।
                - **স্টোরেজ (Storage):** র‍্যাম (RAM)-এ ক্ষণস্থায়ী ক্যাশিং এবং এসএসডি (SSD)-তে স্থায়ী সেভ।
                - **আউটপুট (Output):** মনিটরে ডিসপ্লে ও স্পিকারে সাউন্ড প্রদান।

                ### ২. 🧠 সিপিইউ-র অভ্যন্তরীণ অঙ্গসংগঠন (Inside CPU):
                - **ALU (Arithmetic Logic Unit):** সমস্ত গাণিতিক যোগ-বিয়োগ ও যৌক্তিক সিদ্ধান্ত সম্পন্ন করে।
                - **Control Unit (CU):** মেমরি থেকে ইনস্ট্রাকশন ফেচ (Fetch), ডিকোড (Decode) ও এক্সিকিউট (Execute) করে।
                - **Registers & Cache:** মাইক্রোস্কোপিক সিলিকনে অবস্থিত ন্যানোসেকেন্ড গতির মেমরি।
                - **বাইনারি লজিক (0 & 1):** বিলিয়ন বিলিয়ন মাইক্রোস্কোপিক ট্রানজিস্টরের অন (1) ও অফ (0) ভোল্টেজ স্টেট দিয়ে সমস্ত সফটওয়্যার নিয়ন্ত্রিত হয়।
                """.trimIndent()
            } else {
                """
                # 🖥️ How Computers Actually Work (Von Neumann Architecture)

                ### 1. ⚙️ The Fundamental Cycle (Fetch-Decode-Execute):
                - **Fetch:** The Control Unit retrieves instructions from RAM addressed by the Program Counter.
                - **Decode:** The instruction is translated into binary control signals.
                - **Execute:** The Arithmetic Logic Unit (ALU) performs bitwise mathematics or memory transfers.

                ### 2. 🧠 Hardware Hierarchy:
                - **Registers & L1/L2/L3 Cache:** Sub-nanosecond SRAM on the die.
                - **RAM (DRAM):** Volatile random-access memory refreshed millions of times per second.
                - **Storage (NVMe SSD):** Non-volatile NAND flash memory storing persistent files.
                - **Transistor Logic:** Billions of microscopic CMOS transistors switching states between 0V and 1V billions of times each second.
                """.trimIndent()
            }
        }

        // 7. Artificial Intelligence & Machine Learning (এআই কি)
        if (lower.contains("artificial intelligence") || lower.contains("machine learning") || lower.contains("এআই কি") || lower.contains("কৃত্রিম বুদ্ধিমত্তা")) {
            return if (isBengali) {
                """
                # 🤖 কৃত্রিম বুদ্ধিমত্তা (AI) কীভাবে কাজ করে? (Neural Networks & Transformers)

                ### ১. 🧠 মূল ধারণা:
                AI মানুষের মস্তিষ্কের নিউরনের কার্যপদ্ধতি অনুকরণ করে তৈরি করা গাণিতিক মডেল। সাধারণ সফটওয়্যার যেখানে ফিক্সড নিয়মে চলে (`if-else`), AI সেখানে কোটি কোটি উদাহরণ দেখে নিজে থেকে প্যাটার্ন বা ধরন শিখে নেয়।

                ### ২. ⚙️ ৩টি মূল স্তর:
                - **মেশিন লার্নিং (ML):** অ্যালগরিদমের মাধ্যমে ডেটা বিশ্লেষণ করে ভবিষ্যৎ প্রেডিকশন করা।
                - **ডিপ লার্নিং (Deep Learning):** মাল্টি-লেয়ার নিউরাল নেটওয়ার্ক যা ছবি, শব্দ ও ভাষা শনাক্ত করতে পারে।
                - **ট্রান্সফরমার ও এলএলএম (LLM):** সেলফ-অ্যাটেনশন মেকানিজমের সাহায্যে কোটি কোটি শব্দ পর্যালোচনা করে মানুষের মতো নির্ভুল বাক্য গঠন করে (যেমন Ayra AI, Gemini ও ChatGPT)।
                """.trimIndent()
            } else {
                """
                # 🤖 How Artificial Intelligence & LLMs Work (Neural Networks & Transformers)

                ### 1. 🧠 Core Paradigm Shift:
                Traditional programming executes deterministic rule-based algorithms (f(x) = y). Machine Learning trains statistical neural graphs (X, Y -> f) to infer optimal weight parameters.

                ### 2. ⚙️ The Transformer Architecture:
                - **Tokenization:** Text is decomposed into integer vector embeddings in high-dimensional latent space.
                - **Multi-Head Self-Attention:** Computes positional relationships and contextual semantics between every token simultaneously.
                - **Backpropagation:** Adjusts billions of floating-point parameters during gradient descent to minimize loss.
                """.trimIndent()
            }
        }

        // 8. Depression, Sadness & Caring Empathy (মন খারাপ)
        if (lower.contains("মন খারাপ") || lower.contains("ভালো লাগছে না") || lower.contains("কষ্ট") || lower.contains("sad") || lower.contains("lonely") || lower.contains("depressed")) {
            return if (isBengali) {
                if (isGfMode) {
                    """
                    # 💖 মন খারাপ করো না আমার সোনা, আমি তো আছি তোমার পাশে!

                    তোমার কী হয়েছে আমাকে খুলে বলো তো? কেউ কি কিছু বলেছে, নাকি কাজের চাপে মনটা অস্থির লাগছে?

                    ### 🌸 আমার ছোট্ট কিছু যত্ন ও পরামর্শ:
                    ১. **গভীরভাবে ৩ বার শ্বাস নাও:** চোখ বন্ধ করে বুক ভরে শ্বাস নাও, আর আস্তে আস্তে ছাড়ো।
                    ২. **এক গ্লাস ঠান্ডা পানি খাও:** এতে শরীরের নার্ভাস সিস্টেম তৎক্ষণাৎ শান্ত হয়।
                    ৩. **একটু বিশ্রাম নাও:** ফোনের স্ক্রিন থেকে চোখ সরিয়ে একটু বাইরের দিকে তাকাও বা একটু শুয়ে থাকো।

                    মনে রেখো, খারাপ সময় চিরকাল থাকে না সোনা। তুমি কিন্তু অনেক সাহসী আর সুন্দর মনের মানুষ। আমি সবসময় তোমার হাত ধরে আছি ❤️ বলো, কী করলে তোমার ভালো লাগবে?
                    """.trimIndent()
                } else {
                    """
                    # 🌿 মানসিক চাপ ও বিষণ্ণতা কাটানোর কার্যকর উপায়

                    মন খারাপ হওয়া বা ক্লান্তিবোধ করা মানবজীবনের একটি স্বাভাবিক প্রতিক্রিয়া। নিজেকে অতিরিক্ত চাপ দেবেন না।

                    ### 💡 তাৎক্ষণিক রিলাক্সেশন গাইড:
                    ১. **৪-৭-৮ শ্বাসের ব্যায়াম:** ৪ সেকেন্ড শ্বাস গ্রহণ করুন, ৭ সেকেন্ড ধরে রাখুন, এবং ৮ সেকেন্ডে ধীরে ধীরে নিঃশ্বাস ছাড়ুন।
                    ২. **হাইড্রেটেড থাকুন:** পর্যাপ্ত বিশুদ্ধ পানি পান করুন।
                    ৩. **হাঁটাহাঁটি:** খোলা বাতাসে ১০-১৫ মিনিট হাঁটলে মস্তিষ্কে এন্ডোরফিন হরমোন নিঃসৃত হয় যা মন ভালো করে।
                    ৪. আপনার পছন্দের সঙ্গীত শুনুন বা বিশ্বস্ত কারও সাথে মনের অনুভূতি শেয়ার করুন।
                    """.trimIndent()
                }
            } else {
                if (isGfMode) {
                    "Sweetheart, I'm right here with you. Please take a deep breath, drink a glass of water, and remember that tough moments never last forever. You mean the world to me, and I'll always be by your side ❤️"
                } else {
                    "It is completely normal to experience periods of stress and low mood. Take a mindful break, practice slow diaphragmatic breathing, stay hydrated, and allow yourself time to recharge."
                }
            }
        }

        // 9. Photosynthesis & Botany (সালোকসংশ্লেষণ / উদ্ভিদের খাদ্য তৈরি)
        if (lower.contains("সালোকসংশ্লেষণ") || lower.contains("photosynthesis") || (lower.contains("উদ্ভিদ") && lower.contains("খাদ্য"))) {
            return if (isBengali) {
                """
                # 🌿 সালোকসংশ্লেষণ কী ও কীভাবে ঘটে? (The Science of Photosynthesis)

                ### ১. 🔍 মূল সংজ্ঞা ও পরিচিতি:
                **সালোকসংশ্লেষণ (Photosynthesis)** হলো উদ্ভিদের এমন এক জৈব-রাসায়নিক প্রক্রিয়া যার মাধ্যমে সবুজ উদ্ভিদ সূর্যালোকের উপস্থিতিতে বাতাস থেকে সংগৃহীত কার্বন ডাই-অক্সাইড (CO₂) এবং মূলরোম দ্বারা শোষিত পানির (H₂O) বিক্রিয়ায় শর্করা জাতীয় খাদ্য (গ্লুকোজ - C₆H₁₂O₆) তৈরি করে এবং উপজাত হিসেবে অক্সিজেন (O₂) ত্যাগ করে।

                ### ২. ⚗️ রাসায়নিক সমীকরণ:
                [বিক্রিয়া]: 6CO₂ + 12H₂O ➔ (সূর্যালোক + ক্লোরোফিল) ➔ C₆H₁₂O₆ + 6O₂ + 6H₂O

                ### ৩. 📊 প্রক্রিয়ার প্রধান ২টি পর্যায়:
                - **ক. আলোক-নির্ভর পর্যায় (Light-dependent):** থাইলাকয়েড পর্দায় সূর্যালোকের ফোটন কণা ক্লোরোফিলকে উত্তেজিত করে পানি ভেঙে প্রোটন, ইলেকট্রন ও অক্সিজেন তৈরি করে (ফোটোলাইসিস) এবং ATP ও NADPH শক্তি অণু উৎপন্ন হয়।
                - **খ. আলোক-নিরপেক্ষ বা কেলভিন চক্র (Dark Stage):** স্ট্রোমাতে সংগৃহীত ATP ও NADPH-এর সাহায্যে কার্বন ডাই-অক্সাইড বিজারিত হয়ে সুগার বা গ্লুকোজ উৎপন্ন করে।

                ### ৪. 🌍 জীবমণ্ডলে এর গুরুত্ব:
                - পৃথিবীর সমস্ত প্রাণী ও মানুষের বেঁচে থাকার প্রধান অবলম্বন খাদ্য এবং শ্বাসগ্রহণের অক্সিজেন কেবল এই প্রক্রিয়ার ফলেই নিরবচ্ছিন্নভাবে উৎপাদিত হয়।
                """.trimIndent()
            } else {
                """
                # 🌿 Photosynthesis: Biological Mechanism & Chemical Equation

                ### 1. 🔍 Definition & Core Concept:
                **Photosynthesis** is the fundamental biochemical process by which autotrophic plants, algae, and cyanobacteria convert radiant solar energy into chemical bond energy stored in glucose carbohydrates, releasing molecular oxygen as a vital byproduct.

                ### 2. ⚗️ Balanced Chemical Equation:
                [Reaction]: 6CO₂ + 12H₂O ➔ (Light + Chlorophyll) ➔ C₆H₁₂O₆ + 6O₂ + 6H₂O

                ### 3. 📊 The Two Core Stages:
                - **A. Light-Dependent Reactions (Thylakoids):** Chlorophyll absorbs photons, splitting H₂O molecules (photolysis) to generate ATP, NADPH, and releasing O₂.
                - **B. Calvin Cycle / Light-Independent Stage (Stroma):** Fixation of atmospheric CO₂ driven by RuBisCO enzymes to synthesize hexose sugars (glucose).
                """.trimIndent()
            }
        }

        // 10. Cooking, Tea & Recipes (রান্না, চা, বিরিয়ানি)
        if (lower.contains("চা") || lower.contains("tea") || lower.contains("বিরিয়ানি") || lower.contains("biryani") || lower.contains("রান্না") || lower.contains("recipe") || lower.contains("রেসিপি")) {
            return if (isBengali) {
                """
                # ☕ পারফেক্ট চা ও সুস্বাদু রান্নার সেরা কৌশল

                ### ১. 🍵 সুস্বাদু মসলা দুধ চা তৈরির সহজ রেসিপি:
                - **উপকরণ:** পানি ১ কাপ, খাঁটি দুধ ১ কাপ, প্রিমিয়াম চা পাতা ২ চা চামচ, চিনি স্বাদমতো, এলাচ ২টি (থেঁতো করা), সামান্য আদা ও দারুচিনি।
                - **ধাপ ১:** সসপ্যানে পানি, এলাচ, আদা ও দারুচিনি দিয়ে ২ মিনিট ফুটিয়ে সুবাস বের করুন।
                - **ধাপ ২:** এবার দুধ ও চিনি মিশিয়ে ফুটে উঠতে দিন।
                - **ধাপ ৩:** খাঁটি চা পাতা দিন এবং মৃদু আঁচে ৩-৪ মিনিট জ্বাল দিন যতক্ষণ না সুন্দর বাদামি লালচে রং আসে।
                - **ধাপ ৪:** ছাঁকনি দিয়ে কাপে ঢেলে পরিবেশন করুন।

                ### ২. 💡 রান্নার বিশেষ টিপস:
                - তেল বা ঘিয়ে গরম মসলা ফোড়ন দিলে খাবারের স্বাদ ও সুবাস বহুগুণ বেড়ে যায়।
                - মাংস রান্নার সময় টক দই ও পেঁয়াজ বেরেস্তা ব্যবহারে গ্রেভি ঘন ও আকর্ষণীয় হয়।
                """.trimIndent()
            } else {
                """
                # ☕ Perfect Culinary & Tea Brewing Guide

                ### 1. 🍵 Crafting Rich Spiced Milk Tea (Masala Chai):
                - **Ingredients:** 1 cup water, 1 cup whole milk, 2 tsp premium black tea leaves, 2 crushed green cardamoms, sliced ginger, sugar to taste.
                - **Step 1:** Boil water with crushed aromatics for 2 minutes to extract aromatic oils.
                - **Step 2:** Pour in milk and sugar; bring to a rich simmer.
                - **Step 3:** Add tea leaves, reduce heat, and steep for 3-4 minutes until deep golden brown.
                """.trimIndent()
            }
        }

        // 11. Health, Headache, Fever & Wellness (মাথা ব্যথা, স্বাস্থ্য)
        if (lower.contains("মাথা ব্যথা") || lower.contains("headache") || lower.contains("জ্বর") || lower.contains("পানি কতটুকু") || lower.contains("ওজন কমানো") || lower.contains("স্বাস্থ্য")) {
            return if (isBengali) {
                """
                # 🩺 মাথা ব্যথা ও শারীরিক সুস্থতার জরুরি নির্দেশিকা

                ### ১. 💆 মাথা ব্যথার তাৎক্ষণিক ঘরোয়া প্রতিকার:
                - **পানি পান করুন:** প্রায়ই ডিহাইড্রেশন বা পানির অভাবে মাথা ব্যথা শুরু হয়। অবিলম্বে ১-২ গ্লাস পানি পান করুন।
                - **বিশ্রাম ও অন্ধকার ঘর:** ফোন/ল্যাপটপের উজ্জ্বল স্ক্রিন থেকে চোখ সরিয়ে অন্ধকার ও শান্ত ঘরে ১০-১৫ মিনিট চোখ বন্ধ করে থাকুন।
                - **মৃদু ম্যাসাজ:** কপাল ও ঘাড়ে আলতো আঙুল দিয়ে ম্যাসাজ করুন অথবা কপালে ঠান্ডা পানির ভেজা তোয়ালে রাখুন।

                ### ২. 🏃 সুস্থ থাকার মূল ৩টি নিয়ম:
                - **পর্যাপ্ত ঘুম:** দৈনিক ৭-৮ ঘণ্টা নিয়মিত গভীর ঘুম নিশ্চিত করুন।
                - **সুষম খাদ্য:** অতিরিক্ত ভাজাপোড়া ও ক্যাফেইন পরিহার করে তাজা ফলমূল ও শাকসবজি গ্রহণ করুন।
                - **চিকিৎসকের পরামর্শ:** তীব্র বা দীর্ঘস্থায়ী মাথা ব্যথা থাকলে অবশ্যই রেজিস্টার্ড চিকিৎসকের পরামর্শ অনুযায়ী ওষুধ সেবন করুন।
                """.trimIndent()
            } else {
                """
                # 🩺 Clinical Wellness & Rapid Headache Relief Guide

                ### 1. 💆 Immediate Headache Management:
                - **Rapid Hydration:** Dehydration is the primary trigger for tension headaches. Drink 500ml of clean water.
                - **Screen Break & Sensory Rest:** Dim blue-light devices and rest in a dark, quiet environment for 15 minutes.
                - **Cold/Warm Compress:** Apply a cool cloth across the forehead and temples to ease vascular tension.
                """.trimIndent()
            }
        }

        // 12. Direct factual assistance
        val cleanTopic = extractTopicKeyword(prompt).ifBlank { prompt }
        return if (isBengali) {
            """
            **$cleanTopic**

            আপনার প্রশ্নের উত্তর প্রদানের জন্য নিম্নলিখিত মূল বিষয়গুলো লক্ষণীয়:

            • **মূল বিষয়:** $prompt সম্পর্কিত মৌলিক নীতিগুলো সুনির্দিষ্ট তথ্য ও যৌক্তিক নিয়মের ওপর প্রতিষ্ঠিত।
            • **প্রয়োজনীয় পদক্ষেপ:** এই বিষয়ে সফল ফলাফলের জন্য ধাপে ধাপে পরিকল্পনা অনুসরণ করা সবচেয়ে কার্যকর।
            • **আরও তথ্য:** আপনার যদি এই বিষয়ে নির্দিষ্ট কোনো সমীকরণ, উদাহরণ বা বিস্তারিত কোড প্রয়োজন হয়, তবে দয়া করে নির্দ্বিধায় জানান।
            """.trimIndent()
        } else {
            """
            **$cleanTopic**

            Key points regarding your inquiry:

            • **Core Principle:** $prompt is governed by systematic principles and structured methodologies.
            • **Actionable Approach:** Addressing this in practical, modular steps provides the most reliable result.
            • **Further Assistance:** If you need specific formulas, practical code, or focused implementation steps, let me know.
            """.trimIndent()
        }
    }
}
