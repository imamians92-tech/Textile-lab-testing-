package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.model.JobCardStatus
import com.example.data.model.JobDto
import com.example.data.model.TextileTestType
import com.example.data.model.UpsertJobRequest
import com.example.data.model.UrgencyLevel
import com.example.ui.LabUiState
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.JobStatusPill
import com.example.ui.components.SearchBarField
import com.example.ui.components.UrgencyPill
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite

@Composable
fun JobsScreen(
    state: LabUiState,
    onSaveJob: (UpsertJobRequest, (Boolean) -> Unit) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf<JobCardStatus?>(null) }
    var editingJob by remember { mutableStateOf<JobDto?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val canEdit = state.session.role?.canEditLabData == true

    val filteredJobs = remember(state.jobs, searchQuery, statusFilter) {
        state.jobs.filter { job ->
            val matchesQuery = searchQuery.isBlank() ||
                job.jobNumber.contains(searchQuery, ignoreCase = true) ||
                job.reportNumber.contains(searchQuery, ignoreCase = true) ||
                job.customerName.contains(searchQuery, ignoreCase = true) ||
                job.buyerName.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == null || job.status == statusFilter
            matchesQuery && matchesStatus
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SearchBarField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search Job #, Report #, Customer or Buyer...",
                    testTag = "jobs_search_input"
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { statusFilter = null },
                        label = { Text("All (${state.jobs.size})") }
                    )
                    JobCardStatus.entries.forEach { st ->
                        val count = state.jobs.count { it.status == st }
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text("${st.displayName} ($count)") }
                        )
                    }
                }
            }

            if (filteredJobs.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStatePanel(
                        icon = Icons.Default.WorkOutline,
                        title = "No Matching Jobs",
                        description = if (canEdit) "Tap + Receive Job to log a new textile job card." else "No jobs match your filter."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredJobs, key = { it.id }) { job ->
                        val jobSamples = state.samples.filter { it.jobId == job.id }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = job.jobNumber,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        UrgencyPill(job.urgency)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        JobStatusPill(job.status)
                                        if (canEdit) {
                                            IconButton(
                                                onClick = {
                                                    editingJob = job
                                                    showCreateDialog = true
                                                },
                                                modifier = Modifier.testTag("edit_job_${job.id}")
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit job")
                                            }
                                        }
                                    }
                                }
                                Text(
                                    text = "Report #: ${job.reportNumber} • Date: ${job.receivedDate}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Customer: ${job.customerName} | Buyer: ${job.buyerName}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Qty: ${job.quantity} • Samples: ${jobSamples.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (job.processes.isNotEmpty()) {
                                    Text(
                                        text = "Processes: ${job.processes.joinToString(" • ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (job.remarks.isNotBlank()) {
                                    Text(
                                        text = "Remarks: ${job.remarks}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (canEdit) {
            FloatingActionButton(
                onClick = {
                    editingJob = null
                    showCreateDialog = true
                },
                containerColor = NavyPrimary,
                contentColor = PureWhite,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_job_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Receive New Job")
            }
        }
    }

    if (showCreateDialog) {
        JobUpsertDialog(
            existing = editingJob,
            suggestedIndex = state.jobs.size + 1,
            onDismiss = { showCreateDialog = false },
            onSave = { req ->
                onSaveJob(req) { ok ->
                    if (ok) showCreateDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JobUpsertDialog(
    existing: JobDto?,
    suggestedIndex: Int,
    onDismiss: () -> Unit,
    onSave: (UpsertJobRequest) -> Unit
) {
    val today = remember { NetworkUtils.formatDateOnly(System.currentTimeMillis()) }
    var jobNumber by remember { mutableStateOf(existing?.jobNumber ?: "JOB-2026-%03d".format(suggestedIndex)) }
    var reportNumber by remember { mutableStateOf(existing?.reportNumber ?: "RPT-2026-%03d".format(suggestedIndex)) }
    var customer by remember { mutableStateOf(existing?.customerName ?: "") }
    var buyer by remember { mutableStateOf(existing?.buyerName ?: "") }
    var quantity by remember { mutableStateOf(existing?.quantity ?: "") }
    var date by remember { mutableStateOf(existing?.receivedDate ?: today) }
    var remarks by remember { mutableStateOf(existing?.remarks ?: "") }
    var urgency by remember { mutableStateOf(existing?.urgency ?: UrgencyLevel.NORMAL) }
    var status by remember { mutableStateOf(existing?.status ?: JobCardStatus.RECEIVED) }
    var selectedProcesses by remember {
        mutableStateOf(
            existing?.processes?.toSet() ?: setOf("GSM", "Width", "Shrinkage", "Washing")
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Receive New Job Card" else "Edit Job Card") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = jobNumber,
                    onValueChange = { jobNumber = it },
                    label = { Text("Job Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("job_number_input")
                )
                OutlinedTextField(
                    value = reportNumber,
                    onValueChange = { reportNumber = it },
                    label = { Text("Report Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("report_number_input")
                )
                OutlinedTextField(
                    value = customer,
                    onValueChange = { customer = it },
                    label = { Text("Customer / Mill *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_input")
                )
                OutlinedTextField(
                    value = buyer,
                    onValueChange = { buyer = it },
                    label = { Text("Buyer / Brand") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity *") },
                        placeholder = { Text("e.g. 500 m") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("quantity_input")
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Urgency", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    UrgencyLevel.entries.forEach { level ->
                        FilterChip(
                            selected = urgency == level,
                            onClick = { urgency = level },
                            label = { Text(level.displayName) }
                        )
                    }
                }

                if (existing != null) {
                    Text("Job Card Status", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        JobCardStatus.entries.forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.displayName) }
                            )
                        }
                    }
                }

                Text("Testing Processes", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextileTestType.entries.forEach { proc ->
                        val selected = proc.displayName in selectedProcesses
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedProcesses = if (selected) {
                                    selectedProcesses - proc.displayName
                                } else {
                                    selectedProcesses + proc.displayName
                                }
                            },
                            label = { Text(proc.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jobNumber.isBlank() || reportNumber.isBlank() || customer.isBlank() || quantity.isBlank()) {
                        validationError = "Job Number, Report Number, Customer, and Quantity are required."
                    } else {
                        onSave(
                            UpsertJobRequest(
                                id = existing?.id ?: 0L,
                                jobNumber = jobNumber,
                                reportNumber = reportNumber,
                                customerName = customer,
                                buyerName = buyer.ifBlank { customer },
                                quantity = quantity,
                                receivedDate = date,
                                processes = selectedProcesses.toList(),
                                urgency = urgency,
                                remarks = remarks,
                                status = status
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("save_job_button")
            ) {
                Text("Save Job", color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
