package com.example.ui.components

import android.net.Uri
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.ui.theme.WaBlueAccent
import com.example.ui.theme.WaBlueLight
import com.example.ui.theme.WaBluePrimary
import java.io.File

/**
 * MediaSelectionComponent:
 * Provides zero-permission photo picker capabilities complying with Google Play policy
 * and Material 3 design, preparing the app for fast media sharing functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaSelectionSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onMediaSelected: (uri: Uri, caption: String, isVideo: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State for media picked to prepare fast media sharing
    var pendingSelectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isPendingVideo by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Modern Zero-Permission Android Photo Picker (Single Image)
    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri) ?: ""
            isPendingVideo = mime.startsWith("video", ignoreCase = true)
            pendingSelectedUris = listOf(uri)
        }
    }

    // Modern Zero-Permission Android Photo Picker (Multiple Images)
    val multiplePhotosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            isPendingVideo = false
            pendingSelectedUris = uris
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            isPendingVideo = false
            pendingSelectedUris = listOf(tempCameraUri!!)
        }
    }

    fun launchCamera() {
        try {
            val file = File.createTempFile("whatschat_cam_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (_: Exception) {}
    }

    // Fast Media Sharing Preparation Dialog
    if (pendingSelectedUris.isNotEmpty()) {
        FastMediaSharingPreviewDialog(
            uris = pendingSelectedUris,
            isVideo = isPendingVideo,
            onDismiss = {
                pendingSelectedUris = emptyList()
            },
            onConfirmSend = { primaryUri, caption ->
                onMediaSelected(primaryUri, caption, isPendingVideo)
                pendingSelectedUris = emptyList()
                onDismiss()
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("media_selection_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Pilih Media & Berbagi Cepat",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enkripsi End-to-End • Berbagi Gambar Instan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_media_selection_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grid of media selection choices
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MediaChoiceItem(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Galeri Foto",
                    subtitle = "1 Gambar",
                    badgeColor = WaBluePrimary,
                    testTag = "choice_single_photo",
                    onClick = {
                        singlePhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                MediaChoiceItem(
                    icon = Icons.Default.Collections,
                    title = "Banyak Foto",
                    subtitle = "Hingga 10 foto",
                    badgeColor = Color(0xFF0284C7),
                    testTag = "choice_multi_photo",
                    onClick = {
                        multiplePhotosPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )

                MediaChoiceItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Ambil Foto",
                    subtitle = "Kamera Cepat",
                    badgeColor = Color(0xFF03A9F4),
                    testTag = "choice_camera",
                    onClick = {
                        launchCamera()
                    }
                )

                MediaChoiceItem(
                    icon = Icons.Default.Videocam,
                    title = "Koleksi Video",
                    subtitle = "Video HD",
                    badgeColor = Color(0xFF38BDF8),
                    testTag = "choice_video",
                    onClick = {
                        singlePhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notice about E2EE and fast sharing
            Surface(
                color = WaBlueLight.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = WaBluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Foto dan media langsung dienkripsi sebelum dikirim dan disimpan ke cloud yang aman.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MediaChoiceItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * FastMediaSharingPreviewDialog:
 * Previews the picked image(s), allows captioning, quality tags, and 1-tap fast sending.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FastMediaSharingPreviewDialog(
    uris: List<Uri>,
    isVideo: Boolean,
    onDismiss: () -> Unit,
    onConfirmSend: (selectedUri: Uri, caption: String) -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    val currentUri = uris.getOrElse(selectedIndex) { uris.first() }
    var captionText by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var isHdQuality by remember { mutableStateOf(true) }

    val quickTags = listOf("Penting", "Dokumen", "Bukti Pembayaran", "Kegiatan", "Pribadi")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("preview_cancel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Batal",
                            tint = Color.White
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isHdQuality) WaBluePrimary else Color.DarkGray,
                            modifier = Modifier.clickable { isHdQuality = !isHdQuality }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = "HD",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isHdQuality) "HD Aktif" else "Standar",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x33FFFFFF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "E2EE",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AES-256", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Main Image Preview Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = currentUri,
                        contentDescription = "Media Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("fast_media_preview_image")
                    )
                }

                // If multiple photos picked, show thumbnails row
                if (uris.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uris.indices.toList()) { index ->
                            val uri = uris[index]
                            val isSelected = index == selectedIndex
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) WaBluePrimary else Color.Gray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedIndex = index }
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Quick tags suggestions
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickTags.forEach { tag ->
                        val isSelected = selectedTag == tag
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) WaBluePrimary else Color(0x33FFFFFF),
                            modifier = Modifier
                                .clickable {
                                    if (isSelected) {
                                        selectedTag = null
                                    } else {
                                        selectedTag = tag
                                        if (!captionText.contains(tag)) {
                                            captionText = if (captionText.isBlank()) "[$tag] " else "$captionText [$tag]"
                                        }
                                    }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Bottom Input & Fast Send Bar
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = captionText,
                            onValueChange = { captionText = it },
                            placeholder = {
                                Text(
                                    text = "Tambahkan keterangan foto...",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WaBluePrimary,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("media_caption_input")
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        // Fast Media Send Button
                        IconButton(
                            onClick = {
                                val finalCaption = captionText.ifBlank {
                                    if (isVideo) "Video Media (Terenkripsi E2EE)" else "Foto Media (Terenkripsi E2EE)"
                                }
                                onConfirmSend(currentUri, finalCaption)
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(WaBluePrimary)
                                .testTag("fast_send_media_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Kirim Media Cepat",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
