package com.example.data.remote

import android.util.Log
import com.example.data.model.Contact
import com.example.data.model.UserPresence
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Realtime presence tracking service powered by Firebase Realtime Database.
 * Monitors user connection status via .info/connected and propagates online/offline events.
 */
class FirebasePresenceService {

    private val database: FirebaseDatabase by lazy {
        try {
            FirebaseDatabase.getInstance().apply {
                try {
                    // Set persistence and sync
                    getReference("status").keepSynced(true)
                } catch (e: Exception) {
                    Log.w("FirebasePresenceService", "keepSynced note: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("FirebasePresenceService", "Failed to get FirebaseDatabase instance: ${e.message}")
            FirebaseDatabase.getInstance()
        }
    }

    private var connectedListener: ValueEventListener? = null
    private var activeUserId: String? = null

    /**
     * Initializes presence tracking for the current active user.
     * When online, updates status/userId to online and registers onDisconnect() handler.
     */
    fun startPresenceTracking(userId: String, displayName: String = "WhatsChat User") {
        if (userId.isBlank()) return
        activeUserId = userId
        val safeKey = sanitizeKey(userId)
        val userStatusRef = database.getReference("status/$safeKey")
        val connectedRef = database.getReference(".info/connected")

        connectedListener?.let { connectedRef.removeEventListener(it) }

        connectedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    val offlineMap = hashMapOf<String, Any>(
                        "isOnline" to false,
                        "state" to "offline",
                        "lastSeen" to ServerValue.TIMESTAMP,
                        "displayName" to displayName,
                        "userId" to userId
                    )
                    userStatusRef.onDisconnect().setValue(offlineMap)

                    val onlineMap = hashMapOf<String, Any>(
                        "isOnline" to true,
                        "state" to "online",
                        "lastSeen" to ServerValue.TIMESTAMP,
                        "displayName" to displayName,
                        "userId" to userId
                    )
                    userStatusRef.setValue(onlineMap)
                    Log.d("FirebasePresenceService", "Connected to Firebase Realtime DB. Presence: ONLINE for $userId")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FirebasePresenceService", "Connected listener cancelled: ${error.message}")
            }
        }

        connectedRef.addValueEventListener(connectedListener!!)
    }

    /**
     * Manually set active user offline
     */
    fun setOffline(userId: String) {
        if (userId.isBlank()) return
        val safeKey = sanitizeKey(userId)
        val userStatusRef = database.getReference("status/$safeKey")
        val offlineMap = hashMapOf<String, Any>(
            "isOnline" to false,
            "state" to "offline",
            "lastSeen" to ServerValue.TIMESTAMP,
            "userId" to userId
        )
        userStatusRef.setValue(offlineMap)
    }

    /**
     * Manually set active user online
     */
    fun setOnline(userId: String, displayName: String = "WhatsChat User") {
        if (userId.isBlank()) return
        val safeKey = sanitizeKey(userId)
        val userStatusRef = database.getReference("status/$safeKey")
        val onlineMap = hashMapOf<String, Any>(
            "isOnline" to true,
            "state" to "online",
            "lastSeen" to ServerValue.TIMESTAMP,
            "displayName" to displayName,
            "userId" to userId
        )
        userStatusRef.setValue(onlineMap)
    }

    /**
     * Real-time flow of all users' presence from Firebase Realtime Database
     */
    fun listenToAllPresence(): Flow<Map<String, UserPresence>> = callbackFlow {
        val statusRef = database.getReference("status")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val presenceMap = mutableMapOf<String, UserPresence>()
                for (child in snapshot.children) {
                    val key = child.key ?: continue
                    val isOnline = child.child("isOnline").getValue(Boolean::class.java)
                        ?: (child.child("state").getValue(String::class.java) == "online")
                    val lastSeen = child.child("lastSeen").getValue(Long::class.java) ?: 0L
                    val state = child.child("state").getValue(String::class.java) ?: if (isOnline) "online" else "offline"
                    val rawUserId = child.child("userId").getValue(String::class.java) ?: key
                    val displayName = child.child("displayName").getValue(String::class.java) ?: ""

                    val presence = UserPresence(
                        userId = rawUserId,
                        isOnline = isOnline,
                        lastSeen = lastSeen,
                        state = state,
                        displayName = displayName
                    )
                    presenceMap[key] = presence
                    presenceMap[rawUserId] = presence
                    // Also store with sanitized key
                    presenceMap[sanitizeKey(rawUserId)] = presence
                }
                trySend(presenceMap)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebasePresenceService", "Error listening to presence: ${error.message}")
            }
        }

        statusRef.addValueEventListener(listener)
        awaitClose {
            statusRef.removeEventListener(listener)
        }
    }

    /**
     * Seeds initial presence data for contacts if not yet present in Realtime Database.
     */
    fun seedSamplePresenceIfEmpty(contacts: List<Contact>) {
        val statusRef = database.getReference("status")
        statusRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                for ((index, contact) in contacts.withIndex()) {
                    val safeKey = sanitizeKey(contact.id)
                    if (!snapshot.hasChild(safeKey)) {
                        // Varied sample presence for demo: 1st and 3rd online, others realistic lastSeen
                        val isOnline = (index % 2 == 0) || index == 1
                        val lastSeen = when (index % 4) {
                            0 -> now
                            1 -> now - (3 * 60 * 1000L) // 3 mins ago
                            2 -> now - (25 * 60 * 1000L) // 25 mins ago
                            else -> now - (3 * 3600 * 1000L) // 3 hours ago
                        }
                        val sampleMap = hashMapOf<String, Any>(
                            "userId" to contact.id,
                            "isOnline" to isOnline,
                            "state" to if (isOnline) "online" else "offline",
                            "lastSeen" to lastSeen,
                            "displayName" to contact.name
                        )
                        statusRef.child(safeKey).setValue(sampleMap)
                        val phoneKey = sanitizeKey(contact.phone)
                        if (phoneKey.isNotBlank()) {
                            statusRef.child(phoneKey).setValue(sampleMap)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /**
     * Update presence of a specific contact (for simulation or contact testing)
     */
    fun updateContactPresence(contactId: String, isOnline: Boolean, displayName: String = "") {
        val safeKey = sanitizeKey(contactId)
        val userStatusRef = database.getReference("status/$safeKey")
        val presenceData = hashMapOf<String, Any>(
            "userId" to contactId,
            "isOnline" to isOnline,
            "state" to if (isOnline) "online" else "offline",
            "lastSeen" to System.currentTimeMillis(),
            "displayName" to displayName
        )
        userStatusRef.setValue(presenceData)
    }

    companion object {
        fun sanitizeKey(key: String): String {
            return key.replace(".", "_")
                .replace("#", "_")
                .replace("$", "_")
                .replace("[", "_")
                .replace("]", "_")
                .replace("/", "_")
                .replace(" ", "")
                .replace("+", "")
                .replace("-", "")
        }
    }
}
