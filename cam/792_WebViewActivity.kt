package com.example.houmtest

import android.os.Bundle
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity


class WebViewActivity : AppCompatActivity() {

    val TAG = "TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        val webUrl = intent.getStringExtra(WEB_URL) ?: "https://nishantthite.me/";
        WebView.setWebContentsDebuggingEnabled(true);

        val webView: WebView = findViewById(R.id.webview)
        webView.webViewClient = WebViewClient()
        webView.settings.domStorageEnabled = true

        webView.clearCache(true);
        webView.clearFormData();
        webView.clearHistory();
        webView.clearSslPreferences();
        webView.settings.javaScriptEnabled = true


        webView.webChromeClient = object : WebChromeClient() {
            // Grant permissions for cam
            override fun onPermissionRequest(request: PermissionRequest) {
                Log.d(TAG, "onPermissionRequest $request")
                this@WebViewActivity.runOnUiThread(Runnable {
                    Log.d(TAG, request.origin.toString())
                    if (request.origin.toString() == "file:///") {
                        Log.d(TAG, "GRANTED")
                        request.grant(request.resources)
                    } else {
                        Log.d(TAG, "DENIED")
                        request.deny()
                    }
                })
            }
        }
        webView.loadUrl(webUrl)
    }
}