package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val fullName: String,
    val role: String,
    val passwordHash: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "jobs",
    indices = [
        Index(value = ["jobNumber"], unique = true),
        Index(value = ["reportNumber"], unique = true)
    ]
)
data class JobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobNumber: String,
    val reportNumber: String,
    val customerName: String,
    val buyerName: String,
    val quantity: String,
    val receivedDate: String,
    val processesCsv: String,
    val urgency: String,
    val remarks: String,
    val status: String,
    val createdBy: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "samples",
    indices = [
        Index(value = ["sampleCode"], unique = true),
        Index(value = ["jobId"])
    ]
)
data class SampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sampleCode: String,
    val jobId: Long,
    val jobNumber: String,
    val fabricDescription: String,
    val colorShade: String,
    val assignedTechnician: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "test_results",
    indices = [
        Index(value = ["sampleId"]),
        Index(value = ["jobId"])
    ]
)
data class TestResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sampleId: Long,
    val sampleCode: String,
    val jobId: Long,
    val jobNumber: String,
    val testType: String,
    val resultValue: String,
    val unit: String,
    val standardSpec: String,
    val passFail: String,
    val technicianName: String,
    val testedDate: String,
    val status: String,
    val remarks: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "final_reports",
    indices = [
        Index(value = ["jobId"], unique = true),
        Index(value = ["reportNumber"])
    ]
)
data class FinalReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val jobNumber: String,
    val reportNumber: String,
    val customerName: String,
    val reportDate: String,
    val overallResult: String,
    val comments: String,
    val publishedBy: String,
    val publishedByRole: String,
    val isPublished: Boolean,
    val publishedAt: Long?,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "report_attachments",
    indices = [
        Index(value = ["reportId"]),
        Index(value = ["jobId"])
    ]
)
data class ReportAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportId: Long,
    val jobId: Long,
    val fileName: String,
    val localFilePath: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val sourceType: String,
    val uploadedBy: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actorUsername: String,
    val actorRole: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val details: String,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "backup_records")
data class BackupRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val backupType: String,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
)
