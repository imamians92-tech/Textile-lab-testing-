package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.NetworkUtils
import com.example.data.model.LabNodeMode
import com.example.data.model.UserRole
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LabCrimson
import com.example.ui.theme.LabCrimsonBg
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite

@Composable
fun LoginScreen(
    initialMode: LabNodeMode,
    initialRemoteUrl: String,
    isBusy: Boolean,
    onLogin: (username: String, password: String, mode: LabNodeMode, remoteUrl: String, onError: (String) -> Unit) -> Unit,
    onTestRemoteHost: (String) -> Unit
) {
    var selectedRoleTab by remember { mutableStateOf(UserRole.TECHNICIAN) }
    var username by remember { mutableStateOf("tech1") }
    var password by remember { mutableStateOf("tech123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var nodeMode by remember { mutableStateOf(initialMode) }
    var remoteUrl by remember { mutableStateOf(initialRemoteUrl) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val localIp = remember { NetworkUtils.getLocalWifiIpAddress() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Precision Textile Lab Hero Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(225.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_lab_banner),
                    contentDescription = "Textile Laboratory Weave Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    NavyPrimary.copy(alpha = 0.72f),
                                    NavyPrimary.copy(alpha = 0.94f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanAccent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Textile Lab Pro",
                                tint = CyanAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "TEXTILE LAB PRO",
                                style = MaterialTheme.typography.headlineMedium,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Quality Control • Local Wi-Fi Lab Server",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF93C5FD)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Separate Login Role Tabs: Admin / Technician / Guest
                Text(
                    text = "Select Access Portal",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    UserRole.entries.forEachIndexed { index, role ->
                        SegmentedButton(
                            selected = selectedRoleTab == role,
                            onClick = {
                                selectedRoleTab = role
                                errorMessage = null
                                when (role) {
                                    UserRole.ADMIN -> {
                                        username = "admin"
                                        password = ""
                                    }
                                    UserRole.TECHNICIAN -> {
                                        username = "tech1"
                                        password = ""
                                    }
                                    UserRole.GUEST -> {
                                        username = "guest"
                                        password = ""
                                    }
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = UserRole.entries.size
                            ),
                            icon = {
                                Icon(
                                    imageVector = when (role) {
                                        UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                        UserRole.TECHNICIAN -> Icons.Default.Engineering
                                        UserRole.GUEST -> Icons.Default.Person
                                    },
                                    contentDescription = role.displayName,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("role_tab_${role.name.lowercase()}")
                        ) {
                            Text(role.displayName, maxLines = 1)
                        }
                    }
                }

                // Role Capability Summary Banner
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = when (selectedRoleTab) {
                            UserRole.ADMIN -> "Admin Portal: Full lab control, user management, SQLite backups, audit logs, and exclusive file deletion rights."
                            UserRole.TECHNICIAN -> "Technician Portal: Create jobs & samples, run all 9 textile tests, upload photos, and publish reports directly. File deletion is blocked by server."
                            UserRole.GUEST -> "Guest Portal: Read-only access to search Job/Report numbers, inspect buyer details, Pass/Fail verdicts, and report photos."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Login Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "${selectedRoleTab.displayName} Sign In",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                errorMessage = null
                            },
                            label = { Text("Username") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input")
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle password visibility"
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_password_input")
                        )

                        AnimatedVisibility(visible = errorMessage != null) {
                            Surface(
                                color = LabCrimsonBg,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = LabCrimson,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter username and password"
                                } else {
                                    onLogin(username, password, nodeMode, remoteUrl) { err ->
                                        errorMessage = err
                                    }
                                }
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Sign In as ${selectedRoleTab.displayName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = PureWhite
                                )
                            }
                        }

                        // Quick autofill buttons for initial seeded accounts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val (presetUser, presetPass) = when (selectedRoleTab) {
                                UserRole.ADMIN -> "admin" to "admin123"
                                UserRole.TECHNICIAN -> "tech1" to "tech123"
                                UserRole.GUEST -> "guest" to "guest123"
                            }
                            OutlinedButton(
                                onClick = {
                                    username = presetUser
                                    password = presetPass
                                    errorMessage = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("autofill_default_credentials")
                            ) {
                                Text("Fill Default ($presetUser / $presetPass)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Local Wi-Fi Server / Client Mode Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Router, contentDescription = null, tint = NavyContainer)
                            Text("Wi-Fi Lab Network Role", style = MaterialTheme.typography.titleSmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = nodeMode == LabNodeMode.HOST_SERVER,
                                onClick = { nodeMode = LabNodeMode.HOST_SERVER },
                                label = { Text("Host Server (Local DB)") },
                                leadingIcon = { Icon(Icons.Default.Router, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyPrimary,
                                    selectedLabelColor = PureWhite,
                                    selectedLeadingIconColor = CyanAccent
                                )
                            )
                            FilterChip(
                                selected = nodeMode == LabNodeMode.CLIENT_NODE,
                                onClick = { nodeMode = LabNodeMode.CLIENT_NODE },
                                label = { Text("Client Device") },
                                leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyPrimary,
                                    selectedLabelColor = PureWhite,
                                    selectedLeadingIconColor = CyanAccent
                                )
                            )
                        }
                        if (nodeMode == LabNodeMode.HOST_SERVER) {
                            Text(
                                text = "This device hosts SQLite + Ktor HTTP/WS Server on http://$localIp:${NetworkUtils.DEFAULT_SERVER_PORT} (No internet needed)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = remoteUrl,
                                    onValueChange = { remoteUrl = it },
                                    label = { Text("Host Server URL") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { onTestRemoteHost(remoteUrl) },
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    Text("Ping")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
