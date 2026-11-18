package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.StoredSession
import com.example.data.model.AttachmentDto
import com.example.data.model.JobCardStatus
import com.example.data.model.LabNodeMode
import com.example.data.model.SampleStatus
import com.example.data.model.TestPassFail
import com.example.data.model.TestStatus
import com.example.data.model.UrgencyLevel
import com.example.data.model.UserRole
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LabAmber
import com.example.ui.theme.LabAmberBg
import com.example.ui.theme.LabCrimson
import com.example.ui.theme.LabCrimsonBg
import com.example.ui.theme.LabEmerald
import com.example.ui.theme.LabEmeraldBg
import com.example.ui.theme.LabIndigo
import com.example.ui.theme.LabIndigoBg
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabTopAppBar(
    title: String,
    session: StoredSession,
    isServerOnline: Boolean,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = NavyPrimary,
            titleContentColor = PureWhite,
            actionIconContentColor = PureWhite
        ),
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val liveOk = if (session.nodeMode == LabNodeMode.HOST_SERVER) isServerOnline else isServerOnline
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (liveOk) Color(0xFF34D399) else Color(0xFFFBBF24))
                    )
                    Text(
                        text = "${session.role?.displayName ?: "User"} • ${session.fullName.ifBlank { session.username }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("top_refresh_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync now")
            }
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.testTag("top_theme_toggle")
            ) {
                Icon(
                    if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle light or dark mode"
                )
            }
            IconButton(
                onClick = onLogout,
                modifier = Modifier.testTag("top_logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
            }
        }
    )
}

@Composable
fun StatMetricCard(
    label: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PassFailPill(passFail: TestPassFail) {
    val (bg, fg) = when (passFail) {
        TestPassFail.PASS -> LabEmeraldBg to LabEmerald
        TestPassFail.FAIL -> LabCrimsonBg to LabCrimson
        TestPassFail.PENDING -> LabAmberBg to LabAmber
    }
    StatusPill(text = passFail.displayName, containerColor = bg, contentColor = fg)
}

@Composable
fun UrgencyPill(urgency: UrgencyLevel) {
    val (bg, fg) = when (urgency) {
        UrgencyLevel.URGENT -> LabCrimsonBg to LabCrimson
        UrgencyLevel.HIGH -> LabAmberBg to LabAmber
        UrgencyLevel.NORMAL -> LabIndigoBg to LabIndigo
    }
    StatusPill(text = urgency.displayName.uppercase(), containerColor = bg, contentColor = fg)
}

@Composable
fun JobStatusPill(status: JobCardStatus) {
    val (bg, fg) = when (status) {
        JobCardStatus.RECEIVED -> LabIndigoBg to LabIndigo
        JobCardStatus.IN_TESTING -> LabAmberBg to LabAmber
        JobCardStatus.COMPLETED -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
        JobCardStatus.PUBLISHED -> LabEmeraldBg to LabEmerald
    }
    StatusPill(text = status.displayName, containerColor = bg, contentColor = fg)
}

@Composable
fun SampleStatusPill(status: SampleStatus) {
    val (bg, fg) = when (status) {
        SampleStatus.PENDING -> LabAmberBg to LabAmber
        SampleStatus.IN_PROGRESS -> LabIndigoBg to LabIndigo
        SampleStatus.COMPLETED -> LabEmeraldBg to LabEmerald
    }
    StatusPill(text = status.displayName, containerColor = bg, contentColor = fg)
}

@Composable
fun TestStatusPill(status: TestStatus) {
    val (bg, fg) = when (status) {
        TestStatus.PENDING -> LabAmberBg to LabAmber
        TestStatus.IN_PROGRESS -> LabIndigoBg to LabIndigo
        TestStatus.COMPLETED -> LabEmeraldBg to LabEmerald
    }
    StatusPill(text = status.displayName, containerColor = bg, contentColor = fg)
}

@Composable
fun SearchBarField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun EmptyStatePanel(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LiveConnectionBanner(
    nodeMode: LabNodeMode,
    isConnected: Boolean,
    hostUrl: String,
    connectedClients: Int
) {
    val bgColor by animateColorAsState(
        targetValue = if (isConnected) Color(0xFF0F294A) else Color(0xFF451A03),
        label = "banner_bg"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bgColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = "Network Status",
                    tint = if (isConnected) CyanAccent else Color(0xFFFBBF24),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = if (nodeMode == LabNodeMode.HOST_SERVER) {
                            if (isConnected) "Local Wi-Fi Server Active" else "Local Server Offline"
                        } else {
                            if (isConnected) "WebSocket Live Sync Connected" else "Reconnecting to Lab Server"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = PureWhite
                    )
                    Text(
                        text = hostUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF93C5FD),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (nodeMode == LabNodeMode.HOST_SERVER) {
                Surface(
                    color = Color(0xFF1E3A5F),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "$connectedClients Clients",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Synced",
                    tint = if (isConnected) Color(0xFF34D399) else Color(0xFFFBBF24),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AttachmentPhotoCard(
    attachment: AttachmentDto,
    session: StoredSession,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
        ImageRequest.Builder(context)
            .data("${session.remoteHostUrl}/api/files/${attachment.id}")
            .addHeader("Authorization", "Bearer ${session.token}")
            .crossfade(true)
            .build()
    } else {
        ImageRequest.Builder(context)
            .data(File(attachment.localFilePath))
            .crossfade(true)
            .build()
    }

    Card(
        modifier = Modifier
            .width(142.dp)
            .clickable(onClick = onPreview),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = attachment.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = attachment.sourceType,
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.uploadedBy,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = attachment.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (session.role == UserRole.ADMIN) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_photo_${attachment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete file (Admin)",
                            tint = LabCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (session.role == UserRole.TECHNICIAN) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Delete restricted to Admin",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenPhotoDialog(
    attachment: AttachmentDto,
    session: StoredSession,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
        ImageRequest.Builder(context)
            .data("${session.remoteHostUrl}/api/files/${attachment.id}")
            .addHeader("Authorization", "Bearer ${session.token}")
            .build()
    } else {
        ImageRequest.Builder(context)
            .data(File(attachment.localFilePath))
            .build()
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .padding(16.dp)
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = attachment.fileName,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp)
                    .background(Color.White.copy(alpha = 0.16f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close preview", tint = PureWhite)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(attachment.fileName, style = MaterialTheme.typography.titleSmall, color = PureWhite)
                Text(
                    "Uploaded by ${attachment.uploadedBy} • ${attachment.sourceType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
    }
}
