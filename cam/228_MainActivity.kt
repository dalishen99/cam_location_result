package com.ciunkos.remedy

import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView: WebView = findViewById(R.id.webview)


        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.loadsImagesAutomatically = true;
        webView.settings.domStorageEnabled = true;
        webView.settings.mediaPlaybackRequiresUserGesture = false;
        webView.settings.setAppCacheEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.settings.setSafeBrowsingEnabled(false);
            webView.settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Extras tried for Android 9.0, can be removed if want.
        webView.settings.setAllowContentAccess(true);
        webView.settings.setAllowFileAccess(true);
        webView.settings.setBlockNetworkImage(false);

        webView.loadUrl("https://przemyslawzalewski.pl")
    }
}