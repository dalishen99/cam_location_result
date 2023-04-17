package com.coderivium.p4rcintegrationsample

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class WebMyXRSDK : AppCompatActivity() {
    private val REQUEST_LOCATION = 1
    private var mGeoLocationCallback: GeolocationPermissions.Callback? = null
    private var mGeoLocationRequestOrigin: String? = null

    lateinit var dialog: Dialog
    private lateinit var webView: WebView
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    private val INPUT_FILE_REQUEST_CODE = 1
    private val FILECHOOSER_RESULTCODE = 1
    private var mCapturedImageURI: Uri? = null

    private var mUploadMessage: ValueCallback<Uri>? = null


    private lateinit var refresh: AppCompatImageView

    private lateinit var homeClick: AppCompatImageView
    private lateinit var backClick: AppCompatImageView
    private lateinit var forwardClick: AppCompatImageView
    private lateinit var toolbar: Toolbar
    private var baseUrl: String = "https://myxr-web-stage.kiwi-internal.com/www.google.com"
    private lateinit var webUrl: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_viiew)
        webView = findViewById(R.id.web_view)
        backClick = findViewById(R.id.backClick)
        refresh = findViewById(R.id.refresh)
        homeClick = findViewById(R.id.homeClick)
        forwardClick = findViewById(R.id.forwardClick)
        toolbar = findViewById(R.id.toolbar)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            toolbar.setNavigationOnClickListener({ onBackPressed() })
        }
        setupWebView()
        createProgressDialog();
        loadPaymentWebView(baseUrl)

        backClick.setOnClickListener {
            webView.goBack()
        }
        forwardClick.setOnClickListener {
            webView.goForward()
        }

        refresh.setOnClickListener {
            webView.reload()
        }

        homeClick.setOnClickListener {
            loadPaymentWebView(baseUrl)
        }
    }

    private fun createProgressDialog() {
        val builder = AlertDialog.Builder(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(R.layout.progress)
        }
        dialog = builder.create()
        dialog.setCancelable(false)
        //dialog.window?.decorView?.setBackgroundResource(android.R.color.transparent);
        // dialog.window?.setDimAmount(0.0f)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener(object : DialogInterface.OnKeyListener {
            override fun onKey(dialog: DialogInterface?, keyCode: Int, event: KeyEvent): Boolean {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) return true
                return false
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
           // javaScriptEnabled = true
            setSupportZoom(false)
          //  webView.setInitialScale(170)
            loadWithOverviewMode = true
          builtInZoomControls = true
            javaScriptEnabled = true
            allowFileAccess = true
            javaScriptCanOpenWindowsAutomatically = true
            setGeolocationEnabled(true)
//            changes
            setAppCacheEnabled(true);
           // databaseEnabled = true;
            //domStorageEnabled = true;
//            setSupportMultipleWindows(true)

        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                Log.e("url name", url.toString())

            }


        }

        webView.setDownloadListener({ url, userAgent, contentDisposition, mimeType, contentLength ->

                    val request = DownloadManager.Request(Uri.parse(url))
            Log.e("extentiion type",mimeType);
            Log.e("url",url);
            request.setMimeType(mimeType)
            request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))

            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//            String newString ="THE FILE NAME.mp3"
//            Log.i(ERROR, "this is it " + newString);
//            File sdCard = Environment.getExternalStorageDirectory();
//            String folder = sdCard.getAbsolutePath();
//            request.setDestinationInExternalPublicDir(folder, newString);

            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "b.csv")
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)

        })



        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Log.d("PERMISSION", request.origin.toString());
                    request.grant(request.resources)
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback,
            ) {
                Log.d("TAG", "onGeolocationPermissionsShowPrompt: ")

                runOnUiThread {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@WebMyXRSDK,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ) {
                        Log.d("TAG", "onGeolocationPermissionsShowPrompt: kya ")

                        mGeoLocationRequestOrigin = origin
                        mGeoLocationCallback = callback
                        ActivityCompat.requestPermissions(
                            this@WebMyXRSDK,
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

                    // Do We need to ask for permission?
//                    if (ContextCompat.checkSelfPermission(
//                            this@WebMyXRSDK,
//                            Manifest.permission.ACCESS_FINE_LOCATION
//                        ) != PackageManager.PERMISSION_GRANTED
//                    ) {
//                        Log.d("TAG", "onGeolocationPermissionsShowPrompt: false")
//
//                        mGeoLocationRequestOrigin = origin
//                        mGeoLocationCallback = callback
//                        ActivityCompat.requestPermissions(
//                            this@WebMyXRSDK,
//                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                            REQUEST_LOCATION
//                        )
//
//                        // Should we show an explanation?
//
//                    } else {
//                        // Tell the WebView that permission has been granted
//                        callback.invoke(origin, true, false)
//                    }
                }

            }

            override fun onProgressChanged(view: WebView, progress: Int) {
                //Make the bar disappear after URL is loaded, and changes string to Loading...
                //Make the bar disappear after URL is loaded
                progressDialog(true)
                // Return the app name after finish loading
                if (progress == 100) {
                    progressDialog(false)
                }
            }

            // For Android 5.0
            override fun onShowFileChooser(
                view: WebView,
                filePath: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams,
            ): Boolean {
                // Double check that we don't have any existing callbacks
                mFilePathCallback?.onReceiveValue(null)
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
                        Log.e("TAG", "Unable to create Image File", ex)

                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.absolutePath
                        takePictureIntent!!.putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile)
                        )
                    } else {
                        takePictureIntent = null
                    }
                }
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "*/*"
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

            fun openFileChooser(uploadMsg: ValueCallback<Uri>?, acceptType: String? = "") {
                mUploadMessage = uploadMsg
                val imageStorageDir =
                    File(
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
                i.type = "*/*"
                val mimetypes = arrayOf("image/*", "text/csv")
                val chooserIntent = Intent.createChooser(i, "Image Chooser")
                chooserIntent.putExtra(
                    Intent.EXTRA_INITIAL_INTENTS,
                    arrayOf<Parcelable>(captureIntent)
                )
                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE)
            }

            fun openFileChooser(
                uploadMsg: ValueCallback<Uri?>?,
                acceptType: String?,
                capture: String?,
            ) {
                openFileChooser(uploadMsg, acceptType, capture)
            }
        }

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

    fun progressDialog(show: Boolean) {
        if (show)
            dialog.show()
        else
            dialog.dismiss()
    }

    private fun loadPaymentWebView(url: String) {
        webView.loadUrl(url)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION -> {
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
        }
        // other 'case' lines to check for other
        // permissions this app might request
    }

    override fun onBackPressed() {

        if (webView.copyBackForwardList().getCurrentIndex() > 0) {
            webView.goBack();
        } else {
            // Your exit alert code, or alternatively line below to finish
            super.onBackPressed(); // finishes activity
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
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
}