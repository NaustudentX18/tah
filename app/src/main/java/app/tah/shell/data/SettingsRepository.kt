package app.tah.shell.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()
    val current: UserSettings get() = _settings.value

    fun setPermissionMode(mode: PermissionMode) {
        prefs.edit().putString(KEY_PERM, mode.name).apply()
        _settings.update { it.copy(permissionMode = mode) }
    }

    fun setInputMode(mode: InputMode) {
        prefs.edit().putString(KEY_INPUT, mode.name).apply()
        _settings.update { it.copy(inputMode = mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF, enabled).apply()
        _settings.update { it.copy(notificationsEnabled = enabled) }
    }

    fun setOnboardingComplete(complete: Boolean = true) {
        prefs.edit().putBoolean(KEY_ONBOARD, complete).apply()
        _settings.update { it.copy(onboardingComplete = complete) }
    }

    private fun load(): UserSettings {
        val perm = runCatching {
            PermissionMode.valueOf(prefs.getString(KEY_PERM, PermissionMode.Ask.name)!!)
        }.getOrDefault(PermissionMode.Ask)
        val input = runCatching {
            InputMode.valueOf(prefs.getString(KEY_INPUT, InputMode.TouchSteer.name)!!)
        }.getOrDefault(InputMode.TouchSteer)
        return UserSettings(
            permissionMode = perm,
            inputMode = input,
            notificationsEnabled = prefs.getBoolean(KEY_NOTIF, true),
            onboardingComplete = prefs.getBoolean(KEY_ONBOARD, false),
        )
    }

    companion object {
        private const val PREFS = "tah_settings"
        private const val KEY_PERM = "permission_mode"
        private const val KEY_INPUT = "input_mode"
        private const val KEY_NOTIF = "notifications"
        private const val KEY_ONBOARD = "onboarding_complete"
    }
}
