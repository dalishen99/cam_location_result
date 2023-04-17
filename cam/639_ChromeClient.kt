package com.minicoin.minimaster.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.minicoin.minimaster.data.Constants.FILE_CHOOSER_RESULT_CODE
import com.minicoin.minimaster.data.Constants.INPUT_FILE_REQUEST_CODE

open class ChromeClient(private val activity: MainWebClient) : WebChromeClient() {

    override fun onPermissionRequest(request: PermissionRequest?) {
        activity.requestPermissions(arrayOf(Manifest.permission.CAMERA), 10)
        request?.grant(request.resources)
    }

    open class MainWebClient : androidx.appcompat.app.AppCompatActivity() {

        var valueCallbackWebView: ValueCallback<Array<Uri?>?>? = null

        override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
            super.onCreate(savedInstanceState, persistentState)

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                    1
                )
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == FILE_CHOOSER_RESULT_CODE && resultCode == RESULT_OK && data?.data != null) {
                val results: Array<Uri?>?
                results = arrayOf(data.data)
                this.valueCallbackWebView?.onReceiveValue(results)
            } else {
                this.valueCallbackWebView?.onReceiveValue(null)
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    override fun onShowFileChooser(
        view: WebView?,
        filePath: ValueCallback<Array<Uri?>?>,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        activity.valueCallbackWebView = filePath
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val intentArray: Array<Intent?> = arrayOf(galleryIntent)
        val contentSelectionIntent = Intent().also { intent ->
            intent.action = Intent.ACTION_GET_CONTENT
            intent.type = "*/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
        }
        val chooserIntent = Intent(Intent.ACTION_CHOOSER).also { intent ->
            intent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            intent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        }
        activity.startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
        return true
    }
}