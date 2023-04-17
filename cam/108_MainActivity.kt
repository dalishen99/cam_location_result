package com.snowcrab.smurf_webview

import android.annotation.TargetApi
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var btnExit: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        btnExit = findViewById(R.id.btn_exit)

//        webView.webViewClient = MyWebClient()
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
//            @TargetApi(Build.VERSION_CODES.P)
            override fun onPermissionRequest(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                    request.grant(request.resources)
                }
            }
        }

        webView.settings.loadWithOverviewMode =
            true  // WebView 화면크기에 맞추도록 설정 - setUseWideViewPort 와 같이 써야함
        webView.settings.useWideViewPort =
            true  // wide viewport 설정 - setLoadWithOverviewMode 와 같이 써야함

        webView.settings.setSupportZoom(false)  // 줌 설정 여부
        webView.settings.builtInZoomControls = false  // 줌 확대/축소 버튼 여부

        webView.settings.javaScriptEnabled = true // 자바스크립트 사용여부
//        webviesdJavascptInterface(new AndroidBridge(), "android");
        webView.settings.javaScriptCanOpenWindowsAutomatically = true // javascript가 window.open()을 사용할 수 있도록 설정
        webView.settings.setSupportMultipleWindows(true) // 멀티 윈도우 사용 여부

        webView.settings.mediaPlaybackRequiresUserGesture = false

        val url =  home.url
        Log.d("urlTest", home.url)
//        val url =  "ddcb-203-237-200-37.jp.ngrok.io"
        val webUrl =  "https://${url}/javascript_simple.html"

        webView.loadUrl(webUrl)

        // 새로고침
//        btnOnetomanyRefresh.setOnClickListener {
//            webView.reload()
//        }

        btnExit.setOnClickListener {
            destroyWebView()
        }
    }

    fun destroyWebView(){
        webView.clearCache(true)
        webView.destroy()

        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        destroyWebView()
    }

    override fun onPause() {
        super.onPause()
        destroyWebView()
    }
}