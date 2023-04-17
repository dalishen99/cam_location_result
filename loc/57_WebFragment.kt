package com.lhx.glakit.web

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.RelativeLayout
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import com.lhx.glakit.R
import com.lhx.glakit.base.fragment.BaseFragment
import com.lhx.glakit.base.widget.BaseContainer
import com.lhx.glakit.utils.StringUtils
import com.lhx.glakit.widget.ProgressBar

/**
 * 网页配置
 */
object WebConfig {
    const val URL = "url" //要打开的链接

    const val HTML_STRING = "html" //要加载的html

    const val TITLE = "title" //默认显示的标题

    const val USE_WEB_TITLE = "useWebTitle" //是否使用网页标题 默认使用

    const val DISPLAY_PROGRESS = "display_progress" //是否显示进度条 默认显示

    const val DISPLAY_INDICATOR = "display_indicator" //是否显示菊花，默认不显示

    const val HIDE_BAR = "hideBar" //是否隐藏导航栏 默认不隐藏

    const val GO_BACK_ENABLED = "goBackEnabled" //是否可以返回上一级网页，默认可以
}

/**
 * 网页
 */
@Suppress("SetJavaScriptEnabled", "unused_parameter", "deprecation")
open class WebFragment : BaseFragment() {

    companion object{

        //文件选择
        private const val FILE_CHOOSER_REQUEST_CODE = 1101
    }

    //是否需要使用网页标题
    protected var shouldUseWebTitle = true

    //html
    var htmlString: String? = null

    //链接
    protected var toBeOpenedURL: String? = null

    //原始链接
    protected var originURL: String? = null

    //原始标题
    protected var originTitle: String? = null

    //是否显示加载进度条
    protected var shouldDisplayProgress = true

    //是否显示菊花 与进度条互斥
    protected var shouldDisplayIndicator = false

    //是否隐藏导航栏
    protected var hideTitleBar = false

    //是否可以返回
    protected var goBackEnabled = true

    protected val webView: CustomWebView by lazy {
        CustomWebView(requireContext())
    }
    protected val progressBar: ProgressBar by lazy {
        ProgressBar(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        hideTitleBar = getBooleanFromBundle(WebConfig.HIDE_BAR, false)
        super.onCreate(savedInstanceState)
    }

    @CallSuper
    override fun initialize(inflater: LayoutInflater, container: BaseContainer, saveInstanceState: Bundle?) {

        setContainerContentView(webView)
        container.addView(progressBar)
        val params = progressBar.layoutParams as RelativeLayout.LayoutParams
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = pxFromDip(2f)
        params.topMargin = if (showTitleBar()) getDimenIntValue(R.dimen.title_bar_height) else 0

        shouldUseWebTitle = getBooleanFromBundle(WebConfig.USE_WEB_TITLE, true)
        shouldDisplayProgress = getBooleanFromBundle(WebConfig.DISPLAY_PROGRESS, true)
        shouldDisplayIndicator = getBooleanFromBundle(WebConfig.DISPLAY_INDICATOR, false)
        goBackEnabled = getBooleanFromBundle(WebConfig.GO_BACK_ENABLED, true)

        webConfig()

        if (shouldDisplayIndicator) {
            shouldDisplayProgress = false
        }
        if (StringUtils.isEmpty(toBeOpenedURL)) {
            var url = getStringFromBundle(WebConfig.URL)

            //没有 scheme 的加上
            if (!TextUtils.isEmpty(url)) {
                if (!url!!.contains("//")) {
                    url = "http://$url"
                }
            }
            toBeOpenedURL = url
        }
        if (StringUtils.isEmpty(htmlString)) {
            htmlString = getStringFromBundle(WebConfig.HTML_STRING)
        }

        val title = getStringFromBundle(WebConfig.TITLE)
        if (!TextUtils.isEmpty(title)) {
            setBarTitle(title)
        }

        originURL = toBeOpenedURL
        originTitle = title

        loadWebContent()
    }

    override fun showTitleBar(): Boolean {
        return !hideTitleBar
    }

    //配置webView
    private fun webConfig(){

        progressBar.progressColor = getColorCompat(R.color.web_progress_color)
        webView.also {
            it.webChromeClient = webChromeClient
            it.webViewClient = webViewClient

            val settings = it.settings
            val userAgent = getCustomUserAgent()
            if (!StringUtils.isEmpty(userAgent)) {
                settings.userAgentString = "${settings.userAgentString} $userAgent"
            }

            settings.useWideViewPort = true // 设置此属性，可任意比例缩放,将图片调整到适合webView的大小

            // 便页面支持缩放：
            settings.javaScriptEnabled = true
            settings.builtInZoomControls = false
            settings.setSupportZoom(false)
            settings.defaultFontSize = 12
            settings.defaultFixedFontSize = 12

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                //适配5.0不允许http和https混合使用情况
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                it.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            } else {
                it.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }

            settings.setSupportMultipleWindows(false)
            settings.allowFileAccess = true

            // 通过 file url 加载的 Javascript 读取其他的本地文件 .建议关闭
            settings.allowFileAccessFromFileURLs = false
            // 允许通过 file url 加载的 Javascript 可以访问其他的源，包括其他的文件和 http，https 等其他的源
            settings.allowUniversalAccessFromFileURLs = false

            //settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL); // 支持内容重新布局
            settings.loadWithOverviewMode = true // 缩放至屏幕的大小
            settings.loadsImagesAutomatically = true // 支持自动加载图片
            settings.blockNetworkImage = false
            settings.defaultTextEncodingName = "utf-8" //设置编码格式
            settings.domStorageEnabled = true //设置适应Html5

            //settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); //设置缓存
            settings.databaseEnabled = true
            settings.setGeolocationEnabled(true)

            //设置定位的数据库路径
            val dir = context?.applicationContext?.getDir("database", Context.MODE_PRIVATE)?.path
            settings.setGeolocationDatabasePath(dir)
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    //加载
    fun loadWebContent() {
        if (!StringUtils.isEmpty(toBeOpenedURL) || !StringUtils.isEmpty(htmlString)) {
            if (!StringUtils.isEmpty(toBeOpenedURL)) {
                webView.loadUrl(toBeOpenedURL!!)
            } else {
                var html = htmlString
                if (shouldAddMobileMeta()) {
                    html = "<style>img {width:100%;}</style><meta name='viewport' content='width=device-width, initial-scale=1'/>${htmlString}"
                }
                webView.loadDataWithBaseURL(null, html!!, "text/html", "utf8", null)
            }
        }
    }

    //是否添加移动设备头部
    fun shouldAddMobileMeta(): Boolean {
        return true
    }

    fun showNavigationBar(): Boolean {
        return true
    }

    //调用js
    fun evaluateJavascript(js: String?) {
        if (StringUtils.isEmpty(js)) return
        webView.evaluateJavascript(js!!, null)
    }

    //上传文件回调
    private var mUploadFileCallback: ValueCallback<Array<Uri>>? = null
    private var mUploadMsg: ValueCallback<Uri>? = null

    //
    protected var webChromeClient: WebChromeClient = object : WebChromeClient() {
        override fun onReceivedTitle(view: WebView, title: String) {
            if (shouldUseWebTitle) {
                if (showNavigationBar()) {
                    setBarTitle(title)
                }
            }
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            if (shouldDisplayProgress) {
                progressBar.setProgress(newProgress / 100.0f)
            }
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
            //定位权限
            callback.invoke(origin, true, false)
        }

        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            return super.onJsAlert(view, url, message, result)
        }

        //4.0 - 5.0
        fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String?) {
            mUploadMsg = uploadMsg
            openFileChooser(acceptType)
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {

            //上传文件
            mUploadFileCallback = filePathCallback
            var type: String? = null
            val types = fileChooserParams.acceptTypes
            if (!types.isNullOrEmpty()) {
                val builder = StringBuilder()
                for ((i, str) in types.withIndex()) {
                    builder.append(str)
                    if (i < types.size - 1) {
                        builder.append(",")
                    }
                }
                type = builder.toString()
            }
            openFileChooser(type)
            return true
        }
    }

    //打开文件选择器
    private fun openFileChooser(mineType: String?) {
        var type: String? = mineType
        if (StringUtils.isEmpty(mineType)) {
            type = "*/*"
        }
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = type
            startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    //是否加载出错了
    var hasError = false

    //
    protected var webViewClient: WebViewClient = object : WebViewClient() {

        @Deprecated("Deprecated in Java", ReplaceWith("shouldOpenURL(url)"))
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return shouldOpenURL(url)
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (shouldDisplayIndicator) {
                setPageLoading(false)
                shouldDisplayIndicator = false
            }
            if(!hasError){
                setPageLoadFail(false)
            }
            onPageFinish(url)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            hasError = false
            if (shouldDisplayIndicator) {
                setPageLoading(true)
            }
            if(shouldDisplayProgress){
                progressBar.bringToFront()
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            when (errorCode) {
                ERROR_HOST_LOOKUP,
                ERROR_AUTHENTICATION,
                ERROR_PROXY_AUTHENTICATION,
                ERROR_CONNECT,
                ERROR_IO,
                ERROR_TIMEOUT,
                ERROR_FAILED_SSL_HANDSHAKE,
                ERROR_BAD_URL -> {
                    hasError = true
                    setPageLoadFail(true)
                }
            }
        }
    }

    override fun onReloadPage() {
        webView.reload()
    }

    override fun onBack() {
        if (!hideTitleBar && goBackEnabled && webView.canGoBack()) {
            webView.goBack()
            return
        }
        super.onBack()
    }

    override fun back() {
        onBack()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                FILE_CHOOSER_REQUEST_CODE -> {

                    //文件选择完成
                    if (data != null) {
                        val uri = data.data
                        if (uri != null) {
                            if (mUploadFileCallback != null) {
                                mUploadFileCallback!!.onReceiveValue(arrayOf(uri))
                            } else if (mUploadMsg != null) {
                                mUploadMsg!!.onReceiveValue(uri)
                            }
                        }
                        mUploadFileCallback = null
                        mUploadMsg = null
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    //加载完成
    protected fun onPageFinish(url: String?) {}

    //当前url是否可以打开
    fun shouldOpenURL(url: String): Boolean {

        return if (!StringUtils.isEmpty(url)) {
            url.startsWith("http://") || url.startsWith("https://")
        } else true
    }

    //返回需要设置的自定义 userAgent 会拼在系统的userAgent后面
    fun getCustomUserAgent(): String? {
        return null
    }
}