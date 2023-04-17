package com.example.testwebview

import android.annotation.SuppressLint
import android.app.Activity
import android.webkit.*
import androidx.fragment.app.Fragment
import android.widget.ProgressBar
import androidx.core.view.isVisible


class JavaScriptInterface(val onLoadHtml: (String) -> Unit ) {
    @JavascriptInterface
    fun processHTML(html: String) {
        onLoadHtml.invoke(html)
    }
}

const val DEFAULT_STRING: String = ""


fun WebView.makeItSmart(
    activity: Activity,
    progressBar: ProgressBar? = null,
    extraRedirect: ((String, String?) -> Unit)? = null,
    javaScriptInterface: JavaScriptInterface? = null
) {
    setUpWebView(this, progressBar, javaScriptInterface)
    webViewClient = SmartWebViewClient(activity, extraRedirect)
}

fun WebView.makeItSmart(
    fragment: Fragment,
    progressBar: ProgressBar? = null,
    extraRedirect: ((String, String?) -> Unit)? = null,
    javaScriptInterface: JavaScriptInterface? = null
) {
    setUpWebView(this, progressBar, javaScriptInterface)
    webViewClient = SmartWebViewClient(fragment, extraRedirect)
}

fun String?.toDefaultValue(defaultValue: String = DEFAULT_STRING): String {
    return this ?: defaultValue
}

@SuppressLint("SetJavaScriptEnabled")
private fun setUpWebView(
    webView: WebView,
    progressBar: ProgressBar? = null,
    javaScriptInterface: JavaScriptInterface? = null
) {
    webView.settings.apply {
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        javaScriptEnabled = true
        loadWithOverviewMode = true
        useWideViewPort = true
        cacheMode = WebSettings.LOAD_NO_CACHE
    }
    javaScriptInterface?.let { webView.addJavascriptInterface(it, "HTMLOUT") }
    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            progressBar?.progress = newProgress
            progressBar?.isVisible = newProgress != 100
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {
            callback?.invoke(origin, true, false)
        }
    }
}
