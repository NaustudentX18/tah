package app.tah.shell.ui.dispatch

import androidx.lifecycle.ViewModel
import app.tah.shell.data.AgentSession
import app.tah.shell.data.AppContainer
import app.tah.shell.data.ProviderKind
import app.tah.shell.data.SessionColumn
import app.tah.shell.data.SessionRepository

class DispatchViewModel(private val container: AppContainer) : ViewModel() {
    val provider = container.providers.snapshot
    val skills = container.skills.packs

    fun startRun(
        prompt: String,
        skillId: String,
        iterations: Int,
        wallMinutes: Int,
    ): String {
        val snap = container.providers.current
        val kind = if (snap.isLive) snap.kind else ProviderKind.Demo
        val now = System.currentTimeMillis()
        val id = SessionRepository.newId()
        val title = prompt.lineSequence().firstOrNull { it.isNotBlank() }?.take(72) ?: "Untitled run"
        val session = AgentSession(
            id = id,
            title = title,
            prompt = prompt.trim(),
            skillId = skillId,
            modelId = snap.modelId,
            providerKind = kind,
            column = SessionColumn.Working,
            iterationBudget = iterations.coerceIn(1, 32),
            wallClockMs = wallMinutes.coerceIn(1, 60) * 60_000L,
            startedAt = now,
            updatedAt = now,
        )
        container.sessions.upsertSession(session)
        container.loop.start(id)
        return id
    }
}
