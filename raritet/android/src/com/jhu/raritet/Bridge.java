package com.jhu.raritet;

import android.app.Activity;
import android.content.Intent;
import android.webkit.JavascriptInterface;

// window.RaritetApp in the page: the webview has no navigator.share, so sharing goes through android
public class Bridge {
    private final Activity activity;

    public Bridge(Activity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void share(String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        activity.startActivity(Intent.createChooser(i, "Поделиться"));
    }
}
