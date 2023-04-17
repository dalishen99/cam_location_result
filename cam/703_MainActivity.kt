package br.com.aldemir.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.http.SslError
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.*
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.aldemir.webview.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() = binding.webView.apply {
        settings.javaScriptEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.domStorageEnabled = true

        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.allowFileAccess = true
        settings.mediaPlaybackRequiresUserGesture = false
        webViewClient = object : WebViewClient() {
            @SuppressLint("WebViewClientOnReceivedSslError")
            override
            fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }
        }
    }

    private fun setWebClient() {
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(
                view: WebView,
                newProgress: Int
            ) {
                super.onProgressChanged(view, newProgress)
                binding.progressBar.apply {
                    progress = newProgress
                    if (newProgress < MAX_PROGRESS && visibility == ProgressBar.GONE) {
                        visibility = ProgressBar.VISIBLE
                    }
                    if (newProgress == MAX_PROGRESS) {
                        visibility = ProgressBar.GONE
                    }
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                val requestedResources = request!!.resources
                for (r in requestedResources) {
                    if (r == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                        break
                    }
                }
            }
        }
    }

    private fun checkCameraPermissionAndStartWebView() {
        val accessCameraPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (accessCameraPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
        } else {
            setWebClient()
        }
    }

    private fun loadUrl() {
        binding.webView.loadUrl(BASE_URL)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        checkCameraPermissionAndStartWebView()
        loadUrl()
    }

    companion object {
        const val MAX_PROGRESS = 100
        private const val BASE_URL = "https://www.google.com.br"
    }
}
