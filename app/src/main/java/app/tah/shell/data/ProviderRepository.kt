package app.tah.shell.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProviderRepository(context: Context) {
    private val app = context.applicationContext
    private val publicPrefs = app.getSharedPreferences(PREFS_PUBLIC, Context.MODE_PRIVATE)
    private val secretPrefs: SharedPreferences = openSecretPrefs()

    private val _snapshot = MutableStateFlow(load())
    val snapshot: StateFlow<ProviderSnapshot> = _snapshot.asStateFlow()
    val current: ProviderSnapshot get() = _snapshot.value

    fun save(
        kind: ProviderKind,
        baseUrl: String,
        modelId: String,
        apiKey: String? = null,
    ) {
        publicPrefs.edit()
            .putString(KEY_KIND, kind.name)
            .putString(KEY_URL, baseUrl.trim())
            .putString(KEY_MODEL, modelId.trim())
            .apply()
        if (apiKey != null) {
            secretPrefs.edit().putString(KEY_API, apiKey.trim()).apply()
        }
        _snapshot.update {
            it.copy(
                kind = kind,
                baseUrl = baseUrl.trim(),
                modelId = modelId.trim().ifBlank { it.modelId },
                hasKey = secretPrefs.getString(KEY_API, "").orEmpty().isNotBlank(),
            )
        }
    }

    fun apiKey(): String = secretPrefs.getString(KEY_API, "").orEmpty()

    fun setModel(modelId: String) {
        publicPrefs.edit().putString(KEY_MODEL, modelId).apply()
        _snapshot.update { it.copy(modelId = modelId) }
    }

    fun setProbeResult(ok: Boolean, detail: String, models: List<String>) {
        publicPrefs.edit()
            .putString(KEY_MODELS, models.joinToString("\n"))
            .apply()
        _snapshot.update {
            it.copy(
                lastProbeOk = ok,
                lastProbeDetail = detail,
                models = models.ifEmpty { it.models },
            )
        }
    }

    fun capabilityCopy(offline: Boolean): String {
        val snap = current
        return when {
            snap.kind == ProviderKind.Demo || !snap.isLive ->
                "Offline demo stream · tokens + tool cards work locally · cloud/LAN needs a provider"
            snap.kind == ProviderKind.OllamaLan && offline ->
                "Ollama LAN configured · device is offline to WAN · LAN host must still be reachable"
            snap.kind == ProviderKind.OllamaLan ->
                "Ollama LAN · local models OK · no TAH proxy · host must be on this network"
            snap.kind == ProviderKind.Byok && offline ->
                "BYOK configured · device offline · cloud endpoint will fail until you reconnect"
            else ->
                "BYOK OpenAI-compatible · keys stay on device · needs network"
        }
    }

    private fun load(): ProviderSnapshot {
        val kind = runCatching {
            ProviderKind.valueOf(publicPrefs.getString(KEY_KIND, ProviderKind.Demo.name)!!)
        }.getOrDefault(ProviderKind.Demo)
        val models = publicPrefs.getString(KEY_MODELS, "")
            .orEmpty()
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return ProviderSnapshot(
            kind = kind,
            baseUrl = publicPrefs.getString(KEY_URL, "").orEmpty(),
            modelId = publicPrefs.getString(KEY_MODEL, defaultModel(kind)).orEmpty()
                .ifBlank { defaultModel(kind) },
            hasKey = secretPrefs.getString(KEY_API, "").orEmpty().isNotBlank(),
            models = models,
        )
    }

    private fun openSecretPrefs(): SharedPreferences {
        return try {
            val master = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_SECRET,
                master,
                app,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (t: Throwable) {
            app.getSharedPreferences(PREFS_SECRET_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val PREFS_PUBLIC = "tah_providers"
        private const val PREFS_SECRET = "tah_provider_secrets"
        private const val PREFS_SECRET_FALLBACK = "tah_provider_secrets_fallback"
        private const val KEY_KIND = "kind"
        private const val KEY_URL = "base_url"
        private const val KEY_MODEL = "model"
        private const val KEY_MODELS = "models"
        private const val KEY_API = "api_key"

        fun defaultUrl(kind: ProviderKind): String = when (kind) {
            ProviderKind.Demo -> ""
            ProviderKind.Byok -> "https://api.openai.com/v1"
            ProviderKind.OllamaLan -> "http://192.168.1.10:11434/v1"
        }

        fun defaultModel(kind: ProviderKind): String = when (kind) {
            ProviderKind.Demo -> "demo-offline"
            ProviderKind.Byok -> "gpt-4o-mini"
            ProviderKind.OllamaLan -> "llama3.2"
        }

        fun stockModels(kind: ProviderKind): List<String> = when (kind) {
            ProviderKind.Demo -> listOf("demo-offline")
            ProviderKind.Byok -> listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1")
            ProviderKind.OllamaLan -> listOf("llama3.2", "qwen2.5", "mistral")
        }
    }
}
