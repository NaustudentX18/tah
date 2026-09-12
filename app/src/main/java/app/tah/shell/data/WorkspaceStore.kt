package app.tah.shell.data

import android.content.Context
import app.tah.shell.runtime.WorkspaceNames
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class WorkspaceFile(
    val name: String,
    val bytes: Int,
    val updatedAt: Long,
)

class WorkspaceStore(context: Context) {
    private val root = File(context.applicationContext.filesDir, "workspace").apply { mkdirs() }
    private val _files = MutableStateFlow(listNow())
    val files: StateFlow<List<WorkspaceFile>> = _files.asStateFlow()

    fun listNames(): List<String> = _files.value.map { it.name }

    fun read(name: String): String {
        val file = fileFor(name)
        if (!file.exists()) return ""
        return file.readText().take(MAX_CHARS)
    }

    fun write(name: String, body: String): WorkspaceFile {
        val file = fileFor(name)
        file.parentFile?.mkdirs()
        file.writeText(body.take(MAX_CHARS))
        refresh()
        return WorkspaceFile(file.name, file.length().toInt(), file.lastModified())
    }

    fun delete(name: String) {
        fileFor(name).delete()
        refresh()
    }

    fun snapshotText(): String {
        val listed = listNames()
        if (listed.isEmpty()) return "(workspace empty)"
        return listed.joinToString("\n") { name ->
            val preview = read(name).take(160).replace("\n", " ")
            "- $name: $preview"
        }
    }

    private fun fileFor(name: String): File {
        val safe = WorkspaceNames.sanitize(name)
        return File(root, safe)
    }

    private fun refresh() {
        _files.value = listNow()
    }

    private fun listNow(): List<WorkspaceFile> {
        return root.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?.map { WorkspaceFile(it.name, it.length().toInt(), it.lastModified()) }
            .orEmpty()
    }

    companion object {
        const val MAX_CHARS = 24_000
    }
}
