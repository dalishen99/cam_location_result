package com.example.yn_ui

import android.os.Bundle
import android.util.Log
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity


class LoadActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load)

        val address= intent.getStringExtra("ddd")
        Log.d("TAG", "onCreate:000000000000000000000000000000000000$address ")
        val url =//"http://192.168.0.37:8000/recycle2/mapsample.html"
            "https://m.map.kakao.com/actions/routeView?endLoc=" //중화산로 55-40"
        //"https://map.kakao.com/link/to/카카오판교오피스,37.402056,127.108212"
        val webview = findViewById<WebView>(R.id.webView)
        //intent.getIntExtra("ddd",0)

        webview.settings.javaScriptEnabled = true
        webview.settings.setSupportMultipleWindows(false)
        webview.settings.javaScriptCanOpenWindowsAutomatically=false

        webview.webViewClient = WebViewClient()
        webview.setWebChromeClient(object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)
            }
        })
        webview.loadUrl(url+address)
    }
}