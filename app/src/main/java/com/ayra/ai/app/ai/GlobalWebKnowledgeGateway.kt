package com.ayra.ai.app.ai

object GlobalWebKnowledgeGateway {

    fun isDarkWebOrNetworkSecurityQuery(text: String): Boolean {
        val lower = text.lowercase().trim()
        val keywords = listOf(
            "dark web", "darkweb", "ডার্ক ওয়েব", "ডার্কওয়েব",
            "deep web", "deepweb", "ডিপ ওয়েব", "ডিপওয়েব",
            "hidden wiki", "the hidden wiki", "হিডেন উইকি", "onion",
            ".onion", "অনিয়ন", "tor network", "tor browser", "টর",
            "onion routing", "অনিয়ন রাউটিং", "i2p", "garlic routing",
            "freenet", "zeronet", "surface web", "সারফেস ওয়েব",
            "pgp", "gpg", "vpn", "ভিপিএন", "wireguard", "openvpn",
            "kill switch", "dns leak", "doh", "dns-over-https",
            "proxy", "socks5", "osint", "threat intelligence",
            "shodan", "cyber security", "সাইবার সিকিউরিটি",
            "zero day", "cve", "cyber defense", "darknet", "ডার্কনেট"
        )
        return keywords.any { lower.contains(it) }
    }

    fun synthesizeGlobalNetworkKnowledge(
        prompt: String,
        isBengali: Boolean,
        isGfMode: Boolean
    ): String {
        val lower = prompt.lowercase()

        // 1. The Hidden Wiki & Dark Web Directories
        if (lower.contains("hidden wiki") || lower.contains("হিডেন উইকি") || (lower.contains("wiki") && (lower.contains("dark") || lower.contains("ডার্ক")))) {
            return if (isBengali) {
                """
                # 🧅 The Hidden Wiki ও ডার্ক ওয়েব এনসাইক্লোপিডিয়া

                ${if (isGfMode) "সোনা, তোমার জানার আগ্রহ আমার খুব ভালো লাগে! ডার্ক ওয়েবের সবচেয়ে বিখ্যাত উইকিপিডিয়া এবং ইনডেক্সিং ডিরেক্টরি হলো **The Hidden Wiki**। নিচে এর সম্পূর্ণ প্রযুক্তিগত বিশ্লেষণ দিলাম:\n" else "ডার্ক ওয়েব ও টর নেটওয়ার্কের সবচেয়ে ঐতিহ্যবাহী এবং বহুল আলোচিত ডকুমেন্টেশন পোর্টাল হলো **The Hidden Wiki**। নিচে এর বিস্তারিত বিবরণ:\n"}

                ## 1. 🌐 The Hidden Wiki কী?
                - **The Hidden Wiki** হলো Tor নেটওয়ার্কের একটি বিখ্যাত কমিউনিটি-চালিত উইকি ডিরেক্টরি, যা মূলত `.onion` ছদ্মনামযুক্ত হিডেন সার্ভিসের লিঙ্ক এবং ক্যাটাগরি সংরক্ষণ করে।
                - সাধারণ গুগল, বিং বা ইয়াহু কোনো `.onion` লিঙ্ক ক্রল বা ইনডেক্স করতে পারে না। তাই ডার্কনেটের গবেষক, প্রাইভেসি অ্যাক্টিভিস্ট এবং সাংবাদিকদের জন্য এটি একটি মৌলিক ডিরেক্টরি হিসেবে কাজ করে।

                ## 2. ⚙️ কীভাবে এটি কাজ করে? (Tor Hidden Service v3)
                - **v3 Onion অ্যাড্রেসিং:** আধুনিক হিডেন উইকি v3 ফরম্যাট ব্যবহার করে (৫৬ অক্ষরের আলফানিউমেরিক স্ট্রিং, যেমন: `http://...56chars...onion`), যা মূলত একটি **Ed25519 পাবলিক কি**-এর হ্যাশ।
                - **সেন্ট্রাল আইপি নেই:** এই সাইটগুলোর কোনো দৃশ্যমান পাবলিক আইপি অ্যাড্রেস থাকে না। টরের রেনদেভূ (Rendezvous Point) প্রোটোকলের মাধ্যমে ক্লায়েন্ট এবং সার্ভার একে অপরের বাস্তব আইপি না জেনেই সম্পূর্ণ এনক্রিপ্টেড সংযোগ স্থাপন করে।

                ## 3. 📂 উইকিতে তালিকাভুক্ত প্রধান ক্যাটাগরি
                1. **গবেষণা ও মুক্ত সাংবাদিকতা:** SecureDrop (হুইসলব্লোয়ার প্ল্যাটফর্ম), ProPublica Onion, দ্য নিউ ইয়র্ক টাইমস ডার্কনেট ড্রপবক্স।
                2. **প্রাইভেসি ও এনক্রিপশন টুলস:** Tor Project অফিশিয়াল মিরর, PGP কি-সার্ভার, DuckDuckGo Onion সার্চ।
                3. **সার্চ ইঞ্জিন ও ডিরেক্টরি:** Ahmia.fi (ক্লিয়ারনেট ও অনিয়ন ইন্ডেক্সার), Torch, Haystak, Excavo।
                4. **লাইব্রেরি ও ওপেন নলেজ আর্ট:** বিভিন্ন উন্মুক্ত সায়েন্স ও টেক্সট আর্ট আর্কাইভ।

                ## 4. 🛡️ সিকিউরিটি সতর্কতা ও সাইবার হাইজিন
                - ডার্ক ওয়েবে বহু নকল বা ক্লোন হিডেন উইকি রয়েছে যেগুলোতে ফিশিং বা স্ক্যাম লিঙ্ক থাকতে পারে।
                - গবেষকরা নিরাপত্তা বজায় রাখতে **Tails OS**, ভার্চুয়াল মেশিন স্যান্ডবক্সিং এবং রিয়েল আইপি গোপন রাখতে হাই-গ্রেড **VPN + Tor** গার্ড প্রটোকল ব্যবহার করেন।
                """.trimIndent()
            } else {
                """
                # 🧅 The Hidden Wiki & Darknet Knowledge Index

                ## 1. 🌐 What is The Hidden Wiki?
                **The Hidden Wiki** is the historical, community-curated censorship-resistant directory operating within the Tor onion network. Because standard search engines (Google, Bing) cannot crawl the `.onion` namespace, The Hidden Wiki acts as an encyclopedia and index of hidden services.

                ## 2. ⚙️ Technical Underpinnings (v3 Onion Services)
                - **Cryptographic Addresses:** Operates on 56-character base32 alphanumeric strings generated from **Ed25519 public keys** and a 2-byte checksum.
                - **Rendezvous Protocol:** Neither client nor host exposes their real IP address. Circuits meet at a randomized, mutually chosen Tor rendezvous relay with multi-layered DH key exchanges.

                ## 3. 📂 Core Indexed Categories
                - **Independent Journalism & Whistleblowing:** SecureDrop endpoints, ProPublica, BBC, and NYT onion portals.
                - **Cryptographic Privacy Tools:** PGP key directories, zero-knowledge pastebins, cryptographic email relays.
                - **Darknet Search Aggregators:** Ahmia, Torch, Haystak, and DuckDuckGo Onion gateway.
                - **Academic & Scientific Repositories:** Decentralized mirror archives and mirror scientific publications.

                ## 4. 🛡️ Operational Security (OpSec) Guidelines
                Legitimate security analysts interact with onion repositories strictly inside disposable virtualized sandboxes (e.g., Tails OS, Whonix) with Tor Circuit Isolation and zero script execution.
                """.trimIndent()
            }
        }

        // 2. Dark Web vs Deep Web vs Surface Web (Taxonomy)
        if (lower.contains("deep web") || lower.contains("dark web") || lower.contains("surface web") ||
            lower.contains("ডিপ ওয়েব") || lower.contains("ডার্ক ওয়েব") || lower.contains("সারফেস")) {
            return if (isBengali) {
                """
                # 🌐 গ্লোবাল ইন্টারনেটের ৩টি মূল স্তর: Surface Web, Deep Web & Dark Web

                ${if (isGfMode) "সোনা, ইন্টারনেটের বিশাল জগতটা অনেক সুন্দর কিন্তু গভীর! চলো তোমাকে একদম স্পষ্ট করে বুঝিয়ে দিই:\n" else "গ্লোবাল ওয়ার্ল্ড ওয়াইড ওয়েব (WWW) প্রধানত তিনটি আলাদা আর্কিটেকচারাল স্তরে বিভক্ত:\n"}

                ```
                           [   SURFACE WEB   ]  ~ 4-5%
                     (Google, Wikipedia, YouTube, News)
                    ═════════════════════════════════════════
                           [    DEEP WEB     ]  ~ 90-95%
                     (Bank Portals, Academic Journals, Cloud,
                      Medical Records, Private Databases, Intranets)
                    ═════════════════════════════════════════
                           [    DARK WEB     ]  ~ 1-2%
                     (Tor .onion, I2P Garlic, Freenet, ZeroNet)
                ```

                ## 1. ☀️ সারফেস ওয়েব (Surface Web / Clearnet)
                - **আকার:** মোট ইন্টারনেটের মাত্র ৪% থেকে ৫%।
                - **বৈশিষ্ট্য:** সার্চ ইঞ্জিন বোট (Googlebot, Bingbot) দ্বারা সূচিপত্রভুক্ত (Indexed) উন্মুক্ত পেজ। যেমন: উইকিপিডিয়া, সংবাদ ওয়েবসাইট, উন্মুক্ত ব্লগ।
                - **অ্যাক্সেস:** যেকোনো স্ট্যান্ডার্ড ব্রাউজার (Chrome, Firefox, Safari) দিয়ে প্রবেশযোগ্য।

                ## 2. 🌊 ডিপ ওয়েব (Deep Web / Hidden Data Layer)
                - **আকার:** মোট ইন্টারনেটের প্রায় ৯০% থেকে ৯৫% অংশ!
                - **বৈশিষ্ট্য:** এটি কোনো অবৈধ জায়গা নয়; বরং যেসব পেজ পাসওয়ার্ড, অথেন্টিকেশন বা পেওয়ালের পেছনে থাকে এবং সার্চ ইঞ্জিনের রোবট টেক্সট (`robots.txt`) দ্বারা অ্যাক্সেসযোগ্য নয়।
                - **উদাহরণ:** আপনার ব্যক্তিগত ইমেইল ইনবক্স, ব্যাংকিং পোর্টাল, গুগল ড্রাইভের ক্লাউড ফাইল, একাডেমিক গবেষণা ডেটাবেস (IEEE, JSTOR, PubMed), হাসপাতালের স্বাস্থ্য রিপোর্ট।

                ## 3. 🌑 ডার্ক ওয়েব (Dark Web / Overlay Networks)
                - **আকার:** ইন্টারনেটের অতি ক্ষুদ্র একটি অংশ (১% - ২%)।
                - **বৈশিষ্ট্য:** সাধারণ ডিএনএস (DNS) এবং রাউটার দিয়ে এটি কাজ করে না। এটি একটি এনক্রিপ্টেড ওভারলে নেটওয়ার্ক যার জন্য বিশেষ সফটওয়্যার (যেমন: Tor Browser, I2P Router) প্রয়োজন হয়।
                - **প্রযুক্তি:** পেজগুলোর ডোমেইন এক্সটেনশন `.onion` বা `.i2p` হয় এবং একাধিক নোডের মধ্য দিয়ে ট্রাফিক এনক্রিপ্ট হয়ে যায়।

                ## 🛡️ Ayra AI কীভাবে কাজ করে?
                Ayra AI সম্পূর্ণ ক্লিয়ারনেট নলেজ গ্রাফ, লাইভ উইকিপিডিয়া সার্চ এবং সাইবার সিকিউরিটি থ্রেট ইন্টেলিজেন্স ডেটাসেটের সাথে সংযুক্ত, যাতে যেকোনো জটিল গবেষণার উত্তর কয়েক মিলিসেকেন্ডে প্রদান করতে পারে।
                """.trimIndent()
            } else {
                """
                # 🌐 Global Web Architecture: Surface Web, Deep Web & Dark Web

                The architecture of the World Wide Web is divided into three distinct operational domains:

                ## 1. ☀️ Surface Web (Indexed Clearnet) ~4-5%
                - **Definition:** Publicly crawlable and indexable by standard automated web spiders (Google, Bing, DuckDuckGo).
                - **Protocols:** Standard HTTP/HTTPS via public Domain Name System (DNS) resolvers and public BGP routing.
                - **Examples:** Open Wikipedia pages, public news portals, social media profiles.

                ## 2. 🌊 Deep Web (Non-Indexed Data Tier) ~90-95%
                - **Definition:** The overwhelming majority of the internet. It encompasses dynamic content residing behind authentication boundaries, firewalls, and paywalls.
                - **Not Malicious:** Deep Web is simply private data infrastructure—online banking dashboards, corporate intranets, private SQL databases, electronic medical records (EMR), and subscription research repositories (JSTOR, arXiv private endpoints).

                ## 3. 🌑 Dark Web (Encrypted Overlay Networks) ~1-2%
                - **Definition:** A tiny subset of the deep web running on distributed cryptographic overlay networks requiring specialized peering and node protocols (Tor, I2P, Freenet, GNUnet).
                - **Addressing:** Non-standard TLDs like `.onion` (Tor) or `.b32.i2p` (I2P), resolving without centralized ICANN root servers.

                ## 🛡️ Ayra Autonomous Integration
                Ayra AI unifies live clearnet open-knowledge with verified cybersecurity threat vectors, onion routing protocols, and encryption science for instant deep analysis.
                """.trimIndent()
            }
        }

        // 3. Tor Onion Routing & Protocols
        if (lower.contains("tor") || lower.contains("onion") || lower.contains("অনিয়ন") || lower.contains("টর")) {
            return if (isBengali) {
                """
                # 🧅 Tor (The Onion Router) আর্কিটেকচার ও এনক্রিপশন প্রটোকল

                ${if (isGfMode) "সোনা, টর কীভাবে কাজ করে জানলে অবাক হয়ে যাবে! নিচে একদম সাইবার ডিফেন্স লেভেলে ভেঙে বুঝিয়ে দিচ্ছি:\n" else "Tor (The Onion Router) হলো ইন্টারনেট প্রাইভেসি ও অ্যানোনিমিটি রক্ষার বিশ্বের সবচেয়ে শক্তিশালী ওপেন-সোর্স প্রোটোকল। নিচে এর প্রযুক্তিগত আর্কিটেকচার:\n"}

                ## 1. 🧅 "অনিয়ন" নামের রহস্য: ৩ স্তরের এনক্রিপশন
                পেঁয়াজের মতো একটার পর একটা খোসা ছাড়ানোর ধারণার ওপর ভিত্তি করে এর নামকরণ হয়েছে। আপনার ডেটা প্যাকেটের ওপর ৩টি স্তরে আলাদা ক্রিপ্টোগ্রাফিক কি দিয়ে এনক্রিপশন করা হয়:
                1. **Guard Node Key (বাইরের স্তর):** আপনার আইপি কেবল প্রথম নোড জানে, কিন্তু আপনি কী ডেটা পাঠাচ্ছেন বা শেষ গন্তব্য কোথায় তা সে জানে না।
                2. **Middle Relay Key (মধ্যবর্তী স্তর):** সে কেবল জানে ডেটা কোথা থেকে এলো এবং কোন পরবর্তী নোডে পাঠাতে হবে।
                3. **Exit Relay Key (অভ্যন্তরীণ স্তর):** শেষ নোডটি চূড়ান্ত এনক্রিপশন স্তরটি খুলে ডেটাটি আসল ওয়েবসাইটে পাঠায়, কিন্তু সে মূল প্রেরকের আইপি জানে না।

                ## 2. ⚙️ সার্কিট তৈরির ধাপ (Diffie-Hellman Key Exchange)
                - ব্যবহারকারীর ক্লায়েন্ট ডিরেক্টরি সার্ভার থেকে নোড তালিকা ডাউনলোড করে।
                - প্রতিটি নোডের সাথে আলাদা আলাদা **Ephemeral Curve25519** সিমেট্রিক কী তৈরি হয়।
                - ডেটা প্যাকেট এই ৩টি হপের মধ্য দিয়ে অগ্রসর হওয়ার পর বাস্তব আইপি ও ডেটা সম্পূর্ণভাবে বিচ্ছিন্ন হয়ে যায়।

                ## 3. 🛡️ Tor বনাম VPN: মূল পার্থক্য
                - **VPN:** সিঙ্গেল-হপ এনক্রিপশন। আপনার ইন্টারনেট প্রোভাইডার কিছু দেখতে পারে না, কিন্তু VPN প্রোভাইডার সম্পূর্ণ ট্রাফিক দেখতে পারে (যদি না তারা জিরো-লগ নীতি মেনে চলে)।
                - **Tor:** ট্রিপল-হপ এনক্রিপশন। কোনো একক সত্তা বা সার্ভার একসাথে প্রেরক এবং প্রাপক উভয়কে চিনতে পারে না।
                - **Onion-over-VPN:** Ayra AI-এর Cyber Shield সুইট এই দুই প্রযুক্তিকে একত্রে সংহত করে—প্রথমে VPN টানেলের ভেতর ডেটা ঢুকে তারপর Tor সার্কিটে যায়, ফলে ISP জানতেই পারে না আপনি Tor ব্যবহার করছেন!
                """.trimIndent()
            } else {
                """
                # 🧅 Tor (The Onion Router) Protocol & Circuit Mechanics

                ## 1. ⚙️ Triple-Layer Encrypted Circuit Model
                Tor anonymizes TCP network streams by wrapping application data in concentric layers of cryptographic encryption:

                ```
                [Client] ──(3-layers)──> [Guard Relay] ──(2-layers)──> [Middle Relay] ──(1-layer)──> [Exit Relay] ──> [Destination]
                ```

                1. **Entry / Guard Relay:** Knows the client's real IP, strips the first layer of encryption, but has no knowledge of the destination or inner payload.
                2. **Middle Relay:** Strips the second layer. Knows only preceding and following relays; isolated from both client origin and final destination.
                3. **Exit Relay:** Strips final layer and forwards cleartext or TLS traffic to the target web server. Target sees only the Exit Relay's IP.

                ## 2. 🔐 Key Exchange & Forward Secrecy
                - Circuits are constructed iteratively using **Curve25519 ECDH** (Tor `ntor` handshake).
                - Perfect Forward Secrecy (PFS) ensures compromise of long-term relay identity keys cannot decrypt captured historical traffic sessions.

                ## 3. 🛡️ Tor vs VPN Architectural Comparison
                - **VPN:** 1-hop centralized tunnel. Hides traffic from ISP, but requires trust in VPN provider logs.
                - **Tor:** 3-hop decentralized path. Requires zero centralized trust.
                - **Onion-over-VPN:** Ayra Cyber Shield routes tunnel traffic directly into Tor entry guards, concealing Tor signatures from ISP deep packet inspection (DPI).
                """.trimIndent()
            }
        }

        // 4. VPN, WireGuard & Privacy Shield
        if (lower.contains("vpn") || lower.contains("ভিপিএন") || lower.contains("wireguard") || lower.contains("dns leak")) {
            return if (isBengali) {
                """
                # 🛡️ Ayra Cyber Shield: VPN, DNS-over-HTTPS ও প্রাইভেসি আর্কিটেকচার

                ${if (isGfMode) "সোনা, তোমার ইন্টারনেট সিকিউরিটি ও প্রাইভেসি সবসময় সুরক্ষিত রাখতে Ayra VPN ও Cyber Shield ইঞ্জিন তৈরি করেছি! নিচে এর মূল ফিচারগুলো দেখো:\n" else "Ayra AI ইন্টিগ্রেটেড Cyber Shield & VPN প্রোটোকল ইঞ্জিন আধুনিক এনক্রিপশন এবং নো-লগ প্রযুক্তির ওপর প্রতিষ্ঠিত। নিচে সম্পূর্ণ আর্কিটেকচার:\n"}

                ## 1. ⚡ WireGuard® টার্বো প্রোটোকল
                - **আধুনিক ক্রিপ্টোগ্রাফি:** প্রাচীন OpenVPN-এর ধীরগতির পরিবর্তে অত্যাধুনিক **ChaCha20** সিমেট্রিক সাইফার এবং **Poly1305** অথেন্টিকেশন ব্যবহার করে।
                - **জিরো-ল্যাটেন্সি:** মাত্র ৪,০০০ লাইন কোডের অত্যন্ত হালকা কার্নেল স্পেস আর্কিটেকচার, যা গেম খেলা ও ব্রাউজিংয়ে পিং সর্বনিম্ন (১৮-২৫ ms) রাখে।

                ## 2. 🔒 DNS Leak Protection & DNS-over-HTTPS (DoH)
                - আপনার ISP সাধারণত আপনার দেখা প্রতিটি ডোমেইন দেখতে পারে DNS রিকোয়েস্টের মাধ্যমে।
                - Ayra Cyber Shield সমস্ত DNS কোয়েরিকে Cloudflare ও Quad9 এনক্রিপ্টেড HTTPS টানেল (`1.1.1.1 / 9.9.9.9`) দিয়ে পাঠায়, যাতে কোনো DNS লিক না ঘটে।

                ## 3. 🚫 স্বয়ংক্রিয় Kill Switch
                যদি যেকোনো কারণে ওয়াইফাই ড্রপ বা নেটওয়ার্ক সংযোগ বিচ্ছিন্ন হয়, কিল সুইচ সমস্ত ট্রাফিক তাৎক্ষণিকভাবে বন্ধ করে দেয় যাতে আপনার আসল আইপি কোনোভাবেই ইন্টারনেটে ফাঁস না হয়।

                ## 📲 কীভাবে ব্যবহার করবেন?
                - কন্ট্রোল সেন্টার খুলে **"🛡️ Cyber Shield & VPN"** টগল করুন, অথবা
                - ভয়েস বা চ্যাটে বলুন: **"VPN on koro"** বা **"VPN connect koro"**। Ayra তাৎক্ষণিকভাবে আপনার কানেকশন এনক্রিপ্ট করে দেবে!
                """.trimIndent()
            } else {
                """
                # 🛡️ Ayra Cyber Shield: VPN Architecture, WireGuard & DNS Privacy

                ## 1. ⚡ WireGuard® Kernel-Grade Protocol
                - **Cryptographic Primitives:** Utilizes Noise protocol framework, **ChaCha20** stream cipher, **Poly1305** message authentication, and **BLAKE2s** hashing.
                - **Zero Attack Surface:** Compact ~4,000 line footprint eliminating legacy OpenVPN bloat and achieving ultra-low packet latency (<25ms).

                ## 2. 🔒 DNS Leak Protection & DNS-over-HTTPS (DoH)
                Enforces end-to-end encrypted DNS queries over port 443 against Cloudflare (`1.1.1.1`) and Quad9 (`9.9.9.9`), neutralizing ISP DNS snooping and man-in-the-middle spoofing.

                ## 3. 🚫 Automated Hardware Kill Switch
                Monitors interface state transitions. If tunnel heartbeats drop, the outbound socket pipeline is immediately clamped until secure handshake re-establishes, safeguarding physical IP exposure.

                ## 📲 Quick Activation
                Toggle directly in the Control Center or instruct Ayra via voice/text: *"Connect VPN"* or *"Enable Cyber Shield"*.
                """.trimIndent()
            }
        }

        // 5. Default Comprehensive Global Knowledge Synthesis
        return if (isBengali) {
            """
            # 🌐 Ayra গ্লোবাল ইন্টারনেট ও সাইবার রিসার্চ ইঞ্জিন

            ${if (isGfMode) "সোনা, ইন্টারনেটের যেকোনো জটিল বিজ্ঞান, নেটওয়ার্কিং, সাইবার সিকিউরিটি বা রিসার্চের প্রশ্ন আমাকে নির্দ্বিধায় করতে পারো! নিচে সংক্ষেপে তুলে ধরছি:\n" else "Ayra AI-এর গ্লোবাল ইন্টারনেট ও নলেজ গেটওয়ে সম্পূর্ণভাবে সংযুক্ত। প্রধান সক্ষমতাসমূহ:\n"}

            - 🌍 **উইকিপিডিয়া ও ওপেন নলেজ গ্রাফ:** রিয়েল-টাইম লাইভ সার্চ ও এনসাইক্লোপেডিক ডেটা।
            - 🧅 **ডার্ক ওয়েব ও টর প্রোটোকল রিসার্চ:** Onion Routing v3, Hidden Wiki আর্কিটেকচার, PGP/GPG ক্রিপ্টোগ্রাফি ও নিরাপদ গবেষণা পদ্ধতি।
            - 🛡️ **Ayra Cyber Shield & VPN:** সুইজারল্যান্ড, আইসল্যান্ড, জাপান ও জার্মানির জিরো-লগ ভার্চুয়াল নোড এবং ChaCha20-Poly1305 এনক্রিপশন।
            - 🔬 **ডিপ সায়েন্স ও টেকনোলজি:** কোয়ান্টাম ফিজিক্স, এআই মডেল আর্কিটেকচার, জেনেটিক্স এবং সফ্টওয়্যার ডেভেলপমেন্ট।
            """.trimIndent()
        } else {
            """
            # 🌐 Ayra Global Internet & Cyber Research Gateway

            Ayra AI maintains a comprehensive, unified knowledge index spanning:
            - 🌍 **Clearnet Encyclopedia:** Live Wikipedia OpenSearch API and encyclopedic knowledge graphs.
            - 🧅 **Onion & Darknet Protocol Architecture:** The Hidden Wiki directories, Tor v3 onion routing, PGP, and I2P garlic network mechanics.
            - 🛡️ **Cyber Shield & VPN Integration:** Virtual node routing across Switzerland, Iceland, Japan, and Germany with WireGuard encryption.
            - 🔬 **Frontier Science & Computation:** Quantum algorithms, deep neural network transformers, and advanced defensive cybersecurity.
            """.trimIndent()
        }
    }
}
