package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tablet
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LinkedDevice
import com.example.ui.theme.LocalChatColors
import com.example.ui.theme.WaBadgeGreen
import com.example.ui.theme.WaGreenAccent
import com.example.ui.theme.WaGreenPrimary
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkedDevicesScreen(
    linkedDevices: List<LinkedDevice>,
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chatColors = LocalChatColors.current
    var showScanDialog by remember { mutableStateOf(false) }
    var isScanningHandshake by remember { mutableStateOf(false) }
    var selectedDeviceToLogout by remember { mutableStateOf<LinkedDevice?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perangkat Tertaut", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
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
            // Hero Sync Banner
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(WaGreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = WaGreenPrimary,
                                modifier = Modifier.size(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Gunakan WhatsChat di Perangkat Lain",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Sinkronisasi real-time antar platform (Web, Desktop, Tablet) dengan enkripsi ganda multi-klien.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { showScanDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("link_device_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tautkan Perangkat Baru", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Real-Time E2EE Sync Status Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = WaGreenPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Mesin Sinkronisasi Real-Time",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = WaGreenAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                                text = "Semua riwayat chat, media, dan status pesan diperbarui secara instan di semua sesi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Active Devices Header
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "STATUS PERANGKAT AKTIF (${linkedDevices.size})",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Linked Devices List
            items(linkedDevices, key = { it.id }) { dev ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDeviceToLogout = dev }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("device_item_${dev.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when {
                            dev.platform.contains("Tablet", ignoreCase = true) -> Icons.Default.Tablet
                            dev.platform.contains("Komputer", ignoreCase = true) || dev.platform.contains("Windows", ignoreCase = true) -> Icons.Default.Computer
                            else -> Icons.Default.LaptopMac
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = dev.platform,
                                tint = WaGreenPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dev.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${dev.location} • ${dev.lastActiveTime}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(WaBadgeGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = dev.syncStatus,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = WaBadgeGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = { selectedDeviceToLogout = dev },
                            modifier = Modifier.testTag("logout_device_${dev.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Keluar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 74.dp),
                    thickness = 0.5.dp,
                    color = chatColors.divider
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // QR Scan Handshake Dialog
    if (showScanDialog) {
        AlertDialog(
            onDismissRequest = {
                showScanDialog = false
                isScanningHandshake = false
            },
            title = { Text("Pindai Kode QR WhatsChat", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Buka web.whatschat.com di browser komputer atau tablet Anda, lalu arahkan kamera ke kode QR.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isScanningHandshake) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E2A30),
                            modifier = Modifier.size(180.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = WaGreenAccent,
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(color = WaGreenPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Menghubungkan kunci kriptografis E2EE & sinkronisasi database...",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (!isScanningHandshake) {
                    Button(
                        onClick = {
                            isScanningHandshake = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WaGreenPrimary)
                    ) {
                        Text("Simulasikan Pindai QR Sukses")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showScanDialog = false
                        isScanningHandshake = false
                    }
                ) {
                    Text("Batal")
                }
            }
        )

        LaunchedEffect(isScanningHandshake) {
            if (isScanningHandshake) {
                delay(1200)
                viewModel.linkDevice(
                    name = "Chrome on macOS (${(10..99).random()})",
                    platform = "Web Browser"
                )
                isScanningHandshake = false
                showScanDialog = false
            }
        }
    }

    // Logout Confirmation Dialog
    if (selectedDeviceToLogout != null) {
        val dev = selectedDeviceToLogout!!
        AlertDialog(
            onDismissRequest = { selectedDeviceToLogout = null },
            title = { Text("Keluar dari Perangkat?") },
            text = {
                Text("Apakah Anda ingin mengeluarkan akun WhatsChat dari '${dev.name}'? Sesi sinkronisasi real-time akan diputus dan kunci enkripsi sesi dihapus.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unlinkDevice(dev.id)
                        selectedDeviceToLogout = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDeviceToLogout = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
