package app.tah.shell.ui.settings

import androidx.lifecycle.ViewModel
import app.tah.shell.data.AppContainer
import app.tah.shell.data.InputMode
import app.tah.shell.data.PermissionMode

class SettingsViewModel(container: AppContainer) : ViewModel() {
    private val repo = container.settings
    val settings = repo.settings

    fun setPermissionMode(mode: PermissionMode) = repo.setPermissionMode(mode)
    fun setInputMode(mode: InputMode) = repo.setInputMode(mode)
    fun setNotifications(enabled: Boolean) = repo.setNotificationsEnabled(enabled)
}
