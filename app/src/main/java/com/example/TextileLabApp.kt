package com.example

import android.app.Application
import com.example.core.AppContainer
import com.example.data.backup.DailyBackupWorker
import com.example.data.local.StoredSession
import com.example.data.model.LabNodeMode
import com.example.data.server.LabServerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TextileLabApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        DailyBackupWorker.schedule(this)

        appScope.launch {
            container.repository.ensureDefaultUsersSeeded()
            container.repository.restoreActiveSessionTokenIfNeeded()
            val session: StoredSession = container.sessionPreferences.sessionFlow.first()
            val autoStart = container.sessionPreferences.autoStartServerFlow.first()
            if (session.nodeMode == LabNodeMode.HOST_SERVER && autoStart) {
                runCatching {
                    LabServerService.start(this@TextileLabApp)
                }.onFailure {
                    container.localServer.start()
                }
            }
        }
    }
}
