package com.bnplwebview

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.bnplwebview.preference.AppPreference
import com.fastaccess.permission.base.PermissionHelper
import com.fastaccess.permission.base.callback.OnPermissionCallback
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


fun jsonToStr (s: String): JSONObject {
     val value = s.substring(1, s.length - 1)  // remove wrapping quotes
           .replace("\\\\", "\\")        // unescape \\ -> \
           .replace("\\\"", "\"")
   return JSONObject(value)
}

class MainActivity : AppCompatActivity(),
    OnPermissionCallback {
    protected var permissionHelper: PermissionHelper? = null
    private var safeBrowsingIsInitialized: Boolean = false
    private val APP_IMAGE_DIR = "images"
    val WEBVIEW_LOCAL_CURRENT_STAGE = "javascript:window.sessionStorage.getItem('current_stage');"
    val WEBJS_SESSION_STORAGE = "(function() { return JSON.stringify(sessionStorage); })();"
    val LOCAL_STORAGE_JS = "(function() { return JSON.stringify(localStorage); })();"
    var WEBVIEW_AUTH_JS = "(function() { return localStorage.getItem('auth'); })();"
    var token = ""
    val handler = Handler(Looper.getMainLooper())
    var swipeRefreshLayout: SwipeRefreshLayout? = null


    /**
     * Container for temp file uri
     *
     * @type string
     */
    protected var tempFileUri: String? = null

    /**
     * @type WebView
     */
    protected var webView: WebView? = null

    /**
     * @type ValueCallback<Uri></Uri>[]>
     */
    protected var fileUriCallback: ValueCallback<Array<Uri?>>? = null

    /**
     * @type WebSettings
     */
    protected var webViewSettings: WebSettings? = null
    var mGeoLocationRequestOrigin: String? = null
    var mGeoLocationCallback: GeolocationPermissions.Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lateinit var mUrl :String
        setContentView(R.layout.activity_main)
        val img_close by lazy { findViewById<ImageView>(R.id.img_close) }
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mUrl= intent.getStringExtra("url").toString()
        webView = findViewById<WebView>(R.id.my_web_view)
        swipeRefreshLayout = findViewById(R.id.swipe);
        swipeRefreshLayout!!.setOnRefreshListener {
            swipeRefreshLayout!!.isRefreshing = true
            Handler().postDelayed({
                swipeRefreshLayout!!.isRefreshing = false
                webView!!.reload();
            }, 3000)
        }
        swipeRefreshLayout!!.setColorSchemeColors(
            getResources().getColor(android.R.color.holo_blue_bright),
            getResources().getColor(android.R.color.holo_orange_dark),
            getResources().getColor(android.R.color.holo_green_dark),
            getResources().getColor(android.R.color.holo_red_dark)
        );
        webViewSettings = webView!!.settings
        webViewSettings!!.javaScriptEnabled = true
        webViewSettings!!.loadWithOverviewMode = true
        webViewSettings!!.allowFileAccess = true
        webViewSettings!!.setSupportMultipleWindows(true)
        webViewSettings!!.javaScriptCanOpenWindowsAutomatically = true
        webViewSettings!!.setGeolocationEnabled(true)
        val recorder = PayloadRecorder()
        webView!!.addJavascriptInterface(recorder, "recorder")
//        webView!!.evaluateJavascript("(function(){return window.getSelection().toString()})()",
//            ValueCallback<String> { value -> Log.v("evaluateJavascript", "SELECTION:$value") })
// //        webView!!.addJavascriptInterface(WebAppInterface(), "js")

       //file download manager
        webView!!.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val request = DownloadManager.Request(
                Uri.parse(url)
            )
            request.setMimeType(mimeType)
            val cookies = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading File...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                    url, contentDisposition, mimeType
                )
            )
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(applicationContext, "Downloading File", Toast.LENGTH_LONG).show()
        }
        //injecting data
        webView!!.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
//                val payload = recorder.getPayload(request.method, request.url.toString())
                Log.d("recorder", recorder.toString())
                // handle the request with the given payload and return the response
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)

//                view?.let { webView ->
//                    webView.evaluateJavascript(WEBVIEW_LOCAL_CURRENT_STAGE) { result ->
//                        Log.d("result : ---", result);
////                        {
////                            assets.open("override.js").reader().readText()
////                            null
////                        }
//                    }
//                }
            }
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                val urlStr = url.toString()
                val urlHost = File(URL(urlStr).host).toString()
                val endParams: String = File(URL(urlStr).path).name
                // your code
                if (lastPage != endParams) {
                    lastPage = endParams
                    if (urlStr == "https://redirect-staging.mandii.com/dashboard") {
                        updateSessionStatus()
                        Handler().postDelayed({
                            updateWebToLocal()
                        }, 10000)
                    }
                    if (endParams == "success" || endParams == "failure") {
                        payment_status = endParams
                    }
                    if (urlHost == "m.facebook.com") {
                        webView!!.destroy()
                        finish()
                        Handler().postDelayed({
                            Toast.makeText(
                                this@MainActivity,
                                "Payment got " + payment_status,
                                Toast.LENGTH_SHORT
                            ).show()
                        }, 4000)
                    }
                    if (urlStr == "https://redirect-staging.mandii.com/") {
                        webView!!.destroy()
                        finish()
                    }
                    if (endParams == "logout") {
                        /// clear prefernce storage
                    }
                }
//                Toast.makeText(this@MainActivity, "url: - $url", Toast.LENGTH_SHORT).show();
                super.doUpdateVisitedHistory(view, url, isReload)
            }
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
//                Log.d("onReceivedSslError", "onReceivedSslError")
                super.onReceivedSslError(view, handler, error);
                handler.proceed();
                handler.cancel();
            }


            fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
                webView!!.evaluateJavascript(WEBVIEW_LOCAL_CURRENT_STAGE,
                    ValueCallback<String?> { s -> Log.e("OnRecieve", s!!) })
            }

        }

        webView!!.webChromeClient = MyCustom_Api_ChromeClient()
        webViewSettings!!.mediaPlaybackRequiresUserGesture = false
        webViewSettings!!.javaScriptEnabled = true
        webViewSettings!!.domStorageEnabled=true
        webViewSettings!!.databaseEnabled=true
        webViewSettings!!.useWideViewPort=true
        webViewSettings!!.defaultTextEncodingName = "utf-8"
        webViewSettings!!.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webViewSettings!!.setAppCacheEnabled(true)
        webViewSettings!!.setAppCachePath(cacheDir.absolutePath + "/webViewCache")
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(this, ValueCallback<Boolean> { success ->
                safeBrowsingIsInitialized = true
                if (!success) {
                    Log.e("MY_APP_TAG", "Unable to initialize Safe Browsing!")
                }
            })
        }
        webViewSettings!!.loadWithOverviewMode = true
        webViewSettings!!.allowFileAccess = true
        // If SDK version is greater of 19 then activate hardware acceleration otherwise activate software acceleration
        if (Build.VERSION.SDK_INT >= 19) {
            webViewSettings!!.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            webViewSettings!!.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webView!!.settings.cacheMode=WebSettings.LOAD_DEFAULT
            webView!!.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 19) {
            webView!!.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        webView!!.loadUrl(mUrl)

        permissionHelper = PermissionHelper.getInstance(this)
        img_close.setOnClickListener(View.OnClickListener { finish() })
    }

    fun updateSessionStatus() {
        webView!!.evaluateJavascript(WEBJS_SESSION_STORAGE) { s ->
            try {
                if(s != null) {
                    val obj = jsonToStr(s)
                    val stage = obj.getString("current_stage").toString()
                    if(stage != "ENACH_COMPLETED") {
                        checkSessionEveryTime()
                    }
                    Log.d("stage", stage)
                    AppPreference.GetInstance()!!.setCurrentStatus(this@MainActivity, stage)
                }
            }catch (e: JSONException) {
                // here we can check current stage of session storage
            }

        }
    }
    fun checkSessionEveryTime() {
        // current stage check
        handler.postDelayed(Runnable {
            run {
                updateSessionStatus()
            }
        }, 5000)
    }
    fun updateWebToLocal() {
        webView!!.evaluateJavascript(LOCAL_STORAGE_JS){s ->
            try {
                val obj = jsonToStr(s)
                val pan = obj.getString("company_pan").toString()
                AppPreference.GetInstance()!!.setPan(this@MainActivity, pan)
            }catch(e: JSONException) {
                Log.e("localStorage", e.toString())
            }
        }
        webView!!.evaluateJavascript(WEBVIEW_AUTH_JS) {s ->
            try {
                val obj = jsonToStr(s)
                token = obj.getString("access_token").toString()
                val phoneNo = obj.getString("phone_number").toString()
                AppPreference.GetInstance()!!.setAccessToken(this@MainActivity, token)
                AppPreference.GetInstance()!!.setPhoneNo(this@MainActivity, phoneNo)
            }catch (e: JSONException) {
//                Log.e("Error in auth", e.toString())
            }
        }
    }

    override fun onBackPressed() {
        if (webView!!.isFocused && webView!!.canGoBack()) {
            webView!!.goBack()
        } else {
            super.onBackPressed()
            finish()
        }
    }

    fun js(view: WebView, code: String) {
        val javascriptCode = "javascript:$code"
        if (Build.VERSION.SDK_INT >= 19) {
            view.evaluateJavascript(
                javascriptCode
            ) { response -> Log.i("debug_log", response!!) }
        } else {
            view.loadUrl(javascriptCode)
        }
    }

    inner class MyCustom_Api_ChromeClient : WebChromeClient() {
        // For Android > 5.0
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri?>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            renderImageUploadOptions(filePathCallback)
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            runOnUiThread {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val PERMISSIONS = arrayOf(
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE
                    )
                    request.grant(PERMISSIONS)
                }
            }
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback?
        ) {

            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED
            ) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("Please turn ON the GPS to make app work smoothly")
                        .setNeutralButton(
                            android.R.string.ok,
                            DialogInterface.OnClickListener { dialogInterface, i ->
                                mGeoLocationCallback = callback
                                mGeoLocationRequestOrigin = origin
                                ActivityCompat.requestPermissions(
                                    this@MainActivity,
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001
                                )

                            })
                        .show()

                } else {
                    //no explanation need we can request the locatio
                    mGeoLocationCallback = callback
                    mGeoLocationRequestOrigin = origin
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001
                    )
                }
            } else {
                //tell the webview that permission has granted
                callback!!.invoke(origin, true, true)
            }
        }
    }

    protected fun convertPermToHumanReadable(permission: String?): String? {
        return when (permission) {
            Manifest.permission.CAMERA -> "camera"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "external storage"
            else -> null
        }
    }

    @Throws(IOException::class)
    protected fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir =
            File(this@MainActivity.getExternalFilesDir(null), APP_IMAGE_DIR)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        tempFileUri = image.absolutePath
        return image
    }

    protected fun intentCamera() {
        // Don't try to do individually, for some reason perms get pre-granted then nothing happens
        permissionHelper
            ?.setForceAccepting(false)
            ?.request(PERMISSIONS_CAMERA)
        if (permissionHelper!!.isPermissionGranted(Manifest.permission.CAMERA) &&
            permissionHelper!!.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            showCamera()
        }
    }

    protected fun intentGallery() {
        // Don't try to do individually, for some reason perms get pre-granted then nothing happens
        permissionHelper
            ?.setForceAccepting(false)
            ?.request(PERMISSIONS_GALLERY)
        if (permissionHelper!!.isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            showGallery()
        }
    }



    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionHelper!!.onActivityForResult(requestCode)
        when (requestCode) {
            REQUEST_CODE_ANDROID_5 -> {
                val result: Uri?
                if (null == fileUriCallback) {
                    return
                }
                result = if (data == null || resultCode != RESULT_OK) {
                    null
                } else {
                    data.data
                }
                if (result != null) {
                    fileUriCallback!!.onReceiveValue(arrayOf(result))
                    fileUriCallback = null
                }
            }
            REQUEST_CODE_THUMBNAIL -> {
                val file = File(tempFileUri)
                fileUriCallback = if (resultCode == RESULT_OK) {
                    val localUri = Uri.fromFile(file)
                    val localIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri)
                    this@MainActivity.sendBroadcast(localIntent)
                    fileUriCallback!!.onReceiveValue(arrayOf(localUri))
                    null
                } else {
                    if (file.exists()) {
                        file.delete()
                    }
                    fileUriCallback!!.onReceiveValue(arrayOf())
                    null
                }
            }
            REQUEST_CODE_GALLERY -> fileUriCallback = if (resultCode == RESULT_OK) {
                val selectedImageUri = data?.data
                val localIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, selectedImageUri)
                this@MainActivity.sendBroadcast(localIntent)
                // If we want to downsize check out the post
                fileUriCallback!!.onReceiveValue(arrayOf(selectedImageUri))
                null
            } else {
                fileUriCallback!!.onReceiveValue(arrayOf())
                null
            }
        }
    }

    protected fun renderImageUploadOptions(filePathCallback: ValueCallback<Array<Uri?>>?) {
        fileUriCallback = filePathCallback
        val items = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Add Photo!")
        builder.setItems(items) { dialog, item ->
            if (items[item] == "Take Photo") {
                intentCamera()
            } else if (items[item] == "Choose from Gallery") {
                intentGallery()
            } else if (items[item] == "Cancel") {
                fileUriCallback!!.onReceiveValue(arrayOf())
                fileUriCallback = null
                dialog.dismiss()
            }
        }
        builder.show()
    }

    protected fun showCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(this@MainActivity.packageManager) != null) {
            var imageUri: Uri? = null
            try {
                imageUri = FileProvider.getUriForFile(this@MainActivity,
                "com.bnplwebview.fileprovider",
                    createImageFile())
            } catch (e: IOException) {
                e.printStackTrace()
            }

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(takePictureIntent, REQUEST_CODE_THUMBNAIL)
        }
    }

    protected fun showGallery() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        startActivityForResult(Intent.createChooser(i, "File Chooser"), REQUEST_CODE_GALLERY)
    }

    /*
     * PermissionHelper overrides
     */
    override fun onPermissionGranted(permissionName: Array<String>) {
        //Log.i(LOG_TAG, "onPermissionGranted | Permission(s) " + Arrays.toString(permissionName) + " Granted");
    }

    override fun onPermissionDeclined(permissionName: Array<String>) {
        //Log.i(LOG_TAG, "onPermissionDeclined | Permission(s) " + Arrays.toString(permissionName) + " Declined");
    }

    override fun onPermissionPreGranted(permissionsName: String) {
        //Log.i(LOG_TAG, "onPermissionPreGranted | Permission( " + permissionsName + " ) preGranted");
    }

    override fun onPermissionNeedExplanation(permissionName: String) {
        //Log.d(LOG_TAG, "onPermissionPreGranted | Permission( " + permissionName + " ) preGranted");
    }

    override fun onPermissionReallyDeclined(permissionName: String) {
        //Log.i(LOG_TAG, "onPermissionReallyDeclined | Permission " + permissionName + " can only be granted from settingsScreen");
    }

    override fun onNoPermissionNeeded() {
        //Log.i(LOG_TAG, "onNoPermissionNeeded | Permission(s) not needed");
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Skipping parent on purpose, I don't want to keep annoying the user about declined, and permanently declined perms
        // onRequestPermissionsResult(requestCode, permissions, grantResults);
        val declinedPermissionsAsList: List<String?> =
            PermissionHelper.declinedPermissionsAsList(this, permissions)
        if (declinedPermissionsAsList.isNotEmpty()) {
            val declinedPermissions = declinedPermissionsAsList.toTypedArray()
            for (i in declinedPermissions.indices) {
                declinedPermissions[i] = convertPermToHumanReadable(declinedPermissions[i])
            }
            val dialog = AlertDialog.Builder(this)
                .setTitle("The following perms must be allowed to upload photos:")
                .setItems(declinedPermissions, null)
                .setPositiveButton(
                    "!shouttag Settings"
                ) { dialog, which ->
                    permissionHelper!!.openSettingsScreen()
                    fileUriCallback!!.onReceiveValue(arrayOf())
                    fileUriCallback = null
                }
                .setNegativeButton(
                    "Cancel"
                ) { dialog, which ->
                    fileUriCallback!!.onReceiveValue(arrayOf())
                    fileUriCallback = null
                    dialog.dismiss()
                }
                .show()
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.RED)
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.rgb(60, 179, 113))
        } else {
            if (listOf(*permissions).contains(Manifest.permission.CAMERA)) {
                showCamera()
            }else if(listOf(*permissions).contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
            }
        }
    }

    companion object {
        protected const val REQUEST_CODE_DEFAULT = 1
        protected const val REQUEST_CODE_ANDROID_5 = 2
        protected const val REQUEST_CODE_THUMBNAIL = 3
        protected const val REQUEST_CODE_GALLERY = 4
        protected const val LOG_TAG = "!!!!!"
        protected var payment_status = ""
        protected var lastPage = ""


        protected val PERMISSIONS_CAMERA = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        protected val PERMISSIONS_GALLERY = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
class PayloadRecorder {
    private val payloadMap: MutableMap<String, String> =
        mutableMapOf()
    @JavascriptInterface
    fun recordPayload(
        method: String,
        url: String,
        payload: String
    ) {
        payloadMap["$method-$url"] = payload
    }
    fun getPayload(
        method: String,
        url: String
    ): String? =
        payloadMap["$method-$url"]
}