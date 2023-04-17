package com.example.sportcelebrities.presentation.screens.web_view

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color.WHITE
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController

//var PERMISSION_REQUEST_CODE: Int = 1000
//val REQUEST_SELECT_FILE = 1002
//val FILECHOOSER_RESULTCODE = 1003
//var mUploadMessage: ValueCallback<Uri?>? = null
//var mCapturedImageURI: Uri? = null
//var mFilePathCallback: ValueCallback<Array<Uri>>? = null

@Composable
fun WebViewScreen(
    navController: NavHostController,
    webViewViewModel: WebViewViewModel = hiltViewModel()
) {
    val urlResponse by webViewViewModel.linkResponse.collectAsState()
    val context = LocalContext.current as Activity

    Box(
        modifier = Modifier.fillMaxSize()
    ){
        var mCustomTabsServiceConnection: CustomTabsServiceConnection? = null
        var mClient: CustomTabsClient? = null
        var mCustomTabsSession: CustomTabsSession? = null
        mCustomTabsServiceConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(componentName: ComponentName, customTabsClient: CustomTabsClient) {
                //Pre-warming
                mClient = customTabsClient
                mClient?.warmup(0L)
                mCustomTabsSession = mClient?.newSession(null)
            }
            override fun onServiceDisconnected(name: ComponentName) {
                mClient = null
                context.finish()
            }
        }
        CustomTabsClient.bindCustomTabsService(context, "com.android.chrome", mCustomTabsServiceConnection)
        val customTabsIntent = CustomTabsIntent.Builder(mCustomTabsSession)
            .setToolbarColor(WHITE)
            .setShowTitle(true)
            .build()

        customTabsIntent.launchUrl(context, Uri.parse("https://google.com"))
    }


    //ShowWebView(url = urlResponse ?: "youtube.com")

}

//@Suppress("DEPRECATION")
//@SuppressLint("SetJavaScriptEnabled")
//@Composable
//fun ShowWebView(
//    url: String
//) {
//    val activity = LocalContext.current as Activity
//    AndroidView(
//        factory = ::WebView,
//        modifier = Modifier.fillMaxSize()
//    ) {
//
//
//        var mCustomView: View? = null
//        var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null
//        var mOriginalOrientation = 0
//        var mOriginalSystemUiVisibility = 0
//
//        //Inject javascript code here that is executed after the page is loaded
//        val injectFunction = """
//        function () {
//            window['__NVW_WEBVIEW__'] = { isAndroid: true }
//        }
//        """.trimIndent()
//
//        WebView.setWebContentsDebuggingEnabled(true)
//
//        it.apply {
//            settings.apply {
//                javaScriptEnabled = true
//                allowFileAccessFromFileURLs = true
//                allowFileAccess = true
//                allowContentAccess = true
//                allowUniversalAccessFromFileURLs = true
//                javaScriptCanOpenWindowsAutomatically = true
//                mediaPlaybackRequiresUserGesture = false
//                domStorageEnabled = true
//                useWideViewPort = true
//                loadWithOverviewMode = true
//                displayZoomControls = false
//                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
//                userAgentString = settings.userAgentString.replace("; wv", "")
//                databaseEnabled = true
//            }
//
//            class PostMessageHandler {
//                @JavascriptInterface
//                fun postMessage(json: String?, transferList: String?): Boolean {
//                    return true
//                }
//            }
//            addJavascriptInterface(PostMessageHandler(), "__NVW_WEBVIEW_HANDLER__")
//
//            webViewClient = object : WebViewClient() {
//                override fun onReceivedError(
//                    view: WebView?,
//                    errorCode: Int,
//                    description: String?,
//                    failingUrl: String?
//                ) {
//                    Toast.makeText(activity, description, Toast.LENGTH_SHORT).show()
//                }
//
//                override fun shouldOverrideUrlLoading(
//                    view: WebView?,
//                    request: WebResourceRequest?
//                ): Boolean {
//                    view?.loadUrl(request?.url.toString())
//                    return true
//                }
//
//                override fun onReceivedError(
//                    view: WebView?,
//                    request: WebResourceRequest?,
//                    error: WebResourceError?
//                ) {
//                    super.onReceivedError(view, request, error)
//                    Toast.makeText(activity, "Ошибка в интернет соединении.", Toast.LENGTH_SHORT)
//                        .show()
//                }
//
//                @SuppressLint("WebViewClientOnReceivedSslError")
//                override fun onReceivedSslError(
//                    view: WebView?,
//                    handler: SslErrorHandler?,
//                    error: SslError?
//                ) {
//                    handler?.proceed()
//                }
//
//                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
//                    it.loadUrl("javascript:($injectFunction)()")
//                }
//            }
//            webChromeClient = object : WebChromeClient() {
//
//                // Grant permissions for cam
//                @TargetApi(Build.VERSION_CODES.M)
//                override fun onPermissionRequest(request: PermissionRequest) {
//                    activity.runOnUiThread {
//                        if ("android.webkit.resource.VIDEO_CAPTURE" == request.resources[0]) {
//                            if (ContextCompat.checkSelfPermission(
//                                    activity,
//                                    Manifest.permission.CAMERA
//                                ) == PackageManager.PERMISSION_GRANTED
//                            ) {
//                                request.grant(request.resources)
//                            } else {
//                                ActivityCompat.requestPermissions(
//                                    activity,
//                                    arrayOf(
//                                        Manifest.permission.CAMERA,
//                                        Manifest.permission.READ_EXTERNAL_STORAGE
//                                    ),
//                                    PERMISSION_REQUEST_CODE
//                                )
//                            }
//                        }
//                    }
//                }
//
//                // For Lollipop 5.0+ Devices
//                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//                override fun onShowFileChooser(
//                    mWebView: WebView?,
//                    filePathCallback: ValueCallback<Array<Uri>>?,
//                    fileChooserParams: FileChooserParams
//                ): Boolean {
//                    if (mFilePathCallback != null) {
//                        mFilePathCallback!!.onReceiveValue(null)
//                        mFilePathCallback = null
//                    }
//                    try {
//                        mFilePathCallback = filePathCallback
//
//                        val intent = fileChooserParams.createIntent()
//                        intent.type = "image/*"
//                        try {
//                            Log.d("HIT_TRY", "${intent.toURI()}, REQUEST_SELECT_FILE")
//
//                            activity.startActivity(intent)
//                        } catch (e: ActivityNotFoundException) {
//                            mFilePathCallback = null
//                            Toast.makeText(
//                                activity.applicationContext,
//                                "Cannot Open File Chooser",
//                                Toast.LENGTH_LONG
//                            ).show()
//                            return false
//                        }
//                        return true
//                    } catch (e: ActivityNotFoundException) {
//                        mFilePathCallback = null
//                        Toast.makeText(
//                            activity.applicationContext,
//                            "Cannot Open File Chooser",
//                            Toast.LENGTH_LONG
//                        ).show()
//                        return false
//                    }
//                }
//
//                protected fun openFileChooser(uploadMsg: ValueCallback<Uri?>?) {
//                    mUploadMessage = uploadMsg
//                    val i = Intent(Intent.ACTION_GET_CONTENT)
//                    i.addCategory(Intent.CATEGORY_OPENABLE)
//                    i.type = "image/*"
//                    activity.startActivityForResult(
//                        Intent.createChooser(i, "File Chooser"),
//                        FILECHOOSER_RESULTCODE
//                    )
//                }
//
//                override fun getDefaultVideoPoster(): Bitmap? {
//                    return if (mCustomView == null) {
//                        null
//                    } else BitmapFactory.decodeResource(activity.resources, 2130837573)
//                }
//
//                @Suppress("DEPRECATION")
//                override fun onHideCustomView() {
//                    (activity.window.decorView as FrameLayout).removeView(mCustomView)
//                    mCustomView = null
//                    activity.window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
//                    activity.requestedOrientation = mOriginalOrientation
//                    mCustomViewCallback!!.onCustomViewHidden()
//                    mCustomViewCallback = null
//                }
//
//                @Suppress("DEPRECATION")
//                override fun onShowCustomView(
//                    paramView: View?,
//                    paramCustomViewCallback: CustomViewCallback?
//                ) {
//                    if (mCustomView != null) {
//                        onHideCustomView()
//                        return
//                    }
//                    mCustomView = paramView
//                    mOriginalSystemUiVisibility = activity.window.decorView.systemUiVisibility
//                    mOriginalOrientation = activity.requestedOrientation
//                    mCustomViewCallback = paramCustomViewCallback
//                    (activity.window.decorView as FrameLayout).addView(
//                        mCustomView,
//                        FrameLayout.LayoutParams(-1, -1)
//                    )
//                    activity.window.decorView.systemUiVisibility =
//                        3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                }
//            }
//        }.loadUrl(url)
//    }
//}