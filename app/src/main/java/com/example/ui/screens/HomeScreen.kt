package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ChatViewModel,
    conversations: List<Conversation>,
    selectedFilter: ChatFilter,
    themeMode: AppThemeMode,
    isSearching: Boolean,
    searchQuery: String,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var showFirebaseStatusDialog by remember { mutableStateOf(false) }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var newGroupDesc by remember { mutableStateOf("") }

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
                                placeholder = { Text("Cari chat, grup, atau pesan...", color = Color.White.copy(alpha = 0.8f)) },
                                singleLine = true,
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
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "E2EE",
                                    tint = WaGreenAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        },
                        actions = {
                            // Quick Theme Switcher
                            IconButton(
                                onClick = { viewModel.toggleThemeQuick() },
                                modifier = Modifier.testTag("toggle_theme_quick_button")
                            ) {
                                Icon(
                                    imageVector = if (themeMode == AppThemeMode.DARK) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Ganti Mode Tema",
                                    tint = Color.White
                                )
                            }

                            // Simulated Push Notification trigger button
                            IconButton(
                                onClick = { viewModel.simulateIncomingPush() },
                                modifier = Modifier.testTag("simulate_push_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Uji Push Notifikasi",
                                    tint = Color.White
                                )
                            }

                            // Search Icon
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

                            // More Menu
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.testTag("more_menu_button")
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
                                    text = { Text("Status Firebase Online (Real-Time)") },
                                    leadingIcon = { Icon(Icons.Default.CloudDone, contentDescription = null, tint = WaGreenPrimary) },
                                    onClick = {
                                        showMenu = false
                                        showFirebaseStatusDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_item_firebase_status")
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

                // Filter Pills: Semua, Belum Dibaca, Favorit, Grup
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
                            label = { Text("Semua", color = if (selectedFilter == ChatFilter.ALL) Color.White else Color.White.copy(alpha = 0.8f)) },
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
                            label = { Text("Belum Dibaca", color = if (selectedFilter == ChatFilter.UNREAD) Color.White else Color.White.copy(alpha = 0.8f)) },
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
                            label = { Text("Grup (Maks 200)", color = if (selectedFilter == ChatFilter.GROUPS) Color.White else Color.White.copy(alpha = 0.8f)) },
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
                            label = { Text("Disematkan", color = if (selectedFilter == ChatFilter.FAVORITES) Color.White else Color.White.copy(alpha = 0.8f)) },
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
            if (conversations.isEmpty()) {
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
                        text = if (searchQuery.isNotBlank()) "Tidak ada chat yang sesuai dengan '$searchQuery'" else "Belum ada obrolan",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        val matchingContact = remember(contacts, conv) {
                            contacts.firstOrNull { it.id == conv.id || it.conversationId == conv.id || it.name.equals(conv.name, ignoreCase = true) }
                        }
                        ChatListItem(
                            conversation = conv,
                            isOnline = matchingContact?.isOnline ?: false,
                            onClick = { onOpenConversation(conv.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 82.dp),
                            thickness = 0.5.dp,
                            color = chatColors.divider
                        )
                    }

                    // Bottom info disclaimer on E2EE
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 24.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pesan pribadi dan grup dienkripsi secara end-to-end",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }

    // New Group Dialog (support up to 200 members)
    if (showNewGroupDialog) {
        AlertDialog(
            onDismissRequest = { showNewGroupDialog = false },
            title = {
                Text(
                    text = "Buat Grup Baru (Maks 200 Anggota)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Grup WhatsChat dilengkapi dengan proteksi enkripsi E2EE, peran admin intuitif, dan batas kapasitas 200 peserta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Nama Grup") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newGroupDesc,
                        onValueChange = { newGroupDesc = it },
                        label = { Text("Deskripsi Grup") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newGroupName.isNotBlank()) {
                            showNewGroupDialog = false
                            viewModel.createGroup(newGroupName, newGroupDesc)
                            newGroupName = ""
                            newGroupDesc = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary)
                ) {
                    Text("Buat Grup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGroupDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Firebase Cloud Realtime Status Dialog
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
                        text = "• Proyek ID: whatschat-7d1e6\n• Akun: media.belajar.aguk.id@gmail.com\n• Paket: Firebase Spark (100% Gratis)\n• Database: Cloud Firestore (Real-Time Synchronized)\n• Paket Enkripsi: E2EE AES-256 Cloud Preserved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Pesan yang dikirim langsung disinkronkan ke Cloud Firestore secara instan dan dapat diterima oleh perangkat lain secara online.",
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
