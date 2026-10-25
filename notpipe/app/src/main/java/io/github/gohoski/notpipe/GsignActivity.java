package io.github.gohoski.notpipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.github.gohoski.notpipe.api.Yt2009Account;

/**
 * Created by Claude on 25.10.2026.
 * yt2009 "gsign" on the device itself instead of a PC: Google's sign-in page runs in a WebView, the oauth2_4 token
 * it leaves in the "oauth_token" cookie is sent to the instance, then yt2009's channel picker is shown.
 * Yt2009Account.startSignIn() must have been called before opening this.
 */

public class GsignActivity extends Activity {
    private static final String GOOGLE_COOKIE_URL = "https://accounts.google.com";
    private static final int COOKIE_POLL_INTERVAL = 1500;

    private WebView web;
    private Context context;
    private Handler handler = new Handler();
    private boolean tokenFound = false;
    private boolean isDestroyedFlag = false;

    private final Runnable cookiePoll = new Runnable() {
        @Override
        public void run() {
            if (isDestroyedFlag || tokenFound) return;
            checkCookie();
            handler.postDelayed(this, COOKIE_POLL_INTERVAL);
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gsign);
        setTitle(R.string.sign_in_yt2009);
        context = this;

        if (Yt2009Account.getInstance().getDeviceCode() == null) {
            finish();
            return;
        }

        CookieSyncManager.createInstance(this);
        CookieManager.getInstance().removeAllCookie();
        CookieManager.getInstance().setAcceptCookie(true);

        web = (WebView) findViewById(R.id.gsign_web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        try {
            // Android 2.1+, Google's page needs it
            WebSettings.class.getMethod("setDomStorageEnabled", boolean.class).invoke(settings, true);
        } catch (Exception ignored) {}
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                checkCookie();
            }
        });
        web.loadUrl(Yt2009Account.GOOGLE_SIGN_IN_URL);
        handler.postDelayed(cookiePoll, COOKIE_POLL_INTERVAL);

        findViewById(R.id.gsign_done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new CheckTask().execute();
            }
        });
    }

    private void checkCookie() {
        if (tokenFound) return;
        String cookies = CookieManager.getInstance().getCookie(GOOGLE_COOKIE_URL);
        if (cookies == null) return;
        String[] parts = cookies.split(";");
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (part.startsWith("oauth_token=") && part.length() > 12) {
                tokenFound = true;
                askEmail(part.substring(12));
                return;
            }
        }
    }

    /** yt2009 needs the email alongside the token, and Google's page doesn't expose it to us */
    private void askEmail(final String token) {
        web.stopLoading();
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("name@gmail.com");
        new AlertDialog.Builder(this)
                .setTitle(R.string.gsign_email)
                .setMessage(R.string.gsign_email_)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new SubmitTask(input.getText().toString(), token).execute();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }

    private void setBusy(boolean busy, int status) {
        findViewById(R.id.gsign_progress).setVisibility(busy ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.gsign_status)).setText(status);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyedFlag = true;
        handler.removeCallbacks(cookiePoll);
        if (web != null) web.destroy();
    }

    private class SubmitTask extends AsyncTask<Void, Void, Exception> {
        private final String email, token;

        SubmitTask(String email, String token) {
            this.email = email;
            this.token = token;
        }

        @Override
        protected void onPreExecute() {
            setBusy(true, R.string.gsign_linking);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Yt2009Account.getInstance().submitGoogleToken(email, token);
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (isDestroyedFlag) return;
            if (e != null) {
                setBusy(false, R.string.gsign_step_google);
                Toast.makeText(context, getString(R.string.action_failed, e.getMessage()), Toast.LENGTH_LONG).show();
                tokenFound = false;
                CookieManager.getInstance().removeAllCookie();
                web.loadUrl(Yt2009Account.GOOGLE_SIGN_IN_URL);
                handler.postDelayed(cookiePoll, COOKIE_POLL_INTERVAL);
                return;
            }
            // Signed in; let the user pick the channel (brand accounts) on yt2009's picker page
            setBusy(false, R.string.gsign_step_channel);
            findViewById(R.id.gsign_done).setVisibility(View.VISIBLE);
            web.loadUrl(Yt2009Account.getInstance().getAccountPickerUrl());
        }
    }

    private class CheckTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                return Yt2009Account.getInstance().checkLinked();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean linked) {
            if (isDestroyedFlag) return;
            if (linked) {
                Toast.makeText(context, R.string.signed_in, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(context, R.string.not_signed_in_yet, Toast.LENGTH_LONG).show();
            }
        }
    }
}
