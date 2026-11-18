package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.LabNodeMode
import com.example.data.model.UserDto
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "textile_lab_prefs")

data class StoredSession(
    val token: String = "",
    val userId: Long = 0,
    val username: String = "",
    val fullName: String = "",
    val role: UserRole? = null,
    val nodeMode: LabNodeMode = LabNodeMode.HOST_SERVER,
    val remoteHostUrl: String = "http://192.168.1.100:8080",
    val darkThemeOverride: Boolean? = null
)

class SessionPreferences(private val context: Context) {
    private object Keys {
        val TOKEN = stringPreferencesKey("session_token")
        val USER_ID = stringPreferencesKey("session_user_id")
        val USERNAME = stringPreferencesKey("session_username")
        val FULL_NAME = stringPreferencesKey("session_full_name")
        val ROLE = stringPreferencesKey("session_role")
        val NODE_MODE = stringPreferencesKey("node_mode")
        val REMOTE_HOST = stringPreferencesKey("remote_host_url")
        val DARK_THEME = stringPreferencesKey("dark_theme_mode")
        val AUTO_START_SERVER = booleanPreferencesKey("auto_start_server")
    }

    val sessionFlow: Flow<StoredSession> = context.dataStore.data.map { prefs ->
        val roleStr = prefs[Keys.ROLE]
        val darkStr = prefs[Keys.DARK_THEME]
        StoredSession(
            token = prefs[Keys.TOKEN].orEmpty(),
            userId = prefs[Keys.USER_ID]?.toLongOrNull() ?: 0L,
            username = prefs[Keys.USERNAME].orEmpty(),
            fullName = prefs[Keys.FULL_NAME].orEmpty(),
            role = roleStr?.let { runCatching { UserRole.valueOf(it) }.getOrNull() },
            nodeMode = prefs[Keys.NODE_MODE]?.let {
                runCatching { LabNodeMode.valueOf(it) }.getOrNull()
            } ?: LabNodeMode.HOST_SERVER,
            remoteHostUrl = prefs[Keys.REMOTE_HOST] ?: "http://192.168.1.100:8080",
            darkThemeOverride = when (darkStr) {
                "DARK" -> true
                "LIGHT" -> false
                else -> null
            }
        )
    }

    val autoStartServerFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_START_SERVER] ?: true
    }

    suspend fun saveSession(token: String, user: UserDto) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TOKEN] = token
            prefs[Keys.USER_ID] = user.id.toString()
            prefs[Keys.USERNAME] = user.username
            prefs[Keys.FULL_NAME] = user.fullName
            prefs[Keys.ROLE] = user.role.name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USERNAME)
            prefs.remove(Keys.FULL_NAME)
            prefs.remove(Keys.ROLE)
        }
    }

    suspend fun setNodeConfig(mode: LabNodeMode, remoteHostUrl: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NODE_MODE] = mode.name
            prefs[Keys.REMOTE_HOST] = remoteHostUrl.trim().trimEnd('/')
        }
    }

    suspend fun setDarkThemeOverride(isDark: Boolean?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = when (isDark) {
                true -> "DARK"
                false -> "LIGHT"
                null -> "SYSTEM"
            }
        }
    }

    suspend fun setAutoStartServer(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_START_SERVER] = enabled
        }
    }
}
