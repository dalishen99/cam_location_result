package com.app.systech12

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.webview.R


class MainActivity : AppCompatActivity() {
    lateinit var mUploadMessage: ValueCallback<Uri>
    private val FILECHOOSER_RESULTCODE = 1
    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101
    private var myRequest: PermissionRequest? = null
    var webview: WebView? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

         webview = findViewById<WebView>(R.id.webview)


        webview!!.settings.javaScriptEnabled = true
        webview!!.webViewClient = WebViewClient()
        webview!!.settings.javaScriptEnabled = true
        webview!!.settings.javaScriptCanOpenWindowsAutomatically = true


        webview!!.settings.saveFormData = true
        webview!!.settings.setSupportZoom(false)
        webview!!.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webview!!.settings.pluginState = WebSettings.PluginState.ON

        webview!!.webChromeClient = object :WebChromeClient(){
            override fun onPermissionRequest(request: PermissionRequest?) {
                myRequest = request

                for (permission in request!!.resources) {
                    when (permission) {
                        "android.webkit.resource.AUDIO_CAPTURE" -> {
                            askForPermission(
                                request.origin.toString(),
                                Manifest.permission.RECORD_AUDIO,
                                MY_PERMISSIONS_REQUEST_RECORD_AUDIO
                            )
                        }
                    }
                }
            }
            fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
                mUploadMessage = uploadMsg
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "image/*"
                this@MainActivity.startActivityForResult(
                    Intent.createChooser(i, "Image Browser"),
                    FILECHOOSER_RESULTCODE
                )
            }
        }

        var webViewClient = object  : WebViewClient() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString(), getCustomHeaders()!!)
                return true
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                view.loadUrl(url!!, getCustomHeaders()!!)
                return true
            }
        }
        webview!!.webViewClient = webViewClient
        webview!!.loadUrl("https://av.80070.ae")

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                Log.d("WebView", "PERMISSION FOR AUDIO")
                if (grantResults.size > 0
                    && grantResults[0] === PackageManager.PERMISSION_GRANTED
                ) {


                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    myRequest!!.grant(myRequest!!.resources)
                  //  webview!!.loadUrl("https://av.80070.ae")
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }

        }
    }


    override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        intent: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage) return
            val result = if (intent == null || resultCode != RESULT_OK) null else intent.data
            mUploadMessage.onReceiveValue(result)
            mUploadMessage = null!!
        }
    }

    fun askForPermission(origin: String, permission: String, requestCode: Int) {
        Log.d("WebView", "inside askForPermission for" + origin + "with" + permission)
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                permission
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    permission
                )
            ) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(permission),
                    requestCode
                )
            }
        } else {
            myRequest!!.grant(myRequest!!.resources)
        }
    }

    private fun getCustomHeaders(): Map<String, String>? {
        val headers: MutableMap<String, String> = HashMap()
        headers["User-Agent"] =  "Mozilla/5.0 (Linux; Android 9; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.101 Mobile Safari/537.36 ANDROID"
        return headers
    }
}
