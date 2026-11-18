package com.example.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole(val displayName: String) {
    ADMIN("Admin"),
    TECHNICIAN("Technician"),
    GUEST("Guest");

    val canEditLabData: Boolean get() = this == ADMIN || this == TECHNICIAN
    val canUploadFiles: Boolean get() = this == ADMIN || this == TECHNICIAN
    val canPublishReports: Boolean get() = this == ADMIN || this == TECHNICIAN
    val canDeleteFiles: Boolean get() = this == ADMIN
    val canManageAdmin: Boolean get() = this == ADMIN
}

@Serializable
enum class UrgencyLevel(val displayName: String) {
    NORMAL("Normal"),
    HIGH("High"),
    URGENT("Urgent")
}

@Serializable
enum class JobCardStatus(val displayName: String) {
    RECEIVED("Received"),
    IN_TESTING("In Testing"),
    COMPLETED("Completed"),
    PUBLISHED("Published")
}

@Serializable
enum class SampleStatus(val displayName: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

@Serializable
enum class TextileTestType(
    val displayName: String,
    val unit: String,
    val defaultSpec: String
) {
    GSM("GSM", "g/m²", "160 - 220 g/m²"),
    WIDTH("Width", "in", "58 - 60 in"),
    TEAR("Tear", "N", ">= 18.0 N"),
    TENSILE("Tensile", "N", ">= 250 N"),
    STRETCH_GROWTH("Stretch Growth", "%", "<= 5.0 %"),
    PILLING("Pilling", "Grade", ">= 4.0"),
    ABRASION("Abrasion", "Cycles", ">= 20,000"),
    SHRINKAGE("Shrinkage", "%", "-3.0 to +1.0 %"),
    WASHING("Washing", "Grade", ">= 4.0")
}

@Serializable
enum class TestStatus(val displayName: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

@Serializable
enum class TestPassFail(val displayName: String) {
    PENDING("Pending"),
    PASS("PASS"),
    FAIL("FAIL")
}

@Serializable
enum class LabNodeMode(val displayName: String) {
    HOST_SERVER("Local Wi-Fi Server (Host)"),
    CLIENT_NODE("Wi-Fi Client Device")
}

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    val fullName: String,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: Long
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDto,
    val serverHost: String
)

@Serializable
data class CreateUserRequest(
    val username: String,
    val fullName: String,
    val password: String,
    val role: UserRole
)

@Serializable
data class ToggleUserRequest(
    val isActive: Boolean
)

@Serializable
data class JobDto(
    val id: Long,
    val jobNumber: String,
    val reportNumber: String,
    val customerName: String,
    val buyerName: String,
    val quantity: String,
    val receivedDate: String,
    val processes: List<String>,
    val urgency: UrgencyLevel,
    val remarks: String,
    val status: JobCardStatus,
    val createdBy: String,
    val updatedAt: Long
)

@Serializable
data class UpsertJobRequest(
    val id: Long = 0,
    val jobNumber: String,
    val reportNumber: String,
    val customerName: String,
    val buyerName: String,
    val quantity: String,
    val receivedDate: String,
    val processes: List<String>,
    val urgency: UrgencyLevel,
    val remarks: String,
    val status: JobCardStatus = JobCardStatus.RECEIVED
)

@Serializable
data class SampleDto(
    val id: Long,
    val sampleCode: String,
    val jobId: Long,
    val jobNumber: String,
    val fabricDescription: String,
    val colorShade: String,
    val assignedTechnician: String,
    val status: SampleStatus,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CreateSampleRequest(
    val jobId: Long,
    val fabricDescription: String,
    val colorShade: String,
    val assignedTechnician: String,
    val selectedTests: List<TextileTestType> = TextileTestType.entries
)

@Serializable
data class UpdateSampleStatusRequest(
    val status: SampleStatus,
    val assignedTechnician: String
)

@Serializable
data class TestResultDto(
    val id: Long,
    val sampleId: Long,
    val sampleCode: String,
    val jobId: Long,
    val jobNumber: String,
    val testType: TextileTestType,
    val resultValue: String,
    val unit: String,
    val standardSpec: String,
    val passFail: TestPassFail,
    val technicianName: String,
    val testedDate: String,
    val status: TestStatus,
    val remarks: String,
    val updatedAt: Long
)

@Serializable
data class UpdateTestResultRequest(
    val resultValue: String,
    val standardSpec: String,
    val passFail: TestPassFail,
    val technicianName: String,
    val testedDate: String,
    val status: TestStatus,
    val remarks: String
)

@Serializable
data class AttachmentDto(
    val id: Long,
    val reportId: Long,
    val jobId: Long,
    val fileName: String,
    val localFilePath: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val sourceType: String,
    val uploadedBy: String,
    val uploadedAt: Long
)

@Serializable
data class UploadPhotoRequest(
    val reportId: Long,
    val jobId: Long,
    val fileName: String,
    val mimeType: String,
    val sourceType: String,
    val base64Data: String
)

@Serializable
data class FinalReportDto(
    val id: Long,
    val jobId: Long,
    val jobNumber: String,
    val reportNumber: String,
    val customerName: String,
    val reportDate: String,
    val overallResult: TestPassFail,
    val comments: String,
    val publishedBy: String,
    val publishedByRole: String,
    val isPublished: Boolean,
    val publishedAt: Long?,
    val updatedAt: Long,
    val attachments: List<AttachmentDto> = emptyList(),
    val testResults: List<TestResultDto> = emptyList()
)

@Serializable
data class PublishReportRequest(
    val jobId: Long,
    val reportDate: String,
    val overallResult: TestPassFail,
    val comments: String,
    val publishNow: Boolean = true
)

@Serializable
data class AuditLogDto(
    val id: Long,
    val actorUsername: String,
    val actorRole: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val details: String,
    val status: String,
    val timestamp: Long
)

@Serializable
data class BackupRecordDto(
    val id: Long,
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val backupType: String,
    val createdBy: String,
    val createdAt: Long
)

@Serializable
data class LabDashboardStats(
    val totalJobs: Int = 0,
    val urgentJobs: Int = 0,
    val totalSamples: Int = 0,
    val pendingSamples: Int = 0,
    val completedSamples: Int = 0,
    val pendingTests: Int = 0,
    val completedTests: Int = 0,
    val publishedReports: Int = 0,
    val passedReports: Int = 0,
    val failedReports: Int = 0
)

@Serializable
data class WsEvent(
    val eventType: String,
    val message: String,
    val entityId: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ServerHealthDto(
    val status: String,
    val hostIp: String,
    val port: Int,
    val connectedClients: Int,
    val totalJobs: Int,
    val publishedReports: Int,
    val timestamp: Long
)

@Serializable
data class ApiErrorResponse(
    val error: String,
    val code: Int
)
