package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.LabNodeMode
import com.example.data.model.UserRole
import com.example.ui.LabViewModel
import com.example.ui.components.LabTopAppBar
import com.example.ui.screens.AdminCenterScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.JobsScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SamplesScreen
import com.example.ui.screens.TestingScreen
import com.example.ui.theme.TextileLabTheme

private data class NavTabItem(
    val key: String,
    val label: String,
    val icon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LabViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = state.session.darkThemeOverride ?: systemDark
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(Unit) {
                viewModel.toastMessage.collect { msg ->
                    snackbarHostState.showSnackbar(msg)
                }
            }

            TextileLabTheme(darkTheme = isDark) {
                val activeRole = state.session.role
                if (activeRole == null || state.session.token.isBlank()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { padding ->
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                        ) {
                            LoginScreen(
                                initialMode = state.session.nodeMode,
                                initialRemoteUrl = state.session.remoteHostUrl,
                                isBusy = state.isBusy,
                                onLogin = { user, pass, mode, url, onError ->
                                    viewModel.login(user, pass, mode, url, onError)
                                },
                                onTestRemoteHost = { viewModel.testRemoteServer(it) }
                            )
                        }
                    }
                } else {
                    MainAuthenticatedShell(
                        activeRole = activeRole,
                        isDark = isDark,
                        snackbarHostState = snackbarHostState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun MainAuthenticatedShell(
    activeRole: UserRole,
    isDark: Boolean,
    snackbarHostState: SnackbarHostState,
    viewModel: LabViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = remember(activeRole) {
        when (activeRole) {
            UserRole.ADMIN -> listOf(
                NavTabItem("dashboard", "Home", Icons.Default.Dashboard),
                NavTabItem("jobs", "Jobs", Icons.Default.WorkOutline),
                NavTabItem("samples", "Samples", Icons.Default.Inventory2),
                NavTabItem("testing", "Tests", Icons.Default.Biotech),
                NavTabItem("reports", "Reports", Icons.Default.AssignmentTurnedIn),
                NavTabItem("admin", "Admin", Icons.Default.AdminPanelSettings)
            )
            UserRole.TECHNICIAN -> listOf(
                NavTabItem("dashboard", "Home", Icons.Default.Dashboard),
                NavTabItem("jobs", "Jobs", Icons.Default.WorkOutline),
                NavTabItem("samples", "Samples", Icons.Default.Inventory2),
                NavTabItem("testing", "Tests", Icons.Default.Biotech),
                NavTabItem("reports", "Reports", Icons.Default.AssignmentTurnedIn)
            )
            UserRole.GUEST -> listOf(
                NavTabItem("dashboard", "Overview", Icons.Default.Dashboard),
                NavTabItem("reports", "Published", Icons.Default.AssignmentTurnedIn)
            )
        }
    }

    var currentTab by remember(activeRole) {
        mutableStateOf(if (activeRole == UserRole.GUEST) "reports" else "dashboard")
    }
    if (tabs.none { it.key == currentTab }) {
        currentTab = tabs.first().key
    }

    val isOnline = if (state.session.nodeMode == LabNodeMode.HOST_SERVER) {
        state.isServerRunning
    } else {
        state.wsClientConnected
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 700.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                LabTopAppBar(
                    title = "Textile Lab Pro",
                    session = state.session,
                    isServerOnline = isOnline,
                    isDarkTheme = isDark,
                    onToggleTheme = { viewModel.setDarkTheme(!isDark) },
                    onRefresh = { viewModel.refreshNow() },
                    onLogout = { viewModel.logout() }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab.key,
                                onClick = { currentTab = tab.key },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_tab_${tab.key}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        tabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab.key,
                                onClick = { currentTab = tab.key },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_rail_${tab.key}")
                            )
                        }
                    }
                }

                Crossfade(targetState = currentTab, label = "tab_crossfade") { tabKey ->
                    when (tabKey) {
                        "dashboard" -> DashboardScreen(
                            state = state,
                            onNavigateTab = { currentTab = it }
                        )
                        "jobs" -> JobsScreen(
                            state = state,
                            onSaveJob = { req, cb -> viewModel.saveJob(req, cb) }
                        )
                        "samples" -> SamplesScreen(
                            state = state,
                            onCreateSample = { req, cb -> viewModel.createSample(req, cb) },
                            onUpdateSampleStatus = { id, status, tech -> viewModel.updateSampleStatus(id, status, tech) }
                        )
                        "testing" -> TestingScreen(
                            state = state,
                            onUpdateTest = { id, req, cb -> viewModel.updateTestResult(id, req, cb) }
                        )
                        "reports" -> ReportsScreen(
                            state = state,
                            onPublishReport = { req, cb -> viewModel.publishReport(req, cb) },
                            onUploadPhoto = { jobId, reportId, src, uri ->
                                viewModel.uploadPhoto(jobId, reportId, src, uri)
                            },
                            onDeleteAttachment = { viewModel.deleteAttachment(it) }
                        )
                        "admin" -> AdminCenterScreen(
                            state = state,
                            onToggleServer = { viewModel.toggleLocalServer(it) },
                            onCreateUser = { req, cb -> viewModel.createUser(req, cb) },
                            onToggleUser = { id, active -> viewModel.toggleUser(id, active) },
                            onRunBackup = { viewModel.runManualBackup() },
                            onDeleteBackup = { viewModel.deleteBackup(it) }
                        )
                    }
                }
            }
        }
    }
}
