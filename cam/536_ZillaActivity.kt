package com.zilla.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.webkit.*
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.zilla.EventListener
import com.zilla.Zilla
import com.zilla.ZillaWebInterface
import com.zilla.checkout_android.R
import com.zilla.checkout_android.databinding.ActivityZillaBinding
import com.zilla.commons.*
import com.zilla.di.AppContainer
import com.zilla.di.ViewModelConstructor
import com.zilla.di.ViewModelFactory
import com.zilla.model.ErrorType
import com.zilla.model.PaymentInfo
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class ZillaActivity : AppCompatActivity() {

    private var request: PermissionRequest? = null
    private val REQUEST_CODE_ASK_PERMISSIONS = 101

    private lateinit var binding: ActivityZillaBinding

    private lateinit var viewModel: ZillaViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivityZillaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        prepareWebView()
        viewModel.initiateTransaction()
    }

    private fun setupView() {
        val appContainer = AppContainer()
        viewModel = ViewModelProvider(
            this,
            ViewModelFactory.build(object : ViewModelConstructor {
                override fun create(): ViewModel {
                    return ZillaViewModel(appContainer.zillaRepository)
                }
            })
        )[ZillaViewModel::class.java]

        viewModel.apply {

            lifecycleScope.launch {
                loadingStatus.collect {
                    if (it) {
                        showLoading()
                    } else {
                        dismissLoading()
                    }
                }
            }

            lifecycleScope.launch {
                errorStatus.collect {
                    onError(it)
                }
            }

            lifecycleScope.launch {
                orderValidationSuccessful.collect {
                    hideAllViewsAndShowWebView()
                    loadUrl(it.paymentLink)
                }
            }

            lifecycleScope.launch {
                createWithPublicKeyInfo.collect {
                    hideAllViewsAndShowWebView()
                    loadUrl(it.paymentLink)
                }
            }
        }
    }

    private fun hideAllViewsAndShowWebView() {
        binding.webView.visibility = View.VISIBLE
        binding.webViewContainer.visibility = View.VISIBLE
    }

    @SuppressLint("NewApi", "SetJavaScriptEnabled")
    private fun prepareWebView() {
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.allowFileAccess = true
        binding.webView.settings.allowFileAccessFromFileURLs = true
        binding.webView.settings.allowUniversalAccessFromFileURLs = true
        binding.webView.isScrollbarFadingEnabled = false
        binding.webView.isVerticalScrollBarEnabled = true
        binding.webView.isHorizontalScrollBarEnabled = true
        binding.webView.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false;

        try {
            binding.webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        } catch (e: Exception) {
        }

        binding.webView.webViewClient = webViewClient
        binding.webView.webChromeClient = webViewChromeClient

        val webInterface = ZillaWebInterface(eventListener)

        binding.webView.addJavascriptInterface(webInterface, "ZillaWebInterface")
    }

    private fun loadUrl(paymentLink: String?) {
        Logger.log(this, "Load URL called $paymentLink")
        paymentLink?.let {
            binding.webView.loadUrl(it)
        }
    }

    // Loading Indicator implementation
    private fun isLoading(): Boolean {
        return findViewById<RelativeLayout>(R.id.progress_mask).isVisible
    }

    private fun showLoading() {
        if (isLoading()) return
        hideKeyBoard()
        findViewById<View>(R.id.progress_mask).show()
        findViewById<CircularProgressIndicator>(R.id.progress_indicator).show()
        disableTouch()
    }

    private fun dismissLoading() {
        findViewById<RelativeLayout>(R.id.progress_mask).visibility = View.GONE
        findViewById<CircularProgressIndicator>(R.id.progress_indicator).hide()
        enableTouch()
    }

    private fun onError(errorType: ErrorType) {
        Zilla.instance.callback.onError(errorType)
        finish()
    }

    private val webViewChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, progress: Int) {
            binding.webViewProgressBar.progress = progress
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            this@ZillaActivity.request = request
            val requestedResources = request!!.resources
            for (r in requestedResources) {
                if (r == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                    checkPermission(request)
                    binding.webView.loadUrl("javascript:document.getElementById('VideoClipPlayButton').style.visibility = 'hidden';");
                }
            }
        }
    }

    private fun checkPermission(request: PermissionRequest?) {
        if (hasCameraPermission() && hasStoragePermission()) {
            request?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
        } else {
            requestPermissions.launch(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                )
            )
        }
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var granted = true
            permissions.entries.forEach {
                val isGranted = it.value
                if (!isGranted) {
                    granted = it.value
                }
            }
            if (granted) {
                request?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            }
        }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun hasStoragePermission() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

    private val webViewClient = object : WebViewClient() {

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            Logger.log(this, "onPageStarted $url loading in WebView")
            super.onPageStarted(view, url, favicon)
            binding.webViewProgressBar.visibility = View.VISIBLE

            if (url.contains("/capture", true)) {
                if (!hasCameraPermission()) {
                    requestPermissions.launch(
                        arrayOf(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA,
                        )
                    )
                }
            }
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            Logger.log(this, "onPageFinished $url loading in WebView")

            binding.webViewProgressBar.visibility = View.GONE
        }
    }

    private val eventListener: EventListener = object : EventListener {
        override fun onClose() {
            Zilla.instance.callback.onClose()
            finish()
        }

        override fun onSuccess(paymentInfo: PaymentInfo) {
            Zilla.instance.callback.onSuccess(paymentInfo)
            finish()
        }

        override fun onRequestCameraPermission() {
            if (!hasCameraPermission()) {
                requestPermissions.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA,
                    )
                )
            }
        }
    }

    override fun onBackPressed() {

    }
}