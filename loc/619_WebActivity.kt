package com.payment.iydapayment.customWebview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.payment.krishipay.MainActivity
import com.payment.krishipay.R
import com.payment.krishipay.databinding.ActivityOnlineServiceBinding
import java.io.File
import java.io.IOException


class WebActivity : AppCompatActivity() {
    var mGeoLocationRequestOrigin: String? = null
    var mGeoLocationCallback: GeolocationPermissions.Callback? = null
    val MAX_PROGRESS = 100
    private var serviceType: String? = null
    private var url: String? = null
    lateinit var webView: WebView
    lateinit var progressBar: ProgressBar

    var context: Context? = null

    private val TAG = MainActivity::class.java.simpleName
    private val FILECHOOSER_RESULTCODE = 1
    private var mUploadMessage: ValueCallback<Uri>? = null
    private var mCapturedImageURI: Uri? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null

    lateinit var binding: ActivityOnlineServiceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_online_service)
        binding = ActivityOnlineServiceBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.webViewCloseIc.setOnClickListener {
            finish()
        }
        webView = binding.webView
        progressBar = binding.progressBar

        loadReq()
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        val writeExternalStorage =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (writeExternalStorage != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1001)
        }
    }

    private fun checkPermission() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }
        if (!gps_enabled && !network_enabled) {
            val dialog = AlertDialog.Builder(this)
                .setMessage("GPS Location is not Enable")
                .setPositiveButton(
                    "Location Setting"
                ) { paramDialogInterface: DialogInterface?, paramInt: Int ->
                    this.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    )
                }
                .show()
            dialog.setCancelable(false)
        }
    }

    private fun loadReq() {
        initWebView()
        setWebClient()
        val bundle = intent.extras
        url = bundle!!.getString("url")
        serviceType = bundle.getString("serviceType")
        binding.webViewTitle.text = serviceType
        if (url == null || url!!.isEmpty()) {
            Toast.makeText(this, "Service not available", Toast.LENGTH_SHORT).show()
            finish()
        }

        loadUrl(url!!)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.setGeolocationEnabled(true)
        webView.settings.databaseEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                Log.e("Log", "SSL Error")
                val builder = AlertDialog.Builder(this@WebActivity)
                builder.setMessage(R.string.notification_error_ssl_cert_invalid)
                builder.setPositiveButton("continue") { _, _ -> handler!!.proceed() }
                builder.setNegativeButton("cancel") { _, _ -> handler!!.cancel() }
                val dialog = builder.create()
                dialog.show()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url!!)
                return true
            }

        }
    }


    private fun setWebClient() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {}
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                if (ContextCompat.checkSelfPermission(this@WebActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this@WebActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        AlertDialog.Builder(this@WebActivity)
                            .setMessage("Please turn ON the GPS to make app work smoothly")
                            .setNeutralButton(
                                android.R.string.ok
                            ) { dialogInterface, i ->
                                mGeoLocationCallback = callback
                                mGeoLocationRequestOrigin = origin
                                ActivityCompat.requestPermissions(
                                    this@WebActivity,
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001
                                )
                            }.show()
                    } else {
                        mGeoLocationCallback = callback
                        mGeoLocationRequestOrigin = origin
                        ActivityCompat.requestPermissions(this@WebActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
                    }
                }else if (ContextCompat.checkSelfPermission(this@WebActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@WebActivity, arrayOf(Manifest.permission.CAMERA), 1001)
                } else {
                    //tell the webview that permission has granted
                    Log.e("LOG", "Permission Granted -1 ")
                    callback!!.invoke(origin, true, true)
                    //webView.reload()
                }
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress < MAX_PROGRESS && progressBar.visibility == ProgressBar.GONE) {
                    progressBar.visibility = ProgressBar.VISIBLE
                }
                if (newProgress == MAX_PROGRESS) {
                    progressBar.visibility = ProgressBar.GONE
                }
            }

            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?): Boolean {
//                Toast.makeText(this@WebActivity, "Select Image", Toast.LENGTH_SHORT).show()
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePathCallback
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "image/*"
                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Choose File")
                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE)
                return true
            }

            @Throws(IOException::class)
            private fun createImageFile(): File? {
                var imageStorageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "DirectoryNameHere")
                if (!imageStorageDir.exists()) { imageStorageDir.mkdirs() }
                imageStorageDir = File(imageStorageDir.toString() + File.separator +
                        "IMG_" + System.currentTimeMillis().toString() + ".jpg")
                return imageStorageDir
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


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun loadUrl(pageUrl: String) {
        webView.loadUrl(pageUrl)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (mGeoLocationCallback != null) {
                        mGeoLocationCallback!!.invoke(mGeoLocationRequestOrigin, true, true)
                        Log.e("LOG", "Permission Granted -2 ")
                    }
                } else {
                    if (mGeoLocationCallback != null) {
                        mGeoLocationCallback!!.invoke(mGeoLocationRequestOrigin, false, false)
                    }
                }
            }
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != FILECHOOSER_RESULTCODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        var results: Array<Uri> = arrayOf()
        if (resultCode == Activity.RESULT_OK) {
            if (data == null || data.data == null) {
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
    }
}