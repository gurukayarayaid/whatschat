package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WaGreenDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onRegisterAndLogin: (phone: String, name: String, status: String) -> Unit,
    onLogin: (phone: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Login, 1: Register New Number

    // Login state
    var loginPhone by remember { mutableStateOf("+62 812-3456-7890") }

    // Register state
    var regPhone by remember { mutableStateOf("+62 813-") }
    var regName by remember { mutableStateOf("") }
    var regStatus by remember { mutableStateOf("Tersedia di WhatsChat E2EE") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WhatsChat Autentikasi", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WaGreenDark)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(WaGreenDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Selamat Datang di WhatsChat",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Kelola akun nomor telepon dan enkripsi E2EE",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = WaGreenDark
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Masuk (Login)") },
                    modifier = Modifier.testTag("tab_login")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Daftar Nomor Baru") },
                    modifier = Modifier.testTag("tab_register")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedTab == 0) {
                // Login Form
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = loginPhone,
                        onValueChange = { loginPhone = it },
                        label = { Text("Nomor Telepon Akun") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_login_phone")
                    )

                    Button(
                        onClick = {
                            if (loginPhone.isBlank()) {
                                Toast.makeText(context, "Masukkan nomor telepon", Toast.LENGTH_SHORT).show()
                            } else {
                                onLogin(loginPhone.trim())
                                Toast.makeText(context, "Berhasil masuk dengan $loginPhone", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WaGreenDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_login_submit")
                    ) {
                        Text("Masuk", fontSize = 16.sp, color = Color.White)
                    }
                }
            } else {
                // Register New Number Form
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = regPhone,
                        onValueChange = { regPhone = it },
                        label = { Text("Nomor Telepon Baru (+62...)") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_phone")
                    )

                    OutlinedTextField(
                        value = regName,
                        onValueChange = { regName = it },
                        label = { Text("Nama Lengkap / Profil") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_name")
                    )

                    OutlinedTextField(
                        value = regStatus,
                        onValueChange = { regStatus = it },
                        label = { Text("Status / Bio") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_reg_status")
                    )

                    Button(
                        onClick = {
                            if (regPhone.isBlank() || regName.isBlank()) {
                                Toast.makeText(context, "Nomor telepon dan nama wajib diisi", Toast.LENGTH_SHORT).show()
                            } else {
                                onRegisterAndLogin(regPhone.trim(), regName.trim(), regStatus.trim())
                                Toast.makeText(context, "Nomor baru berhasil didaftarkan & masuk!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WaGreenDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_register_submit")
                    ) {
                        Text("Daftar & Masuk", fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
