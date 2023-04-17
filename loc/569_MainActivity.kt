package com.chwimi.bobchoo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var mWebView: WebView
    private lateinit var mToast: Toast
    private var backWait: Long = 0

    companion object {
        private const val MY_PERMISSION_REQUEST_LOCATION: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionCheck()
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
    }

    //위치정보 권한 확인
    private fun permissionCheck() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
            initWebView()
        else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSION_REQUEST_LOCATION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSION_REQUEST_LOCATION)
            initWebView()
    }

    private fun initWebView() {
        mWebView = findViewById(R.id.webview)
        mWebView.loadUrl("https://bobchoo.site")

        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.loadWithOverviewMode = true
        mWebView.settings.useWideViewPort = true

        mWebView.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                callback?.invoke(origin, true, false)
            }
        }
    }

    override fun onBackPressed() {
        if (mWebView.canGoBack())
            mWebView.goBack()
        else {
            if (System.currentTimeMillis() - backWait >= 2000) {
                backWait = System.currentTimeMillis()
                mToast.setText("뒤로가기 버튼을 한 번 더 누르면 종료됩니다.")
                mToast.show()
            } else
                super.onBackPressed()
        }

    }


}