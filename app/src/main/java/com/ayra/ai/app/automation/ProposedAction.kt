package com.ayra.ai.app.automation

sealed class ProposedAction {
    abstract val title: String
    abstract val description: String
    abstract val isCritical: Boolean

    data class DirectCall(
        val displayName: String,
        val phoneNumber: String,
        val spokenMessage: String? = null
    ) : ProposedAction() {
        override val title: String = "Initiate Phone Call"
        override val description: String = "Open dialer for $displayName ($phoneNumber)${if (!spokenMessage.isNullOrBlank()) "\nSpoken note: \"$spokenMessage\"" else ""}"
        override val isCritical: Boolean = true
    }

    data class SendSms(
        val recipient: String,
        val phoneNumber: String,
        val messageBody: String
    ) : ProposedAction() {
        override val title: String = "Compose SMS"
        override val description: String = "Open SMS composer to $recipient ($phoneNumber) with text:\n\"$messageBody\""
        override val isCritical: Boolean = true
    }

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val label: String
    ) : ProposedAction() {
        override val title: String = "Set Alarm"
        val formattedTime: String = String.format("%02d:%02d", hour, minute)
        override val description: String = "Set device alarm for $formattedTime (Label: \"$label\")"
        override val isCritical: Boolean = false
    }

    data class OpenApp(
        val appName: String,
        val packageName: String?
    ) : ProposedAction() {
        override val title: String = "Launch Application"
        override val description: String = "Open app: $appName"
        override val isCritical: Boolean = false
    }

    data class ToggleTorch(
        val enable: Boolean
    ) : ProposedAction() {
        override val title: String = "Flashlight"
        override val description: String = if (enable) "Turn flashlight ON" else "Turn flashlight OFF"
        override val isCritical: Boolean = false
    }

    data class AdjustVolume(
        val level: String
    ) : ProposedAction() {
        override val title: String = "Adjust Volume"
        override val description: String = "Set media volume level: $level"
        override val isCritical: Boolean = false
    }
}
