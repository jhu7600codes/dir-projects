package app.krala.data.sync

import app.krala.di.AppScope
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthState {
    data object NotConfigured : AuthState
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class Guest(override val userId: String) : AuthState
    data class Account(override val userId: String, val email: String, val confirmed: Boolean) : AuthState

    val userId: String? get() = null
}

@Singleton
class AuthRepository @Inject constructor(
    private val holder: SupabaseHolder,
    private val http: io.ktor.client.HttpClient,
    @AppScope scope: CoroutineScope,
) {
    val configured get() = holder.configured && holder.client != null

    val state: StateFlow<AuthState> = run {
        val c = holder.client
        if (c == null) MutableStateFlow(AuthState.NotConfigured)
        else c.auth.sessionStatus.map { st ->
            when (st) {
                is SessionStatus.Authenticated -> {
                    val u = st.session.user
                    val email = u?.email
                    if (u == null) AuthState.SignedOut
                    else if (email.isNullOrBlank()) AuthState.Guest(u.id)
                    else AuthState.Account(u.id, email, u.emailConfirmedAt != null || u.confirmedAt != null)
                }
                is SessionStatus.Initializing -> AuthState.Loading
                else -> AuthState.SignedOut
            }
        }.stateIn(scope, SharingStarted.Eagerly, AuthState.Loading)
    }

    private fun client() = holder.client ?: error("cloud accounts are not set up in this build")

    /** makes sure there is a session; first launch becomes an anonymous guest */
    suspend fun ensureGuest() {
        val c = holder.client ?: return
        c.auth.awaitInitialization()
        if (c.auth.currentUserOrNull() == null) {
            runCatching { c.auth.signInAnonymously() }
                .onFailure { android.util.Log.w("krala", "anonymous sign in failed (enable it in supabase auth settings?)", it) }
        }
    }

    suspend fun signIn(email: String, password: String) {
        client().auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    /**
     * New account. A guest is upgraded in place (same user id), so every synced chat, memory and
     * setting stays with them.
     */
    enum class SignUpResult { CREATED, GUEST_UPGRADED, GUEST_NEEDS_CONFIRM }

    suspend fun signUp(email: String, password: String): SignUpResult {
        val c = client()
        val current = state.value
        if (current !is AuthState.Guest) {
            c.auth.signUpWith(Email, redirectUrl = REDIRECT) {
                this.email = email.trim()
                this.password = password
            }
            return SignUpResult.CREATED
        }
        return try {
            c.auth.updateUser(redirectUrl = REDIRECT) {
                this.email = email.trim()
                this.password = password
            }
            SignUpResult.GUEST_UPGRADED
        } catch (e: Exception) {
            // some projects only allow a password once the email is verified: link the email now,
            // the password gets set from "change password" after the link is tapped
            c.auth.updateUser(redirectUrl = REDIRECT) { this.email = email.trim() }
            SignUpResult.GUEST_NEEDS_CONFIRM
        }
    }

    companion object {
        const val REDIRECT = "krala://auth-callback"
    }

    suspend fun changePassword(newPassword: String) {
        client().auth.updateUser { password = newPassword }
    }

    suspend fun signOut() {
        val c = client()
        runCatching { c.auth.signOut(SignOutScope.LOCAL) }.onFailure { c.auth.clearSession() }
    }

    /** deletes the auth user server side (delete-account edge function); every table cascades */
    suspend fun deleteAccount() {
        val c = client()
        val token = c.auth.currentSessionOrNull()?.accessToken ?: error("not signed in")
        val resp = http.post(app.krala.BuildConfig.SUPABASE_URL.trimEnd('/') + "/functions/v1/delete-account") {
            header("Authorization", "Bearer $token")
            header("apikey", app.krala.BuildConfig.SUPABASE_ANON_KEY)
        }
        if (!resp.status.isSuccess()) error(resp.bodyAsText().take(200).ifBlank { "delete failed (${resp.status.value})" })
        runCatching { c.auth.signOut(SignOutScope.LOCAL) }
        runCatching { c.auth.clearSession() }
    }
}
