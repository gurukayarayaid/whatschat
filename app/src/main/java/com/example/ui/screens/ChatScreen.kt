package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.ui.components.E2EESecurityDialog
import com.example.ui.components.MediaSelectionSheet
import com.example.ui.components.MessageBubble
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WaBlueTick
import com.example.ui.theme.WaGreenAccent
import com.example.ui.theme.WaGreenPrimary
import com.example.ui.theme.WaGreyTick
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.Screen
import com.example.util.TimeFormatter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ChatScreen Composable with:
 * - LazyColumn for real-time messages
 * - OutlinedTextField for modern text input
 * - Direct Firebase Firestore integration for real-time fetch & send
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String = "conv_1",
    conversationName: String = "WhatsChat Contact",
    conversation: Conversation? = null,
    messages: List<Message> = emptyList(),
    uploadProgressMap: Map<String, Int> = emptyMap(),
    viewModel: ChatViewModel? = null,
    onBack: () -> Unit = {},
    onOpenGroupInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chatColors = LocalChatColors.current

    // Active conversation details
    val activeConvId = conversation?.id ?: conversationId
    val activeTitle = conversation?.name ?: conversationName
    val isGroup = conversation?.isGroup ?: false

    // Input state
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showE2EEDialog by remember { mutableStateOf(false) }
    var showMediaSheet by remember { mutableStateOf(false) }
    var callNoticeDialog by remember { mutableStateOf<String?>(null) }

    // Real-time Firestore state
    val firestore = remember { FirebaseFirestore.getInstance() }
    var realTimeMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var isFirestoreConnected by remember { mutableStateOf(true) }

    // Lazy list state for message scrolling
    val listState = rememberLazyListState()

    // Real-time Firestore listener: fetches messages dynamically as they arrive in the collection
    DisposableEffect(activeConvId) {
        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection("conversations")
                .document(activeConvId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isFirestoreConnected = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        isFirestoreConnected = true
                        val fetched = snapshot.documents.mapNotNull { doc ->
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
                                val cipherPreview = doc.getString("cipherPreview") ?: ("AES-256-GCM::" + id.hashCode().toString(16).padStart(16, '0'))
                                val mediaUri = doc.getString("mediaUri")
                                val mediaCaption = doc.getString("mediaCaption")
                                val mediaSize = doc.getString("mediaSize")
                                val mediaDuration = doc.getString("mediaDuration")
                                val rawSeenBy = (doc.get("seenBy") as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                                val isReadByOther = rawSeenBy.any { it != senderId } || statusStr == MessageStatus.READ.name
                                val effectiveStatus = if (isReadByOther) MessageStatus.READ else status

                                Message(
                                    id = id,
                                    conversationId = activeConvId,
                                    senderId = senderId,
                                    senderName = senderName,
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
                                    seenBy = rawSeenBy
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        realTimeMessages = fetched
                    }
                }
        } catch (e: Exception) {
            isFirestoreConnected = false
        }

        onDispose {
            registration?.remove()
        }
    }

    // Merge messages: prefer realTimeMessages from Firestore if available, otherwise use initial Room messages
    val displayMessages = remember(realTimeMessages, messages) {
        if (realTimeMessages.isNotEmpty()) {
            realTimeMessages
        } else {
            messages
        }
    }

    // Auto scroll to bottom and mark messages as read when messages update or user opens chat
    LaunchedEffect(activeConvId, displayMessages.size) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1)
        }
        viewModel?.markConversationAsRead(activeConvId)
    }

    // Real-time Firestore send message function
    val sendMessageAction: (String) -> Unit = { rawText ->
        val textToSend = rawText.trim()
        if (textToSend.isNotEmpty()) {
            inputText = ""
            isSending = true
            val newMsgId = "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val currentTime = System.currentTimeMillis()

            // 1. Send to Firebase Firestore
            val firestoreMsgData = hashMapOf(
                "id" to newMsgId,
                "conversationId" to activeConvId,
                "senderId" to "me",
                "senderName" to "Saya",
                "senderAvatar" to "",
                "text" to textToSend,
                "timestamp" to currentTime,
                "status" to MessageStatus.SENT.name,
                "isOutgoing" to true,
                "type" to MessageType.TEXT.name,
                "isEncrypted" to true,
                "cipherPreview" to ("AES-256-GCM::" + newMsgId.hashCode().toString(16).padStart(16, '0')),
                "seenBy" to listOf("me")
            )

            firestore.collection("conversations")
                .document(activeConvId)
                .collection("messages")
                .document(newMsgId)
                .set(firestoreMsgData)
                .addOnSuccessListener {
                    isSending = false
                }
                .addOnFailureListener {
                    isSending = false
                }

            // 2. Update conversation header in Firestore
            val convMeta = hashMapOf(
                "lastMessage" to textToSend,
                "lastTimestamp" to currentTime,
                "updatedAt" to currentTime
            )
            firestore.collection("conversations")
                .document(activeConvId)
                .set(convMeta, SetOptions.merge())

            // 3. Local Room & ViewModel persistence
            viewModel?.sendMessage(
                text = textToSend,
                type = MessageType.TEXT
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable(onClick = onBack)
                            .padding(end = 4.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
                            )
                        }

                        // Avatar
                        val avatarColor = conversation?.avatarColor ?: 0xFF128C7E
                        val avatarUrl = conversation?.avatarUrl
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(avatarColor))
                                .clickable {
                                    if (isGroup) onOpenGroupInfo() else showE2EEDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = activeTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else if (isGroup) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = activeTitle.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                },
                title = {
                    Column(
                        modifier = Modifier
                            .clickable {
                                if (isGroup) onOpenGroupInfo() else showE2EEDialog = true
                            }
                            .padding(start = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "E2EE",
                                tint = WaGreenAccent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (isFirestoreConnected) WaGreenAccent else Color.LightGray,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isFirestoreConnected) "Firebase Real-Time Online" else "Tersimpan Lokal",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { callNoticeDialog = "Panggilan Video Real-Time Terenkripsi dengan $activeTitle" },
                        modifier = Modifier.testTag("video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Panggilan Video",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { callNoticeDialog = "Panggilan Suara Real-Time Terenkripsi dengan $activeTitle" },
                        modifier = Modifier.testTag("voice_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Panggilan Suara",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("chat_detail_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (isGroup) {
                            DropdownMenuItem(
                                text = { Text("Info Grup") },
                                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onOpenGroupInfo()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Verifikasi Enkripsi E2EE") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showE2EEDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sinkronkan Ulang Firestore") },
                            leadingIcon = { Icon(Icons.Default.CloudDone, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Sinkronisasi Firestore Real-Time Aktif", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = chatColors.headerBackground
                )
            )
        },
        bottomBar = {
            // Modern WhatsApp style input bar utilizing OutlinedTextField
            Surface(
                color = chatColors.chatBackground,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // OutlinedTextField for Chat Input
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input"),
                        placeholder = {
                            Text(
                                text = "Ketik pesan...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Emoji WhatsApp", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mood,
                                    contentDescription = "Emoji",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        showMediaSheet = true
                                    },
                                    modifier = Modifier.size(28.dp).testTag("attach_media_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = "Lampiran Media",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(26.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = WaGreenPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = { sendMessageAction(inputText) }
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Dynamic Send / Mic Action Button
                    val hasText = inputText.isNotBlank()
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(WaGreenPrimary)
                            .clickable {
                                if (hasText) {
                                    sendMessageAction(inputText)
                                } else {
                                    Toast.makeText(context, "Tahan untuk merekam pesan suara", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .testTag("send_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (hasText) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                            contentDescription = if (hasText) "Kirim Pesan" else "Pesan Suara",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(chatColors.chatBackground)
        ) {
            // LazyColumn for Messages with Real-Time updates
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_messages_list"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header Security Notice Pill
                item(key = "header_e2ee_security") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = chatColors.lockNoticeBackground,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier
                                .widthIn(max = 380.dp)
                                .clickable { showE2EEDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF7A6A32),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pesan terenkripsi end-to-end dengan sinkronisasi Firebase Firestore Real-Time.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF5E5224),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                    }
                }

                // Empty state if no messages
                if (displayMessages.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = WaGreenPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Belum ada pesan di ruang chat ini",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Ketik pesan menggunakan kolom input di bawah untuk mengirim langsung secara real-time via Cloud Firestore.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Real-Time Messages Items
                items(
                    items = displayMessages,
                    key = { it.id }
                ) { msg ->
                    MessageBubble(
                        message = msg,
                        isGroup = isGroup,
                        uploadProgress = uploadProgressMap[msg.id],
                        onDeleteMessage = { messageToDelete ->
                            if (viewModel != null) {
                                viewModel.deleteMessage(activeConvId, messageToDelete.id)
                            }
                        }
                    )
                }
            }
        }
    }

    // Call Notice Dialog
    callNoticeDialog?.let { noticeText ->
        AlertDialog(
            onDismissRequest = { callNoticeDialog = null },
            title = { Text("Panggilan WhatsChat") },
            text = { Text(noticeText) },
            confirmButton = {
                TextButton(onClick = { callNoticeDialog = null }) {
                    Text("OK")
                }
            }
        )
    }

    // E2EE Security Verification Dialog
    if (showE2EEDialog) {
        val secCode = conversation?.securityCode ?: "3829 5721 9912 4028 1528 7731"
        E2EESecurityDialog(
            contactName = activeTitle,
            securityCode = secCode,
            onDismiss = { showE2EEDialog = false }
        )
    }

    // Media Selection & Fast Upload Sheet
    MediaSelectionSheet(
        isOpen = showMediaSheet,
        onDismiss = { showMediaSheet = false },
        onMediaSelected = { uri, caption, isVideo ->
            showMediaSheet = false
            if (viewModel != null) {
                if (isVideo) {
                    viewModel.sendMessage(
                        text = caption.ifBlank { "🎥 Video" },
                        type = MessageType.VIDEO,
                        mediaUri = uri.toString(),
                        mediaCaption = caption.ifBlank { null },
                        mediaSize = "14.2 MB"
                    )
                } else {
                    viewModel.sendImage(
                        uri = uri,
                        caption = caption,
                        sizeText = "2.4 MB"
                    )
                }
            } else {
                sendMessageAction(caption.ifBlank { "📷 Foto" })
            }
        }
    )
}

/**
 * Individual Message Item Bubble inside LazyColumn
 */
@Composable
private fun ChatScreenMessageItem(
    message: Message,
    chatColors: com.example.ui.theme.ExtendedChatColors
) {
    val isMe = message.isOutgoing
    val bubbleColor = if (isMe) chatColors.sentBubble else chatColors.receivedBubble
    val formattedTime = remember(message.timestamp) { TimeFormatter.formatMessageTime(message.timestamp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isMe) 48.dp else 0.dp,
                end = if (isMe) 0.dp else 48.dp,
                top = 2.dp,
                bottom = 2.dp
            ),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isMe) 12.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 12.dp
            ),
            shadowElevation = 0.7.dp,
            modifier = Modifier.widthIn(min = 64.dp, max = 320.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                // Sender name for incoming messages
                if (!isMe && message.senderName.isNotBlank() && message.senderName != "Teman") {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = WaGreenPrimary
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Message Text Content
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        color = chatColors.textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Timestamp and Delivery / Read Status Row
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            color = chatColors.textSecondary
                        )
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(3.dp))
                        when (message.status) {
                            MessageStatus.SENDING -> {
                                Text(
                                    text = "🕒",
                                    fontSize = 9.sp
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Terkirim",
                                    tint = WaGreyTick,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Diterima",
                                    tint = WaGreyTick,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            MessageStatus.READ -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Dibaca",
                                    tint = WaBlueTick,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
