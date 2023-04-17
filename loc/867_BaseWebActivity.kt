package com.myrobot.org.web

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.webkit.*
import com.myrobot.org.R
import kotlinx.android.synthetic.main.activity_web.*

/**
 * @author Lixingxing
 */
abstract class BaseWebActivity : AppCompatActivity() {

    private lateinit var mWebView: WebView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        initView()
    }

    protected abstract fun getUrl():String

    private fun initView() {
        mWebView = web
        initWebCline()
        initWebSetting()
        mWebView.loadUrl(getUrl())
    }

    protected fun initWebCline(){
        mWebView.webViewClient = webClient
        mWebView.webChromeClient = webChromeClient
    }
    @SuppressLint("JavascriptInterface", "SetJavaScriptEnabled")
    protected fun initWebSetting() {
        val settings = mWebView.settings
        settings.javaScriptEnabled = true
        // 设置可以支持缩放
        settings.setSupportZoom(true)
        // 设置出现缩放工具
        settings.builtInZoomControls = true
        //扩大比例的缩放
        settings.useWideViewPort = true
        settings.displayZoomControls = false//隐藏

        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = true

        settings.domStorageEnabled = true

        settings.cacheMode = getCacheModes()
    }
    fun getCacheModes(): Int {
        return WebSettings.LOAD_NO_CACHE
    }


    protected var webClient: WebViewClient = CcWewViewClient()
    protected var webChromeClient: WebChromeClient = CcWebChromeClient()
    protected inner class CcWewViewClient : WebViewClient() {
        override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
            super.onReceivedError(view, errorCode, description, failingUrl)
//            showToast("网页加载失败,请稍后重试")
            //            finish();
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            if(!checkUrl(request?.toString())){
                return super.shouldOverrideUrlLoading(view, request)
            }else{
                return true
            }
        }
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if(!checkUrl(url?.toString())){
                return super.shouldOverrideUrlLoading(view, url)
            }else{
                return true
            }
        }

    }

    protected inner class CcWebChromeClient : WebChromeClient() {

        override fun onProgressChanged(view: WebView, newProgress: Int) {
//            progress.setProgress(newProgress)
//            if (newProgress == 100) {
//                progress.setVisibility(View.GONE)
//            }
        }
        //配置权限（同样在WebChromeClient中实现）
        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            callback.invoke(origin, true, false)
            super.onGeolocationPermissionsShowPrompt(origin, callback)
        }

    }



    private fun checkUrl(url: String?):Boolean{
        url?.apply{
            if(url.startsWith("qqkj:")){
                doSomethingByUrl(splitUrl(url))
                return true
            }
        }
        return false
    }

    protected fun splitUrl(url: String):String{
        return url
    }

    protected abstract fun doSomethingByUrl(url: String)
}
