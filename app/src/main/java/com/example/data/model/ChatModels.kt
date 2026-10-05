package com.example.data.model

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    LOCATION
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String = "",
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.READ,
    val isOutgoing: Boolean = false,
    val type: MessageType = MessageType.TEXT,
    val mediaUri: String? = null,
    val mediaCaption: String? = null,
    val mediaSize: String? = null,
    val mediaDuration: String? = null,
    val isEncrypted: Boolean = true,
    val cipherPreview: String = "AES-256-GCM::" + id.hashCode().toString(16).padStart(16, '0'),
    val seenBy: List<String> = emptyList()
)

data class Participant(
    val id: String,
    val name: String,
    val phone: String,
    val avatarColor: Long = 0xFF008069,
    val isAdmin: Boolean = false,
    val isCurrentUser: Boolean = false,
    val statusMessage: String = "Ada menggunakan WhatsChat",
    val email: String = ""
)

data class UserPresence(
    val userId: String,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val state: String = "offline",
    val displayName: String = ""
)

data class Contact(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val avatarColor: Long = 0xFF008069,
    val statusMessage: String = "Ada menggunakan WhatsChat",
    val isRegistered: Boolean = true,
    val conversationId: String? = null,
    val isOnline: Boolean = false,
    val lastSeenTimestamp: Long = 0L
)

data class Conversation(
    val id: String,
    val name: String,
    val avatarColor: Long = 0xFF128C7E,
    val avatarUrl: String? = null,
    val isGroup: Boolean = false,
    val participantCount: Int = 1,
    val maxParticipants: Int = 200,
    val unreadCount: Int = 0,
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val e2eeVerified: Boolean = true,
    val securityCode: String = "7492 0184 9284 1094 8839 2019 4810 5928 3719 0284 5719 2840",
    val participants: List<Participant> = emptyList(),
    val groupDescription: String = "",
    val onlyAdminsCanSend: Boolean = false,
    val onlyAdminsCanEdit: Boolean = false
)

data class LinkedDevice(
    val id: String,
    val name: String,
    val platform: String, // e.g. "Google Chrome (macOS)", "WhatsChat Desktop (Windows 11)"
    val location: String,
    val lastActiveTime: String,
    val isCurrent: Boolean = false,
    val e2eeKeyFingerprint: String = "SHA256:4a8b...f192",
    val syncStatus: String = "Sinkron Real-Time"
)

enum class BackupFrequency(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
    MANUAL("Hanya saat diklik")
}

data class BackupConfig(
    val frequency: BackupFrequency = BackupFrequency.DAILY,
    val isE2EEncrypted: Boolean = true,
    val lastBackupTime: String = "Hari ini, 02:00",
    val lastBackupSize: String = "34.8 MB",
    val cloudAccount: String = "user.backup@gmail.com",
    val isAutoBackupOnWifiOnly: Boolean = true,
    val includeVideos: Boolean = true
)

data class PushNotificationPayload(
    val id: String,
    val conversationId: String,
    val senderName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isGroup: Boolean = false,
    val groupTitle: String? = null
)

data class UserProfile(
    val displayName: String = "Guru Kaya Raya",
    val email: String = "guru.kayaraya.id@gmail.com",
    val status: String = "Ada menggunakan WhatsChat E2EE",
    val phoneNumber: String = "+62 812-3456-7890",
    val avatarUrl: String? = null,
    val avatarColor: Long = 0xFF128C7E
)
