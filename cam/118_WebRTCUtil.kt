package com.hcl.videocall.util

import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.hcl.videocall.model.JavaScriptInterface
import java.text.SimpleDateFormat
import java.util.*

enum class CallType{ CREATE, JOIN }

object WebRTCUtil {

    var roomId = ""
    var callType: CallType? = null

    fun setupWebView(webView: WebView) {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(JavaScriptInterface(), "Android")
        loadVideoCall(webView)
    }

    private fun loadVideoCall(webView: WebView) {
        val filePath = "file:android_asset/call.html"
        webView.loadUrl(filePath)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                initializePeer(webView)
            }
        }
    }

    fun callJavaScriptFunction(webView: WebView, function: String) = webView.post { webView.loadUrl(function) }

    fun getUniqueId() = UUID.randomUUID().toString()

    fun initializePeer(webView: WebView) {
        if(roomId.isEmpty()) roomId = getUniqueId()
        when(callType){
            CallType.CREATE -> {   callJavaScriptFunction(webView, "javascript:init('$roomId')") }
            else -> { callJavaScriptFunction(webView, "javascript:init('${getUniqueId()}')") }
        }
        callJavaScriptFunction(webView, "javascript:startCall('$roomId')")
    }
}