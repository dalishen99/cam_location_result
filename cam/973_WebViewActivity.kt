package com.teleferik

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.teleferik.databinding.ActivityWebViewBinding
import com.teleferik.utils.Constants


class WebViewActivity : AppCompatActivity() {

    var context: Context? = null
    var binding: ActivityWebViewBinding? = null
    private val restoredStatus: Bundle? = null
    private val lastUrl = ""
    override fun onResume() {
        super.onResume()
        if (binding!!.webView.canGoBack()) {
            binding!!.webView.goBack()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        context = applicationContext
        if (savedInstanceState == null) {
            openWebView()
        }

        binding!!.tvTitle.text = intent.getStringExtra(Constants.PARAMS.SCREEN_TITLE)
        binding!!.imgback.setOnClickListener { finish() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding!!.webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding!!.webView.restoreState(savedInstanceState)
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun openWebView() {
        val connectivityManager =
            context?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo?
        if (connectivityManager != null) {
            networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnectedOrConnecting) {
                binding!!.webView.visibility = View.VISIBLE
                binding!!.webView.settings.javaScriptEnabled = true
                binding!!.webView.settings.useWideViewPort = true
                binding!!.webView.settings.allowContentAccess = true
                binding!!.webView.settings.allowFileAccess = true
                binding!!.webView.settings.allowFileAccessFromFileURLs = true
                binding!!.webView.settings.allowUniversalAccessFromFileURLs = true
                binding!!.webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
                binding!!.webView.settings.domStorageEnabled = true
                binding!!.webView.settings.loadWithOverviewMode = true
                binding!!.webView.settings.mediaPlaybackRequiresUserGesture = false
                binding!!.webView.settings.loadsImagesAutomatically = true
                binding!!.webView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
                binding!!.webView.settings.pluginState = WebSettings.PluginState.ON
                binding!!.webView.webChromeClient = MyWebChromeClient()
                binding!!.webView.webViewClient = object : WebViewClient() {

                    @Deprecated("Deprecated")
                    override fun shouldOverrideUrlLoading(webview: WebView, url: String): Boolean {
                        webview.loadUrl(url)
                        return true
                    }

                    @TargetApi(Build.VERSION_CODES.N)
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest
                    ): Boolean {
                        return false
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        super.onPageFinished(view, url)
                    }
                }
                binding!!.webView.loadUrl(intent.getStringExtra(Constants.PARAMS.SCREEN_URL)!!)
            } else {

                binding!!.webView.visibility = View.INVISIBLE
                Toast.makeText(context, "Connect to Internet and Refresh Again", Toast.LENGTH_LONG)
                    .show()
            }
        } else {

            binding!!.webView.visibility = View.INVISIBLE
            Toast.makeText(context, "Connect to Internet and Refresh Again", Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (binding!!.webView.canGoBack()) {
                    binding!!.webView.goBack()
                } else {
                    binding!!.webView.loadUrl("about:blank")
                    finish()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    internal inner class MyWebChromeClient : WebChromeClient() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onPermissionRequest(request: PermissionRequest) {
            val requestedResources = request.resources
            for (r in requestedResources) {
                if (r == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    break
                }
            }
        }

        override fun onProgressChanged(view: WebView, progress: Int) {
            if (progress < 100 /* && pBar.getVisibility() == View.VISIBLE*/) {
                binding!!.pb.visibility = View.VISIBLE
            }
            binding!!.pb.progress = progress
            if (progress == 100) {
                binding!!.pb.visibility = View.GONE
            }
        }

        init {
            // TODO Auto-generated constructor stub
            binding!!.pb.progress = 0
        }
    }

    override fun onPause() {
        super.onPause()
        binding!!.webView.loadUrl("about:blank")
    }

    override fun onStop() {
        super.onStop()
        binding!!.webView.loadUrl("about:blank")
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}