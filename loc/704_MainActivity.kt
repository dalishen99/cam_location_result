package com.example.presskg


import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.presskg.MyWebViewClient.ViewCallback
import com.example.presskg.databinding.ActivityMainBinding
import com.example.presskg.utils.Objects.isNetworkAvailable
import com.google.android.gms.ads.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var uploadMessage: ValueCallback<Array<Uri>>? = null
    private val url = "https://presskg.com/gezit/"
    private var mAdapter: BannerAdapter? = null


    lateinit var mAdView: AdView
    //private var mInterstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setWebView()
        checkNetworkAndGps(this@MainActivity)

//        val strings: MutableList<String> = ArrayList()
//        strings.add(AdRequest.DEVICE_ID_EMULATOR)
//        strings.add("70DB16CE52EF1A7A34EA28790F9AB60F")
//        MobileAds.setRequestConfiguration(
//            RequestConfiguration.Builder()
//                .setTestDeviceIds(strings)
//                .build()
//        )

        adMob()
        mAdapter = BannerAdapter(this, getString(R.string.admob_inner))
        mAdapter!!.loadNewBanner(this)
    }


    private fun adMob() {
        //banner
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.adListener = object : AdListener() {

            override fun onAdClosed() {}

            override fun onAdFailedToLoad(var1: LoadAdError) {
                Log.d("@@@adView", ": " + var1.toString())
            }

            override fun onAdImpression() {}

            override fun onAdLoaded() {}

            override fun onAdOpened() {}
        }
        mAdView.loadAd(adRequest)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setWebView() {
        binding.web.settings.defaultTextEncodingName = "utf-8"
        binding.web.settings.javaScriptEnabled = true
        binding.web.settings.loadWithOverviewMode = true
        binding.web.settings.useWideViewPort = true
        binding.web.settings.domStorageEnabled = true
        binding.web.settings.databaseEnabled = true
        binding.web.settings.allowFileAccess = true
        binding.web.settings.allowContentAccess = true
        binding.web.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.web.settings.allowFileAccess = true
        //binding.web.settings.builtInZoomControls = true
        //binding.web.settings.displayZoomControls = false
        //binding.web.settings.setSupportZoom(true)
        binding.web.settings.setGeolocationEnabled(true)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.web, true)
        binding.web.webChromeClient = MyWebChromeClient()
        webViewClient()
        otherSettings()
        binding.web.loadUrl(url)
    }

    internal inner class MyWebChromeClient : WebChromeClient() {
        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback
        ) {
            callback.invoke(origin, true, false)
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onShowFileChooser(
            mWebView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            if (uploadMessage != null) {
                uploadMessage!!.onReceiveValue(null)
                uploadMessage = null
            }

            uploadMessage = filePathCallback

            val intent = fileChooserParams.createIntent()
            try {
                launchSomeActivity.launch(intent)
            } catch (e: Exception) {
                uploadMessage = null
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.toast_cnt_open_file_chooser),
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
            return true
        }
    }

    private fun webViewClient() {
        binding.web.webViewClient = object : MyWebViewClient(this, ViewCallback {
            //Toast.makeText(this, "@@@", Toast.LENGTH_SHORT).show()
            this.mAdapter!!.showBanner(this@MainActivity)
        }) {}
    }

    private fun otherSettings() {
        binding.web.setOnKeyListener(
            object : View.OnKeyListener {
                override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        val webView = v as WebView
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            if (webView.canGoBack()) {
                                webView.goBack()
                                return true
                            }
                        }
                    }
                    return false
                }
            })

        binding.web.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)

//            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
//            val builder = AlertDialog.Builder(this)
//            builder.setTitle(getString(R.string.download))
//            builder.setMessage(getString(R.string.to_save) + filename + " ?")
//            builder.setPositiveButton(getString(R.string.yes)) { dialog, which ->
//                val request = DownloadManager.Request(Uri.parse(url))
//                val cookie = CookieManager.getInstance().getCookie(url)
//                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
//                request.setTitle(filename)
//                request.addRequestHeader("Cookie", cookie)
//                request.addRequestHeader("User-Agent", userAgent)
//                request.allowScanningByMediaScanner()
//                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//                val downloadable =
//                    this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
//                downloadable.enqueue(request)
//
//                showToast(this, R.string.toast_downloading_file)
//            }
//            builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
//                dialog.cancel()
//            }
//            val dialog: AlertDialog = builder.create()
//            dialog.show()
        }

//        ActivityCompat.requestPermissions(
//            this, arrayOf(
//                Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ), 0
//        )
    }

    private fun checkNetworkAndGps(context: Context) {
        if (isNetworkAvailable(context)) {
            binding.web.visibility = View.VISIBLE
        } else {
            binding.web.visibility = View.GONE
        }
//        if (!isGpsEnabled(context)){
//            alertGps(context)
//        }
    }

    var launchSomeActivity = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            when {
                uploadMessage != null -> {
                    uploadMessage?.onReceiveValue(
                        WebChromeClient.FileChooserParams.parseResult(
                            result.resultCode,
                            data
                        )
                    )
                    uploadMessage = null
                }
            }
        }
    }

}
