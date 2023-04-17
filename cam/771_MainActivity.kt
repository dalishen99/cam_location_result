package com.worldcoin.webviewpermissions

import android.Manifest
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.web.AccompanistWebChromeClient
import com.google.accompanist.web.AccompanistWebViewClient
import com.google.accompanist.web.WebViewState
import com.google.accompanist.web.WebView as AccompanistWebView
import com.google.accompanist.web.rememberWebViewState
import com.worldcoin.webviewpermissions.ui.theme.WebViewPermissionsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url =
            "https://buy-sandbox.moonpay.com/?apiKey=pk_test_PjABKr88VlgosyTueq3exrVnYYLd4ZB&currencyCode=usdc_polygon&baseCurrencyAmount=500&baseCurrencyCode=EUR&externalTransactionId=cn1PZFMRO5bCOC3FTYZCGQ--&lockAmount=true&walletAddress=0x345e65b0ea27db00592f485182b82e276c06b6c7&walletAddressTag=null&signature=NRoMywMU6n3JgksbOXKPP87vtx4He1Wm1rV5xx74W3c%3D"
        setContent {
            WebViewPermissionsTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.error) {
                    val webViewState = rememberWebViewState("")
                    legacy(url)
//                    accompanist(webViewState, url)
                }
            }
        }
    }

    @Composable
    private fun legacy(url: String) {
        AndroidView(factory = {
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
                }
                loadUrl(url)
            }
        }, update = {
            it.loadUrl(url)
        })
    }

    @Composable
    private fun accompanist(webViewState: WebViewState, url: String) {
        AccompanistWebView(
            modifier = Modifier.navigationBarsPadding(),
            state = webViewState,
            captureBackPresses = false,
            onCreated = {
                it.settings.javaScriptEnabled = true
                it.settings.domStorageEnabled = true
                it.settings.allowFileAccess = true

                CookieManager.getInstance().setAcceptThirdPartyCookies(it, true)

                it.loadUrl(url)
            },
            client = object : AccompanistWebViewClient(){},
                    chromeClient = object : AccompanistWebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.grant(request.resources)
                }
            })
    }
}
