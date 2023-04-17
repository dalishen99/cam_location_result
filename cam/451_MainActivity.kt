package com.example.poc_webviewinterface

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.web_journey.*


class MainActivity : AppCompatActivity() {

    val message: String? = null

    private var requestPermission =
        this.registerForActivityResult(ActivityResultContracts.RequestPermission()) { grant ->
            if (grant) {
                webViewJourney.evaluateJavascript(
                    "javascript: updateFromNative(\"camera permission granted$grant\")"
                ) { "true" }

            } else {
                webViewJourney.evaluateJavascript(
                    "javascript: updateFromNative(\"camera permission Not granted $grant\")"
                ) { "false" }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_journey)
        setUpView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpView() {
        val webView: WebSettings = webViewJourney.settings
        webViewJourney.loadUrl("file:///android_asset/test.html")
        webView.javaScriptEnabled = true

//        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            webViewJourney.loadUrl("javascript:popup()")
//            webViewJourney.evaluateJavascript("enable();", null);
//        }

        webViewJourney.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                super.onPermissionRequest(request)
                request?.grant(request.resources)
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.i("onPermissionRequest", "Request Permission")
                } else {
                    Log.i("onPermissionRequest", "Permission already granted")
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                super.onPermissionRequestCanceled(request)
                Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
        webViewJourney.addJavascriptInterface(this@MainActivity, "Android")

    }

    @JavascriptInterface
    fun requestCameraPermission() {

        if (this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

}