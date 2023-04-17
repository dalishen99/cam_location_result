package com.example.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.widget.ui.theme.DummyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DummyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val widget = "https://widget.onramper.com?skipTransactionScreen=true&txnAmount=555&txnCrypto=USDC_POLYGON&txnFiat=EUR&txnPaymentMethod=creditCard&txnGateway=Moonpay&apiKey=pk_test__jyaekSekz5BnLJGG8SOciws2zOU0Elzf0ZJu0ZqIJk0&partnerContext=%7B%22data%22:%20%227875d36a9e4070619ddd0055e41ee1d9e244d5d69e81288d64a043099e1dd2ad%22%20%7D&wallets=USDC_POLYGON:0x135027c0efc1385e68585fabf30bfe405378eb0d&isAddressEditable=false"

                    WebView(widget)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun WebView(url: String) {
        var callback: ValueCallback<Array<Uri>>? = null

        val startForResult =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                var results: Array<Uri>? = null

                if (result.resultCode == Activity.RESULT_OK) {
                    val intent = result.data

                    val dataString = intent?.dataString
                    if (dataString != null) {
                        results = arrayOf(Uri.parse(dataString))
                    }
                }

                callback?.onReceiveValue(results)
                callback = null
            }

        AndroidView(
            modifier = Modifier.fillMaxHeight(0.9f),
            factory = {
            WebView(it).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true

                }
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                webChromeClient = object: WebChromeClient(){
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request!!.grant(request.resources)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        callback?.onReceiveValue(null)
                        callback = filePathCallback

                        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                        contentSelectionIntent.type = "image/*"

                        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")

                        startForResult.launch(chooserIntent)

                        return true
                    }
                }
                loadUrl(url)
            }
        }, update = {
            it.loadUrl(url)
        })
    }
}
