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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CreateSampleRequest
import com.example.data.model.JobDto
import com.example.data.model.SampleDto
import com.example.data.model.SampleStatus
import com.example.data.model.TextileTestType
import com.example.ui.LabUiState
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.SampleStatusPill
import com.example.ui.components.SearchBarField
import com.example.ui.theme.LabAmber
import com.example.ui.theme.LabEmerald
import com.example.ui.theme.LabIndigo
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite

@Composable
fun SamplesScreen(
    state: LabUiState,
    onCreateSample: (CreateSampleRequest, (Boolean) -> Unit) -> Unit,
    onUpdateSampleStatus: (Long, SampleStatus, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var groupByMode by remember { mutableStateOf("SAMPLE") } // SAMPLE, JOB, TECHNICIAN
    var statusFilter by remember { mutableStateOf<SampleStatus?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val canEdit = state.session.role?.canEditLabData == true
    val pendingCount = state.samples.count { it.status != SampleStatus.COMPLETED }
    val completedCount = state.samples.count { it.status == SampleStatus.COMPLETED }

    val filtered = remember(state.samples, searchQuery, statusFilter) {
        state.samples.filter { s ->
            val q = searchQuery.isBlank() ||
                s.sampleCode.contains(searchQuery, ignoreCase = true) ||
                s.jobNumber.contains(searchQuery, ignoreCase = true) ||
                s.assignedTechnician.contains(searchQuery, ignoreCase = true) ||
                s.fabricDescription.contains(searchQuery, ignoreCase = true)
            val st = statusFilter == null || s.status == statusFilter
            q && st
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
                // Pending & Completed Counts Summary Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Pending / Active", style = MaterialTheme.typography.labelMedium, color = LabAmber)
                            Text("$pendingCount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Completed Samples", style = MaterialTheme.typography.labelMedium, color = LabEmerald)
                            Text("$completedCount", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                SearchBarField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search Sample ID, Job #, Technician or Fabric...",
                    testTag = "samples_search_input"
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SAMPLE" to "Sample-wise", "JOB" to "Job-wise", "TECH" to "Technician-wise").forEach { (key, label) ->
                        FilterChip(
                            selected = groupByMode == key,
                            onClick = { groupByMode = key },
                            label = { Text(label) }
                        )
                    }
                    SampleStatus.entries.forEach { st ->
                        FilterChip(
                            selected = statusFilter == st,
                            onClick = { statusFilter = if (statusFilter == st) null else st },
                            label = { Text(st.displayName) }
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStatePanel(
                        icon = Icons.Default.Inventory2,
                        title = "No Samples Tracked",
                        description = if (state.jobs.isEmpty()) {
                            "Receive a Job first in Jobs tab, then register fabric samples with automatic unique Sample IDs."
                        } else {
                            "Tap + Register Sample to auto-generate a unique Sample ID and queue its textile tests."
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (groupByMode) {
                        "JOB" -> {
                            val grouped = filtered.groupBy { it.jobNumber }
                            grouped.forEach { (jobNo, list) ->
                                item(key = "hdr_job_$jobNo") {
                                    Text(
                                        text = "$jobNo • ${list.size} sample(s) (${list.count { it.status == SampleStatus.COMPLETED }} done)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = LabIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(list, key = { it.id }) { sample ->
                                    SampleCard(sample, state, canEdit, onUpdateSampleStatus)
                                }
                            }
                        }
                        "TECH" -> {
                            val grouped = filtered.groupBy { it.assignedTechnician }
                            grouped.forEach { (tech, list) ->
                                item(key = "hdr_tech_$tech") {
                                    Text(
                                        text = "Technician: $tech • ${list.count { it.status != SampleStatus.COMPLETED }} pending / ${list.count { it.status == SampleStatus.COMPLETED }} completed",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = LabIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                items(list, key = { it.id }) { sample ->
                                    SampleCard(sample, state, canEdit, onUpdateSampleStatus)
                                }
                            }
                        }
                        else -> {
                            items(filtered, key = { it.id }) { sample ->
                                SampleCard(sample, state, canEdit, onUpdateSampleStatus)
                            }
                        }
                    }
                }
            }
        }

        if (canEdit && state.jobs.isNotEmpty()) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = NavyPrimary,
                contentColor = PureWhite,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("add_sample_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Register Sample")
            }
        }
    }

    if (showCreateDialog) {
        CreateSampleDialog(
            jobs = state.jobs,
            defaultTech = state.session.fullName.ifBlank { state.session.username },
            onDismiss = { showCreateDialog = false },
            onSave = { req ->
                onCreateSample(req) { ok ->
                    if (ok) showCreateDialog = false
                }
            }
        )
    }
}

@Composable
private fun SampleCard(
    sample: SampleDto,
    state: LabUiState,
    canEdit: Boolean,
    onUpdateSampleStatus: (Long, SampleStatus, String) -> Unit
) {
    val sampleTests = state.tests.filter { it.sampleId == sample.id }
    val doneTests = sampleTests.count { it.status.name == "COMPLETED" }

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
                Column {
                    Text(
                        text = sample.sampleCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Job: ${sample.jobNumber} • Tests: $doneTests/${sampleTests.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                SampleStatusPill(sample.status)
            }
            Text(
                text = "Fabric: ${sample.fabricDescription} • Shade: ${sample.colorShade}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Assigned Technician: ${sample.assignedTechnician}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (canEdit) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SampleStatus.entries.forEach { target ->
                        FilterChip(
                            selected = sample.status == target,
                            onClick = {
                                onUpdateSampleStatus(
                                    sample.id,
                                    target,
                                    state.session.fullName.ifBlank { sample.assignedTechnician }
                                )
                            },
                            label = { Text(target.displayName) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateSampleDialog(
    jobs: List<JobDto>,
    defaultTech: String,
    onDismiss: () -> Unit,
    onSave: (CreateSampleRequest) -> Unit
) {
    var selectedJob by remember { mutableStateOf(jobs.first()) }
    var jobMenuExpanded by remember { mutableStateOf(false) }
    var fabric by remember { mutableStateOf("") }
    var shade by remember { mutableStateOf("") }
    var technician by remember { mutableStateOf(defaultTech) }
    var selectedTests by remember { mutableStateOf(TextileTestType.entries.toSet()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register New Sample (Auto ID)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Job Card", style = MaterialTheme.typography.labelLarge)
                Box {
                    OutlinedButton(
                        onClick = { jobMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${selectedJob.jobNumber} (${selectedJob.customerName})")
                    }
                    DropdownMenu(
                        expanded = jobMenuExpanded,
                        onDismissRequest = { jobMenuExpanded = false }
                    ) {
                        jobs.forEach { job ->
                            DropdownMenuItem(
                                text = { Text("${job.jobNumber} • ${job.customerName}") },
                                onClick = {
                                    selectedJob = job
                                    jobMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = fabric,
                    onValueChange = { fabric = it },
                    label = { Text("Fabric Description *") },
                    placeholder = { Text("e.g. 100% Cotton Single Jersey") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sample_fabric_input")
                )
                OutlinedTextField(
                    value = shade,
                    onValueChange = { shade = it },
                    label = { Text("Color / Shade *") },
                    placeholder = { Text("e.g. Navy 19-4024") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("sample_shade_input")
                )
                OutlinedTextField(
                    value = technician,
                    onValueChange = { technician = it },
                    label = { Text("Assigned Technician") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Assign Testing Modules (9 Available)", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextileTestType.entries.forEach { type ->
                        val active = type in selectedTests
                        FilterChip(
                            selected = active,
                            onClick = {
                                selectedTests = if (active && selectedTests.size > 1) {
                                    selectedTests - type
                                } else {
                                    selectedTests + type
                                }
                            },
                            label = { Text(type.displayName) }
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
                    if (fabric.isBlank() || shade.isBlank()) {
                        error = "Fabric description and color shade are required."
                    } else {
                        onSave(
                            CreateSampleRequest(
                                jobId = selectedJob.id,
                                fabricDescription = fabric,
                                colorShade = shade,
                                assignedTechnician = technician,
                                selectedTests = selectedTests.toList()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("save_sample_button")
            ) {
                Text("Generate Sample ID", color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
