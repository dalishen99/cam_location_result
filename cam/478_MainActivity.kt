package com.example.secureme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray


@SuppressLint("SetJavaScriptEnabled")
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SecureMe"
        const val CAMERA_REQUEST_CODE = 100
        const val REQUEST_CODE_GALLERY = 4
        const val JS_INTERFACE_NAME = "webview"
    }

    private var fileUriCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), CAMERA_REQUEST_CODE
            )
        } else {
            if (savedInstanceState == null) {
                setupWebView(intent?.data)
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webview.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        webview.saveState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (requestCode) {
            REQUEST_CODE_GALLERY -> fileUriCallback = if (resultCode == RESULT_OK) {
                val selectedImageUri = intent!!.data!!
                val localIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, selectedImageUri)
                this.sendBroadcast(localIntent)
                fileUriCallback!!.onReceiveValue(arrayOf(selectedImageUri))
                null
            } else {
                fileUriCallback?.onReceiveValue(arrayOf())
                null
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show()
            }
            setupWebView(intent?.data)
        }
    }

    private fun setupWebView(data: Uri?) {
        val enumerationResult = CameraEnumerationHelper.enumerateDevices(context = this)
        WebView.setWebContentsDebuggingEnabled(true)
        webview.apply {
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK;
            settings.setGeolocationDatabasePath(context.filesDir.path)
            addJavascriptInterface(
                JsObject(this@MainActivity, enumerationResult),
                JS_INTERFACE_NAME
            )

            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    Log.d(TAG, "onPermissionRequest")
                    runOnUiThread {
                        Log.d(TAG, "PERMISSION GRANTED")
                        request.grant(request.resources)
                    }
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {
                    callback?.invoke(origin, true, false)
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    Log.d(TAG, "onShowFileChooser")
                    this@MainActivity.fileUriCallback = filePathCallback
                    showGallery()
                    return true
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    Log.d(TAG, "shouldOverrideUrlLoading")
                    view!!.loadUrl(url)
                    return true
                }
            }
            loadUrl(data.toString())
        }
    }

    private fun showGallery() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_PICK
        startActivityForResult(
            Intent.createChooser(intent, "Select Image From Gallery"), REQUEST_CODE_GALLERY
        )
    }

    internal class JsObject(private val activity: MainActivity, private val deviceList: JSONArray) {

        @JavascriptInterface
        fun enumerateDevices(): String {
            return deviceList.toString()
        }

        @JavascriptInterface
        fun postMessage(json: String?, origin: String?): Boolean {
            if (json != null) {
                Log.d(TAG, "JsObject - $json")
                val eventObj = Gson().fromJson(json, JSEventObj::class.java)
                if (eventObj.eventType == "Success") {
                    Toast.makeText(activity, "PostMessage - Success", Toast.LENGTH_LONG).show()
                }
            }
            return false // here we return true if we handled the post.
        }
    }
}
