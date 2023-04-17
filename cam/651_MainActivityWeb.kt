package com.baskets.winl


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.*
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout


class MainActivityWeb : AppCompatActivity() {

    private var uploadMessage: ValueCallback<Uri>? = null
    private var uploadMessageAboveL: ValueCallback<Array<Uri>>? = null

    private val networkMonitor = NetworkMonitorUtil(this)

    @SuppressLint("SetJavaScriptEnabled", "HardwareIds", "CommitPrefEdits")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web)
        supportActionBar?.hide()

        val web: WebView = findViewById(R.id.web_browser)

        val web_no_connect: LinearLayout = findViewById(R.id.no_connect)

        var url_adress: String

        //инет
        networkMonitor.result = { isAvailable, type ->
            runOnUiThread {
                if (isAvailable) {
                    web.visibility = VISIBLE
                    web_no_connect.visibility = GONE
                } else {
                    web.visibility = GONE
                    web_no_connect.visibility = VISIBLE
                }
            }
        }

        val permissionStatus =
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_PERMISSION_CAMERA
            )
        }

        //загрузка url
        val sharedPreference = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        val savedString = sharedPreference.getString("url_address", "")

        url_adress = savedString.toString()

        web.settings.javaScriptEnabled = true
        web.settings.loadsImagesAutomatically = true
        web.settings.setAppCacheEnabled(false)
        web.settings.databaseEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccessFromFileURLs = true
        web.settings.allowUniversalAccessFromFileURLs = true
        web.settings.blockNetworkImage = false
        web.settings.useWideViewPort = true
        web.settings.loadWithOverviewMode = true
        web.settings.javaScriptCanOpenWindowsAutomatically = true
        web.settings.allowFileAccess = true
        web.settings.setSupportMultipleWindows(true)
        web.settings.allowContentAccess = true

        web.settings.cacheMode = WebSettings.LOAD_DEFAULT
        web.canGoBack()

        val data = intent.getStringExtra("polisy")


        val token = sharedPreference.getString("token", "")
        val offer_id = sharedPreference.getString("offer_id", "")
        if ((token == "") or (token == "null")) {
            val i = Intent(this, PrivacyPolicy::class.java)
            web.loadDataWithBaseURL(null, data!!, "text/html", "utf-8", null);
            i.putExtra("polisy", data)
            startActivity(i)
            finish()
        } else {
            val tracker = "https://pomidorkin.space/Zmnyjkbw?id=com.baskets.winl"
            web.loadUrl("$tracker/?_lp=1&_token=$token&offer_id=\"$offer_id")
        }

        //взаимодествие после прогрузки страницы в вэбвью
        web.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                val progressBar: ProgressBar = findViewById(R.id.progressBar)

                if (newProgress == 100) {
                    progressBar.visibility = GONE
                } else {
                    progressBar.visibility = VISIBLE
                    progressBar.progress = newProgress;
                }

            }

            // For Android >= 5.0
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams,
            ): Boolean {
                uploadMessageAboveL = filePathCallback
                openImageChooserActivity()
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest) {

                runOnUiThread {
                    val permission =
                        arrayOf(
                            PermissionRequest.RESOURCE_AUDIO_CAPTURE,
                            PermissionRequest.RESOURCE_VIDEO_CAPTURE
                        )
                    request.grant(permission)
                }

            }
        }

        //обновлялка
        val swipeRefresh: SwipeRefreshLayout = findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            swipeRefresh.isEnabled = web.scrollY == 0
        }

        web.webViewClient = object : WebViewClient() {

            @SuppressLint("CommitPrefEdits")
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                view?.loadUrl(request?.url.toString())

                //сохранение текущей ссылки
                val editor = sharedPreference.edit()
                url_adress = request?.url.toString()
                editor.apply {
                    putString("url_address", url_adress)
                }.apply()

                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                if (swipeRefresh.isRefreshing) {
                    swipeRefresh.isRefreshing = false
                }

            }

        }
        swipeRefresh.setColorSchemeResources(android.R.color.holo_blue_light)

    }

    override fun onResume() {
        super.onResume()
        networkMonitor.register()
    }


    override fun onStop() {
        super.onStop()
        networkMonitor.unregister()
    }

    //чек проверка инета
    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)

        return (capabilities != null && capabilities.hasCapability(NET_CAPABILITY_INTERNET))

    }

    private fun openImageChooserActivity() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "image/*"
        startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILE_CHOOSER_RESULT_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == uploadMessage && null == uploadMessageAboveL) return
            val result = if (data == null || resultCode != Activity.RESULT_OK) null else data.data
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data)
            } else if (uploadMessage != null) {
                uploadMessage!!.onReceiveValue(result)
                uploadMessage = null
            }
        }
    }

    private fun onActivityResultAboveL(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null)
            return
        var results: Array<Uri>? = null
        if (resultCode == Activity.RESULT_OK) {
            if (intent != null) {
                val dataString = intent.dataString
                val clipData = intent.clipData
                if (clipData != null) {
                    results = Array(clipData.itemCount) { i ->
                        clipData.getItemAt(i).uri
                    }
                }
                if (dataString != null)
                    results = arrayOf(Uri.parse(dataString))
            }
        }
        uploadMessageAboveL!!.onReceiveValue(results)
        uploadMessageAboveL = null
    }

    companion object {
        private val FILE_CHOOSER_RESULT_CODE = 10000
        private val REQUEST_CODE_PERMISSION_CAMERA = 100
    }

    override fun onBackPressed() {
        val web_browser: WebView = findViewById(R.id.web_browser)
        if (web_browser.canGoBack())
            web_browser.goBack()
        else
            finish()
    }

}