package com.avijekrl.proald.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.FileProvider
import com.avijekrl.proald.BuildConfig
import java.io.File


private fun getTmpFileUri(context: Context): Uri {
    val tmpFile = File.createTempFile("tmp_image_file", ".png", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }

    return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
}


@Composable
fun WebView(url: String) {
    println("loading Url : $url")
    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    var curRequest: PermissionRequest? = null
    var myFilePathCallback by remember {
        mutableStateOf<ValueCallback<Array<Uri>>?>(null)
    }
    var uri: Uri? = null
    var backEnabled by remember { mutableStateOf(false) }
    var webView: WebView? = null
    val showDialog = remember { mutableStateOf(false) }
    //val urlLoaded = remember { mutableStateOf(false) }

    val getPictureFromCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
        if(it) {

            println("MY URI INSIDE : $uri")

            if(uri != null) {
                myFilePathCallback?.onReceiveValue(arrayOf(uri!!))
                println("Value set")
            }
        }
    }

    val getImageFromGallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { curUri: Uri? ->
        println("MY URI INSIDE : $curUri")
        if(curUri != null)
            myFilePathCallback?.onReceiveValue(arrayOf(curUri))
    }

    val permissionForCameraApp =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when {
                    granted -> {
                        uri = getTmpFileUri(context)
                        println("MY URI IS : $uri")
                        getPictureFromCamera.launch(uri)
                    }
                    !shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) -> {
                        Toast.makeText(context, "Access to camera denied", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(context, "Access to camera denied", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    @Composable
    fun showChooserDialog() {
        val builder = AlertDialog.Builder(context)
        val options = arrayOf("From gallery", "From camera", "File from explorer")

        if(showDialog.value) AlertDialog(
            onDismissRequest = {},
            title = {
                Text("Get picture from")
            },

            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        getImageFromGallery.launch("image/*")
                    },
                ) {
                    Text("From gallery")
                }
                Button(
                    onClick = {
                        showDialog.value = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(activity,
                                    Manifest.permission.CAMERA)
                            ) {
                                uri = getTmpFileUri(context)
                                println("MY URI IS : $uri")
                                getPictureFromCamera.launch(uri)
                            } else {
                                permissionForCameraApp.launch(Manifest.permission.CAMERA)
                            }
                        } else {
                            uri = getTmpFileUri(context)
                            println("MY URI IS : $uri")
                            getPictureFromCamera.launch(uri)
                        }
                    },
                ) {
                    Text("From camera")
                }
                Button(
                    onClick = {
                        showDialog.value = false
                        getImageFromGallery.launch("*/*")
                    },
                ) {
                    Text("File from explorer")
                }
            }
        )

        builder
            .setCancelable(false)
            .setTitle("Get picture from").setSingleChoiceItems(options, -1) { dialog, item ->
                when (item) {
                    0 -> {
                        getImageFromGallery.launch("image/*")
                    }
                    1 -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (shouldShowRequestPermissionRationale(activity,
                                    Manifest.permission.CAMERA)
                            ) {
                                uri = getTmpFileUri(context)
                                println("MY URI IS : $uri")
                                getPictureFromCamera.launch(uri)
                            } else {
                                permissionForCameraApp.launch(Manifest.permission.CAMERA)
                            }
                        } else {
                            uri = getTmpFileUri(context)
                            println("MY URI IS : $uri")
                            getPictureFromCamera.launch(uri)
                        }
                    }
                    2 -> {
                        getImageFromGallery.launch("*/*")
                    }
                }
                dialog.cancel()
            }
        //builder.create()
        //builder.show()
    }
    showChooserDialog()

    val cameraPermissionForDirect =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when {
                    granted -> {
                        curRequest?.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    }
                    !shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA) -> {
                        Toast.makeText(context, "Access to camera denied", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(context, "Access to camera denied", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    val mWebChromeClient = object: WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest?) {
            curRequest = request

            request?.resources?.forEach { r ->
                if(r == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        cameraPermissionForDirect.launch(Manifest.permission.CAMERA)
                    }
                    else {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    }
                }
            }

        }

        override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
            myFilePathCallback = filePathCallback
            println("NEW CALLBACK $myFilePathCallback")
            //showChooserDialog()
            showDialog.value = true
            return true
        }
    }

    AndroidView(factory = {
        WebView(it).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setDownloadListener { url, s2, s3, s4, l ->
                println("Started downloading")
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                context.startActivity(i)
            }
            webViewClient = object: WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    backEnabled = view.canGoBack()
                }
            }
            webChromeClient = mWebChromeClient
            settings.apply {
                javaScriptEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                domStorageEnabled = true
                databaseEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                javaScriptCanOpenWindowsAutomatically = true
                defaultTextEncodingName = "utf-8"
            }
            loadUrl(url)
        }
    }, update = {
        webView = it
        //if(!urlLoaded.value) it.loadUrl(mUrl)
        //urlLoaded.value = true
    })

    BackHandler() {
        if(backEnabled) webView?.goBack()
    }
}