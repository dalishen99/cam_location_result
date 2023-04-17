package com.ssongh.webview.sample.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Message
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.ssongh.webview.sample.`object`.ReqCodeObject
import com.ssongh.webview.sample.manager.WebViewFileManager
import com.ssongh.webview.sample.manager.WebViewPopupManager
import com.ssongh.webview.sample.utils.CustomWebView
import com.ssongh.webview.sample.utils.DialogUtils
import com.ssongh.webview.sample.utils.L
import java.lang.Exception


open class BaseWebChromeClient(private val activity: Activity?) : WebChromeClient() {
    private var customView: View? = null
    private var customViewCallback: CustomViewCallback? = null
    private var fullScreenContainer: FrameLayout? = null
    private var coverScreenParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        L.i("onProgressChanged:newProgress = $newProgress")
        super.onProgressChanged(view, newProgress)
    }

    /**
     * 비디오 일반 > 풀스크린 시 호출 됨
     */
    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (customView != null) {
            callback?.onCustomViewHidden()
            return
        }
        activity?.run {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR

            fullScreenContainer = FullscreenHolder(this)
            fullScreenContainer?.addView(view, coverScreenParams)

            val decor = window.decorView as FrameLayout
            decor.addView(fullScreenContainer, coverScreenParams)
            customView = view
            setFullscreen(true)
            customViewCallback = callback

            WebViewPopupManager.getInstance(this).isVideoFullScreen = true
            L.i("onShowCustomView")
        }

        super.onShowCustomView(view, callback)
    }

    /**
     * 비디오 풀스크린 > 일반 시 호출 됨
     */
    override fun onHideCustomView() {
        if (customView == null) {
            return
        }
        activity?.run {
            setFullscreen(false)
            val decor = window.decorView as FrameLayout
            decor.removeView(fullScreenContainer)
            fullScreenContainer = null
            customView = null
            customViewCallback?.onCustomViewHidden()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

            WebViewPopupManager.getInstance(this).isVideoFullScreen = false
            L.i("onHideCustomView")
        }

        activity?.currentFocus?.clearFocus()
    }

    /**
     * 비디오 풀스크린 뷰의 width, height 조정
     * width : 가로 값
     * height : 세로 값
     */
    fun resizeChildView(width: Int, height: Int) {
        activity?.run {
            fullScreenContainer?.getChildAt(0)?.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                this.width = width
                this.height = height
                this.gravity = Gravity.CENTER
            }
        }
        L.i("resizeChildView")
    }

    /**
     * 화면을 풀스크린으로 변경 (navigation 등 숨김)
     * enabled : true-풀스크린 상태, false-풀스크린이 아닌 상태
     */
    private fun setFullscreen(enabled: Boolean) {
        activity?.run {
            val win = window

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insetsController = window.insetsController
                if (enabled) {
                    insetsController?.hide(WindowInsets.Type.statusBars())
                    L.i("setFullscreen_hide(WindowInsets.Type.statusBars())")
                } else {
                    insetsController?.show(WindowInsets.Type.statusBars())
                    L.i("setFullscreen_show(WindowInsets.Type.statusBars())")
                }

            } else {
                val winParams = win.attributes
                val bits = WindowManager.LayoutParams.FLAG_FULLSCREEN
                val immersiveFlags =
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

                if (enabled) {
                    winParams.flags = winParams.flags or bits
                    customView?.systemUiVisibility = immersiveFlags
                    customView?.setOnSystemUiVisibilityChangeListener {
                        if (customView?.systemUiVisibility != immersiveFlags) {
                            customView?.systemUiVisibility = immersiveFlags
                        }
                    }
                } else {
                    winParams.flags = winParams.flags and bits.inv()
                    customView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
                win.attributes = winParams
                L.i("setFullscreen_Build.VERSION.SDK_INT < Build.VERSION_CODES.R)")
            }
        }
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        view?.context?.let {
            DialogUtils.showConfirm(
                context = it,
                msg = message,
                cancelable = false,
                positiveFun = { result?.confirm() },
                negativeFun = { result?.cancel() }
            )
        }
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        view?.context?.let {
            DialogUtils.showAlert(
                context = it,
                msg = message,
                cancelable = false,
                positiveFun = { result?.confirm() })
        }
        return true
    }

    /**
     * 웹뷰 팝업 생성
     */
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        view?.context?.let {
            val childWebViewClient = BaseWebViewClient()
            val childWebChromeClient = BaseWebChromeClient(activity)

            val childView = CustomWebView(it).apply {
                webViewClient = childWebViewClient
                webChromeClient = childWebChromeClient
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                overScrollMode = View.OVER_SCROLL_NEVER
            }


            WebViewPopupManager.getInstance(it)
                .addWebView(activity, childView, childWebChromeClient)

            (resultMsg?.obj as? WebView.WebViewTransport)?.webView = childView
            resultMsg?.sendToTarget()
        }
        L.i("onCreateWindow")
        return true
    }

    /**
     * 웹뷰 팝업 해제
     */
    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)

        window?.context?.let {
            WebViewPopupManager.getInstance(it).removeWebView(activity)
        }
        L.i("onCloseWindow")
    }

    /**
     * 웹뷰에서 파일 업로드시
     */
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        webView?.context?.run {
            if (filePathCallback != null) {
                WebViewFileManager.getInstance(this).resetFilePathCallback()

                WebViewFileManager.getInstance(this).filePathCallback = filePathCallback

                (this as Activity).startActivityForResult(
                    Intent.createChooser(
                        Intent(Intent.ACTION_GET_CONTENT)
                            .addCategory(Intent.CATEGORY_OPENABLE)
                            .setType("*/*"), "파일 선택"
                    ),
                    ReqCodeObject.REQ_CODE_FILE_CHOOSER
                )
            }
        }
        L.i("onShowFileChooser")
        return true
    }

    /**
     * 웹뷰 동영상 풀스크린 시 레이아웃
     */
    private class FullscreenHolder(context: Context) : FrameLayout(context) {
        init {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.black))
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent?): Boolean {
            return true
        }
    }

    /**
     * 웹뷰 GPS 사용시
     */
    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        super.onGeolocationPermissionsShowPrompt(origin, callback)
        callback?.invoke(origin, true, false)
    }
}