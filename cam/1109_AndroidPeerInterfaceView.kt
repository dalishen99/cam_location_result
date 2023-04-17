package shim.shim.androidpeerrtc.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.webkit.*
import android.widget.LinearLayout

@SuppressLint("SetJavaScriptEnabled")
abstract class AndroidPeerInterfaceView(context: Context, attr: AttributeSet?) :
    LinearLayout(context, attr) {
    protected abstract val htmlUrl: String
    protected abstract val TAG: String

    protected val webView = WebView(context)
    private var onPageLoaded: (() -> Unit)? = null

    init {

        webView.also {
            it.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            it.setBackgroundColor(Color.BLACK)

            val settings =  it.settings
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false

            it.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(
                    message: String?,
                    lineNumber: Int,
                    sourceID: String?
                ) {
//                    Log.e(TAG, message.toString())

                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.grant(request.resources)
                }


            }

            it.webViewClient = object :WebViewClient(){
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    onPageLoaded?.invoke()
                }
            }





        }

        this.addView(webView)

    }

    fun loadView(onLoad: (() -> Unit)?) {
        this.onPageLoaded = onLoad
        webView.loadUrl(htmlUrl)
    }

    fun evaluateJavascript(script: String) {
        webView.evaluateJavascript(script, null)
    }
}