package com.example.core

import android.content.Context
import com.example.data.backup.BackupManager
import com.example.data.local.SessionPreferences
import com.example.data.local.TextileLabDatabase
import com.example.data.network.LabSyncClient
import com.example.data.repository.LabRepository
import com.example.data.server.LabLocalServer

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: TextileLabDatabase by lazy {
        TextileLabDatabase.getInstance(appContext)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(appContext, database)
    }

    val localServer: LabLocalServer by lazy {
        LabLocalServer(database, backupManager)
    }

    val syncClient: LabSyncClient by lazy {
        LabSyncClient()
    }

    val sessionPreferences: SessionPreferences by lazy {
        SessionPreferences(appContext)
    }

    val repository: LabRepository by lazy {
        LabRepository(
            context = appContext,
            database = database,
            backupManager = backupManager,
            localServer = localServer,
            syncClient = syncClient,
            sessionPrefs = sessionPreferences
        )
    }
}
