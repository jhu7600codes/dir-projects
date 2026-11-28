package app.krala

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface Incoming {
    /** text / images shared from another app: start a new chat with them in the composer */
    data class Share(val text: String, val images: List<Uri>) : Incoming
    data class OpenChat(val id: String) : Incoming
    data object NewChat : Incoming
}

@Singleton
class IntentRouter @Inject constructor() {
    private val _incoming = MutableStateFlow<Incoming?>(null)
    val incoming: StateFlow<Incoming?> = _incoming.asStateFlow()

    fun post(i: Incoming) {
        _incoming.value = i
    }

    fun consume() {
        _incoming.value = null
    }
}
