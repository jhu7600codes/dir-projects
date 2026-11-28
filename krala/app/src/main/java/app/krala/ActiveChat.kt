package app.krala

import javax.inject.Inject
import javax.inject.Singleton

/** the chat currently on screen, so going to the background can end it for memory */
@Singleton
class ActiveChat @Inject constructor() {
    @Volatile
    var id: String? = null
}
