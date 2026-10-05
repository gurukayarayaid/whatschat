package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType

/**
 * ConversationEntity: Local cached representation of a chat conversation.
 * Optimized with compound indexing for sorting by pinned state and latest activity timestamp.
 */
@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["isPinned", "lastTimestamp"]),
        Index(value = ["lastTimestamp"])
    ]
)
data class ConversationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarColor: Long,
    val isGroup: Boolean,
    val participantCount: Int,
    val maxParticipants: Int,
    val unreadCount: Int,
    val lastMessage: String,
    val lastTimestamp: Long,
    val isPinned: Boolean,
    val isMuted: Boolean,
    val e2eeVerified: Boolean,
    val securityCode: String,
    val groupDescription: String,
    val onlyAdminsCanSend: Boolean,
    val onlyAdminsCanEdit: Boolean,
    val participantsJson: String, // serialized list of participants
    val avatarUrl: String? = null
)

/**
 * MessageEntity: Local cached message entity for offline storage and high-speed retrieval.
 * Compound indices on [conversationId, timestamp] ensure instantaneous loading of message threads
 * without scanning the full table.
 */
@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId", "timestamp"]),
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
        Index(value = ["isSyncPending"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String,
    val text: String,
    val timestamp: Long,
    val status: MessageStatus,
    val isOutgoing: Boolean,
    val type: MessageType,
    val mediaUri: String?,
    val mediaCaption: String?,
    val mediaSize: String?,
    val mediaDuration: String?,
    val isEncrypted: Boolean,
    val cipherPreview: String,
    val isSyncPending: Boolean = false,
    val localFilePath: String? = null,
    val seenByJson: String = "[]"
)

/**
 * ContactEntity: Local cached contact entity for fast offline contact directory access.
 * Indexed by phone, email, and name for instantaneous auto-complete, lookup, and search.
 */
@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["email"]),
        Index(value = ["name"]),
        Index(value = ["isRegistered"])
    ]
)
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val avatarColor: Long,
    val statusMessage: String,
    val isRegistered: Boolean,
    val conversationId: String?,
    val lastSeenTimestamp: Long = 0L,
    val syncTimestamp: Long = System.currentTimeMillis(),
    val publicKey: String? = null
)

