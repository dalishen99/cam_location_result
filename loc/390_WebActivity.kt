package soft.com.softcommerce.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MenuItem
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import soft.com.softcommerce.R
import soft.com.softcommerce.databinding.ActivityWebBinding
import soft.com.softcommerce.ui.networkManager.NetworkChangeReceiver


class WebActivity : AppCompatActivity(), NetworkChangeReceiver.ConnectionChangeCallback {
    private lateinit var binding: ActivityWebBinding
    private var websiteUrl: String = "https://softcommerceltd.com/"
    private var doubleBackToExitPressedOnce: Boolean = false

    var mGeoLocationRequestOrigin: String? = null
    var mGeoLocationCallback: GeolocationPermissions.Callback? = null
    val maxProgress = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initWebView()
        setWebClient()
        loadUrl(websiteUrl)

        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        val networkChangeReceiver = NetworkChangeReceiver()
        registerReceiver(networkChangeReceiver, intentFilter)
        networkChangeReceiver.connectionChangeCallback = this@WebActivity

    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        // settings.
        //   val settings = binding.webView.settings
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.databaseEnabled = true
        binding.webView.settings.setAppCacheEnabled(true)
        binding.webView.webViewClient = object : WebViewClient() {
            override
            fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }
        }

    }

    private fun setWebClient() {
        if (isNetworkAvailable()) {
            binding.webView.webChromeClient = object : WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {

                    if (ContextCompat.checkSelfPermission(
                            this@WebActivity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        != PackageManager.PERMISSION_GRANTED
                    ) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                this@WebActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        ) {
                            AlertDialog.Builder(this@WebActivity)
                                .setMessage("Please turn ON the GPS to make app work smoothly")
                                .setNeutralButton(
                                    android.R.string.ok,
                                    DialogInterface.OnClickListener { dialogInterface, i ->
                                        mGeoLocationCallback = callback
                                        mGeoLocationRequestOrigin = origin
                                        ActivityCompat.requestPermissions(
                                            this@WebActivity,
                                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001
                                        )

                                    })
                                .show()

                        } else {
                            //no explanation need we can request the locatio
                            mGeoLocationCallback = callback
                            mGeoLocationRequestOrigin = origin
                            ActivityCompat.requestPermissions(
                                this@WebActivity,
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001
                            )
                        }
                    } else {
                        //tell the webview that permission has granted
                        callback!!.invoke(origin, true, true)
                    }

                }

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    binding.progressBar.progress = newProgress
                    if (newProgress < maxProgress && binding.progressBar.visibility == ProgressBar.GONE) {
                        binding.progressBar.visibility = ProgressBar.VISIBLE
                    }
                    if (newProgress == maxProgress) {
                        binding.progressBar.visibility = ProgressBar.GONE
                    }
                }


            }
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
/*        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        }
        // If it wasn't the Back key or there's no web page history, exit the activity)
        return super.onKeyDown(keyCode, event)*/

        if (event?.action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    showToastToExit()
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)

    }

    private fun loadUrl(pageUrl: String) {
        binding.webView.loadUrl(pageUrl)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1001 -> {
                //if permission is cancel result array would be empty
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission was granted
                    if (mGeoLocationCallback != null) {
                        mGeoLocationCallback!!.invoke(mGeoLocationRequestOrigin, true, true)
                    }
                } else {
                    //permission denied
                    if (mGeoLocationCallback != null) {
                        mGeoLocationCallback!!.invoke(mGeoLocationRequestOrigin, false, false)
                    }
                }
            }

        }
    }


    private fun showToastToExit() {
        when {
            doubleBackToExitPressedOnce -> {
                onBackPressed()
            }
            else -> {
                doubleBackToExitPressedOnce = true
                Toast.makeText(this, getString(R.string.back_again_to_exit), Toast.LENGTH_LONG)
                    .show()

                Handler(Looper.myLooper()!!).postDelayed({
                    doubleBackToExitPressedOnce = false

                }, 2000)

                // Handler().postDelayed({doubleBackToExitPressedOnce = false},2000)
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {

        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                        return true
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        return true
                    }
                }
            }
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected) {
                return true
            }
        }
        return false
    }

    override fun onNavigateUp(): Boolean {
        finish()
        return super.onNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        onBackPressed()
        return super.onOptionsItemSelected(item)
    }

    override fun onConnectionChange(isConnected: Boolean) {
        if (isConnected) {
            // will be called when internet is back
            Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show()
            binding.webView.reload()
        } else {
            // will be called when internet is gone.
            Toast.makeText(this, "No Internet", Toast.LENGTH_LONG).show()

            //Snackbar(view)
            val snackbar = Snackbar.make(window.decorView.findViewById(R.id.webView), "Check your Internet Connection",
                Snackbar.LENGTH_INDEFINITE)
            snackbar.setAction("Dismiss" ){

            }
            snackbar.setActionTextColor(Color.RED)
          val snackbarView = snackbar.view
            //snackbarView.setBackgroundColor(Color.LTGRAY)
            val textView =
                snackbarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
            textView.setTextColor(Color.WHITE)
//            textView.textSize = 28f
            snackbar.show()
        }
    }
}

