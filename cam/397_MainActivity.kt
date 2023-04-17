package com.example.webviewtest

import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.example.webviewtest.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        initWebView()
    }

    private fun initWebView() {
        binding.webView1.settings.allowFileAccessFromFileURLs = true;
        binding.webView1.settings.allowUniversalAccessFromFileURLs = true;
        binding.webView1.settings.javaScriptEnabled = true;
        binding.webView1.settings.setDomStorageEnabled(true);
        binding.webView1.settings.setJavaScriptCanOpenWindowsAutomatically(true);
        binding.webView1.settings.setBuiltInZoomControls(true);
        binding.webView1.settings.setAllowFileAccess(true);
        binding.webView1.settings.setSupportZoom(true);
        binding.webView1.settings.mediaPlaybackRequiresUserGesture = false

        WebView.setWebContentsDebuggingEnabled(true)
        binding.webView1.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        binding.webView1.setWebViewClient(Callback())

//        binding.webView1.loadUrl("https://liveness.devel.mati.io");
//        binding.webView1.loadUrl("https://product.devel-28.mati.io/biometric-sdk/index.html?mobile=true&color=0097a7&locale=en&merchantToken=62500b0d25323c8d5a757d08&flowId=63bc1e339eb239357ece43d6&identityId=63bdce76d86823001bff3915&verificationId=63bdce76d86823001bff3917");
        binding.webView1.loadUrl("file:///android_asset/index.html")
    }

    private class Callback : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return false
        }

//        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
//            view.loadUrl(request.url.toString(), getCustomHeaders())
//            return true
//        }
//
//        private fun getCustomHeaders(): Map<String, String> {
//            val headers: MutableMap<String, String> = HashMap()
//            headers["User-Agent"] = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.5304.105 Mobile Safari/537.36"
//            return headers
//        }
    }
}