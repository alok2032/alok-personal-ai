package com.ayra.ai.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_tasks")
data class ScheduledTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetApp: String, // "WhatsApp", "Facebook", "Instagram", "SMS", "Alarm", "Routine"
    val recipient: String,
    val messageText: String,
    val scheduledEpochMillis: Long,
    val formattedTime: String,
    val status: String = "PENDING", // "PENDING", "COMPLETED", "CANCELLED"
    val taskType: String = "MESSAGE", // "CALL", "SMS", "WHATSAPP", "REMINDER"
    val phoneNumber: String = "",
    val speakOnCall: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
