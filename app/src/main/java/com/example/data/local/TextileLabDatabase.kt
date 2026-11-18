package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.data.local.dao.AttachmentDao
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.BackupDao
import com.example.data.local.dao.FinalReportDao
import com.example.data.local.dao.JobDao
import com.example.data.local.dao.SampleDao
import com.example.data.local.dao.TestResultDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.BackupRecordEntity
import com.example.data.local.entities.FinalReportEntity
import com.example.data.local.entities.JobEntity
import com.example.data.local.entities.ReportAttachmentEntity
import com.example.data.local.entities.SampleEntity
import com.example.data.local.entities.TestResultEntity
import com.example.data.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        JobEntity::class,
        SampleEntity::class,
        TestResultEntity::class,
        FinalReportEntity::class,
        ReportAttachmentEntity::class,
        AuditLogEntity::class,
        BackupRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TextileLabDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun jobDao(): JobDao
    abstract fun sampleDao(): SampleDao
    abstract fun testResultDao(): TestResultDao
    abstract fun finalReportDao(): FinalReportDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun backupDao(): BackupDao

    suspend fun walCheckpoint() {
        jobDao().checkpoint(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))
    }

    companion object {
        const val DATABASE_NAME = "textile_lab_pro.db"

        @Volatile
        private var INSTANCE: TextileLabDatabase? = null

        fun getInstance(context: Context): TextileLabDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TextileLabDatabase::class.java,
                    DATABASE_NAME
                ).build().also { INSTANCE = it }
            }
        }
    }
}
