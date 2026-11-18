package com.example.data.server

import com.example.core.NetworkUtils
import com.example.core.SecurityUtils
import com.example.data.backup.BackupManager
import com.example.data.local.TextileLabDatabase
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.FinalReportEntity
import com.example.data.local.entities.JobEntity
import com.example.data.local.entities.ReportAttachmentEntity
import com.example.data.local.entities.SampleEntity
import com.example.data.local.entities.TestResultEntity
import com.example.data.local.entities.UserEntity
import com.example.data.local.toDto
import com.example.data.model.ApiErrorResponse
import com.example.data.model.CreateSampleRequest
import com.example.data.model.CreateUserRequest
import com.example.data.model.JobCardStatus
import com.example.data.model.LabDashboardStats
import com.example.data.model.LoginRequest
import com.example.data.model.LoginResponse
import com.example.data.model.PublishReportRequest
import com.example.data.model.SampleStatus
import com.example.data.model.ServerHealthDto
import com.example.data.model.TestPassFail
import com.example.data.model.TestStatus
import com.example.data.model.TextileTestType
import com.example.data.model.ToggleUserRequest
import com.example.data.model.UpdateSampleStatusRequest
import com.example.data.model.UpdateTestResultRequest
import com.example.data.model.UploadPhotoRequest
import com.example.data.model.UpsertJobRequest
import com.example.data.model.UserDto
import com.example.data.model.UserRole
import com.example.data.model.WsEvent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Base64
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class LabLocalServer(
    private val database: TextileLabDatabase,
    private val backupManager: BackupManager,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var engine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    private val activeSessions = ConcurrentHashMap<String, UserDto>()
    private val wsSessions = CopyOnWriteArraySet<DefaultWebSocketServerSession>()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients.asStateFlow()

    private val _serverHostAddress = MutableStateFlow("http://${NetworkUtils.getLocalWifiIpAddress()}:${NetworkUtils.DEFAULT_SERVER_PORT}")
    val serverHostAddress: StateFlow<String> = _serverHostAddress.asStateFlow()

    fun registerLocalSession(token: String, user: UserDto) {
        activeSessions[token] = user
    }

    fun start(port: Int = NetworkUtils.DEFAULT_SERVER_PORT) {
        if (_isRunning.value) return
        val ip = NetworkUtils.getLocalWifiIpAddress()
        _serverHostAddress.value = "http://$ip:$port"

        val server = embeddedServer(CIO, host = "0.0.0.0", port = port) {
            install(ContentNegotiation) {
                json(json)
            }
            install(WebSockets)
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiErrorResponse(cause.message ?: "Server error", 500)
                    )
                }
            }

            routing {
                get("/api/health") {
                    val jobs = database.jobDao().getAll().size
                    val published = database.finalReportDao().getPublished().size
                    call.respond(
                        ServerHealthDto(
                            status = "ONLINE",
                            hostIp = NetworkUtils.getLocalWifiIpAddress(),
                            port = port,
                            connectedClients = wsSessions.size,
                            totalJobs = jobs,
                            publishedReports = published,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }

                post("/api/auth/login") {
                    val req = call.receive<LoginRequest>()
                    val userEntity = database.userDao().getByUsername(req.username.trim())
                    if (userEntity == null || !userEntity.isActive ||
                        !SecurityUtils.verifyPassword(req.password, userEntity.passwordHash)
                    ) {
                        logAudit(
                            actor = req.username.ifBlank { "anonymous" },
                            role = "UNKNOWN",
                            action = "LOGIN",
                            targetType = "AUTH",
                            targetId = req.username,
                            details = "Invalid credentials or inactive account",
                            status = "DENIED"
                        )
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            ApiErrorResponse("Invalid username or password", 401)
                        )
                        return@post
                    }
                    val token = SecurityUtils.generateSessionToken()
                    val dto = userEntity.toDto()
                    activeSessions[token] = dto
                    logAudit(
                        actor = dto.username,
                        role = dto.role.name,
                        action = "LOGIN",
                        targetType = "AUTH",
                        targetId = dto.id.toString(),
                        details = "Authenticated via LAN API",
                        status = "ALLOWED"
                    )
                    call.respond(
                        LoginResponse(
                            token = token,
                            user = dto,
                            serverHost = _serverHostAddress.value
                        )
                    )
                }

                get("/api/dashboard") {
                    val user = requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN, UserRole.GUEST))
                        ?: return@get
                    val jobs = database.jobDao().getAll()
                    val samples = database.sampleDao().getAll()
                    val tests = database.testResultDao().getAll()
                    val reports = database.finalReportDao().getPublished()
                    call.respond(
                        LabDashboardStats(
                            totalJobs = jobs.size,
                            urgentJobs = jobs.count { it.urgency == "URGENT" && it.status != "PUBLISHED" },
                            totalSamples = samples.size,
                            pendingSamples = samples.count { it.status != "COMPLETED" },
                            completedSamples = samples.count { it.status == "COMPLETED" },
                            pendingTests = tests.count { it.status != "COMPLETED" },
                            completedTests = tests.count { it.status == "COMPLETED" },
                            publishedReports = reports.size,
                            passedReports = reports.count { it.overallResult == "PASS" },
                            failedReports = reports.count { it.overallResult == "FAIL" }
                        )
                    )
                }

                get("/api/jobs") {
                    requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN, UserRole.GUEST))
                        ?: return@get
                    call.respond(database.jobDao().getAll().map { it.toDto() })
                }

                post("/api/jobs") {
                    val user = requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN)) ?: return@post
                    val req = call.receive<UpsertJobRequest>()
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
                        createdBy = user.username,
                        updatedAt = now
                    )
                    val savedId = if (req.id == 0L) {
                        database.jobDao().insert(entity)
                    } else {
                        database.jobDao().update(entity)
                        req.id
                    }
                    logAudit(user.username, user.role.name, "UPSERT_JOB", "JOB", savedId.toString(), "Job ${req.jobNumber}", "ALLOWED")
                    broadcastEvent(WsEvent("JOB_UPDATED", "Job ${req.jobNumber} saved", savedId))
                    call.respond(entity.copy(id = savedId).toDto())
                }

                get("/api/samples") {
                    requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN, UserRole.GUEST))
                        ?: return@get
                    call.respond(database.sampleDao().getAll().map { it.toDto() })
                }

                post("/api/samples") {
                    val user = requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN)) ?: return@post
                    val req = call.receive<CreateSampleRequest>()
                    val job = database.jobDao().getById(req.jobId)
                    if (job == null) {
                        call.respond(HttpStatusCode.NotFound, ApiErrorResponse("Job not found", 404))
                        return@post
                    }
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
                        assignedTechnician = req.assignedTechnician.ifBlank { user.fullName },
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
                    logAudit(user.username, user.role.name, "CREATE_SAMPLE", "SAMPLE", sampleId.toString(), "Generated $sampleCode for ${job.jobNumber}", "ALLOWED")
                    broadcastEvent(WsEvent("SAMPLE_UPDATED", "Sample $sampleCode created", sampleId))
                    call.respond(sample.copy(id = sampleId).toDto())
                }

                put("/api/samples/{id}") {
                    val user = requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN)) ?: return@put
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid sample ID", 400))
                    val req = call.receive<UpdateSampleStatusRequest>()
                    val now = System.currentTimeMillis()
                    database.sampleDao().updateStatus(id, req.status.name, req.assignedTechnician, now)
                    logAudit(user.username, user.role.name, "UPDATE_SAMPLE", "SAMPLE", id.toString(), "Status=${req.status}", "ALLOWED")
                    broadcastEvent(WsEvent("SAMPLE_UPDATED", "Sample #$id updated to ${req.status.displayName}", id))
                    val updated = database.sampleDao().getById(id)
                    if (updated != null) call.respond(updated.toDto())
                    else call.respond(HttpStatusCode.NotFound, ApiErrorResponse("Sample not found", 404))
                }

                get("/api/tests") {
                    requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN, UserRole.GUEST))
                        ?: return@get
                    call.respond(database.testResultDao().getAll().map { it.toDto() })
                }

                put("/api/tests/{id}") {
                    val user = requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN)) ?: return@put
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid test ID", 400))
                    val existing = database.testResultDao().getById(id)
                        ?: return@put call.respond(HttpStatusCode.NotFound, ApiErrorResponse("Test not found", 404))
                    val req = call.receive<UpdateTestResultRequest>()
                    val now = System.currentTimeMillis()
                    val updated = existing.copy(
                        resultValue = req.resultValue.trim(),
                        standardSpec = req.standardSpec.trim(),
                        passFail = req.passFail.name,
                        technicianName = req.technicianName.ifBlank { user.fullName },
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
                    logAudit(user.username, user.role.name, "UPDATE_TEST", "TEST", id.toString(), "${existing.testType}=${req.resultValue} (${req.passFail})", "ALLOWED")
                    broadcastEvent(WsEvent("TEST_UPDATED", "${existing.sampleCode} ${existing.testType} updated", id))
                    call.respond(updated.toDto())
                }

                get("/api/reports") {
                    requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN)) ?: return@get
                    val reports = database.finalReportDao().getAll().map { report ->
                        val attachments = database.attachmentDao().getByJobId(report.jobId).map { it.toDto() }
                        val tests = database.testResultDao().getByJobId(report.jobId).map { it.toDto() }
                        report.toDto(attachments, tests)
                    }
                    call.respond(reports)
                }

                get("/api/reports/published") {
                    requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN, UserRole.GUEST))
                        ?: return@get
                    val reports = database.finalReportDao().getPublished().map { report ->
                        val attachments = database.attachmentDao().getByJobId(report.jobId).map { it.toDto() }
                        val tests = database.testResultDao().getByJobId(report.jobId).map { it.toDto() }
                        report.toDto(attachments, tests)
                    }
                    call.respond(reports)
                }

                post("/api/reports/publish") {
                    val user = requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN)) ?: return@post
                    val req = call.receive<PublishReportRequest>()
                    val job = database.jobDao().getById(req.jobId)
                        ?: return@post call.respond(HttpStatusCode.NotFound, ApiErrorResponse("Job not found", 404))
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
                        publishedBy = user.fullName,
                        publishedByRole = user.role.name,
                        isPublished = req.publishNow,
                        publishedAt = if (req.publishNow) now else existing?.publishedAt,
                        updatedAt = now
                    )
                    val reportId = database.finalReportDao().upsert(entity)
                    if (req.publishNow) {
                        database.jobDao().updateStatus(job.id, JobCardStatus.PUBLISHED.name, now)
                    }
                    logAudit(user.username, user.role.name, "PUBLISH_REPORT", "REPORT", reportId.toString(), "Report ${job.reportNumber} (${req.overallResult})", "ALLOWED")
                    broadcastEvent(WsEvent("REPORT_PUBLISHED", "Report ${job.reportNumber} published (${req.overallResult.displayName})", reportId))
                    val attachments = database.attachmentDao().getByJobId(job.id).map { it.toDto() }
                    val tests = database.testResultDao().getByJobId(job.id).map { it.toDto() }
                    call.respond(entity.copy(id = reportId).toDto(attachments, tests))
                }

                post("/api/files/upload") {
                    val user = requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN)) ?: return@post
                    val req = call.receive<UploadPhotoRequest>()
                    val bytes = Base64.getDecoder().decode(req.base64Data)
                    val safeName = "${System.currentTimeMillis()}_${req.fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")}"
                    val outFile = File(backupManager.photosDir, safeName)
                    outFile.writeBytes(bytes)
                    val entity = ReportAttachmentEntity(
                        reportId = req.reportId,
                        jobId = req.jobId,
                        fileName = safeName,
                        localFilePath = outFile.absolutePath,
                        mimeType = req.mimeType,
                        fileSizeBytes = bytes.size.toLong(),
                        sourceType = req.sourceType,
                        uploadedBy = user.username,
                        uploadedAt = System.currentTimeMillis()
                    )
                    val savedId = database.attachmentDao().insert(entity)
                    logAudit(user.username, user.role.name, "UPLOAD_FILE", "FILE", savedId.toString(), "Uploaded $safeName (${req.sourceType})", "ALLOWED")
                    broadcastEvent(WsEvent("FILE_UPLOADED", "Photo uploaded by ${user.fullName}", savedId))
                    call.respond(entity.copy(id = savedId).toDto())
                }

                get("/api/files/{id}") {
                    requireRole(call, setOf(UserRole.ADMIN, UserRole.TECHNICIAN, UserRole.GUEST))
                        ?: return@get
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid file ID", 400))
                    val record = database.attachmentDao().getById(id)
                        ?: return@get call.respond(HttpStatusCode.NotFound, ApiErrorResponse("File not found", 404))
                    val file = File(record.localFilePath)
                    if (!file.exists()) {
                        call.respond(HttpStatusCode.NotFound, ApiErrorResponse("File missing on server storage", 404))
                        return@get
                    }
                    call.respondBytes(
                        bytes = file.readBytes(),
                        contentType = ContentType.parse(record.mimeType)
                    )
                }

                // STRICT SERVER-SIDE SECURITY ENFORCEMENT:
                // Only ADMIN can delete files. Technicians and Guests are ALWAYS rejected with 403 Forbidden.
                delete("/api/files/{id}") {
                    val user = requireRole(call, setOf(UserRole.ADMIN), actionName = "DELETE_FILE")
                        ?: return@delete
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid file ID", 400))
                    val record = database.attachmentDao().getById(id)
                        ?: return@delete call.respond(HttpStatusCode.NotFound, ApiErrorResponse("File not found", 404))
                    val file = File(record.localFilePath)
                    if (file.exists()) file.delete()
                    database.attachmentDao().deleteById(id)
                    logAudit(user.username, user.role.name, "DELETE_FILE", "FILE", id.toString(), "Deleted ${record.fileName}", "ALLOWED")
                    broadcastEvent(WsEvent("FILE_DELETED", "File ${record.fileName} deleted by Admin", id))
                    call.respond(mapOf("deleted" to "true", "id" to id.toString()))
                }

                get("/api/users") {
                    requireRole(call, setOf(UserRole.ADMIN)) ?: return@get
                    call.respond(database.userDao().getAllUsers().map { it.toDto() })
                }

                post("/api/users") {
                    val admin = requireRole(call, setOf(UserRole.ADMIN)) ?: return@post
                    val req = call.receive<CreateUserRequest>()
                    val entity = UserEntity(
                        username = req.username.trim(),
                        fullName = req.fullName.trim(),
                        role = req.role.name,
                        passwordHash = SecurityUtils.hashPassword(req.password),
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )
                    val id = database.userDao().insert(entity)
                    logAudit(admin.username, admin.role.name, "CREATE_USER", "USER", id.toString(), "Created ${req.username} (${req.role})", "ALLOWED")
                    call.respond(entity.copy(id = id).toDto())
                }

                put("/api/users/{id}/toggle") {
                    val admin = requireRole(call, setOf(UserRole.ADMIN)) ?: return@put
                    val id = call.parameters["id"]?.toLongOrNull()
                        ?: return@put call.respond(HttpStatusCode.BadRequest, ApiErrorResponse("Invalid user ID", 400))
                    val req = call.receive<ToggleUserRequest>()
                    database.userDao().setActive(id, req.isActive)
                    logAudit(admin.username, admin.role.name, "TOGGLE_USER", "USER", id.toString(), "isActive=${req.isActive}", "ALLOWED")
                    val updated = database.userDao().getById(id)
                    if (updated != null) call.respond(updated.toDto())
                    else call.respond(HttpStatusCode.NotFound, ApiErrorResponse("User not found", 404))
                }

                get("/api/audit-logs") {
                    requireRole(call, setOf(UserRole.ADMIN)) ?: return@get
                    call.respond(database.auditLogDao().getRecent().map { it.toDto() })
                }

                get("/api/backups") {
                    requireRole(call, setOf(UserRole.ADMIN)) ?: return@get
                    call.respond(database.backupDao().getAll().map { it.toDto() })
                }

                post("/api/backups") {
                    val admin = requireRole(call, setOf(UserRole.ADMIN)) ?: return@post
                    val result = backupManager.createBackup("MANUAL", admin.username)
                    result.onSuccess { record ->
                        call.respond(record.toDto())
                    }.onFailure { err ->
                        call.respond(HttpStatusCode.InternalServerError, ApiErrorResponse(err.message ?: "Backup failed", 500))
                    }
                }

                webSocket("/ws/updates") {
                    wsSessions.add(this)
                    _connectedClients.value = wsSessions.size
                    try {
                        send(Frame.Text(json.encodeToString(WsEvent.serializer(), WsEvent("CONNECTED", "Connected to Textile Lab Pro Live Stream"))))
                        for (frame in incoming) {
                            // Keep-alive listener
                        }
                    } catch (_: Exception) {
                    } finally {
                        wsSessions.remove(this)
                        _connectedClients.value = wsSessions.size
                        runCatching { close() }
                    }
                }
            }
        }

        engine = server
        server.start(wait = false)
        _isRunning.value = true
    }

    fun stop() {
        engine?.stop(gracePeriodMillis = 500, timeoutMillis = 1500)
        engine = null
        wsSessions.clear()
        _connectedClients.value = 0
        _isRunning.value = false
    }

    fun broadcastEvent(event: WsEvent) {
        if (wsSessions.isEmpty()) return
        val payload = json.encodeToString(WsEvent.serializer(), event)
        scope.launch {
            val dead = mutableListOf<DefaultWebSocketServerSession>()
            for (session in wsSessions) {
                try {
                    session.send(Frame.Text(payload))
                } catch (_: Exception) {
                    dead.add(session)
                }
            }
            if (dead.isNotEmpty()) {
                wsSessions.removeAll(dead.toSet())
                _connectedClients.value = wsSessions.size
            }
        }
    }

    private suspend fun requireRole(
        call: ApplicationCall,
        allowedRoles: Set<UserRole>,
        actionName: String = call.request.local.uri
    ): UserDto? {
        val header = call.request.headers[HttpHeaders.Authorization].orEmpty()
        val token = header.removePrefix("Bearer ").trim()
        val sessionUser = activeSessions[token]
        if (sessionUser == null) {
            call.respond(HttpStatusCode.Unauthorized, ApiErrorResponse("Missing or expired session token", 401))
            return null
        }
        val freshDbUser = database.userDao().getById(sessionUser.id)
        if (freshDbUser == null || !freshDbUser.isActive) {
            activeSessions.remove(token)
            call.respond(HttpStatusCode.Unauthorized, ApiErrorResponse("Account disabled", 401))
            return null
        }
        val freshDto = freshDbUser.toDto()
        activeSessions[token] = freshDto
        if (freshDto.role !in allowedRoles) {
            logAudit(
                actor = freshDto.username,
                role = freshDto.role.name,
                action = actionName,
                targetType = "SECURITY",
                targetId = call.request.local.uri,
                details = "Server rejected ${freshDto.role.name} request for $actionName",
                status = "DENIED"
            )
            call.respond(
                HttpStatusCode.Forbidden,
                ApiErrorResponse("403 Forbidden: ${freshDto.role.displayName} role does not have permission for $actionName", 403)
            )
            return null
        }
        return freshDto
    }

    private suspend fun logAudit(
        actor: String,
        role: String,
        action: String,
        targetType: String,
        targetId: String,
        details: String,
        status: String
    ) {
        database.auditLogDao().insert(
            AuditLogEntity(
                actorUsername = actor,
                actorRole = role,
                action = action,
                targetType = targetType,
                targetId = targetId,
                details = details,
                status = status
            )
        )
    }
}
