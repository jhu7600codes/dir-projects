package app.krala.ui.settings

import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.krala.data.backup.BackupService
import app.krala.data.chat.ChatRepository
import app.krala.data.memory.EmbeddingIndex
import app.krala.data.provider.Provider
import app.krala.data.provider.ProviderRepository
import app.krala.data.settings.AppSettings
import app.krala.data.settings.SettingsRepository
import app.krala.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsMessage {
    data class BackupDone(val s: BackupService.Summary) : SettingsMessage
    data class RestoreDone(val s: BackupService.Summary) : SettingsMessage
    data class Failed(val error: String) : SettingsMessage
    data object ChatsDeleted : SettingsMessage
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val backup: BackupService,
    private val chats: ChatRepository,
    private val embeddings: EmbeddingIndex,
    providers: ProviderRepository,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val providers: StateFlow<List<Provider>> = providers.providers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _messages = MutableSharedFlow<SettingsMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<SettingsMessage> = _messages

    fun setTheme(m: ThemeMode) = viewModelScope.launch { repo.setThemeMode(m) }
    fun setDynamic(v: Boolean) = viewModelScope.launch { repo.setDynamicColor(v) }
    fun setHaptics(v: Boolean) = viewModelScope.launch { repo.setHaptics(v) }
    fun setSendOnEnter(v: Boolean) = viewModelScope.launch { repo.setSendOnEnter(v) }
    fun setAutoRecall(v: Boolean) = viewModelScope.launch { repo.setAutoRecall(v) }
    fun setBiometric(v: Boolean) = viewModelScope.launch { repo.setBiometricLock(v) }
    fun setMemoryModel(p: String?, m: String?) = viewModelScope.launch { repo.setMemoryModel(p, m) }

    fun setLanguage(tag: String) {
        viewModelScope.launch { repo.setLanguage(tag) }
        AppCompatDelegate.setApplicationLocales(if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }

    fun setEmbeddings(enabled: Boolean, providerId: String?, model: String?) = viewModelScope.launch {
        val old = settings.value
        if (old.embeddingModel != model || old.embeddingProviderId != providerId) embeddings.clear()
        repo.setEmbeddings(enabled, providerId, model)
    }

    fun savePersonality(prompt: String, instructions: String) = viewModelScope.launch {
        repo.setSystemPrompt(if (prompt.trim() == app.krala.data.settings.Personality.DEFAULT_SYSTEM_PROMPT) null else prompt)
        repo.setCustomInstructions(instructions.trim())
    }

    fun backupTo(uri: Uri) = viewModelScope.launch {
        runCatching { backup.export(uri) }
            .onSuccess { _messages.emit(SettingsMessage.BackupDone(it)) }
            .onFailure { _messages.emit(SettingsMessage.Failed(it.message ?: "backup failed")) }
    }

    fun restoreFrom(uri: Uri) = viewModelScope.launch {
        runCatching { backup.restore(uri) }
            .onSuccess { _messages.emit(SettingsMessage.RestoreDone(it)) }
            .onFailure { _messages.emit(SettingsMessage.Failed(it.message ?: "restore failed")) }
    }

    fun deleteAllChats() = viewModelScope.launch {
        chats.deleteAll()
        _messages.emit(SettingsMessage.ChatsDeleted)
    }
}
