package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WaBadgeGreen
import com.example.ui.theme.WaGreenAccent
import com.example.ui.theme.WaGreenPrimary
import com.example.ui.viewmodel.ChatViewModel

/**
 * ProfileScreen:
 * Allows users to view their email, set their display name, status,
 * and upload/set a profile avatar stored in Firebase Storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatColors = LocalChatColors.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    // Dialog & Sheet States
    var showAvatarBottomSheet by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditEmailDialog by remember { mutableStateOf(false) }
    var showEditStatusDialog by remember { mutableStateOf(false) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var showColorPaletteDialog by remember { mutableStateOf(false) }

    // Form Temporary values
    var tempDisplayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var tempEmail by remember(profile.email) { mutableStateOf(profile.email) }
    var tempStatus by remember(profile.status) { mutableStateOf(profile.status) }
    var tempPhone by remember(profile.phoneNumber) { mutableStateOf(profile.phoneNumber) }

    // Upload state
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableIntStateOf(0) }
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Android Photo Picker launcher (zero-permission standard for modern Android)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploadingAvatar = true
            uploadProgress = 0
            uploadStatusMessage = "Mengunggah avatar ke Firebase Storage..."

            viewModel.uploadAvatarToFirebaseStorage(
                uri = uri,
                onProgress = { progress ->
                    uploadProgress = progress
                },
                onSuccess = { downloadUrl ->
                    isUploadingAvatar = false
                    uploadStatusMessage = null
                    Toast.makeText(
                        context,
                        "Foto profil berhasil diunggah ke Firebase Storage!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { errorMsg ->
                    isUploadingAvatar = false
                    uploadStatusMessage = null
                    Toast.makeText(
                        context,
                        "Info: $errorMsg",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    val presetColors = listOf(
        0xFF128C7E, // WA Teal
        0xFF075E54, // WA Dark Green
        0xFF25D366, // WA Light Green
        0xFF0088CC, // Telegram Blue
        0xFFE91E63, // Pink
        0xFF9C27B0, // Purple
        0xFFFF9800, // Orange
        0xFF3F51B5  // Indigo
    )

    val quickStatuses = listOf(
        "Ada menggunakan WhatsChat E2EE",
        "Sibuk, hanya menerima chat",
        "Di kantor / sekolah",
        "Sedang rapat penting",
        "Baterai hampir habis",
        "Hanya pesan singkat"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Profil",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = WaGreenAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Firebase Storage & Firestore Sinkron",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("profile_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Pengaturan",
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
            // Avatar Showcase Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .testTag("profile_avatar_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Avatar Circular Container
                        if (!profile.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(profile.avatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Foto Profil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, WaGreenPrimary, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(CircleShape)
                                    .background(Color(profile.avatarColor))
                                    .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.displayName.firstOrNull()?.uppercase() ?: "U",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        // Uploading Progress Overlay
                        if (isUploadingAvatar) {
                            Box(
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        progress = { uploadProgress / 100f },
                                        color = WaGreenAccent,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$uploadProgress%",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Camera / Edit Avatar Button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(WaBadgeGreen)
                                .clickable { showAvatarBottomSheet = true }
                                .testTag("avatar_picker_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Ganti Foto Profil",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Upload feedback info
                    AnimatedVisibility(visible = isUploadingAvatar) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(
                                text = "Menyimpan ke Firebase Storage...",
                                style = MaterialTheme.typography.bodySmall,
                                color = WaGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { uploadProgress / 100f },
                                modifier = Modifier
                                    .width(180.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = WaGreenPrimary
                            )
                        }
                    }
                }
            }

            // Display Name Section
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempDisplayName = profile.displayName
                                showEditNameDialog = true
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = WaGreenPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nama",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ini bukan nama pengguna atau PIN Anda. Nama ini akan terlihat oleh kontak WhatsChat Anda.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                        IconButton(
                            onClick = {
                                tempDisplayName = profile.displayName
                                showEditNameDialog = true
                            },
                            modifier = Modifier.testTag("edit_display_name_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Nama",
                                tint = WaGreenPrimary
                            )
                        }
                    }
                }
            }

            // User Email Section (Primary Account info)
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempEmail = profile.email
                                showEditEmailDialog = true
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = WaGreenPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Email Akun",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(WaGreenPrimary.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Terverifikasi",
                                        tint = WaGreenPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Terhubung",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = WaGreenPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.email,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Email akun Anda terhubung untuk autentikasi Google Firebase, sinkronisasi kontak, dan cadangan cloud.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                            )
                        }
                        IconButton(
                            onClick = {
                                tempEmail = profile.email
                                showEditEmailDialog = true
                            },
                            modifier = Modifier.testTag("edit_email_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Ubah Email",
                                tint = WaGreenPrimary
                            )
                        }
                    }
                }
            }

            // Info / Status (About) Section
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempStatus = profile.status
                                showEditStatusDialog = true
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = WaGreenPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Info (Status)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.status,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                tempStatus = profile.status
                                showEditStatusDialog = true
                            },
                            modifier = Modifier.testTag("edit_status_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Status",
                                tint = WaGreenPrimary
                            )
                        }
                    }
                }
            }

            // Phone Number Section
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempPhone = profile.phoneNumber
                                showEditPhoneDialog = true
                            }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = WaGreenPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nomor Telepon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.phoneNumber,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                tempPhone = profile.phoneNumber
                                showEditPhoneDialog = true
                            },
                            modifier = Modifier.testTag("edit_phone_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Telepon",
                                tint = WaGreenPrimary
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Cloud Storage Metadata Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = WaGreenPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Penyimpanan Cloud WhatsChat",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Bucket Avatar: whatschat-7d1e6.firebasestorage.app\n" +
                                "• Jalur Firestore: users/me\n" +
                                "• Keamanan: Enkripsi E2EE AES-256\n" +
                                "• Foto profil diunggah langsung ke Google Firebase Storage dan tersinkronisasi ke seluruh perangkat tertaut.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.updateDisplayName(profile.displayName)
                                Toast.makeText(
                                    context,
                                    "Profil berhasil disinkronkan ke Cloud Firestore!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("sync_profile_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sinkronkan Profil Sekarang")
                        }
                    }
                }
            }

            // Logout & Sign Out Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                var showLogoutDialog by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showLogoutDialog = true }
                        .testTag("profile_logout_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Log Out",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keluar Akun (Log Out)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Hapus sesi lokal, keluar dari Firebase Auth & kembali ke Login",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (showLogoutDialog) {
                    AlertDialog(
                        onDismissRequest = { showLogoutDialog = false },
                        title = { Text("Konfirmasi Keluar Akun") },
                        text = { Text("Apakah Anda yakin ingin keluar? Sesi lokal dan Firebase Auth Anda akan diakhiri dan dialihkan kembali ke layar Login.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showLogoutDialog = false
                                    viewModel.logout()
                                    Toast.makeText(context, "Berhasil keluar dari akun", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.testTag("confirm_logout_button")
                            ) {
                                Text("Keluar", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLogoutDialog = false }) {
                                Text("Batal")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Modal BottomSheet for Avatar Options
    if (showAvatarBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAvatarBottomSheet = false },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Foto Profil",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Option 1: Galeri (Photo Picker)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showAvatarBottomSheet = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("action_pick_gallery")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WaGreenPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Galeri",
                                tint = WaGreenPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Galeri Foto",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    // Option 2: Warna Inisial
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                showAvatarBottomSheet = false
                                showColorPaletteDialog = true
                            }
                            .testTag("action_pick_color")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WaGreenPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Warna Inisial",
                                tint = WaGreenPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pilih Warna",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    // Option 3: Hapus Foto Profil (if custom image exists)
                    if (!profile.avatarUrl.isNullOrBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    showAvatarBottomSheet = false
                                    viewModel.updateAvatarUrl(null)
                                    Toast.makeText(context, "Foto profil dihapus", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("action_delete_avatar")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Hapus Foto",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog: Edit Display Name
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Masukkan Nama Anda") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempDisplayName,
                        onValueChange = {
                            if (it.length <= 25) tempDisplayName = it
                        },
                        label = { Text("Nama Tampilan") },
                        singleLine = true,
                        supportingText = {
                            Text("${25 - tempDisplayName.length} karakter tersisa")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempDisplayName.isNotBlank()) {
                            viewModel.updateDisplayName(tempDisplayName)
                            showEditNameDialog = false
                            Toast.makeText(context, "Nama tampilan berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary),
                    modifier = Modifier.testTag("save_name_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Edit Email
    if (showEditEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEditEmailDialog = false },
            title = { Text("Alamat Email Akun") },
            text = {
                Column {
                    Text(
                        text = "Masukkan alamat email yang valid untuk identitas akun WhatsChat dan integrasi Cloud Anda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text("Email Akun") },
                        placeholder = { Text("contoh@gmail.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempEmail.contains("@") && tempEmail.contains(".")) {
                            viewModel.updateEmail(tempEmail)
                            showEditEmailDialog = false
                            Toast.makeText(context, "Alamat email berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Format email tidak valid", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary),
                    modifier = Modifier.testTag("save_email_button")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditEmailDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Edit Status (About)
    if (showEditStatusDialog) {
        AlertDialog(
            onDismissRequest = { showEditStatusDialog = false },
            title = { Text("Pilih atau Tulis Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempStatus,
                        onValueChange = { tempStatus = it },
                        label = { Text("Info Pribadi") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_status_input")
                    )

                    Text(
                        text = "Pilih dari status siap pakai:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                    )

                    LazyColumn(
                        modifier = Modifier.height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(quickStatuses) { presetStatus ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { tempStatus = presetStatus }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = presetStatus,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (tempStatus == presetStatus) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = WaGreenPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempStatus.isNotBlank()) {
                            viewModel.updateStatus(tempStatus)
                            showEditStatusDialog = false
                            Toast.makeText(context, "Status info berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditStatusDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Edit Phone Number
    if (showEditPhoneDialog) {
        AlertDialog(
            onDismissRequest = { showEditPhoneDialog = false },
            title = { Text("Ubah Nomor Telepon") },
            text = {
                OutlinedTextField(
                    value = tempPhone,
                    onValueChange = { tempPhone = it },
                    label = { Text("Nomor Telepon") },
                    placeholder = { Text("+62 812-3456-7890") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempPhone.isNotBlank()) {
                            viewModel.updatePhoneNumber(tempPhone)
                            showEditPhoneDialog = false
                            Toast.makeText(context, "Nomor telepon berhasil diperbarui", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary)
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPhoneDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Pick Avatar Palette Color
    if (showColorPaletteDialog) {
        AlertDialog(
            onDismissRequest = { showColorPaletteDialog = false },
            title = { Text("Pilih Warna Inisial Avatar") },
            text = {
                Column {
                    Text(
                        text = "Warna ini akan digunakan saat tidak menggunakan foto profil:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetColors) { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .border(
                                        width = if (profile.avatarColor == colorHex) 3.dp else 1.dp,
                                        color = if (profile.avatarColor == colorHex) WaGreenPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.updateAvatarColor(colorHex)
                                        viewModel.updateAvatarUrl(null) // reset to colored initial
                                        showColorPaletteDialog = false
                                        Toast.makeText(context, "Warna avatar diperbarui", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (profile.avatarColor == colorHex) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPaletteDialog = false }) {
                    Text("Selesai")
                }
            }
        )
    }
}
