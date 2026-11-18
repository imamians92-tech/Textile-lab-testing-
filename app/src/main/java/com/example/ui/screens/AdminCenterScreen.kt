package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.NetworkUtils
import com.example.data.model.CreateUserRequest
import com.example.data.model.LabNodeMode
import com.example.data.model.UserRole
import com.example.ui.LabUiState
import com.example.ui.components.StatusPill
import com.example.ui.theme.LabCrimson
import com.example.ui.theme.LabCrimsonBg
import com.example.ui.theme.LabEmerald
import com.example.ui.theme.LabEmeraldBg
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite

@Composable
fun AdminCenterScreen(
    state: LabUiState,
    onToggleServer: (Boolean) -> Unit,
    onCreateUser: (CreateUserRequest, (Boolean) -> Unit) -> Unit,
    onToggleUser: (Long, Boolean) -> Unit,
    onRunBackup: () -> Unit,
    onDeleteBackup: (Long) -> Unit
) {
    var selectedSection by remember { mutableStateOf("SERVER") } // SERVER, USERS, BACKUPS, AUDIT
    var showCreateUserDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "SERVER" to "Wi-Fi Server",
                "USERS" to "Users (${state.users.size})",
                "BACKUPS" to "Backups",
                "AUDIT" to "Audit Logs"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = selectedSection == key,
                    onClick = { selectedSection = key },
                    label = { Text(label) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedSection) {
                "SERVER" -> {
                    item {
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
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text("Embedded Ktor Wi-Fi Server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    if (state.session.nodeMode == LabNodeMode.HOST_SERVER) {
                                        Switch(
                                            checked = state.isServerRunning,
                                            onCheckedChange = onToggleServer,
                                            modifier = Modifier.testTag("server_toggle_switch")
                                        )
                                    }
                                }
                                Text(
                                    text = "Endpoint: ${if (state.session.nodeMode == LabNodeMode.HOST_SERVER) state.serverUrl else state.session.remoteHostUrl}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "WebSocket Stream: /ws/updates • Connected Client Devices: ${state.connectedClients}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = "Engine: Ktor CIO (Coroutine-based, optimized for 4GB RAM / 64GB storage Infinix host serving 20+ Wi-Fi client devices without internet).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = LabEmerald)
                                    Text("Server-Side Security Policy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Text("• Passwords hashed with PBKDF2-HMAC-SHA256 + 16-byte random salt", style = MaterialTheme.typography.bodySmall)
                                Text("• DELETE /api/files/{id} strictly enforces ADMIN role (Technicians get HTTP 403 Forbidden)", style = MaterialTheme.typography.bodySmall)
                                Text("• GUEST role is strictly read-only on published reports", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                "USERS" -> {
                    item {
                        Button(
                            onClick = { showCreateUserDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.fillMaxWidth().testTag("create_user_button")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PureWhite)
                            Text(" Create Staff / Guest Account", color = PureWhite)
                        }
                    }
                    items(state.users, key = { it.id }) { user ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${user.fullName} (@${user.username})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Role: ${user.role.displayName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatusPill(
                                        text = if (user.isActive) "ACTIVE" else "DISABLED",
                                        containerColor = if (user.isActive) LabEmeraldBg else LabCrimsonBg,
                                        contentColor = if (user.isActive) LabEmerald else LabCrimson
                                    )
                                    if (user.username != state.session.username) {
                                        Switch(
                                            checked = user.isActive,
                                            onCheckedChange = { onToggleUser(user.id, it) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                "BACKUPS" -> {
                    item {
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
                                    Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Automatic Daily SQLite Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "WorkManager executes a WAL-checkpointed SQLite + Photo ZIP backup every 24 hours automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onRunBackup,
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                    modifier = Modifier.fillMaxWidth().testTag("manual_backup_button")
                                ) {
                                    Text("Create Manual SQLite Backup Now", color = PureWhite)
                                }
                            }
                        }
                    }
                    items(state.backups, key = { it.id }) { backup ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(backup.fileName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${backup.backupType} • ${NetworkUtils.formatBytes(backup.sizeBytes)} • ${NetworkUtils.formatDateTime(backup.createdAt)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteBackup(backup.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete backup", tint = LabCrimson)
                                }
                            }
                        }
                    }
                }

                "AUDIT" -> {
                    items(state.auditLogs, key = { it.id }) { log ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${log.action} • @${log.actorUsername} (${log.actorRole})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusPill(
                                        text = log.status,
                                        containerColor = if (log.status == "ALLOWED") LabEmeraldBg else LabCrimsonBg,
                                        contentColor = if (log.status == "ALLOWED") LabEmerald else LabCrimson
                                    )
                                }
                                Text(log.details, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = NetworkUtils.formatDateTime(log.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateUserDialog) {
        CreateUserDialog(
            onDismiss = { showCreateUserDialog = false },
            onSave = { req ->
                onCreateUser(req) { ok ->
                    if (ok) showCreateUserDialog = false
                }
            }
        )
    }
}

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onSave: (CreateUserRequest) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.TECHNICIAN) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Lab User") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 6 chars) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Role", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UserRole.entries.forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.displayName) }
                        )
                    }
                }
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isBlank() || fullName.isBlank() || password.length < 6) {
                        error = "Enter username, full name, and password (6+ chars)."
                    } else {
                        onSave(CreateUserRequest(username, fullName, password, role))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Create User", color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
