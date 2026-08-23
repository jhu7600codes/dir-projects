package com.jhulian.android.youtube.classic.auth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
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
        // doesn't affect whether the session is usable.
        binding.webView.evaluateJavascript(
            "(function(){try{var el=document.querySelector('#account-name, yt-formatted-string#account-name');" +
                "return el ? el.textContent : '';}catch(e){return '';}})();",
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
