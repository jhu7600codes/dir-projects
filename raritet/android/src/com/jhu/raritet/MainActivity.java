package com.jhu.raritet;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

// the whole game is web/index.html, copied into assets; this is just a webview around it
public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setBackgroundColor(0xff0f1215);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(this), "RaritetApp");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("document.getElementById('menu').classList.contains('on')", (v) -> {
            if ("true".equals(v)) finish();
            else web.evaluateJavascript("menu()", null);
        });
    }
}
