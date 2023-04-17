package at.lw1.kurzparkzonen.wien

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat




private class LocalContentWebViewClient(
    private val assetLoader: WebViewAssetLoader,
    private val mainActivity: MainActivity
) :
    WebViewClientCompat() {
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val intercepted = assetLoader.shouldInterceptRequest(request.url)
        if (request.url.toString().endsWith("js")) {
            intercepted?.mimeType = "text/javascript"
        }
        return intercepted;
    }


    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        if (url.contains("appassets.androidplatform.net")) {
            view.loadUrl(url)
            return false
        } else {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            mainActivity.startActivity(i)
            return true
        }
    }
}


class MainActivity : AppCompatActivity() {
    private val MY_PERMISSIONS_REQUEST_LOCATION: Int = 1234;
    private var mGeoLocationRequestOrigin: String? = null
    private var mGeoLocationCallback: GeolocationPermissions.Callback? = null
    private var rootView: CoordinatorLayout? = null
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val crash_reports = sharedPref.getBoolean("crash_reports", false)
        var userAgent = System.getProperty("http.agent")
        if (userAgent == null) {
            userAgent = "Android"
        }
        userAgent += " Kurzparkzonen"
        if (crash_reports) {
            Log.i("Kurzparkzonen", "crash reports are enabled")
        } else {
            Log.i("Kurzparkzonen", "crash reports are disabled")
            userAgent += " PrivateMode"
        }
        if (Build.VERSION.SDK_INT < 30) {
            Log.i("Kurzparkzonen", "detected older Android version")
            userAgent += " OlderAndroid"
        }
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.activity_main_webview)
        rootView = findViewById(R.id.action_bar_root)
        val webView = webView
        if (webView !== null) {
            WebView.setWebContentsDebuggingEnabled(true)
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
//                .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(this))
                .build()
            webView.webViewClient = LocalContentWebViewClient(assetLoader,this)
            webView.settings.builtInZoomControls = false
            // Below required for geolocation
            webView.settings.javaScriptEnabled = true
            webView.settings.allowUniversalAccessFromFileURLs = true
            webView.settings.setGeolocationEnabled(true)
            webView.webChromeClient = GeoWebChromeClient()
            webView.settings.databaseEnabled = true
            webView.settings.domStorageEnabled = true
            webView.settings.userAgentString = userAgent
            webView.settings.setGeolocationDatabasePath(filesDir.path)
            webView.loadUrl("https://appassets.androidplatform.net/dist/index.html")
        }
    }

    /**
     * WebChromeClient subclass handles UI-related calls
     * Note: think chrome as in decoration, not the Chrome browser
     */
    inner class GeoWebChromeClient : WebChromeClient() {

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
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
                        .setMessage(R.string.location_permission_reason)
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
                callback.invoke(origin, true, false)
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
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
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar items
        return when (item.itemId) {
            R.id.to_preferences -> {
                val i = Intent(this, MySettingsActivity::class.java)
                startActivity(i)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Log.i("BackButton", "pressed")
        Log.i("BackButton", webView.toString())
        webView!!.evaluateJavascript("closePopup();") { returnString: String ->
            if (returnString == "false") {
                Log.i("BackButton", "no popup was open")
                super.onBackPressed()
            } else {
                Log.i("BackButton", "popup was closed")
            }
        }
    }
}