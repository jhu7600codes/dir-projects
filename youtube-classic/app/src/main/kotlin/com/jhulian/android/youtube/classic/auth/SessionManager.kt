package com.jhulian.android.youtube.classic.auth

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Holds the YouTube session captured by [LoginActivity]'s WebView login.
 * There is no OAuth token here at all - just the raw `Cookie:` header value
 * youtube.com's own web login left in the WebView's cookie jar, exactly the
 * approach PipePipe (and yt-dlp's `--cookies-from-browser`) use to act as a
 * logged-in browser without ever touching Google's official API/OAuth
 * surface.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // Falls back to a plain prefs file if Keystore is unavailable
        // (some emulators/custom ROMs); the cookie is still only ever
        // stored on-device, never sent anywhere but youtube.com.
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val isSignedIn: Boolean
        get() = !cookie.isNullOrBlank()

    var cookie: String?
        get() = prefs.getString(KEY_COOKIE, null)
        private set(value) { prefs.edit().putString(KEY_COOKIE, value).apply() }

    var accountName: String?
        get() = prefs.getString(KEY_ACCOUNT_NAME, null)
        private set(value) { prefs.edit().putString(KEY_ACCOUNT_NAME, value).apply() }

    /** Called once the WebView login flow reaches a logged-in youtube.com page. */
    fun saveSession(cookieHeader: String, displayName: String?) {
        cookie = cookieHeader
        accountName = displayName
    }

    fun signOut() {
        prefs.edit().remove(KEY_COOKIE).remove(KEY_ACCOUNT_NAME).apply()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    companion object {
        private const val PREFS_NAME = "ytclassic_session"
        private const val KEY_COOKIE = "cookie"
        private const val KEY_ACCOUNT_NAME = "account_name"
    }
}
