package com.media365.pwa

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.http.SslError
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.pwa.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity.class"
    val MY_PERMISSIONS_REQUEST_LOCATION = 1
    val PERMISSION_REQUEST_CODE_CAMERA = 2
    var mGeoLocationRequestOrigin: String? = null
    var mGeoLocationCallback: GeolocationPermissions.Callback? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initListeners()

        val mWebChromeCient = object : WebChromeClient() {

            override fun onPermissionRequest(request: PermissionRequest?) {
                //super.onPermissionRequest(request)
                Log.d(TAG, "onPermissionRequest: ${request?.resources?.size}")
                //request?.grant(request.resources)

                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.CAMERA),
                    PERMISSION_REQUEST_CODE_CAMERA
                );

            }


            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {

                if (!isLocationEnabled(this@MainActivity)) {
                    this@MainActivity.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    return
                }

                Log.d(
                    TAG,
                    "onGeolocationPermissionsShowPrompt: ${isLocationEnabled(this@MainActivity)}"
                )
                mGeoLocationRequestOrigin = null
                mGeoLocationCallback = null
                // Do We need to ask for permission?

                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {

                        AlertDialog.Builder(this@MainActivity)
                            .setMessage("Need permission")
                            .setNeutralButton(android.R.string.ok) { _, _ ->
                                mGeoLocationRequestOrigin = origin
                                mGeoLocationCallback = callback
                                ActivityCompat.requestPermissions(
                                    this@MainActivity,
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                    MY_PERMISSIONS_REQUEST_LOCATION
                                )
                            }
                            .show()

                    } else {
                        // No explanation needed, we can request the permission.

                        mGeoLocationRequestOrigin = origin
                        mGeoLocationCallback = callback
                        ActivityCompat.requestPermissions(
                            this@MainActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            MY_PERMISSIONS_REQUEST_LOCATION
                        )
                    }
                } else {
                    // Tell the WebView that permission has been granted
                    Log.d(TAG, "permission has been granted")
                    callback.invoke(origin, true, false)
                }
            }


        }

        with(binding) {
            webView.webViewClient = WebViewClient()
            webView.webChromeClient = mWebChromeCient
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            webView.overScrollMode = WebView.OVER_SCROLL_NEVER

            webView.isClickable = true
            webView.settings.domStorageEnabled = true
            webView.settings.setAppCacheEnabled(false)
            webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webView.clearCache(true)
            webView.settings.allowFileAccessFromFileURLs = true
            webView.settings.allowUniversalAccessFromFileURLs = true

            webView.webViewClient = MyWebViewClient()

            webView.addJavascriptInterface(JavascriptInterface(this@MainActivity), "AndroidInterface")

            webView.loadUrl("https://mybldev.doctime.com.bd/")
        }

    }

    private fun initListeners() {
        binding.swipe.setOnRefreshListener { binding.webView.reload() }
    }


    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    mGeoLocationCallback?.invoke(mGeoLocationRequestOrigin, true, false)
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    mGeoLocationCallback?.invoke(mGeoLocationRequestOrigin, false, false)
                }
            }
            PERMISSION_REQUEST_CODE_CAMERA -> {

            }
        }
        // other 'case' lines to check for other
        // permissions this app might request
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            val builder = AlertDialog.Builder(this@MainActivity)
            var message = "SSL Certificate error."
            when (error.primaryError) {
                SslError.SSL_UNTRUSTED -> message = "The certificate authority is not trusted."
                SslError.SSL_EXPIRED -> message = "The certificate has expired."
                SslError.SSL_IDMISMATCH -> message = "The certificate Hostname mismatch."
                SslError.SSL_NOTYETVALID -> message = "The certificate is not yet valid."
            }
            message += " Do you want to continue anyway?"
            builder.setTitle("SSL Certificate Error")
            builder.setMessage(message)
            builder.setPositiveButton(
                "continue"
            ) { dialog: DialogInterface?, which: Int -> handler.proceed() }
            builder.setNegativeButton(
                "cancel"
            ) { dialog: DialogInterface?, which: Int -> handler.cancel() }
            val dialog = builder.create()
            dialog.show()
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
           /* Log.e("Q#_BKASH", "External URL: $url")
            if (url == "https://www.bkash.com/terms-and-conditions") {
                val myIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(myIntent)
                return true
            }*/
            return super.shouldOverrideUrlLoading(view, url)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            //progressBar.setVisibility(View.VISIBLE)
        }

        override fun onPageFinished(view: WebView, url: String) {
            binding.swipe.isRefreshing = false
            // progressBar.setVisibility(View.GONE)

            //progressBar.setVisibility(View.GONE);
        }

    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }


}