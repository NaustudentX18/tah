package app.tah.shell.data

import android.app.Application
import app.tah.shell.notify.NeedsYouNotifier
import app.tah.shell.runtime.AgentLoop
import app.tah.shell.runtime.OpenAiCompatClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(app: Application) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings = SettingsRepository(app)
    val providers = ProviderRepository(app)
    val sessions = SessionRepository(app)
    val memory = MemoryStore(app)
    val notifier = NeedsYouNotifier(app)
    val client = OpenAiCompatClient()
    val loop = AgentLoop(
        sessions = sessions,
        providers = providers,
        settings = settings,
        memory = memory,
        notifier = notifier,
        client = client,
        scope = scope,
    )

    fun notifySeededNeedsYou() {
        if (!settings.current.notificationsEnabled) return
        sessions.sessions()
            .filter { it.column == SessionColumn.NeedsYou }
            .forEach { session ->
                val pending = sessions.pendingFor(session.id) ?: return@forEach
                notifier.notifyNeedsYou(session, pending)
            }
    }
}
