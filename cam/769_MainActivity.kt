package com.acesso.acessobio_android.poc.webview

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.webkit.*
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var imageView: ImageView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        imageView = findViewById(R.id.imageView)

        webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            clearCache(true)
            // Add javascript interface on web application
            addJavascriptInterface(WebInterface {
                Handler(Looper.getMainLooper()).postDelayed({ loadImage(it) }, 500)
            }, "WebInterface")
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.grant(request.resources)
                }
            }
            loadUrl("https://yourwebapplication.com")
        }
    }

    override fun onBackPressed() {
        webView.visibility = View.VISIBLE
        imageView.visibility = View.GONE
    }

    private fun loadImage(base64: String) {
        webView.visibility = View.GONE
        imageView.visibility = View.VISIBLE

        val base64Image = base64.split(",").toTypedArray()[1]
        val decodedString = Base64.decode(base64Image, Base64.DEFAULT)
        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        imageView.setImageBitmap(decodedByte)
    }
}