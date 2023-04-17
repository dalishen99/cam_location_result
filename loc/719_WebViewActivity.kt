package com.co.brasso.feature.webView

import android.net.Uri
import android.os.Bundle
import android.webkit.*
import com.co.brasso.databinding.ActivityWebViewBinding
import com.co.brasso.feature.shared.base.BaseActivity
import com.co.brasso.utils.constant.BundleConstant
import com.co.brasso.utils.extension.viewGone
import com.co.brasso.utils.extension.viewVisible
import com.co.brasso.utils.util.NetworkUtils

class WebViewActivity : BaseActivity<WebView, WebViewPresenter>(), WebView {

    private lateinit var binding: ActivityWebViewBinding
    private var webLink: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        getIntentData()
        loadUrl()
        initToolbar()
        initListeners()
    }

    private fun initListeners() {
        binding.incToolBar.imgBack.setOnClickListener { onBackPressed() }
    }

    private fun initToolbar() {
        hideCartIcon()
        hideNotificationIcon()
    }

    private fun hideNotificationIcon() {
        binding.incToolBar.imgNotify.viewGone()
        binding.incToolBar.cnlNotificationCount.viewGone()
    }

    private fun hideCartIcon() {
        binding.incToolBar.imgCart.viewGone()
        binding.incToolBar.imgCartItem.viewGone()
        binding.incToolBar.txvCartCount.viewGone()
    }

    private fun getIntentData() {
        webLink = intent.getStringExtra(BundleConstant.webViewLink)
    }

    private fun loadUrl() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            loadUrl(webLink)
        } else {
            hideProgressBar()
        }
    }

    private fun loadUrl(url: String?) {
        binding.wbvWebView.webViewClient = WebViewClient()
        binding.wbvWebView.settings.javaScriptEnabled = true
        binding.wbvWebView.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.wbvWebView.settings.builtInZoomControls = true
        binding.wbvWebView.settings.setGeolocationEnabled(true)
        binding.wbvWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: android.webkit.WebView, progress: Int) {
                showProgressBar()
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)

            }
        }
        binding.wbvWebView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: android.webkit.WebView, url: String) {
                super.onPageFinished(view, url)
                hideProgressBar()
            }

            override fun onReceivedError(
                view: android.webkit.WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                hideProgressBar()
            }

            override fun onReceivedHttpError(
                view: android.webkit.WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                hideProgressBar()
            }
        }
//        binding.wbvWebView.settings.setAppCacheEnabled(true)
        binding.wbvWebView.settings.databaseEnabled = true
        binding.wbvWebView.settings.domStorageEnabled = true
        binding.wbvWebView.settings.setGeolocationDatabasePath(filesDir.path)
        binding.wbvWebView.loadUrl(Uri.parse(webLink).toString())
    }

    override fun showProgressBar() {
        binding.incProgressbar.cnsProgress.viewVisible()
    }

    override fun hideProgressBar() {
        binding.incProgressbar.cnsProgress.viewGone()
    }

    override fun createPresenter() = WebViewPresenter()
}