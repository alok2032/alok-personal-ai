# Ayra AI — Remediation & Audit Log

**Target Application:** `com.ayra.ai.app` (Kotlin, Jetpack Compose, Room, Android SDK 26–36)  
**Audit Baseline Date:** 8 Nov 2026  
**Auditor / Engineering Lead:** Principal Android Engineer & QA  

---

## 1. Audit Matrix (F01 – F21)

| Finding ID | Title & Scope | Priority | Severity | Planned Fix Strategy | Test Strategy | Status |
|---|---|---|---|---|---|---|
| **F01** | Camera attach FileProvider crash | P0 | Crash | Place capture file under `cacheDir/exports/`, wrap in try/catch with permission rationale check | Unit & intent test verifying path matching `file_paths.xml` | **Fixed (Phase 1)** |
| **F02** | PDF open & share file_paths mismatch | P0 | Crash / Broken UX | Expose exact `generated_pdfs/` and `chat_exports/` in `file_paths.xml`, share via FileProvider | Path resolution & sharing test on target APIs | **Fixed (Phase 1)** |
| **F03** | Fake VPN service packet drop | P0 | False Claim & Broken Net | **Option B**: Remove packet-dropping VPN service, manifest declaration, and fake encryption claims | Connectivity & state test confirming no network hijacking | **Fixed (Phase 1)** |
| **F04** | Device actions run unconfirmed from raw text | P0 | Security / Action Safety | Implement typed `ProposedAction` ActionGate with explicit user confirmation cards; ACTION_DIAL and composer | Unit tests ensuring unconfirmed prompts never trigger execution | **Fixed (Phase 1)** |
| **F05** | Scheduled task unreliability & alarm leak | P0 | Reliability / Data Leak | Cancel AlarmManager PendingIntents on delete; state machine (PENDING, FIRED, DONE, FAILED); notification actions | Task cancellation & lifecycle reboot tests | **Fixed (Phase 1)** |
| **F06** | Simulated features presented as real | P0 | False Claims | Replace simulated results with honest fallback/labels (e.g., Demo/Simulated) or remove fake terminal stats | Golden assertion tests validating honesty of labels | **Planned (Phase 3)** |
| **F07** | Keyword router hijacks ordinary prompts | P0 | Intent Routing Bug | Remove over-broad substring keyword matches; implement structured tool calling with regression corpus | 300+ prompt regression test with strict negatives | **Planned (Phase 3)** |
| **F08** | Private document data leak to third party | P0 | Privacy / Exfiltration | Eliminate unconsented document text forwarding to third-party endpoints; add explicit data disclosure | Request assertion test verifying no unsolicited exfiltration | **Fixed (Phase 1)** |
| **F09** | Secret storage & key security | P0 | Security / Key Handling | Fail closed on Keystore errors, remove insecure fallback algorithms; keep secrets out of logs/APK | Keystore fail-closed unit tests | **Fixed (Phase 1)** |
| **F10** | Companion persona safety & crisis detection | P0 | User Safety | Default to neutral assistant, 18+ gate for companion, add CrisisDetector with regional help resources | Crisis detector unit tests (EN, BN, HI) with high recall | **Fixed (Phase 1)** |
| **F11** | Chat pipeline lifecycle, rotation & double-tap | P1 | High UX / Stability | Migrate state handling to `ChatViewModel`, add in-flight guards, rotation safety, and IO dispatchers | ViewModel lifecycle and double-send unit tests | **Fixed (Phase 2)** |
| **F12** | AI provider circuit breaker & error taxonomy | P1 | Resilience | Unified orchestrator with structured retry caps (max 3), honest error states, and bounded LruCache | MockWebServer matrix (200, 401, 429, 500) | **Fixed (Phase 2)** |
| **F13** | Dead or misleading settings | P1 | False Affordance | Connect settings to real DataStore persistence or remove non-functional toggles | Settings toggle assertion test suite | **Planned (Phase 3)** |
| **F14** | Number & time parsing bugs in Bengali/English | P1 | Functional Bug | Deterministic java.time parser supporting Bengali/English numerals, range checks, and token matching | Property test over Bengali/English digit strings | **Planned (Phase 3)** |
| **F15** | Voice recognizer leak & loop | P1 | Battery / Resource Leak | Push-to-talk default, exponential backoff, proper recognizer/TTS cleanup in onCleared | Voice lifecycle and error-code unit tests | **Planned (Phase 3)** |
| **F16** | Over-broad launch permissions | P1 | Privacy / Play Policy | Just-in-time permission requests, remove unused declarations, handle permanent denial gracefully | Permission denial flow tests | **Planned (Phase 3)** |
| **F17** | Room destructive migration & PDF text parsing | P1 | Data Loss / Bug | Controlled schema migrations, memory windowing, and safe PDF text parsing with honest truncation notice | Room migration tests and PDF extraction tests | **Fixed (Phase 2)** |
| **F18** | Cyber terminal NetworkOnMainThreadException | P1 | ANR / Crash | Offload terminal queries to Dispatchers.IO with timeouts and consent checks | Threading & timeout test on mock network calls | **Planned (Phase 3)** |
| **F19** | UI string hardcoding & accessibility | P2 | Quality / A11y | Externalize user strings to XML resources, add content descriptions, 48dp touch targets | Accessibility and layout validation | **Planned (Phase 4)** |
| **F20** | Build hygiene & dependency cleanup | P2 | Build Hygiene | Add Gradle wrapper, clean unused dependencies, configure StrictMode and .gitignore | Dependency check & clean build validation | **Fixed in Phase 0** |
| **F21** | Test suite fidelity & regression coverage | P2 | QA Gap | Replace mock-only assertions with real JVM tests covering CUJs, parsers, and error states | Full test suite execution via Gradle | **Verified (49 tests passed)** |

---

## 2. Phase Execution Log

### Phase 0: Foundation, Tooling, Keystore & StrictMode
- **Status:** Completed
- **Changes:**
  - Verified Gradle wrapper generation (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.properties`).
  - Added `AyraApplication.kt` with `StrictMode` enabled for debug builds.
  - Registered `AyraApplication` in `AndroidManifest.xml`.
  - Created `AUDIT.md` tracking all 21 findings across phases.
  - Created `AyraAuditTestSkeleton.kt` establishing regression baselines.

### Phase 1: Critical Fixes & Honesty (F01–F05, F08–F10)
- **Status:** Completed & Verified
- **Changes:**
  - **F01 (Camera crash):** Added runtime camera permission handling, safely placed capture target files in `cacheDir/exports/`, updated `file_paths.xml` with `cache_exports`.
  - **F02 (PDF Open & Share):** Configured `generated_pdfs/` and `chat_exports/` in `file_paths.xml`; caught typed `ActivityNotFoundException` without swallowing errors.
  - **F03 (Fake VPN Removal):** Deleted `AyraVpnService.kt`, purged `BIND_VPN_SERVICE` from `AndroidManifest.xml`, replaced VPN interface with an honest "Device Privacy & Security" status card.
  - **F04 (ActionGate & ProposedAction):** Introduced typed `ProposedAction` sealed class, `ContactResolver`, and Compose `ActionConfirmationCard`. Requires explicit user confirmation; dispatches calls via `Intent.ACTION_DIAL` and SMS via `Intent.ACTION_SENDTO`.
  - **F05 (Scheduled Tasks & Alarms):** Added `cancelAlarm()` via `AlarmManager` with matching `PendingIntent` cancellation; registered system boot/time receivers in `AndroidManifest.xml`; unique notification IDs generated deterministically.
  - **F08 (Third-party Query Privacy):** Purged unconsented query forwarding and URL-encoded fallback leaks in `UnifiedAiEngine`.
  - **F09 (Secrets Security):** Re-implemented `SecureKeyStore` using AES-256-GCM failing closed on errors without plaintext/Base64 fallbacks.
  - **F10 (Persona Safety & Crisis Detector):** Built multilingual `CrisisDetector` covering Bengali, English, Hindi, and Banglish with idiom exclusions; changed default persona to `AI_ASSISTANT`; integrated hotline guidance.

### Phase 2: Architecture, AI Circuit Breaker, API Key Perfection & Data Layer (F11, F12, F17)
- **Status:** Completed & Verified
- **Changes:**
  - **F11 (ChatViewModel & Concurrency):** Created `ChatViewModel` managing `inputText`, `isGenerating`, attachments, and pending actions using `StateFlow` and atomic coroutine mutex `tryStartSending()`. Prevents duplicate requests from rapid user double-taps and protects chat state during device orientation changes.
  - **F12 (Provider Resilience & Circuit Breaker):** Built `ProviderCircuitBreaker` enforcing a 3-consecutive-failure threshold before tripping to `OPEN` state, automatic cooldown recovery to `HALF_OPEN`, and an in-memory bounded LRU cache providing 0ms responses for duplicate queries.
  - **All API Keys & Providers Overhaul ("sob api key er sob kichu akdom perfect"):**
    - Supported core LLM providers: `GEMINI`, `OPENAI`, `ANTHROPIC`, `DEEPSEEK`, `XAI`, `MISTRAL`, `PERPLEXITY`, `OPENROUTER`, and `LOCAL_OLLAMA`.
    - Added dedicated live search & voice engines: `TavilySearchEngine` (live web grounding & citations) and `ElevenLabsVoiceEngine` (neural voice synthesis).
    - In `ProviderKeysSection.kt`: added one-tap clipboard paste, password visibility toggle, instant Keystore wipe on clear, live test ping with millisecond roundtrip latency, and on-device AES-256-GCM Keystore persistence.
    - Wired `TavilySearchEngine` into `UnifiedAiEngine.executeDeepResearch` for real live web search citations.
  - **F17 (Storage & Document Safety):** Implemented path traversal prevention with `sanitizeFileName()`, enforced a 50,000-character safety bound on PDF/document text extraction, and appended a transparent context truncation notice.
- **Verification:**
  - Unit tests: **49 tests completed, 49 passed, 0 failures** (`gradle :app:testDebugUnitTest`).
  - Compilation: **Clean build succeeded** (`compile_applet`).
