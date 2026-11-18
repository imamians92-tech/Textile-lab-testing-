package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.BackupRecordEntity
import com.example.data.local.entities.FinalReportEntity
import com.example.data.local.entities.JobEntity
import com.example.data.local.entities.ReportAttachmentEntity
import com.example.data.local.entities.SampleEntity
import com.example.data.local.entities.TestResultEntity
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    fun observeUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY createdAt ASC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity): Long

    @Query("UPDATE users SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    @Query("UPDATE users SET passwordHash = :passwordHash WHERE id = :id")
    suspend fun updatePassword(id: Long, passwordHash: String)
}

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs ORDER BY updatedAt DESC")
    suspend fun getAll(): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): JobEntity?

    @Query("SELECT * FROM jobs WHERE jobNumber = :jobNumber LIMIT 1")
    suspend fun getByJobNumber(jobNumber: String): JobEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(job: JobEntity): Long

    @Update
    suspend fun update(job: JobEntity)

    @Query("UPDATE jobs SET status = :status, updatedAt = :updatedAt WHERE id = :jobId")
    suspend fun updateStatus(jobId: Long, status: String, updatedAt: Long)

    @RawQuery
    suspend fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}

@Dao
interface SampleDao {
    @Query("SELECT * FROM samples ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SampleEntity>>

    @Query("SELECT * FROM samples ORDER BY createdAt DESC")
    suspend fun getAll(): List<SampleEntity>

    @Query("SELECT * FROM samples WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SampleEntity?

    @Query("SELECT * FROM samples WHERE jobId = :jobId ORDER BY createdAt DESC")
    suspend fun getByJobId(jobId: Long): List<SampleEntity>

    @Query("SELECT COUNT(*) FROM samples")
    suspend fun countSamples(): Int

    @Query("SELECT MAX(id) FROM samples")
    suspend fun maxId(): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(sample: SampleEntity): Long

    @Query("UPDATE samples SET status = :status, assignedTechnician = :technician, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, technician: String, updatedAt: Long)
}

@Dao
interface TestResultDao {
    @Query("SELECT * FROM test_results ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TestResultEntity>>

    @Query("SELECT * FROM test_results ORDER BY id ASC")
    suspend fun getAll(): List<TestResultEntity>

    @Query("SELECT * FROM test_results WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TestResultEntity?

    @Query("SELECT * FROM test_results WHERE jobId = :jobId ORDER BY sampleId ASC, id ASC")
    suspend fun getByJobId(jobId: Long): List<TestResultEntity>

    @Query("SELECT * FROM test_results WHERE sampleId = :sampleId ORDER BY id ASC")
    suspend fun getBySampleId(sampleId: Long): List<TestResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<TestResultEntity>)

    @Update
    suspend fun update(result: TestResultEntity)
}

@Dao
interface FinalReportDao {
    @Query("SELECT * FROM final_reports ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<FinalReportEntity>>

    @Query("SELECT * FROM final_reports WHERE isPublished = 1 ORDER BY publishedAt DESC")
    fun observePublished(): Flow<List<FinalReportEntity>>

    @Query("SELECT * FROM final_reports ORDER BY updatedAt DESC")
    suspend fun getAll(): List<FinalReportEntity>

    @Query("SELECT * FROM final_reports WHERE isPublished = 1 ORDER BY publishedAt DESC")
    suspend fun getPublished(): List<FinalReportEntity>

    @Query("SELECT * FROM final_reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FinalReportEntity?

    @Query("SELECT * FROM final_reports WHERE jobId = :jobId LIMIT 1")
    suspend fun getByJobId(jobId: Long): FinalReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(report: FinalReportEntity): Long

    @Update
    suspend fun update(report: FinalReportEntity)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM report_attachments ORDER BY uploadedAt DESC")
    fun observeAll(): Flow<List<ReportAttachmentEntity>>

    @Query("SELECT * FROM report_attachments ORDER BY uploadedAt DESC")
    suspend fun getAll(): List<ReportAttachmentEntity>

    @Query("SELECT * FROM report_attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReportAttachmentEntity?

    @Query("SELECT * FROM report_attachments WHERE reportId = :reportId ORDER BY uploadedAt DESC")
    suspend fun getByReportId(reportId: Long): List<ReportAttachmentEntity>

    @Query("SELECT * FROM report_attachments WHERE jobId = :jobId ORDER BY uploadedAt DESC")
    suspend fun getByJobId(jobId: Long): List<ReportAttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: ReportAttachmentEntity): Long

    @Query("DELETE FROM report_attachments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 300")
    fun observeRecent(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 300")
    suspend fun getRecent(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity): Long
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BackupRecordEntity>>

    @Query("SELECT * FROM backup_records ORDER BY createdAt DESC")
    suspend fun getAll(): List<BackupRecordEntity>

    @Query("SELECT * FROM backup_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BackupRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BackupRecordEntity): Long

    @Query("DELETE FROM backup_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
