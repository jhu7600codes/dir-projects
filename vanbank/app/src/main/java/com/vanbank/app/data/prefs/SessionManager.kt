package com.vanbank.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "vanbank_session")

/** Holds just the signed-in user's id -- the whole "session" for this fake bank. */
class SessionManager(private val context: Context) {
    private val currentUserIdKey = longPreferencesKey("current_user_id")

    val currentUserId: Flow<Long?> = context.sessionDataStore.data.map { prefs ->
        prefs[currentUserIdKey]?.takeIf { it > 0 }
    }

    suspend fun signIn(userId: Long) {
        context.sessionDataStore.edit { it[currentUserIdKey] = userId }
    }

    suspend fun signOut() {
        context.sessionDataStore.edit { it.remove(currentUserIdKey) }
    }
}
