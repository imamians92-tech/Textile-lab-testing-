package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.core.NetworkUtils
import com.example.data.model.AttachmentDto
import com.example.data.model.FinalReportDto
import com.example.data.model.JobDto
import com.example.data.model.PublishReportRequest
import com.example.data.model.TestPassFail
import com.example.data.model.UserRole
import com.example.ui.LabUiState
import com.example.ui.components.AttachmentPhotoCard
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.FullScreenPhotoDialog
import com.example.ui.components.PassFailPill
import com.example.ui.components.SearchBarField
import com.example.ui.theme.LabEmerald
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.PureWhite
import java.io.File

@Composable
fun ReportsScreen(
    state: LabUiState,
    onPublishReport: (PublishReportRequest, (Boolean) -> Unit) -> Unit,
    onUploadPhoto: (jobId: Long, reportId: Long, sourceType: String, uri: Uri) -> Unit,
    onDeleteAttachment: (Long) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var resultFilter by remember { mutableStateOf<TestPassFail?>(null) }
    var showPublishDialog by remember { mutableStateOf(false) }
    var preselectedJobId by remember { mutableStateOf<Long?>(null) }
    var previewPhoto by remember { mutableStateOf<AttachmentDto?>(null) }
    var pendingUploadTarget by remember { mutableStateOf<Pair<Long, Long>?>(null) } // jobId to reportId
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val role = state.session.role
    val isGuest = role == UserRole.GUEST
    val canPublishOrUpload = role?.canPublishReports == true

    // Zero-permission Android Photo Picker for Gallery photos
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingUploadTarget
        if (uri != null && target != null) {
            onUploadPhoto(target.first, target.second, "GALLERY", uri)
        }
    }

    // System Camera Capture via FileProvider
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val target = pendingUploadTarget
        val uri = pendingCameraUri
        if (success && uri != null && target != null) {
            onUploadPhoto(target.first, target.second, "CAMERA", uri)
        }
    }

    val filteredReports = remember(state.reports, searchQuery, resultFilter, isGuest) {
        state.reports.filter { r ->
            val guestCheck = if (isGuest) r.isPublished else true
            val q = searchQuery.isBlank() ||
                r.jobNumber.contains(searchQuery, ignoreCase = true) ||
                r.reportNumber.contains(searchQuery, ignoreCase = true) ||
                r.customerName.contains(searchQuery, ignoreCase = true)
            val pf = resultFilter == null || r.overallResult == resultFilter
            guestCheck && q && pf
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    placeholder = "Search Job Number, Report Number or Customer...",
                    testTag = "reports_search_input"
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = resultFilter == null,
                        onClick = { resultFilter = null },
                        label = { Text("All (${state.reports.size})") }
                    )
                    FilterChip(
                        selected = resultFilter == TestPassFail.PASS,
                        onClick = { resultFilter = if (resultFilter == TestPassFail.PASS) null else TestPassFail.PASS },
                        label = { Text("PASS") }
                    )
                    FilterChip(
                        selected = resultFilter == TestPassFail.FAIL,
                        onClick = { resultFilter = if (resultFilter == TestPassFail.FAIL) null else TestPassFail.FAIL },
                        label = { Text("FAIL") }
                    )
                }
            }

            if (filteredReports.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStatePanel(
                        icon = Icons.Default.AssignmentTurnedIn,
                        title = if (isGuest) "No Published Reports Found" else "No Reports Created Yet",
                        description = if (isGuest) {
                            "Published reports appear here automatically in real time as soon as a Technician or Admin publishes them."
                        } else {
                            "Tap + Publish Report to publish a Final Report with PASS/FAIL, comments, and Camera/Gallery photos."
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredReports, key = { it.id }) { report ->
                        FinalReportCard(
                            report = report,
                            state = state,
                            canPublishOrUpload = canPublishOrUpload,
                            onPreviewPhoto = { previewPhoto = it },
                            onDeletePhoto = onDeleteAttachment,
                            onLaunchGallery = {
                                pendingUploadTarget = report.jobId to report.id
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onLaunchCamera = {
                                pendingUploadTarget = report.jobId to report.id
                                val tempDir = File(context.cacheDir, "camera_temp").apply { mkdirs() }
                                val tempFile = File(tempDir, "cam_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    tempFile
                                )
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                            onRepublish = {
                                preselectedJobId = report.jobId
                                showPublishDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (canPublishOrUpload && state.jobs.isNotEmpty()) {
            FloatingActionButton(
                onClick = {
                    preselectedJobId = null
                    showPublishDialog = true
                },
                containerColor = NavyPrimary,
                contentColor = PureWhite,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("publish_report_fab")
            ) {
                Icon(Icons.Default.Publish, contentDescription = "Publish Final Report")
            }
        }
    }

    if (showPublishDialog && state.jobs.isNotEmpty()) {
        PublishReportDialog(
            jobs = state.jobs,
            initialJobId = preselectedJobId,
            onDismiss = { showPublishDialog = false },
            onPublish = { req ->
                onPublishReport(req) { ok ->
                    if (ok) showPublishDialog = false
                }
            }
        )
    }

    previewPhoto?.let { photo ->
        FullScreenPhotoDialog(
            attachment = photo,
            session = state.session,
            onDismiss = { previewPhoto = null }
        )
    }
}

@Composable
private fun FinalReportCard(
    report: FinalReportDto,
    state: LabUiState,
    canPublishOrUpload: Boolean,
    onPreviewPhoto: (AttachmentDto) -> Unit,
    onDeletePhoto: (Long) -> Unit,
    onLaunchGallery: () -> Unit,
    onLaunchCamera: () -> Unit,
    onRepublish: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.reportNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Job #: ${report.jobNumber} • Date: ${report.reportDate}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                PassFailPill(report.overallResult)
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.small
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Customer: ${report.customerName}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = if (report.isPublished) {
                            "Published directly by ${report.publishedBy} (${report.publishedByRole})"
                        } else {
                            "Draft (Not yet visible to Guest)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (report.isPublished) LabEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (report.comments.isNotBlank()) {
                        Text(
                            text = "Comments: ${report.comments}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Test Summary Breakdown
            if (report.testResults.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Lab Test Results (${report.testResults.size})",
                    style = MaterialTheme.typography.labelLarge
                )
                report.testResults.take(9).forEach { t ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${t.testType.displayName} (${t.sampleCode})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (t.resultValue.isBlank()) "Pending" else "${t.resultValue} ${t.unit}",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        PassFailPill(t.passFail)
                    }
                }
            }

            // Report Photos Section
            if (report.attachments.isNotEmpty() || canPublishOrUpload) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Report Photos (${report.attachments.size})",
                        style = MaterialTheme.typography.labelLarge
                    )
                    if (state.session.role == UserRole.TECHNICIAN) {
                        Text(
                            text = "Upload enabled • Delete: Admin only",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (report.attachments.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(report.attachments, key = { it.id }) { photo ->
                        AttachmentPhotoCard(
                            attachment = photo,
                            session = state.session,
                            onPreview = { onPreviewPhoto(photo) },
                            onDelete = { onDeletePhoto(photo.id) }
                        )
                    }
                }
            }

            if (canPublishOrUpload) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLaunchCamera,
                        modifier = Modifier.weight(1f).testTag("camera_upload_${report.id}")
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null)
                        Text(" Camera")
                    }
                    OutlinedButton(
                        onClick = onLaunchGallery,
                        modifier = Modifier.weight(1f).testTag("gallery_upload_${report.id}")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Text(" Gallery")
                    }
                    Button(
                        onClick = onRepublish,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update", color = PureWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishReportDialog(
    jobs: List<JobDto>,
    initialJobId: Long?,
    onDismiss: () -> Unit,
    onPublish: (PublishReportRequest) -> Unit
) {
    var selectedJob by remember {
        mutableStateOf(jobs.firstOrNull { it.id == initialJobId } ?: jobs.first())
    }
    var jobMenuExpanded by remember { mutableStateOf(false) }
    var reportDate by remember { mutableStateOf(NetworkUtils.formatDateOnly(System.currentTimeMillis())) }
    var overallResult by remember { mutableStateOf(TestPassFail.PASS) }
    var comments by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Publish Final Lab Report") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Job & Report #", style = MaterialTheme.typography.labelLarge)
                Box {
                    OutlinedButton(
                        onClick = { jobMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${selectedJob.jobNumber} • ${selectedJob.reportNumber}")
                    }
                    DropdownMenu(
                        expanded = jobMenuExpanded,
                        onDismissRequest = { jobMenuExpanded = false }
                    ) {
                        jobs.forEach { job ->
                            DropdownMenuItem(
                                text = { Text("${job.jobNumber} (${job.reportNumber}) • ${job.customerName}") },
                                onClick = {
                                    selectedJob = job
                                    jobMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Text(
                    text = "Customer: ${selectedJob.customerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reportDate,
                    onValueChange = { reportDate = it },
                    label = { Text("Report Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Overall Lab Decision", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TestPassFail.PASS, TestPassFail.FAIL).forEach { pf ->
                        FilterChip(
                            selected = overallResult == pf,
                            onClick = { overallResult = pf },
                            label = { Text(pf.displayName) }
                        )
                    }
                }
                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text("Final Remarks / Comments") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("report_comments_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPublish(
                        PublishReportRequest(
                            jobId = selectedJob.id,
                            reportDate = reportDate,
                            overallResult = overallResult,
                            comments = comments,
                            publishNow = true
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.testTag("confirm_publish_report")
            ) {
                Text("Publish Now", color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
