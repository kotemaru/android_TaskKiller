package org.kotemaru.android.taskkiller.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.kotemaru.android.taskkiller.R;
import org.kotemaru.android.taskkiller.service.InstanceKeepService;

import java.util.Locale;

public class WebViewActivity extends Activity {
    private static final String TAG = "WebViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        WebView webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("file://")) return true;
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                view.loadUrl(view.getUrl());
                return false;
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String uri = intent.getDataString();
        if (uri == null) uri = "file:///android_asset/about.html";
        String lang = Locale.getDefault().getLanguage();
        Log.d(TAG, "lang=" + lang + ":" + Locale.JAPANESE.getLanguage());
        if (Locale.JAPANESE.getLanguage().equals(lang)
                && uri.indexOf("/help.html") > 0) {
            uri = uri.replaceFirst(".html$", "-ja.html");
        }
        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setAppCacheEnabled(false);
        webView.loadUrl(uri);
    }
}
