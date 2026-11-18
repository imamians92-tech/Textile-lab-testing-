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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.LabNodeMode
import com.example.data.model.UserRole
import com.example.ui.LabUiState
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.JobStatusPill
import com.example.ui.components.LiveConnectionBanner
import com.example.ui.components.PassFailPill
import com.example.ui.components.StatMetricCard
import com.example.ui.components.UrgencyPill
import com.example.ui.theme.LabAmber
import com.example.ui.theme.LabCrimson
import com.example.ui.theme.LabEmerald
import com.example.ui.theme.LabIndigo
import com.example.ui.theme.SkyAccent

@Composable
fun DashboardScreen(
    state: LabUiState,
    onNavigateTab: (String) -> Unit
) {
    val stats = state.stats
    val isConnected = if (state.session.nodeMode == LabNodeMode.HOST_SERVER) {
        state.isServerRunning
    } else {
        state.wsClientConnected
    }
    val hostDisplayUrl = if (state.session.nodeMode == LabNodeMode.HOST_SERVER) {
        state.serverUrl
    } else {
        state.session.remoteHostUrl
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LiveConnectionBanner(
                nodeMode = state.session.nodeMode,
                isConnected = isConnected,
                hostUrl = hostDisplayUrl,
                connectedClients = state.connectedClients
            )
        }

        if (state.session.role != UserRole.GUEST) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        label = "Job Cards",
                        value = stats.totalJobs.toString(),
                        subtitle = "${stats.urgentJobs} urgent open",
                        icon = Icons.Default.WorkOutline,
                        accentColor = SkyAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab("jobs") }
                    )
                    StatMetricCard(
                        label = "Samples",
                        value = stats.totalSamples.toString(),
                        subtitle = "${stats.pendingSamples} pending • ${stats.completedSamples} done",
                        icon = Icons.Default.Inventory2,
                        accentColor = LabIndigo,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab("samples") }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        label = "9-Module Tests",
                        value = "${stats.completedTests}/${stats.completedTests + stats.pendingTests}",
                        subtitle = "${stats.pendingTests} tests queued",
                        icon = Icons.Default.Biotech,
                        accentColor = LabAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab("testing") }
                    )
                    StatMetricCard(
                        label = "Published Reports",
                        value = stats.publishedReports.toString(),
                        subtitle = "${stats.passedReports} PASS • ${stats.failedReports} FAIL",
                        icon = Icons.Default.AssignmentTurnedIn,
                        accentColor = LabEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab("reports") }
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMetricCard(
                        label = "Published Reports",
                        value = stats.publishedReports.toString(),
                        subtitle = "Verified lab releases",
                        icon = Icons.Default.FactCheck,
                        accentColor = SkyAccent,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab("reports") }
                    )
                    StatMetricCard(
                        label = "PASS / FAIL",
                        value = "${stats.passedReports} / ${stats.failedReports}",
                        subtitle = "Overall lab outcomes",
                        icon = Icons.Default.Science,
                        accentColor = LabEmerald,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab("reports") }
                    )
                }
            }
        }

        if (state.session.role != UserRole.GUEST) {
            item {
                Text(
                    text = "Active Job Queue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (state.jobs.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Default.WorkOutline,
                        title = "No Job Cards Received Yet",
                        description = "Go to Jobs to receive a new textile testing job card and assign samples."
                    )
                }
            } else {
                items(state.jobs.take(4), key = { it.id }) { job ->
                    Card(
                        onClick = { onNavigateTab("jobs") },
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
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = job.jobNumber,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    UrgencyPill(job.urgency)
                                }
                                Text(
                                    text = "${job.customerName} • Buyer: ${job.buyerName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Report #${job.reportNumber} • ${job.quantity}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            JobStatusPill(job.status)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Latest Published Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        val publishedList = state.reports.filter { it.isPublished }.take(5)
        if (publishedList.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Default.PriorityHigh,
                    title = "No Published Reports Yet",
                    description = "Completed reports published by Technicians or Admin will appear here immediately."
                )
            }
        } else {
            items(publishedList, key = { "rep_${it.id}" }) { report ->
                Card(
                    onClick = { onNavigateTab("reports") },
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
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${report.reportNumber} (${report.jobNumber})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${report.customerName} • ${report.reportDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Published by ${report.publishedBy} • ${report.attachments.size} photo(s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PassFailPill(report.overallResult)
                    }
                }
            }
        }
    }
}
