package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TextileLabApp
import com.example.data.local.StoredSession
import com.example.data.model.AuditLogDto
import com.example.data.model.BackupRecordDto
import com.example.data.model.CreateSampleRequest
import com.example.data.model.CreateUserRequest
import com.example.data.model.FinalReportDto
import com.example.data.model.JobCardStatus
import com.example.data.model.JobDto
import com.example.data.model.LabDashboardStats
import com.example.data.model.LabNodeMode
import com.example.data.model.PublishReportRequest
import com.example.data.model.SampleDto
import com.example.data.model.SampleStatus
import com.example.data.model.ServerHealthDto
import com.example.data.model.TestPassFail
import com.example.data.model.TestResultDto
import com.example.data.model.UpdateSampleStatusRequest
import com.example.data.model.UpdateTestResultRequest
import com.example.data.model.UpsertJobRequest
import com.example.data.model.UserDto
import com.example.data.model.UserRole
import com.example.data.server.LabServerService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LabUiState(
    val session: StoredSession = StoredSession(),
    val isServerRunning: Boolean = false,
    val serverUrl: String = "",
    val connectedClients: Int = 0,
    val wsClientConnected: Boolean = false,
    val remoteHealth: ServerHealthDto? = null,
    val stats: LabDashboardStats = LabDashboardStats(),
    val jobs: List<JobDto> = emptyList(),
    val samples: List<SampleDto> = emptyList(),
    val tests: List<TestResultDto> = emptyList(),
    val reports: List<FinalReportDto> = emptyList(),
    val users: List<UserDto> = emptyList(),
    val auditLogs: List<AuditLogDto> = emptyList(),
    val backups: List<BackupRecordDto> = emptyList(),
    val isBusy: Boolean = false
)

class LabViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as TextileLabApp).container
    private val repo = container.repository
    private val prefs = container.sessionPreferences

    private val _isBusy = MutableStateFlow(false)
    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val uiState: StateFlow<LabUiState> = combine(
        prefs.sessionFlow,
        container.localServer.isRunning,
        container.localServer.serverHostAddress,
        container.localServer.connectedClients,
        container.syncClient.wsConnected,
        repo.remoteHealth,
        repo.localStatsFlow,
        repo.localJobsFlow,
        repo.localSamplesFlow,
        repo.localTestsFlow,
        repo.localReportsFlow,
        repo.localUsersFlow,
        repo.localAuditLogsFlow,
        repo.localBackupsFlow,
        repo.remoteJobs,
        repo.remoteSamples,
        repo.remoteTests,
        repo.remoteReports,
        repo.remoteUsers,
        repo.remoteAuditLogs,
        repo.remoteBackups,
        _isBusy
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val session = values[0] as StoredSession
        val isServerRunning = values[1] as Boolean
        val serverUrl = values[2] as String
        val connectedClients = values[3] as Int
        val wsClientConnected = values[4] as Boolean
        val remoteHealth = values[5] as ServerHealthDto?
        val localStats = values[6] as LabDashboardStats
        val localJobs = values[7] as List<JobDto>
        val localSamples = values[8] as List<SampleDto>
        val localTests = values[9] as List<TestResultDto>
        val localReports = values[10] as List<FinalReportDto>
        val localUsers = values[11] as List<UserDto>
        val localAudit = values[12] as List<AuditLogDto>
        val localBackups = values[13] as List<BackupRecordDto>
        val remoteJobs = values[14] as List<JobDto>
        val remoteSamples = values[15] as List<SampleDto>
        val remoteTests = values[16] as List<TestResultDto>
        val remoteReports = values[17] as List<FinalReportDto>
        val remoteUsers = values[18] as List<UserDto>
        val remoteAudit = values[19] as List<AuditLogDto>
        val remoteBackups = values[20] as List<BackupRecordDto>
        val isBusy = values[21] as Boolean

        val isClientMode = session.nodeMode == LabNodeMode.CLIENT_NODE
        val activeJobs = if (isClientMode) remoteJobs else localJobs
        val activeSamples = if (isClientMode) remoteSamples else localSamples
        val activeTests = if (isClientMode) remoteTests else localTests
        val activeReports = if (isClientMode) remoteReports else localReports
        val filteredReports = if (session.role == UserRole.GUEST) {
            activeReports.filter { it.isPublished }
        } else {
            activeReports
        }

        val activeStats = if (isClientMode) {
            val published = activeReports.filter { it.isPublished }
            LabDashboardStats(
                totalJobs = activeJobs.size,
                urgentJobs = activeJobs.count { it.urgency.name == "URGENT" && it.status != JobCardStatus.PUBLISHED },
                totalSamples = activeSamples.size,
                pendingSamples = activeSamples.count { it.status != SampleStatus.COMPLETED },
                completedSamples = activeSamples.count { it.status == SampleStatus.COMPLETED },
                pendingTests = activeTests.count { it.status.name != "COMPLETED" },
                completedTests = activeTests.count { it.status.name == "COMPLETED" },
                publishedReports = published.size,
                passedReports = published.count { it.overallResult == TestPassFail.PASS },
                failedReports = published.count { it.overallResult == TestPassFail.FAIL }
            )
        } else {
            localStats
        }

        LabUiState(
            session = session,
            isServerRunning = isServerRunning,
            serverUrl = serverUrl,
            connectedClients = connectedClients,
            wsClientConnected = wsClientConnected,
            remoteHealth = remoteHealth,
            stats = activeStats,
            jobs = activeJobs,
            samples = activeSamples,
            tests = activeTests,
            reports = filteredReports,
            users = if (isClientMode) remoteUsers else localUsers,
            auditLogs = if (isClientMode) remoteAudit else localAudit,
            backups = if (isClientMode) remoteBackups else localBackups,
            isBusy = isBusy
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LabUiState()
    )

    init {
        viewModelScope.launch {
            container.syncClient.events.collect { event ->
                val session = uiState.value.session
                if (session.nodeMode == LabNodeMode.CLIENT_NODE && session.token.isNotBlank()) {
                    session.role?.let { role ->
                        repo.refreshRemoteData(session.remoteHostUrl, session.token, role)
                    }
                    _toastMessage.tryEmit("Live Sync: ${event.message}")
                }
            }
        }
    }

    fun login(
        username: String,
        password: String,
        nodeMode: LabNodeMode,
        remoteHostUrl: String,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repo.login(username, password, nodeMode, remoteHostUrl)
            _isBusy.value = false
            result.onSuccess { user ->
                _toastMessage.tryEmit("Signed in as ${user.fullName} (${user.role.displayName})")
            }.onFailure { err ->
                onError(err.message ?: "Authentication failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repo.logout()
        }
    }

    fun setDarkTheme(isDark: Boolean?) {
        viewModelScope.launch {
            prefs.setDarkThemeOverride(isDark)
        }
    }

    fun toggleLocalServer(start: Boolean) {
        viewModelScope.launch {
            prefs.setAutoStartServer(start)
            val app = getApplication<Application>()
            if (start) {
                runCatching { LabServerService.start(app) }
                    .onFailure { container.localServer.start() }
                _toastMessage.tryEmit("Local Wi-Fi Lab Server started")
            } else {
                runCatching { LabServerService.stop(app) }
                container.localServer.stop()
                _toastMessage.tryEmit("Local Wi-Fi Lab Server stopped")
            }
        }
    }

    fun testRemoteServer(url: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.pingRemoteServer(url)
            _isBusy.value = false
            res.onSuccess { health ->
                _toastMessage.tryEmit("Connected to ${health.hostIp}:${health.port} (${health.totalJobs} jobs)")
            }.onFailure { err ->
                _toastMessage.tryEmit("Host unreachable: ${err.message}")
            }
        }
    }

    fun refreshNow() {
        val session = uiState.value.session
        val role = session.role ?: return
        if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
            viewModelScope.launch {
                _isBusy.value = true
                repo.refreshRemoteData(session.remoteHostUrl, session.token, role)
                _isBusy.value = false
                _toastMessage.tryEmit("Synced with host server")
            }
        } else {
            _toastMessage.tryEmit("Host SQLite database is up to date")
        }
    }

    fun saveJob(req: UpsertJobRequest, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.upsertJob(uiState.value.session, req)
            _isBusy.value = false
            res.onSuccess { job ->
                _toastMessage.tryEmit("Job ${job.jobNumber} saved")
                onComplete(true)
            }.onFailure { err ->
                _toastMessage.tryEmit(err.message ?: "Failed to save job")
                onComplete(false)
            }
        }
    }

    fun createSample(req: CreateSampleRequest, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.createSample(uiState.value.session, req)
            _isBusy.value = false
            res.onSuccess { sample ->
                _toastMessage.tryEmit("Registered ${sample.sampleCode}")
                onComplete(true)
            }.onFailure { err ->
                _toastMessage.tryEmit(err.message ?: "Failed to register sample")
                onComplete(false)
            }
        }
    }

    fun updateSampleStatus(sampleId: Long, status: SampleStatus, technician: String) {
        viewModelScope.launch {
            val res = repo.updateSampleStatus(
                uiState.value.session,
                sampleId,
                UpdateSampleStatusRequest(status, technician)
            )
            res.onSuccess {
                _toastMessage.tryEmit("Sample ${it.sampleCode} marked ${status.displayName}")
            }.onFailure {
                _toastMessage.tryEmit(it.message ?: "Failed to update sample")
            }
        }
    }

    fun updateTestResult(testId: Long, req: UpdateTestResultRequest, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.updateTestResult(uiState.value.session, testId, req)
            _isBusy.value = false
            res.onSuccess {
                _toastMessage.tryEmit("${it.testType.displayName} saved (${it.passFail.displayName})")
                onComplete(true)
            }.onFailure { err ->
                _toastMessage.tryEmit(err.message ?: "Failed to update test")
                onComplete(false)
            }
        }
    }

    fun publishReport(req: PublishReportRequest, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.publishFinalReport(uiState.value.session, req)
            _isBusy.value = false
            res.onSuccess { report ->
                _toastMessage.tryEmit("Report ${report.reportNumber} published (${report.overallResult.displayName})")
                onComplete(true)
            }.onFailure { err ->
                _toastMessage.tryEmit(err.message ?: "Failed to publish report")
                onComplete(false)
            }
        }
    }

    fun uploadPhoto(jobId: Long, reportId: Long, sourceType: String, uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.uploadReportPhoto(uiState.value.session, jobId, reportId, sourceType, uri)
            _isBusy.value = false
            res.onSuccess {
                _toastMessage.tryEmit("Photo uploaded ($sourceType)")
            }.onFailure { err ->
                _toastMessage.tryEmit(err.message ?: "Upload failed")
            }
        }
    }

    fun deleteAttachment(attachmentId: Long) {
        viewModelScope.launch {
            val res = repo.deleteAttachment(uiState.value.session, attachmentId)
            res.onSuccess {
                _toastMessage.tryEmit("File deleted by Admin")
            }.onFailure { err ->
                _toastMessage.tryEmit(err.message ?: "Deletion rejected")
            }
        }
    }

    fun createUser(req: CreateUserRequest, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.createUser(uiState.value.session, req)
            _isBusy.value = false
            res.onSuccess {
                _toastMessage.tryEmit("Created ${it.role.displayName} user: ${it.username}")
                onComplete(true)
            }.onFailure { err ->
                _toastMessage.tryEmit(err.message ?: "User creation failed")
                onComplete(false)
            }
        }
    }

    fun toggleUser(userId: Long, isActive: Boolean) {
        viewModelScope.launch {
            repo.toggleUserActive(uiState.value.session, userId, isActive).onSuccess {
                _toastMessage.tryEmit("User status updated")
            }.onFailure {
                _toastMessage.tryEmit(it.message ?: "Update failed")
            }
        }
    }

    fun runManualBackup() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repo.triggerManualBackup(uiState.value.session)
            _isBusy.value = false
            res.onSuccess {
                _toastMessage.tryEmit("SQLite backup saved: ${it.fileName}")
            }.onFailure {
                _toastMessage.tryEmit(it.message ?: "Backup failed")
            }
        }
    }

    fun deleteBackup(backupId: Long) {
        viewModelScope.launch {
            repo.deleteBackup(uiState.value.session, backupId).onSuccess {
                _toastMessage.tryEmit("Backup removed")
            }.onFailure {
                _toastMessage.tryEmit(it.message ?: "Delete failed")
            }
        }
    }
}
