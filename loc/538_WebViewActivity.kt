package com.coderivium.p4rcintegrationsample

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.appcompat.widget.AppCompatImageView
import android.os.Bundle
import com.coderivium.p4rcintegrationsample.R
import android.os.Environment
import android.webkit.WebChromeClient.FileChooserParams
import android.content.Intent
import android.provider.MediaStore
import com.coderivium.p4rcintegrationsample.WebViewActivity
import kotlin.jvm.JvmOverloads
import android.os.Parcelable
import android.os.Build
import android.widget.Toast
import kotlin.Throws
import android.graphics.Bitmap
import android.content.DialogInterface
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.content.pm.PackageManager
import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.IntentSender
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.Window
import android.webkit.*
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class WebViewActivity : AppCompatActivity() {
    private val REQUEST_LOCATION = 1
    private var mGeoLocationCallback: GeolocationPermissions.Callback? = null
    private var mGeoLocationRequestOrigin: String? = null

    private var webView: WebView? = null
    var back: ImageView? = null
    var namesForward = ""
    private var mUploadMessage: ValueCallback<Uri?>? = null
    private var mCapturedImageURI: Uri? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    private val REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124
    private var webViewPreviousState = 0
    private val PAGE_STARTED = 0x1
    private val PAGE_REDIRECTED = 0x2
    private val rootView: CoordinatorLayout? = null
    var baseUrl = "https://myxr-web-stage.kiwi-internal.com/www.google.com"
    private var refresh: AppCompatImageView? = null
    private var homeClick: AppCompatImageView? = null
    private var backClick: AppCompatImageView? = null
    private var forwardClick: AppCompatImageView? = null
    private var toolbar: Toolbar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE) //will hide the title// hide the title bar
        setContentView(R.layout.activity_web_viiew)
        namesForward = baseUrl
        webView = findViewById<View>(R.id.web_view) as WebView
        backClick = findViewById(R.id.backClick)
        refresh = findViewById(R.id.refresh)
        homeClick = findViewById(R.id.homeClick)
        forwardClick = findViewById(R.id.forwardClick)
        toolbar = findViewById(R.id.toolbar)
        val webSettings = webView!!.settings
        //webView!!.setInitialScale(160)
        webSettings.setSupportZoom(false)
        webSettings.loadWithOverviewMode = true
        webSettings.builtInZoomControls = false
        webSettings.javaScriptEnabled = true
        webSettings.allowFileAccess = true

//            changes
        webSettings.setAppCacheEnabled(true);
        webSettings.databaseEnabled = true;
        webSettings.domStorageEnabled = true;
        webSettings.setGeolocationEnabled(true)
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webView!!.loadUrl(baseUrl)
        backClick?.setOnClickListener(View.OnClickListener { v: View? -> webView!!.goBack() })
        forwardClick?.setOnClickListener(View.OnClickListener { v: View? -> webView!!.goForward() })
        refresh?.setOnClickListener(View.OnClickListener { v: View? -> webView!!.reload() })
        homeClick?.setOnClickListener(View.OnClickListener { v: View? -> webView!!.loadUrl(baseUrl) })
        webView!!.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimetype)
            request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalFilesDir(
                this@WebViewActivity,
                Environment.DIRECTORY_DOWNLOADS,
                ".png"
            )
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }
        webView!!.setWebChromeClient(object : WebChromeClient() {
            // For Android 5.0
            override fun onShowFileChooser(
                view: WebView,
                filePath: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                // Double check that we don't have any existing callbacks
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePath
                var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (takePictureIntent!!.resolveActivity(packageManager) != null) {
                    // Create the File where the photo should go
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                    } catch (ex: IOException) {
                        // Error occurred while creating the File
                        Log.e(TAG, "Unable to create Image File", ex)
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        takePictureIntent.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile)
                        )
                    } else {
                        takePictureIntent = null
                    }
                }
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "text/csv"
                val intentArray: Array<Intent?>
                intentArray = takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls(0)
                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
                return true
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri?>?, acceptType: String? = "") {
                mUploadMessage = uploadMsg
                val imageStorageDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "AndroidExampleFolder"
                )
                if (!imageStorageDir.exists()) {
                    imageStorageDir.mkdirs()
                }
                val file = File(
                    imageStorageDir.toString() + File.separator + "IMG_" + System.currentTimeMillis()
                        .toString() + ".jpg"
                )
                mCapturedImageURI = Uri.fromFile(file)
                val captureIntent = Intent(
                    MediaStore.ACTION_IMAGE_CAPTURE
                )
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "text/csv"
                val chooserIntent = Intent.createChooser(i, "Image Chooser")
                chooserIntent.putExtra(
                    Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(captureIntent)
                )
                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE)
            }

            fun openFileChooser(
                uploadMsg: ValueCallback<Uri?>?,
                acceptType: String?,
                capture: String?
            ) {
                openFileChooser(uploadMsg, acceptType, capture)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback,
            ) {
                Log.d("TAG", "onGeolocationPermissionsShowPrompt: ")

                runOnUiThread {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@WebViewActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        Log.d("TAG", "onGeolocationPermissionsShowPrompt: kya ")

                        mGeoLocationRequestOrigin = origin
                        mGeoLocationCallback = callback
                        ActivityCompat.requestPermissions(
                            this@WebViewActivity,
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                            REQUEST_LOCATION
                        )
                    } else {

                        if (checkGPSEnabled()) {
                            callback.invoke(origin, true, false)

                        } else {
                            enableLocationSettings()
//                            todo wait until the location turn on and if user turn it on then call the callback
//                            callback.invoke(origin, false, false)

                        }
                    }
                }

            }
        })
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data)
                return
            }
            var results: Array<Uri>? = null
            // Check that the response is a good one
            if (resultCode == RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
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
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data)
                return
            }
            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == mUploadMessage) {
                    return
                }
                var result: Uri? = null
                try {
                    result = if (resultCode != RESULT_OK) {
                        null
                    } else {
                        // retrieve from the private variable if the intent is null
                        if (data == null) mCapturedImageURI else data.data
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        applicationContext, "activity :$e",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mUploadMessage!!.onReceiveValue(result)
                mUploadMessage = null
            }
        }
        return
    }

    private fun checkGPSEnabled(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return !manager.isProviderEnabled(LocationManager.GPS_PROVIDER).not()
    }

    private fun enableLocationSettings() {
        val request = LocationRequest.create().apply {
            interval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(request)
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnFailureListener {
            Log.d("TAG", "enableLocationSettings: fail")
            if (it is ResolvableApiException) {
                try {
                    it.startResolutionForResult(this, 12345)
                } catch (sendEx: IntentSender.SendIntentException) {
                }
            }
        }.addOnSuccessListener {
            Log.d("TAG", "enableLocationSettings: success")
            //here GPS is On
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
    }

    inner class GeoWebChromeClient : WebChromeClient() {
        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            callback.invoke(origin, true, false)
        }
    }

    inner class GeoWebViewClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            // When user clicks a hyperlink, load in the existing WebView
            view.loadUrl(url)
            return true
        }

        var loadingDialog: Dialog? = Dialog(this@WebViewActivity)
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap) {
            super.onPageStarted(view, url, favicon)
            webViewPreviousState = PAGE_STARTED
            if (loadingDialog == null || !loadingDialog!!.isShowing) loadingDialog =
                ProgressDialog.show(
                    this@WebViewActivity, "",
                    "Loading Please Wait", true, true
                ) {
                    // do something
                }
            loadingDialog!!.setCancelable(false)
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        override fun onReceivedError(
            view: WebView, request: WebResourceRequest,
            error: WebResourceError
        ) {
            if (isConnected) {
                val snackBar = Snackbar.make(
                    rootView!!,
                    "onReceivedError : " + error.description,
                    Snackbar.LENGTH_INDEFINITE
                )
                snackBar.setAction("Reload") { webView!!.loadUrl("javascript:window.location.reload( true )") }
                snackBar.show()
            } else {
                val snackBar =
                    Snackbar.make(rootView!!, "No Internet Connection ", Snackbar.LENGTH_INDEFINITE)
                snackBar.setAction("Enable Data") {
                    startActivityForResult(Intent(Settings.ACTION_WIRELESS_SETTINGS), 0)
                    webView!!.loadUrl("javascript:window.location.reload( true )")
                    snackBar.dismiss()
                }
                snackBar.show()
            }
            super.onReceivedError(view, request, error)
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            if (isConnected) {
                val snackBar = Snackbar.make(
                    rootView!!,
                    "HttpError : " + errorResponse.reasonPhrase,
                    Snackbar.LENGTH_INDEFINITE
                )
                snackBar.setAction("Reload") { webView!!.loadUrl("javascript:window.location.reload( true )") }
                snackBar.show()
            } else {
                val snackBar =
                    Snackbar.make(rootView!!, "No Internet Connection ", Snackbar.LENGTH_INDEFINITE)
                snackBar.setAction("Enable Data") {
                    startActivityForResult(Intent(Settings.ACTION_WIRELESS_SETTINGS), 0)
                    webView!!.loadUrl("javascript:window.location.reload( true )")
                    snackBar.dismiss()
                }
                snackBar.show()
            }
            super.onReceivedHttpError(view, request, errorResponse)
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (webViewPreviousState == PAGE_STARTED) {
                if (null != loadingDialog) {
                    loadingDialog!!.dismiss()
                    loadingDialog = null
                }
            }
        }
    }

    val isConnected: Boolean
        get() {
            val cm = this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            if (null != cm) {
                val info = cm.activeNetworkInfo
                return info != null && info.isConnected
            }
            return false
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS) {
            val perms: MutableMap<String, Int> = HashMap()
            // Initial
            perms[Manifest.permission.ACCESS_FINE_LOCATION] = PackageManager.PERMISSION_GRANTED
            for (i in permissions.indices) perms[permissions[i]] = grantResults[i]
            if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@WebViewActivity,
                    "All Permission GRANTED !! Thank You :)",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                Toast.makeText(
                    this@WebViewActivity,
                    "One or More Permissions are DENIED Exiting App :(",
                    Toast.LENGTH_SHORT
                )
                    .show()
                finish()
            }
        }
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.size > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun askRuntimePermission() {
        val permissionsNeeded: MutableList<String> = ArrayList()
        val permissionsList: MutableList<String> = ArrayList()
        if (!addPermission(
                permissionsList,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) permissionsNeeded.add("Show Location")
        if (permissionsList.size > 0) {
            if (permissionsNeeded.size > 0) {

                // Need Rationale
                var message = "App need access to " + permissionsNeeded[0]
                for (i in 1 until permissionsNeeded.size) message =
                    message + ", " + permissionsNeeded[i]
                showMessageOKCancel(
                    message
                ) { dialog, which ->
                    requestPermissions(
                        permissionsList.toTypedArray(),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
                    )
                }
                return
            }
            requestPermissions(
                permissionsList.toTypedArray(),
                REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS
            )
            return
        }
        Toast.makeText(
            this@WebViewActivity,
            "No new Permission Required- Launching App .You are Awesome!!",
            Toast.LENGTH_SHORT
        )
            .show()
    }

    private fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this@WebViewActivity)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun addPermission(permissionsList: MutableList<String>, permission: String): Boolean {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission)
            // Check for Rationale Option
            if (!shouldShowRequestPermissionRationale(permission)) return false
        }
        return true
    }

    override fun onBackPressed() {
        if (webView!!.copyBackForwardList().currentIndex > 0) {
            webView!!.goBack()
        } else {
            // Your exit alert code, or alternatively line below to finish
            super.onBackPressed() // finishes activity
        }
    }

    companion object {
        private const val INPUT_FILE_REQUEST_CODE = 1
        private const val FILECHOOSER_RESULTCODE = 1
        private val TAG = WebViewActivity::class.java.simpleName
        private const val CAMERA_PERMISSION_CODE = 100
        private const val STORAGE_PERMISSION_CODE = 101
    }
}