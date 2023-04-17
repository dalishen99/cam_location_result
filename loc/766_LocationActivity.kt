package com.example.webview

import android.Manifest
import android.app.DownloadManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class LocationActivity : AppCompatActivity() {

    var myWebView:WebView?=null

    override fun onCreate(savedInstanceState: Bundle?) {


        var mGeoLocationRequestOrigin: String? = null
        var mGeoLocationCallback: GeolocationPermissions.Callback? = null



        super.onCreate(savedInstanceState)
        setContentView(R.layout.location_activity)
        supportActionBar?.hide()
        myWebView = findViewById(R.id.locationView)
        myWebView?.settings?.javaScriptEnabled = true;
        myWebView?.settings?.domStorageEnabled = true

        myWebView?.settings?.javaScriptCanOpenWindowsAutomatically = true
        myWebView?.settings?.builtInZoomControls = true

        myWebView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url);
                return true;
            }
        }
        myWebView?.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            //------------------------COOKIE!!------------------------
            val cookies = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            //------------------------COOKIE!!------------------------
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(url, contentDisposition, mimeType)
            )
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(applicationContext, "Downloading File, Size: ${contentLength/1000000} MB", Toast.LENGTH_LONG).show()
        })
        myWebView?.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.resources)
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                val permissionCheckFineLocation = ActivityCompat.checkSelfPermission(
                    this@LocationActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (permissionCheckFineLocation != PackageManager.PERMISSION_GRANTED) {
                    mGeoLocationCallback = callback
                    mGeoLocationRequestOrigin = origin
                    //requesting permission
                    ActivityCompat.requestPermissions(
                        this@LocationActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        123
                    )
                } else { // permission and the user has therefore already granted it
                    callback.invoke(origin, true, false)
                }
            }
        }
        val value = 101
        fun makeRequest() {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                value)
        }
        fun setView() {
            myWebView?.loadUrl("https://www.google.com/maps")
        }
        makeRequest()
        setView();

    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action === KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (myWebView?.canGoBack() == true) {
                        myWebView?.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}