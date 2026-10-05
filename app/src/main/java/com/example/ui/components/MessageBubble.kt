package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WaBlueTick
import com.example.ui.theme.WaGreenAccent
import com.example.ui.theme.WaGreenPrimary
import com.example.ui.theme.WaGreyTick
import com.example.util.TimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isGroup: Boolean = false,
    uploadProgress: Int? = null,
    onDeleteMessage: (Message) -> Unit = {},
    onViewCipher: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val chatColors = LocalChatColors.current
    val isOutgoing = message.isOutgoing
    var showContextMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // System message banner
    if (message.senderId == "system") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = chatColors.lockNoticeBackground,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "E2EE",
                        tint = WaGreenPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }

    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 4.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isOutgoing) 48.dp else 8.dp,
                end = if (isOutgoing) 8.dp else 48.dp,
                top = 3.dp,
                bottom = 3.dp
            ),
        contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = bubbleShape,
            color = if (isOutgoing) chatColors.sentBubble else chatColors.receivedBubble,
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(min = 90.dp, max = 340.dp)
                .testTag("message_bubble_${message.id}")
                .combinedClickable(
                    onClick = { /* normal tap */ },
                    onLongClick = {
                        showContextMenu = true
                    }
                )
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = 6.dp,
                    bottom = 6.dp
                )
            ) {
                // Sender name in group chat
                if (isGroup && !isOutgoing) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = WaGreenPrimary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Media Attachment Rendering
                when (message.type) {
                    MessageType.IMAGE -> {
                        ImageMediaCard(message = message, uploadProgress = uploadProgress)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    MessageType.VIDEO -> {
                        VideoMediaCard(message = message, uploadProgress = uploadProgress)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    MessageType.AUDIO -> {
                        AudioMediaCard(message = message)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    MessageType.DOCUMENT -> {
                        DocumentMediaCard(message = message)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    MessageType.LOCATION -> {
                        LocationMediaCard(message = message)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    MessageType.TEXT -> {}
                }

                // Message text / Caption
                if (message.text.isNotBlank() && message.type != MessageType.AUDIO && message.type != MessageType.DOCUMENT) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            lineHeight = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom Metadata: E2EE lock icon, timestamp, outgoing status checkmarks
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    // E2EE Lock Icon (tap to see cipher details)
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "E2EE",
                        tint = WaGreenPrimary.copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(11.dp)
                            .clickable { onViewCipher(message.cipherPreview) }
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = TimeFormatter.formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val isRead = message.status == MessageStatus.READ || message.seenBy.any { it != message.senderId && it != "me" }
                        val isDelivered = message.status == MessageStatus.DELIVERED || message.seenBy.isNotEmpty()
                        when {
                            isRead -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Dibaca",
                                    tint = WaBlueTick,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            isDelivered -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Diterima",
                                    tint = WaGreyTick,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            message.status == MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Terkirim",
                                    tint = WaGreyTick,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "Mengirim",
                                    tint = WaGreyTick,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Long-Press Context Menu
            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false }
            ) {
                // Delete Message Option
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Hapus Pesan",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Pesan",
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        showDeleteConfirmDialog = true
                    },
                    modifier = Modifier.testTag("menu_delete_message_${message.id}")
                )

                HorizontalDivider()

                // Copy Text Option
                if (message.text.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text("Salin Teks") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Salin"
                            )
                        },
                        onClick = {
                            showContextMenu = false
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Pesan WhatsChat", message.text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Teks pesan disalin ke papan klip", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("menu_copy_message")
                    )
                }

                // View E2EE Cipher Option
                DropdownMenuItem(
                    text = { Text("Informasi Enkripsi E2EE") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "E2EE",
                            tint = WaGreenPrimary
                        )
                    },
                    onClick = {
                        showContextMenu = false
                        onViewCipher(message.cipherPreview)
                    }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Pesan",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Hapus Pesan?")
            },
            text = {
                Text(
                    "Pesan ini akan dihapus permanen dari basis data lokal (Room) dan disinkronkan secara real-time ke Cloud Firestore."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteMessage(message)
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text(
                        text = "Hapus",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ImageMediaCard(message: Message, uploadProgress: Int?) {
    val context = LocalContext.current
    var showFullPreview by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E2A30))
            .clickable { showFullPreview = true }
            .testTag("media_image_${message.id}"),
        contentAlignment = Alignment.Center
    ) {
        val mediaUri = message.mediaUri
        val isValidUri = mediaUri != null && (
            mediaUri.startsWith("content://") ||
            mediaUri.startsWith("file://") ||
            mediaUri.startsWith("http://") ||
            mediaUri.startsWith("https://")
        )

        if (isValidUri) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaUri)
                    .crossfade(true)
                    .build(),
                contentDescription = message.mediaCaption ?: "Foto Berbagi Cepat",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = WaGreenAccent,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                error = {
                    DefaultImagePlaceholder(caption = message.mediaCaption, fileSize = message.mediaSize)
                }
            )
        } else {
            DefaultImagePlaceholder(caption = message.mediaCaption, fileSize = message.mediaSize)
        }

        // Fast upload progress indicator
        if (uploadProgress != null && uploadProgress < 100) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = { uploadProgress / 100f },
                        color = WaGreenAccent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mengompresi & Mengirim $uploadProgress%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    if (showFullPreview) {
        AlertDialog(
            onDismissRequest = { showFullPreview = false },
            confirmButton = {
                TextButton(onClick = { showFullPreview = false }) {
                    Text("Tutup", color = WaGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = message.mediaCaption ?: "Foto Terenkripsi E2EE",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val mediaUri = message.mediaUri
                    val isValidUri = mediaUri != null && (
                        mediaUri.startsWith("content://") ||
                        mediaUri.startsWith("file://") ||
                        mediaUri.startsWith("http://") ||
                        mediaUri.startsWith("https://")
                    )
                    if (isValidUri) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(mediaUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto Penuh",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else {
                        DefaultImagePlaceholder(caption = message.mediaCaption, fileSize = message.mediaSize)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${message.mediaSize ?: "2.1 MB"} • Dilindungi Enkripsi Kriptografi E2EE",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
fun VideoMediaCard(message: Message, uploadProgress: Int?) {
    val context = LocalContext.current
    var showVideoDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141E24))
            .clickable { showVideoDialog = true }
            .testTag("media_video_${message.id}"),
        contentAlignment = Alignment.Center
    ) {
        val mediaUri = message.mediaUri
        val isValidUri = mediaUri != null && (
            mediaUri.startsWith("content://") ||
            mediaUri.startsWith("file://") ||
            mediaUri.startsWith("http://") ||
            mediaUri.startsWith("https://")
        )

        if (isValidUri) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaUri)
                    .crossfade(true)
                    .build(),
                contentDescription = message.mediaCaption ?: "Video",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    DefaultVideoPlaceholder(caption = message.mediaCaption)
                }
            )
        } else {
            DefaultVideoPlaceholder(caption = message.mediaCaption)
        }

        // Semi-transparent dark overlay with Play button
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar Video",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        // Video badges (Duration bottom-start)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = message.mediaDuration ?: "0:15",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    if (message.mediaSize != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• ${message.mediaSize}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        // Fast upload progress indicator
        if (uploadProgress != null && uploadProgress < 100) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = { uploadProgress / 100f },
                        color = WaGreenAccent,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Mengompresi & Mengirim Video $uploadProgress%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    if (showVideoDialog) {
        AlertDialog(
            onDismissRequest = { showVideoDialog = false },
            confirmButton = {
                TextButton(onClick = { showVideoDialog = false }) {
                    Text("Tutup", color = WaGreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = message.mediaCaption ?: "Pemutar Video E2EE",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1E1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = WaGreenAccent,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${message.mediaCaption ?: "Video Terenkripsi"} (${message.mediaDuration ?: "0:15"}, ${message.mediaSize ?: "4.2 MB"})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "🔒 Video dilindungi enkripsi ujung-ke-ujung E2EE dan siap diputar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

@Composable
private fun DefaultImagePlaceholder(caption: String?, fileSize: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Color(0xFF0F3B32),
                size = Size(w, h),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawCircle(
                color = Color(0xFF128C7E).copy(alpha = 0.5f),
                radius = h * 0.7f,
                center = Offset(w * 0.8f, h * 0.2f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = "Foto Media",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = caption ?: "Foto Beresolusi Tinggi",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            if (fileSize != null) {
                Text(
                    text = "$fileSize • Enkripsi E2EE Cepat",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                )
            }
        }
    }
}

@Composable
private fun DefaultVideoPlaceholder(caption: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Color(0xFF1C2D35),
                size = Size(w, h),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawCircle(
                color = Color(0xFF00A884).copy(alpha = 0.35f),
                radius = h * 0.6f,
                center = Offset(w * 0.75f, h * 0.3f)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = "Video",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = caption ?: "Video E2EE",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
fun AudioMediaCard(message: Message) {
    var isPlaying by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play / Pause Circle
        IconButton(
            onClick = { isPlaying = !isPlaying },
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WaGreenPrimary)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Jeda" else "Putar",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Waveform Visualizer
        Column(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                val bars = 24
                val barWidth = 3.dp.toPx()
                val space = (size.width - (bars * barWidth)) / (bars - 1)
                val heights = listOf(0.3f, 0.6f, 0.9f, 0.4f, 0.7f, 0.5f, 0.8f, 1.0f, 0.6f, 0.4f, 0.7f, 0.9f, 0.5f, 0.8f, 0.3f, 0.6f, 0.7f, 0.4f, 0.8f, 0.5f, 0.3f, 0.7f, 0.6f, 0.4f)

                for (i in 0 until bars) {
                    val h = heights[i % heights.size] * size.height
                    val x = i * (barWidth + space)
                    val y = (size.height - h) / 2
                    val activeBar = if (isPlaying) i < bars / 2 else false
                    drawRoundRect(
                        color = if (activeBar) WaGreenPrimary else Color.Gray.copy(alpha = 0.5f),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = message.mediaDuration ?: "0:38",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = WaGreenPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Pesan Suara E2EE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentMediaCard(message: Message) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE53935)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Dokumen",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.text.ifBlank { "Dokumen_Terenkripsi.pdf" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = "${message.mediaSize ?: "1.8 MB"} • PDF • E2EE Enkripsi Penuh",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LocationMediaCard(message: Message) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0288D1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Lokasi",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lokasi Terkini Dibagikan",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Jakarta Pusat, Indonesia • Akurasi 10m",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
