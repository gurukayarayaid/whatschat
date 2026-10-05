package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.TimeFormatter
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.ui.components.ChatInputBar
import com.example.ui.components.E2EESecurityDialog
import com.example.ui.components.MessageBubble
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WaGreenAccent
import com.example.ui.theme.WaGreenPrimary
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: Conversation?,
    messages: List<Message>,
    uploadProgressMap: Map<String, Int>,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenGroupInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()

    val matchingContact = remember(contacts, conversation) {
        if (conversation == null || conversation.isGroup) null
        else contacts.firstOrNull { it.id == conversation.id || it.conversationId == conversation.id || it.name.equals(conversation.name, ignoreCase = true) }
    }

    val contactPresenceStatus = remember(matchingContact) {
        if (matchingContact == null) "online"
        else TimeFormatter.formatLastSeen(matchingContact.lastSeenTimestamp, matchingContact.isOnline)
    }
    var showMenu by remember { mutableStateOf(false) }
    var selectedCipherToInspect by remember { mutableStateOf<String?>(null) }
    var showE2EEDialog by remember { mutableStateOf(false) }
    var showCallNotice by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isScrolledUp by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex < totalItems - 2
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(onClick = onBack)
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
                        if (conversation != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(conversation.avatarColor))
                                    .clickable {
                                        if (conversation.isGroup) onOpenGroupInfo() else showE2EEDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!conversation.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = conversation.avatarUrl,
                                        contentDescription = conversation.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else if (conversation.isGroup) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = conversation.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                },
                title = {
                    if (conversation != null) {
                        Column(
                            modifier = Modifier
                                .clickable {
                                    if (conversation.isGroup) onOpenGroupInfo() else showE2EEDialog = true
                                }
                                .padding(start = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = conversation.name,
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
                            Text(
                                text = if (conversation.isGroup) {
                                    "${conversation.participantCount} peserta (ketuk info grup)"
                                } else {
                                    "$contactPresenceStatus • E2EE aktif"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    // Video Call (E2EE encrypted call notification)
                    IconButton(
                        onClick = { showCallNotice = "Panggilan Video Terenkripsi End-to-End dengan ${conversation?.name}" },
                        modifier = Modifier.testTag("video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Panggilan Video",
                            tint = Color.White
                        )
                    }

                    // Voice Call
                    IconButton(
                        onClick = { showCallNotice = "Panggilan Suara Terenkripsi End-to-End dengan ${conversation?.name}" },
                        modifier = Modifier.testTag("voice_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Panggilan Suara",
                            tint = Color.White
                        )
                    }

                    // E2EE verification shortcut
                    IconButton(
                        onClick = { showE2EEDialog = true },
                        modifier = Modifier.testTag("verify_e2ee_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Verifikasi Keamanan",
                            tint = Color.White
                        )
                    }

                    // More menu
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
                        if (conversation?.isGroup == true) {
                            DropdownMenuItem(
                                text = { Text("Info Grup & Admin (Maks 200)") },
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
                            text = { Text("Perangkat Tertaut") },
                            onClick = {
                                showMenu = false
                                viewModel.navigateTo(Screen.LINKED_DEVICES)
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
            ChatInputBar(
                onSendMessage = { text, type, mediaUri, caption, size, duration ->
                    viewModel.sendMessage(
                        text = text,
                        type = type,
                        mediaUri = mediaUri,
                        mediaCaption = caption,
                        mediaSize = size,
                        mediaDuration = duration
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(chatColors.chatBackground)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Top E2EE Notice Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = chatColors.lockNoticeBackground,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .clickable { showE2EEDialog = true }
                            .testTag("e2ee_banner_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = WaGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🔒 Pesan dan panggilan ini dienkripsi secara end-to-end. Tidak seorang pun di luar chat ini, termasuk WhatsChat, yang dapat membaca atau mendengarkannya. Ketuk untuk info lebih lanjut.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Empty state or date separator
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = chatColors.lockNoticeBackground.copy(alpha = 0.9f),
                                shadowElevation = 0.5.dp
                            ) {
                                Text(
                                    text = "Belum ada pesan. Mulai percakapan!",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Date separator
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = chatColors.lockNoticeBackground.copy(alpha = 0.85f),
                                shadowElevation = 0.5.dp
                            ) {
                                Text(
                                    text = "HARI INI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Messages
                items(messages, key = { it.id }) { msg ->
                    val uploadProgress = uploadProgressMap[msg.id]
                    MessageBubble(
                        message = msg,
                        isGroup = conversation?.isGroup == true,
                        uploadProgress = uploadProgress,
                        onViewCipher = { cipher -> selectedCipherToInspect = cipher }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Scroll to Bottom Floating Action Button
            AnimatedVisibility(
                visible = isScrolledUp,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    },
                    shape = CircleShape,
                    containerColor = chatColors.headerBackground,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("scroll_to_bottom_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll to bottom",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    // Call Simulation Dialog
    if (showCallNotice != null) {
        AlertDialog(
            onDismissRequest = { showCallNotice = null },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = WaGreenPrimary) },
            title = { Text("Panggilan E2EE Terproteksi", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "$showCallNotice\n\nKoneksi terenkripsi Peer-to-Peer (SRTP/DTLS) aktif secara real-time.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showCallNotice = null }) {
                    Text("Mengerti")
                }
            }
        )
    }

    // Cipher Inspection Dialog (Shows raw cryptographic proof)
    if (selectedCipherToInspect != null) {
        AlertDialog(
            onDismissRequest = { selectedCipherToInspect = null },
            icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = WaGreenPrimary) },
            title = { Text("Kriptografi Pesan E2EE", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Protokol: Signal Protocol / Double Ratchet + AES-256-GCM.\nPayload terenkripsi dalam transit:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = selectedCipherToInspect ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedCipherToInspect = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // E2EE Security Dialog
    if (showE2EEDialog && conversation != null) {
        E2EESecurityDialog(
            contactName = conversation.name,
            securityCode = conversation.securityCode,
            onDismiss = { showE2EEDialog = false }
        )
    }
}
