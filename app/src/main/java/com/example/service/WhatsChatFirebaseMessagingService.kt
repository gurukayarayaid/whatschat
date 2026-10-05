package com.example.service

import android.util.Log
import com.example.data.repository.ChatRepository
import com.example.util.FcmNotificationManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * WhatsChatFirebaseMessagingService:
 * Service to handle incoming Firebase Cloud Messaging (FCM) messages in real-time,
 * deliver high-priority push notifications, and maintain token synchronization.
 */
class WhatsChatFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "WhatsChatFCMService"
    }

    /**
     * Called when a new FCM registration token is generated or refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        try {
            Log.d(TAG, "New FCM Token received: ${token.take(15)}...")
            FcmNotificationManager.saveToken(applicationContext, token)
            FcmNotificationManager.subscribeToDefaultTopics(applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Notice handling new FCM token: ${e.message}")
        }
    }

    /**
     * Called when an incoming FCM push message is received.
     * Handles both notification payloads and data payloads for reliable delivery.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val data = remoteMessage.data

        // Extract chat message fields from data payload or fallback to notification payload
        val conversationId = data["conversation_id"]
            ?: data["conversationId"]
            ?: "conv-1" // Default to active chat if not specified

        val senderName = data["sender_name"]
            ?: data["senderName"]
            ?: remoteMessage.notification?.title
            ?: "Teman WhatsChat"

        val messageText = data["message_text"]
            ?: data["text"]
            ?: data["message"]
            ?: remoteMessage.notification?.body
            ?: "Pesan terenkripsi baru masuk"

        val isGroup = (data["is_group"] ?: data["isGroup"])?.toBoolean() ?: false
        val groupTitle = data["group_title"] ?: data["groupTitle"]
        val messageId = data["message_id"] ?: data["id"] ?: UUID.randomUUID().toString()

        serviceScope.launch {
            try {
                // 1. Persist to local Room database and emit reactive flow for active screens
                val repository = ChatRepository.getInstance(applicationContext)
                val payload = repository.handleIncomingFcmMessage(
                    messageId = messageId,
                    conversationId = conversationId,
                    senderName = senderName,
                    messageText = messageText,
                    isGroup = isGroup,
                    groupTitle = groupTitle
                )

                // 2. Dispatch system status-bar & lockscreen push notification with heads-up display
                FcmNotificationManager.showSystemNotification(applicationContext, payload)
                Log.d(TAG, "FCM push processed and system notification dispatched for: $senderName")
            } catch (e: Exception) {
                Log.e(TAG, "Error handling incoming FCM message", e)
            }
        }
    }
}
