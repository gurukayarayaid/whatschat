package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.util.TimeFormatter
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contact
import com.example.ui.theme.WaBadgeGreen
import com.example.ui.theme.WaGreenDark
import com.example.ui.theme.WaGreenPrimary
import com.example.ui.theme.LocalChatColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    contacts: List<Contact>,
    onBack: () -> Unit,
    onContactClick: (Contact) -> Unit,
    onAddContact: (name: String, email: String, phone: String, status: String) -> Unit,
    onDeleteContact: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onTogglePresence: ((contactId: String, isOnline: Boolean, name: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatColors = LocalChatColors.current

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            val q = searchQuery.trim().lowercase()
            contacts.filter {
                it.name.lowercase().contains(q) ||
                        it.email.lowercase().contains(q) ||
                        it.phone.lowercase().contains(q) ||
                        it.statusMessage.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                Surface(
                    color = chatColors.headerBackground,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isSearching = false
                                searchQuery = ""
                            },
                            modifier = Modifier.testTag("close_contact_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Tutup pencarian",
                                tint = Color.White
                            )
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    "Cari nama, email (@), atau nomor...",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 15.sp
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hapus kata kunci",
                                            tint = Color.White
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("contact_search_input")
                        )
                    }
                }
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Pilih Kontak",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${contacts.size} kontak tersimpan",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("back_from_contacts_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali ke Beranda",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { isSearching = true },
                            modifier = Modifier.testTag("open_contact_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari Kontak",
                                tint = Color.White
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showOptionsMenu = true },
                                modifier = Modifier.testTag("contact_options_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Pilihan lainnya",
                                    tint = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tambah Kontak Baru") },
                                    leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        showAddContactDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Perbarui Daftar Kontak") },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                    onClick = {
                                        showOptionsMenu = false
                                        Toast.makeText(context, "Daftar kontak diperbarui (${contacts.size} kontak)", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = chatColors.headerBackground
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddContactDialog = true },
                containerColor = WaBadgeGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_contact")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Tambah Kontak Baru"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Quick Action Rows (Only when not searching)
            if (!isSearching) {
                item {
                    QuickActionRow(
                        icon = Icons.Default.GroupAdd,
                        iconBackground = WaBadgeGreen,
                        title = "Grup Baru",
                        subtitle = "Hingga 200 peserta",
                        onClick = onCreateGroupClick,
                        modifier = Modifier.testTag("action_new_group")
                    )
                }

                item {
                    QuickActionRow(
                        icon = Icons.Default.PersonAdd,
                        iconBackground = WaGreenPrimary,
                        title = "Kontak Baru",
                        subtitle = "Simpan nama, email, dan nomor telepon",
                        onClick = { showAddContactDialog = true },
                        modifier = Modifier.testTag("action_new_contact")
                    )
                }

                item {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSearching) "HASIL PENCARIAN (${filteredContacts.size})" else "KONTAK DI WHATSCHAT (${filteredContacts.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Empty State
            if (filteredContacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 40.dp, start = 32.dp, end = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = WaGreenPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ditemukan kontak untuk \"$searchQuery\"" else "Belum ada kontak tersimpan",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tambahkan kontak baru dengan alamat email agar mudah bertukar pesan dan email.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAddContactDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("empty_state_add_contact_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tambah Kontak Baru")
                        }
                    }
                }
            } else {
                items(
                    items = filteredContacts,
                    key = { it.id }
                ) { contact ->
                    ContactListItem(
                        contact = contact,
                        onChatClick = { onContactClick(contact) },
                        onSendEmailClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${contact.email}")
                                putExtra(Intent.EXTRA_SUBJECT, "Pesan dari WhatsChat")
                            }
                            runCatching {
                                context.startActivity(intent)
                            }.onFailure {
                                Toast.makeText(context, "Tidak ada aplikasi email terpasang", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeleteClick = { contactToDelete = contact },
                        onTogglePresence = onTogglePresence,
                        modifier = Modifier.testTag("contact_item_${contact.id}")
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add Contact Dialog with Email
    if (showAddContactDialog) {
        AddContactDialog(
            initialQuery = searchQuery,
            onDismiss = { showAddContactDialog = false },
            onSave = { name, email, phone, status ->
                onAddContact(name, email, phone, status)
                showAddContactDialog = false
                Toast.makeText(context, "Kontak $name ($email) berhasil disimpan!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Hapus Kontak?") },
            text = {
                Text("Apakah Anda yakin ingin menghapus ${contact.name} (${contact.email}) dari daftar kontak WhatsChat?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteContact(contact.id)
                        contactToDelete = null
                        Toast.makeText(context, "Kontak ${contact.name} telah dihapus", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun QuickActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBackground: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconBackground,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    onChatClick: () -> Unit,
    onSendEmailClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTogglePresence: ((contactId: String, isOnline: Boolean, name: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showItemMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onChatClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Initials Avatar with Realtime Presence Indicator Dot
        val initials = remember(contact.name) {
            contact.name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
                .ifEmpty { "U" }
        }

        Box(modifier = Modifier.size(48.dp)) {
            Surface(
                shape = CircleShape,
                color = Color(contact.avatarColor),
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.Center)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Realtime Online/Offline Presence Indicator Badge
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(if (contact.isOnline) Color(0xFF25D366) else Color(0xFF9E9E9E))
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Contact info column
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Email Address with badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = WaGreenPrimary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = contact.email.ifBlank { "Belum ada email" },
                    fontSize = 12.5.sp,
                    color = WaGreenPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Realtime Presence Status & Last Seen
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (contact.isOnline) Color(0xFF25D366) else Color(0xFF9E9E9E))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = TimeFormatter.formatLastSeen(contact.lastSeenTimestamp, contact.isOnline),
                    fontSize = 12.sp,
                    fontWeight = if (contact.isOnline) FontWeight.Bold else FontWeight.Normal,
                    color = if (contact.isOnline) Color(0xFF25D366) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Quick action buttons: Email & Chat & Menu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (contact.email.isNotBlank()) {
                IconButton(
                    onClick = onSendEmailClick,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("send_email_button_${contact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Kirim email ke ${contact.email}",
                        tint = WaGreenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(
                onClick = onChatClick,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("start_chat_button_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Mulai chat dengan ${contact.name}",
                    tint = WaGreenDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showItemMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("contact_item_menu_${contact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu kontak",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showItemMenu,
                    onDismissRequest = { showItemMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Mulai Chat") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) },
                        onClick = {
                            showItemMenu = false
                            onChatClick()
                        }
                    )
                    if (contact.email.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text("Kirim Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            onClick = {
                                showItemMenu = false
                                onSendEmailClick()
                            }
                        )
                    }
                    if (onTogglePresence != null) {
                        DropdownMenuItem(
                            text = { Text(if (contact.isOnline) "Ubah ke Offline (Demo)" else "Ubah ke Online (Demo)") },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (contact.isOnline) Color(0xFF9E9E9E) else Color(0xFF25D366))
                                )
                            },
                            onClick = {
                                showItemMenu = false
                                onTogglePresence(contact.id, contact.isOnline, contact.name)
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Hapus Kontak", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showItemMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddContactDialog(
    initialQuery: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, phone: String, status: String) -> Unit
) {
    var name by remember {
        mutableStateOf(if (initialQuery.contains("@")) "" else initialQuery)
    }
    var email by remember {
        mutableStateOf(if (initialQuery.contains("@")) initialQuery else "")
    }
    var phone by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ada menggunakan WhatsChat") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    fun validateAndSave() {
        var isValid = true

        if (name.trim().isBlank()) {
            nameError = "Nama kontak tidak boleh kosong"
            isValid = false
        } else {
            nameError = null
        }

        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank()) {
            emailError = "Alamat email wajib diisi"
            isValid = false
        } else if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".") || trimmedEmail.length < 5) {
            emailError = "Format email tidak valid (contoh: nama@domain.com)"
            isValid = false
        } else {
            emailError = null
        }

        val trimmedPhone = phone.trim()
        if (trimmedPhone.isBlank()) {
            phoneError = "Nomor telepon wajib diisi"
            isValid = false
        } else if (trimmedPhone.length < 5) {
            phoneError = "Nomor telepon minimal 5 digit"
            isValid = false
        } else {
            phoneError = null
        }

        if (isValid) {
            onSave(name.trim(), trimmedEmail, trimmedPhone, status.trim())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = WaGreenPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = WaGreenPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Tambah Kontak Baru", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Lengkapi detail kontak beserta alamat email untuk sinkronisasi obrolan aman E2EE.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError != null) nameError = null
                    },
                    label = { Text("Nama Lengkap *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = WaGreenPrimary) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_name")
                )

                // Email Input (Highlighted Feature)
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (emailError != null) emailError = null
                    },
                    label = { Text("Alamat Email *") },
                    placeholder = { Text("contoh@domain.id") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = WaGreenPrimary) },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                        ?: { Text("Digunakan untuk korespondensi & verifikasi", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_email")
                )

                // Phone Input
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        if (phoneError != null) phoneError = null
                    },
                    label = { Text("Nomor Telepon *") },
                    placeholder = { Text("+62 812-xxxx-xxxx") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = WaGreenPrimary) },
                    isError = phoneError != null,
                    supportingText = phoneError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_phone")
                )

                // Status Input
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Info / Bio (Opsional)") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { validateAndSave() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_status")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { validateAndSave() },
                colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary),
                modifier = Modifier.testTag("save_contact_button")
            ) {
                Text("Simpan Kontak")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_contact_button")
            ) {
                Text("Batal")
            }
        }
    )
}
