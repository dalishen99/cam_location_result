package id.depok.depoksinglewindow.ui.shared

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.*
import android.webkit.WebSettings.RenderPriority.HIGH
import id.depok.depoksinglewindow.R
import id.depok.depoksinglewindow.databinding.ActivityWebpageBinding
import id.depok.depoksinglewindow.ui.shared.Arguments.ARG_TITLE
import id.depok.depoksinglewindow.ui.shared.Arguments.ARG_URL
import io.vrinda.kotlinpermissions.PermissionsActivity






@Suppress("DEPRECATION", "OverridingDeprecatedMember")
class WebPageActivity : PermissionsActivity() {

    private var title: String? = null
    private var url: String? = null
    lateinit var binding: ActivityWebpageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_webpage)

        handleExtras()
        initialize()

        /*callPermission.setOnClickListener {
            requestPermissions(Manifest.permission.ACCESS_FINE_LOCATION, object : PermissionCallBack {
                override fun permissionGranted() {
                    super.permissionGranted()
                    Log.v("Call permissions", "Granted")
                }

                override fun permissionDenied() {
                    super.permissionDenied()
                    Log.v("Call permissions", "Denied")
                }
            })
        }*/
    }

    inner class webChromeClient : WebChromeClient() {
        override fun onGeolocationPermissionsShowPrompt(origin: String,
                                                        callback: GeolocationPermissions.Callback) {
            //Log.d("Reza Testing", "onGeolocationPermissionsShowPrompt")
            // Always grant permission since the app itself requires location
            // permission and the user has therefore already granted it
            callback.invoke(origin, true, false)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webview.canGoBack()) {
            Log.d("Reza Testing", "onKeyDown")
            binding.webview.goBack()
            return true
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event)
    }

    private fun handleExtras() {
        title = intent.extras.getString(ARG_TITLE)
        url = intent.extras.getString(ARG_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initialize() {
        Log.d("Reza Testing", "Initiated")
        setSupportActionBar(binding.toolbarWebpage)

        binding.apply {
            toolbarWebpage.setNavigationOnClickListener { finish() }


            val webSettings = webview.settings
            webSettings.javaScriptEnabled = true
            webSettings.allowContentAccess = true
            webSettings.domStorageEnabled = true
            webSettings.useWideViewPort = true
            webSettings.saveFormData = true
            webSettings.loadWithOverviewMode = true
            webSettings.loadsImagesAutomatically = true
            webSettings.setRenderPriority(HIGH)
            webSettings.setAppCacheEnabled(true)
            webSettings.setGeolocationEnabled(true)
            webSettings.setSupportMultipleWindows(true)
            webSettings.pluginState = WebSettings.PluginState.ON
            webSettings.allowContentAccess = true
            webSettings.allowFileAccessFromFileURLs = true
            webSettings.allowUniversalAccessFromFileURLs = true
            webSettings.javaScriptCanOpenWindowsAutomatically = true
            webSettings.allowFileAccess = true
            webSettings.builtInZoomControls = true
            webSettings.displayZoomControls = false
            webSettings.saveFormData = true
            webSettings.enableSmoothTransition()
            webSettings.allowUniversalAccessFromFileURLs = true


//            webview.webChromeClient = object : WebChromeClient() {
//                // For 3.0+ Devices (Start)
//                // onActivityResult attached before constructor
//                protected fun openFileChooser(uploadMsg: ValueCallback<*>, acceptType: String) {
//                    val mUploadMessage = uploadMsg
//                    val i = Intent(Intent.ACTION_GET_CONTENT)
//                    i.addCategory(Intent.CATEGORY_OPENABLE)
//                    i.type = "image/*"
//                    startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE)
//                }
//
//
//                // For Lollipop 5.0+ Devices
//                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//                override fun onShowFileChooser(mWebView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
//                    if (uploadMessage != null) {
//                        uploadMessage!!.onReceiveValue(null)
//                        uploadMessage = null
//                    }
//
//                    uploadMessage = filePathCallback
//
//                    val intent = fileChooserParams.createIntent()
//                    try {
//                        startActivityForResult(intent, REQUEST_SELECT_FILE)
//                    } catch (e: ActivityNotFoundException) {
//                        uploadMessage = null
//                        Toast.makeText(applicationContext, "Cannot Open File Chooser", Toast.LENGTH_LONG).show()
//                        return false
//                    }
//
//                    return true
//                }
//
//                //For Android 4.1 only
//                protected fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String) {
//                    mUploadMessage = uploadMsg
//                    val intent = Intent(Intent.ACTION_GET_CONTENT)
//                    intent.addCategory(Intent.CATEGORY_OPENABLE)
//                    intent.type = "image/*"
//                    startActivityForResult(Intent.createChooser(intent, "File Browser"), FILECHOOSER_RESULTCODE)
//                }
//
//                protected fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
//                    mUploadMessage = uploadMsg
//                    val i = Intent(Intent.ACTION_GET_CONTENT)
//                    i.addCategory(Intent.CATEGORY_OPENABLE)
//                    i.type = "image/*"
//                    startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE)
//                }
//            }

            webview.webViewClient = object : WebViewClient() {
                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    Log.d("Reza Testing", "onReceivedSslError")
                    webview.loadUrl(url)
                    handler.proceed()
                }

                override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                    Log.d("testing received error", "onReceivedError")
                    webview.loadUrl("file:///android_asset/index.html")

                }

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean
                {
                    Log.d("Reza Testing", "shouldOverrideUrlLoading")
                    if (url.startsWith("tel:") ||
                            url.startsWith("mailto:") ||
                            url.startsWith("sms:") ||
                            url.startsWith("smsto:") ||
                            url.startsWith("mms:") ||
                            url.startsWith("mmsto:") ||
                            url.startsWith("geo:") ||
                            url.startsWith("maps:"))
//                            url.startsWith("http:") ||
//                            url.startsWith("https:"))
                    {
                        Log.d("Reza Testing", "shouldOverrideUrlLoading TRUE")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    }
//                    // allow the OS to handle it
//                    val intent = Intent(Intent.ACTION_VIEW)
//                    intent.data = Uri.parse(url)
//                    cordova.getActivity()
                    return super.shouldOverrideUrlLoading(view, url)
                }

                @TargetApi(Build.VERSION_CODES.N)
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean
                {
                    Log.d("Reza Testing", "shouldOverrideUrlLoading N")
                    if (request?.url.toString().startsWith("tel:") ||
                            request?.url.toString().startsWith("mailto:") ||
                            request?.url.toString().startsWith("sms:") ||
                            request?.url.toString().startsWith("smsto:") ||
                            request?.url.toString().startsWith("mms:") ||
                            request?.url.toString().startsWith("mmsto:") ||
                            request?.url.toString().startsWith("geo:") ||
                            request?.url.toString().startsWith("maps:"))
//                            request?.url.toString().startsWith("http:") ||
//                            request?.url.toString().startsWith("https:"))
                    {
                        Log.d("Reza Testing", "shouldOverrideUrlLoading N TRUE")
                        val intent = Intent(Intent.ACTION_VIEW, request?.url)
                        startActivity(intent)

                        return true
                    }

                    return super.shouldOverrideUrlLoading(view, request)
                }
            }
            
            if (!url.isNullOrEmpty()) 
            {
                Log.d("Reza Testing", "isNullOrEmpty")
                webview.loadUrl(url)
            }
            webview.webChromeClient = webChromeClient()

        webview.setDownloadListener { url,
                                      _,
                                      _,
                                      _,
                                      _
            -> val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.title = title
    }
}

object Arguments 
{
    const val ARG_TITLE = "title"
    const val ARG_URL = "url"
}
