package app.krala.ui.providers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.krala.data.provider.OpenAiClient
import app.krala.data.provider.Preset
import app.krala.data.provider.Presets
import app.krala.data.provider.Provider
import app.krala.data.provider.ProviderDraft
import app.krala.data.provider.ProviderException
import app.krala.data.provider.ProviderRepository
import app.krala.data.provider.TestResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProvidersViewModel @Inject constructor(private val repo: ProviderRepository) : ViewModel() {
    val providers: StateFlow<List<Provider>> = repo.providers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun reorder(ids: List<String>) = viewModelScope.launch { repo.reorder(ids) }
    fun setEnabled(id: String, on: Boolean) = viewModelScope.launch { repo.setEnabled(id, on) }
}

data class HeaderRow(val key: String = "", val value: String = "")

data class EditState(
    val id: String? = null,
    val presetId: String? = null,
    val name: String = "",
    val baseUrl: String = "",
    val accountId: String = "",
    val apiKey: String = "",
    val models: List<String> = emptyList(),
    val defaultModel: String? = null,
    val contextSize: String = "32000",
    val vision: Boolean = false,
    val enabled: Boolean = true,
    val headers: List<HeaderRow> = emptyList(),
    val fetching: Boolean = false,
    val fetchError: String? = null,
    val testing: Boolean = false,
    val test: TestResult? = null,
    val loaded: Boolean = false,
) {
    val needsAccountId get() = baseUrl.contains(Presets.ACCOUNT_PLACEHOLDER)
    fun resolvedUrl() = if (needsAccountId) baseUrl.replace(Presets.ACCOUNT_PLACEHOLDER, accountId.trim()) else baseUrl
    val valid get() = name.isNotBlank() && resolvedUrl().startsWith("http") && (!needsAccountId || accountId.isNotBlank())
}

@HiltViewModel
class ProviderEditViewModel @Inject constructor(
    private val repo: ProviderRepository,
    private val client: OpenAiClient,
    handle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(EditState())
    val state: StateFlow<EditState> = _state

    val preset: Preset? get() = Presets.byId(_state.value.presetId)

    init {
        val id: String? = handle["id"]
        val presetId: String? = handle["preset"]
        viewModelScope.launch {
            val p = id?.let { repo.get(it) }
            if (p != null) {
                val sec = repo.secrets(p.entity)
                _state.value = EditState(
                    id = p.id, presetId = p.entity.presetId, name = p.name, baseUrl = p.entity.baseUrl,
                    apiKey = sec.apiKey, models = p.models, defaultModel = p.entity.defaultModel,
                    contextSize = p.entity.contextSize.toString(), vision = p.entity.supportsVision, enabled = p.entity.enabled,
                    headers = sec.headers.map { (k, v) -> HeaderRow(k, v) }, loaded = true,
                )
            } else {
                val pr = Presets.byId(presetId)
                _state.value = if (pr != null) EditState(
                    presetId = pr.id, name = pr.name, baseUrl = pr.baseUrl, apiKey = pr.fixedKey ?: "",
                    models = pr.models, defaultModel = pr.models.firstOrNull(), contextSize = pr.contextSize.toString(),
                    vision = pr.vision, headers = pr.headers.map { (k, v) -> HeaderRow(k, v) }, loaded = true,
                ) else EditState(loaded = true)
            }
        }
    }

    fun update(f: (EditState) -> EditState) = _state.update { f(it).copy(test = if (f(it).let { n -> n.baseUrl != it.baseUrl || n.apiKey != it.apiKey }) null else it.test) }

    private fun draft(s: EditState) = ProviderDraft(
        id = s.id, name = s.name, baseUrl = s.resolvedUrl(), apiKey = s.apiKey,
        headers = s.headers.filter { it.key.isNotBlank() }.associate { it.key.trim() to it.value.trim() },
        models = s.models, defaultModel = s.defaultModel, contextSize = s.contextSize.toIntOrNull() ?: 32_000,
        supportsVision = s.vision, enabled = s.enabled, presetId = s.presetId,
    )

    fun fetchModels() {
        val s = _state.value
        if (!s.valid) return
        _state.update { it.copy(fetching = true, fetchError = null) }
        viewModelScope.launch {
            try {
                val list = client.listModels(repo.configFromDraft(draft(s)))
                _state.update {
                    it.copy(
                        fetching = false,
                        models = list.ifEmpty { it.models },
                        defaultModel = it.defaultModel?.takeIf { d -> d in list } ?: it.defaultModel ?: list.firstOrNull(),
                        fetchError = if (list.isEmpty()) "no models returned" else null,
                    )
                }
            } catch (e: ProviderException) {
                _state.update { it.copy(fetching = false, fetchError = client.describe(e)) }
            }
        }
    }

    fun test() {
        val s = _state.value
        if (!s.valid) return
        _state.update { it.copy(testing = true, test = null) }
        viewModelScope.launch {
            val r = client.test(repo.configFromDraft(draft(s)))
            _state.update { it.copy(testing = false, test = r) }
        }
    }

    suspend fun save(): Boolean {
        val s = _state.value
        if (!s.valid) return false
        repo.save(draft(s))
        return true
    }

    suspend fun delete() {
        _state.value.id?.let { repo.delete(it) }
    }
}
