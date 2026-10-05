package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.local.WhatsChatDatabase
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.PushNotificationPayload
import com.example.data.repository.ChatRepository
import com.example.util.FcmNotificationManager
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

/**
 * FirestoreMessageSyncService:
 * Background service that continuously listens to Cloud Firestore updates
 * for new incoming messages and conversations in real-time, automatically
 * persisting them into the local Room database and notifying the user.
 */
class FirestoreMessageSyncService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private lateinit var database: WhatsChatDatabase
    private lateinit var repository: ChatRepository

    private var messagesListener: ListenerRegistration? = null
    private var conversationsListener: ListenerRegistration? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FirestoreMessageSyncService created. Initializing realtime listeners...")
        database = WhatsChatDatabase.getInstance(applicationContext)
        repository = ChatRepository.getInstance(applicationContext)
        _syncState.value = SyncState.CONNECTING

        startFirestoreListeners()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "FirestoreMessageSyncService onStartCommand received.")
        if (_syncState.value == SyncState.DISCONNECTED || _syncState.value == SyncState.ERROR) {
            startFirestoreListeners()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Set up realtime snapshot listeners on Cloud Firestore.
     */
    private fun startFirestoreListeners() {
        listenToIncomingMessages()
        listenToConversations()
    }

    /**
     * Listens to incoming messages across all conversations in Cloud Firestore
     * using a Collection Group query.
     */
    private fun listenToIncomingMessages() {
        messagesListener?.remove()

        try {
            messagesListener = firestore.collectionGroup("messages")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to messages collection group: ${error.message}", error)
                        _syncState.value = SyncState.ERROR
                        return@addSnapshotListener
                    }

                    if (snapshots == null) return@addSnapshotListener

                    _syncState.value = SyncState.CONNECTED

                    serviceScope.launch {
                        for (change in snapshots.documentChanges) {
                            if (change.type == DocumentChange.Type.ADDED || change.type == DocumentChange.Type.MODIFIED) {
                                processIncomingMessageDoc(change)
                            } else if (change.type == DocumentChange.Type.REMOVED) {
                                val messageId = change.document.getString("id") ?: change.document.id
                                val conversationId = change.document.getString("conversationId")
                                    ?: change.document.reference.parent.parent?.id
                                database.messageDao().deleteMessage(messageId)
                                if (conversationId != null) {
                                    val latest = database.messageDao().getLatestMessage(conversationId)
                                    val conv = database.conversationDao().getConversationDirect(conversationId)
                                    if (conv != null && latest != null) {
                                        database.conversationDao().updateLastMessage(
                                            id = conversationId,
                                            lastMsg = latest.text,
                                            timestamp = latest.timestamp,
                                            unread = conv.unreadCount
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing messages listener: ${e.message}", e)
            _syncState.value = SyncState.ERROR
        }
    }

    /**
     * Process an individual message document change from Cloud Firestore.
     */
    private suspend fun processIncomingMessageDoc(change: DocumentChange) {
        try {
            val doc = change.document
            val messageId = doc.getString("id") ?: doc.id
            val conversationId = doc.getString("conversationId")
                ?: doc.reference.parent.parent?.id
                ?: return

            val senderId = doc.getString("senderId") ?: "unknown"
            val isOutgoing = doc.getBoolean("isOutgoing") ?: (senderId == "me")

            // Skip messages originated by this client if already handled
            val existingMessage = database.messageDao().getMessageById(messageId)

            val text = doc.getString("text") ?: ""
            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            val senderName = doc.getString("senderName") ?: "Teman WhatsChat"
            val senderAvatar = doc.getString("senderAvatar") ?: ""
            val statusStr = doc.getString("status") ?: MessageStatus.DELIVERED.name
            val status = runCatching { MessageStatus.valueOf(statusStr) }.getOrDefault(MessageStatus.DELIVERED)
            val typeStr = doc.getString("type") ?: MessageType.TEXT.name
            val type = runCatching { MessageType.valueOf(typeStr) }.getOrDefault(MessageType.TEXT)
            val mediaUri = doc.getString("mediaUri")
            val mediaCaption = doc.getString("mediaCaption")
            val mediaSize = doc.getString("mediaSize")
            val mediaDuration = doc.getString("mediaDuration")
            val isEncrypted = doc.getBoolean("isEncrypted") ?: true
            val cipherPreview = doc.getString("cipherPreview") ?: "AES-256-GCM::"
            val rawSeenBy = (doc.get("seenBy") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
            val seenByJson = JSONArray(rawSeenBy).toString()
            val isReadByOther = rawSeenBy.any { it != senderId } || statusStr == MessageStatus.READ.name
            val effectiveStatus = if (isReadByOther) MessageStatus.READ else status

            val messageEntity = MessageEntity(
                id = messageId,
                conversationId = conversationId,
                senderId = senderId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                text = text,
                timestamp = timestamp,
                status = effectiveStatus,
                isOutgoing = isOutgoing,
                type = type,
                mediaUri = mediaUri,
                mediaCaption = mediaCaption,
                mediaSize = mediaSize,
                mediaDuration = mediaDuration,
                isEncrypted = isEncrypted,
                cipherPreview = cipherPreview,
                isSyncPending = false,
                seenByJson = seenByJson
            )

            // Save or update message in local Room database
            database.messageDao().insertMessage(messageEntity)

            // Update conversation metadata
            val previewText = when (type) {
                MessageType.IMAGE -> "📷 Foto"
                MessageType.VIDEO -> "🎥 Video"
                MessageType.AUDIO -> "🎵 Pesan Suara"
                MessageType.DOCUMENT -> "📄 Dokumen"
                MessageType.LOCATION -> "📍 Lokasi"
                else -> text
            }

            var conv = database.conversationDao().getConversationDirect(conversationId)
            if (conv == null) {
                // Fetch or create conversation entity locally if not yet present
                conv = fetchOrCreateConversation(conversationId, senderName, previewText, timestamp)
            } else {
                val newUnread = if (!isOutgoing && existingMessage == null) conv.unreadCount + 1 else conv.unreadCount
                database.conversationDao().updateLastMessage(
                    id = conversationId,
                    lastMsg = previewText,
                    timestamp = timestamp,
                    unread = newUnread
                )
            }

            _syncedMessagesCount.value = _syncedMessagesCount.value + 1

            // If it is a new incoming message (not outgoing and wasn't previously in local Room)
            if (!isOutgoing && existingMessage == null && !doc.metadata.hasPendingWrites()) {
                val isGroup = conv.isGroup
                val payload = PushNotificationPayload(
                    id = messageId,
                    conversationId = conversationId,
                    senderName = senderName,
                    messageText = text.ifBlank { previewText },
                    timestamp = timestamp,
                    isGroup = isGroup,
                    groupTitle = if (isGroup) conv.name else null
                )

                // Emit notification into the reactive flow
                repository.emitIncomingNotification(payload)

                // Show Android system status-bar notification
                FcmNotificationManager.showSystemNotification(applicationContext, payload)
                Log.d(TAG, "Realtime Firestore message synced & notified: $messageId from $senderName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming Firestore message", e)
        }
    }

    /**
     * Listens to conversation document updates in Cloud Firestore.
     */
    private fun listenToConversations() {
        conversationsListener?.remove()

        try {
            conversationsListener = firestore.collection("conversations")
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to conversations: ${error.message}", error)
                        return@addSnapshotListener
                    }

                    if (snapshots == null) return@addSnapshotListener

                    serviceScope.launch {
                        for (doc in snapshots.documents) {
                            try {
                                val convId = doc.getString("id") ?: doc.id
                                val name = doc.getString("name") ?: "Obrolan"
                                val avatarColor = doc.getLong("avatarColor") ?: 0xFF008069
                                val isGroup = doc.getBoolean("isGroup") ?: false
                                val participantCount = doc.getLong("participantCount")?.toInt() ?: 2
                                val maxParticipants = doc.getLong("maxParticipants")?.toInt() ?: 200
                                val lastMessage = doc.getString("lastMessage") ?: ""
                                val lastTimestamp = doc.getLong("lastTimestamp") ?: System.currentTimeMillis()
                                val isPinned = doc.getBoolean("isPinned") ?: false
                                val isMuted = doc.getBoolean("isMuted") ?: false
                                val e2eeVerified = doc.getBoolean("e2eeVerified") ?: true
                                val securityCode = doc.getString("securityCode") ?: "7492 0184 9284 1094"
                                val groupDesc = doc.getString("groupDescription") ?: ""
                                val onlyAdminsCanSend = doc.getBoolean("onlyAdminsCanSend") ?: false
                                val onlyAdminsCanEdit = doc.getBoolean("onlyAdminsCanEdit") ?: false
                                val participantsJson = doc.getString("participantsJson") ?: "[]"

                                val existing = database.conversationDao().getConversationDirect(convId)
                                val unread = existing?.unreadCount ?: 0

                                val entity = ConversationEntity(
                                    id = convId,
                                    name = name,
                                    avatarColor = avatarColor,
                                    isGroup = isGroup,
                                    participantCount = participantCount,
                                    maxParticipants = maxParticipants,
                                    unreadCount = unread,
                                    lastMessage = lastMessage,
                                    lastTimestamp = lastTimestamp,
                                    isPinned = isPinned,
                                    isMuted = isMuted,
                                    e2eeVerified = e2eeVerified,
                                    securityCode = securityCode,
                                    groupDescription = groupDesc,
                                    onlyAdminsCanSend = onlyAdminsCanSend,
                                    onlyAdminsCanEdit = onlyAdminsCanEdit,
                                    participantsJson = participantsJson
                                )

                                database.conversationDao().insertOrUpdate(entity)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing conversation from Firestore", e)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in listenToConversations: ${e.message}", e)
        }
    }

    private suspend fun fetchOrCreateConversation(
        conversationId: String,
        fallbackName: String,
        lastMessage: String,
        timestamp: Long
    ): ConversationEntity {
        return try {
            val doc = firestore.collection("conversations").document(conversationId).get().await()
            val entity = if (doc.exists()) {
                ConversationEntity(
                    id = conversationId,
                    name = doc.getString("name") ?: fallbackName,
                    avatarColor = doc.getLong("avatarColor") ?: 0xFF008069,
                    isGroup = doc.getBoolean("isGroup") ?: false,
                    participantCount = doc.getLong("participantCount")?.toInt() ?: 2,
                    maxParticipants = doc.getLong("maxParticipants")?.toInt() ?: 200,
                    unreadCount = 1,
                    lastMessage = lastMessage,
                    lastTimestamp = timestamp,
                    isPinned = doc.getBoolean("isPinned") ?: false,
                    isMuted = doc.getBoolean("isMuted") ?: false,
                    e2eeVerified = doc.getBoolean("e2eeVerified") ?: true,
                    securityCode = doc.getString("securityCode") ?: "7492 0184 9284 1094",
                    groupDescription = doc.getString("groupDescription") ?: "",
                    onlyAdminsCanSend = doc.getBoolean("onlyAdminsCanSend") ?: false,
                    onlyAdminsCanEdit = doc.getBoolean("onlyAdminsCanEdit") ?: false,
                    participantsJson = doc.getString("participantsJson") ?: "[]"
                )
            } else {
                ConversationEntity(
                    id = conversationId,
                    name = fallbackName,
                    avatarColor = 0xFF008069,
                    isGroup = false,
                    participantCount = 2,
                    maxParticipants = 2,
                    unreadCount = 1,
                    lastMessage = lastMessage,
                    lastTimestamp = timestamp,
                    isPinned = false,
                    isMuted = false,
                    e2eeVerified = true,
                    securityCode = "7492 0184 9284 1094",
                    groupDescription = "",
                    onlyAdminsCanSend = false,
                    onlyAdminsCanEdit = false,
                    participantsJson = "[]"
                )
            }
            database.conversationDao().insertOrUpdate(entity)
            entity
        } catch (e: Exception) {
            val fallback = ConversationEntity(
                id = conversationId,
                name = fallbackName,
                avatarColor = 0xFF008069,
                isGroup = false,
                participantCount = 2,
                maxParticipants = 2,
                unreadCount = 1,
                lastMessage = lastMessage,
                lastTimestamp = timestamp,
                isPinned = false,
                isMuted = false,
                e2eeVerified = true,
                securityCode = "7492 0184 9284 1094",
                groupDescription = "",
                onlyAdminsCanSend = false,
                onlyAdminsCanEdit = false,
                participantsJson = "[]"
            )
            database.conversationDao().insertOrUpdate(fallback)
            fallback
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FirestoreMessageSyncService destroyed. Cleaning up listeners.")
        messagesListener?.remove()
        conversationsListener?.remove()
        serviceScope.cancel()
        _syncState.value = SyncState.DISCONNECTED
    }

    enum class SyncState {
        IDLE,
        CONNECTING,
        CONNECTED,
        ERROR,
        DISCONNECTED
    }

    companion object {
        private const val TAG = "FirestoreSyncService"

        private val _syncState = MutableStateFlow(SyncState.IDLE)
        val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

        private val _syncedMessagesCount = MutableStateFlow(0)
        val syncedMessagesCount: StateFlow<Int> = _syncedMessagesCount.asStateFlow()

        /**
         * Start the FirestoreMessageSyncService background sync.
         */
        fun start(context: Context) {
            try {
                val intent = Intent(context, FirestoreMessageSyncService::class.java)
                context.startService(intent)
                Log.d(TAG, "FirestoreMessageSyncService start requested.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start FirestoreMessageSyncService: ${e.message}", e)
            }
        }

        /**
         * Stop the FirestoreMessageSyncService.
         */
        fun stop(context: Context) {
            try {
                val intent = Intent(context, FirestoreMessageSyncService::class.java)
                context.stopService(intent)
                Log.d(TAG, "FirestoreMessageSyncService stop requested.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop FirestoreMessageSyncService: ${e.message}", e)
            }
        }
    }
}
