package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WhatsChatDatabase
import com.example.data.model.BackupConfig
import com.example.data.model.BackupFrequency
import com.example.data.model.Contact
import com.example.data.model.Conversation
import com.example.data.model.LinkedDevice
import com.example.data.model.Message
import com.example.data.model.MessageType
import com.example.data.model.PushNotificationPayload
import com.example.data.model.UserProfile
import com.example.data.model.UserPresence
import com.example.data.remote.FirebasePresenceService
import com.example.data.repository.ChatRepository
import com.example.service.FirestoreMessageSyncService
import com.example.ui.theme.AppThemeMode
import com.example.util.FcmNotificationManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChatFilter {
    ALL,
    UNREAD,
    FAVORITES,
    GROUPS
}

enum class Screen {
    CHAT_LIST,
    CHAT_DETAIL,
    GROUP_INFO,
    CONTACTS,
    LINKED_DEVICES,
    CHAT_BACKUP,
    SETTINGS,
    PROFILE
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val repository = ChatRepository.getInstance(application)

    // Firebase Cloud Messaging (FCM) State
    private val _fcmToken = MutableStateFlow<String?>(FcmNotificationManager.getCachedToken(application))
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _fcmStatus = MutableStateFlow("Terhubung ke Firebase Cloud Messaging")
    val fcmStatus: StateFlow<String> = _fcmStatus.asStateFlow()

    // Firestore Realtime Sync Service state & metrics
    val firestoreSyncState: StateFlow<FirestoreMessageSyncService.SyncState> = FirestoreMessageSyncService.syncState
    val firestoreSyncedCount: StateFlow<Int> = FirestoreMessageSyncService.syncedMessagesCount

    // Theme Mode
    private val _themeMode = MutableStateFlow(AppThemeMode.LIGHT)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // Screen Navigation
    private val _currentScreen = MutableStateFlow(Screen.CHAT_LIST)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Group Avatar Upload Progress tracking (convId -> progress percent)
    private val _groupAvatarUploadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val groupAvatarUploadProgress: StateFlow<Map<String, Int>> = _groupAvatarUploadProgress.asStateFlow()

    // Active conversation
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    // Filters and search
    private val _selectedFilter = MutableStateFlow(ChatFilter.ALL)
    val selectedFilter: StateFlow<ChatFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Push notification banner state
    private val _activeNotification = MutableStateFlow<PushNotificationPayload?>(null)
    val activeNotification: StateFlow<PushNotificationPayload?> = _activeNotification.asStateFlow()

    // Security verify dialog state
    private val _showE2EEDialog = MutableStateFlow(false)
    val showE2EEDialog: StateFlow<Boolean> = _showE2EEDialog.asStateFlow()

    // User Profile
    private val prefs = application.getSharedPreferences("whatschat_user_profile", Context.MODE_PRIVATE)
    private val _userProfile = MutableStateFlow(
        UserProfile(
            displayName = prefs.getString("display_name", "Guru Kaya Raya") ?: "Guru Kaya Raya",
            email = prefs.getString("email", "guru.kayaraya.id@gmail.com") ?: "guru.kayaraya.id@gmail.com",
            status = prefs.getString("status", "Ada menggunakan WhatsChat E2EE") ?: "Ada menggunakan WhatsChat E2EE",
            phoneNumber = prefs.getString("phone_number", "+62 812-3456-7890") ?: "+62 812-3456-7890",
            avatarUrl = prefs.getString("avatar_url", null),
            avatarColor = prefs.getLong("avatar_color", 0xFF128C7E)
        )
    )
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // Auth & Session Management
    private val authPrefs = application.getSharedPreferences("whatschat_auth", Context.MODE_PRIVATE)
    private val _isLoggedIn = MutableStateFlow(authPrefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentPhoneNumber = MutableStateFlow(authPrefs.getString("current_phone", null))
    val currentPhoneNumber: StateFlow<String?> = _currentPhoneNumber.asStateFlow()

    fun registerAndLogin(phone: String, name: String, status: String) {
        authPrefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("current_phone", phone)
            .apply()
        _isLoggedIn.value = true
        _currentPhoneNumber.value = phone
        updateDisplayName(name)
        updatePhoneNumber(phone)
        updateStatus(status)
    }

    fun login(phone: String) {
        authPrefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("current_phone", phone)
            .apply()
        _isLoggedIn.value = true
        _currentPhoneNumber.value = phone
        updatePhoneNumber(phone)
    }

    fun logout() {
        authPrefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("current_phone")
            .apply()
        _isLoggedIn.value = false
        _currentPhoneNumber.value = null
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        _currentScreen.value = Screen.CHAT_LIST
    }

    init {
        // Fetch or sync user profile from Firestore users/me
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document("me")
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val name = doc.getString("displayName") ?: _userProfile.value.displayName
                        val email = doc.getString("email") ?: _userProfile.value.email
                        val status = doc.getString("status") ?: _userProfile.value.status
                        val phone = doc.getString("phoneNumber") ?: _userProfile.value.phoneNumber
                        val avatar = doc.getString("avatarUrl") ?: _userProfile.value.avatarUrl
                        val color = doc.getLong("avatarColor") ?: _userProfile.value.avatarColor
                        val updated = UserProfile(name, email, status, phone, avatar, color)
                        _userProfile.value = updated
                        // Cache locally
                        prefs.edit()
                            .putString("display_name", updated.displayName)
                            .putString("email", updated.email)
                            .putString("status", updated.status)
                            .putString("phone_number", updated.phoneNumber)
                            .putString("avatar_url", updated.avatarUrl)
                            .putLong("avatar_color", updated.avatarColor)
                            .apply()
                    }
                }
        } catch (_: Exception) {}

        // Initialize FCM Notification Channel & Token Synchronization
        FcmNotificationManager.createNotificationChannel(application)
        refreshFcmToken()

        // Start Firestore Realtime Sync Service
        FirestoreMessageSyncService.start(application)

        // Start Firebase Realtime Database User Presence Tracking
        try {
            repository.startUserPresenceTracking("me", _userProfile.value.displayName)
        } catch (e: Exception) {
            Log.w("ChatViewModel", "Could not start presence tracking: ${e.message}")
        }

        // Observe incoming notifications
        viewModelScope.launch {
            repository.incomingNotification.collect { payload ->
                // Don't pop up if user is already in that active chat
                if (_activeConversationId.value != payload.conversationId || _currentScreen.value != Screen.CHAT_DETAIL) {
                    _activeNotification.value = payload
                    delay(5000)
                    if (_activeNotification.value?.id == payload.id) {
                        _activeNotification.value = null
                    }
                }
            }
        }
    }

    // Conversations filtered
    val conversations: StateFlow<List<Conversation>> = combine(
        repository.allConversations,
        _selectedFilter,
        _searchQuery
    ) { list, filter, query ->
        var result = list
        when (filter) {
            ChatFilter.ALL -> {}
            ChatFilter.UNREAD -> result = result.filter { it.unreadCount > 0 }
            ChatFilter.FAVORITES -> result = result.filter { it.isPinned }
            ChatFilter.GROUPS -> result = result.filter { it.isGroup }
        }
        if (query.isNotBlank()) {
            result = result.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.lastMessage.contains(query, ignoreCase = true)
            }
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active conversation detail
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeConversation: StateFlow<Conversation?> = _activeConversationId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getConversation(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Messages for active conversation
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message>> = _activeConversationId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getMessages(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val linkedDevices: StateFlow<List<LinkedDevice>> = repository.linkedDevices
    val backupConfig: StateFlow<BackupConfig> = repository.backupConfig
    val backupProgress: StateFlow<Int?> = repository.backupProgress
    val mediaUploadProgress: StateFlow<Map<String, Int>> = repository.mediaUploadProgress

    // Realtime User Presence Map
    val userPresenceMap: StateFlow<Map<String, UserPresence>> = repository.presenceMap.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Contacts with Email Addresses and Realtime Online/Offline Presence
    val contacts: StateFlow<List<Contact>> = combine(
        repository.allContacts,
        userPresenceMap
    ) { contactList, presenceMap ->
        contactList.map { contact ->
            val presence = presenceMap[contact.id]
                ?: presenceMap[contact.phone]
                ?: presenceMap[FirebasePresenceService.sanitizeKey(contact.id)]
                ?: presenceMap[FirebasePresenceService.sanitizeKey(contact.phone)]

            if (presence != null) {
                contact.copy(
                    isOnline = presence.isOnline,
                    lastSeenTimestamp = presence.lastSeen
                )
            } else {
                contact
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleContactPresence(contactId: String, currentOnline: Boolean, displayName: String = "") {
        repository.updateContactPresence(contactId, !currentOnline, displayName)
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun openConversation(id: String) {
        _activeConversationId.value = id
        _currentScreen.value = Screen.CHAT_DETAIL
        markConversationAsRead(id)
    }

    fun markConversationAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun openChatWithContact(contact: Contact) {
        viewModelScope.launch {
            val convId = repository.getOrCreateConversationForContact(contact)
            openConversation(convId)
        }
    }

    fun addContact(name: String, email: String, phone: String, statusMessage: String = "Ada menggunakan WhatsChat") {
        viewModelScope.launch {
            repository.addContact(name, email, phone, statusMessage)
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
        }
    }

    fun goBack() {
        when (_currentScreen.value) {
            Screen.CHAT_DETAIL -> {
                _activeConversationId.value = null
                _currentScreen.value = Screen.CHAT_LIST
            }
            Screen.GROUP_INFO -> {
                _currentScreen.value = Screen.CHAT_DETAIL
            }
            Screen.CONTACTS, Screen.LINKED_DEVICES, Screen.CHAT_BACKUP, Screen.SETTINGS, Screen.PROFILE -> {
                _currentScreen.value = Screen.CHAT_LIST
            }
            Screen.CHAT_LIST -> {}
        }
    }

    fun setFilter(filter: ChatFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch(active: Boolean) {
        _isSearching.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    fun toggleThemeQuick() {
        _themeMode.value = if (_themeMode.value == AppThemeMode.LIGHT) AppThemeMode.DARK else AppThemeMode.LIGHT
    }

    fun showSecurityVerification(show: Boolean) {
        _showE2EEDialog.value = show
    }

    fun dismissNotification() {
        _activeNotification.value = null
    }

    fun sendMessage(
        text: String,
        type: MessageType = MessageType.TEXT,
        mediaUri: String? = null,
        mediaCaption: String? = null,
        mediaSize: String? = null,
        mediaDuration: String? = null
    ) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = convId,
                text = text,
                type = type,
                mediaUri = mediaUri,
                mediaCaption = mediaCaption,
                mediaSize = mediaSize,
                mediaDuration = mediaDuration
            )
        }
    }

    /**
     * Send image directly via Firebase Storage and local Room database integration.
     */
    fun sendImage(
        uri: Uri,
        caption: String = "",
        sizeText: String = "2.4 MB"
    ) {
        val convId = _activeConversationId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                conversationId = convId,
                text = caption.ifBlank { "📷 Foto" },
                type = MessageType.IMAGE,
                mediaUri = uri.toString(),
                mediaCaption = caption.ifBlank { null },
                mediaSize = sizeText
            )
        }
    }

    /**
     * Retrieve image bytes from Firebase Storage for a given download URL.
     */
    suspend fun retrieveImageBytes(downloadUrl: String): Result<ByteArray> {
        return repository.retrieveImageBytes(downloadUrl)
    }

    /**
     * Delete a message from local Room DB and Cloud Firestore.
     */
    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(conversationId, messageId)
        }
    }

    fun createGroup(name: String, description: String, selectedContacts: List<Contact> = emptyList()) {
        viewModelScope.launch {
            val newId = repository.createGroup(name, description, selectedContacts)
            openConversation(newId)
        }
    }

    fun addParticipant(convId: String, name: String, phone: String, email: String = "") {
        viewModelScope.launch {
            repository.addParticipant(convId, name, phone, email)
        }
    }

    fun removeParticipant(convId: String, participantId: String) {
        viewModelScope.launch {
            repository.removeParticipant(convId, participantId)
        }
    }

    fun toggleAdmin(convId: String, participantId: String) {
        viewModelScope.launch {
            repository.toggleAdminStatus(convId, participantId)
        }
    }

    fun updateGroupPermissions(convId: String, onlyAdminsCanSend: Boolean, onlyAdminsCanEdit: Boolean) {
        viewModelScope.launch {
            repository.updateGroupSettings(convId, onlyAdminsCanSend, onlyAdminsCanEdit)
        }
    }

    fun uploadGroupAvatar(convId: String, imageUri: Uri) {
        viewModelScope.launch {
            _groupAvatarUploadProgress.value = _groupAvatarUploadProgress.value + (convId to 0)
            repository.uploadAndSetGroupAvatar(
                conversationId = convId,
                imageUri = imageUri,
                onProgress = { progress ->
                    _groupAvatarUploadProgress.value = _groupAvatarUploadProgress.value + (convId to progress)
                }
            )
            _groupAvatarUploadProgress.value = _groupAvatarUploadProgress.value - convId
        }
    }

    fun updateGroupAvatarUrl(convId: String, avatarUrl: String) {
        viewModelScope.launch {
            repository.updateGroupAvatar(convId, avatarUrl)
        }
    }

    fun linkDevice(name: String, platform: String) {
        repository.linkNewDevice(name, platform)
    }

    fun unlinkDevice(deviceId: String) {
        repository.removeLinkedDevice(deviceId)
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        repository.updateBackupFrequency(frequency)
    }

    fun toggleE2EBackup(enabled: Boolean) {
        repository.toggleE2EBackupEncryption(enabled)
    }

    fun startBackup() {
        repository.performCloudBackup()
    }

    fun startRestore() {
        repository.restoreCloudBackup()
    }

    fun refreshFcmToken() {
        FcmNotificationManager.syncFcmToken(getApplication()) { token, error ->
            if (token != null) {
                _fcmToken.value = token
                _fcmStatus.value = "FCM Aktif • Siap Menerima Pesan Real-time"
            } else {
                _fcmStatus.value = if (error != null) "Status FCM: $error" else "FCM Siap (Lokal/Offline)"
            }
        }
    }

    fun simulateIncomingPush() {
        val conv = conversations.value.randomOrNull() ?: return
        val sampleSenders = listOf("Budi Santoso", "Siti Rahma", "Ahmad Fauzi", "Rina Marlina", "Dewi Lestari")
        val sampleMessages = listOf(
            "Halo! File sudah diterima dengan aman lewat E2EE ya.",
            "Bisakah kirim foto revisi terbarunya sekarang?",
            "Sinkronisasi perangkat tablet berhasil tersambung!",
            "Jangan lupa cek cadangan cloud malam ini.",
            "Grup sudah mencapai 185 peserta, sangat ramai!"
        )
        val sender = if (conv.isGroup) sampleSenders.random() else conv.name
        val text = sampleMessages.random()

        viewModelScope.launch {
            val payload = repository.handleIncomingFcmMessage(
                conversationId = conv.id,
                senderName = sender,
                messageText = text,
                isGroup = conv.isGroup,
                groupTitle = if (conv.isGroup) conv.name else null
            )
            // Show real Android system notification
            FcmNotificationManager.showSystemNotification(getApplication(), payload)
        }
    }

    fun testFcmPushNotification(
        senderName: String = "Siti Rahma",
        messageText: String = "Pesan FCM real-time telah diterima dengan enkripsi E2EE!",
        isGroup: Boolean = false
    ) {
        val conv = conversations.value.firstOrNull()
        val convId = conv?.id ?: "conv-1"
        viewModelScope.launch {
            val payload = repository.handleIncomingFcmMessage(
                conversationId = convId,
                senderName = senderName,
                messageText = messageText,
                isGroup = isGroup,
                groupTitle = if (isGroup) "Komunitas WhatsChat" else null
            )
            FcmNotificationManager.showSystemNotification(getApplication(), payload)
        }
    }

    // Profile Management Methods
    fun updateDisplayName(name: String) {
        val updated = _userProfile.value.copy(displayName = name.trim())
        _userProfile.value = updated
        prefs.edit().putString("display_name", updated.displayName).apply()
        syncProfileToFirestore(updated)
    }

    fun updateEmail(email: String) {
        val updated = _userProfile.value.copy(email = email.trim())
        _userProfile.value = updated
        prefs.edit().putString("email", updated.email).apply()
        syncProfileToFirestore(updated)
    }

    fun updateStatus(status: String) {
        val updated = _userProfile.value.copy(status = status.trim())
        _userProfile.value = updated
        prefs.edit().putString("status", updated.status).apply()
        syncProfileToFirestore(updated)
    }

    fun updatePhoneNumber(phone: String) {
        val updated = _userProfile.value.copy(phoneNumber = phone.trim())
        _userProfile.value = updated
        prefs.edit().putString("phone_number", updated.phoneNumber).apply()
        syncProfileToFirestore(updated)
    }

    fun updateAvatarUrl(url: String?) {
        val updated = _userProfile.value.copy(avatarUrl = url)
        _userProfile.value = updated
        prefs.edit().putString("avatar_url", url).apply()
        syncProfileToFirestore(updated)
    }

    fun updateAvatarColor(color: Long) {
        val updated = _userProfile.value.copy(avatarColor = color)
        _userProfile.value = updated
        prefs.edit().putLong("avatar_color", color).apply()
        syncProfileToFirestore(updated)
    }

    fun uploadAvatarToFirebaseStorage(
        uri: Uri,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val storage = FirebaseStorage.getInstance()
                val filename = "avatars/profile_${System.currentTimeMillis()}.jpg"
                val ref = storage.reference.child(filename)

                val uploadTask = ref.putFile(uri)
                uploadTask.addOnProgressListener { taskSnapshot ->
                    val total = taskSnapshot.totalByteCount
                    if (total > 0) {
                        val progress = ((100.0 * taskSnapshot.bytesTransferred) / total).toInt()
                        onProgress(progress)
                    }
                }.addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUri ->
                        val downloadUrl = downloadUri.toString()
                        updateAvatarUrl(downloadUrl)
                        onSuccess(downloadUrl)
                    }.addOnFailureListener {
                        // Fallback to local uri string if download URL generation encounters an issue
                        val localUriString = uri.toString()
                        updateAvatarUrl(localUriString)
                        onSuccess(localUriString)
                    }
                }.addOnFailureListener { exception ->
                    // If remote storage upload fails (e.g. security rules or offline), save local uri
                    val localUriString = uri.toString()
                    updateAvatarUrl(localUriString)
                    onFailure(exception.localizedMessage ?: "Gagal upload ke Firebase Storage. Gambar disimpan lokal.")
                }
            } catch (e: Exception) {
                val localUriString = uri.toString()
                updateAvatarUrl(localUriString)
                onFailure(e.localizedMessage ?: "Terjadi kesalahan upload")
            }
        }
    }

    private fun syncProfileToFirestore(profile: UserProfile) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val data = mapOf(
                "displayName" to profile.displayName,
                "email" to profile.email,
                "status" to profile.status,
                "phoneNumber" to profile.phoneNumber,
                "avatarUrl" to profile.avatarUrl,
                "avatarColor" to profile.avatarColor,
                "updatedAt" to System.currentTimeMillis()
            )
            firestore.collection("users").document("me").set(data)
        } catch (_: Exception) {}
    }
}
