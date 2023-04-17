package com.example.food2

import android.Manifest.permission.*
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.*
import android.widget.Toast
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.os.Environment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import java.io.File

class MainActivity : AppCompatActivity() {
    private val url = "https://penggerak.foodmedia.id"
    private val urlCode = "https://www.sekolahkoding.com/"

    var requiredPermissions = arrayOf<String>(CAMERA, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE/*, Permissions.WRITE_SETTINGS*/)
    var uploadMessage:ValueCallback<Array<Uri>>? = null
    private val FILECHOOSER_RESULTCODE = 1
    private var mUploadMessage:ValueCallback<Uri>? = null
    var link : String? = null
    val REQUEST_SELECT_FILE = 100
    lateinit var mySwipeRefreshLayout : SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var myWebView: WebView = findViewById(R.id.webView)
        mySwipeRefreshLayout = findViewById(R.id.swipe_up)
        myWebView.loadUrl(url)
        myWebView.clearCache(true)
        val webViewSetting = myWebView.settings

        webViewSetting.javaScriptEnabled = true
        webViewSetting.setAppCacheEnabled(true)
        webViewSetting.saveFormData = true
        webViewSetting.savePassword = true
        webViewSetting.domStorageEnabled = true
        webViewSetting.setNeedInitialFocus(true)
        webViewSetting.loadsImagesAutomatically = true
        webViewSetting.javaScriptCanOpenWindowsAutomatically = true
        webViewSetting.allowFileAccess = true
        webViewSetting.allowContentAccess = true
        webViewSetting.allowFileAccessFromFileURLs = true
        webViewSetting.domStorageEnabled = true
        webViewSetting.databaseEnabled = true
        webViewSetting.setAppCacheEnabled(true)
        webViewSetting.allowUniversalAccessFromFileURLs = true
        webViewSetting.userAgentString = "Mozilla/5.0 (Linux; Android 4.1.1; Galaxy Nexus Build/JRO03C) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.166 Mobile Safari/535.19"

        mySwipeRefreshLayout.setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener {
                mySwipeRefreshLayout.isRefreshing = true
                myWebView.loadUrl(url)
                myWebView.reload() // refreshes the WebView
        })

        myWebView.webViewClient = object : WebViewClient(){
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    view?.loadUrl(request?.url.toString())
                }
                return true
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url.toString())
                mySwipeRefreshLayout.isRefreshing = false
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                Toast.makeText(this@MainActivity, "Halaman selesai di muat", Toast.LENGTH_LONG)
                mySwipeRefreshLayout.isRefreshing = false
                super.onPageFinished(view, url)
            }

        }

        myWebView.webChromeClient = object : WebChromeClient(){
            lateinit var imageUri : Uri

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                Log.d("alert", message.toString())
                val dialogBuilder = AlertDialog.Builder(this@MainActivity)
                dialogBuilder.setMessage(message)
                    .setCancelable(true)
                    .setPositiveButton("OK"){ _,_ ->
                    result?.confirm()
                }
                val alert = dialogBuilder.create()
                alert.show()
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    request?.grant(request?.resources)
                }
            }

    fun openFileChooser(uploadMsg : ValueCallback<Uri>, acceptType: String){

        val imageStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Pictures"
        )
        if (!imageStorageDir.exists()) {
            imageStorageDir.mkdirs()
        }
        val file = File(
            imageStorageDir.toString() + File.separator.toString() + "IMG_" + System.currentTimeMillis()
                .toString() + ".jpg"
        )
        imageUri = Uri.fromFile(file)

        val cameraIntents: MutableList<Intent> = ArrayList()
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val packageManager = packageManager
        val listCam = packageManager.queryIntentActivities(captureIntent, 0)
        for (res in listCam) {
            val packageName = res.activityInfo.packageName
            val i = Intent(captureIntent)
            i.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            i.setPackage(packageName)
            i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            cameraIntents.add(i)
        }

        mUploadMessage = uploadMsg
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE)
    }

     override fun onShowFileChooser(mWebView: WebView, filePathCB: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            if (uploadMessage != null){
                uploadMessage?.onReceiveValue(null)
                uploadMessage = null
            }

            uploadMessage = filePathCB
            val intent = fileChooserParams.createIntent()
            try {
                startActivityForResult(intent, REQUEST_SELECT_FILE)
            }catch (e: ActivityNotFoundException){
                uploadMessage = null
                Toast.makeText(getApplicationContext(), "Cannot Open File Chooser", Toast.LENGTH_LONG).show()
                return false
            }
            return true
        }else{
            return false
        }
    }

    fun openFileChooser(uploadMsg:ValueCallback<Uri>, acceptType:String, capture:String) {
        val imageStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "MyApp"
        )
        if (!imageStorageDir.exists()) {
            imageStorageDir.mkdirs()
        }
        val file = File(
            imageStorageDir.toString() + File.separator.toString() + "IMG_" + System.currentTimeMillis()
                .toString() + ".jpg"
        )
        imageUri = Uri.fromFile(file)

        val cameraIntents: MutableList<Intent> = ArrayList()
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val packageManager = packageManager
        val listCam = packageManager.queryIntentActivities(captureIntent, 0)
        for (res in listCam) {
            val packageName = res.activityInfo.packageName
            val i = Intent(captureIntent)
            i.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            i.setPackage(packageName)
            i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            cameraIntents.add(i)
        }

        mUploadMessage = uploadMsg
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(Intent.createChooser(intent, "File Browser"), FILECHOOSER_RESULTCODE)
    }

    fun openFileChooser(uploadMsg:ValueCallback<Uri>) {
        val imageStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "MyApp"
        )
        if (!imageStorageDir.exists()) {
            imageStorageDir.mkdirs()
        }
        val file = File(
            imageStorageDir.toString() + File.separator.toString() + "IMG_" + System.currentTimeMillis()
                .toString() + ".jpg"
        )
        imageUri = Uri.fromFile(file)

        val cameraIntents: MutableList<Intent> = ArrayList()
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val packageManager = packageManager
        val listCam = packageManager.queryIntentActivities(captureIntent, 0)
        for (res in listCam) {
            val packageName = res.activityInfo.packageName
            val i = Intent(captureIntent)
            i.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            i.setPackage(packageName)
            i.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            cameraIntents.add(i)
        }

        mUploadMessage = uploadMsg
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE)
    }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage != null) {
                    uploadMessage?.onReceiveValue(
                        WebChromeClient.FileChooserParams.parseResult(
                            resultCode,
                            data
                        )
                    )
                    uploadMessage = null
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val grant = ContextCompat.checkSelfPermission(this@MainActivity, CAMERA)
            val permissionList = arrayOf(CAMERA)

            if (grant != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this@MainActivity, permissionList, 1)
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (mUploadMessage != null) {
                var result = data?.data
                mUploadMessage?.onReceiveValue(result)
                mUploadMessage = null
            }
        } else {
            Toast.makeText(
                this,
                "Tidak bisa membuka files, silahkan cek perizinan files.",
                Toast.LENGTH_LONG
            ).show()
            super.onActivityResult(requestCode, resultCode, data)
        }
    }



        override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        var myWebView: WebView = findViewById(R.id.webView)
        if (keyCode == KeyEvent.KEYCODE_BACK && myWebView.canGoBack()){
            myWebView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun  checkInternetConnection(content : Context) : Boolean{
        val conManager : ConnectivityManager = content.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return (conManager.activeNetworkInfo != null && conManager.activeNetworkInfo!!.isAvailable
                && conManager.activeNetworkInfo!!.isConnected)
    }
}

