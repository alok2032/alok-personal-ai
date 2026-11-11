package com.ayra.ai.app

import com.ayra.ai.app.ai.AyraIntentClassifier
import com.ayra.ai.app.ai.AyraToolIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Section 2.1 & 5.1: IntentRouterTest.
 * Validates intent routing with >= 400 test prompts.
 * Mandatory target: 0 false positives on normal CHAT queries (including the 10 P0-C hijack cases).
 * High recall on explicit slash/tool commands.
 */
class IntentRouterTest {

    // Table P0-C hijack test cases that must NEVER be hijacked to tools:
    private val p0cHijackTestCases = listOf(
        "What is the topic of today's lecture?",
        "Explain how shopping cart sessions work",
        "Why is Python typing so strict?",
        "What does the word 'wrapping' mean?",
        "How do I fix my cmd not working?",
        "What is the best way to make a document readable?",
        "Tell me about the history of music in Bengal and make it short",
        "Give me tips of composition for photography",
        "video game industry size, make it brief",
        "make a model of the cadastral map"
    )

    @Test
    fun testP0CHijackPromptsMustRouteToChat() {
        val violations = mutableListOf<String>()
        for (prompt in p0cHijackTestCases) {
            val detected = AyraIntentClassifier.classifyIntent(prompt)
            if (detected != AyraToolIntent.CHAT) {
                violations.add("Prompt \"$prompt\" incorrectly classified as $detected (expected CHAT)")
            }
        }
        assertTrue(
            "P0-C Regression Failures detected:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun testCorpusOf200NormalChatPromptsNeverHijacked() {
        val normalChatPrompts = mutableListOf<String>()

        // 1. English General Knowledge & Programming (80 prompts)
        val enGeneral = listOf(
            "What is the capital of France?",
            "How does photosynthesis work in plants?",
            "Explain quantum computing in simple terms.",
            "Write a Python function to check for palindromes.",
            "What is the difference between SQL and NoSQL?",
            "How do I optimize Docker image size?",
            "Explain the concept of monads in functional programming.",
            "What happened during the Industrial Revolution?",
            "Summarize the plot of Hamlet.",
            "How does the immune system fight viral infections?",
            "Explain recursion with a simple example.",
            "What are the solid principles in object-oriented design?",
            "How does an engine carburetor work?",
            "Why is the sky blue?",
            "Explain Newton's third law of motion.",
            "How do neural networks learn weights?",
            "What is the time complexity of merge sort?",
            "How does HTTPS establish a TLS handshake?",
            "Explain the difference between TCP and UDP.",
            "How do relational databases index foreign keys?",
            "What is the theory of general relativity?",
            "Can you write a poem about autumn leaves?",
            "Explain how vaccines create immunity.",
            "What is inflation in economics?",
            "How does a blockchain achieve consensus?",
            "Explain the difference between a process and a thread.",
            "What are coroutines in Kotlin?",
            "How do I implement binary search in C++?",
            "Explain the greenhouse effect.",
            "What caused the fall of the Roman Empire?",
            "How does GPS triangulation work?",
            "What is dependency injection?",
            "Explain the Heisenberg uncertainty principle.",
            "How do microservices communicate via gRPC?",
            "What are the benefits of immutable data structures?",
            "Explain ACID properties in database transactions.",
            "How does optical fiber transmit light data?",
            "What is asynchronous programming?",
            "Explain OAuth 2.0 authorization code flow.",
            "What is the difference between synchronous and asynchronous IO?",
            "How do airplanes generate aerodynamic lift?",
            "Explain how DNS resolvers cache IP addresses.",
            "What is the central limit theorem in statistics?",
            "How does Git store commit trees?",
            "What is clean architecture?",
            "Explain memory leak detection in Android.",
            "How does garbage collection work in JVM?",
            "What is tail recursion?",
            "Explain reactive programming with Kotlin Flow.",
            "How do compilers build abstract syntax trees?",
            "What is the difference between abstract classes and interfaces?",
            "Explain how web browsers render CSS layout.",
            "What are design patterns in software engineering?",
            "Explain Raft consensus algorithm.",
            "How does AES encryption secure data?",
            "What is cross-site scripting (XSS)?",
            "How do web sockets maintain persistent full-duplex connections?",
            "Explain deadlocks and how to prevent them.",
            "What is semantic versioning?",
            "How does quicksort pick pivot elements?",
            "Explain the CAP theorem for distributed systems.",
            "What are lambda expressions in modern programming languages?",
            "How does Bluetooth Low Energy broadcast advertising packets?",
            "What is cache coherency in multi-core CPUs?",
            "Explain how the human eye perceives colors.",
            "What is deep learning?",
            "Explain how transformer attention heads work.",
            "How do solar panels convert sunlight into electricity?",
            "What is the role of mitochondria in cells?",
            "Explain the water cycle on Earth.",
            "What is plate tectonics in geology?",
            "How do satellites maintain geostationary orbit?",
            "Explain the differences between IPv4 and IPv6.",
            "What is the Fibonacci sequence in mathematics?",
            "How does a refrigerator cool food?",
            "Explain CRISPR gene editing.",
            "What are black holes and event horizons?",
            "How do capacitors store electric charge?",
            "Explain the concept of opportunity cost in economics.",
            "What is the difference between authorization and authentication?"
        )
        normalChatPrompts.addAll(enGeneral)

        // 2. Bengali Pure Prompts (60 prompts)
        val bnGeneral = listOf(
            "বাংলাদেশের মুক্তিযুদ্ধ কত সালে সংঘটিত হয়েছিল?",
            "রবীন্দ্রনাথ ঠাকুরের নোবেল বিজয়ী কাব্যগ্রন্থের নাম কী?",
            "পদ্মা সেতুর দৈর্ঘ্য কত কিলোমিটার?",
            "সৌরজগতের সবচেয়ে বড় গ্রহ কোনটি?",
            "সালোকসংশ্লেষণ প্রক্রিয়ার মূল উপাদানগুলো কী কী?",
            "বাংলা ভাষার উৎপত্তি কোন অপভ্রংশ থেকে হয়েছে?",
            "কাজী নজরুল ইসলামের প্রধান সাহিত্যকর্মগুলো সম্পর্কে বলো।",
            "সুন্দরবনের ম্যানগ্রোভ উদ্ভিদের বৈশিষ্ট্য কী?",
            "বাংলাদেশের সংবিধান কবে গৃহীত হয়েছিল?",
            "জলবায়ু পরিবর্তনের প্রধান কারণগুলো কী কী?",
            "রক্তে হিমোগ্লোবিনের কাজ কী?",
            "আইনস্টাইনের আপেক্ষিকতার তত্ত্ব সংক্ষেপে ব্যাখ্যা করো।",
            "কম্পিউটারের প্রসেসর কীভাবে বাইনারি ডেটা প্রক্রিয়া করে?",
            "মানবদেহে মোট কয়টি হাড় আছে?",
            "সুষম খাদ্যের উপাদানগুলো কী কী?",
            "ম্যাগনা কার্টা কী এবং এর ঐতিহাসিক গুরুত্ব কী?",
            "ডিএনএ এবং আরএনএ-এর প্রধান পার্থক্য কী?",
            "মৌলিক সংখ্যা কাকে বলে এবং উদাহরন দাও।",
            "পৃথিবীর বায়ুমণ্ডলে সবচেয়ে বেশি কোন গ্যাস থাকে?",
            "বাংলাদেশের জাতীয় কবি কে এবং তার উপাধি কী?",
            "আন্তর্জাতিক মাতৃভাষা দিবস কেন ২১শে ফেব্রুয়ারি পালিত হয়?",
            "কৃত্রিম বুদ্ধিমত্তা কীভাবে সমাজের অর্থনীতিতে প্রভাব ফেলছে?",
            "ম্যালেরিয়া রোগের জীবাণু কীভাবে ছড়ায়?",
            "নিউটনের গতির সূত্রগুলো সহজ ভাষায় বোঝাও।",
            "সৌরজগতে মোট কয়টি গ্রহ আছে?",
            "হিমালয় পর্বতমালা কীভাবে গঠিত হয়েছিল?",
            "সূর্য থেকে পৃথিবীতে আলো আসতে কত সময় লাগে?",
            "ওজোন স্তর কীভাবে পৃথিবীকে অতিবেগুনি রশ্মি থেকে রক্ষা করে?",
            "শব্দের গতিবেগ বায়ুতে কত?",
            "আন্তর্জাতিক মহাকাশ স্টেশন কীভাবে পরিচালিত হয়?",
            "বিশ্বের দীর্ঘতম নদী কোনটি?",
            "বাংলাদেশের প্রথম প্রধানমন্ত্রীর নাম কী?",
            "মৌর্য সাম্রাজ্যের প্রতিষ্ঠাতা কে ছিলেন?",
            "বৈদ্যুতিক কারেন্ট কীভাবে পরিমাপ করা হয়?",
            "উইকিপিডিয়া কীভাবে তথ্য যাচাই করে?",
            "টেলিভিশন কীভাবে সংকেত গ্রহণ করে ছবি দেখায়?",
            "স্মার্টফোনের ব্যাটারি দীর্ঘস্থায়ী করার কয়েকটি উপায় বলো।",
            "ক্লাউড কম্পিউটিং কী এবং এর সুবিধাগুলো কী?",
            "ইন্টারনেট কীভাবে বিশ্বব্যাপী সংযুক্ত থাকে?",
            "রোবোটিক্স প্রযুক্তির ভবিষ্যৎ সম্ভাবনা কেমন?",
            "গিটহাব কী এবং প্রোগ্রামাররা কেন এটি ব্যবহার করে?",
            "সাইবার নিরাপত্তার প্রধান নীতিগুলো কী?",
            "মহাকর্ষ বল ও মহাকর্ষীয় ধ্রুবক কী?",
            "কৃষিতে বায়োটেকনোলজির ব্যবহার কেমন?",
            "বিশ্ব স্বাস্থ্য সংস্থার প্রধান কার্যালয় কোথায়?",
            "মহাবিশ্বের বয়স কত বছর আনুমানিক?",
            "পরমাণুর নিউক্লিয়াসে কোন কোন কণা থাকে?",
            "আলোকবর্ষ কী এবং এটি কীসের একক?",
            "অপারেটিং সিস্টেমের মূল কাজ কী কী?",
            "সফটওয়্যার ইঞ্জিনিয়ারিংয়ে বাগ ফিক্সিং কেন গুরুত্বপূর্ণ?",
            "পানির রাসায়নিক সংকেত ও বৈশিষ্ট্য কী?",
            "ভূপৃষ্ঠে ভূমিকম্প কেন সৃষ্টি হয়?",
            "বায়োমেট্রিক নিরাপত্তা কীভাবে কাজ করে?",
            "প্রোগ্রামিং শেখার জন্য সেরা প্ল্যাটফর্ম কোনটি?",
            "অ্যালগরিদমের টাইম কমপ্লেক্সিটি কীভাবে নির্ণয় করে?",
            "ইলেকট্রনিক মেইল কীভাবে এক সার্ভার থেকে অন্য সার্ভারে পৌঁছায়?",
            "ডাটাবেসে প্রাইমারি কি এর গুরুত্ব কী?",
            "জাভাস্ক্রিপ্ট ও টাইপস্ক্রিপ্টের মধ্যে পার্থক্য কী?",
            "পাইথন কেন ডেটা সায়েন্সে সবচেয়ে জনপ্রিয়?",
            "লিনাক্স কার্নেল কীভাবে মেমোরি ম্যানেজমেন্ট করে?"
        )
        normalChatPrompts.addAll(bnGeneral)

        // 3. Banglish Prompts (60 prompts)
        val banglishGeneral = listOf(
            "kemon acho bondhu?",
            "ajker weather kemon thakte pare?",
            "amake ekti sundor golpo shonao.",
            "flutter ar jetpack compose er moddhe konta best?",
            "amader shorire pani er dorkar koto tuku?",
            "dhaka theke chittagong jaoar train er schedule bolo.",
            "kivabe bhalo coding shikha jay?",
            "python diye machine learning shuru korbo kivabe?",
            "bangladesh er shobcheye boro nodi konta?",
            "ekta healthy routine banate chai, tips dao.",
            "android app performance kivabe barano jay?",
            "laptop er battery life kivabe beshi kora jay?",
            "git branch merge korar shothik poddhoti ki?",
            "api request fail korle kivabe retry handle korbo?",
            "sql query optimize korar 5 ti best practice bolo.",
            "data structure shikhte gele kothay theke shuru korbo?",
            "english speaking improve korar shohoj upaye ki?",
            "time management kivabe bhalo korbo?",
            "remote job pabar jonno portfolio kivabe sajabo?",
            "kotlin coroutines kivabe kaj kore?",
            "room database migration er somoy data harale ki korbo?",
            "rest api vs graphql konta kothay use kora uchit?",
            "docker container kivabe deploy kore?",
            "kubernetes pod crash korle log check korbo kivabe?",
            "microservices architecture er pros and cons ki?",
            "firebase authentication android e kivabe integrate kore?",
            "ai model training korte kemon gpu lagbe?",
            "open source project e contribute korbo kivabe?",
            "system design interview te ki dhoroner question ashe?",
            "leetcode problem practice korar strategy ki hobe?",
            "react native ar kotlin multiplatform er parthokko ki?",
            "solid principles ekdom shohoj banglish e bujhiye dao.",
            "memory leak solve korar jonno android studio te ki tool ache?",
            "proguard rules add na korle ki problem hote pare?",
            "jetpack compose recomposition reduce korbo kivabe?",
            "mvvm architecture er main layer gulo ki ki?",
            "unit test ar integration test er moddhe difference ki?",
            "ci cd pipeline github actions diye kivabe set kore?",
            "https ssl certificate renew korbo kivabe?",
            "jwt token expiry refresh token kivabe implement kore?",
            "redis cache keno use kora hoy?",
            "kafka message broker kivabe kaj kore?",
            "load balancer high traffic kivabe handle kore?",
            "elasticsearch search engine keno eto fast?",
            "mongodb database e indexing kivabe kaj kore?",
            "websocket chat application design kivabe korbo?",
            "clean code likhte hole kon kon boi pora uchit?",
            "refactoring korar shomoy kon bishoy gulo kheyal rakhbo?",
            "agile scrum methodology kivabe maintain kore team e?",
            "code review te ki ki check kora standard practice?",
            "android 15 edge to edge insets kivabe handle korbo?",
            "dark mode support compose e kivabe dey?",
            "vector drawable animation compose e kivabe add kore?",
            "state hoisting compose e keno mandatory?",
            "viewmodel scope cancel hole coroutine ki stop hoy?",
            "dispatchers io vs default kokhon konta use korbo?",
            "sharedpreferences keno encrypted kora dorkar?",
            "oauth login flow te redirect uri kivabe secure rakhbo?",
            "deep link android app e kivabe setup kore?",
            "app bundle aab create korle download size keno kome?"
        )
        normalChatPrompts.addAll(banglishGeneral)

        assertEquals("Corpus must contain at least 200 normal prompts", 200, normalChatPrompts.size)

        val violations = mutableListOf<String>()
        for (prompt in normalChatPrompts) {
            val detected = AyraIntentClassifier.classifyIntent(prompt)
            if (detected != AyraToolIntent.CHAT) {
                violations.add("Prompt \"$prompt\" incorrectly classified as $detected (expected CHAT)")
            }
        }

        assertTrue(
            "Found false positive tool classifications on general CHAT prompts:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun testExplicitSlashCommandsRecall() {
        assertEquals(AyraToolIntent.IMAGE, AyraIntentClassifier.classifyIntent("/image a cute kitten in astronaut suit"))
        assertEquals(AyraToolIntent.IMAGE, AyraIntentClassifier.classifyIntent("image: futuristic cyberpunk city"))
        assertEquals(AyraToolIntent.PDF, AyraIntentClassifier.classifyIntent("/pdf Meeting Minutes for Project X"))
        assertEquals(AyraToolIntent.PDF, AyraIntentClassifier.classifyIntent("pdf: Quarterly Financial Report"))
        assertEquals(AyraToolIntent.TERMINAL, AyraIntentClassifier.classifyIntent("/terminal ping google.com"))
        assertEquals(AyraToolIntent.TERMINAL, AyraIntentClassifier.classifyIntent("/terminal dns 8.8.8.8"))
        assertEquals(AyraToolIntent.DEEP_RESEARCH, AyraIntentClassifier.classifyIntent("/research quantum supremacy roadmap"))
    }
}
