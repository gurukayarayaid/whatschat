package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.Conversation
import com.example.data.model.Participant
import com.example.ui.components.E2EESecurityDialog
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WaBadgeGreen
import com.example.ui.theme.WaGreenAccent
import com.example.ui.theme.WaGreenPrimary
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    conversation: Conversation?,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (conversation == null) return

    val chatColors = LocalChatColors.current
    val uploadProgressMap by viewModel.groupAvatarUploadProgress.collectAsStateWithLifecycle()
    val currentUploadProgress = uploadProgressMap[conversation.id]

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadGroupAvatar(conversation.id, uri)
        }
    }

    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberName by remember { mutableStateOf("") }
    var newMemberPhone by remember { mutableStateOf("") }
    var newMemberEmail by remember { mutableStateOf("") }
    var selectedParticipantForAction by remember { mutableStateOf<Participant?>(null) }
    var showInviteLinkDialog by remember { mutableStateOf(false) }
    var showE2EEDialog by remember { mutableStateOf(false) }
    var copiedLinkToast by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Info Grup", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showInviteLinkDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Undang via Tautan",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = chatColors.headerBackground
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Group Hero Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Group Avatar with Firebase Storage Upload & Camera Badge
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .testTag("group_avatar_upload_container"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Color(conversation.avatarColor))
                                    .border(2.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!conversation.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = conversation.avatarUrl,
                                        contentDescription = "Foto Profil Grup ${conversation.name}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(52.dp)
                                    )
                                }

                                // Upload Progress Indicator
                                if (currentUploadProgress != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.65f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { (currentUploadProgress.toFloat() / 100f).coerceIn(0.05f, 1f) },
                                            modifier = Modifier.size(46.dp),
                                            color = Color.White,
                                            strokeWidth = 3.dp
                                        )
                                        Text(
                                            text = "$currentUploadProgress%",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }

                            // Camera Action Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(WaGreenPrimary)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Ganti Foto Profil Grup",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (currentUploadProgress != null) "Mengunggah ke Firebase Storage ($currentUploadProgress%)..." else "Ketuk untuk mengubah foto profil grup",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = if (currentUploadProgress != null) WaGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = conversation.name,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Grup • ${conversation.participantCount} peserta (Maks. 200)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Capacity Bar (up to 200)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Kapasitas Grup",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${conversation.participantCount} / ${conversation.maxParticipants} Peserta",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = WaGreenPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { conversation.participantCount.toFloat() / conversation.maxParticipants },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = WaGreenPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        if (conversation.groupDescription.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = conversation.groupDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // E2EE Notice Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showE2EEDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = WaGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enkripsi End-to-End",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pesan dan panggilan dienkripsi secara end-to-end. Ketuk untuk memverifikasi kode keamanan 60-digit grup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Admin Permission Controls Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = WaGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Izin & Pengaturan Admin",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hanya Admin yang dapat mengirim pesan",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Mode pengumuman siaran searah",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = conversation.onlyAdminsCanSend,
                                onCheckedChange = { checked ->
                                    viewModel.updateGroupPermissions(conversation.id, checked, conversation.onlyAdminsCanEdit)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = WaGreenPrimary)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hanya Admin yang dapat mengedit info grup",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Nama grup, ikon, dan deskripsi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = conversation.onlyAdminsCanEdit,
                                onCheckedChange = { checked ->
                                    viewModel.updateGroupPermissions(conversation.id, conversation.onlyAdminsCanSend, checked)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = WaGreenPrimary)
                            )
                        }
                    }
                }
            }

            // Participant Header & Add Actions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${conversation.participantCount} Peserta (Batas 200)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tersisa: ${200 - conversation.participantCount} slot",
                                style = MaterialTheme.typography.labelSmall,
                                color = WaGreenPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Add Participant Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddMemberDialog = true }
                                .padding(vertical = 8.dp)
                                .testTag("add_participant_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(WaGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tambah",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Tambah peserta baru",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = WaGreenPrimary
                                )
                            )
                        }

                        // Invite via Link Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showInviteLinkDialog = true }
                                .padding(vertical = 8.dp)
                                .testTag("invite_via_link_button"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "Tautan",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Undang ke grup via tautan",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // Participant List
            items(conversation.participants, key = { it.id }) { participant ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!participant.isCurrentUser) {
                                    selectedParticipantForAction = participant
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("participant_item_${participant.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(participant.avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = participant.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (participant.isCurrentUser) "${participant.name} (Anda)" else participant.name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = participant.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (participant.email.isNotBlank()) {
                                Text(
                                    text = participant.email,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = WaGreenPrimary,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (participant.isAdmin) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = WaGreenPrimary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "Admin Grup",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = WaGreenPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Add Member Dialog
    if (showAddMemberDialog) {
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Tambah Peserta ke Grup", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Kapasitas saat ini: ${conversation.participantCount}/200 peserta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        label = { Text("Nama Kontak") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMemberPhone,
                        onValueChange = { newMemberPhone = it },
                        label = { Text("Nomor Telepon (misal: +62 812-xxxx-xxxx)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMemberEmail,
                        onValueChange = { newMemberEmail = it },
                        label = { Text("Alamat Email (opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMemberName.isNotBlank()) {
                            viewModel.addParticipant(
                                convId = conversation.id,
                                name = newMemberName,
                                phone = newMemberPhone.ifBlank { "+62 812-9876-5432" },
                                email = newMemberEmail.trim()
                            )
                            showAddMemberDialog = false
                            newMemberName = ""
                            newMemberPhone = ""
                            newMemberEmail = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary)
                ) {
                    Text("Tambahkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMemberDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Participant Action Dialog (Admin Promotion/Demotion & Removal)
    if (selectedParticipantForAction != null) {
        val p = selectedParticipantForAction!!
        AlertDialog(
            onDismissRequest = { selectedParticipantForAction = null },
            title = { Text(p.name, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Pengaturan peran admin untuk ${p.name} (${p.phone}):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Promote / Demote
                    Button(
                        onClick = {
                            viewModel.toggleAdmin(conversation.id, p.id)
                            selectedParticipantForAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (p.isAdmin) "Hapus Sebagai Admin" else "Jadikan Admin Grup")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Remove member
                    Button(
                        onClick = {
                            viewModel.removeParticipant(conversation.id, p.id)
                            selectedParticipantForAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Keluarkan dari Grup")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedParticipantForAction = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Invite Link Dialog
    if (showInviteLinkDialog) {
        AlertDialog(
            onDismissRequest = { showInviteLinkDialog = false },
            icon = { Icon(Icons.Default.Link, contentDescription = null, tint = WaGreenPrimary) },
            title = { Text("Tautan Undangan Grup", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Siapa pun yang memiliki WhatsChat dapat menggunakan tautan ini untuk bergabung ke grup ini (maks. 200 peserta). Bagikan hanya kepada orang yang Anda percayai.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "https://chat.whatschat.com/invite/${conversation.id.takeLast(8)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = WaGreenPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    if (copiedLinkToast) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "✓ Tautan berhasil disalin ke papan klip!",
                            style = MaterialTheme.typography.labelSmall,
                            color = WaGreenPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        copiedLinkToast = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salin Tautan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteLinkDialog = false }) {
                    Text("Selesai")
                }
            }
        )
    }

    // E2EE Security Dialog
    if (showE2EEDialog) {
        E2EESecurityDialog(
            contactName = conversation.name,
            securityCode = conversation.securityCode,
            onDismiss = { showE2EEDialog = false }
        )
    }
}
