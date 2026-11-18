package com.example.data.network

import com.example.data.model.ApiErrorResponse
import com.example.data.model.AttachmentDto
import com.example.data.model.AuditLogDto
import com.example.data.model.BackupRecordDto
import com.example.data.model.CreateSampleRequest
import com.example.data.model.CreateUserRequest
import com.example.data.model.FinalReportDto
import com.example.data.model.JobDto
import com.example.data.model.LabDashboardStats
import com.example.data.model.LoginRequest
import com.example.data.model.LoginResponse
import com.example.data.model.PublishReportRequest
import com.example.data.model.SampleDto
import com.example.data.model.ServerHealthDto
import com.example.data.model.TestResultDto
import com.example.data.model.ToggleUserRequest
import com.example.data.model.UpdateSampleStatusRequest
import com.example.data.model.UpdateTestResultRequest
import com.example.data.model.UploadPhotoRequest
import com.example.data.model.UpsertJobRequest
import com.example.data.model.UserDto
import com.example.data.model.WsEvent
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class LabSyncClient(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wsJob: Job? = null

    private val client = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(5, TimeUnit.SECONDS)
                readTimeout(15, TimeUnit.SECONDS)
                writeTimeout(15, TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(WebSockets)
    }

    private val _wsConnected = MutableStateFlow(false)
    val wsConnected: StateFlow<Boolean> = _wsConnected.asStateFlow()

    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<WsEvent> = _events.asSharedFlow()

    fun connectWebSocket(baseUrl: String) {
        wsJob?.cancel()
        val wsUrl = baseUrl.trimEnd('/')
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "/ws/updates"

        wsJob = scope.launch {
            while (isActive) {
                try {
                    client.webSocket(urlString = wsUrl) {
                        _wsConnected.value = true
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                runCatching {
                                    json.decodeFromString(WsEvent.serializer(), text)
                                }.onSuccess { event ->
                                    _events.tryEmit(event)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                } finally {
                    _wsConnected.value = false
                }
                delay(3_000L)
            }
        }
    }

    fun disconnectWebSocket() {
        wsJob?.cancel()
        wsJob = null
        _wsConnected.value = false
    }

    suspend fun checkHealth(baseUrl: String): Result<ServerHealthDto> = runCatching {
        val res = client.get("${baseUrl.trimEnd('/')}/api/health")
        parseOrThrow(res)
    }

    suspend fun login(baseUrl: String, request: LoginRequest): Result<LoginResponse> = runCatching {
        val res = client.post("${baseUrl.trimEnd('/')}/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        parseOrThrow(res)
    }

    suspend fun getDashboard(baseUrl: String, token: String): Result<LabDashboardStats> = runCatching {
        val res = client.get("${baseUrl.trimEnd('/')}/api/dashboard") { bearerAuth(token) }
        parseOrThrow(res)
    }

    suspend fun getJobs(baseUrl: String, token: String): Result<List<JobDto>> = runCatching {
        val res = client.get("${baseUrl.trimEnd('/')}/api/jobs") { bearerAuth(token) }
        parseOrThrow(res)
    }

    suspend fun upsertJob(baseUrl: String, token: String, req: UpsertJobRequest): Result<JobDto> = runCatching {
        val res = client.post("${baseUrl.trimEnd('/')}/api/jobs") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        parseOrThrow(res)
    }

    suspend fun getSamples(baseUrl: String, token: String): Result<List<SampleDto>> = runCatching {
        val res = client.get("${baseUrl.trimEnd('/')}/api/samples") { bearerAuth(token) }
        parseOrThrow(res)
    }

    suspend fun createSample(baseUrl: String, token: String, req: CreateSampleRequest): Result<SampleDto> = runCatching {
        val res = client.post("${baseUrl.trimEnd('/')}/api/samples") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        parseOrThrow(res)
    }

    suspend fun updateSample(baseUrl: String, token: String, id: Long, req: UpdateSampleStatusRequest): Result<SampleDto> = runCatching {
        val res = client.put("${baseUrl.trimEnd('/')}/api/samples/$id") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        parseOrThrow(res)
    }

    suspend fun getTests(baseUrl: String, token: String): Result<List<TestResultDto>> = runCatching {
        val res = client.get("${baseUrl.trimEnd('/')}/api/tests") { bearerAuth(token) }
        parseOrThrow(res)
    }

    suspend fun updateTest(baseUrl: String, token: String, id: Long, req: UpdateTestResultRequest): Result<TestResultDto> = runCatching {
        val res = client.put("${baseUrl.trimEnd('/')}/api/tests/$id") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        parseOrThrow(res)
    }

    suspend fun getReports(baseUrl: String, token: String, publishedOnly: Boolean): Result<List<FinalReportDto>> = runCatching {
        val endpoint = if (publishedOnly) "/api/reports/published" else "/api/reports"
        val res = client.get("${baseUrl.trimEnd('/')}$endpoint") { bearerAuth(token) }
        parseOrThrow(res)
    }

    suspend fun publishReport(baseUrl: String, token: String, req: PublishReportRequest): Result<FinalReportDto> = runCatching {
        val res = client.post("${baseUrl.trimEnd('/')}/api/reports/publish") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        parseOrThrow(res)
    }

    suspend fun uploadPhoto(baseUrl: String, token: String, req: UploadPhotoRequest): Result<AttachmentDto> = runCatching {
        val res = client.post("${baseUrl.trimEnd('/')}/api/files/upload") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        parseOrThrow(res)
    }

    suspend fun deleteFile(baseUrl: String, token: String, fileId: Long): Result<Unit> = runCatching {
        val res = client.delete("${baseUrl.trimEnd('/')}/api/files/$fileId") { bearerAuth(token) }
        if (!res.status.isSuccess()) {
            val errMsg = runCatching {
                json.decodeFromString(ApiErrorResponse.serializer(), res.bodyAsText()).error
            }.getOrElse { "HTTP ${res.status.value}: Request rejected by server" }
            throw IllegalStateException(errMsg)
        }
    }

    suspend fun getUsers(baseUrl: String, token: String): Result<List<UserDto>> = runCatching {
        val res = client.get("${baseUrl.trimEnd('/')}/api/users") { bearerAuth(token) }
        parseOrThrow(res)
    }

    suspend fun createUser(baseUrl: String, token: String, req: CreateUserRequest): Result<UserDto> = runCatching {
        val res = client.post("${baseUrl.trimEnd('/')}/api/users") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        parseOrThrow(res)
    }

    suspend fun toggleUser(baseUrl: String, token: String, id: Long, isActive: Boolean): Result<UserDto> = runCatching {
        val res = client.put("${baseUrl.trimEnd('/')}/api/users/$id/toggle") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(ToggleUserRequest(isActive))
        }
        parseOrThrow(res)
    }

    suspend fun getAuditLogs(baseUrl: String, token: String): Result<List<AuditLogDto>> = runCatching {
        val res = client.get("${baseUrl.trimEnd('/')}/api/audit-logs") { bearerAuth(token) }
        parseOrThrow(res)
    }

    suspend fun getBackups(baseUrl: String, token: String): Result<List<BackupRecordDto>> = runCatching {
        val res = client.get("${baseUrl.trimEnd('/')}/api/backups") { bearerAuth(token) }
        parseOrThrow(res)
    }

    suspend fun triggerBackup(baseUrl: String, token: String): Result<BackupRecordDto> = runCatching {
        val res = client.post("${baseUrl.trimEnd('/')}/api/backups") { bearerAuth(token) }
        parseOrThrow(res)
    }

    private suspend inline fun <reified T> parseOrThrow(response: HttpResponse): T {
        if (response.status.isSuccess()) {
            return response.body()
        }
        val text = response.bodyAsText()
        val msg = runCatching {
            json.decodeFromString(ApiErrorResponse.serializer(), text).error
        }.getOrElse { "HTTP ${response.status.value}: $text" }
        throw IllegalStateException(msg)
    }
}
