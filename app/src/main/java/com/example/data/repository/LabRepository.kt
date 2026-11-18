package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.core.NetworkUtils
import com.example.core.SecurityUtils
import com.example.data.backup.BackupManager
import com.example.data.local.SessionPreferences
import com.example.data.local.StoredSession
import com.example.data.local.TextileLabDatabase
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.FinalReportEntity
import com.example.data.local.entities.JobEntity
import com.example.data.local.entities.ReportAttachmentEntity
import com.example.data.local.entities.SampleEntity
import com.example.data.local.entities.TestResultEntity
import com.example.data.local.entities.UserEntity
import com.example.data.local.toDto
import com.example.data.model.AttachmentDto
import com.example.data.model.AuditLogDto
import com.example.data.model.BackupRecordDto
import com.example.data.model.CreateSampleRequest
import com.example.data.model.CreateUserRequest
import com.example.data.model.FinalReportDto
import com.example.data.model.JobCardStatus
import com.example.data.model.JobDto
import com.example.data.model.LabDashboardStats
import com.example.data.model.LabNodeMode
import com.example.data.model.LoginRequest
import com.example.data.model.PublishReportRequest
import com.example.data.model.SampleDto
import com.example.data.model.SampleStatus
import com.example.data.model.ServerHealthDto
import com.example.data.model.TestPassFail
import com.example.data.model.TestResultDto
import com.example.data.model.TestStatus
import com.example.data.model.TextileTestType
import com.example.data.model.UpdateSampleStatusRequest
import com.example.data.model.UpdateTestResultRequest
import com.example.data.model.UploadPhotoRequest
import com.example.data.model.UpsertJobRequest
import com.example.data.model.UserDto
import com.example.data.model.UserRole
import com.example.data.model.WsEvent
import com.example.data.network.LabSyncClient
import com.example.data.server.LabLocalServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Base64
import java.util.Locale

class LabRepository(
    private val context: Context,
    private val database: TextileLabDatabase,
    private val backupManager: BackupManager,
    private val localServer: LabLocalServer,
    private val syncClient: LabSyncClient,
    private val sessionPrefs: SessionPreferences
) {
    private val _remoteJobs = MutableStateFlow<List<JobDto>>(emptyList())
    private val _remoteSamples = MutableStateFlow<List<SampleDto>>(emptyList())
    private val _remoteTests = MutableStateFlow<List<TestResultDto>>(emptyList())
    private val _remoteReports = MutableStateFlow<List<FinalReportDto>>(emptyList())
    private val _remoteUsers = MutableStateFlow<List<UserDto>>(emptyList())
    private val _remoteAuditLogs = MutableStateFlow<List<AuditLogDto>>(emptyList())
    private val _remoteBackups = MutableStateFlow<List<BackupRecordDto>>(emptyList())
    private val _remoteHealth = MutableStateFlow<ServerHealthDto?>(null)

    val remoteJobs: StateFlow<List<JobDto>> = _remoteJobs.asStateFlow()
    val remoteSamples: StateFlow<List<SampleDto>> = _remoteSamples.asStateFlow()
    val remoteTests: StateFlow<List<TestResultDto>> = _remoteTests.asStateFlow()
    val remoteReports: StateFlow<List<FinalReportDto>> = _remoteReports.asStateFlow()
    val remoteUsers: StateFlow<List<UserDto>> = _remoteUsers.asStateFlow()
    val remoteAuditLogs: StateFlow<List<AuditLogDto>> = _remoteAuditLogs.asStateFlow()
    val remoteBackups: StateFlow<List<BackupRecordDto>> = _remoteBackups.asStateFlow()
    val remoteHealth: StateFlow<ServerHealthDto?> = _remoteHealth.asStateFlow()

    val localJobsFlow: Flow<List<JobDto>> = combine(database.jobDao().observeAll()) { it[0].map { j -> j.toDto() } }
    val localSamplesFlow: Flow<List<SampleDto>> = combine(database.sampleDao().observeAll()) { it[0].map { s -> s.toDto() } }
    val localTestsFlow: Flow<List<TestResultDto>> = combine(database.testResultDao().observeAll()) { it[0].map { t -> t.toDto() } }
    val localUsersFlow: Flow<List<UserDto>> = combine(database.userDao().observeUsers()) { it[0].map { u -> u.toDto() } }
    val localAuditLogsFlow: Flow<List<AuditLogDto>> = combine(database.auditLogDao().observeRecent()) { it[0].map { l -> l.toDto() } }
    val localBackupsFlow: Flow<List<BackupRecordDto>> = combine(database.backupDao().observeAll()) { it[0].map { b -> b.toDto() } }

    val localReportsFlow: Flow<List<FinalReportDto>> = combine(
        database.finalReportDao().observeAll(),
        database.attachmentDao().observeAll(),
        database.testResultDao().observeAll()
    ) { reports, attachments, tests ->
        reports.map { r ->
            r.toDto(
                attachments = attachments.filter { it.jobId == r.jobId }.map { it.toDto() },
                testResults = tests.filter { it.jobId == r.jobId }.map { it.toDto() }
            )
        }
    }

    val localStatsFlow: Flow<LabDashboardStats> = combine(
        database.jobDao().observeAll(),
        database.sampleDao().observeAll(),
        database.testResultDao().observeAll(),
        database.finalReportDao().observePublished()
    ) { jobs, samples, tests, published ->
        LabDashboardStats(
            totalJobs = jobs.size,
            urgentJobs = jobs.count { it.urgency == "URGENT" && it.status != "PUBLISHED" },
            totalSamples = samples.size,
            pendingSamples = samples.count { it.status != "COMPLETED" },
            completedSamples = samples.count { it.status == "COMPLETED" },
            pendingTests = tests.count { it.status != "COMPLETED" },
            completedTests = tests.count { it.status == "COMPLETED" },
            publishedReports = published.size,
            passedReports = published.count { it.overallResult == "PASS" },
            failedReports = published.count { it.overallResult == "FAIL" }
        )
    }

    suspend fun ensureDefaultUsersSeeded() = withContext(Dispatchers.IO) {
        if (database.userDao().countUsers() == 0) {
            database.userDao().insert(
                UserEntity(
                    username = "admin",
                    fullName = "Lab Director (Admin)",
                    role = UserRole.ADMIN.name,
                    passwordHash = SecurityUtils.hashPassword("admin123"),
                    isActive = true
                )
            )
            database.userDao().insert(
                UserEntity(
                    username = "tech1",
                    fullName = "Senior Lab Technician",
                    role = UserRole.TECHNICIAN.name,
                    passwordHash = SecurityUtils.hashPassword("tech123"),
                    isActive = true
                )
            )
            database.userDao().insert(
                UserEntity(
                    username = "guest",
                    fullName = "Buyer / Guest Viewer",
                    role = UserRole.GUEST.name,
                    passwordHash = SecurityUtils.hashPassword("guest123"),
                    isActive = true
                )
            )
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = "SYSTEM",
                    actorRole = "SYSTEM",
                    action = "INIT_ACCOUNTS",
                    targetType = "USER",
                    targetId = "3",
                    details = "Initialized Admin, Technician, and Guest accounts with PBKDF2-HMAC-SHA256",
                    status = "ALLOWED"
                )
            )
        }
    }

    suspend fun login(
        username: String,
        password: String,
        nodeMode: LabNodeMode,
        remoteHostUrl: String
    ): Result<UserDto> = withContext(Dispatchers.IO) {
        sessionPrefs.setNodeConfig(nodeMode, remoteHostUrl)

        if (nodeMode == LabNodeMode.CLIENT_NODE) {
            val res = syncClient.login(remoteHostUrl, LoginRequest(username.trim(), password))
            res.onSuccess { loginRes ->
                sessionPrefs.saveSession(loginRes.token, loginRes.user)
                syncClient.connectWebSocket(remoteHostUrl)
                refreshRemoteData(remoteHostUrl, loginRes.token, loginRes.user.role)
            }
            return@withContext res.map { it.user }
        }

        // Host Server local authentication against SQLite Room DB
        val userEntity = database.userDao().getByUsername(username.trim())
        if (userEntity == null || !userEntity.isActive ||
            !SecurityUtils.verifyPassword(password, userEntity.passwordHash)
        ) {
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = username.ifBlank { "anonymous" },
                    actorRole = "UNKNOWN",
                    action = "LOGIN",
                    targetType = "AUTH",
                    targetId = username,
                    details = "Failed login attempt",
                    status = "DENIED"
                )
            )
            return@withContext Result.failure(IllegalArgumentException("Invalid username or password"))
        }

        val token = SecurityUtils.generateSessionToken()
        val dto = userEntity.toDto()
        localServer.registerLocalSession(token, dto)
        sessionPrefs.saveSession(token, dto)
        database.auditLogDao().insert(
            AuditLogEntity(
                actorUsername = dto.username,
                actorRole = dto.role.name,
                action = "LOGIN",
                targetType = "AUTH",
                targetId = dto.id.toString(),
                details = "Signed in on host terminal",
                status = "ALLOWED"
            )
        )
        Result.success(dto)
    }

    suspend fun restoreActiveSessionTokenIfNeeded() = withContext(Dispatchers.IO) {
        val session = sessionPrefs.sessionFlow.first()
        if (session.token.isNotBlank() && session.userId > 0L) {
            if (session.nodeMode == LabNodeMode.HOST_SERVER) {
                val dbUser = database.userDao().getById(session.userId)
                if (dbUser != null && dbUser.isActive) {
                    localServer.registerLocalSession(session.token, dbUser.toDto())
                } else {
                    sessionPrefs.clearSession()
                }
            } else {
                syncClient.connectWebSocket(session.remoteHostUrl)
                session.role?.let { refreshRemoteData(session.remoteHostUrl, session.token, it) }
            }
        }
    }

    suspend fun logout() {
        syncClient.disconnectWebSocket()
        sessionPrefs.clearSession()
    }

    suspend fun pingRemoteServer(baseUrl: String): Result<ServerHealthDto> {
        val res = syncClient.checkHealth(baseUrl)
        res.onSuccess { _remoteHealth.value = it }
        return res
    }

    suspend fun refreshRemoteData(baseUrl: String, token: String, role: UserRole) = withContext(Dispatchers.IO) {
        syncClient.checkHealth(baseUrl).onSuccess { _remoteHealth.value = it }
        syncClient.getJobs(baseUrl, token).onSuccess { _remoteJobs.value = it }
        syncClient.getSamples(baseUrl, token).onSuccess { _remoteSamples.value = it }
        syncClient.getTests(baseUrl, token).onSuccess { _remoteTests.value = it }
        syncClient.getReports(baseUrl, token, publishedOnly = role == UserRole.GUEST)
            .onSuccess { _remoteReports.value = it }
        if (role == UserRole.ADMIN) {
            syncClient.getUsers(baseUrl, token).onSuccess { _remoteUsers.value = it }
            syncClient.getAuditLogs(baseUrl, token).onSuccess { _remoteAuditLogs.value = it }
            syncClient.getBackups(baseUrl, token).onSuccess { _remoteBackups.value = it }
        }
    }

    suspend fun upsertJob(session: StoredSession, req: UpsertJobRequest): Result<JobDto> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (!role.canEditLabData) {
            logLocalDenied(session, "UPSERT_JOB", "JOB", req.jobNumber)
            return@withContext Result.failure(SecurityException("Guests have read-only access"))
        }
        if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
            return@withContext syncClient.upsertJob(session.remoteHostUrl, session.token, req).also {
                if (it.isSuccess) refreshRemoteData(session.remoteHostUrl, session.token, role)
            }
        }
        runCatching {
            val now = System.currentTimeMillis()
            val entity = JobEntity(
                id = req.id,
                jobNumber = req.jobNumber.trim(),
                reportNumber = req.reportNumber.trim(),
                customerName = req.customerName.trim(),
                buyerName = req.buyerName.trim(),
                quantity = req.quantity.trim(),
                receivedDate = req.receivedDate.trim(),
                processesCsv = req.processes.joinToString(", "),
                urgency = req.urgency.name,
                remarks = req.remarks.trim(),
                status = req.status.name,
                createdBy = session.username,
                updatedAt = now
            )
            val savedId = if (req.id == 0L) database.jobDao().insert(entity) else {
                database.jobDao().update(entity)
                req.id
            }
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = session.username,
                    actorRole = role.name,
                    action = if (req.id == 0L) "CREATE_JOB" else "EDIT_JOB",
                    targetType = "JOB",
                    targetId = savedId.toString(),
                    details = "Job ${req.jobNumber} / Report ${req.reportNumber}",
                    status = "ALLOWED"
                )
            )
            localServer.broadcastEvent(WsEvent("JOB_UPDATED", "Job ${req.jobNumber} saved", savedId))
            entity.copy(id = savedId).toDto()
        }
    }

    suspend fun createSample(session: StoredSession, req: CreateSampleRequest): Result<SampleDto> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (!role.canEditLabData) {
            logLocalDenied(session, "CREATE_SAMPLE", "SAMPLE", req.jobId.toString())
            return@withContext Result.failure(SecurityException("Guests cannot create samples"))
        }
        if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
            return@withContext syncClient.createSample(session.remoteHostUrl, session.token, req).also {
                if (it.isSuccess) refreshRemoteData(session.remoteHostUrl, session.token, role)
            }
        }
        runCatching {
            val job = database.jobDao().getById(req.jobId)
                ?: throw IllegalArgumentException("Selected job no longer exists")
            val nextSeq = (database.sampleDao().maxId() ?: 0L) + 1L
            val yearMonth = NetworkUtils.formatDateOnly(System.currentTimeMillis()).replace("-", "").substring(2, 6)
            val sampleCode = String.format(Locale.US, "SMP-%s-%04d", yearMonth, nextSeq)
            val now = System.currentTimeMillis()
            val sample = SampleEntity(
                sampleCode = sampleCode,
                jobId = job.id,
                jobNumber = job.jobNumber,
                fabricDescription = req.fabricDescription.trim(),
                colorShade = req.colorShade.trim(),
                assignedTechnician = req.assignedTechnician.ifBlank { session.fullName },
                status = SampleStatus.PENDING.name,
                createdAt = now,
                updatedAt = now
            )
            val sampleId = database.sampleDao().insert(sample)
            val testsToCreate = req.selectedTests.ifEmpty { TextileTestType.entries }
            val testEntities = testsToCreate.map { type ->
                TestResultEntity(
                    sampleId = sampleId,
                    sampleCode = sampleCode,
                    jobId = job.id,
                    jobNumber = job.jobNumber,
                    testType = type.name,
                    resultValue = "",
                    unit = type.unit,
                    standardSpec = type.defaultSpec,
                    passFail = TestPassFail.PENDING.name,
                    technicianName = sample.assignedTechnician,
                    testedDate = NetworkUtils.formatDateOnly(now),
                    status = TestStatus.PENDING.name,
                    remarks = "",
                    updatedAt = now
                )
            }
            database.testResultDao().insertAll(testEntities)
            if (job.status == JobCardStatus.RECEIVED.name) {
                database.jobDao().updateStatus(job.id, JobCardStatus.IN_TESTING.name, now)
            }
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = session.username,
                    actorRole = role.name,
                    action = "CREATE_SAMPLE",
                    targetType = "SAMPLE",
                    targetId = sampleId.toString(),
                    details = "Created $sampleCode (${testsToCreate.size} tests) for ${job.jobNumber}",
                    status = "ALLOWED"
                )
            )
            localServer.broadcastEvent(WsEvent("SAMPLE_UPDATED", "Sample $sampleCode registered", sampleId))
            sample.copy(id = sampleId).toDto()
        }
    }

    suspend fun updateSampleStatus(
        session: StoredSession,
        sampleId: Long,
        req: UpdateSampleStatusRequest
    ): Result<SampleDto> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (!role.canEditLabData) {
            logLocalDenied(session, "UPDATE_SAMPLE", "SAMPLE", sampleId.toString())
            return@withContext Result.failure(SecurityException("Guests cannot update samples"))
        }
        if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
            return@withContext syncClient.updateSample(session.remoteHostUrl, session.token, sampleId, req).also {
                if (it.isSuccess) refreshRemoteData(session.remoteHostUrl, session.token, role)
            }
        }
        runCatching {
            val now = System.currentTimeMillis()
            database.sampleDao().updateStatus(sampleId, req.status.name, req.assignedTechnician, now)
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = session.username,
                    actorRole = role.name,
                    action = "UPDATE_SAMPLE",
                    targetType = "SAMPLE",
                    targetId = sampleId.toString(),
                    details = "Marked ${req.status.displayName} (${req.assignedTechnician})",
                    status = "ALLOWED"
                )
            )
            localServer.broadcastEvent(WsEvent("SAMPLE_UPDATED", "Sample #$sampleId -> ${req.status.displayName}", sampleId))
            database.sampleDao().getById(sampleId)?.toDto() ?: throw IllegalStateException("Sample not found")
        }
    }

    suspend fun updateTestResult(
        session: StoredSession,
        testId: Long,
        req: UpdateTestResultRequest
    ): Result<TestResultDto> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (!role.canEditLabData) {
            logLocalDenied(session, "UPDATE_TEST", "TEST", testId.toString())
            return@withContext Result.failure(SecurityException("Guests cannot record test results"))
        }
        if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
            return@withContext syncClient.updateTest(session.remoteHostUrl, session.token, testId, req).also {
                if (it.isSuccess) refreshRemoteData(session.remoteHostUrl, session.token, role)
            }
        }
        runCatching {
            val existing = database.testResultDao().getById(testId)
                ?: throw IllegalArgumentException("Test record not found")
            val now = System.currentTimeMillis()
            val updated = existing.copy(
                resultValue = req.resultValue.trim(),
                standardSpec = req.standardSpec.trim(),
                passFail = req.passFail.name,
                technicianName = req.technicianName.ifBlank { session.fullName },
                testedDate = req.testedDate,
                status = req.status.name,
                remarks = req.remarks.trim(),
                updatedAt = now
            )
            database.testResultDao().update(updated)

            val sampleTests = database.testResultDao().getBySampleId(existing.sampleId)
            val allDone = sampleTests.isNotEmpty() && sampleTests.all { it.status == TestStatus.COMPLETED.name }
            val anyStarted = sampleTests.any { it.status != TestStatus.PENDING.name }
            val newSampleStatus = when {
                allDone -> SampleStatus.COMPLETED.name
                anyStarted -> SampleStatus.IN_PROGRESS.name
                else -> SampleStatus.PENDING.name
            }
            database.sampleDao().updateStatus(
                existing.sampleId,
                newSampleStatus,
                updated.technicianName,
                now
            )
            val jobTests = database.testResultDao().getByJobId(existing.jobId)
            if (jobTests.isNotEmpty() && jobTests.all { it.status == TestStatus.COMPLETED.name }) {
                val job = database.jobDao().getById(existing.jobId)
                if (job != null && job.status != JobCardStatus.PUBLISHED.name) {
                    database.jobDao().updateStatus(job.id, JobCardStatus.COMPLETED.name, now)
                }
            }
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = session.username,
                    actorRole = role.name,
                    action = "RECORD_TEST",
                    targetType = "TEST",
                    targetId = testId.toString(),
                    details = "${existing.sampleCode} ${existing.testType}: ${req.resultValue} (${req.passFail})",
                    status = "ALLOWED"
                )
            )
            localServer.broadcastEvent(WsEvent("TEST_UPDATED", "${existing.sampleCode} ${existing.testType} updated", testId))
            updated.toDto()
        }
    }

    suspend fun publishFinalReport(
        session: StoredSession,
        req: PublishReportRequest
    ): Result<FinalReportDto> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (!role.canPublishReports) {
            logLocalDenied(session, "PUBLISH_REPORT", "REPORT", req.jobId.toString())
            return@withContext Result.failure(SecurityException("Guests cannot publish reports"))
        }
        if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
            return@withContext syncClient.publishReport(session.remoteHostUrl, session.token, req).also {
                if (it.isSuccess) refreshRemoteData(session.remoteHostUrl, session.token, role)
            }
        }
        runCatching {
            val job = database.jobDao().getById(req.jobId)
                ?: throw IllegalArgumentException("Job not found")
            val now = System.currentTimeMillis()
            val existing = database.finalReportDao().getByJobId(job.id)
            val entity = FinalReportEntity(
                id = existing?.id ?: 0L,
                jobId = job.id,
                jobNumber = job.jobNumber,
                reportNumber = job.reportNumber,
                customerName = job.customerName,
                reportDate = req.reportDate,
                overallResult = req.overallResult.name,
                comments = req.comments.trim(),
                publishedBy = session.fullName,
                publishedByRole = role.name,
                isPublished = req.publishNow,
                publishedAt = if (req.publishNow) now else existing?.publishedAt,
                updatedAt = now
            )
            val reportId = database.finalReportDao().upsert(entity)
            if (req.publishNow) {
                database.jobDao().updateStatus(job.id, JobCardStatus.PUBLISHED.name, now)
            }
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = session.username,
                    actorRole = role.name,
                    action = "PUBLISH_REPORT",
                    targetType = "REPORT",
                    targetId = reportId.toString(),
                    details = "Published ${job.reportNumber} (${req.overallResult.displayName})",
                    status = "ALLOWED"
                )
            )
            localServer.broadcastEvent(WsEvent("REPORT_PUBLISHED", "Report ${job.reportNumber} published!", reportId))
            val attachments = database.attachmentDao().getByJobId(job.id).map { it.toDto() }
            val tests = database.testResultDao().getByJobId(job.id).map { it.toDto() }
            entity.copy(id = reportId).toDto(attachments, tests)
        }
    }

    suspend fun uploadReportPhoto(
        session: StoredSession,
        jobId: Long,
        reportId: Long,
        sourceType: String,
        uri: Uri
    ): Result<AttachmentDto> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (!role.canUploadFiles) {
            logLocalDenied(session, "UPLOAD_FILE", "FILE", jobId.toString())
            return@withContext Result.failure(SecurityException("Guests cannot upload photos"))
        }
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Unable to read image bytes")
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val ext = if (mimeType.contains("png")) "png" else "jpg"
            val fileName = "lab_job${jobId}_${System.currentTimeMillis()}.$ext"

            if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
                val base64 = Base64.getEncoder().encodeToString(bytes)
                val uploaded = syncClient.uploadPhoto(
                    session.remoteHostUrl,
                    session.token,
                    UploadPhotoRequest(
                        reportId = reportId,
                        jobId = jobId,
                        fileName = fileName,
                        mimeType = mimeType,
                        sourceType = sourceType,
                        base64Data = base64
                    )
                ).getOrThrow()
                refreshRemoteData(session.remoteHostUrl, session.token, role)
                return@runCatching uploaded
            }

            val outFile = File(backupManager.photosDir, fileName)
            outFile.writeBytes(bytes)
            val entity = ReportAttachmentEntity(
                reportId = reportId,
                jobId = jobId,
                fileName = fileName,
                localFilePath = outFile.absolutePath,
                mimeType = mimeType,
                fileSizeBytes = bytes.size.toLong(),
                sourceType = sourceType,
                uploadedBy = session.username,
                uploadedAt = System.currentTimeMillis()
            )
            val savedId = database.attachmentDao().insert(entity)
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = session.username,
                    actorRole = role.name,
                    action = "UPLOAD_FILE",
                    targetType = "FILE",
                    targetId = savedId.toString(),
                    details = "Uploaded $fileName via $sourceType",
                    status = "ALLOWED"
                )
            )
            localServer.broadcastEvent(WsEvent("FILE_UPLOADED", "Photo $fileName uploaded", savedId))
            entity.copy(id = savedId).toDto()
        }
    }

    suspend fun deleteAttachment(
        session: StoredSession,
        attachmentId: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
            return@withContext syncClient.deleteFile(session.remoteHostUrl, session.token, attachmentId).also {
                if (it.isSuccess) refreshRemoteData(session.remoteHostUrl, session.token, role)
            }
        }
        // Strict server-side / host-side RBAC check: Technicians & Guests are NEVER permitted to delete files.
        if (!role.canDeleteFiles) {
            logLocalDenied(session, "DELETE_FILE", "FILE", attachmentId.toString())
            return@withContext Result.failure(
                SecurityException("403 Forbidden: ${role.displayName} role does not have file deletion permission. Only Admin can delete files.")
            )
        }
        runCatching {
            val record = database.attachmentDao().getById(attachmentId)
                ?: throw IllegalArgumentException("File not found")
            val f = File(record.localFilePath)
            if (f.exists()) f.delete()
            database.attachmentDao().deleteById(attachmentId)
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = session.username,
                    actorRole = role.name,
                    action = "DELETE_FILE",
                    targetType = "FILE",
                    targetId = attachmentId.toString(),
                    details = "Deleted ${record.fileName}",
                    status = "ALLOWED"
                )
            )
            localServer.broadcastEvent(WsEvent("FILE_DELETED", "File ${record.fileName} deleted by Admin", attachmentId))
            Unit
        }
    }

    suspend fun createUser(session: StoredSession, req: CreateUserRequest): Result<UserDto> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (!role.canManageAdmin) {
            logLocalDenied(session, "CREATE_USER", "USER", req.username)
            return@withContext Result.failure(SecurityException("403 Forbidden: Admin only"))
        }
        if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
            return@withContext syncClient.createUser(session.remoteHostUrl, session.token, req).also {
                if (it.isSuccess) refreshRemoteData(session.remoteHostUrl, session.token, role)
            }
        }
        runCatching {
            val entity = UserEntity(
                username = req.username.trim(),
                fullName = req.fullName.trim(),
                role = req.role.name,
                passwordHash = SecurityUtils.hashPassword(req.password),
                isActive = true
            )
            val id = database.userDao().insert(entity)
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = session.username,
                    actorRole = role.name,
                    action = "CREATE_USER",
                    targetType = "USER",
                    targetId = id.toString(),
                    details = "Created user ${req.username} (${req.role.displayName})",
                    status = "ALLOWED"
                )
            )
            entity.copy(id = id).toDto()
        }
    }

    suspend fun toggleUserActive(session: StoredSession, userId: Long, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (!role.canManageAdmin) {
            logLocalDenied(session, "TOGGLE_USER", "USER", userId.toString())
            return@withContext Result.failure(SecurityException("403 Forbidden: Admin only"))
        }
        if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
            return@withContext syncClient.toggleUser(session.remoteHostUrl, session.token, userId, isActive).map {
                refreshRemoteData(session.remoteHostUrl, session.token, role)
            }
        }
        runCatching {
            database.userDao().setActive(userId, isActive)
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = session.username,
                    actorRole = role.name,
                    action = "TOGGLE_USER",
                    targetType = "USER",
                    targetId = userId.toString(),
                    details = "Set active=$isActive",
                    status = "ALLOWED"
                )
            )
            Unit
        }
    }

    suspend fun triggerManualBackup(session: StoredSession): Result<BackupRecordDto> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (!role.canManageAdmin) {
            logLocalDenied(session, "CREATE_BACKUP", "BACKUP", "manual")
            return@withContext Result.failure(SecurityException("403 Forbidden: Admin only"))
        }
        if (session.nodeMode == LabNodeMode.CLIENT_NODE) {
            return@withContext syncClient.triggerBackup(session.remoteHostUrl, session.token).also {
                if (it.isSuccess) refreshRemoteData(session.remoteHostUrl, session.token, role)
            }
        }
        backupManager.createBackup("MANUAL", session.username).map { it.toDto() }
    }

    suspend fun deleteBackup(session: StoredSession, backupId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        val role = session.role ?: return@withContext Result.failure(SecurityException("Not logged in"))
        if (!role.canManageAdmin) {
            logLocalDenied(session, "DELETE_BACKUP", "BACKUP", backupId.toString())
            return@withContext Result.failure(SecurityException("403 Forbidden: Admin only"))
        }
        runCatching {
            backupManager.deleteBackup(backupId, session.username)
            Unit
        }
    }

    private suspend fun logLocalDenied(session: StoredSession, action: String, targetType: String, targetId: String) {
        database.auditLogDao().insert(
            AuditLogEntity(
                actorUsername = session.username.ifBlank { "anonymous" },
                actorRole = session.role?.name ?: "NONE",
                action = action,
                targetType = targetType,
                targetId = targetId,
                details = "Rejected ${session.role?.name ?: "NONE"} attempt for $action",
                status = "DENIED"
            )
        )
    }
}
