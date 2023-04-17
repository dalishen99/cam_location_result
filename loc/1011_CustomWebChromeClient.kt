package tech.okcredit.web.web_clients

import android.content.Intent
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.NonNull
import androidx.webkit.WebViewClientCompat
import tech.okcredit.android.base.extensions.gone
import tech.okcredit.android.base.extensions.visible
import tech.okcredit.base.exceptions.ExceptionUtils
import tech.okcredit.web.ui.WebViewActivity
import timber.log.Timber
import java.lang.ref.WeakReference

class CustomWebChromeClient internal constructor(
    private val activity: WeakReference<WebViewActivity>,
    private val webView: WebView,
    private val customViewContainer: FrameLayout
) : WebChromeClient() {

    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null) {
            callback?.onCustomViewHidden()
            return
        }
        customView = view
        webView.gone()
        customViewContainer.visible()
        customViewContainer.addView(view)
        customViewCallback = callback
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        if (customView == null)
            return

        webView.visible()
        customViewContainer.gone()
        customView?.gone()

        customViewContainer.removeView(customView)
        customViewCallback?.onCustomViewHidden()
        customView = null
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        callback?.invoke(origin, true, false)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        Timber.d("Android Console: %s", consoleMessage.message())
        return true
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        activity.get()?.webviewFilePaths = filePathCallback

        if (fileChooserParams?.isCaptureEnabled == true) {
            activity.get()?.handleCameraUpload()
        } else {
            activity.get()?.handleGalleryOpen()
        }
        return true
    }

    override fun onCreateWindow(
        view: WebView,
        dialog: Boolean,
        userGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        Timber.v(WebViewActivity.TAG_ANDROID_WEBVIEW, "new create window: url => ${view.url}")
        view.removeAllViews()

        val newView = WebView(view.context)

        newView.apply {
            settings.apply {
                javaScriptEnabled = true
                setGeolocationEnabled(true)
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
            }

            webViewClient = object : WebViewClientCompat() {
                override fun shouldOverrideUrlLoading(
                    @NonNull view: WebView,
                    @NonNull request: WebResourceRequest
                ): Boolean {
                    Timber.d(WebViewActivity.TAG_ANDROID_WEBVIEW, "URl from New tab => ${view.url}")
                    Timber.d(WebViewActivity.TAG_ANDROID_WEBVIEW, "Url => ${view.url}")
                    val loadInBrowser =
                        request.url.getQueryParameter("browser")?.toBoolean()
                            ?: request.url.path?.startsWith("upi://") ?: false

                    return if (loadInBrowser) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, request.url)
                            activity.get()?.startActivity(intent)
                            true
                        } catch (e: Exception) {
                            ExceptionUtils.logException("Web Activity unable to open url in browser ", e)
                            true
                        }
                    } else {
                        false
                    }
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onCloseWindow(w: WebView) {
                    Timber.v(WebViewActivity.TAG_ANDROID_WEBVIEW, " Close window from subview")
                    super.onCloseWindow(w)
                    view.removeView(newView)
                }
            }
        }

        newView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        view.addView(newView)
        val transport =
            resultMsg?.obj as WebView.WebViewTransport
        transport.webView = newView
        resultMsg.sendToTarget()
        return true
    }

    override fun onCloseWindow(w: WebView) {
        Timber.v(WebViewActivity.TAG_ANDROID_WEBVIEW, "Onclose from main")
        super.onCloseWindow(w)
    }
}
