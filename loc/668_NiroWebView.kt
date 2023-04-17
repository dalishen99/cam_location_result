package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import java.io.File
import android.location.LocationManager;
import android.webkit.GeolocationPermissions

import android.webkit.WebChromeClient








open class NiroWebView : AppCompatActivity(), Handler.Callback, LocationListener {
    private lateinit var mContext: Context
    internal var mLoaded = false

    // set your custom url here
    internal var URL = "https://dev.niro.money/location-poc/"

    private lateinit var location: LocationManager

    //AdView adView;
    private lateinit var btnTryAgain: Button
    private lateinit var tvLastMessage: TextView
    private lateinit var mWebView: WebView
    private lateinit var prgs: ProgressBar
    private var viewSplash: View? = null
    private lateinit var layoutSplash: RelativeLayout
    private lateinit var layoutWebview: RelativeLayout
    private lateinit var layoutNoInternet: RelativeLayout
    private val handler by lazy { Handler(this) }

    var isMockedLocation: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_niro_web_view)

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        mContext = this
        location = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        initViews()
        addClickListeners()
    }

    private fun getLocation() {
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 99)
        }
        location.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 5f, this)
    }
    private fun loadExternalJavaScript() {

        mWebView.loadUrl(
            "javascript:(function f() {<script>  var x = document.createElement(\\\"BUTTON\\\");\\n\" +\n" +
                    "                \"  var t = document.createTextNode(\\\"Ask Permission\\\");\\n\" +\n" +
                    "                \"  x.appendChild(t); alert('ww'); \\n\" +\n" +
                    "                \"  document.body.appendChild(x); </script> } )()"
        )

    }

    private fun addClickListeners() {
        mWebView.addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun performClick(string: String) {
                println("performClick>>>>> $string ")

                when(string) {
                    "read_sms" -> {
                        if (checkByPermissionName(
                                Manifest.permission.RECEIVE_SMS,
                                MY_PERMISSIONS_REQUEST_SMS
                            )
                        ) {
                            setSuccessMessage("We can read SMS now.")
                        } else {
                            setSuccessMessage("SMS PERMISSION NOT GRANTED.")
                        }
                    }
                    "capture_location" -> {
                        if (checkByPermissionName(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                MY_PERMISSIONS_REQUEST_LOCATION
                            )
                        ) {
                            setSuccessMessage("Location PERMISSION SUCCESS")
                        } else {
                            setSuccessMessage("LOCATION PERMISSION NOT GRANTED.")
                        }
                    }
                    "check_location_spoofing" -> {
                        getLocation()
                        if (isMockedLocation) {
                            setSuccessMessage("LOCATION SPOOFING ENABLED")
                        } else {
                            setSuccessMessage("LOCATION SPOOFING DISABLED")

                        }
                    }
                    "check_phone_rooted" -> {
                        if (isRooted()) {
                            setSuccessMessage("PHONE IS ROOTED")
                        } else {
                            setSuccessMessage("PHONE IS NOT ROOTED")
                        }
                    }
                }
            }
        }, "NiroWebChannel")
    }

    private fun isRooted(): Boolean {
        return findBinary("su")
    }


    open fun findBinary(binaryName: String): Boolean {
        var found = false
        if (!found) {
            val places = arrayOf(
                "/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
                "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"
            )
            for (where in places) {
                if (File(where + binaryName).exists()) {
                    found = true
                    break
                }
            }
        }
        return found
    }

    private fun setSuccessMessage(message: String) {
        mWebView.post {
            mWebView.evaluateJavascript(
                "document.getElementById('searchInput').value='$message'"
            ) {
                Log.d("LogName", it!!) // Prints asd
            }
        }
    }

    private fun initViews() {
        mWebView = findViewById<View>(R.id.webview) as WebView
        prgs = findViewById<View>(R.id.progressBar) as ProgressBar
        btnTryAgain = findViewById<View>(R.id.btn_try_again) as Button
        viewSplash = findViewById(R.id.view_splash)
        tvLastMessage = findViewById(R.id.tvLastMessage)
        layoutWebview = findViewById<View>(R.id.layout_webview) as RelativeLayout
        layoutNoInternet = findViewById<View>(R.id.layout_no_internet) as RelativeLayout
        /** Layout of Splash screen View  */
        layoutSplash = findViewById<View>(R.id.layout_splash) as RelativeLayout

        val preference = getSharedPreferences("MyPref", MODE_PRIVATE)
        tvLastMessage.text = preference.getString("messageBody", "Not found")

        //request for show website
        requestForWebview()

        btnTryAgain.setOnClickListener {
            mWebView.visibility = View.GONE
            prgs.visibility = View.VISIBLE
            layoutSplash.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            requestForWebview()
        }

        mWebView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)
            }
        }
    }

    private fun requestForWebview() {

        if (!mLoaded) {
            requestWebView()
            Handler().postDelayed({
                prgs.visibility = View.VISIBLE
                //viewSplash.getBackground().setAlpha(145);
                mWebView.visibility = View.VISIBLE
            }, 3000)

        } else {
            mWebView.visibility = View.VISIBLE
            prgs.visibility = View.GONE
            layoutSplash.visibility = View.GONE
            layoutNoInternet.visibility = View.GONE
        }

    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun requestWebView() {
        /** Layout of webview screen View  */
        if (internetCheck(mContext)) {
            mWebView.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            mWebView.loadUrl(URL)
        } else {
            prgs.visibility = View.GONE
            mWebView.visibility = View.GONE
            layoutSplash.visibility = View.GONE
            layoutNoInternet.visibility = View.VISIBLE

            return
        }
        mWebView.isFocusable = true
        mWebView.isFocusableInTouchMode = true
        mWebView.settings.javaScriptEnabled = true
        mWebView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        mWebView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        mWebView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        mWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        mWebView.settings.domStorageEnabled = true
        mWebView.settings.setAppCacheEnabled(true)
        mWebView.settings.databaseEnabled = true
        //mWebView.getSettings().setDatabasePath(
        //        this.getFilesDir().getPath() + this.getPackageName() + "/databases/");

        // this force use chromeWebClient
        mWebView.settings.setSupportMultipleWindows(false)
        mWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {

                Log.d(TAG, "URL: " + url!!)
                if (internetCheck(mContext)) {
                    // If you wnat to open url inside then use
                    view.loadUrl(url);

                    // if you wanna open outside of app
                    /*if (url.contains(URL)) {
                        view.loadUrl(url)
                        return false
                    }else {
                        // Otherwise, give the default behavior (open in browser)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    }*/
                } else {
                    prgs.visibility = View.GONE
                    mWebView.visibility = View.GONE
                    layoutSplash.visibility = View.GONE
                    layoutNoInternet.visibility = View.VISIBLE
                }

                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (prgs.visibility == View.GONE) {
                    prgs.visibility = View.VISIBLE
                }
            }

            override fun onLoadResource(view: WebView, url: String) {
                super.onLoadResource(view, url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                onPageFinished()
                mLoaded = true
                if (prgs.visibility == View.VISIBLE)
                    prgs.visibility = View.GONE

                // check if layoutSplash is still there, get it away!
                Handler().postDelayed({
                    layoutSplash.visibility = View.GONE
                    //viewSplash.getBackground().setAlpha(255);
                }, 2000)
            }
        }

    }

    open fun onPageFinished() {
    }

    open fun isMockSettingsON(context: Context): Boolean {
        // returns true if mock location enabled, false if not enabled.
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ALLOW_MOCK_LOCATION
        ) != "0"
    }

    open fun areThereMockPermissionApps(context: Context): Boolean {
        var count = 0
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (applicationInfo in packages) {
            try {
                val packageInfo = pm.getPackageInfo(
                    applicationInfo.packageName,
                    PackageManager.GET_PERMISSIONS
                )

                // Get Permissions
                val requestedPermissions = packageInfo.requestedPermissions
                if (requestedPermissions != null) {
                    for (i in requestedPermissions.indices) {
                        if ((requestedPermissions[i]
                                    == "android.permission.ACCESS_MOCK_LOCATION") && applicationInfo.packageName != context.packageName
                        ) {
                            count++
                        }
                    }
                }
            } catch (e: NameNotFoundException) {
                Log.e("Got exception ", e.message!!)
            }
        }
        return count > 0
    }

    companion object {
        internal var TAG = "---MainActivity"

        //for security
        @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
        fun generateKey(): SecretKey {
            val random = SecureRandom()
            val key = byteArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0)
            //random.nextBytes(key);
            return SecretKeySpec(key, "AES")
        }


        fun internetCheck(context: Context): Boolean {
            var available = false
            val connectivity =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (connectivity != null) {
                val networkInfo = connectivity.allNetworkInfo
                if (networkInfo != null) {
                    for (i in networkInfo.indices) {
                        if (networkInfo[i].state == NetworkInfo.State.CONNECTED) {
                            available = true
                            break
                        }
                    }
                }
            }
            return available
        }
    }


    val MY_PERMISSIONS_REQUEST_LOCATION = 99
    val MY_PERMISSIONS_REQUEST_SMS = 98

    open fun checkByPermissionName(permission: String, requestCode: Int): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                permission
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    permission
                )
            ) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                AlertDialog.Builder(this)
                    .setTitle(permission)
                    .setCancelable(false)
                    .setMessage("We need your ${permission.substring(permission.lastIndexOf("."))} to access the required data.")
                    .setPositiveButton(
                        "Okay"
                    ) { _, _ -> //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(
                            this@NiroWebView,
                            arrayOf(permission),
                            requestCode
                        )
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this, arrayOf(permission),
                    requestCode
                )
            }
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        setSuccessMessage("LOCATION PERMISSION SUCCESS")
                    }
                } else {

                    openAppSettingsAlert()
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
            MY_PERMISSIONS_REQUEST_SMS -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECEIVE_SMS
                        )
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        println("#Permission Success >>>> ")
                        setSuccessMessage("SMS PERMISSION SUCCESS")
                    }
                } else {

                    openAppSettingsAlert()
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun openAppSettingsAlert() {
        AlertDialog.Builder(this)
            .setTitle("App Settings")
            .setCancelable(false)
            .setMessage("We need your permission to access the location.")
            .setPositiveButton(
                "Open Settings"
            ) { _, _ -> //Prompt the user once explanation has been shown
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:com.android.webpoc")
                    )
                )
            }
            .create()
            .show()
    }

    override fun handleMessage(p0: Message): Boolean {
        mWebView.loadUrl("javascript:document.getElementById('searchInput').value = \"Permission Found\"");
        return true
    }

    override fun onLocationChanged(p0: Location) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            println("onLocationChanged >>>1 ${p0.isMock}")
            isMockedLocation = p0.isMock
        } else {
            println("onLocationChanged >>>2 ${p0.isFromMockProvider}")
            isMockedLocation = p0.isFromMockProvider
        }
    }
}