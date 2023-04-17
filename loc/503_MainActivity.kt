package com.bluetoolth.cupping.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.webkit.*
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bluetoolth.cupping.CameraUtil
import com.bluetoolth.cupping.LocationUtil
import com.bluetoolth.cupping.R
import com.bluetoolth.cupping.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    companion object {
        const val LOCATION_PERMISSION = 11
        const val CAMERA_PERMISSION = 12
        const val READ_EXTERNAL_STORAGE_PERMISSION = 13
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var callback: GeolocationPermissions.Callback? = null
    private var origin: String? = null
    var currentPhotoPath: String? = null

    private val viewModel by viewModels<MainViewModel>()
    private lateinit var mDataBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
                onBackKey()
            }
        } else {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackKey()
                }
            })
        }

        WebView.setWebContentsDebuggingEnabled(true)
        mDataBinding = DataBindingUtil.setContentView<ActivityMainBinding>(this,
            R.layout.activity_main
        ).apply {
            swipeRefreshLayout.setOnRefreshListener(this@MainActivity)

            webView.settings.apply {
                textZoom = 100
                javaScriptEnabled = true
                domStorageEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setGeolocationEnabled(true)
                setSupportMultipleWindows(true)
            }

            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    // 전화
                    request?.apply {
                        if (url.toString().startsWith("tel:")) {
                            Intent(Intent.ACTION_VIEW, url).run {
                                startActivity(this)
                            }
                            return true
                        }

                        if (!listOf("http", "https").contains(url.scheme)) {
                            val intent = Intent.parseUri(url.toString(), Intent.URI_INTENT_SCHEME)
                            try {
                                startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            // Fallback Url이 있으면 현재 웹뷰에 로딩
                            val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                            if (fallbackUrl != null) {
                                view?.loadUrl(fallbackUrl)
                                return true
                            }
                        }
                    }

                    return super.shouldOverrideUrlLoading(view, request)
                }
            }

            webView.webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message?
                ): Boolean {
                    view?.context?.apply {
                        resultMsg?.let { resultMsg ->
                            val webView = WebView(this)
                            val transport: WebView.WebViewTransport = resultMsg.obj as WebView.WebViewTransport
                            transport.webView = webView
                            resultMsg.sendToTarget()

                            webView.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    request?.url?.let { url ->
                                        Intent(Intent.ACTION_VIEW, url).run {
                                            view?.context?.startActivity(this)
                                        }
                                    }
                                    return super.shouldOverrideUrlLoading(view, request)
                                }
                            }
                        }
                    }
                    return true
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = filePathCallback

                    fileChooserParams?.let {
                        CameraUtil(this@MainActivity).checkPermission(it.isCaptureEnabled)
                    }
                    return true
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {
                    this@MainActivity.origin = origin
                    this@MainActivity.callback = callback
                    super.onGeolocationPermissionsShowPrompt(origin, callback)
                    LocationUtil(this@MainActivity).checkPermission(origin, callback)
                }
            }

            lifecycleOwner = this@MainActivity
        }
    }

    private fun onBackKey() {
        with(mDataBinding.webView) {
            if (canGoBack()) goBack()
            else finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION -> {
                for (grantResult in grantResults) {
                    if (grantResult == PackageManager.PERMISSION_DENIED) {
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            // 다시 묻지 않기..
                            permissionGrantSetting(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        callback?.invoke(origin, true, false)
                        return
                    }
                }
                LocationUtil(this@MainActivity).checkPermission(origin, callback)
            }
            CAMERA_PERMISSION -> {
                for (grantResult in grantResults) {
                    if (grantResult == PackageManager.PERMISSION_DENIED) {
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                            // 다시 묻지 않기..
                            permissionGrantSetting(Manifest.permission.CAMERA)
                        }
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = null
                        return
                    }
                }
                CameraUtil(this@MainActivity).checkPermission(true)
            }
            READ_EXTERNAL_STORAGE_PERMISSION -> {
                for (grantResult in grantResults) {
                    if (grantResult == PackageManager.PERMISSION_DENIED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES)) {
                                permissionGrantSetting(Manifest.permission.READ_MEDIA_IMAGES)
                            }
                        } else {
                            if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                                // 다시 묻지 않기..
                                permissionGrantSetting(Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = null
                        }
                        return
                    }
                }
                CameraUtil(this@MainActivity).checkPermission(false)
            }
        }
    }

    override fun onRefresh() {
        Log.d("kimbh", "onRefresh()")
        if (::mDataBinding.isInitialized) {
            with(mDataBinding) {
                webView.reload()
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun permissionGrantSetting(permission: String) {
        val snackbar = when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> {
                // 위치 권한..
                Snackbar.make(
                    mDataBinding.webView,
                    R.string.location_permission_deny,
                    Snackbar.LENGTH_LONG
                )
            }
            Manifest.permission.CAMERA -> {
                // 카메라 권한..
                Snackbar.make(
                    mDataBinding.webView,
                    R.string.camera_permission_deny,
                    Snackbar.LENGTH_LONG
                )

            }
            else -> {
                Snackbar.make(
                    mDataBinding.webView,
                    R.string.external_storage_permission_deny,
                    Snackbar.LENGTH_LONG
                )
            }
        }

        snackbar.setAction(R.string.permission_confirm) {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).run {
                data = Uri.fromParts("package", packageName, null)
                startActivity(this)
            }
        }.show()
    }

    private fun getResultUri(data: Intent?): Uri? {
        var result: Uri? = null
        if (data == null || data.dataString.isNullOrEmpty()) {
            currentPhotoPath?.let {
                result = FileProvider.getUriForFile(
                    this@MainActivity,
                    getString(R.string.file_provider_authorities),
                    File(it)
                )
            }
        } else {
            result = Uri.parse(data.dataString)
        }
        return result
    }
}