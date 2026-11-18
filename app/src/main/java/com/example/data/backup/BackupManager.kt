package com.example.data.backup

import android.content.Context
import com.example.data.local.TextileLabDatabase
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.BackupRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: TextileLabDatabase
) {
    val photosDir: File
        get() = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    val backupsDir: File
        get() = File(context.filesDir, "backups").apply { if (!exists()) mkdirs() }

    suspend fun createBackup(
        backupType: String,
        createdBy: String
    ): Result<BackupRecordEntity> = withContext(Dispatchers.IO) {
        runCatching {
            database.walCheckpoint()

            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val zipFile = File(backupsDir, "textile_lab_backup_$stamp.zip")
            val dbFile = context.getDatabasePath(TextileLabDatabase.DATABASE_NAME)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                if (dbFile.exists()) {
                    addFileToZip(zos, dbFile, "database/${dbFile.name}")
                }
                val photoFiles = photosDir.listFiles()?.filter { it.isFile }.orEmpty()
                photoFiles.forEach { photo ->
                    addFileToZip(zos, photo, "photos/${photo.name}")
                }
            }

            val record = BackupRecordEntity(
                fileName = zipFile.name,
                filePath = zipFile.absolutePath,
                sizeBytes = zipFile.length(),
                backupType = backupType,
                createdBy = createdBy,
                createdAt = System.currentTimeMillis()
            )
            val id = database.backupDao().insert(record)
            database.auditLogDao().insert(
                AuditLogEntity(
                    actorUsername = createdBy,
                    actorRole = if (backupType == "AUTO_DAILY") "SYSTEM" else "ADMIN",
                    action = "CREATE_BACKUP",
                    targetType = "BACKUP",
                    targetId = id.toString(),
                    details = "Created $backupType backup ${zipFile.name} (${zipFile.length()} bytes)",
                    status = "ALLOWED"
                )
            )
            record.copy(id = id)
        }
    }

    suspend fun deleteBackup(id: Long, actorUsername: String): Boolean = withContext(Dispatchers.IO) {
        val record = database.backupDao().getById(id) ?: return@withContext false
        val file = File(record.filePath)
        if (file.exists()) {
            file.delete()
        }
        database.backupDao().deleteById(id)
        database.auditLogDao().insert(
            AuditLogEntity(
                actorUsername = actorUsername,
                actorRole = "ADMIN",
                action = "DELETE_BACKUP",
                targetType = "BACKUP",
                targetId = id.toString(),
                details = "Deleted backup ${record.fileName}",
                status = "ALLOWED"
            )
        )
        true
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        val buffer = ByteArray(8192)
        BufferedInputStream(FileInputStream(file), buffer.size).use { bis ->
            zos.putNextEntry(ZipEntry(entryName))
            var count: Int
            while (bis.read(buffer).also { count = it } != -1) {
                zos.write(buffer, 0, count)
            }
            zos.closeEntry()
        }
    }
}
