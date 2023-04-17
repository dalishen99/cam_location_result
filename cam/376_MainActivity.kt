package com.example.chatsansar
import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val MULTIPLE_PERMISSION_CODE = 105

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        val webSettings = webView.getSettings()
        webSettings.setJavaScriptEnabled(true) //allow javascript
        webSettings.setDomStorageEnabled(true) //allow cookies
        webSettings.setAllowFileAccess(true); //allow filesystem access
        webSettings.setMediaPlaybackRequiresUserGesture(false); //disable user requirement to acess for sound output
        webView.webChromeClient = object : WebChromeClient() {
//            Handle file upload
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                val intent = fileChooserParams?.createIntent()
                startActivityForResult(intent, 1)
             //Todo Handle form submit by calling the api
                return true
            }
           //get permission status
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }
        //Runtime permissions request
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE), MULTIPLE_PERMISSION_CODE)

        webView.loadUrl("https://metachat.chatsansar.com/")
    }
}