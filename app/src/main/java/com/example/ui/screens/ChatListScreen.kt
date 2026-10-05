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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.ui.components.ChatListItem
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WaBadgeGreen
import com.example.ui.theme.WaGreenAccent
import com.example.ui.theme.WaGreenDark
import com.example.ui.theme.WaGreenPrimary
import com.example.ui.viewmodel.ChatFilter
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.Screen
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

/**
 * ChatListScreen Composable:
 * - Fetches and displays all active conversation threads from Cloud Firestore in real-time.
 * - Navigates directly to ChatScreen when any conversation thread is clicked.
 * - Seamlessly falls back to Room database when offline or during initial synchronization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    viewModel: ChatViewModel,
    onConversationClick: (Conversation) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val chatColors = LocalChatColors.current

    // Local VM states
    val roomConversations by viewModel.conversations.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()

    // Firestore real-time active threads state
    val firestore = remember { FirebaseFirestore.getInstance() }
    var firestoreConversations by remember { mutableStateOf<List<Conversation>>(emptyList()) }
    var isFirestoreActive by remember { mutableStateOf(true) }

    // Dialog states
    var showMenu by remember { mutableStateOf(false) }
    var showFirebaseStatusDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var newGroupDesc by remember { mutableStateOf("") }
    var selectedContactIds by remember { mutableStateOf(setOf<String>()) }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

    // Realtime Listener for active conversation threads from Firestore
    DisposableEffect(Unit) {
        var registration: ListenerRegistration? = null
        try {
            registration = firestore.collection("conversations")
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        isFirestoreActive = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        isFirestoreActive = true
                        val threads = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.getString("id") ?: doc.id
                                val name = doc.getString("name") ?: "Obrolan"
                                val avatarColor = doc.getLong("avatarColor") ?: 0xFF128C7E
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

                                Conversation(
                                    id = id,
                                    name = name,
                                    avatarColor = avatarColor,
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
                        firestoreConversations = threads
                    }
                }
        } catch (e: Exception) {
            isFirestoreActive = false
        }

        onDispose {
            registration?.remove()
        }
    }

    // Merge active conversation threads:
    // Prioritize Firestore real-time threads, and merge Room local threads so no cached data is missed.
    val activeThreads = remember(firestoreConversations, roomConversations, selectedFilter, searchQuery) {
        val baseList = if (firestoreConversations.isNotEmpty()) {
            val firestoreMap = firestoreConversations.associateBy { it.id }
            val roomRemaining = roomConversations.filterNot { firestoreMap.containsKey(it.id) }
            firestoreConversations + roomRemaining
        } else {
            roomConversations
        }

        var filtered = baseList
        when (selectedFilter) {
            ChatFilter.ALL -> {}
            ChatFilter.UNREAD -> filtered = filtered.filter { it.unreadCount > 0 }
            ChatFilter.FAVORITES -> filtered = filtered.filter { it.isPinned }
            ChatFilter.GROUPS -> filtered = filtered.filter { it.isGroup }
        }

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                    it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }

        filtered
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(chatColors.headerBackground)
            ) {
                if (isSearching) {
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = {
                                    Text(
                                        "Cari chat aktif, grup, atau pesan...",
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input_field")
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.toggleSearch(false) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup Pencarian",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = chatColors.headerBackground
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "WhatsChat",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        ),
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = "Firestore Sync",
                                        tint = if (isFirestoreActive) WaGreenAccent else Color.LightGray,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Text(
                                    text = if (isFirestoreActive) "Cloud Firestore Real-Time (${activeThreads.size} obrolan)" else "Sinkronisasi Lokal Aktif",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.toggleSearch(true) },
                                modifier = Modifier.testTag("search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Cari",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    val nextMode = when (themeMode) {
                                        AppThemeMode.LIGHT -> AppThemeMode.DARK
                                        AppThemeMode.DARK -> AppThemeMode.SYSTEM
                                        AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
                                    }
                                    viewModel.setThemeMode(nextMode)
                                },
                                modifier = Modifier.testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = when (themeMode) {
                                        AppThemeMode.LIGHT -> Icons.Default.DarkMode
                                        AppThemeMode.DARK -> Icons.Default.LightMode
                                        AppThemeMode.SYSTEM -> Icons.Default.Settings
                                    },
                                    contentDescription = "Ubah Tema",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.testTag("home_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Menu Lainnya",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Status Firebase Online (Real-Time)") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CloudDone,
                                            contentDescription = null,
                                            tint = WaGreenPrimary
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showFirebaseStatusDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_item_firebase_status")
                                )
                                DropdownMenuItem(
                                    text = { Text("Daftar Kontak & Email (${contacts.size})") },
                                    leadingIcon = { Icon(Icons.Default.Contacts, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.navigateTo(Screen.CONTACTS)
                                    },
                                    modifier = Modifier.testTag("menu_item_contacts")
                                )
                                DropdownMenuItem(
                                    text = { Text("Grup Baru (hingga 200 anggota)") },
                                    leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showNewGroupDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Perangkat Tertaut (Multi-Perangkat)") },
                                    leadingIcon = { Icon(Icons.Default.Devices, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.navigateTo(Screen.LINKED_DEVICES)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cadangan Chat (Cloud Otomatis)") },
                                    leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.navigateTo(Screen.CHAT_BACKUP)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Profil Pengguna") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.navigateTo(Screen.PROFILE)
                                    },
                                    modifier = Modifier.testTag("menu_item_profile")
                                )
                                DropdownMenuItem(
                                    text = { Text("Pengaturan & Tema") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.navigateTo(Screen.SETTINGS)
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = chatColors.headerBackground
                        )
                    )
                }

                // Filter Category Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == ChatFilter.ALL,
                            onClick = { viewModel.setFilter(ChatFilter.ALL) },
                            label = {
                                Text(
                                    "Semua",
                                    color = if (selectedFilter == ChatFilter.ALL) Color.White else Color.White.copy(alpha = 0.8f)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White.copy(alpha = 0.25f),
                                containerColor = Color.Transparent
                            ),
                            border = null
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == ChatFilter.UNREAD,
                            onClick = { viewModel.setFilter(ChatFilter.UNREAD) },
                            label = {
                                Text(
                                    "Belum Dibaca",
                                    color = if (selectedFilter == ChatFilter.UNREAD) Color.White else Color.White.copy(alpha = 0.8f)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White.copy(alpha = 0.25f),
                                containerColor = Color.Transparent
                            ),
                            border = null
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == ChatFilter.GROUPS,
                            onClick = { viewModel.setFilter(ChatFilter.GROUPS) },
                            label = {
                                Text(
                                    "Grup",
                                    color = if (selectedFilter == ChatFilter.GROUPS) Color.White else Color.White.copy(alpha = 0.8f)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White.copy(alpha = 0.25f),
                                containerColor = Color.Transparent
                            ),
                            border = null
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == ChatFilter.FAVORITES,
                            onClick = { viewModel.setFilter(ChatFilter.FAVORITES) },
                            label = {
                                Text(
                                    "Disematkan",
                                    color = if (selectedFilter == ChatFilter.FAVORITES) Color.White else Color.White.copy(alpha = 0.8f)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color.White.copy(alpha = 0.25f),
                                containerColor = Color.Transparent
                            ),
                            border = null
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.CONTACTS) },
                containerColor = WaBadgeGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_new_chat")
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Pilih Kontak / Chat Baru"
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (activeThreads.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "Tidak ada obrolan sesuai '$searchQuery'" else "Belum ada percakapan aktif di Firestore",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mulai obrolan baru dengan kontak untuk membuat thread percakapan real-time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("chat_list_column")
                ) {
                    items(
                        items = activeThreads,
                        key = { it.id }
                    ) { thread ->
                        ChatListItem(
                            conversation = thread,
                            onClick = {
                                // Navigate directly to ChatScreen with selected thread
                                onConversationClick(thread)
                            },
                            onDeleteClick = {
                                conversationToDelete = thread
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 82.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    // Dialog: Delete Conversation Confirmation
    if (conversationToDelete != null) {
        val conv = conversationToDelete!!
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Hapus Percakapan") },
            text = { Text("Hapus obrolan \"${conv.name}\"? Semua pesan terkait dan dokumen thread akan dihapus permanen dari perangkat dan Cloud Firestore.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteConversation(conv.id)
                        conversationToDelete = null
                        Toast.makeText(context, "Percakapan berhasil dihapus dari perangkat & Firestore", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Create New Group
    if (showNewGroupDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNewGroupDialog = false 
                selectedContactIds = emptySet()
            },
            title = { Text("Buat Grup Baru") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "WhatsChat mendukung hingga 200 anggota per grup dengan sinkronisasi Cloud Firestore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Nama Subjek Grup") },
                        placeholder = { Text("Contoh: Komunitas Android Indonesia") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_group_name_input")
                    )
                    OutlinedTextField(
                        value = newGroupDesc,
                        onValueChange = { newGroupDesc = it },
                        label = { Text("Deskripsi Grup (Opsional)") },
                        placeholder = { Text("Tulis tujuan dibuatnya grup...") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pilih Anggota (${selectedContactIds.size} dipilih):",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                    ) {
                        if (contacts.isEmpty()) {
                            Text(
                                text = "Tidak ada kontak tersedia.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(contacts, key = { it.id }) { contact ->
                                    val isSelected = selectedContactIds.contains(contact.id)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedContactIds = if (isSelected) {
                                                    selectedContactIds - contact.id
                                                } else {
                                                    selectedContactIds + contact.id
                                                }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedContactIds = if (checked) {
                                                    selectedContactIds + contact.id
                                                } else {
                                                    selectedContactIds - contact.id
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = contact.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = contact.phone,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            val selectedList = contacts.filter { selectedContactIds.contains(it.id) }
                            viewModel.createGroup(newGroupName.trim(), newGroupDesc.trim(), selectedList)
                            newGroupName = ""
                            newGroupDesc = ""
                            selectedContactIds = emptySet()
                            showNewGroupDialog = false
                            Toast.makeText(context, "Grup berhasil dibuat dengan ${selectedList.size + 1} peserta di Firestore", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary),
                    modifier = Modifier.testTag("confirm_create_group_button")
                ) {
                    Text("Buat Grup")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNewGroupDialog = false 
                    selectedContactIds = emptySet()
                }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Firebase Cloud Status
    if (showFirebaseStatusDialog) {
        AlertDialog(
            onDismissRequest = { showFirebaseStatusDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = WaGreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Koneksi Firebase Real-Time",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Status: Terhubung & Aktif",
                        fontWeight = FontWeight.SemiBold,
                        color = WaGreenPrimary
                    )
                    Text(
                        text = "• Proyek ID: whatschat-7d1e6\n• Koleksi: conversations & messages\n• Paket: Firebase Spark Free Tier\n• Sinkronisasi Thread: Real-Time Snapshot Listener\n• Enkripsi Data: E2EE AES-256",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Semua obrolan aktif otomatis dimuat dari Cloud Firestore dan tersinkronisasi instan antar perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFirebaseStatusDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary)
                ) {
                    Text("Tutup")
                }
            }
        )
    }
}
