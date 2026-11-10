package com.ayra.ai.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachedImagePath: String? = null,
    val craftType: String? = null, // "IMAGE", "SONG", "3D", "LOCATION", "ACTION"
    val craftPayload: String? = null,
    val actionCommand: String? = null
)
