package com.ayra.ai.app.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ayra.ai.app.data.ChatDatabase
import com.ayra.ai.app.data.ScheduledTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class AyraTaskScheduler(private val context: Context) {

    /**
     * Checks if user prompt is asking to schedule:
     * 1. A scheduled Call with spoken voice message ("call rahul at 5 pm and say...", "কাল ১০টায় বাবাকে কল করে বলো...")
     * 2. A scheduled SMS / Message ("send message to ... at ...", "মেসেজ পাঠাও...", "উইশ করো...")
     * 3. A scheduled Reminder ("remind me in 10 minutes to...", "মনে করিয়ে দিও...", "রিমাইন্ডার...")
     */
    fun isSchedulingIntent(userInput: String): Boolean {
        if (DeviceAutomationController.isEducationalOrInformationalQuery(userInput)) {
            return false
        }

        val lower = userInput.lowercase()

        val hasTime = hasTimeSignal(lower)
        if (!hasTime) return false

        val isCallSchedule = (lower.contains("schedule a call") || lower.contains("schedule call") ||
                ((lower.startsWith("call ") || lower.contains("কল করো")) && (lower.contains("বলবে") || lower.contains("বলো") || lower.contains("say") || lower.contains("tell"))))

        val isReminderSchedule = lower.contains("remind me") || lower.contains("মনে করিয়ে দাও") ||
                lower.contains("মনে করিয়ে দিও") || lower.contains("set reminder") || lower.contains("রিমাইন্ডার দাও")

        val isMessageSchedule = (lower.contains("schedule a message") || lower.contains("schedule message") || lower.contains("schedule sms") ||
                lower.contains("send message to") || lower.contains("send sms to") || lower.contains("উইশ করো") ||
                lower.contains("wish happy birthday") || lower.contains("মেসেজ পাঠাও"))

        return isCallSchedule || isReminderSchedule || isMessageSchedule
    }

    private fun hasTimeSignal(lower: String): Boolean {
        val hasClockTime = Regex("\\b\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)\\b", RegexOption.IGNORE_CASE).containsMatchIn(lower) ||
                Regex("\\b\\d{1,2}\\s*টায়\\b").containsMatchIn(lower) ||
                Regex("\\b\\d{1,2}\\s*baje\\b").containsMatchIn(lower) ||
                Regex("\\b\\d{1,2}\\s*o'clock\\b", RegexOption.IGNORE_CASE).containsMatchIn(lower)

        val hasRelative = Regex("\\b\\d+\\s*(?:মিনিট|minute|minutes|min|mins|ঘণ্টা|ঘন্টা|hour|hours)(?:\\s*পর|\\s*later)?\\b").containsMatchIn(lower)

        val hasDayPeriod = lower.contains("রাত") || lower.contains("সকাল") ||
                lower.contains("দুপুর") || lower.contains("বিকাল") || lower.contains("সন্ধ্যা") ||
                lower.contains("tomorrow") || lower.contains("কাল ") || lower.contains("কালকে")

        return hasClockTime || hasRelative || hasDayPeriod
    }

    suspend fun scheduleTaskFromPrompt(userInput: String): AutomationResult = withContext(Dispatchers.IO) {
        val lower = userInput.lowercase()
        val isBengali = userInput.any { it in '\u0980'..'\u09FF' }

        // 1. Determine Task Category: CALL, REMINDER, or MESSAGE
        val isCall = (lower.contains("call") || lower.contains("কল") || lower.contains("ফোন")) &&
                (lower.contains("বল") || lower.contains("say") || lower.contains("tell") || lower.contains("কথা") || !lower.contains("মেসেজ"))

        val isReminder = lower.contains("remind") || lower.contains("মনে করিয়ে") || lower.contains("মনে করো") ||
                lower.contains("মনে করিয়ে") || lower.contains("রিমাইন্ড") || lower.contains("reminder")

        // 2. Parse Execution Time
        val (epochMillis, formattedTime) = parseTime(userInput)

        // 3. Handle by Category
        if (isCall) {
            return@withContext scheduleCallTask(userInput, lower, isBengali, epochMillis, formattedTime)
        } else if (isReminder) {
            return@withContext scheduleReminderTask(userInput, lower, isBengali, epochMillis, formattedTime)
        } else {
            return@withContext scheduleMessageTask(userInput, lower, isBengali, epochMillis, formattedTime)
        }
    }

    // --- A. SCHEDULED CALL TASK ---
    private suspend fun scheduleCallTask(
        userInput: String,
        lower: String,
        isBengali: Boolean,
        epochMillis: Long,
        formattedTime: String
    ): AutomationResult {
        // Extract Recipient / Phone Number
        var recipient = extractRecipient(userInput)
        val phoneNumber = extractPhoneNumber(userInput)
        if (phoneNumber.isNotBlank() && (recipient == "Friend" || recipient.isBlank())) {
            recipient = phoneNumber
        }

        // Extract message to speak during call
        var messageToSpeak = extractSpokenMessage(userInput)
        if (messageToSpeak.isBlank()) {
            messageToSpeak = if (isBengali) {
                "হ্যালো $recipient! এটি আয়রা এআই থেকে আপনার জন্য শিডিউল করা ভয়েস কল। আশা করি আপনি ভালো আছেন!"
            } else {
                "Hello $recipient! This is a scheduled voice call from Ayra AI. Wishing you a great day!"
            }
        }

        val db = ChatDatabase.getDatabase(context)
        val task = ScheduledTask(
            title = "Scheduled Call to $recipient",
            targetApp = "CALL",
            recipient = recipient,
            messageText = messageToSpeak,
            scheduledEpochMillis = epochMillis,
            formattedTime = formattedTime,
            status = "PENDING",
            taskType = "CALL",
            phoneNumber = phoneNumber,
            speakOnCall = true
        )
        val taskId = db.scheduledTaskDao().insertTask(task)

        armAlarmManager(taskId, "CALL", recipient, messageToSpeak, phoneNumber, epochMillis)

        val extraJson = JSONObject().apply {
            put("taskId", taskId)
            put("taskType", "CALL")
            put("recipient", recipient)
            put("phone", phoneNumber)
            put("time", formattedTime)
            put("spokenMessage", messageToSpeak)
        }.toString()

        val reply = if (isBengali) {
            "📞 **ভয়েস কল ও বার্তা শিডিউল সম্পন্ন হয়েছে!**\n\n" +
                    "• যাকে কল করা হবে: **$recipient** ${if (phoneNumber.isNotBlank()) "($phoneNumber)" else ""}\n" +
                    "• নির্ধারিত সময়: **$formattedTime**\n" +
                    "• কলে যে কথাটা বলে দেবে: *\"$messageToSpeak\"*\n\n" +
                    "✅ নির্ধারিত সময়ে আয়রা স্বয়ংক্রিয়ভাবে কল ডায়াল করবে এবং স্পিকারের মাধ্যমে আপনার বার্তাটি তাকে স্পষ্ট কণ্ঠে বলে দেবে! 🎙️"
        } else {
            "📞 **Scheduled Voice Call Armed!**\n\n" +
                    "• Recipient: **$recipient** ${if (phoneNumber.isNotBlank()) "($phoneNumber)" else ""}\n" +
                    "• Scheduled Time: **$formattedTime**\n" +
                    "• Message to Speak: *\"$messageToSpeak\"*\n\n" +
                    "✅ At the exact time, Ayra will initiate the phone call and speak your message aloud through automated speech synthesis! 🎙️"
        }

        return AutomationResult(
            executed = true,
            feedbackMessage = reply,
            actionType = "SCHEDULED_CALL",
            extraData = extraJson
        )
    }

    // --- B. SCHEDULED REMINDER TASK ---
    private suspend fun scheduleReminderTask(
        userInput: String,
        lower: String,
        isBengali: Boolean,
        epochMillis: Long,
        formattedTime: String
    ): AutomationResult {
        var reminderText = extractReminderText(userInput)
        if (reminderText.isBlank()) {
            reminderText = if (isBengali) "আপনার নির্ধারিত গুরুত্বপূর্ণ কাজটি সম্পন্ন করুন" else "Complete your scheduled task"
        }

        val db = ChatDatabase.getDatabase(context)
        val task = ScheduledTask(
            title = "Reminder: $reminderText",
            targetApp = "REMINDER",
            recipient = "You",
            messageText = reminderText,
            scheduledEpochMillis = epochMillis,
            formattedTime = formattedTime,
            status = "PENDING",
            taskType = "REMINDER"
        )
        val taskId = db.scheduledTaskDao().insertTask(task)

        armAlarmManager(taskId, "REMINDER", "You", reminderText, "", epochMillis)

        val extraJson = JSONObject().apply {
            put("taskId", taskId)
            put("taskType", "REMINDER")
            put("time", formattedTime)
            put("reminder", reminderText)
        }.toString()

        val reply = if (isBengali) {
            "⏰ **স্মার্ট রিমাইন্ডার সেট করা হয়েছে!**\n\n" +
                    "• কাজের বিবরণ: **$reminderText**\n" +
                    "• মনে করিয়ে দেওয়ার সময়: **$formattedTime**\n\n" +
                    "🔔 নির্ধারিত সময়ে মোবাইল লক বা স্লিপে থাকলেও রিংটোন বাজিয়ে এবং ভয়েসে বলে আয়রা আপনাকে এই কাজটি মনে করিয়ে দেবে! ⚡"
        } else {
            "⏰ **Smart Reminder Armed!**\n\n" +
                    "• Task: **$reminderText**\n" +
                    "• Reminder Time: **$formattedTime**\n\n" +
                    "🔔 Even if the screen is off or phone is sleeping, Ayra will wake up, chime the alarm, and speak this reminder to you! ⚡"
        }

        return AutomationResult(
            executed = true,
            feedbackMessage = reply,
            actionType = "SCHEDULED_REMINDER",
            extraData = extraJson
        )
    }

    // --- C. SCHEDULED MESSAGE / SMS TASK ---
    private suspend fun scheduleMessageTask(
        userInput: String,
        lower: String,
        isBengali: Boolean,
        epochMillis: Long,
        formattedTime: String
    ): AutomationResult {
        var targetApp = when {
            lower.contains("whatsapp") || lower.contains("হোয়াটসঅ্যাপ") -> "WhatsApp"
            lower.contains("sms") || lower.contains("এসএমএস") -> "SMS"
            lower.contains("facebook") || lower.contains("ফেসবুক") -> "Facebook"
            lower.contains("instagram") || lower.contains("ইনস্টাগ্রাম") -> "Instagram"
            else -> if (extractPhoneNumber(userInput).isNotBlank()) "SMS" else "WhatsApp"
        }

        var recipient = extractRecipient(userInput)
        val phoneNumber = extractPhoneNumber(userInput)
        if (phoneNumber.isNotBlank() && (recipient == "Friend" || recipient.isBlank())) {
            recipient = phoneNumber
        }

        // Extract message or build personalized greeting
        val isBirthday = lower.contains("birthday") || lower.contains("জন্মদিন") || lower.contains("উইশ")
        var messageText = extractSpokenMessage(userInput)
        if (messageText.isBlank()) {
            messageText = if (isBirthday) {
                if (isBengali) {
                    "শুভ জন্মদিন $recipient! 🎉🎂 তোমার আগামী দিনগুলো অফুরন্ত আনন্দ ও ভালোবাসায় ভরে উঠুক! — Ayra AI মারফত আন্তরিক শুভেচ্ছা ❤️"
                } else {
                    "Happy Birthday $recipient! 🎉🎂 Wishing you joy, success and boundless happiness! — via Ayra AI ❤️"
                }
            } else {
                if (isBengali) "প্রিয় $recipient, এটি আয়রা এআই থেকে আপনার নির্ধারিত বার্তা। দিনটি শুভ হোক! ✨"
                else "Hello $recipient! This is your scheduled message from Ayra AI. Have a great day! ✨"
            }
        }

        val db = ChatDatabase.getDatabase(context)
        val task = ScheduledTask(
            title = if (isBirthday) "Birthday Wish to $recipient" else "Scheduled Message to $recipient",
            targetApp = targetApp,
            recipient = recipient,
            messageText = messageText,
            scheduledEpochMillis = epochMillis,
            formattedTime = formattedTime,
            status = "PENDING",
            taskType = if (targetApp == "SMS") "SMS" else "WHATSAPP",
            phoneNumber = phoneNumber
        )
        val taskId = db.scheduledTaskDao().insertTask(task)

        armAlarmManager(taskId, targetApp, recipient, messageText, phoneNumber, epochMillis)

        val extraJson = JSONObject().apply {
            put("taskId", taskId)
            put("taskType", "MESSAGE")
            put("targetApp", targetApp)
            put("recipient", recipient)
            put("phone", phoneNumber)
            put("time", formattedTime)
            put("message", messageText)
        }.toString()

        val reply = if (isBengali) {
            "✉️ **স্বয়ংক্রিয় মেসেজ শিডিউল সম্পন্ন হয়েছে!**\n\n" +
                    "• প্রাপক: **$recipient** ${if (phoneNumber.isNotBlank()) "($phoneNumber)" else ""}\n" +
                    "• মাধ্যম: **$targetApp** (নেট অফ থাকলে সরাসরি অফলাইন SMS ব্যাকআপ সক্রিয়)\n" +
                    "• নির্ধারিত সময়: **$formattedTime**\n" +
                    "• মেসেজ: *\"$messageText\"*\n\n" +
                    "✅ নির্ধারিত সময়ে নেটওয়ার্ক বন্ধ থাকলেও অফলাইন SMS বা ওয়াইফাই অন করে বার্তাটি সঠিকভাবে পৌঁছে দেওয়া হবে! 🚀"
        } else {
            "✉️ **Scheduled Message Armed!**\n\n" +
                    "• Recipient: **$recipient** ${if (phoneNumber.isNotBlank()) "($phoneNumber)" else ""}\n" +
                    "• Channel: **$targetApp** (Offline Direct SMS Fallback Active)\n" +
                    "• Scheduled Time: **$formattedTime**\n" +
                    "• Message: *\"$messageText\"*\n\n" +
                    "✅ Even if internet is disconnected, direct SMS delivery guarantees this message reaches on time! 🚀"
        }

        return AutomationResult(
            executed = true,
            feedbackMessage = reply,
            actionType = "SCHEDULED_MESSAGE",
            extraData = extraJson
        )
    }

    suspend fun rearmAllPendingTasks() = withContext(Dispatchers.IO) {
        val db = ChatDatabase.getDatabase(context)
        val pendingList = db.scheduledTaskDao().getPendingTasksSync()
        val now = System.currentTimeMillis()

        for (task in pendingList) {
            if (task.scheduledEpochMillis > now) {
                armAlarmManager(
                    task.id,
                    task.targetApp,
                    task.recipient,
                    task.messageText,
                    task.phoneNumber,
                    task.scheduledEpochMillis
                )
            } else {
                db.scheduledTaskDao().updateStatus(task.id, "MISSED")
            }
        }
    }

    // --- Helper Methods ---
    fun armAlarmManager(
        taskId: Long,
        targetApp: String,
        recipient: String,
        messageText: String,
        phoneNumber: String,
        epochMillis: Long
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ScheduledTaskReceiver::class.java).apply {
                action = "com.ayra.ai.app.ACTION_EXECUTE_SCHEDULED_TASK"
                putExtra("TASK_ID", taskId)
                putExtra("TARGET_APP", targetApp)
                putExtra("RECIPIENT", recipient)
                putExtra("MESSAGE_TEXT", messageText)
                putExtra("PHONE_NUMBER", phoneNumber)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        epochMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        epochMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    epochMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskScheduler", "Error arming alarm for taskId=$taskId", e)
        }
    }

    /**
     * Cancels an armed alarm from AlarmManager when a task is deleted or finished.
     */
    fun cancelAlarm(taskId: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ScheduledTaskReceiver::class.java).apply {
                action = "com.ayra.ai.app.ACTION_EXECUTE_SCHEDULED_TASK"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                android.util.Log.i("TaskScheduler", "Cancelled AlarmManager PendingIntent for taskId=$taskId")
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskScheduler", "Error cancelling alarm for taskId=$taskId", e)
        }
    }

    private fun convertBengaliDigits(input: String): String {
        val bengaliDigits = "০১২৩৪৫৬৭৮৯"
        val builder = StringBuilder()
        for (ch in input) {
            val idx = bengaliDigits.indexOf(ch)
            if (idx != -1) {
                builder.append(idx)
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    private fun parseTime(input: String): Pair<Long, String> {
        val normalized = convertBengaliDigits(input).lowercase()
        val cal = Calendar.getInstance()
        val nowMillis = cal.timeInMillis

        // 1. Relative time (e.g., "10 মিনিট পর", "in 15 minutes", "after 1 hour")
        val relativeMinMatch = Regex("(\\d+)\\s*(?:মিনিট|minute|min|mins)(?:\\s*পর|\\s*later)?").find(normalized)
        if (relativeMinMatch != null) {
            val mins = relativeMinMatch.groupValues[1].toIntOrNull() ?: 10
            cal.add(Calendar.MINUTE, mins)
            val epoch = cal.timeInMillis
            val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
            return Pair(epoch, sdf.format(Date(epoch)))
        }

        val relativeHourMatch = Regex("(\\d+)\\s*(?:ঘণ্টা|ঘন্টা|hour|hours)(?:\\s*পর|\\s*later)?").find(normalized)
        if (relativeHourMatch != null) {
            val hrs = relativeHourMatch.groupValues[1].toIntOrNull() ?: 1
            cal.add(Calendar.HOUR_OF_DAY, hrs)
            val epoch = cal.timeInMillis
            val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
            return Pair(epoch, sdf.format(Date(epoch)))
        }

        // 2. Absolute time (Clock time)
        var targetHour = 0
        var targetMinute = 0
        var isSpecificTime = false

        if (normalized.contains("রাত ১২") || normalized.contains("12 am") || normalized.contains("midnight") || normalized.contains("রাত 12")) {
            targetHour = 0
            targetMinute = 0
            isSpecificTime = true
        } else {
            val timeRegex = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|টা|টা বাজে|baje)?").find(normalized)
            if (timeRegex != null) {
                val h = timeRegex.groupValues[1].toIntOrNull() ?: 0
                val m = timeRegex.groupValues[2].toIntOrNull() ?: 0
                val tag = timeRegex.groupValues[3]
                val isPm = tag == "pm" || normalized.contains("রাত") || normalized.contains("সন্ধ্যা") || normalized.contains("বিকাল") || normalized.contains("দুপুর")

                targetHour = when {
                    isPm && h < 12 -> h + 12
                    !isPm && h == 12 -> 0
                    else -> h
                }
                targetMinute = m
                isSpecificTime = true
            }
        }

        if (isSpecificTime) {
            cal.set(Calendar.HOUR_OF_DAY, targetHour)
            cal.set(Calendar.MINUTE, targetMinute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val isTomorrow = normalized.contains("কাল") || normalized.contains("tomorrow")
            if (isTomorrow || cal.timeInMillis <= nowMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Default 15 minutes ahead
            cal.add(Calendar.MINUTE, 15)
        }

        val epoch = cal.timeInMillis
        val sdf = SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault())
        return Pair(epoch, sdf.format(Date(epoch)))
    }

    private fun extractRecipient(input: String): String {
        // Match word before "কে" (Bengali)
        val bengaliMatch = Regex("([a-zA-Z\u0980-\u09FF0-9+]+)কে").find(input)
        if (bengaliMatch != null) {
            val candidate = bengaliMatch.groupValues[1].trim()
            if (candidate.length in 2..20 && candidate != "আমাকে" && candidate != "কাউ" && candidate != "কাউকে") {
                return candidate
            }
        }

        // Match after "to" or "call"
        val englishMatch = Regex("(?i)(?:to|call)\\s+([a-zA-Z\u0980-\u09FF0-9+]+)").find(input)
        if (englishMatch != null) {
            val candidate = englishMatch.groupValues[1].trim()
            if (candidate.length in 2..20 && !candidate.equals("me", ignoreCase = true) && !candidate.equals("tomorrow", ignoreCase = true)) {
                return candidate
            }
        }

        return "Friend"
    }

    private fun extractPhoneNumber(input: String): String {
        val matcher = Pattern.compile("(\\+?[0-9]{10,14})").matcher(input)
        return if (matcher.find()) matcher.group(1) ?: "" else ""
    }

    private fun extractSpokenMessage(input: String): String {
        // Bengali pattern: "বলো <কথা>" / "বলবে <কথা>"
        val sayMatch = Regex("(?i)(?:বলো|বলবে|বলতে|say|tell(?:\\s+him|\\s+her)?|saying|message|বার্তা)\\s*[:,-]?\\s*(.+)").find(input)
        if (sayMatch != null) {
            var msg = sayMatch.groupValues[1].trim()
            // Clean ending words like "বলে দেবে", "পাঠাবে"
            msg = msg.replace(Regex("(?i)\\s*(বলে\\s*দেবে|বলে\\s*দিও|পাঠিয়ে\\s*দিও|পাঠাবে)$"), "").trim()
            if (msg.isNotBlank()) return msg
        }
        return ""
    }

    private fun extractReminderText(input: String): String {
        val remMatch = Regex("(?i)(?:মনে\\s*করিয়ে\\s*দিও|মনে\\s*করিয়ে\\s*দাও|মনে\\s*করো|remind\\s*me\\s*(?:to|for)?)\\s*(.+)").find(input)
        if (remMatch != null) {
            var rem = remMatch.groupValues[1].trim()
            // Remove time references
            rem = rem.replace(Regex("(?i)\\s*(কাল|আজ|রাত|সকাল|দুপুর|বিকাল|\\d{1,2}\\s*টায়|at\\s*\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?).*$"), "").trim()
            if (rem.isNotBlank()) return rem
        }
        return input.replace(Regex("(?i)(remind me|আমাকে মনে করিয়ে দিও|মনে করিয়ে দাও)"), "").trim()
    }
}
