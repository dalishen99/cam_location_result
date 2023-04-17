package com.example.webviewapp_android_camera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 23) {
            // AndroidManifest.xml    <uses-permission android:name="android.permission.CAMERA" />
            // カメラへの許可をチェック
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // 許可が取れなかった
                ActivityCompat.requestPermissions(this,
                    arrayOf(
                        Manifest.permission.CAMERA
                    ),
                    PERMISSIONS_REQUEST_CODE
                )
            }
        }

        val webView: WebView = findViewById(R.id.webview)
        webView.apply {
            settings.apply {
                javaScriptEnabled = true                                // JavaScriptの有効化
                mediaPlaybackRequiresUserGesture = false                // カメラ自動再生
                userAgentString = userAgentString + " qo.kenkoappli"    // アプリケーションかWebかを判断
            }
            webChromeClient = object : WebChromeClient() {
                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (request?.resources!!.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                        request.grant(request.resources)
                    }
                }
            }
            webViewClient = WebViewClient()
        }

//        webView.loadUrl("https://masaki1103.github.io/camera_demo/")
        webView.loadUrl("https://masaki1103.github.io/qrcode_demo/")
//        webView.loadUrl("https://masaki1103.github.io/capture_demo/")
     }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
         when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // 許可が取れた場合
                    println("permission granted")
                } else {
                    // 許可が取れなかった場合
                    RuntimePermissionUtils().showAlertDialog(supportFragmentManager,"カメラ")
                }
                return
            }
        }
    }
}