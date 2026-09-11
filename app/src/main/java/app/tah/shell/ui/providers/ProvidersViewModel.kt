package app.tah.shell.ui.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tah.shell.data.AppContainer
import app.tah.shell.data.ProviderKind
import app.tah.shell.data.ProviderRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ProvidersViewModel(private val container: AppContainer) : ViewModel() {
    val snapshot = container.providers.snapshot
    val probing = MutableStateFlow(false)

    fun save(kind: ProviderKind, baseUrl: String, modelId: String, apiKey: String?) {
        val url = baseUrl.ifBlank { ProviderRepository.defaultUrl(kind) }
        val model = modelId.ifBlank { ProviderRepository.defaultModel(kind) }
        container.providers.save(kind, url, model, apiKey)
    }

    fun setModel(modelId: String) = container.providers.setModel(modelId)

    fun probe() {
        probing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val snap = container.providers.current
            val result = container.client.probe(snap, container.providers.apiKey())
            val models = result.models.ifEmpty { ProviderRepository.stockModels(snap.kind) }
            container.providers.setProbeResult(result.ok, result.detail, models)
            probing.value = false
        }
    }

    fun apiKeyHint(): String {
        val key = container.providers.apiKey()
        if (key.isBlank()) return ""
        return "••••" + key.takeLast(4)
    }
}
