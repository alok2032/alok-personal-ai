package com.ayra.ai.app.automation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.ayra.ai.app.vpn.AyraVpnPrivacyController
import java.util.regex.Pattern

data class AutomationResult(
    val executed: Boolean,
    val feedbackMessage: String,
    val actionType: String? = null,
    val extraData: String? = null
)

class DeviceAutomationController(private val context: Context) {

    companion object {
        fun isEducationalOrInformationalQuery(text: String): Boolean {
            val trimmed = text.trim()
            val lower = trimmed.lowercase()

            val hasQuestionMark = trimmed.endsWith("?") || trimmed.endsWith("？")

            val questionIndicators = listOf(
                "what is", "what are", "what does", "what's", "why is", "why does", "why do", "why",
                "how to", "how do", "how does", "how can", "how is", "how", "who is", "who was", "who invented",
                "who discovered", "when was", "when did", "where is", "which is", "which one",
                "explain", "describe", "define", "definition of", "meaning of", "summary of",
                "difference between", "compare", "versus", "vs", "code for", "write code",
                "program for", "write a program", "write an app", "create a website", "solve",
                "prove", "calculate", "tell me about", "give me", "show me", "send me", "example of",
                "tutorial", "guide",
                "কি", "কী", "কেন", "কীভাবে", "কেমন", "কারা", "কখন", "কোথায় অবস্থিত",
                "পার্থক্য", "ব্যাখ্যা", "বোঝাও", "বলুন", "বলো তো", "কাকে বলে", "সংজ্ঞা",
                "কোড লেখো", "প্রোগ্রাম", "অ্যালগরিদম", "গণিত", "ইতিহাস", "উদাহরণ"
            )

            for (indicator in questionIndicators) {
                if (lower.startsWith(indicator) || lower.contains(" $indicator ") || lower.contains("$indicator:")) {
                    val isDirectCommand = (lower.contains("turn on") || lower.contains("turn off") ||
                            lower.contains("switch on") || lower.contains("switch off") ||
                            lower.contains("জ্বালাও") || lower.contains("নিভাও") ||
                            lower.contains("অন করো") || lower.contains("অফ করো") ||
                            lower.contains("কানেক্ট করো") || lower.contains("ডিসকানেক্ট করো")) &&
                            (lower.contains("torch") || lower.contains("flashlight") || lower.contains("wifi") ||
                                    lower.contains("bluetooth") || lower.contains("vpn") || lower.contains("টর্চ") ||
                                    lower.contains("ওয়াইফাই") || lower.contains("ব্লুটুথ") || lower.contains("ভিপিএন"))
                    if (!isDirectCommand) return true
                }
            }

            if (hasQuestionMark) {
                val isDirectCommand = (lower.contains("turn on") || lower.contains("turn off") ||
                        lower.contains("জ্বালাও") || lower.contains("নিভাও")) &&
                        (lower.contains("torch") || lower.contains("flashlight") || lower.contains("টর্চ"))
                if (!isDirectCommand) return true
            }

            val techConcepts = listOf(
                "call by value", "call by reference", "callback", "function call", "api call",
                "system call", "method call", "call stack", "recursive call", "syscall",
                "socket", "kafka", "rabbitmq", "message queue", "message passing",
                "http", "tcp", "udp", "osi model", "bluetooth low energy protocol",
                "wifi standard", "wifi 6", "wifi 7", "802.11", "speed of sound", "sound wave",
                "travel of sound", "speed of light", "gps satellite", "trilateration",
                "clock history", "alarm circuit", "camera lens", "camera sensor"
            )
            for (term in techConcepts) {
                if (lower.contains(term)) return true
            }

            return false
        }
    }

    private var isTorchActive = false

    fun tryExecuteAutomation(userInput: String): AutomationResult? {
        if (isEducationalOrInformationalQuery(userInput)) {
            return null
        }

        val lower = userInput.trim().lowercase()

        // 1. Flashlight / Torch Direct Control
        if (isTorchIntent(lower)) {
            return executeTorchCommand(lower)
        }

        // 2. Volume & Audio Automation
        if (isVolumeIntent(lower)) {
            return executeVolumeCommand(lower)
        }

        // 3. Wi-Fi / Bluetooth Direct Settings
        if (isConnectivityIntent(lower)) {
            return executeConnectivityCommand(lower)
        }

        // Camera Direct Capture
        if (isCameraIntent(lower)) {
            return executeCamera()
        }

        // Open Specific App Request
        if (isOpenAppIntent(lower)) {
            return executeOpenApp(lower)
        }

        // Location Request
        if (isLocationIntent(lower)) {
            return executeGetLocation()
        }

        return null
    }

    /**
     * ActionGate evaluation: Identifies actions requiring explicit user confirmation.
     * Returns a ProposedAction model to be confirmed via UI card before execution.
     */
    fun proposeAction(userInput: String): ProposedAction? {
        val lower = userInput.lowercase().trim()
        if (isEducationalOrInformationalQuery(userInput)) return null

        // 1. Phone Call Intent
        if (isCallIntent(lower)) {
            val sayMatch = Regex("(?i)(?:বলো|বলবে|say|tell)\\s*[:,-]?\\s*(.+)").find(userInput)
            val spokenMsg = sayMatch?.groupValues?.get(1)?.trim()

            val numberMatcher = Pattern.compile("(\\+?[0-9]{3,14})").matcher(userInput)
            val (name, number) = if (numberMatcher.find()) {
                val num = numberMatcher.group(1) ?: ""
                Pair(num, num)
            } else {
                val candidateName = userInput.replace(Regex("(?i)^(call|dial|কল করো|ফোন করো)\\s*"), "")
                    .replace(Regex("(?i)\\s*(?:and\\s+say|say|tell|বলো|বলবে).*"), "")
                    .replace("কে", "")
                    .trim()
                val resolved = ContactResolver.resolveContact(context, candidateName)
                if (resolved != null) {
                    Pair(resolved.name, resolved.phoneNumber)
                } else {
                    Pair(candidateName, candidateName)
                }
            }
            return ProposedAction.DirectCall(displayName = name, phoneNumber = number, spokenMessage = spokenMsg)
        }

        // 2. SMS Intent
        if (isDirectSmsIntent(lower)) {
            val numberMatcher = Pattern.compile("(\\+?[0-9]{5,14})").matcher(userInput)
            val msgMatch = Regex("(?i)(?:saying|message|বার্তা|যে)\\s*[:,-]?\\s*(.+)").find(userInput)
            val messageText = msgMatch?.groupValues?.get(1)?.trim() ?: "Hello from Ayra AI!"

            val (recipientName, phoneNumber) = if (numberMatcher.find()) {
                val num = numberMatcher.group(1) ?: ""
                Pair(num, num)
            } else {
                val candidate = userInput.replace(Regex("(?i)^(sms to|send sms to|send message to|মেসেজ পাঠাও|send text to)\\s*"), "")
                    .replace(Regex("(?i)\\s*(?:saying|message|বার্তা|যে).*"), "")
                    .replace("কে", "")
                    .trim()
                val resolved = ContactResolver.resolveContact(context, candidate)
                if (resolved != null) {
                    Pair(resolved.name, resolved.phoneNumber)
                } else {
                    Pair(candidate, candidate)
                }
            }
            return ProposedAction.SendSms(recipient = recipientName, phoneNumber = phoneNumber, messageBody = messageText)
        }

        // 3. Alarm Intent
        if (isAlarmIntent(lower)) {
            var hour = 7
            var minute = 0
            val isPm = lower.contains("pm") || lower.contains("রাত") || lower.contains("সন্ধ্যা")

            val timeMatcher = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(userInput)
            if (timeMatcher.find()) {
                hour = timeMatcher.group(1)?.toIntOrNull() ?: 7
                minute = timeMatcher.group(2)?.toIntOrNull() ?: 0
            } else {
                val singleHourMatcher = Pattern.compile("(\\d{1,2})\\s*(?:am|pm|টা|baje|o'clock)?", Pattern.CASE_INSENSITIVE).matcher(userInput)
                if (singleHourMatcher.find()) {
                    hour = singleHourMatcher.group(1)?.toIntOrNull() ?: 7
                }
            }
            if (isPm && hour < 12) hour += 12
            if (!isPm && hour == 12 && (lower.contains("am") || lower.contains("সকাল"))) hour = 0

            return ProposedAction.SetAlarm(hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59), label = "Ayra Assistant Alarm")
        }

        return null
    }

    /**
     * Executes an action after explicit confirmation in the UI.
     */
    fun executeConfirmedAction(action: ProposedAction): AutomationResult {
        return when (action) {
            is ProposedAction.DirectCall -> {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${action.phoneNumber}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    AutomationResult(
                        executed = true,
                        feedbackMessage = "📞 Opened phone dialer for ${action.displayName} (${action.phoneNumber}) / ডায়ালার খোলা হয়েছে।",
                        actionType = "CALL_PHONE",
                        extraData = action.phoneNumber
                    )
                } catch (e: Exception) {
                    AutomationResult(executed = false, feedbackMessage = "Unable to open phone dialer: ${e.localizedMessage}")
                }
            }
            is ProposedAction.SendSms -> {
                val target = if (action.phoneNumber.matches(Regex("\\+?[0-9]+"))) action.phoneNumber else ""
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$target")).apply {
                    putExtra("sms_body", action.messageBody)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    AutomationResult(
                        executed = true,
                        feedbackMessage = "✉️ Opened SMS composer to ${action.recipient} (${action.phoneNumber}) with message: \"${action.messageBody}\"",
                        actionType = "SMS",
                        extraData = action.phoneNumber
                    )
                } catch (e: Exception) {
                    AutomationResult(executed = false, feedbackMessage = "Unable to open SMS composer: ${e.localizedMessage}")
                }
            }
            is ProposedAction.SetAlarm -> {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, action.hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    AutomationResult(
                        executed = true,
                        feedbackMessage = "⏰ Device alarm scheduled for ${action.formattedTime} / এলার্ম তৈরি করা হয়েছে।",
                        actionType = "ALARM",
                        extraData = action.formattedTime
                    )
                } catch (e: Exception) {
                    openAlarm()
                    AutomationResult(
                        executed = true,
                        feedbackMessage = "⏰ Opened Alarm manager to set alarm for ${action.formattedTime} / এলার্ম ম্যানেজার খোলা হয়েছে।",
                        actionType = "ALARM"
                    )
                }
            }
            is ProposedAction.OpenApp -> {
                executeOpenApp(action.appName.lowercase()) ?: AutomationResult(
                    executed = false,
                    feedbackMessage = "Could not find or launch application: ${action.appName}"
                )
            }
            is ProposedAction.ToggleTorch -> {
                toggleFlashlight(action.enable)
                AutomationResult(
                    executed = true,
                    feedbackMessage = if (action.enable) "🔦 Flashlight turned ON / টর্চ জ্বালানো হয়েছে।" else "🔦 Flashlight turned OFF / টর্চ বন্ধ করা হয়েছে।",
                    actionType = "FLASHLIGHT"
                )
            }
            is ProposedAction.AdjustVolume -> {
                AutomationResult(executed = true, feedbackMessage = "Volume adjusted: ${action.level}", actionType = "VOLUME")
            }
        }
    }

    // --- 1. Flashlight / Torch ---
    private fun isTorchIntent(text: String): Boolean {
        if (text.contains("speed of light") || text.contains("আলোর গতি") ||
            text.contains("কী") || text.contains("what is") || text.contains("explain") ||
            text.contains("who") || text.contains("how") || text.contains("circuit") || text.contains("battery")) {
            return false
        }
        return text.contains("torch on") || text.contains("torch off") ||
                text.contains("turn on torch") || text.contains("turn off torch") ||
                text.contains("flashlight on") || text.contains("flashlight off") ||
                text.contains("turn on flashlight") || text.contains("turn off flashlight") ||
                text.contains("টর্চ অন") || text.contains("টর্চ অফ") ||
                text.contains("লাইট অন") || text.contains("লাইট অফ") ||
                text.contains("লাইট জ্বালাও") || text.contains("লাইট বন্ধ") ||
                text.contains("টর্চ জ্বালাও") || text.contains("টর্চ বন্ধ") ||
                text.contains("আলো জ্বালাও") || text.contains("আলো বন্ধ") ||
                text == "light on" || text == "light off" || text == "torch" || text == "টর্চ"
    }

    fun toggleFlashlight(forceState: Boolean? = null): Boolean {
        val targetState = forceState ?: !isTorchActive
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager != null) {
                val cameraIdList = cameraManager.cameraIdList
                for (id in cameraIdList) {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                    if (hasFlash) {
                        cameraManager.setTorchMode(id, targetState)
                        isTorchActive = targetState
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    fun isFlashlightActive(): Boolean = isTorchActive

    private fun executeTorchCommand(input: String): AutomationResult {
        val shouldTurnOff = input.contains("off") || input.contains("বন্ধ") ||
                input.contains("নিভাও") || input.contains("nibhao") || input.contains("bondho")

        val target = !shouldTurnOff
        val success = toggleFlashlight(target)

        return if (success) {
            val msg = if (target) {
                "🔦 Flashlight turned ON! / টর্চলাইট জ্বালানো হয়েছে।"
            } else {
                "🔦 Flashlight turned OFF! / টর্চলাইট বন্ধ করা হয়েছে।"
            }
            AutomationResult(executed = true, feedbackMessage = msg, actionType = "FLASHLIGHT", extraData = target.toString())
        } else {
            AutomationResult(
                executed = true,
                feedbackMessage = if (target) "🔦 Flashlight command sent. (Ensure camera flash permission is granted)." else "🔦 Flashlight turned off.",
                actionType = "FLASHLIGHT"
            )
        }
    }

    // --- 2. Volume & Audio ---
    private fun isVolumeIntent(text: String): Boolean {
        // Negative guard: physics, science, question explanations about sound
        if (text.contains("explain") || text.contains("how ") || text.contains("what is") ||
            text.contains("travel") || text.contains("speed") || text.contains("কী") ||
            text.contains("গতি") || text.contains("তরঙ্গ") || text.contains("বিজ্ঞান") ||
            text.contains("কাকে বলে")) {
            return false
        }
        val hasVolumeWord = text.contains("volume") || text.contains("ভলিউম") ||
                text.contains("sound") || text.contains("সাউন্ড") ||
                text.contains("mute") || text.contains("ম্যুট") ||
                text.contains("silent") || text.contains("আওয়াজ")
        if (!hasVolumeWord) return false

        // Must contain an actionable verb or explicit state change
        return text.contains("up") || text.contains("down") || text.contains("বাড়িয়ে") ||
                text.contains("বাড়াও") || text.contains("কমিয়ে") || text.contains("কমাও") ||
                text.contains("mute") || text.contains("unmute") || text.contains("max") ||
                text.contains("full") || text.contains("silent") || text.contains("আওয়াজ বাড়াও") ||
                text.contains("আওয়াজ কমাও") || text == "mute" || text == "silent" ||
                text == "volume up" || text == "volume down"
    }

    fun adjustVolume(increase: Boolean): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Audio unavailable"
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return if (increase) "Volume increased 🔊" else "Volume lowered 🔉"
    }

    fun muteVolume(): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Audio unavailable"
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
        return "Audio muted 🔇 / মিডিয়া ভলিউম নিঃশব্দ করা হয়েছে।"
    }

    fun maxVolume(): String {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Audio unavailable"
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI)
        return "Volume set to MAXIMUM 🔊! / সর্বোচ্চ ভলিউম সেট করা হয়েছে।"
    }

    private fun executeVolumeCommand(input: String): AutomationResult {
        val isMute = input.contains("mute") || input.contains("ম্যুট") || input.contains("silent") || input.contains("quiet")
        val isMax = input.contains("max") || input.contains("full") || input.contains("সর্বোচ্চ") || input.contains("ফুল সাউন্ড")
        val isUp = input.contains(Regex("\\bup\\b")) || input.contains("barao") || input.contains("বাড়িয়ে") || input.contains("বাড়াও") || input.contains("raise")

        val feedback = when {
            isMute -> muteVolume()
            isMax -> maxVolume()
            isUp -> adjustVolume(true)
            else -> adjustVolume(false)
        }

        return AutomationResult(
            executed = true,
            feedbackMessage = feedback,
            actionType = "VOLUME"
        )
    }

    // --- 3. Connectivity (Wi-Fi & Bluetooth) ---
    private fun isConnectivityIntent(text: String): Boolean {
        val hasWifi = text.contains("wifi") || text.contains("ওয়াইফাই")
        val hasBt = text.contains("bluetooth") || text.contains("ব্লুটুথ")
        if (!hasWifi && !hasBt) return false

        val isExplicitAction = text.contains("turn on") || text.contains("turn off") ||
                text.contains("switch on") || text.contains("switch off") ||
                text.contains("open") || text.contains("খোলো") || text.contains("অন") ||
                text.contains("অফ") || text.contains("চালু") || text.contains("বন্ধ") ||
                text.contains("settings") || text.contains("সেটিংস") ||
                text == "wifi on" || text == "wifi off" || text == "bluetooth on" || text == "bluetooth off" ||
                text == "wifi" || text == "bluetooth" || text == "ওয়াইফাই" || text == "ব্লুটুথ"
        return isExplicitAction
    }

    fun openWifiSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openBluetoothSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun executeConnectivityCommand(input: String): AutomationResult {
        return if (input.contains("wifi") || input.contains("ওয়াইফাই")) {
            openWifiSettings()
            AutomationResult(
                executed = true,
                feedbackMessage = "📶 Opening Wi-Fi controls... / ওয়াইফাই সেটিংস খোলা হয়েছে।",
                actionType = "WIFI"
            )
        } else {
            openBluetoothSettings()
            AutomationResult(
                executed = true,
                feedbackMessage = "ᛒ Opening Bluetooth controls... / ব্লুটুথ সেটিংস খোলা হয়েছে।",
                actionType = "BLUETOOTH"
            )
        }
    }

    // --- 4. Phone Calls ---
    private fun isCallIntent(text: String): Boolean {
        if (text.contains("call by") || text.contains("callback") || text.contains("function call") ||
            text.contains("api call") || text.contains("system call") || text.contains("call stack") ||
            text.contains("recursive call") || text.contains("margin call") || text.contains("call of duty") ||
            text.contains("what") || text.contains("how") || text.contains("explain") || text.contains("কী") || text.contains("কি")) {
            return false
        }
        val isExplicitBengaliCall = text.contains("কল করো") || text.contains("ফোন করো") || text.contains("কল লাগাও") || text.contains("ফোন লাগাও")
        val isExplicitEnglishCall = (text.startsWith("call ") || text.startsWith("dial ")) &&
                !text.startsWith("call me") && !text.startsWith("call it") && !text.startsWith("call this")
        return isExplicitBengaliCall || isExplicitEnglishCall
    }

    @SuppressLint("MissingPermission")
    private fun executePhoneCall(input: String): AutomationResult {
        val numberMatcher = Pattern.compile("(\\+?[0-9]{3,14})").matcher(input)
        val target = if (numberMatcher.find()) {
            numberMatcher.group(1) ?: "100"
        } else {
            input.replace(Regex("(?i)^(call|dial|কল করো|ফোন করো)\\s*"), "").trim()
        }

        // Check if there is a message to speak
        val sayMatch = Regex("(?i)(?:বলো|বলবে|say|tell)\\s*[:,-]?\\s*(.+)").find(input)
        val spokenMsg = sayMatch?.groupValues?.get(1)?.trim()
        if (!spokenMsg.isNullOrBlank()) {
            val isBengali = spokenMsg.any { it in '\u0980'..'\u09FF' }
            val announcement = if (isBengali) "হ্যালো $target! আপনার জন্য বার্তা: $spokenMsg" else "Hello $target! Message: $spokenMsg"
            com.ayra.ai.app.voice.AyraVoiceAnnouncer.speakAloud(context, announcement)
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission && target.matches(Regex("\\+?[0-9]+"))) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$target")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$target")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        return try {
            context.startActivity(intent)
            val feedback = if (!spokenMsg.isNullOrBlank()) {
                "📞 Calling $target and speaking: \"$spokenMsg\" / $target কে কল করা হচ্ছে এবং বার্তাটি বলে দেওয়া হচ্ছে..."
            } else {
                "📞 Calling $target immediately... / $target কে কল করা হচ্ছে..."
            }
            AutomationResult(
                executed = true,
                feedbackMessage = feedback,
                actionType = "CALL_PHONE",
                extraData = target
            )
        } catch (e: Exception) {
            AutomationResult(
                executed = false,
                feedbackMessage = "Unable to start call: ${e.message}"
            )
        }
    }

    // --- 4b. Direct SMS Automation ---
    private fun isDirectSmsIntent(text: String): Boolean {
        if (text.contains("kafka") || text.contains("rabbitmq") || text.contains("socket") ||
            text.contains("queue") || text.contains("protocol") || text.contains("architecture") ||
            text.contains("what") || text.contains("how") || text.contains("explain") ||
            text.contains("code") || text.contains("example") || text.contains("কী") || text.contains("কি")) {
            return false
        }
        return (text.startsWith("sms to ") || text.startsWith("send sms to ") || text.startsWith("send message to ") ||
                text.contains("মেসেজ পাঠাও") || text.contains("এসএমএস পাঠাও") || text.contains("send text to"))
    }

    private fun executeDirectSms(input: String): AutomationResult {
        val numberMatcher = Pattern.compile("(\\+?[0-9]{5,14})").matcher(input)
        var targetPhone = if (numberMatcher.find()) numberMatcher.group(1) ?: "" else ""

        val msgMatch = Regex("(?i)(?:saying|message|বার্তা|যে)\\s*[:,-]?\\s*(.+)").find(input)
        val messageText = msgMatch?.groupValues?.get(1)?.trim() ?: "Hello from Ayra AI!"

        val hasSmsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasSmsPerm && targetPhone.isNotBlank()) {
            try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    context.getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(messageText)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(targetPhone, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(targetPhone, null, messageText, null, null)
                }
                AutomationResult(
                    executed = true,
                    feedbackMessage = "✉️ SMS Sent directly to $targetPhone: \"$messageText\"! / $targetPhone নম্বরে সফলভাবে মেসেজ পাঠানো হয়েছে!",
                    actionType = "SMS",
                    extraData = targetPhone
                )
            } catch (e: Exception) {
                openSmsComposer(targetPhone, messageText)
            }
        } else {
            openSmsComposer(targetPhone, messageText)
        }
    }

    private fun openSmsComposer(targetPhone: String, messageText: String): AutomationResult {
        return try {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${if (targetPhone.matches(Regex("\\+?[0-9]+"))) targetPhone else ""}")).apply {
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            AutomationResult(
                executed = true,
                feedbackMessage = "✉️ Opening SMS Composer with prefilled message: \"$messageText\" / মেসেজ প্রেরক উইন্ডো খোলা হয়েছে।",
                actionType = "SMS",
                extraData = targetPhone
            )
        } catch (e: Exception) {
            AutomationResult(executed = false, feedbackMessage = "Failed to launch SMS: ${e.message}")
        }
    }

    // --- 5. Alarm ---
    private fun isAlarmIntent(text: String): Boolean {
        if (text.contains("সূর্য") || text.contains("sun") || text.contains("explain") ||
            text.contains("কি") || text.contains("কী") || text.contains("why") ||
            text.contains("what") || text.contains("how") || text.contains("ইতিহাস") ||
            text.contains("history") || text.contains("invent") || text.contains("circuit")) {
            return false
        }
        return text.contains("alarm for") || text.contains("set alarm") || text.contains("set an alarm") ||
                text.contains("wake me up") || text.contains("wake me at") ||
                text.contains("এলার্ম সেট") || text.contains("এলার্ম দাও") || text.contains("এলার্ম লাগাও") ||
                text.contains("আমাকে জাগাও") ||
                (text.contains("এলার্ম") && (text.contains("টায়") || text.contains("pm") || text.contains("am") || text.contains("রাত") || text.contains("সকাল")))
    }

    fun openAlarm(): Boolean {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun executeSetAlarm(input: String): AutomationResult {
        var hour = 7
        var minute = 0
        var isPm = false

        val lower = input.lowercase()
        if (lower.contains("pm") || lower.contains("রাত") || lower.contains("সন্ধ্যা")) {
            isPm = true
        }

        val timeMatcher = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(input)
        if (timeMatcher.find()) {
            hour = timeMatcher.group(1)?.toIntOrNull() ?: 7
            minute = timeMatcher.group(2)?.toIntOrNull() ?: 0
        } else {
            val singleHourMatcher = Pattern.compile("(\\d{1,2})\\s*(?:am|pm|টা|baje|o'clock)?", Pattern.CASE_INSENSITIVE).matcher(input)
            if (singleHourMatcher.find()) {
                hour = singleHourMatcher.group(1)?.toIntOrNull() ?: 7
            }
        }

        if (isPm && hour < 12) {
            hour += 12
        }

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Ayra Assistant Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            val formatted = String.format("%02d:%02d", hour, minute)
            AutomationResult(
                executed = true,
                feedbackMessage = "⏰ Alarm set for $formatted! / $formatted এ এলার্ম সক্রিয় করা হয়েছে।",
                actionType = "SET_ALARM",
                extraData = formatted
            )
        } catch (e: Exception) {
            AutomationResult(
                executed = false,
                feedbackMessage = "Failed to set alarm: ${e.message}"
            )
        }
    }

    // --- 6. Camera ---
    private fun isCameraIntent(text: String): Boolean {
        if (text.contains("banao") || text.contains("বানাও") || text.contains("generate") ||
            text.contains("create") || text.contains("draw") || text.contains("image") ||
            text.contains("photo") || text.contains("3d") || text.contains("video") ||
            text.contains("কী") || text.contains("how")) {
            return false
        }
        return text.startsWith("open camera") || text.startsWith("launch camera") ||
                text.contains("ক্যামেরা খোলো") || text.contains("ক্যামেরা অন") ||
                text == "camera" || text == "ক্যামেরা"
    }

    fun openCamera(): Boolean {
        return try {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(cameraIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun executeCamera(): AutomationResult {
        val ok = openCamera()
        return AutomationResult(
            executed = ok,
            feedbackMessage = if (ok) "📸 Camera opened! / ক্যামেরা চালু করা হয়েছে।" else "Could not launch camera.",
            actionType = "OPEN_APP",
            extraData = "Camera"
        )
    }

    // --- 7. Open Apps ---
    private fun isOpenAppIntent(text: String): Boolean {
        if (text.contains("how to") || text.contains("কীভাবে") || text.contains("কেন") ||
            text.contains("কী") || text.contains("what is") || text.contains("explain") ||
            text.contains("banao") || text.contains("বানাও") || text.contains("generate") ||
            text.contains("create") || text.contains("draw") || text.contains("write") ||
            text.contains("code") || text.contains("photo") || text.contains("video") ||
            text.contains("song") || text.contains("3d") || text.contains("গান")) {
            return false
        }
        val t = text.trim()
        val startsWithOpen = t.startsWith("open ") || t.startsWith("launch ") ||
                t.startsWith("খুলুন ") || t.startsWith("খোল ") || t.startsWith("চালু করো ") ||
                t.startsWith("chalu karo ") || t.endsWith("খুলুন") || t.endsWith("খোলো")
        val isDirectApp = t == "youtube" || t == "facebook" || t == "whatsapp" ||
                t == "instagram" || t == "spotify" || t == "settings" || t == "settings kholo"
        return startsWithOpen || isDirectApp
    }

    fun openSettings(): Boolean {
        return try {
            val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun executeOpenApp(input: String): AutomationResult? {
        val appName = input
            .replace(Regex("(?i)^(please\\s+)?(open|launch|start|খুলুন|খোল|চালু করো|chalu karo)\\s*"), "")
            .replace(Regex("(?i)\\s+(app|application|ta|khul|kholo)$"), "")
            .trim()

        if (appName.isBlank() || appName.length < 2) return null

        val pm = context.packageManager

        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "facebook" to "com.facebook.katana",
            "instagram" to "com.instagram.android",
            "whatsapp" to "com.whatsapp",
            "spotify" to "com.spotify.music",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm"
        )

        val webFallbacks = mapOf(
            "youtube" to "https://www.youtube.com",
            "facebook" to "https://www.facebook.com",
            "instagram" to "https://www.instagram.com",
            "whatsapp" to "https://web.whatsapp.com",
            "spotify" to "https://open.spotify.com",
            "chrome" to "https://www.google.com",
            "maps" to "https://maps.google.com"
        )

        // Camera
        if (appName.contains("camera") || appName.contains("ক্যামেরা")) {
            return executeCamera()
        }

        // Settings
        if (appName.contains("settings") || appName.contains("সেটিংস")) {
            openSettings()
            return AutomationResult(
                executed = true,
                feedbackMessage = "⚙️ Settings opened! / সেটিংস খোলা হয়েছে।",
                actionType = "OPEN_APP",
                extraData = "Settings"
            )
        }

        // Check known packages
        for ((key, pkg) in knownPackages) {
            if (appName.contains(key)) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return AutomationResult(
                        executed = true,
                        feedbackMessage = "🚀 Opening ${key.replaceFirstChar { it.uppercase() }}... / ${key.replaceFirstChar { it.uppercase() }} খোলা হচ্ছে...",
                        actionType = "OPEN_APP",
                        extraData = key
                    )
                } else {
                    val fallbackUrl = webFallbacks[key] ?: "https://www.google.com/search?q=$key"
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(webIntent)
                    return AutomationResult(
                        executed = true,
                        feedbackMessage = "🚀 Launching ${key.replaceFirstChar { it.uppercase() }}... / ${key.replaceFirstChar { it.uppercase() }} ওপেন করা হয়েছে...",
                        actionType = "OPEN_APP",
                        extraData = key
                    )
                }
            }
        }

        // Search installed packages
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                if (label.contains(appName) || appName.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return AutomationResult(
                            executed = true,
                            feedbackMessage = "🚀 Opening ${pm.getApplicationLabel(appInfo)}... / অ্যাপটি খোলা হয়েছে...",
                            actionType = "OPEN_APP",
                            extraData = appInfo.packageName
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        // If not a recognized app, return null so AI engine can answer the prompt!
        return null
    }

    // --- 8. Location ---
    private fun isLocationIntent(text: String): Boolean {
        if (text.contains("where is") || text.contains("location of") || text.contains("how gps") ||
            text.contains("satellite") || text.contains("explain") || text.contains("what is") ||
            text.contains("কোথায় অবস্থিত") || text.contains("ইতিহাস") || text.contains("কী")) {
            return false
        }
        return text.contains("where am i") || text.contains("my location") || text.contains("what is my location") ||
                text.contains("current location") || text.contains("kothay achi") || text.contains("আমি কোথায়") ||
                text.contains("আমি কোথায় আছি") || text.contains("আমার লোকেশন") || text.contains("বর্তমান লোকেশন")
    }

    @SuppressLint("MissingPermission")
    private fun executeGetLocation(): AutomationResult {
        val hasLoc = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasLoc) {
            return AutomationResult(
                executed = false,
                feedbackMessage = "Location permission is required to detect GPS coordinates. / জিপিএস লোকেশন দেখার জন্য লোকেশন পারমিশন প্রয়োজন।"
            )
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val loc: Location? = try {
            lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            null
        }

        return if (loc != null) {
            val locStr = "Latitude: ${loc.latitude}, Longitude: ${loc.longitude} (Accuracy: ±${loc.accuracy.toInt()}m)"
            AutomationResult(
                executed = true,
                feedbackMessage = "📍 Your current device location: $locStr\nআপনি বর্তমানে এই কোঅর্ডিনেটসে অবস্থান করছেন।",
                actionType = "LOCATION",
                extraData = "${loc.latitude},${loc.longitude}"
            )
        } else {
            AutomationResult(
                executed = false,
                feedbackMessage = "📍 Unable to acquire GPS lock. Please check if device GPS / Location services are enabled in Settings. / বর্তমানে জিপিএস লোকেশন পাওয়া যায়নি, দয়া করে ডিভাইসের লোকেশন সার্ভিস অন আছে কিনা চেক করুন।"
            )
        }
    }

    // --- 9. Cyber Shield & VPN Control ---
    private fun isVpnActionIntent(text: String): Boolean {
        if (text.startsWith("what is vpn") || text.startsWith("explain vpn") || text.contains("ভিপিএন কি") || text.contains("কাকে বলে")) {
            return false
        }
        val hasVpnWord = text.contains("vpn") || text.contains("ভিপিএন") || text.contains("cyber shield") ||
                text.contains("privacy shield") || text.contains("আইপি") || text.contains("ip address") ||
                text.contains("my ip")
        if (!hasVpnWord) return false

        return text.contains("on") || text.contains("অন") || text.contains("connect") || text.contains("কানেক্ট") ||
                text.contains("off") || text.contains("অফ") || text.contains("disconnect") || text.contains("বন্ধ") ||
                text.contains("status") || text.contains("check") || text.contains("কত") || text.contains("switch") ||
                text.contains("change") || text.contains("পাল্টাও") || text.contains("toggle") || text == "vpn" || text == "ভিপিএন"
    }

    private fun executeVpnCommand(text: String): AutomationResult {
        AyraVpnPrivacyController.initialize(context)
        val isBengali = text.any { it in '\u0980'..'\u09FF' }

        // Connect / Turn on
        val isConnect = text.contains("on") || text.contains("অন") || text.contains("connect") || text.contains("কানেক্ট") || text.contains("chalu") || text.contains("চালু")
        val isDisconnect = text.contains("off") || text.contains("অফ") || text.contains("disconnect") || text.contains("বন্ধ") || text.contains("bondho")

        return AutomationResult(
            executed = false,
            feedbackMessage = if (isBengali) "🛡️ ভিপিএন সার্ভিস প্রাইভেসি পলিসির কারণে নিষ্ক্রিয় রাখা হয়েছে।"
            else "🛡️ VPN tunnel service is decommissioned in accordance with Google Play privacy guidelines.",
            actionType = "VPN_DECOMMISSIONED"
        )

        val summary = AyraVpnPrivacyController.formatStatusSummary(isBengali)
        return AutomationResult(
            executed = true,
            feedbackMessage = summary,
            actionType = "VPN_STATUS",
            extraData = if (AyraVpnPrivacyController.vpnStatus.value.isConnected) "CONNECTED" else "DISCONNECTED"
        )
    }
}
