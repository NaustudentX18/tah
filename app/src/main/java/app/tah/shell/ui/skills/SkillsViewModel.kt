package app.tah.shell.ui.skills

import androidx.lifecycle.ViewModel
import app.tah.shell.data.AppContainer
import app.tah.shell.data.MemoryNote
import app.tah.shell.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SkillsViewModel(private val container: AppContainer) : ViewModel() {
    val packs = container.skills.packs
    val notes = container.memory.notes

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun setEnabled(id: String, enabled: Boolean) {
        container.skills.setEnabled(id, enabled)
    }

    fun importMarkdown(raw: String) {
        val err = container.skills.importMarkdown(raw)
        _message.value = err ?: "Skill pack loaded. Dispatch can apply it."
    }

    fun removePack(id: String) {
        _message.value = container.skills.remove(id) ?: "Imported pack removed."
    }

    fun addNote(title: String, body: String) {
        if (body.isBlank()) {
            _message.value = "Memory note needs a body."
            return
        }
        container.memory.upsert(
            MemoryNote(
                id = SessionRepository.newId(),
                title = title.ifBlank { "Note" },
                body = body.trim(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
        _message.value = "Note saved — agent loop can read it on the next run."
    }

    fun updateNote(id: String, title: String, body: String) {
        if (body.isBlank()) {
            _message.value = "Memory note needs a body."
            return
        }
        container.memory.upsert(
            MemoryNote(
                id = id,
                title = title.ifBlank { "Note" },
                body = body.trim(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
        _message.value = "Note updated."
    }

    fun deleteNote(id: String) {
        container.memory.delete(id)
        _message.value = "Note deleted."
    }
}
