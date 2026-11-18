package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.SecurityUtils
import com.example.data.backup.BackupManager
import com.example.data.local.TextileLabDatabase
import com.example.data.local.entities.UserEntity
import com.example.data.model.CreateSampleRequest
import com.example.data.model.JobCardStatus
import com.example.data.model.LoginRequest
import com.example.data.model.PublishReportRequest
import com.example.data.model.TestPassFail
import com.example.data.model.TestStatus
import com.example.data.model.TextileTestType
import com.example.data.model.UpdateTestResultRequest
import com.example.data.model.UploadPhotoRequest
import com.example.data.model.UpsertJobRequest
import com.example.data.model.UrgencyLevel
import com.example.data.model.UserRole
import com.example.data.network.LabSyncClient
import com.example.data.server.LabLocalServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.net.ServerSocket
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TextileLabServerAndDbTest {

    private lateinit var context: Context
    private lateinit var db: TextileLabDatabase
    private lateinit var backupManager: BackupManager
    private lateinit var server: LabLocalServer
    private lateinit var client: LabSyncClient
    private var port: Int = 0
    private lateinit var baseUrl: String

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, TextileLabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupManager = BackupManager(context, db)
        server = LabLocalServer(db, backupManager)
        client = LabSyncClient()

        db.userDao().insert(
            UserEntity(
                username = "admin",
                fullName = "Admin User",
                role = UserRole.ADMIN.name,
                passwordHash = SecurityUtils.hashPassword("admin123")
            )
        )
        db.userDao().insert(
            UserEntity(
                username = "tech1",
                fullName = "Tech One",
                role = UserRole.TECHNICIAN.name,
                passwordHash = SecurityUtils.hashPassword("tech123")
            )
        )
        db.userDao().insert(
            UserEntity(
                username = "guest",
                fullName = "Guest Buyer",
                role = UserRole.GUEST.name,
                passwordHash = SecurityUtils.hashPassword("guest123")
            )
        )

        port = ServerSocket(0).use { it.localPort }
        baseUrl = "http://127.0.0.1:$port"
        server.start(port)
    }

    @After
    fun teardown() {
        server.stop()
        db.close()
    }

    @Test
    fun `password hashing uses random salt and verifies accurately`() {
        val h1 = SecurityUtils.hashPassword("secret99")
        val h2 = SecurityUtils.hashPassword("secret99")
        assertTrue(h1 != h2)
        assertTrue(SecurityUtils.verifyPassword("secret99", h1))
        assertFalse(SecurityUtils.verifyPassword("wrong", h1))
    }

    @Test
    fun `full workflow and server RBAC reject technician file deletion while allowing admin`() = runBlocking {
        // 1. Separate logins
        val adminLogin = client.login(baseUrl, LoginRequest("admin", "admin123")).getOrThrow()
        val techLogin = client.login(baseUrl, LoginRequest("tech1", "tech123")).getOrThrow()
        val guestLogin = client.login(baseUrl, LoginRequest("guest", "guest123")).getOrThrow()

        assertEquals(UserRole.ADMIN, adminLogin.user.role)
        assertEquals(UserRole.TECHNICIAN, techLogin.user.role)
        assertEquals(UserRole.GUEST, guestLogin.user.role)

        // 2. Guest cannot create job
        val guestCreateJob = client.upsertJob(
            baseUrl,
            guestLogin.token,
            UpsertJobRequest(
                jobNumber = "JOB-999",
                reportNumber = "RPT-999",
                customerName = "Test Mill",
                buyerName = "Buyer",
                quantity = "100m",
                receivedDate = "2026-11-17",
                processes = listOf("GSM"),
                urgency = UrgencyLevel.NORMAL,
                remarks = ""
            )
        )
        assertTrue(guestCreateJob.isFailure)

        // 3. Technician creates job & sample (all 9 testing modules)
        val job = client.upsertJob(
            baseUrl,
            techLogin.token,
            UpsertJobRequest(
                jobNumber = "JOB-2026-101",
                reportNumber = "RPT-2026-101",
                customerName = "Apex Textiles",
                buyerName = "Nordic Wear",
                quantity = "1200 yds",
                receivedDate = "2026-11-17",
                processes = TextileTestType.entries.map { it.displayName },
                urgency = UrgencyLevel.URGENT,
                remarks = "Priority export lot",
                status = JobCardStatus.RECEIVED
            )
        ).getOrThrow()

        val sample = client.createSample(
            baseUrl,
            techLogin.token,
            CreateSampleRequest(
                jobId = job.id,
                fabricDescription = "Cotton Twill 280GSM",
                colorShade = "Navy",
                assignedTechnician = "Tech One",
                selectedTests = TextileTestType.entries
            )
        ).getOrThrow()
        assertTrue(sample.sampleCode.startsWith("SMP-"))

        val tests = client.getTests(baseUrl, techLogin.token).getOrThrow()
        assertEquals(9, tests.size)

        // 4. Technician updates GSM test
        val gsmTest = tests.first { it.testType == TextileTestType.GSM }
        val updatedGsm = client.updateTest(
            baseUrl,
            techLogin.token,
            gsmTest.id,
            UpdateTestResultRequest(
                resultValue = "205",
                standardSpec = "160 - 220 g/m²",
                passFail = TestPassFail.PASS,
                technicianName = "Tech One",
                testedDate = "2026-11-17",
                status = TestStatus.COMPLETED,
                remarks = "Within spec"
            )
        ).getOrThrow()
        assertEquals("205", updatedGsm.resultValue)
        assertEquals(TestPassFail.PASS, updatedGsm.passFail)

        // 5. Technician publishes Final Report directly
        val published = client.publishReport(
            baseUrl,
            techLogin.token,
            PublishReportRequest(
                jobId = job.id,
                reportDate = "2026-11-17",
                overallResult = TestPassFail.PASS,
                comments = "Approved for shipment",
                publishNow = true
            )
        ).getOrThrow()
        assertTrue(published.isPublished)

        // 6. Guest immediately sees published report
        val guestReports = client.getReports(baseUrl, guestLogin.token, publishedOnly = true).getOrThrow()
        assertEquals(1, guestReports.size)
        assertEquals("RPT-2026-101", guestReports.first().reportNumber)
        assertEquals(TestPassFail.PASS, guestReports.first().overallResult)

        // 7. Technician uploads photo
        val dummyBytes = "fake_jpeg_data".toByteArray()
        val uploaded = client.uploadPhoto(
            baseUrl,
            techLogin.token,
            UploadPhotoRequest(
                reportId = published.id,
                jobId = job.id,
                fileName = "swatch.jpg",
                mimeType = "image/jpeg",
                sourceType = "CAMERA",
                base64Data = Base64.getEncoder().encodeToString(dummyBytes)
            )
        ).getOrThrow()
        assertTrue(File(uploaded.localFilePath).exists())

        // 8. CRITICAL SECURITY CHECK: Technician MUST NOT be able to delete file
        val techDeleteAttempt = client.deleteFile(baseUrl, techLogin.token, uploaded.id)
        assertTrue("Server must reject technician file delete", techDeleteAttempt.isFailure)
        assertTrue(
            techDeleteAttempt.exceptionOrNull()?.message?.contains("403") == true ||
                techDeleteAttempt.exceptionOrNull()?.message?.contains("Admin", ignoreCase = true) == true
        )

        // Verify DENIED audit log recorded
        val logs = client.getAuditLogs(baseUrl, adminLogin.token).getOrThrow()
        assertTrue(logs.any { it.action == "DELETE_FILE" && it.status == "DENIED" && it.actorUsername == "tech1" })

        // 9. Admin CAN delete the file
        val adminDelete = client.deleteFile(baseUrl, adminLogin.token, uploaded.id)
        assertTrue(adminDelete.isSuccess)
    }

    @Test
    fun `backup manager creates real zip archive record`() = runBlocking {
        val record = backupManager.createBackup("MANUAL", "admin").getOrThrow()
        val file = File(record.filePath)
        assertTrue(file.exists())
        assertTrue(record.sizeBytes > 0)
    }
}
