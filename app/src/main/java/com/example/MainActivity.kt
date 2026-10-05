package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.PushNotificationBanner
import com.example.ui.screens.ChatBackupScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.GroupInfoScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LinkedDevicesScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.WhatsChatTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.FcmNotificationManager

class MainActivity : ComponentActivity() {

    private var pendingConversationId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create FCM Notification Channels early
        FcmNotificationManager.createNotificationChannel(this)

        // Start Firestore Realtime Sync Service for automatic incoming message ingestion
        com.example.service.FirestoreMessageSyncService.start(this)

        // Capture notification open intent extras
        pendingConversationId = intent?.getStringExtra("conversation_id")

        setContent {
            val chatViewModel: ChatViewModel = viewModel()
            val themeMode by chatViewModel.themeMode.collectAsStateWithLifecycle()

            // Navigate to target conversation when notification is opened
            LaunchedEffect(pendingConversationId) {
                pendingConversationId?.let { convId ->
                    chatViewModel.openConversation(convId)
                    pendingConversationId = null
                }
            }

            WhatsChatTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WhatsChatApp(viewModel = chatViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("conversation_id")?.let { convId ->
            pendingConversationId = convId
        }
    }
}

@Composable
fun WhatsChatApp(viewModel: ChatViewModel) {
    val context = LocalContext.current

    // Android 13+ (API 33+) Runtime POST_NOTIFICATIONS Permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModel.refreshFcmToken()
            }
        }

        LaunchedEffect(Unit) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    if (!isLoggedIn) {
        LoginScreen(
            onRegisterAndLogin = { phone, name, status ->
                viewModel.registerAndLogin(phone, name, status)
            },
            onLogin = { phone ->
                viewModel.login(phone)
            }
        )
        return
    }

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val linkedDevices by viewModel.linkedDevices.collectAsStateWithLifecycle()
    val backupConfig by viewModel.backupConfig.collectAsStateWithLifecycle()
    val backupProgress by viewModel.backupProgress.collectAsStateWithLifecycle()
    val activeNotification by viewModel.activeNotification.collectAsStateWithLifecycle()
    val uploadProgressMap by viewModel.mediaUploadProgress.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()

    // System Back navigation handler
    BackHandler(enabled = currentScreen != Screen.CHAT_LIST) {
        viewModel.goBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.CHAT_LIST -> {
                ChatListScreen(
                    viewModel = viewModel,
                    onConversationClick = { conv ->
                        viewModel.openConversation(conv.id)
                    }
                )
            }
            Screen.CONTACTS -> {
                ContactsScreen(
                    contacts = contacts,
                    onBack = { viewModel.goBack() },
                    onContactClick = { contact ->
                        viewModel.openChatWithContact(contact)
                    },
                    onAddContact = { name, email, phone, status ->
                        viewModel.addContact(name, email, phone, status)
                    },
                    onDeleteContact = { contactId ->
                        viewModel.deleteContact(contactId)
                    },
                    onCreateGroupClick = {
                        viewModel.navigateTo(Screen.CHAT_LIST)
                    },
                    onTogglePresence = { contactId, currentOnline, name ->
                        viewModel.toggleContactPresence(contactId, currentOnline, name)
                    }
                )
            }
            Screen.CHAT_DETAIL -> {
                ChatDetailScreen(
                    conversation = activeConversation,
                    messages = messages,
                    uploadProgressMap = uploadProgressMap,
                    viewModel = viewModel,
                    onBack = { viewModel.goBack() },
                    onOpenGroupInfo = { viewModel.navigateTo(Screen.GROUP_INFO) }
                )
            }
            Screen.GROUP_INFO -> {
                GroupInfoScreen(
                    conversation = activeConversation,
                    viewModel = viewModel,
                    onBack = { viewModel.goBack() }
                )
            }
            Screen.LINKED_DEVICES -> {
                LinkedDevicesScreen(
                    linkedDevices = linkedDevices,
                    viewModel = viewModel,
                    onBack = { viewModel.goBack() }
                )
            }
            Screen.CHAT_BACKUP -> {
                ChatBackupScreen(
                    backupConfig = backupConfig,
                    backupProgress = backupProgress,
                    viewModel = viewModel,
                    onBack = { viewModel.goBack() }
                )
            }
            Screen.SETTINGS -> {
                SettingsScreen(
                    themeMode = themeMode,
                    viewModel = viewModel,
                    onBack = { viewModel.goBack() }
                )
            }
            Screen.PROFILE -> {
                ProfileScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.goBack() }
                )
            }
        }

        // Global Heads-up Push Notification Banner
        PushNotificationBanner(
            payload = activeNotification,
            onOpenChat = { convId ->
                viewModel.openConversation(convId)
            },
            onDismiss = { viewModel.dismissNotification() }
        )
    }
}
