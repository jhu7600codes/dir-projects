package app.stash.providers

import android.content.Context
import android.webkit.CookieManager
import java.io.File

/**
 * an optional youtube login, only for getting song audio (search and catalogs stay
 * anonymous). like pipepipe and yt-dlp's --cookies-from-browser: no api, no oauth, just the
 * cookie jar youtube.com's own web login leaves in a webview, handed to yt-dlp as a cookies.txt.
 * it never leaves the phone except to youtube.
 */
object YouTubeLogin {
    fun cookieFile(context: Context) = File(context.filesDir, "youtube_cookies.txt")

    fun isLoggedIn(context: Context) = cookieFile(context).exists()

    /** true once the webview's cookies hold a signed-in youtube session */
    fun hasSession(cookieHeader: String?) =
        cookieHeader != null && ("SAPISID=" in cookieHeader || "__Secure-3PAPISID=" in cookieHeader)

    /** saves the webview's youtube cookies in the netscape cookies.txt format yt-dlp reads */
    fun save(context: Context, cookieHeader: String) {
        val expires = System.currentTimeMillis() / 1000 + 365L * 24 * 60 * 60
        val lines = cookieHeader.split(";").map { it.trim() }.filter { "=" in it }.map { pair ->
            val name = pair.substringBefore("=")
            val value = pair.substringAfter("=")
            listOf(".youtube.com", "TRUE", "/", "TRUE", expires.toString(), name, value).joinToString("\t")
        }
        cookieFile(context).writeText("# Netscape HTTP Cookie File\n" + lines.joinToString("\n") + "\n")
    }

    fun logOut(context: Context) {
        cookieFile(context).delete()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }
}
