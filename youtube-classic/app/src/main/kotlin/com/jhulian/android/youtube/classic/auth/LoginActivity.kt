package com.jhulian.android.youtube.classic.auth

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.jhulian.android.youtube.classic.R
import com.jhulian.android.youtube.classic.YtClassicApp
import com.jhulian.android.youtube.classic.databinding.ActivityLoginBinding

/**
 * Logs into youtube.com inside a plain embedded WebView and, once the
 * cookie jar shows a signed-in session, hands the raw `Cookie` header back
 * to [SessionManager]. No Google OAuth client, no API key, no consent
 * screen we control - it's the same page a desktop browser would show,
 * which is deliberate: this app never asks Google for API access at all.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    // REQUESTED_WITH_HEADER_ALLOW_LIST is public API but still flagged
    // @RestrictTo(LIBRARY_GROUP) in webkit 1.11.0 - this is the documented,
    // intended way to call it from outside AndroidX during that API's
    // rollout, gated by the isFeatureSupported() check right below.
    @SuppressLint("SetJavaScriptEnabled", "RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            userAgentString = DESKTOP_USER_AGENT
        }

        // WebView has historically sent an `X-Requested-With: <our package
        // name>` header on every request, which Google's sign-in page uses
        // as its primary signal to reject "this browser or app may not be
        // secure" - it's a dead giveaway that the page is loading inside an
        // embedded WebView rather than a real browser. Clearing the allow
        // list makes WebView stop sending that header to anyone, which is
        // the documented fix for this exact block; on WebView versions too
        // old to support the API at all, this is a no-op and the block can
        // still appear.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(binding.webView.settings, emptySet())
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)
                binding.loginProgress.visibility = View.GONE
                checkForSignedInSession(url)
            }
        }

        binding.webView.loadUrl(LOGIN_URL)

        binding.pasteCookieLink.setOnClickListener { showPasteCookieDialog() }
    }

    private fun showPasteCookieDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_paste_cookie, null)
        val input = dialogView.findViewById<EditText>(R.id.cookieInput)

        // Deliberately no package name here (not "org.mozilla.firefox" or
        // any other specific browser) - a plain, unrestricted ACTION_VIEW
        // lets Android route to whatever the user actually has (Firefox,
        // Fennec, Iceraven, their default browser...), showing a chooser
        // itself if more than one is installed. Guessing a package would
        // just mean "doesn't work if you use anything else."
        dialogView.findViewById<View>(R.id.openFirefoxButton).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<View>(R.id.pasteFromClipboardButton).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = clipboard.primaryClipText()?.trim()
            if (clipText.isNullOrBlank()) {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            } else {
                input.setText(clipText)
                input.setSelection(clipText.length)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.paste_cookie_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ -> trySaveCookie(input.text.toString()) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun ClipboardManager.primaryClipText(): String? =
        if (hasPrimaryClip()) primaryClip?.getItemAt(0)?.coerceToText(this@LoginActivity)?.toString() else null

    private fun trySaveCookie(rawInput: String) {
        val cookie = rawInput.trim()
        if (!cookie.contains("SAPISID=") && !cookie.contains("__Secure-3PAPISID=")) {
            Toast.makeText(this, R.string.paste_cookie_invalid, Toast.LENGTH_LONG).show()
            return
        }
        // Seed CookieManager with the pasted cookies and let the same
        // WebView flow (checkForSignedInSession, wired to onPageFinished
        // in onCreate) pick up the account name the same way a normal
        // WebView login does, instead of hardcoding a null name here -
        // that hardcoded null was why Settings always showed "Signed in
        // as you" for anyone who signed in via paste-cookie.
        cookie.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { pair ->
                val name = pair.substringBefore("=")
                // Real device evidence: every browse() auth log showed
                // __Secure-3PAPISID/__Secure-1PAPISID/__Secure-1PSID as
                // absent no matter what was pasted, even when Cookie-Editor
                // confirmed they were present in the source browser and
                // exported correctly. Root cause - a raw "Cookie:" request
                // header is always bare "name=value" pairs (attributes only
                // ever live on a Set-Cookie *response* header), but
                // Chromium's cookie store (which WebView's CookieManager is
                // built on) enforces the __Secure- prefix rule: such a
                // cookie is rejected outright unless the Secure attribute
                // is present on this exact setCookie() call. Every
                // __Secure-/__Host- pasted cookie was silently failing to
                // store, which meant this app was never actually holding a
                // complete modern session - just the legacy SID/HSID/SSID/
                // SAPISID cookies YouTube's current web client leans on
                // less than it used to.
                val attributed = if (name.startsWith("__Secure-") || name.startsWith("__Host-")) {
                    "$pair; Secure"
                } else {
                    pair
                }
                CookieManager.getInstance().setCookie("https://www.youtube.com", attributed)
            }
        CookieManager.getInstance().flush()
        binding.loginProgress.visibility = View.VISIBLE
        binding.webView.loadUrl("https://www.youtube.com")
    }

    private fun checkForSignedInSession(url: String?) {
        if (url == null || !url.startsWith("https://www.youtube.com")) return

        val cookies = CookieManager.getInstance().getCookie("https://www.youtube.com") ?: return
        // SAPISID/APISID are the cookies innertube's SAPISIDHASH auth scheme
        // signs requests with - their presence is the actual signal we're
        // logged in, not just "we're somewhere on youtube.com".
        val loggedIn = cookies.contains("SAPISID=") || cookies.contains("__Secure-3PAPISID=")
        if (!loggedIn) return

        // Best-effort account name for the settings screen; failure here
        // doesn't affect whether the session is usable. The `#account-name`
        // selector alone was reportedly never matching anything (Settings
        // always fell back to "you") - it's likely only present once the
        // avatar dropdown menu is actually open, not on the plain page.
        // Falling back to the avatar button's aria-label/alt text (which
        // YouTube has used consistently across redesigns as "Account menu
        // for <name>"/"Avatar <name>") is unverified against a live page
        // but strictly broadens what this can match rather than narrowing it.
        binding.webView.evaluateJavascript(
            "(function(){try{" +
                "var el=document.querySelector('#account-name, yt-formatted-string#account-name');" +
                "if(el&&el.textContent)return el.textContent;" +
                "var btn=document.querySelector('#avatar-btn, button#avatar-btn, ytd-topbar-menu-button-renderer #avatar-btn');" +
                "if(btn){" +
                "var label=btn.getAttribute('aria-label');" +
                "var img=btn.querySelector('img');" +
                "if(!label&&img)label=img.getAttribute('alt');" +
                "if(label)return label.replace(/^Account menu for\\s*/i,'').replace(/^Avatar\\s*/i,'').trim();" +
                "}" +
                "return '';" +
                "}catch(e){return '';}})();",
        ) { rawResult ->
            val name = rawResult?.trim('"')?.takeIf { it.isNotBlank() && it != "null" }
            (application as YtClassicApp).sessionManager.saveSession(cookies, name)
            setResult(RESULT_OK)
            finish()
        }
    }

    companion object {
        private const val LOGIN_URL =
            "https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com/signin"
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    }
}
