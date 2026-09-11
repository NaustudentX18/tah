package app.tah.shell.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import app.tah.shell.data.AppContainer
import app.tah.shell.ui.board.BoardViewModel
import app.tah.shell.ui.detail.DetailViewModel
import app.tah.shell.ui.dispatch.DispatchViewModel
import app.tah.shell.ui.providers.ProvidersViewModel
import app.tah.shell.ui.settings.SettingsViewModel
import app.tah.shell.ui.skills.SkillsViewModel

class TahVmFactory(
    private val container: AppContainer,
    private val sessionId: String? = null,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val vm: ViewModel = when {
            modelClass.isAssignableFrom(BoardViewModel::class.java) ->
                BoardViewModel(container)
            modelClass.isAssignableFrom(DetailViewModel::class.java) ->
                DetailViewModel(sessionId ?: "unknown", container)
            modelClass.isAssignableFrom(DispatchViewModel::class.java) ->
                DispatchViewModel(container)
            modelClass.isAssignableFrom(ProvidersViewModel::class.java) ->
                ProvidersViewModel(container)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(container)
            modelClass.isAssignableFrom(SkillsViewModel::class.java) ->
                SkillsViewModel(container)
            else -> error("Unknown ViewModel ${modelClass.name}")
        }
        return vm as T
    }
}
