package com.example.data.remote

import android.util.Log
import com.example.data.local.ConversationEntity
import com.example.data.local.MessageEntity
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray

/**
 * Realtime sync service powered by Firebase Firestore (Free Spark Plan).
 * Project ID: whatschat-7d1e6
 */
class FirebaseChatService {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val isConfigured: Boolean
        get() = true

    /**
     * Send or sync a message to Cloud Firestore in real time.
     * Collection: conversations/{conversationId}/messages/{messageId}
     */
    suspend fun syncMessageToCloud(message: MessageEntity): Boolean {
        return try {
            val seenByList = if (message.seenByJson.isNotBlank()) {
                try {
                    val arr = JSONArray(message.seenByJson)
                    (0 until arr.length()).map { arr.getString(it) }
                } catch (e: Exception) {
                    listOf(message.senderId)
                }
            } else {
                listOf(message.senderId)
            }

            val messageData = hashMapOf(
                "id" to message.id,
                "conversationId" to message.conversationId,
                "senderId" to message.senderId,
                "senderName" to message.senderName,
                "senderAvatar" to message.senderAvatar,
                "text" to message.text,
                "timestamp" to message.timestamp,
                "status" to message.status.name,
                "isOutgoing" to message.isOutgoing,
                "type" to message.type.name,
                "mediaUri" to (message.mediaUri ?: ""),
                "mediaCaption" to (message.mediaCaption ?: ""),
                "mediaSize" to (message.mediaSize ?: ""),
                "mediaDuration" to (message.mediaDuration ?: ""),
                "isEncrypted" to message.isEncrypted,
                "cipherPreview" to message.cipherPreview,
                "seenBy" to seenByList
            )

            firestore.collection("conversations")
                .document(message.conversationId)
                .collection("messages")
                .document(message.id)
                .set(messageData, SetOptions.merge())
                .await()

            // Update conversation metadata on Firestore
            val convUpdate = hashMapOf(
                "lastMessage" to message.text,
                "lastTimestamp" to message.timestamp,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("conversations")
                .document(message.conversationId)
                .set(convUpdate, SetOptions.merge())
                .await()

            true
        } catch (e: Exception) {
            Log.e("FirebaseChatService", "Gagal sinkronisasi pesan ke Firebase: ${e.message}")
            false
        }
    }

    /**
     * Listen to realtime incoming messages from Cloud Firestore.
     */
    fun listenToConversationMessages(conversationId: String): Flow<List<MessageEntity>> = callbackFlow {
        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseChatService", "Firestore listen error: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val text = doc.getString("text") ?: ""
                                val senderId = doc.getString("senderId") ?: "unknown"
                                val senderName = doc.getString("senderName") ?: "Teman"
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val statusStr = doc.getString("status") ?: MessageStatus.DELIVERED.name
                                val status = runCatching { MessageStatus.valueOf(statusStr) }.getOrDefault(MessageStatus.DELIVERED)
                                val typeStr = doc.getString("type") ?: MessageType.TEXT.name
                                val type = runCatching { MessageType.valueOf(typeStr) }.getOrDefault(MessageType.TEXT)
                                val isOutgoing = doc.getBoolean("isOutgoing") ?: (senderId == "me")
                                val isEncrypted = doc.getBoolean("isEncrypted") ?: true
                                val cipherPreview = doc.getString("cipherPreview") ?: ""
                                val mediaUri = doc.getString("mediaUri")
                                val mediaCaption = doc.getString("mediaCaption")
                                val rawSeenBy = (doc.get("seenBy") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                val seenByJson = JSONArray(rawSeenBy).toString()
                                val isReadByOther = rawSeenBy.any { it != senderId } || statusStr == MessageStatus.READ.name
                                val effectiveStatus = if (isReadByOther) MessageStatus.READ else status

                                MessageEntity(
                                    id = id,
                                    conversationId = conversationId,
                                    senderId = senderId,
                                    senderName = senderName,
                                    senderAvatar = "",
                                    text = text,
                                    timestamp = timestamp,
                                    status = effectiveStatus,
                                    isOutgoing = isOutgoing,
                                    type = type,
                                    mediaUri = mediaUri,
                                    mediaCaption = mediaCaption,
                                    mediaSize = doc.getString("mediaSize"),
                                    mediaDuration = doc.getString("mediaDuration"),
                                    isEncrypted = isEncrypted,
                                    cipherPreview = cipherPreview,
                                    seenByJson = seenByJson
                                )
                            } catch (ex: Exception) {
                                null
                            }
                        }
                        trySend(messages)
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseChatService", "Error registrasi listener Firestore: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * Mark messages in a conversation as read by the recipient in Cloud Firestore.
     * Updates the `seenBy` array with FieldValue.arrayUnion and updates `status` to "READ".
     */
    suspend fun markMessagesAsReadInCloud(conversationId: String, readerId: String = "me") {
        try {
            val messagesRef = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")

            val snapshot = messagesRef.get().await()
            if (snapshot.isEmpty) return

            val batch = firestore.batch()
            var updateCount = 0

            for (doc in snapshot.documents) {
                val senderId = doc.getString("senderId") ?: ""
                val seenByList = (doc.get("seenBy") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()

                if (senderId != readerId && !seenByList.contains(readerId)) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "seenBy" to FieldValue.arrayUnion(readerId),
                            "status" to MessageStatus.READ.name,
                            "readAt" to System.currentTimeMillis()
                        )
                    )
                    updateCount++
                }
            }

            if (updateCount > 0) {
                batch.commit().await()
                Log.d("FirebaseChatService", "Marked $updateCount messages as READ in Cloud Firestore for conv $conversationId")
            }
        } catch (e: Exception) {
            Log.e("FirebaseChatService", "Error syncing read status to Cloud Firestore: ${e.message}")
        }
    }

    /**
     * Sync conversation metadata
     */
    suspend fun syncConversation(conv: ConversationEntity) {
        try {
            val data = hashMapOf(
                "id" to conv.id,
                "name" to conv.name,
                "avatarColor" to conv.avatarColor,
                "avatarUrl" to (conv.avatarUrl ?: ""),
                "isGroup" to conv.isGroup,
                "participantCount" to conv.participantCount,
                "maxParticipants" to conv.maxParticipants,
                "lastMessage" to conv.lastMessage,
                "lastTimestamp" to conv.lastTimestamp,
                "e2eeVerified" to conv.e2eeVerified,
                "securityCode" to conv.securityCode,
                "groupDescription" to conv.groupDescription,
                "onlyAdminsCanSend" to conv.onlyAdminsCanSend,
                "onlyAdminsCanEdit" to conv.onlyAdminsCanEdit,
                "participantsJson" to conv.participantsJson
            )
            firestore.collection("conversations")
                .document(conv.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseChatService", "Error sync conversation: ${e.message}")
        }
    }

    /**
     * Update conversation group avatar in Cloud Firestore
     */
    suspend fun updateConversationAvatar(conversationId: String, avatarUrl: String) {
        try {
            val updateData = hashMapOf(
                "avatarUrl" to avatarUrl,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("conversations")
                .document(conversationId)
                .set(updateData, SetOptions.merge())
                .await()
            Log.d("FirebaseChatService", "Updated avatarUrl for conversation $conversationId in Firestore: $avatarUrl")
        } catch (e: Exception) {
            Log.e("FirebaseChatService", "Error updating conversation avatar in Firestore: ${e.message}")
        }
    }

    /**
     * Realtime snapshot listener for all conversation threads in Firestore
     */
    fun listenToConversations(): Flow<List<com.example.data.model.Conversation>> = callbackFlow {
        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection("conversations")
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseChatService", "Error listening to conversations: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val list = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val name = doc.getString("name") ?: "Obrolan"
                                val avatarColor = doc.getLong("avatarColor") ?: 0xFF128C7E
                                val avatarUrl = doc.getString("avatarUrl")?.takeIf { it.isNotBlank() }
                                val isGroup = doc.getBoolean("isGroup") ?: false
                                val participantCount = doc.getLong("participantCount")?.toInt() ?: 1
                                val maxParticipants = doc.getLong("maxParticipants")?.toInt() ?: 200
                                val unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
                                val lastMessage = doc.getString("lastMessage") ?: ""
                                val lastTimestamp = doc.getLong("lastTimestamp") ?: System.currentTimeMillis()
                                val isPinned = doc.getBoolean("isPinned") ?: false
                                val isMuted = doc.getBoolean("isMuted") ?: false
                                val e2eeVerified = doc.getBoolean("e2eeVerified") ?: true
                                val securityCode = doc.getString("securityCode") ?: "7492 0184 9284 1094"
                                val groupDesc = doc.getString("groupDescription") ?: ""
                                val onlyAdminsCanSend = doc.getBoolean("onlyAdminsCanSend") ?: false
                                val onlyAdminsCanEdit = doc.getBoolean("onlyAdminsCanEdit") ?: false

                                com.example.data.model.Conversation(
                                    id = id,
                                    name = name,
                                    avatarColor = avatarColor,
                                    avatarUrl = avatarUrl,
                                    isGroup = isGroup,
                                    participantCount = participantCount,
                                    maxParticipants = maxParticipants,
                                    unreadCount = unreadCount,
                                    lastMessage = lastMessage,
                                    lastTimestamp = lastTimestamp,
                                    isPinned = isPinned,
                                    isMuted = isMuted,
                                    e2eeVerified = e2eeVerified,
                                    securityCode = securityCode,
                                    groupDescription = groupDesc,
                                    onlyAdminsCanSend = onlyAdminsCanSend,
                                    onlyAdminsCanEdit = onlyAdminsCanEdit
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        trySend(list)
                    }
                }
        } catch (e: Exception) {
            Log.e("FirebaseChatService", "Error register conversation listener: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }

    /**
     * Delete conversation document and all associated messages from Cloud Firestore.
     */
    suspend fun deleteConversationFromCloud(conversationId: String) {
        try {
            val messagesRef = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
            val snapshot = messagesRef.get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()

            firestore.collection("conversations")
                .document(conversationId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("FirebaseChatService", "Error deleting conversation from cloud: ${e.message}")
        }
    }

    /**
     * Delete a single message document from Cloud Firestore.
     */
    suspend fun deleteMessageFromCloud(conversationId: String, messageId: String) {
        try {
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .delete()
                .await()
            Log.d("FirebaseChatService", "Message $messageId deleted from Cloud Firestore")
        } catch (e: Exception) {
            Log.e("FirebaseChatService", "Error deleting message from Cloud Firestore: ${e.message}")
        }
    }
}

