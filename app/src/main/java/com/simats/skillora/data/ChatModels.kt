package com.simats.skillora.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Chat(
    val chatId: String = "",
    val skillId: String = "",
    val skillTitle: String = "",
    val studentId: String = "",
    val publisherId: String = "",
    val lastMessage: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    val studentUnreadCount: Int = 0,
    val publisherUnreadCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    // Supplement information for UI
    val otherParticipantName: String = "",
    val otherParticipantPhotoUrl: String? = null
)

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    @ServerTimestamp val sentAt: Timestamp? = null,
    val isRead: Boolean = false
)
