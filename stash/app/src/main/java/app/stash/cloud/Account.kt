package app.stash.cloud

import app.stash.BuildConfig
import app.stash.data.prefs.Settings
import app.stash.providers.ProviderException
import app.stash.providers.httpIo
import app.stash.providers.lenientJson
import app.stash.providers.num
import app.stash.providers.obj
import app.stash.providers.str
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/** stash accounts: email + password through supabase auth's rest api */
@Singleton
class Account @Inject constructor(
    private val http: OkHttpClient,
    private val settings: Settings,
) {
    private val refreshLock = Mutex()

    sealed interface SignUpResult {
        data object SignedIn : SignUpResult
        /** supabase wants the email confirmed first */
        data object CheckEmail : SignUpResult
    }

    suspend fun signUp(email: String, password: String): SignUpResult {
        val o = post("/auth/v1/signup", buildJsonObject { put("email", email.trim()); put("password", password) }.toString())
        // with email confirmation on, signup returns the user but no session
        if (o.str("access_token") == null) return SignUpResult.CheckEmail
        save(o, email)
        return SignUpResult.SignedIn
    }

    suspend fun signIn(email: String, password: String) {
        val o = post("/auth/v1/token?grant_type=password", buildJsonObject { put("email", email.trim()); put("password", password) }.toString())
        save(o, email)
    }

    suspend fun signOut() = settings.clearSession()

    /** a valid access token, refreshed when it's close to expiring; null when logged out */
    suspend fun accessToken(): String? = refreshLock.withLock {
        val s = settings.session() ?: return null
        if (s.access.isNotEmpty() && System.currentTimeMillis() < s.expiresAt) return s.access
        val o = runCatching {
            post("/auth/v1/token?grant_type=refresh_token", buildJsonObject { put("refresh_token", s.refresh) }.toString())
        }.getOrElse { e ->
            // a refresh token that's been revoked means logging in again
            if (e is ProviderException && e.message?.contains("invalid", true) == true) settings.clearSession()
            throw e
        }
        save(o, s.email)
        o.str("access_token")
    }

    private suspend fun save(o: JsonObject, email: String) {
        val access = o.str("access_token") ?: throw ProviderException("login failed")
        val refresh = o.str("refresh_token") ?: throw ProviderException("login failed")
        val expiresIn = (o.num("expires_in") ?: 3600.0).toLong()
        val userEmail = o.obj("user")?.str("email") ?: email.trim()
        settings.saveSession(Settings.Session(userEmail, access, refresh, System.currentTimeMillis() + (expiresIn - 60) * 1000))
    }

    private suspend fun post(path: String, json: String): JsonObject = httpIo {
        val req = Request.Builder()
            .url(BuildConfig.SUPABASE_URL + path)
            .header("apikey", BuildConfig.SUPABASE_KEY)
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        http.newCall(req).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) throw ProviderException(authError(body, r.code))
            lenientJson.parseToJsonElement(body).jsonObject
        }
    }

    private fun authError(body: String, code: Int): String {
        val o = runCatching { lenientJson.parseToJsonElement(body).jsonObject }.getOrNull()
        val msg = o?.str("msg") ?: o?.str("error_description") ?: o?.str("message") ?: "error $code"
        return when {
            msg.contains("Invalid login credentials", true) -> "wrong email or password"
            msg.contains("Email not confirmed", true) -> "confirm your email first (check your inbox)"
            msg.contains("already registered", true) -> "that email already has an account, log in instead"
            else -> msg.lowercase()
        }
    }
}
