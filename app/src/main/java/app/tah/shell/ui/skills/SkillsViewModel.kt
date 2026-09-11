package app.tah.shell.ui.skills

import androidx.lifecycle.ViewModel
import app.tah.shell.data.AppContainer
import app.tah.shell.data.MemoryNote
import app.tah.shell.data.SessionRepository
import app.tah.shell.data.SkillCatalog

class SkillsViewModel(private val container: AppContainer) : ViewModel() {
    val packs = SkillCatalog.packs
    val notes = container.memory.notes

    fun addNote(title: String, body: String) {
        container.memory.upsert(
            MemoryNote(
                id = SessionRepository.newId(),
                title = title.ifBlank { "Note" },
                body = body,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}
