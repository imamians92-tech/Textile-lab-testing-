package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.TestPassFail
import com.example.data.model.TestResultDto
import com.example.data.model.TestStatus
import com.example.data.model.TextileTestType
import com.example.data.model.UpdateTestResultRequest
import com.example.ui.LabUiState
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.PassFailPill
import com.example.ui.components.SearchBarField
import com.example.ui.components.TestStatusPill
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite

@Composable
fun TestingScreen(
    state: LabUiState,
    onUpdateTest: (Long, UpdateTestResultRequest, (Boolean) -> Unit) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedModule by remember { mutableStateOf<TextileTestType?>(null) }
    var selectedStatus by remember { mutableStateOf<TestStatus?>(null) }
    var editingTest by remember { mutableStateOf<TestResultDto?>(null) }

    val canEdit = state.session.role?.canEditLabData == true

    val filteredTests = remember(state.tests, searchQuery, selectedModule, selectedStatus) {
        state.tests.filter { t ->
            val q = searchQuery.isBlank() ||
                t.sampleCode.contains(searchQuery, ignoreCase = true) ||
                t.jobNumber.contains(searchQuery, ignoreCase = true) ||
                t.technicianName.contains(searchQuery, ignoreCase = true) ||
                t.testType.displayName.contains(searchQuery, ignoreCase = true)
            val m = selectedModule == null || t.testType == selectedModule
            val s = selectedStatus == null || t.status == selectedStatus
            q && m && s
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SearchBarField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search Sample ID, Job #, or Technician...",
                testTag = "tests_search_input"
            )

            // 9 Textile Testing Modules Filter Strip
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedModule == null,
                    onClick = { selectedModule = null },
                    label = { Text("All 9 Modules (${state.tests.size})") }
                )
                TextileTestType.entries.forEach { module ->
                    FilterChip(
                        selected = selectedModule == module,
                        onClick = { selectedModule = if (selectedModule == module) null else module },
                        label = { Text(module.displayName) }
                    )
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TestStatus.entries.forEach { st ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { selectedStatus = if (selectedStatus == st) null else st },
                        label = { Text("${st.displayName} (${state.tests.count { it.status == st }})") }
                    )
                }
            }
        }

        if (filteredTests.isEmpty()) {
            Box(modifier = Modifier.padding(16.dp)) {
                EmptyStatePanel(
                    icon = Icons.Default.Biotech,
                    title = "No Tests Queued",
                    description = "Register a sample in the Samples tab to populate GSM, Width, Tear, Tensile, Stretch Growth, Pilling, Abrasion, Shrinkage, and Washing tests."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTests, key = { it.id }) { test ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${test.testType.displayName} • ${test.sampleCode}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Job: ${test.jobNumber} • Spec: ${test.standardSpec}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TestStatusPill(test.status)
                                    PassFailPill(test.passFail)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val resultDisplay = if (test.resultValue.isBlank()) {
                                        "Not recorded yet"
                                    } else {
                                        "${test.resultValue} ${test.unit}"
                                    }
                                    Text(
                                        text = "Result: $resultDisplay",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Tech: ${test.technicianName} • Date: ${test.testedDate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (test.remarks.isNotBlank()) {
                                        Text(
                                            text = "Remarks: ${test.remarks}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (canEdit) {
                                    OutlinedButton(
                                        onClick = { editingTest = test },
                                        modifier = Modifier.testTag("record_test_${test.id}")
                                    ) {
                                        Icon(Icons.Default.EditNote, contentDescription = null)
                                        Text(" Record")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingTest?.let { test ->
        EditTestResultDialog(
            test = test,
            defaultTech = state.session.fullName.ifBlank { state.session.username },
            onDismiss = { editingTest = null },
            onSave = { req ->
                onUpdateTest(test.id, req) { ok ->
                    if (ok) editingTest = null
                }
            }
        )
    }
}

@Composable
private fun EditTestResultDialog(
    test: TestResultDto,
    defaultTech: String,
    onDismiss: () -> Unit,
    onSave: (UpdateTestResultRequest) -> Unit
) {
    var resultValue by remember { mutableStateOf(test.resultValue) }
    var standardSpec by remember { mutableStateOf(test.standardSpec) }
    var passFail by remember { mutableStateOf(test.passFail) }
    var status by remember { mutableStateOf(if (test.status == TestStatus.PENDING) TestStatus.COMPLETED else test.status) }
    var technician by remember { mutableStateOf(test.technicianName.ifBlank { defaultTech }) }
    var testedDate by remember { mutableStateOf(test.testedDate.ifBlank { NetworkUtils.formatDateOnly(System.currentTimeMillis()) }) }
    var remarks by remember { mutableStateOf(test.remarks) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${test.testType.displayName} (${test.sampleCode})") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = resultValue,
                    onValueChange = { resultValue = it },
                    label = { Text("Measured Result (${test.unit})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("test_value_input")
                )
                OutlinedTextField(
                    value = standardSpec,
                    onValueChange = { standardSpec = it },
                    label = { Text("Standard Tolerance / Spec") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Pass / Fail Verdict", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TestPassFail.entries.forEach { pf ->
                        FilterChip(
                            selected = passFail == pf,
                            onClick = { passFail = pf },
                            label = { Text(pf.displayName) }
                        )
                    }
                }
                Text("Test Status", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TestStatus.entries.forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = technician,
                    onValueChange = { technician = it },
                    label = { Text("Technician") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = testedDate,
                    onValueChange = { testedDate = it },
                    label = { Text("Test Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        UpdateTestResultRequest(
                            resultValue = resultValue,
                            standardSpec = standardSpec,
                            passFail = passFail,
                            technicianName = technician,
                            testedDate = testedDate,
                            status = status,
                            remarks = remarks
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("save_test_button")
            ) {
                Text("Save Result", color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
