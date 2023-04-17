package me.ibore.xweb

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.os.Parcelable
import android.view.View
import android.view.WindowManager
import android.webkit.*
import kotlinx.parcelize.Parcelize
import me.ibore.R
import me.ibore.base.XActivity
import me.ibore.databinding.XActivityWebBinding
import me.ibore.permissions.OnPermissionListener
import me.ibore.permissions.Permission
import me.ibore.permissions.XPermissions
import me.ibore.utils.ActivityUtils
import me.ibore.utils.BarUtils
import me.ibore.utils.DialogUtils
import me.ibore.utils.LogUtils

class XWebActivity : XActivity<XActivityWebBinding>(), XWebViewListener {
    companion object {

        private const val REQUEST_CODE = 1200
        private const val REQUEST_CODE_FILE_CHOOSER = 1201

        fun startActivity(activity: Activity, builder: Builder, requestCode: Int = REQUEST_CODE) {
            val bundle = Bundle()
            bundle.putParcelable("builder", builder)
            ActivityUtils.startActivityForResult(bundle, activity, XWebActivity::class.java, requestCode)
        }
    }

    private lateinit var builder: Builder
    private var mFilePathCallback: ValueCallback<Array<Uri>?>? = null
    private var mCustomViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun XActivityWebBinding.onBindView(bundle: Bundle?, savedInstanceState: Bundle?) {
        builder = Builder()
        builder = bundle?.getParcelable("builder") ?: Builder()
        ivTitleBarBack.setOnClickListener {
            onBackPressed()
        }
        ivTitleBarClose.setOnClickListener {
            super.onBackPressed()
        }
        ivTitleBarClose.visibility = View.GONE
        tvTitleBarTitle.text = builder.title
        webView.setXWebViewListener(this@XWebActivity)
    }

    override fun onBindConfig() {
        super.onBindConfig()
        BarUtils.setStatusBarLightMode(this, true)
    }

    override fun onBindData() {
        if (builder.url.isNotBlank()) {
            mBinding.webView.loadUrl(builder.url)
        } else {
            mBinding.webView.loadData(builder.data, "text/html", "UTF-8")
        }
        mBinding.tvTitleBarTitle.text = builder.title
    }

    override fun onReceivedTitle(title: String?) {
        if (!title.isNullOrEmpty()) {
            mBinding.tvTitleBarTitle.text = title
        }
    }

    override fun onReceivedIcon(icon: Bitmap?) {

    }

    override fun onPageStarted(webView: XWebView, url: String?, favicon: Bitmap?) {
        mBinding.webProgress.setWebProgress(0)
    }

    override fun onProgressChanged(view: XWebView, newProgress: Int) {
        mBinding.webProgress.setWebProgress(newProgress)
    }

    override fun onPageFinished(webView: XWebView, url: String?) {
        mBinding.webProgress.hide()
    }

    override fun onDownload(url: String, suggestedFilename: String?, userAgent: String?, contentDisposition: String?, mimeType: String?, contentLength: Long) {
        /*val request = DownloadManager.Request(Uri.parse(url))
         request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI)
         request.setTitle("下载")
         request.setDescription("今日头条正在下载.....")
         request.setAllowedOverRoaming(false)
         request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, suggestedFilename)
         val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
         downloadManager.enqueue(request)*/
        LogUtils.d(url, suggestedFilename, userAgent, contentDisposition, mimeType, contentLength)
    }

    override fun shouldOverrideUrlLoading(webView: XWebView, uri: Uri, requestHeaders: MutableMap<String, String>?): Boolean {
        if (webView.canGoBack()) {
            mBinding.ivTitleBarClose.visibility = View.VISIBLE
        }
        return false
    }

    override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
        mCustomViewCallback = callback
        mBinding.titleBar.visibility = View.GONE
        mBinding.webView.visibility = View.GONE
        mBinding.flContainer.visibility = View.VISIBLE
        mBinding.flContainer.addView(view)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onHideCustomView() {
        mBinding.titleBar.visibility = View.VISIBLE
        mBinding.webView.visibility = View.VISIBLE
        mBinding.flContainer.visibility = View.GONE
        mBinding.flContainer.removeAllViews()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        DialogUtils.showAlert(getXActivity(), getString(R.string.location_authorization),
            getString(R.string.location_authorization_prompt, origin ?: ""), negativeListener = {
                callback.invoke(origin, false, false)
            }, positiveListener = {
                XPermissions.with(getXActivity())
                    .permission(*Permission.GROUP_LOCATION)
                    .request(object : OnPermissionListener {
                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (all) {
                                callback.invoke(origin, true, true)
                            } else {
                                callback.invoke(origin, true, false)
                        }
                    }
                })
        })
    }

    override fun onGeolocationPermissionsHidePrompt() {

    }

    override fun onJsAlert(view: XWebView, url: String, message: String?, result: JsResult): Boolean {
        DialogUtils.showAlert(getXActivity(), title = url, content = message, positiveListener = {
            result.confirm()
        })
        LogUtils.d("onJsAlert")
        return true
    }

    override fun onJsConfirm(webView: XWebView, url: String, message: String?, result: JsResult): Boolean {
        DialogUtils.showAlert(getXActivity(), title = url, content = message, negativeListener = {
            result.cancel()
        }, positiveListener = {
            result.confirm()
        })
        LogUtils.d("onJsConfirm$url")
        return true
    }

    override fun onJsPrompt(webView: XWebView, url: String, message: String?, defaultValue: String?, result: JsPromptResult): Boolean {
        DialogUtils.showInput(getXActivity(), url, message, negativeListener = {
            result.cancel()
        }, positiveListener = { input, _ ->
            result.confirm(input)
        })
        LogUtils.d("onJsPrompt", message, defaultValue)
        return true
    }

    override fun onJsBeforeUnload(webView: XWebView, url: String, message: String?, result: JsResult): Boolean {
        LogUtils.d("onJsBeforeUnload")
        return false
    }

    override fun onCreateWindow(webView: XWebView, dialog: Boolean, userGesture: Boolean, resultMsg: Message?): Boolean {
        return false
    }

    override fun onCloseWindow(webView: XWebView) {

    }

    override fun onRequestFocus(webView: XWebView) {
        webView.requestFocus()
        webView.requestFocusFromTouch()
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
    }

    override fun onPermissionRequest(request: PermissionRequest) {

    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {

    }

    override fun onShowFileChooser(
        webView: XWebView, filePathCallback: ValueCallback<Array<Uri>?>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ): Boolean {
        mFilePathCallback?.onReceiveValue(null)
        mFilePathCallback = filePathCallback
        startActivityForResult(fileChooserParams.createIntent(), REQUEST_CODE_FILE_CHOOSER)
        return true
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        LogUtils.d(consoleMessage?.message())
        return true
    }

    override fun onResume() {
        super.onResume()
        mBinding.webView.onResume()
    }

    override fun onPause() {
        mBinding.webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mBinding.webView.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (null != mCustomViewCallback && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mCustomViewCallback?.onCustomViewHidden()
            mCustomViewCallback = null
        } else if (mBinding.webView.canGoBack()) {
            mBinding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FILE_CHOOSER) {
            mFilePathCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data))
            mFilePathCallback = null
        } else {
            mFilePathCallback?.onReceiveValue(null)
        }
    }

    @SuppressLint("SwitchIntDef")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            }
        }
    }

    @Parcelize
    data class Builder(val title: String = "", val url: String = "", val data: String = "") : Parcelable




}