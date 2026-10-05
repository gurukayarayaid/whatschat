package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.MessageType
import com.example.ui.theme.WaGreenAccent
import com.example.ui.theme.WaGreenPrimary
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    onSendMessage: (text: String, type: MessageType, mediaUri: String?, caption: String?, size: String?, duration: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("") }
    var showAttachSheet by remember { mutableStateOf(false) }
    var showQuickCameraSheet by remember { mutableStateOf(false) }
    var showMediaSelectionSheet by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    var permissionRationaleMessage by remember { mutableStateOf<String?>(null) }
    var pendingCameraMode by remember { mutableStateOf<String?>(null) } // "photo" or "video"
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempVideoUri by remember { mutableStateOf<Uri?>(null) }

    // Camera Photo Capture Launcher
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            onSendMessage(
                "",
                MessageType.IMAGE,
                tempPhotoUri.toString(),
                "Foto Kamera (Terenkripsi E2EE)",
                "2.2 MB",
                null
            )
        }
    }

    // Camera Video Capture Launcher
    val recordVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempVideoUri != null) {
            onSendMessage(
                "",
                MessageType.VIDEO,
                tempVideoUri.toString(),
                "Video Kamera (Terenkripsi E2EE)",
                "6.5 MB",
                "0:15"
            )
        }
    }

    // Gallery Picker Launcher (Photos & Videos)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val isVideo = mimeType.startsWith("video", ignoreCase = true) || uri.toString().endsWith(".mp4", ignoreCase = true)
            if (isVideo) {
                onSendMessage(
                    "",
                    MessageType.VIDEO,
                    uri.toString(),
                    "Video Galeri (Terenkripsi E2EE)",
                    "7.4 MB",
                    "0:32"
                )
            } else {
                onSendMessage(
                    "",
                    MessageType.IMAGE,
                    uri.toString(),
                    "Foto Galeri (Terenkripsi E2EE)",
                    "2.8 MB",
                    null
                )
            }
        }
    }

    // File creators for Camera capture
    fun launchPhotoCamera() {
        try {
            val file = File.createTempFile("whatschat_photo_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempPhotoUri = uri
            takePhotoLauncher.launch(uri)
        } catch (e: Exception) {
            // Fallback
            onSendMessage("", MessageType.IMAGE, null, "Foto Kamera", "1.9 MB", null)
        }
    }

    fun launchVideoCamera() {
        try {
            val file = File.createTempFile("whatschat_video_${System.currentTimeMillis()}", ".mp4", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempVideoUri = uri
            recordVideoLauncher.launch(uri)
        } catch (e: Exception) {
            // Fallback
            onSendMessage("", MessageType.VIDEO, null, "Video Kamera", "5.8 MB", "0:15")
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (pendingCameraMode == "video") {
                launchVideoCamera()
            } else {
                launchPhotoCamera()
            }
        } else {
            permissionRationaleMessage = "Izin kamera diperlukan untuk mengambil foto atau merekam video langsung dalam obrolan WhatsChat."
        }
        pendingCameraMode = null
    }

    fun requestCamera(isVideo: Boolean) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (isVideo) launchVideoCamera() else launchPhotoCamera()
        } else {
            pendingCameraMode = if (isVideo) "video" else "photo"
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Storage/Gallery Permission Launcher
    var pendingGalleryPickType by remember { mutableStateOf<String?>(null) } // "image", "video", "any"
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val request = when (pendingGalleryPickType) {
            "image" -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            "video" -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            else -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        }
        mediaPickerLauncher.launch(request)
        pendingGalleryPickType = null
    }

    fun requestGallery(type: String) {
        pendingGalleryPickType = type
        val request = when (type) {
            "image" -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            "video" -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            else -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perms = when (type) {
                "image" -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                "video" -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                else -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            }
            val allGranted = perms.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                mediaPickerLauncher.launch(request)
            } else {
                galleryPermissionLauncher.launch(perms)
            }
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                mediaPickerLauncher.launch(request)
            } else {
                galleryPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRecordingAudio) {
                // Recording Mode Bar
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val transition = rememberInfiniteTransition(label = "rec")
                            val alpha by transition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(600),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "blink"
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red.copy(alpha = alpha))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Merekam suara 0:${recordingSeconds.toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = { isRecordingAudio = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Batal",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Finish recording send button
                IconButton(
                    onClick = {
                        val dur = "0:${recordingSeconds.toString().padStart(2, '0')}"
                        isRecordingAudio = false
                        onSendMessage(
                            "Pesan Suara",
                            MessageType.AUDIO,
                            "recorded_audio",
                            null,
                            "${recordingSeconds * 12} KB",
                            dur
                        )
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WaGreenPrimary)
                        .testTag("send_voice_note_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Kirim Suara",
                        tint = Color.White
                    )
                }
            } else {
                // Regular Text / Media Input Bar
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { textInput += "😊" },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mood,
                                contentDescription = "Emoji",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    text = "Ketik pesan...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            maxLines = 4,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_text_field")
                        )

                        // Attachment button (Paperclip)
                        IconButton(
                            onClick = { showAttachSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("attach_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Lampiran Media",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Fast Camera button
                        IconButton(
                            onClick = { showMediaSelectionSheet = true },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("camera_quick_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Kamera / Galeri",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Send Button or Mic Button
                if (textInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val toSend = textInput
                            textInput = ""
                            onSendMessage(toSend, MessageType.TEXT, null, null, null, null)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(WaGreenPrimary)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Kirim",
                            tint = Color.White
                        )
                    }
                } else {
                    IconButton(
                        onClick = { isRecordingAudio = true },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(WaGreenPrimary)
                            .testTag("record_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Rekam Pesan Suara",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Quick Camera Sheet (Capture Photo, Record Video, or Open Gallery)
    if (showQuickCameraSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickCameraSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Kamera & Galeri Media",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachOptionItem(
                        icon = Icons.Default.CameraAlt,
                        title = "Ambil Foto",
                        bgColor = Color(0xFFD3396D),
                        onClick = {
                            showQuickCameraSheet = false
                            requestCamera(isVideo = false)
                        }
                    )
                    AttachOptionItem(
                        icon = Icons.Default.Videocam,
                        title = "Rekam Video",
                        bgColor = Color(0xFFE91E63),
                        onClick = {
                            showQuickCameraSheet = false
                            requestCamera(isVideo = true)
                        }
                    )
                    AttachOptionItem(
                        icon = Icons.Default.Image,
                        title = "Galeri Foto",
                        bgColor = Color(0xFFAC44CF),
                        onClick = {
                            showQuickCameraSheet = false
                            requestGallery("image")
                        }
                    )
                    AttachOptionItem(
                        icon = Icons.Default.Videocam,
                        title = "Galeri Video",
                        bgColor = Color(0xFF7B1FA2),
                        onClick = {
                            showQuickCameraSheet = false
                            requestGallery("video")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Modal Attachment Sheet
    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Berbagi Media Cepat (Enkripsi End-to-End)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachOptionItem(
                        icon = Icons.Default.CameraAlt,
                        title = "Kamera Foto",
                        bgColor = Color(0xFFD3396D),
                        onClick = {
                            showAttachSheet = false
                            showMediaSelectionSheet = true
                        }
                    )
                    AttachOptionItem(
                        icon = Icons.Default.Videocam,
                        title = "Kamera Video",
                        bgColor = Color(0xFFE91E63),
                        onClick = {
                            showAttachSheet = false
                            requestCamera(isVideo = true)
                        }
                    )
                    AttachOptionItem(
                        icon = Icons.Default.Image,
                        title = "Galeri Foto",
                        bgColor = Color(0xFF0288D1),
                        onClick = {
                            showAttachSheet = false
                            showMediaSelectionSheet = true
                        }
                    )
                    AttachOptionItem(
                        icon = Icons.Default.Videocam,
                        title = "Galeri Video",
                        bgColor = Color(0xFF7B1FA2),
                        onClick = {
                            showAttachSheet = false
                            requestGallery("video")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachOptionItem(
                        icon = Icons.Default.Description,
                        title = "Dokumen",
                        bgColor = Color(0xFF5F66CD),
                        onClick = {
                            showAttachSheet = false
                            onSendMessage(
                                "Dokumen_Rencana_Proyek_v3.pdf",
                                MessageType.DOCUMENT,
                                "doc_uri",
                                null,
                                "3.4 MB",
                                null
                            )
                        }
                    )
                    AttachOptionItem(
                        icon = Icons.Default.Mic,
                        title = "Audio",
                        bgColor = Color(0xFFE67E22),
                        onClick = {
                            showAttachSheet = false
                            onSendMessage(
                                "Pesan Rekaman Suara Studio",
                                MessageType.AUDIO,
                                "audio_file",
                                null,
                                "890 KB",
                                "1:15"
                            )
                        }
                    )
                    AttachOptionItem(
                        icon = Icons.Default.LocationOn,
                        title = "Lokasi",
                        bgColor = Color(0xFF27AE60),
                        onClick = {
                            showAttachSheet = false
                            onSendMessage(
                                "Lokasi Terkini",
                                MessageType.LOCATION,
                                "geo:-6.2088,106.8456",
                                null,
                                null,
                                null
                            )
                        }
                    )
                    AttachOptionItem(
                        icon = Icons.Default.ContactPhone,
                        title = "Kontak",
                        bgColor = Color(0xFF009688),
                        onClick = {
                            showAttachSheet = false
                            onSendMessage(
                                "Budi Santoso (+62 812-4455-6677)",
                                MessageType.TEXT,
                                null,
                                null,
                                null,
                                null
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Permission Rationale / Denied Dialog
    if (permissionRationaleMessage != null) {
        AlertDialog(
            onDismissRequest = { permissionRationaleMessage = null },
            title = {
                Text(
                    text = "Izin Diperlukan",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = permissionRationaleMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { permissionRationaleMessage = null }) {
                    Text(
                        text = "Mengerti",
                        color = WaGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Media Selection Component (Photo Picker & Fast Media Sharing)
    MediaSelectionSheet(
        isOpen = showMediaSelectionSheet,
        onDismiss = { showMediaSelectionSheet = false },
        onMediaSelected = { uri, caption, isVideo ->
            val type = if (isVideo) MessageType.VIDEO else MessageType.IMAGE
            val sizeStr = if (isVideo) "6.8 MB" else "2.4 MB"
            val durationStr = if (isVideo) "0:25" else null
            onSendMessage(
                "",
                type,
                uri.toString(),
                caption,
                sizeStr,
                durationStr
            )
        }
    )
}

@Composable
private fun AttachOptionItem(
    icon: ImageVector,
    title: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
