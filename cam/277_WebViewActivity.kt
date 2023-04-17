package com.example.wwleadstest

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.wwleadstest.databinding.ActivityWebviewBinding
import com.example.wwleadstest.entites.UserInfo
import com.example.wwleadstest.utils.Constatns.URL_KEY
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebviewBinding
    private var cameraUploadCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private lateinit var filePickerLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebviewBinding.inflate(layoutInflater)
        val databaseReference = FirebaseDatabase.getInstance().reference
        setContentView(binding.root)
        initWebView(databaseReference)
    }

    private fun initWebView(databaseReference: DatabaseReference) {
        setInitSettingsForWebView()
        val url = intent.extras?.getString(URL_KEY) ?: ""
        binding.web.loadUrl(url)
        handleOnPageFinished(databaseReference)
        handleLoadingImage()
        handleDownloadingFiles()
    }

    private fun handleLoadingImage() {
        binding.web.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                cameraUploadCallback = filePathCallback
                val mimeTypes = arrayOf("image/*")
                filePickerLauncher.launch(mimeTypes)
                return true
            }
        }
        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    val photoUri = result.data?.data
                    val resultArray = photoUri?.let { arrayOf(it) }
                    cameraUploadCallback?.onReceiveValue(resultArray)
                    cameraUploadCallback = null
                }
            }
        filePickerLauncher =
            registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { result ->
                if (result != null) {
                    val resultArray = result.map { it }.toTypedArray()
                    cameraUploadCallback?.onReceiveValue(resultArray)
                    cameraUploadCallback = null
                }
            }
    }

    private fun setInitSettingsForWebView() {
        if (ContextCompat.checkSelfPermission(this@WebViewActivity, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@WebViewActivity,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
        binding.web.settings.allowFileAccess = true
        binding.web.settings.loadWithOverviewMode = true
        binding.web.settings.mediaPlaybackRequiresUserGesture = false
        binding.web.settings.javaScriptEnabled = true
        binding.web.settings.domStorageEnabled = true
        binding.web.settings.useWideViewPort = true
        binding.web.settings.builtInZoomControls = true
        binding.web.settings.displayZoomControls = false
    }

    private fun handleOnPageFinished(databaseReference: DatabaseReference) {
        binding.web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                updateUrl(databaseReference, url)
            }
        }
    }

    private fun handleDownloadingFiles() {
        binding.web.setDownloadListener { url, _, _, _, _ ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val fileName = URLUtil.guessFileName(url, null, null)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
        }
    }

    fun updateUrl(databaseReference: DatabaseReference, url: String) {
        val myId = databaseReference.push().key!!
        val newRequest = UserInfo(myId, url)
        databaseReference.child(myId).setValue(newRequest)
    }

    override fun onBackPressed() {
        if (binding.web.canGoBack()) {
            binding.web.goBack()
        } else {
            Toast.makeText(this, "()_()", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.web.destroy()
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 1001
    }
}