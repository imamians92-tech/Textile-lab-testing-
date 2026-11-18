package com.example.data.local

import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.BackupRecordEntity
import com.example.data.local.entities.FinalReportEntity
import com.example.data.local.entities.JobEntity
import com.example.data.local.entities.ReportAttachmentEntity
import com.example.data.local.entities.SampleEntity
import com.example.data.local.entities.TestResultEntity
import com.example.data.local.entities.UserEntity
import com.example.data.model.AttachmentDto
import com.example.data.model.AuditLogDto
import com.example.data.model.BackupRecordDto
import com.example.data.model.FinalReportDto
import com.example.data.model.JobCardStatus
import com.example.data.model.JobDto
import com.example.data.model.SampleDto
import com.example.data.model.SampleStatus
import com.example.data.model.TestPassFail
import com.example.data.model.TestResultDto
import com.example.data.model.TestStatus
import com.example.data.model.TextileTestType
import com.example.data.model.UrgencyLevel
import com.example.data.model.UserDto
import com.example.data.model.UserRole

fun UserEntity.toDto(): UserDto = UserDto(
    id = id,
    username = username,
    fullName = fullName,
    role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.GUEST),
    isActive = isActive,
    createdAt = createdAt
)

fun JobEntity.toDto(): JobDto = JobDto(
    id = id,
    jobNumber = jobNumber,
    reportNumber = reportNumber,
    customerName = customerName,
    buyerName = buyerName,
    quantity = quantity,
    receivedDate = receivedDate,
    processes = processesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    urgency = runCatching { UrgencyLevel.valueOf(urgency) }.getOrDefault(UrgencyLevel.NORMAL),
    remarks = remarks,
    status = runCatching { JobCardStatus.valueOf(status) }.getOrDefault(JobCardStatus.RECEIVED),
    createdBy = createdBy,
    updatedAt = updatedAt
)

fun SampleEntity.toDto(): SampleDto = SampleDto(
    id = id,
    sampleCode = sampleCode,
    jobId = jobId,
    jobNumber = jobNumber,
    fabricDescription = fabricDescription,
    colorShade = colorShade,
    assignedTechnician = assignedTechnician,
    status = runCatching { SampleStatus.valueOf(status) }.getOrDefault(SampleStatus.PENDING),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun TestResultEntity.toDto(): TestResultDto = TestResultDto(
    id = id,
    sampleId = sampleId,
    sampleCode = sampleCode,
    jobId = jobId,
    jobNumber = jobNumber,
    testType = runCatching { TextileTestType.valueOf(testType) }.getOrDefault(TextileTestType.GSM),
    resultValue = resultValue,
    unit = unit,
    standardSpec = standardSpec,
    passFail = runCatching { TestPassFail.valueOf(passFail) }.getOrDefault(TestPassFail.PENDING),
    technicianName = technicianName,
    testedDate = testedDate,
    status = runCatching { TestStatus.valueOf(status) }.getOrDefault(TestStatus.PENDING),
    remarks = remarks,
    updatedAt = updatedAt
)

fun ReportAttachmentEntity.toDto(): AttachmentDto = AttachmentDto(
    id = id,
    reportId = reportId,
    jobId = jobId,
    fileName = fileName,
    localFilePath = localFilePath,
    mimeType = mimeType,
    fileSizeBytes = fileSizeBytes,
    sourceType = sourceType,
    uploadedBy = uploadedBy,
    uploadedAt = uploadedAt
)

fun FinalReportEntity.toDto(
    attachments: List<AttachmentDto> = emptyList(),
    testResults: List<TestResultDto> = emptyList()
): FinalReportDto = FinalReportDto(
    id = id,
    jobId = jobId,
    jobNumber = jobNumber,
    reportNumber = reportNumber,
    customerName = customerName,
    reportDate = reportDate,
    overallResult = runCatching { TestPassFail.valueOf(overallResult) }.getOrDefault(TestPassFail.PENDING),
    comments = comments,
    publishedBy = publishedBy,
    publishedByRole = publishedByRole,
    isPublished = isPublished,
    publishedAt = publishedAt,
    updatedAt = updatedAt,
    attachments = attachments,
    testResults = testResults
)

fun AuditLogEntity.toDto(): AuditLogDto = AuditLogDto(
    id = id,
    actorUsername = actorUsername,
    actorRole = actorRole,
    action = action,
    targetType = targetType,
    targetId = targetId,
    details = details,
    status = status,
    timestamp = timestamp
)

fun BackupRecordEntity.toDto(): BackupRecordDto = BackupRecordDto(
    id = id,
    fileName = fileName,
    filePath = filePath,
    sizeBytes = sizeBytes,
    backupType = backupType,
    createdBy = createdBy,
    createdAt = createdAt
)
