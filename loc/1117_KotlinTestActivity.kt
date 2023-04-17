package com.example.myapplication

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.webkit.*
import kotlinx.android.synthetic.main.activity_kotlin_test.*

class KotlinTestActivity : AppCompatActivity() {
    private val mWebView: WebView by lazy { webview2 }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kotlin_test)

//        mWebView = findViewById(R.id.webview2)

        val webSettings = mWebView.settings
        with(webSettings) {
            javaScriptEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
            allowFileAccess = true// 设置允许访问文件数据
            setSupportZoom(true)//支持缩放
            cacheMode = WebSettings.LOAD_NO_CACHE
            domStorageEnabled = true
            databaseEnabled = true
        }
        mWebView.webChromeClient =webChromeClient

//        mWebView.webViewClient =
        mWebView.loadUrl("http://carlcare.shalltry.com:90/CarlcareManager/discover/findH5PostDetailsById?postId=4256")

//        val fruits = listOf("banana", "avocado", "apple", "kiwifruit")
////        fruits
////                .filter { it.startsWith("a") }
////                .sortedBy { it }
////                .map { it.toUpperCase() }
////                .forEach { println(it) }
//        fruits.map{
//            it
//        }
//KtTest.Singleton.aa = 5
    }

    internal var webChromeClient: WebChromeClient = object : WebChromeClient() {

        // 扩充数据库的容量（在WebChromeClinet中实现）
        override fun onExceededDatabaseQuota(url: String,
                                             databaseIdentifier: String, currentQuota: Long,
                                             estimatedSize: Long, totalUsedQuota: Long,
                                             quotaUpdater: WebStorage.QuotaUpdater) {

            quotaUpdater.updateQuota(estimatedSize * 2)
        }

        // 扩充缓存的容量
        override fun onReachedMaxAppCacheSize(spaceNeeded: Long,
                                              totalUsedQuota: Long, quotaUpdater: WebStorage.QuotaUpdater) {

            quotaUpdater.updateQuota(spaceNeeded * 2)
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
            callback.invoke(origin, true, false)//注意个函数，第二个参数就是是否同意定位权限，第三个是是否希望内核记住
            super.onGeolocationPermissionsShowPrompt(origin, callback)
        }


        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
            val transport = resultMsg.obj as WebView.WebViewTransport
            transport.webView = view
            resultMsg.sendToTarget()
            return true
        }

    }

    override fun onResume() {
        super.onResume()
        mWebView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mWebView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()

        mWebView.clearCache(true)
        mWebView?.stopLoading()
        mWebView?.setWebViewClient(null)
        mWebView?.setWebChromeClient(null)
        mWebView?.removeAllViews()
        mWebView?.destroy()
    }
}
