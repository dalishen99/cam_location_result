package com.example.tastee.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.*
import android.webkit.*
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.example.tastee.Classes.GpsUtils
import com.example.tastee.Classes.GpsUtils.onGpsListener
import com.example.tastee.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEventListener
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity2 : AppCompatActivity() {
    private lateinit var webView: WebView
    var GPSMutableData: MutableLiveData<Boolean> = MutableLiveData()


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        overridePendingTransition(0,0)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        userbottomNavigationView = findViewById(R.id.userbottomNavigationView)
        progressbar = findViewById(R.id.progressBar)
        root = findViewById(R.id.rootView)

        webView = findViewById(R.id.webView)
        webView.loadUrl("https://tastee.inspeero.com")
        webView.webViewClient = webViewClient()
        webView.webChromeClient = webViewChromeClient()
        webView.settings.javaScriptEnabled = true
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.setAppCacheEnabled(true)
        webView.settings.javaScriptCanOpenWindowsAutomatically = true

        if (Build.VERSION.SDK_INT >= 21) {
            webView.settings.mixedContentMode = 0
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        } else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }


        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)


    }

    private fun showAccessLocationDialogue() {
        val dialog= Dialog(this)
        dialog.setContentView(R.layout.dialogue_box_access_location)
        dialog.window!!.setBackgroundDrawable(getDrawable(R.drawable.dialogue_bg))
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        dialog.show()

        val btn_allow=dialog.findViewById<RelativeLayout>(R.id.btn_allow)
        btn_allow.setOnClickListener{
            dialog.dismiss()
            requestLocationPermission()
        }
    }

    private fun showNoInternetDialogue() {
        val dialog= Dialog(this)
        dialog.setContentView(R.layout.dialogue_box_no_internet)
        dialog.window!!.setBackgroundDrawable(getDrawable(R.drawable.dialogue_bg))
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        dialog.show()

        val btn_ok=dialog.findViewById<RelativeLayout>(R.id.btn_ok)
        btn_ok.setOnClickListener{
            dialog.dismiss()
            finish()
        }
    }

    private fun showFailedToLoadDialogue() {
        val dialog= Dialog(this)
        dialog.setContentView(R.layout.dialogue_box_failed_to_load)
        dialog.window!!.setBackgroundDrawable(getDrawable(R.drawable.dialogue_bg))
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        dialog.show()

        val btn_ok=dialog.findViewById<RelativeLayout>(R.id.btn_ok)
        btn_ok.setOnClickListener{
            dialog.dismiss()
            finish()
        }
    }

    private fun showAllowCameraDialogue() {
        val dialog= Dialog(this)
        dialog.setContentView(R.layout.dialogue_box_access_camera)
        dialog.window!!.setBackgroundDrawable(getDrawable(R.drawable.dialogue_bg))
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        dialog.show()

        val btn_allow=dialog.findViewById<RelativeLayout>(R.id.btn_allow)
        btn_allow.setOnClickListener{
            dialog.dismiss()
            requestCameraPermission()
        }
    }

    private fun showAllowStoragePermission() {
        val dialog= Dialog(this)
        dialog.setContentView(R.layout.dialogue_box_access_storage)
        dialog.window!!.setBackgroundDrawable(getDrawable(R.drawable.dialogue_bg))
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        dialog.show()

        val btn_allow=dialog.findViewById<RelativeLayout>(R.id.btn_allow)
        btn_allow.setOnClickListener{
            dialog.dismiss()
            requestReadWriteExternalStoragePermission()
        }
    }

    private fun showAllowGPSDialogue() {
        val dialog= Dialog(this)
        dialog.setContentView(R.layout.dialogue_box_access_gps)
        dialog.window!!.setBackgroundDrawable(getDrawable(R.drawable.dialogue_bg))
        dialog.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)
        dialog.show()

        val btn_ok=dialog.findViewById<RelativeLayout>(R.id.btn_ok)
        btn_ok.setOnClickListener{
            dialog.dismiss()
            GpsUtils(this@MainActivity2).turnGPSOn(object : onGpsListener {
                override fun gpsStatus(isGPSEnable: Boolean) {
                    // turn on GPS
                    isGPSEnabled = isGPSEnable
                    if(isGPSEnabled){
                        if(mGeolocationCallback!=null) {
                            mGeolocationCallback!!.invoke(mGeolocationOrigin, true, true)
                        }
                    }
                    else{
                        showAllowGPSDialogue()
                    }
                }
            })

        }
    }

    private fun isConnectedToInternet(mainActivity: MainActivity2): Boolean {
        val connectivityManager =
            mainActivity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wificon = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        val mobilecon = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
        return wificon != null && wificon.isConnected || mobilecon != null && mobilecon.isConnected
    }

    companion object {
        private var mGeolocationOrigin: String? = null
        private var mGeolocationCallback: GeolocationPermissions.Callback? = null
        var userbottomNavigationView: BottomNavigationView? = null
        var progressbar: RelativeLayout? = null
        private var mUploadMessage: ValueCallback<Uri>? = null
        private var mCapturedImageURI: Uri? = null
        private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
        private var mCameraPhotoPath: String? = null
        private const val INPUT_FILE_REQUEST_CODE = 1
        private const val FINE_ACCESS_LOCATION = 5
        private const val READ_WRITE_STORAGE_ACCESS_LOCATION = 2
        private const val GPS_REQUEST_CODE = 3
        private const val CAMERA_ACCESS_LOCATION = 4

        var isGPSEnabled=false

        var cameradialog:Dialog?=null
        var storagedialog:Dialog?=null

        var currentUrl: String? = null
        var root: ConstraintLayout? = null

    }


    inner class webViewChromeClient : WebChromeClient() {
        // For Android 5.0
        @SuppressLint("QueryPermissionsNeeded")
        @RequiresApi(Build.VERSION_CODES.N)
        override fun onShowFileChooser(
            view: WebView?,
            filePath: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            if (hasPermission(Manifest.permission.CAMERA)
            ) {
                if(hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.d("tag", "showFileChooser")
                    mFilePathCallback?.onReceiveValue(null)
                    mFilePathCallback = filePath
                    var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    try {
                        if (takePictureIntent!!.resolveActivity(packageManager) != null) {
                            var photoFile: File? = null

                            photoFile = createImageFile()
                            takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)


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
                        contentSelectionIntent.type = "image/*"
                        val intentArray: Array<Intent?> =
                            takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls(0)
                        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Continue action using")
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
                    } catch (e: IOException) {
                        //Log.d("tag", "Unable to create Image File", e)
                    }
                }
                else{

                        showAllowStoragePermission()

                }
            }
            else{

                    showAllowCameraDialogue()

            }
            return true
        }


        // openFileChooser for Android 3.0+
        fun openFileChooser(uploadMsg: ValueCallback<Uri>?, acceptType: String?) {
            if (hasPermission(Manifest.permission.CAMERA)
            ) {
                if(hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) && hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.d("tag", "openFileChooser")
                    mUploadMessage = uploadMsg
                    val imageStorageDir = File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                        ), "AndroidExampleFolder"
                    )
                    if (!imageStorageDir.exists()) {
                        imageStorageDir.mkdirs()
                    }

                    val file = File(
                        imageStorageDir.toString() + File.separator + "IMG_"
                                + System.currentTimeMillis().toString() + ".jpg"
                    )
                    mCapturedImageURI = Uri.fromFile(file)

                    val captureIntent = Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE
                    )
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
                    val i = Intent(Intent.ACTION_GET_CONTENT)
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    i.type = "image/*"

                    val chooserIntent = Intent.createChooser(i, "Continue action using")

                    chooserIntent.putExtra(
                        Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(captureIntent)
                    )

                    startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
                }
                else{

                        showAllowStoragePermission()

                }
            }
            else{

                    showAllowCameraDialogue()

            }
        }

        // openFileChooser for Android < 3.0
        fun openFileChooser(uploadMsg: ValueCallback<Uri>?) {


            openFileChooser(uploadMsg, "")

        }

        //openFileChooser for other Android versions
        fun openFileChooser(
            uploadMsg: ValueCallback<Uri>?,
            acceptType: String?,
            capture: String?
        ) {

            openFileChooser(uploadMsg, acceptType)

        }


        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            mGeolocationCallback=callback
            mGeolocationOrigin=origin
        }

    }


    private fun requestReadWriteExternalStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE),READ_WRITE_STORAGE_ACCESS_LOCATION)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),CAMERA_ACCESS_LOCATION)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            FINE_ACCESS_LOCATION)
    }


    private fun hasPermission(id: String): Boolean {
        return ActivityCompat.checkSelfPermission(this@MainActivity2,id)==PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.N)
    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        return File.createTempFile(
            imageFileName, ".jpg", storageDir
        )
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_ACCESS_LOCATION -> {
                if(wasPermissionGranted(grantResults)){
                    Log.d("tag", "Permission Granted")
                }
                else{
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity2,
                            Manifest.permission.CAMERA
                        )
                    ){

                            showAllowCameraDialogue()

                    }
                    else {
                        val i = Intent()
                        i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        i.addCategory(Intent.CATEGORY_DEFAULT)
                        i.data = Uri.parse("package:" + applicationContext.packageName)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        applicationContext.startActivity(i)
                    }
                }
            }

            READ_WRITE_STORAGE_ACCESS_LOCATION -> {
                if(wasPermissionGranted(grantResults)){
                    Log.d("tag", "Permission Granted")
                }
                else{
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity2,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) || ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity2,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    ){

                            showAllowStoragePermission()

                    }
                    else {
                        val i = Intent()
                        i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        i.addCategory(Intent.CATEGORY_DEFAULT)
                        i.data = Uri.parse("package:" + applicationContext.packageName)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        applicationContext.startActivity(i)
                    }
                }
            }

            FINE_ACCESS_LOCATION -> {
                if(wasPermissionGranted(grantResults)){
                    if(isGPSEnabled) {
                        if (mGeolocationCallback != null) {
                            mGeolocationCallback!!.invoke(mGeolocationOrigin, true, false)
                        }
                    }
                    else{
                        showAllowGPSDialogue()
                    }
                }
                else{
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@MainActivity2,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    ){

                            showAccessLocationDialogue()

                    }
                    else {
                        val i = Intent()
                        i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        i.addCategory(Intent.CATEGORY_DEFAULT)
                        i.data = Uri.parse("package:" + applicationContext.packageName)
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        applicationContext.startActivity(i)
                    }
                }
            }
        }
    }

    private fun wasPermissionGranted(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() && grantResults[0] ==PackageManager.PERMISSION_GRANTED
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == INPUT_FILE_REQUEST_CODE
            || mFilePathCallback != null
        ) {
            var results: Array<Uri>? = null
            if (resultCode == RESULT_OK) {
                if (data == null) {
                    if (mCameraPhotoPath != null) { //if there is not data here, then we may have taken a photo/video
                        results = arrayOf(Uri.parse(mCameraPhotoPath))
                    }
                } else {
                    val dataString: String = data.dataString!!
                    results = arrayOf(Uri.parse(dataString))
                }
            }
            mFilePathCallback!!.onReceiveValue(results)
            mFilePathCallback = null
        }

        if (requestCode == GPS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                isGPSEnabled=true
            }
        }

    }

    inner class webViewClient : WebViewClient() {
        @SuppressLint("JavascriptInterface")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {

            return if (url.contains("mailto:")) {
                view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                true
            } else {
                view.loadUrl(url)
                true
            }

        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (!isConnectedToInternet(this@MainActivity2)) {
                progressbar!!.visibility = View.GONE
                showNoInternetDialogue()
            }
            else{
                progressbar!!.visibility = View.VISIBLE
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            progressbar!!.visibility = View.GONE
        }

        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            showFailedToLoadDialogue()
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            when (url!!) {
                //ForAll
                "https://tastee.inspeero.com/choose" -> {
                    webView.visibility=View.GONE
                    userbottomNavigationView!!.visibility = View.GONE
                    webView.loadUrl("https://tastee.inspeero.com/login")
                    currentUrl="https://tastee.inspeero.com/choose"
                    KeyboardVisibilityEvent.setEventListener(this@MainActivity2,object:
                        KeyboardVisibilityEventListener {
                        override fun onVisibilityChanged(isOpen: Boolean) {
                            if(isOpen){
                                userbottomNavigationView!!.visibility=View.GONE
                            }
                            else{
                                userbottomNavigationView!!.visibility=View.GONE
                            }
                        }

                    })
                }

                "https://tastee.inspeero.com/login" -> {
                    webView.visibility=View.VISIBLE
                    userbottomNavigationView!!.visibility = View.GONE
                    currentUrl="https://tastee.inspeero.com/login"
                    KeyboardVisibilityEvent.setEventListener(this@MainActivity2,object:
                        KeyboardVisibilityEventListener {
                        override fun onVisibilityChanged(isOpen: Boolean) {
                            if(isOpen){
                                userbottomNavigationView!!.visibility=View.GONE
                            }
                            else{
                                userbottomNavigationView!!.visibility=View.GONE
                            }
                        }

                    })
                }


                //ForUser
                "https://tastee.inspeero.com/home" -> {
                    currentUrl="https://tastee.inspeero.com/home"


                    if (!hasPermission(Manifest.permission.CAMERA))
                    {

                            showAllowCameraDialogue()

                    }
                    if(!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) || !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){

                            showAllowStoragePermission()

                    }
                    if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        isGPSEnabled()
                        if (isGPSEnabled) {
                            if(mGeolocationCallback!=null) {
                                mGeolocationCallback!!.invoke(mGeolocationOrigin, true, false)
                            }
                        }
                        else{
                            showAllowGPSDialogue()
                        }
                    } else {

                        showAccessLocationDialogue()

                    }


                    webView.visibility=View.VISIBLE
                    userbottomNavigationView!!.visibility = View.VISIBLE
                    if (userbottomNavigationView!!.selectedItemId != R.id.ic_home) {
                        userbottomNavigationView!!.selectedItemId = R.id.ic_home
                    }
                    userbottomNavigationView!!.setOnItemSelectedListener { item ->
                        when (item.itemId) {
                            R.id.ic_home -> {
                                webView.loadUrl("https://tastee.inspeero.com/home")
                                true
                            }
                            R.id.ic_prom -> {
                                webView.loadUrl("https://tastee.inspeero.com/followedbyme")
                                true
                            }
                            R.id.ic_msg -> {
                                webView.loadUrl("https://tastee.inspeero.com/messages/null")
                                true
                            }
                            R.id.ic_feedback -> {
                                webView.loadUrl("https://tastee.inspeero.com/feedback")
                                true
                            }
                            else -> {
                                false
                            }
                        }

                    }

                    KeyboardVisibilityEvent.setEventListener(this@MainActivity2,object:
                        KeyboardVisibilityEventListener {
                        override fun onVisibilityChanged(isOpen: Boolean) {
                            if(isOpen){
                                userbottomNavigationView!!.visibility=View.GONE
                            }
                            else{
                                userbottomNavigationView!!.visibility=View.VISIBLE
                            }
                        }

                    })

                }

                "https://tastee.inspeero.com/followedbyme" -> {
                    currentUrl="https://tastee.inspeero.com/followedbyme"

                    webView.visibility=View.VISIBLE
                    if (userbottomNavigationView!!.selectedItemId != R.id.ic_prom) {
                        userbottomNavigationView!!.selectedItemId = R.id.ic_prom
                    }
                    KeyboardVisibilityEvent.setEventListener(this@MainActivity2,object:
                        KeyboardVisibilityEventListener {
                        override fun onVisibilityChanged(isOpen: Boolean) {
                            if(isOpen){
                                userbottomNavigationView!!.visibility=View.GONE
                            }
                            else{
                                userbottomNavigationView!!.visibility=View.VISIBLE
                            }
                        }

                    })
                }

                "https://tastee.inspeero.com/messages/null" -> {
                    currentUrl="https://tastee.inspeero.com/messages/null"

                    webView.visibility=View.VISIBLE
                    if (userbottomNavigationView!!.selectedItemId != R.id.ic_msg) {
                        userbottomNavigationView!!.selectedItemId = R.id.ic_msg
                    }
                    KeyboardVisibilityEvent.setEventListener(this@MainActivity2,object:
                        KeyboardVisibilityEventListener {
                        override fun onVisibilityChanged(isOpen: Boolean) {
                            if(isOpen){
                                userbottomNavigationView!!.visibility=View.GONE
                            }
                            else{
                                userbottomNavigationView!!.visibility=View.VISIBLE
                            }
                        }

                    })
                }

                "https://tastee.inspeero.com/feedback" -> {
                    currentUrl="https://tastee.inspeero.com/feedback"

                    webView.visibility=View.VISIBLE
                    if (userbottomNavigationView!!.selectedItemId != R.id.ic_feedback) {
                        userbottomNavigationView!!.selectedItemId = R.id.ic_feedback
                    }
                    KeyboardVisibilityEvent.setEventListener(this@MainActivity2,object:
                        KeyboardVisibilityEventListener {
                        override fun onVisibilityChanged(isOpen: Boolean) {
                            if(isOpen){
                                userbottomNavigationView!!.visibility=View.GONE
                            }
                            else{
                                userbottomNavigationView!!.visibility=View.VISIBLE
                            }
                        }

                    })

                }
                else ->{
                    webView.visibility= View.VISIBLE
                    userbottomNavigationView!!.selectedItemId = R.id.invisible
                    KeyboardVisibilityEvent.setEventListener(this@MainActivity2,object:
                        KeyboardVisibilityEventListener {
                        override fun onVisibilityChanged(isOpen: Boolean) {
                            if(isOpen){
                                userbottomNavigationView!!.visibility=View.GONE
                            }
                            else{
                                userbottomNavigationView!!.visibility=View.VISIBLE
                            }
                        }

                    })
                }
            }

        }

    }


    override fun onRestart() {
        super.onRestart()
        Log.d("tag", "Restart")
        if(currentUrl=="https://tastee.inspeero.com/home")
        {
            if (!hasPermission(Manifest.permission.CAMERA))
            {
                if(cameradialog!=null) {
                    if (!cameradialog!!.isShowing) {
                        showAllowCameraDialogue()
                    }
                }
            }
            if(!hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) || !hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                if(storagedialog!=null) {
                    if (!storagedialog!!.isShowing) {
                        showAllowStoragePermission()
                    }
                }

            }
            if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (isGPSEnabled) {
                    if(mGeolocationCallback!=null) {
                        mGeolocationCallback!!.invoke(mGeolocationOrigin, true, false)
                    }
                }
                else{
                    showAllowGPSDialogue()
                }
            } else {
                showAccessLocationDialogue()
            }

        }
    }




    private fun isGPSEnabled() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val providerEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        isGPSEnabled=providerEnable
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            if (currentUrl == "https://tastee.inspeero.com/login" || currentUrl == "https://tastee.inspeero.com/home") {
                onBackPressed()
            } else {
                webView.goBack()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

}