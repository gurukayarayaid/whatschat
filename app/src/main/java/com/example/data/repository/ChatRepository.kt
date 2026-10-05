package com.example.data.repository

import com.example.data.local.ContactDao
import com.example.data.local.ContactEntity
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.model.BackupConfig
import com.example.data.model.BackupFrequency
import com.example.data.model.Contact
import com.example.data.model.Conversation
import com.example.data.model.LinkedDevice
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.Participant
import com.example.data.model.PushNotificationPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.example.data.remote.FirebaseChatService
import com.example.data.remote.FirebasePresenceService
import com.example.data.remote.FirebaseStorageService
import com.example.data.model.UserPresence
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val contactDao: ContactDao,
    private val firebaseService: FirebaseChatService = FirebaseChatService(),
    private val storageService: FirebaseStorageService = FirebaseStorageService.getInstance(),
    private val presenceService: FirebasePresenceService = FirebasePresenceService()
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Online Firebase Connection status
    val isOnlineFirebaseEnabled: Boolean = firebaseService.isConfigured
    private val activeListeners = mutableMapOf<String, kotlinx.coroutines.Job>()

    // Realtime User Presence Stream from Firebase Realtime Database
    val presenceMap: Flow<Map<String, UserPresence>> = presenceService.listenToAllPresence()

    // Push notification stream for incoming messages
    private val _incomingNotification = MutableSharedFlow<PushNotificationPayload>(extraBufferCapacity = 5)
    val incomingNotification: SharedFlow<PushNotificationPayload> = _incomingNotification.asSharedFlow()

    // Linked Devices State
    private val _linkedDevices = MutableStateFlow<List<LinkedDevice>>(
        listOf(
            LinkedDevice(
                id = "dev-1",
                name = "Google Chrome (macOS Sequoia)",
                platform = "Web Browser",
                location = "Jakarta, Indonesia",
                lastActiveTime = "Aktif sekarang",
                isCurrent = false,
                e2eeKeyFingerprint = "SHA256:7f8a...c93d",
                syncStatus = "Sinkron Real-Time Aktif"
            ),
            LinkedDevice(
                id = "dev-2",
                name = "WhatsChat Desktop (Windows 11)",
                platform = "Aplikasi Komputer",
                location = "Bandung, Indonesia",
                lastActiveTime = "Kemarin pukul 18.42",
                isCurrent = false,
                e2eeKeyFingerprint = "SHA256:3e1b...902a",
                syncStatus = "Tersambung (E2EE)"
            ),
            LinkedDevice(
                id = "dev-3",
                name = "iPad Pro 11 (Safari)",
                platform = "Tablet Browser",
                location = "Jakarta, Indonesia",
                lastActiveTime = "12 September 2026",
                isCurrent = false,
                e2eeKeyFingerprint = "SHA256:8b42...e541",
                syncStatus = "Siap Sinkron"
            )
        )
    )
    val linkedDevices: StateFlow<List<LinkedDevice>> = _linkedDevices.asStateFlow()

    // Backup Configuration State
    private val _backupConfig = MutableStateFlow(
        BackupConfig(
            frequency = BackupFrequency.DAILY,
            isE2EEncrypted = true,
            lastBackupTime = "Hari ini, 02:14 WIB",
            lastBackupSize = "48.2 MB",
            cloudAccount = "user.backup@gmail.com",
            isAutoBackupOnWifiOnly = true,
            includeVideos = true
        )
    )
    val backupConfig: StateFlow<BackupConfig> = _backupConfig.asStateFlow()

    // Backup Progress State
    private val _backupProgress = MutableStateFlow<Int?>(null)
    val backupProgress: StateFlow<Int?> = _backupProgress.asStateFlow()

    // Media Upload Progress Simulation (messageId -> progress 0-100)
    private val _mediaUploadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val mediaUploadProgress: StateFlow<Map<String, Int>> = _mediaUploadProgress.asStateFlow()

    init {
        scope.launch {
            conversationDao.deleteAllConversations()
            messageDao.deleteAllMessages()
            seedInitialDataIfEmpty()
        }
    }

    val allConversations: Flow<List<Conversation>> = conversationDao.getAllConversations().map { list ->
        list.map { entityToConversation(it) }
    }

    // Contacts Stream with Email
    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts().map { list ->
        list.map { entityToContact(it) }
    }

    val registeredContacts: Flow<List<Contact>> = contactDao.getRegisteredContacts().map { list ->
        list.map { entityToContact(it) }
    }

    fun searchContacts(query: String): Flow<List<Contact>> =
        contactDao.searchContacts(query.trim()).map { list ->
            list.map { entityToContact(it) }
        }

    suspend fun getContactByPhone(phone: String): Contact? =
        contactDao.getContactByPhone(phone.trim())?.let { entityToContact(it) }

    fun searchMessages(conversationId: String, query: String): Flow<List<Message>> =
        messageDao.searchMessages(conversationId, query.trim()).map { list ->
            list.map { entityToMessage(it) }
        }

    fun searchAllMessages(query: String): Flow<List<Message>> =
        messageDao.searchAllMessages(query.trim()).map { list ->
            list.map { entityToMessage(it) }
        }

    suspend fun getPendingSyncMessages(): List<Message> =
        messageDao.getPendingSyncMessages().map { entityToMessage(it) }

    suspend fun addContact(
        name: String,
        email: String,
        phone: String,
        statusMessage: String = "Ada menggunakan WhatsChat"
    ): Contact {
        val id = "contact-${UUID.randomUUID().toString().take(8)}"
        val palette = listOf(0xFF008069, 0xFF128C7E, 0xFF0288D1, 0xFF7B1FA2, 0xFFE65100, 0xFFC2185B, 0xFF00A884)
        val color = palette[kotlin.math.abs(name.hashCode()) % palette.size]
        val contact = Contact(
            id = id,
            name = name.trim(),
            email = email.trim(),
            phone = phone.trim(),
            avatarColor = color,
            statusMessage = statusMessage.ifBlank { "Ada menggunakan WhatsChat" },
            isRegistered = true
        )
        contactDao.insertContact(contactToEntity(contact))
        return contact
    }

    suspend fun updateContact(contact: Contact) {
        contactDao.updateContact(contactToEntity(contact))
    }

    suspend fun deleteContact(id: String) {
        contactDao.deleteContact(id)
    }

    suspend fun deleteConversation(conversationId: String) {
        conversationDao.deleteById(conversationId)
        messageDao.clearChat(conversationId)
        activeListeners[conversationId]?.cancel()
        activeListeners.remove(conversationId)
        scope.launch {
            firebaseService.deleteConversationFromCloud(conversationId)
        }
    }

    suspend fun getOrCreateConversationForContact(contact: Contact): String {
        if (!contact.conversationId.isNullOrBlank()) {
            val existing = conversationDao.getConversationDirect(contact.conversationId)
            if (existing != null) return contact.conversationId
        }

        val directId = "conv-${contact.id}"
        val direct = conversationDao.getConversationDirect(directId)
        if (direct != null) return directId

        val conv = ConversationEntity(
            id = directId,
            name = contact.name,
            avatarColor = contact.avatarColor,
            isGroup = false,
            participantCount = 2,
            maxParticipants = 2,
            unreadCount = 0,
            lastMessage = "Mulai obrolan aman dengan ${contact.name}",
            lastTimestamp = System.currentTimeMillis(),
            isPinned = false,
            isMuted = false,
            e2eeVerified = true,
            securityCode = "7492 0184 9284 1094 8839 2019 4810 5928 3719 0284 5719 2840",
            groupDescription = "",
            onlyAdminsCanSend = false,
            onlyAdminsCanEdit = false,
            participantsJson = serializeParticipants(
                listOf(
                    Participant("p-me", "Saya", "+62 812-3456-7890", email = "guru.kayaraya.id@gmail.com", avatarColor = 0xFF008069, isCurrentUser = true),
                    Participant(contact.id, contact.name, contact.phone, email = contact.email, avatarColor = contact.avatarColor, statusMessage = contact.statusMessage)
                )
            )
        )
        conversationDao.insertOrUpdate(conv)
        contactDao.updateContact(contactToEntity(contact.copy(conversationId = directId)))
        return directId
    }

    fun getConversation(id: String): Flow<Conversation?> = conversationDao.getConversationById(id).map {
        it?.let { entityToConversation(it) }
    }

    fun getMessages(conversationId: String): Flow<List<Message>> {
        // Start listening to Firebase cloud messages for this conversation
        startRealtimeCloudSync(conversationId)
        return messageDao.getMessagesForConversation(conversationId).map { list ->
            list.map { entityToMessage(it) }
        }
    }

    private fun startRealtimeCloudSync(conversationId: String) {
        if (activeListeners.containsKey(conversationId)) return
        val job = scope.launch {
            try {
                firebaseService.listenToConversationMessages(conversationId).collect { cloudMessages ->
                    for (cloudMsg in cloudMessages) {
                        val localMsg = messageDao.getMessageById(cloudMsg.id)
                        if (localMsg == null) {
                            // Incoming message from another device or user
                            messageDao.insertMessage(cloudMsg.copy(isOutgoing = (cloudMsg.senderId == "me")))
                            conversationDao.updateLastMessage(
                                id = conversationId,
                                lastMsg = cloudMsg.text,
                                timestamp = cloudMsg.timestamp,
                                unread = 0
                            )
                        } else {
                            // Update status and seenBy if changed in cloud (e.g. recipient read the message)
                            if (localMsg.status != cloudMsg.status || localMsg.seenByJson != cloudMsg.seenByJson) {
                                messageDao.insertMessage(
                                    localMsg.copy(
                                        status = cloudMsg.status,
                                        seenByJson = cloudMsg.seenByJson
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep local offline functionality seamless
            }
        }
        activeListeners[conversationId] = job
    }

    suspend fun markAsRead(conversationId: String) {
        conversationDao.markAsRead(conversationId)
        messageDao.markMessagesAsReadForConversation(conversationId, "[\"me\"]")
        scope.launch {
            firebaseService.markMessagesAsReadInCloud(conversationId, "me")
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        text: String,
        type: MessageType = MessageType.TEXT,
        mediaUri: String? = null,
        mediaCaption: String? = null,
        mediaSize: String? = null,
        mediaDuration: String? = null
    ) {
        val messageId = "msg-" + UUID.randomUUID().toString()
        val cipher = "AES-256-GCM::" + UUID.randomUUID().toString().replace("-", "").take(24)

        val message = MessageEntity(
            id = messageId,
            conversationId = conversationId,
            senderId = "me",
            senderName = "Saya",
            senderAvatar = "",
            text = text,
            timestamp = System.currentTimeMillis(),
            status = if (type == MessageType.TEXT) MessageStatus.DELIVERED else MessageStatus.SENDING,
            isOutgoing = true,
            type = type,
            mediaUri = mediaUri,
            mediaCaption = mediaCaption,
            mediaSize = mediaSize,
            mediaDuration = mediaDuration,
            isEncrypted = true,
            cipherPreview = cipher,
            seenByJson = "[\"me\"]"
        )

        messageDao.insertMessage(message)

        val preview = when (type) {
            MessageType.IMAGE -> "📷 Foto"
            MessageType.VIDEO -> "🎥 Video"
            MessageType.AUDIO -> "🎵 Pesan Suara"
            MessageType.DOCUMENT -> "📄 Dokumen"
            MessageType.LOCATION -> "📍 Lokasi"
            else -> text
        }
        conversationDao.updateLastMessage(conversationId, preview, System.currentTimeMillis(), 0)

        // Sync immediately to Firebase Firestore in background
        scope.launch {
            firebaseService.syncMessageToCloud(message)
        }

        // If image media with local URI, upload to Firebase Storage and update download URL in Room & Firestore
        if (type == MessageType.IMAGE && mediaUri != null && (mediaUri.startsWith("content://") || mediaUri.startsWith("file://"))) {
            scope.launch(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(mediaUri)
                    val uploadResult = storageService.uploadChatImage(
                        conversationId = conversationId,
                        messageId = messageId,
                        imageUri = uri,
                        onProgress = { progress ->
                            _mediaUploadProgress.value = _mediaUploadProgress.value + (messageId to progress)
                        }
                    )

                    val downloadUrl = uploadResult.getOrNull()
                    if (downloadUrl != null) {
                        _mediaUploadProgress.value = _mediaUploadProgress.value + (messageId to 100)
                        delay(100)

                        // Update local Room database with permanent Firebase Storage download URL
                        val updatedMessage = message.copy(
                            mediaUri = downloadUrl,
                            status = MessageStatus.READ
                        )
                        messageDao.insertMessage(updatedMessage)
                        _mediaUploadProgress.value = _mediaUploadProgress.value - messageId
                        firebaseService.syncMessageToCloud(updatedMessage)
                        Log.d("ChatRepository", "Image uploaded & stored in Room + Cloud: $downloadUrl")
                    } else {
                        throw uploadResult.exceptionOrNull() ?: Exception("Unknown upload error")
                    }
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Gagal upload gambar ke Firebase Storage: ${e.message}")
                    val fallbackMessage = message.copy(status = MessageStatus.READ)
                    messageDao.insertMessage(fallbackMessage)
                    _mediaUploadProgress.value = _mediaUploadProgress.value - messageId
                    firebaseService.syncMessageToCloud(fallbackMessage)
                }
            }
        } else if (type != MessageType.TEXT) {
            scope.launch {
                for (p in listOf(25, 55, 85, 100)) {
                    delay(250)
                    _mediaUploadProgress.value = _mediaUploadProgress.value + (messageId to p)
                }
                delay(150)
                val updatedMessage = message.copy(status = MessageStatus.READ)
                messageDao.insertMessage(updatedMessage)
                _mediaUploadProgress.value = _mediaUploadProgress.value - messageId
                firebaseService.syncMessageToCloud(updatedMessage)
            }
        }
    }

    /**
     * Deletes a message from both the local Room database and Cloud Firestore record,
     * cleaning up any attached cloud storage assets and updating the conversation metadata.
     */
    suspend fun deleteMessage(conversationId: String, messageId: String) {
        val existing = messageDao.getMessageById(messageId)
        if (existing?.type == MessageType.IMAGE) {
            scope.launch {
                storageService.deleteChatImage(conversationId, messageId)
            }
        }

        // 1. Delete from local Room database
        messageDao.deleteMessage(messageId)

        // 2. Delete from Cloud Firestore
        scope.launch {
            firebaseService.deleteMessageFromCloud(conversationId, messageId)
        }

        // 3. Update conversation last message preview
        val latest = messageDao.getLatestMessage(conversationId)
        val conv = conversationDao.getConversationDirect(conversationId)
        if (conv != null) {
            if (latest != null) {
                val preview = when (latest.type) {
                    MessageType.IMAGE -> "📷 Foto"
                    MessageType.VIDEO -> "🎥 Video"
                    MessageType.AUDIO -> "🎵 Pesan Suara"
                    MessageType.DOCUMENT -> "📄 Dokumen"
                    MessageType.LOCATION -> "📍 Lokasi"
                    else -> latest.text
                }
                conversationDao.updateLastMessage(
                    id = conversationId,
                    lastMsg = preview,
                    timestamp = latest.timestamp,
                    unread = conv.unreadCount
                )
            } else {
                conversationDao.updateLastMessage(
                    id = conversationId,
                    lastMsg = "Belum ada pesan",
                    timestamp = conv.lastTimestamp,
                    unread = 0
                )
            }
        }
    }

    suspend fun addParticipant(conversationId: String, newName: String, phone: String, email: String = "") {
        val conv = conversationDao.getConversationDirect(conversationId) ?: return
        val currentParticipants = parseParticipants(conv.participantsJson).toMutableList()
        if (currentParticipants.size >= conv.maxParticipants) {
            return // Max 200 reached
        }
        val colors = listOf(0xFF008069, 0xFF128C7E, 0xFF075E54, 0xFF25D366, 0xFF1E88E5, 0xFF7B1FA2, 0xFFE65100)
        val newParticipant = Participant(
            id = "user-" + UUID.randomUUID().toString().take(8),
            name = newName,
            phone = phone,
            email = email,
            avatarColor = colors.random(),
            isAdmin = false,
            isCurrentUser = false,
            statusMessage = "Ada menggunakan WhatsChat"
        )
        currentParticipants.add(newParticipant)
        val updatedJson = serializeParticipants(currentParticipants)
        val updatedConv = conv.copy(
            participantCount = currentParticipants.size,
            participantsJson = updatedJson
        )
        conversationDao.update(updatedConv)

        // Post system message in group
        val sysMsg = MessageEntity(
            id = "msg-" + UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = "system",
            senderName = "Sistem",
            senderAvatar = "",
            text = "🔒 Anda menambahkan $newName ke dalam grup. Obrolan ini terenkripsi end-to-end.",
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.READ,
            isOutgoing = false,
            type = MessageType.TEXT,
            mediaUri = null,
            mediaCaption = null,
            mediaSize = null,
            mediaDuration = null,
            isEncrypted = true,
            cipherPreview = "SYS-ENCRYPTED"
        )
        messageDao.insertMessage(sysMsg)
    }

    suspend fun removeParticipant(conversationId: String, participantId: String) {
        val conv = conversationDao.getConversationDirect(conversationId) ?: return
        val currentParticipants = parseParticipants(conv.participantsJson).toMutableList()
        val removed = currentParticipants.find { it.id == participantId }
        currentParticipants.removeAll { it.id == participantId }
        val updatedConv = conv.copy(
            participantCount = currentParticipants.size,
            participantsJson = serializeParticipants(currentParticipants)
        )
        conversationDao.update(updatedConv)

        if (removed != null) {
            val sysMsg = MessageEntity(
                id = "msg-" + UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = "system",
                senderName = "Sistem",
                senderAvatar = "",
                text = "${removed.name} telah dikeluarkan dari grup.",
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.READ,
                isOutgoing = false,
                type = MessageType.TEXT,
                mediaUri = null,
                mediaCaption = null,
                mediaSize = null,
                mediaDuration = null,
                isEncrypted = true,
                cipherPreview = "SYS-NOTICE"
            )
            messageDao.insertMessage(sysMsg)
        }
    }

    suspend fun createGroup(name: String, description: String, selectedContacts: List<Contact> = emptyList()): String {
        val id = "conv-group-" + UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val myParticipant = Participant("p-me", "Saya", "+62 812-3456-7890", 0xFF008069, isAdmin = true, isCurrentUser = true)
        
        val participantList = mutableListOf(myParticipant)
        for (contact in selectedContacts) {
            participantList.add(
                Participant(
                    id = contact.id,
                    name = contact.name,
                    phone = contact.phone,
                    avatarColor = contact.avatarColor,
                    isAdmin = false,
                    isCurrentUser = false,
                    statusMessage = contact.statusMessage,
                    email = contact.email
                )
            )
        }
        if (participantList.size == 1) {
            participantList.add(Participant("p-budi", "Budi Santoso", "+62 812-8888-9999", 0xFF008069, isAdmin = false))
        }

        val newConv = ConversationEntity(
            id = id,
            name = name,
            avatarColor = 0xFF008069,
            isGroup = true,
            participantCount = participantList.size,
            maxParticipants = 200,
            unreadCount = 0,
            lastMessage = "Grup dibuat",
            lastTimestamp = now,
            isPinned = false,
            isMuted = false,
            e2eeVerified = true,
            securityCode = "7829 4019 5810 6928 4719 0284 6719 2840 8492 0184 9284 2095",
            groupDescription = description,
            onlyAdminsCanSend = false,
            onlyAdminsCanEdit = false,
            participantsJson = serializeParticipants(participantList)
        )
        conversationDao.insertOrUpdate(newConv)
        firebaseService.syncConversation(newConv)

        val sysMsg = MessageEntity(
            id = "msg-" + UUID.randomUUID().toString(),
            conversationId = id,
            senderId = "system",
            senderName = "Sistem",
            senderAvatar = "",
            text = "Anda membuat grup \"$name\" dengan ${participantList.size} anggota dan proteksi E2EE.",
            timestamp = now,
            status = MessageStatus.READ,
            isOutgoing = false,
            type = MessageType.TEXT,
            mediaUri = null,
            mediaCaption = null,
            mediaSize = null,
            mediaDuration = null,
            isEncrypted = true,
            cipherPreview = "E2EE-INIT-SESSION"
        )
        messageDao.insertMessage(sysMsg)
        return id
    }

    suspend fun uploadAndSetGroupAvatar(
        conversationId: String,
        imageUri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> {
        val uploadResult = storageService.uploadGroupAvatar(conversationId, imageUri, onProgress)
        val downloadUrl = uploadResult.getOrNull()
        if (downloadUrl != null) {
            conversationDao.updateGroupAvatar(conversationId, downloadUrl)
            scope.launch {
                firebaseService.updateConversationAvatar(conversationId, downloadUrl)
            }
            return Result.success(downloadUrl)
        }
        return uploadResult
    }

    suspend fun updateGroupAvatar(conversationId: String, avatarUrl: String) {
        conversationDao.updateGroupAvatar(conversationId, avatarUrl)
        scope.launch {
            firebaseService.updateConversationAvatar(conversationId, avatarUrl)
        }
    }

    suspend fun toggleAdminStatus(conversationId: String, participantId: String) {
        val conv = conversationDao.getConversationDirect(conversationId) ?: return
        val currentParticipants = parseParticipants(conv.participantsJson).toMutableList()
        val index = currentParticipants.indexOfFirst { it.id == participantId }
        if (index != -1) {
            val p = currentParticipants[index]
            val updatedP = p.copy(isAdmin = !p.isAdmin)
            currentParticipants[index] = updatedP
            val updatedConv = conv.copy(
                participantsJson = serializeParticipants(currentParticipants)
            )
            conversationDao.update(updatedConv)
        }
    }

    suspend fun updateGroupSettings(conversationId: String, onlyAdminsCanSend: Boolean, onlyAdminsCanEdit: Boolean) {
        val conv = conversationDao.getConversationDirect(conversationId) ?: return
        val updated = conv.copy(
            onlyAdminsCanSend = onlyAdminsCanSend,
            onlyAdminsCanEdit = onlyAdminsCanEdit
        )
        conversationDao.update(updated)
    }

    // Linked Devices methods
    fun linkNewDevice(name: String, platform: String) {
        val newDev = LinkedDevice(
            id = "dev-" + UUID.randomUUID().toString().take(6),
            name = name,
            platform = platform,
            location = "Jakarta, ID",
            lastActiveTime = "Aktif sekarang",
            isCurrent = false,
            e2eeKeyFingerprint = "SHA256:" + UUID.randomUUID().toString().take(8),
            syncStatus = "Sinkronisasi P2P Berhasil"
        )
        _linkedDevices.value = listOf(newDev) + _linkedDevices.value
    }

    fun removeLinkedDevice(deviceId: String) {
        _linkedDevices.value = _linkedDevices.value.filter { it.id != deviceId }
    }

    // Cloud Backup methods
    fun updateBackupFrequency(frequency: BackupFrequency) {
        _backupConfig.value = _backupConfig.value.copy(frequency = frequency)
    }

    fun toggleE2EBackupEncryption(enabled: Boolean) {
        _backupConfig.value = _backupConfig.value.copy(isE2EEncrypted = enabled)
    }

    fun performCloudBackup() {
        scope.launch {
            for (p in 0..100 step 20) {
                _backupProgress.value = p
                delay(300)
            }
            delay(200)
            val sdf = SimpleDateFormat("Hari ini, HH:mm 'WIB'", Locale.getDefault())
            _backupConfig.value = _backupConfig.value.copy(
                lastBackupTime = sdf.format(Date()),
                lastBackupSize = "${(35..55).random()}.${(1..9).random()} MB"
            )
            _backupProgress.value = null
        }
    }

    fun restoreCloudBackup() {
        scope.launch {
            for (p in 0..100 step 25) {
                _backupProgress.value = p
                delay(250)
            }
            delay(150)
            _backupProgress.value = null
        }
    }

    // Simulate incoming push notification message
    fun triggerSimulatedPushMessage(conversationId: String, senderName: String, text: String, isGroup: Boolean = false) {
        scope.launch {
            val msgId = "msg-" + UUID.randomUUID().toString()
            val msg = MessageEntity(
                id = msgId,
                conversationId = conversationId,
                senderId = "incoming-" + UUID.randomUUID().toString().take(4),
                senderName = senderName,
                senderAvatar = "",
                text = text,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.DELIVERED,
                isOutgoing = false,
                type = MessageType.TEXT,
                mediaUri = null,
                mediaCaption = null,
                mediaSize = null,
                mediaDuration = null,
                isEncrypted = true,
                cipherPreview = "AES-256-GCM::" + UUID.randomUUID().toString().take(12)
            )
            messageDao.insertMessage(msg)
            val conv = conversationDao.getConversationDirect(conversationId)
            val unread = (conv?.unreadCount ?: 0) + 1
            conversationDao.updateLastMessage(conversationId, text, System.currentTimeMillis(), unread)

            // Trigger heads-up notification banner
            _incomingNotification.emit(
                PushNotificationPayload(
                    id = msgId,
                    conversationId = conversationId,
                    senderName = senderName,
                    messageText = text,
                    timestamp = System.currentTimeMillis(),
                    isGroup = isGroup,
                    groupTitle = if (isGroup) conv?.name else null
                )
            )
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        // Seed initial contacts with email addresses if empty
        if (contactDao.getContactCount() == 0) {
            val initialContacts = listOf(
                ContactEntity(
                    id = "contact-siti",
                    name = "Siti Rahma",
                    email = "siti.rahma@gmail.com",
                    phone = "+62 813-7722-1144",
                    avatarColor = 0xFF7B1FA2,
                    statusMessage = "Sibuk • Hanya obrolan penting",
                    isRegistered = true,
                    conversationId = null
                ),
                ContactEntity(
                    id = "contact-budi",
                    name = "Budi Santoso",
                    email = "budi.santoso@startup.id",
                    phone = "+62 812-4455-6677",
                    avatarColor = 0xFF0288D1,
                    statusMessage = "Tersedia di WhatsChat",
                    isRegistered = true,
                    conversationId = null
                ),
                ContactEntity(
                    id = "contact-ahmad",
                    name = "Ahmad Fauzi (Lead)",
                    email = "ahmad.fauzi@techcorp.id",
                    phone = "+62 811-2233-4455",
                    avatarColor = 0xFF128C7E,
                    statusMessage = "Mengembangkan WhatsChat E2EE",
                    isRegistered = true,
                    conversationId = null
                ),
                ContactEntity(
                    id = "contact-dewi",
                    name = "Dewi Lestari",
                    email = "dewi.lestari@desain.co.id",
                    phone = "+62 819-3322-1100",
                    avatarColor = 0xFFE91E63,
                    statusMessage = "UI/UX Designer • Fast Response",
                    isRegistered = true,
                    conversationId = null
                ),
                ContactEntity(
                    id = "contact-eko",
                    name = "Eko Prasetyo",
                    email = "eko.prasetyo@android.dev",
                    phone = "+62 856-1122-3344",
                    avatarColor = 0xFF2E7D32,
                    statusMessage = "Always coding with Kotlin & Compose",
                    isRegistered = true,
                    conversationId = null
                ),
                ContactEntity(
                    id = "contact-rina",
                    name = "Rina Marlina",
                    email = "rina.marlina@webmail.id",
                    phone = "+62 813-9988-7766",
                    avatarColor = 0xFFE65100,
                    statusMessage = "Ada menggunakan WhatsChat",
                    isRegistered = true,
                    conversationId = null
                ),
                ContactEntity(
                    id = "contact-guru",
                    name = "Guru Kaya Raya",
                    email = "guru.kayaraya.id@gmail.com",
                    phone = "+62 812-9988-1122",
                    avatarColor = 0xFF008069,
                    statusMessage = "Pendidik Digital & Finansial Indonesia",
                    isRegistered = true,
                    conversationId = null
                ),
                ContactEntity(
                    id = "contact-desi",
                    name = "Desi (Design Lead)",
                    email = "desi.ux@creativestudio.com",
                    phone = "+62 815-9900-1122",
                    avatarColor = 0xFFF57C00,
                    statusMessage = "Design System & Prototyping",
                    isRegistered = true,
                    conversationId = null
                )
            )
            contactDao.insertAll(initialContacts)
            presenceService.seedSamplePresenceIfEmpty(initialContacts.map { entityToContact(it) })
        }
    }

    // Realtime Presence operations
    fun startUserPresenceTracking(userId: String, displayName: String) {
        presenceService.startPresenceTracking(userId, displayName)
    }

    fun setUserPresenceOffline(userId: String) {
        presenceService.setOffline(userId)
    }

    fun setUserPresenceOnline(userId: String, displayName: String) {
        presenceService.setOnline(userId, displayName)
    }

    fun updateContactPresence(contactId: String, isOnline: Boolean, displayName: String = "") {
        presenceService.updateContactPresence(contactId, isOnline, displayName)
    }

    private fun entityToConversation(entity: ConversationEntity): Conversation {
        return Conversation(
            id = entity.id,
            name = entity.name,
            avatarColor = entity.avatarColor,
            avatarUrl = entity.avatarUrl,
            isGroup = entity.isGroup,
            participantCount = entity.participantCount,
            maxParticipants = entity.maxParticipants,
            unreadCount = entity.unreadCount,
            lastMessage = entity.lastMessage,
            lastTimestamp = entity.lastTimestamp,
            isPinned = entity.isPinned,
            isMuted = entity.isMuted,
            e2eeVerified = entity.e2eeVerified,
            securityCode = entity.securityCode,
            participants = parseParticipants(entity.participantsJson),
            groupDescription = entity.groupDescription,
            onlyAdminsCanSend = entity.onlyAdminsCanSend,
            onlyAdminsCanEdit = entity.onlyAdminsCanEdit
        )
    }

    private fun parseSeenBy(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun entityToMessage(entity: MessageEntity): Message {
        val seenList = parseSeenBy(entity.seenByJson)
        val isRead = entity.status == MessageStatus.READ || seenList.any { it != entity.senderId && it != "me" }
        val effectiveStatus = if (isRead) MessageStatus.READ else entity.status
        return Message(
            id = entity.id,
            conversationId = entity.conversationId,
            senderId = entity.senderId,
            senderName = entity.senderName,
            senderAvatar = entity.senderAvatar,
            text = entity.text,
            timestamp = entity.timestamp,
            status = effectiveStatus,
            isOutgoing = entity.isOutgoing,
            type = entity.type,
            mediaUri = entity.mediaUri,
            mediaCaption = entity.mediaCaption,
            mediaSize = entity.mediaSize,
            mediaDuration = entity.mediaDuration,
            isEncrypted = entity.isEncrypted,
            cipherPreview = entity.cipherPreview,
            seenBy = seenList
        )
    }

    private fun serializeParticipants(participants: List<Participant>): String {
        val array = JSONArray()
        participants.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("phone", p.phone)
            obj.put("email", p.email)
            obj.put("avatarColor", p.avatarColor)
            obj.put("isAdmin", p.isAdmin)
            obj.put("isCurrentUser", p.isCurrentUser)
            obj.put("statusMessage", p.statusMessage)
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseParticipants(json: String): List<Participant> {
        if (json.isBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<Participant>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Participant(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", "Peserta"),
                        phone = obj.optString("phone", "-"),
                        avatarColor = obj.optLong("avatarColor", 0xFF008069),
                        isAdmin = obj.optBoolean("isAdmin", false),
                        isCurrentUser = obj.optBoolean("isCurrentUser", false),
                        statusMessage = obj.optString("statusMessage", ""),
                        email = obj.optString("email", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun entityToContact(entity: ContactEntity): Contact {
        return Contact(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            phone = entity.phone,
            avatarColor = entity.avatarColor,
            statusMessage = entity.statusMessage,
            isRegistered = entity.isRegistered,
            conversationId = entity.conversationId
        )
    }

    private fun contactToEntity(contact: Contact): ContactEntity {
        return ContactEntity(
            id = contact.id,
            name = contact.name,
            email = contact.email,
            phone = contact.phone,
            avatarColor = contact.avatarColor,
            statusMessage = contact.statusMessage,
            isRegistered = contact.isRegistered,
            conversationId = contact.conversationId
        )
    }

    /**
     * Handles real-time push messages delivered via Firebase Cloud Messaging (FCM).
     * Saves incoming message directly into Room and triggers active reactive stream.
     */
    suspend fun handleIncomingFcmMessage(
        messageId: String = UUID.randomUUID().toString(),
        conversationId: String,
        senderName: String,
        messageText: String,
        isGroup: Boolean = false,
        groupTitle: String? = null
    ): PushNotificationPayload {
        val timestamp = System.currentTimeMillis()
        val incomingMsg = MessageEntity(
            id = messageId,
            conversationId = conversationId,
            senderId = "fcm-remote-$senderName",
            senderName = senderName,
            senderAvatar = "",
            text = messageText,
            timestamp = timestamp,
            status = MessageStatus.DELIVERED,
            isOutgoing = false,
            type = MessageType.TEXT,
            mediaUri = null,
            mediaCaption = null,
            mediaSize = null,
            mediaDuration = null,
            isEncrypted = true,
            cipherPreview = "AES-256-GCM::" + UUID.randomUUID().toString().take(12)
        )

        // Insert into database
        messageDao.insertMessage(incomingMsg)

        // Update conversation metadata
        val conv = conversationDao.getConversationDirect(conversationId)
        val unread = (conv?.unreadCount ?: 0) + 1
        conversationDao.updateLastMessage(conversationId, messageText, timestamp, unread)

        val payload = PushNotificationPayload(
            id = messageId,
            conversationId = conversationId,
            senderName = senderName,
            messageText = messageText,
            timestamp = timestamp,
            isGroup = isGroup,
            groupTitle = groupTitle ?: if (isGroup) conv?.name else null
        )

        // Emit for in-app heads up banner
        _incomingNotification.emit(payload)
        return payload
    }

    /**
     * Emits an incoming push notification payload into the reactive flow
     * for in-app banners and foreground observers.
     */
    suspend fun emitIncomingNotification(payload: PushNotificationPayload) {
        _incomingNotification.emit(payload)
    }

    /**
     * Upload an image to Firebase Storage and update local Room DB with the download URL.
     */
    suspend fun uploadChatImage(
        conversationId: String,
        messageId: String,
        imageUri: Uri,
        onProgress: ((Int) -> Unit)? = null
    ): Result<String> {
        val result = storageService.uploadChatImage(conversationId, messageId, imageUri, onProgress)
        val downloadUrl = result.getOrNull()
        if (downloadUrl != null) {
            messageDao.updateMessageMediaUriAndStatus(messageId, downloadUrl, MessageStatus.READ)
        }
        return result
    }

    /**
     * Retrieve the public download URL for a chat image.
     */
    suspend fun getChatImageDownloadUrl(conversationId: String, messageId: String): Result<String> {
        return storageService.getChatImageDownloadUrl(conversationId, messageId)
    }

    /**
     * Retrieve raw image bytes from Firebase Storage.
     */
    suspend fun retrieveImageBytes(downloadUrlOrPath: String): Result<ByteArray> {
        return storageService.downloadImageBytes(downloadUrlOrPath)
    }

    /**
     * Delete chat image from Firebase Storage.
     */
    suspend fun deleteChatImage(conversationId: String, messageId: String): Result<Unit> {
        return storageService.deleteChatImage(conversationId, messageId)
    }

    companion object {
        @Volatile
        private var INSTANCE: ChatRepository? = null

        fun getInstance(context: android.content.Context): ChatRepository {
            return INSTANCE ?: synchronized(this) {
                val db = com.example.data.local.WhatsChatDatabase.getInstance(context.applicationContext)
                val instance = INSTANCE ?: ChatRepository(
                    db.conversationDao(),
                    db.messageDao(),
                    db.contactDao()
                )
                INSTANCE = instance
                instance
            }
        }
    }
}
