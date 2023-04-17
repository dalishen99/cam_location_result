package com.syb.syblibrary.ui.view.web

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.*
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.syb.syblibrary.R
import com.syb.syblibrary.ui.view.permission.PermissionFragment
import com.syb.syblibrary.ui.view.popupdialog.CustomPopupDialog
import com.syb.syblibrary.ui.view.web.fullscreen.FullScreenLayout
import com.syb.syblibrary.ui.view.web.util.FileChooser
import com.syb.syblibrary.util.log.YLog

abstract class BaseWebChromeClient : WebChromeClient
{
    private var webView: WebView
    private var activity: AppCompatActivity
    private var fileChooserMessage: String
    private var fileChooser: FileChooser
    private var mCustomView: View? = null
    private var mCustomViewCallback: CustomViewCallback? = null
    private var mFullscreenContainer: FullScreenLayout? = null
    private val COVER_SCREEN_PARAMS = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    private var mProgressVideo: View? = null

    constructor(@NonNull webView: WebView, @NonNull activity: AppCompatActivity)
    {
        this.webView = webView
        this.activity = activity
        fileChooser = FileChooser(activity)
        fileChooserMessage = activity.getString(R.string.message_permission_photo_request)
    }

    /**
     * 파일 선택 안내 문구 Set
     */
    fun setFileChooserMessage(fileChooserMessage: String)
    {
        this.fileChooserMessage = fileChooserMessage
    }

    /**
     * fileChooser 반환
     */
    fun getFileChooser() = fileChooser

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        CustomPopupDialog(activity).apply {
            contentText.set(message ?: "")
            onConfirmClick = {
                result?.confirm()
            }
            onCancelClick = {
                result?.cancel()
            }
            show()
        }
        return true
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        CustomPopupDialog(activity).apply {
            contentText.set(message ?: "")
            cancelBtnVisible.set(false)
            onConfirmClick = {
                result?.confirm()
            }
            show()
        }
        return true
    }

    override fun onJsBeforeUnload(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        CustomPopupDialog(activity).apply {
            contentText.set("${message ?: ""}\n\n${context.getString(R.string.message_quest_content)}")
            confirmText.set(context.getString(R.string.message_quest_confirm))
            cancelText.set(context.getString(R.string.message_quest_cancel))
            onConfirmClick = {
                result?.confirm()
            }
            onCancelClick = {
                result?.cancel()
            }
            show()
        }

        return true
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        CustomPopupDialog(activity).apply {
            contentText.set(message ?: "")
            editTextVisible.set(true)
            onConfirmClick = {
                result?.confirm(this.getEditTextContent())
            }
            onCancelClick = {
                result?.cancel()
            }
            show()
        }
        return true
    }

    override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
        super.onConsoleMessage(message, lineNumber, sourceID)
        YLog.i("WebView Log Message => ${message}")
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            onConsoleMessage(it.message(), it.lineNumber(), it.sourceId())
        }
        return super.onConsoleMessage(consoleMessage)
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        if(view is BaseWebView) {
            val baseWebView: BaseWebView = view
            baseWebView.baseWebViewListener?.onProgressChanged(view, newProgress)
            val hProgressBar = baseWebView.hProgressBar
            hProgressBar?.progress = newProgress
            if (newProgress == 100)
                hProgressBar?.visibility = View.GONE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        val targetUrl = webView.hitTestResult.extra

        val newWebView = WebView(webView.context)
        newWebView.settings.javaScriptEnabled = true
        val transport = resultMsg?.obj as WebView.WebViewTransport
        transport.webView = newWebView
        resultMsg.sendToTarget()

        newWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                }
                webView.context.startActivity(browserIntent)
                return true
            }
        }
        return true
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        webView?.context?.let {
            val permission = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            val deniedSize = PermissionFragment.deniedPermissionsSize(it, permission)
            if (deniedSize > 0)
            {
                val permissionFragment = PermissionFragment.newInstance(permission, fileChooserMessage)
                val fragmentManager = activity.supportFragmentManager
                fragmentManager?.beginTransaction()?.apply {
                    add(permissionFragment, null)
                    commit()
                }
                return false
            }

            fileChooser.valueCallbackV21?.onReceiveValue(null)
            fileChooser.valueCallbackV21 = filePathCallback
            fileChooser.imageChooser(FileChooser.REQUEST_FILE_CHOOSE_V21, arrayOf("image/*"), "")
            return true
        }
        return false
    }

    /**
     * For Android 3.0+ onShowFileChooser
     */
    fun openFileChooser(callback: ValueCallback<Uri>)
    {
        fileChooser.imageChooser(FileChooser.REQUEST_FILE_CHOOSE_V11, arrayOf("image/*"), "")
        fileChooser.valueCallbackV11 = callback
    }

    /**
     * For Android 3.0+ onShowFileChooser
     */
    fun openFileChooser(callback: ValueCallback<Uri>, acceptType: String)
    {
        fileChooser.imageChooser(FileChooser.REQUEST_FILE_CHOOSE_V11, arrayOf("image/*"), "")
        fileChooser.valueCallbackV11 = callback
    }

    /**
     * For Android 4.1 onShowFileChooser
     */
    fun openFileChooser(callback: ValueCallback<Uri>, acceptType: String, capture: String)
    {
        fileChooser.imageChooser(FileChooser.REQUEST_FILE_CHOOSE_V11, arrayOf("image/*"), "")
        fileChooser.valueCallbackV11 = callback
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.grant(request.resources)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (mCustomView != null) {
                callback?.onCustomViewHidden()
                return
            }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            val decor = activity.window.decorView as FrameLayout
            mFullscreenContainer = FullScreenLayout(activity)
            mFullscreenContainer?.addView(view, COVER_SCREEN_PARAMS)
            decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS)
            mCustomView = view
            mCustomViewCallback = callback
            setFullscreen(true)
        }
        super.onShowCustomView(view, callback)
    }

    override fun onShowCustomView(
        view: View?,
        requestedOrientation: Int,
        callback: CustomViewCallback?
    ) {
        this.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        if (mCustomView == null)
            return
        setFullscreen(false)
        val decor = activity.window.decorView as FrameLayout
        decor.removeView(mFullscreenContainer)
        mFullscreenContainer = null
        mCustomView = null
        mCustomViewCallback?.onCustomViewHidden()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun getVideoLoadingProgressView(): View? {
        if(mProgressVideo == null)
            mProgressVideo = LayoutInflater.from(webView.context).inflate(R.layout.webview_video_loading_progress, null)
        return mProgressVideo
    }

    /**
     * 전체화면 보여주기 / 종료
     * @param isShow 전체화면 보여주기 / 종료 여부
     */
    private fun setFullscreen(isShow: Boolean) {
        val window: Window = activity.window
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        val behavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        val type = WindowInsetsCompat.Type.systemBars()
        insetsController.systemBarsBehavior = behavior
        if (isShow)
            insetsController.hide(type)
        else
            insetsController.show(type)
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?,
        callback: GeolocationPermissions.Callback?
    ) {
        super.onGeolocationPermissionsShowPrompt(origin, callback)
        callback?.invoke(origin, true, false)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            val permission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION )
            val deniedSize = PermissionFragment.deniedPermissionsSize(activity, permission)
            if (deniedSize > 0) {
                val permissionFragment = PermissionFragment.newInstance(permission, activity.getString(R.string.message_permission_gps))
                activity.supportFragmentManager.beginTransaction().apply {
                        add(permissionFragment, "permissionFragment")
                        commit()
                    }
                return
            }
        }
    }
}