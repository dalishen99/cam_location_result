@file:Suppress("KDocUnresolvedReference")

package nl.arnhem.flash.web

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Environment
import android.os.Message
import android.util.AttributeSet
import android.util.Base64
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import kotlinx.android.synthetic.main.activity_browser.view.*
import nl.arnhem.flash.contracts.MEDIA_CHOOSER_RESULT
import nl.arnhem.flash.facebook.FbCookie
import nl.arnhem.flash.facebook.FbItem
import nl.arnhem.flash.facebook.formattedFbUrl
import nl.arnhem.flash.injectors.*
import nl.arnhem.flash.utils.*
import nl.arnhem.flash.utils.iab.IS_Flash_PRO
import org.jetbrains.anko.withAlpha
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.util.*


// return value change of decodeBase64 method

@Suppress("OverridingDeprecatedMember", "DEPRECATION", "NAME_SHADOWING", "KDocUnresolvedReference")
class WebViewFlash : NestedWebView {

    interface Listener {
        fun onPageStarted(url: String, favicon: Bitmap?)

        fun onPageFinished(url: String)

        fun onPageError(errorCode: Int, description: String, failingUrl: String)

        fun onDownloadRequested(url: String, suggestedFilename: String, mimeType: String, contentLength: Long, contentDisposition: String, userAgent: String)

        fun onExternalPageRequest(url: String)
    }

    private lateinit var mActivity: WeakReference<Activity>
    private var mFragment: WeakReference<Fragment>? = null
    private var mListener: Listener? = null
    private val mPermittedHostnames: MutableList<String> = LinkedList()
    /**
     * File upload callback for platform versions prior to Android 5.0
     */
    private var mFileUploadCallbackFirst: ValueCallback<Array<Uri>>? = null
    /**
     * File upload callback for Android 5.0+
     */
    private lateinit var mFileUploadCallbackSecond: ValueCallback<Array<Uri?>>
    private var filePathCallback: ValueCallback<Array<Uri>?>? = null
    private var mLastError: Long = 0
    private var mLanguageIso3: String? = null
    private var mRequestCodeFilePicker = REQUEST_CODE_FILE_PICKER
    private var mCustomWebViewClient: WebViewClient? = null
    private var mCustomWebChromeClient: WebChromeClient? = null
    private var mGeolocationEnabled: Boolean = false
    private var mUploadableFileTypes = "*/*"
    private val mHttpHeaders: MutableMap<String, String> = HashMap()
    private inline fun v(crossinline message: () -> Any?) = L.v { "web client: ${message()}" }

    init {
        isNestedScrollingEnabled = true
    }

    val currentUrl: String
        get() = url ?: ""

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    override fun setWebViewClient(client: WebViewClient?) {
        mCustomWebViewClient = client
    }

    override fun setWebChromeClient(client: WebChromeClient?) {
        mCustomWebChromeClient = client
    }

    /**
     * Loads and displays the provided HTML source text

     * @param html       the HTML source text to load
     * *
     * @param baseUrl    the URL to use as the page's base URL
     * *
     * @param historyUrl the URL to use for the page's history entry
     * *
     * @param encoding   the encoding or charset of the HTML source text
     */

    @SuppressLint("NewApi")
    override fun onResume() {
            super.onResume()
        resumeTimers()
    }

    @SuppressLint("NewApi")
    override fun onPause() {
        pauseTimers()
            super.onPause()
    }

    /**
     * Removes one of the HTTP headers that have previously been added via `addHttpHeader()`
     *
     *
     * If you want to unset a pre-defined header, set it to an empty string with `addHttpHeader()` instead
     *
     *
     * The `WebView` implementation may in some cases overwrite headers that you set or unset

     * @param name the name of the HTTP header to remove
     */

    fun onBackPressed(): Boolean {
        return if (canGoBack()) {
            goBack()
            false
        } else {
            true
        }
    }


    @SuppressLint("NewApi")
    private fun setMixedContentAllowed(webSettings: WebSettings, allowed: Boolean) {
        if (Build.VERSION.SDK_INT >= 21) {
            webSettings.mixedContentMode = if (allowed) WebSettings.MIXED_CONTENT_ALWAYS_ALLOW else WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
    }

    fun setDesktopMode(enabled: Boolean) {
        val webSettings = settings

        val newUserAgent: String
        newUserAgent = if (enabled) {
            webSettings.userAgentString.replace("Mobile", "eliboM").replace("Android", "diordnA")
        } else {
            webSettings.userAgentString.replace("eliboM", "Mobile").replace("diordnA", "Android")
        }

        webSettings.userAgentString = newUserAgent
        webSettings.useWideViewPort = enabled
        webSettings.loadWithOverviewMode = enabled
        webSettings.setSupportZoom(enabled)
        webSettings.builtInZoomControls = enabled
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun init(context: Context) {
        // in IDE's preview mode
        if (isInEditMode) {
            // do not run the code from this method
            return
        }

        if (context is Activity) {
            mActivity = WeakReference(context)
        }

        mLanguageIso3 = languageIso3

        isFocusable = true
        isFocusableInTouchMode = true

        isSaveEnabled = true


        val webSettings = settings
        webSettings.allowFileAccess = false
        setAllowAccessFromFileUrls(webSettings, false)
        webSettings.builtInZoomControls = false
        webSettings.javaScriptEnabled = true


        super.setWebViewClient(object : WebViewClient() {

            private fun injectBackgroundColor() {
                webview.setBackgroundColor(
                        when {
                            url.isFacebookUrl -> Prefs.bgColor.withAlpha(255)
                            else -> Color.WHITE
                        }
                )
            }

            override fun onPageCommitVisible(view: WebView, url: String?) {
                super.onPageCommitVisible(view, url)
                injectBackgroundColor()
                if (url.isFacebookUrl)
                    view.jsInject(
                            CssAssets.ROUND_ICONS.maybe(Prefs.showRoundedIcons),
                            //CssStatus.COMPOSER_STATUS,
                            CssHider.CORE,
                            CssHider.COMPOSER.maybe(!Prefs.showComposer),
                            CssHider.STORIESTRAY.maybe(!Prefs.showStoriesTray),
                            CssFixed.COMPOSER_FIXED.maybe(Prefs.fixedComposer),
                            CssPadding.COMPOSER_PADDING.maybe(Prefs.paddingComposer),
                            CssHider.COMPOSER_BOTTOM.maybe(Prefs.bottomComposer),
                            CssHider.PEOPLE_YOU_MAY_KNOW.maybe(!Prefs.showSuggestedFriends && IS_Flash_PRO),
                            CssHider.SUGGESTED_GROUPS.maybe(!Prefs.showSuggestedGroups && IS_Flash_PRO),
                            Prefs.themeInjector,
                            CssHider.VIDEOS,
                            CssHider.VIDEOS2,
                            //CssHider.WWW_FACEBOOK_COM,
                            CssHider.HEADER_TOP,
                            CssHider.HEADER,
                            JsAssets.DOCUMENT_WATCHER,
                            JsAssets.HEADER_HIDER,
                            JsAssets.CLICK_A,
                            CssHider.ADS.maybe(!Prefs.showFacebookAds && IS_Flash_PRO),
                            JsAssets.CONTEXT_A,
                            JsActions.AUDIO_OFF.maybe(Prefs.DisableAudio),
                            //JsActions.AUTO_VIDEO_OFF.maybe(!Prefs.DisableVideoAUTO),
                            JsAssets.MEDIA)
            }

            fun onPageFinishedActions(url: String) {
                if (url.startsWith("${FbItem.MESSAGES.url}/read/") && Prefs.messageScrollToBottom)
                    webview.pageDown(true)
                injectAndFinish()
            }

            fun injectAndFinish() {
                v { "page finished reveal" }
                injectBackgroundColor()
                webview.jsInject(
                        JsActions.LOGIN_CHECK,
                        JsAssets.TEXTAREA_LISTENER)
            }

            /**
             * Helper to format the request and launch it
             * returns true to override the url
             * returns false if we are already in an overlaying activity
             */
            private fun launchRequest(request: WebResourceRequest): Boolean {
                v { "Launching url: ${request.url}" }
                return webview.requestWebOverlay(request.url.toString())
            }

            private fun launchImage(url: String, text: String? = null, title: String? = null, cookie: String? = null): Boolean {
                v { "Launching image: $url" }
                webview.context.launchImageActivity(url, text, title, cookie)
                if (webview.canGoBack()) webview.goBack()
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (!hasError()) {
                    if (mListener != null) {
                        mListener!!.onPageStarted(url, favicon)
                    }
                }

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onPageStarted(view, url, favicon)
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!hasError()) {
                    if (mListener != null) {
                        mListener!!.onPageFinished(url)
                    }
                }
                onPageFinishedActions(url)
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onPageFinished(view, url)
                }
            }

            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                setLastError()

                if (mListener != null) {
                    mListener!!.onPageError(errorCode, description, failingUrl)
                }

                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onReceivedError(view, errorCode, description, failingUrl)
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                // if the hostname may not be accessed
                if (!isHostnameAllowed(url)) {
                    // if a listener is available
                    if (mListener != null) {
                        // inform the listener about the request
                        mListener!!.onExternalPageRequest(url)
                    }

                    // cancel the original request
                    return true
                }

                // if there is a user-specified handler available
                if (mCustomWebViewClient != null) {
                    // if the user-specified handler asks to override the request
                    if (mCustomWebViewClient!!.shouldOverrideUrlLoading(view, url)) {
                        // cancel the original request
                        return true
                    }
                }

                // route the request through the custom URL loading method
                view.loadUrl(url)

                // cancel the original request
                return true
            }

            override fun onLoadResource(view: WebView, url: String) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onLoadResource(view, url)
                } else {
                    super.onLoadResource(view, url)
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                v { "Url loading: ${request.url}" }
                val path = request.url?.path ?: return super.shouldOverrideUrlLoading(view, request)
                v { "Url path $path" }
                val url = request.url.toString()
                if (url.isExplicitIntent) {
                    view.context.resolveActivityForUri(request.url)
                    return true
                }
                if (path.startsWith("/composer/")) return launchRequest(request)
                if (url.isImageUrl)
                    return launchImage(url.formattedFbUrl)
                if (url.isIndirectImageUrl)
                    return launchImage(url.formattedFbUrl, view.title, cookie = FbCookie.webCookie)
                if (Prefs.linksInDefaultApp && view.context.resolveActivityForUri(request.url)) return true
                return super.shouldOverrideUrlLoading(view, request)
            }

            @SuppressLint("NewApi")
            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                return if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.shouldInterceptRequest(view, url)
                } else {
                    super.shouldInterceptRequest(view, url)
                }
            }

            @SuppressLint("NewApi")
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                return if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient!!.shouldInterceptRequest(view, request)
                    } else {
                        super.shouldInterceptRequest(view, request)
                    }
                } else {
                    null
                }
            }

            override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onFormResubmission(view, dontResend, resend)
                } else {
                    super.onFormResubmission(view, dontResend, resend)
                }
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.doUpdateVisitedHistory(view, url, isReload)
                } else {
                    super.doUpdateVisitedHistory(view, url, isReload)
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onReceivedSslError(view, handler, error)
                } else {
                    super.onReceivedSslError(view, handler, error)
                }
            }

            @SuppressLint("NewApi")
            override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebViewClient != null) {
                        mCustomWebViewClient!!.onReceivedClientCertRequest(view, request)
                    } else {
                        super.onReceivedClientCertRequest(view, request)
                    }
                }
            }

            override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onReceivedHttpAuthRequest(view, handler, host, realm)
                } else {
                    super.onReceivedHttpAuthRequest(view, handler, host, realm)
                }
            }

            override fun shouldOverrideKeyEvent(view: WebView, event: KeyEvent): Boolean {
                return if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.shouldOverrideKeyEvent(view, event)
                } else {
                    super.shouldOverrideKeyEvent(view, event)
                }
            }

            override fun onUnhandledKeyEvent(view: WebView, event: KeyEvent) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onUnhandledKeyEvent(view, event)
                } else {
                    super.onUnhandledKeyEvent(view, event)
                }
            }

            override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onScaleChanged(view, oldScale, newScale)
                } else {
                    super.onScaleChanged(view, oldScale, newScale)
                }
            }

            @SuppressLint("NewApi")
            override fun onReceivedLoginRequest(view: WebView, realm: String, account: String?, args: String) {
                if (mCustomWebViewClient != null) {
                    mCustomWebViewClient!!.onReceivedLoginRequest(view, realm, account, args)
                } else {
                    super.onReceivedLoginRequest(view, realm, account, args)
                }
            }

        })

        super.setWebChromeClient(object : WebChromeClient() {

            // file upload callback (Android 5.0 (API level 21) -- current) (public method)
            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri?>>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
                openFileChooser(null, filePathCallback, true)
                return true
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onProgressChanged(view, newProgress)
                } else {
                    super.onProgressChanged(view, newProgress)
                }
            }

            override fun onReceivedTitle(view: WebView, title: String) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onReceivedTitle(view, title)
                } else {
                    super.onReceivedTitle(view, title)
                }
            }

            override fun onReceivedIcon(view: WebView, icon: Bitmap) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onReceivedIcon(view, icon)
                } else {
                    super.onReceivedIcon(view, icon)
                }
            }

            override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onReceivedTouchIconUrl(view, url, precomposed)
                } else {
                    super.onReceivedTouchIconUrl(view, url, precomposed)
                }
            }

            override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onShowCustomView(view, callback)
                } else {
                    super.onShowCustomView(view, callback)
                }
            }

            @SuppressLint("NewApi")
            override fun onShowCustomView(view: View, requestedOrientation: Int, callback: WebChromeClient.CustomViewCallback) {
                if (Build.VERSION.SDK_INT >= 14) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient!!.onShowCustomView(view, requestedOrientation, callback)
                    } else {
                        super.onShowCustomView(view, requestedOrientation, callback)
                    }
                }
            }

            override fun onHideCustomView() {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onHideCustomView()
                } else {
                    super.onHideCustomView()
                }
            }

            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                return if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                } else {
                    super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
                }
            }

            override fun onRequestFocus(view: WebView) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onRequestFocus(view)
                } else {
                    super.onRequestFocus(view)
                }
            }

            override fun onCloseWindow(window: WebView) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onCloseWindow(window)
                } else {
                    super.onCloseWindow(window)
                }
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                return if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onJsAlert(view, url, message, result)
                } else {
                    super.onJsAlert(view, url, message, result)
                }
            }

            override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
                return if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onJsConfirm(view, url, message, result)
                } else {
                    super.onJsConfirm(view, url, message, result)
                }
            }

            override fun onJsPrompt(view: WebView, url: String, message: String, defaultValue: String, result: JsPromptResult): Boolean {
                return if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onJsPrompt(view, url, message, defaultValue, result)
                } else {
                    super.onJsPrompt(view, url, message, defaultValue, result)
                }
            }

            override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean {
                return if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onJsBeforeUnload(view, url, message, result)
                } else {
                    super.onJsBeforeUnload(view, url, message, result)
                }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                if (mGeolocationEnabled) {
                    callback.invoke(origin, true, false)
                } else {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient!!.onGeolocationPermissionsShowPrompt(origin, callback)
                    } else {
                        super.onGeolocationPermissionsShowPrompt(origin, callback)
                    }
                }
            }

            override fun onGeolocationPermissionsHidePrompt() {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onGeolocationPermissionsHidePrompt()
                } else {
                    super.onGeolocationPermissionsHidePrompt()
                }
            }

            @SuppressLint("NewApi")
            override fun onPermissionRequest(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient!!.onPermissionRequest(request)
                    } else {
                        super.onPermissionRequest(request)
                    }
                }
            }

            @SuppressLint("NewApi")
            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                if (Build.VERSION.SDK_INT >= 21) {
                    if (mCustomWebChromeClient != null) {
                        mCustomWebChromeClient!!.onPermissionRequestCanceled(request)
                    } else {
                        super.onPermissionRequestCanceled(request)
                    }
                }
            }

            override fun onJsTimeout(): Boolean {
                return if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onJsTimeout()
                } else {
                    super.onJsTimeout()
                }
            }

            override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onConsoleMessage(message, lineNumber, sourceID)
                } else {
                    super.onConsoleMessage(message, lineNumber, sourceID)
                }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                return if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onConsoleMessage(consoleMessage)
                } else {
                    super.onConsoleMessage(consoleMessage)
                }
            }

            override fun getDefaultVideoPoster(): Bitmap? {
                return if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.defaultVideoPoster
                } else {
                    super.getDefaultVideoPoster()
                }
            }

            override fun getVideoLoadingProgressView(): View? {
                return if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.videoLoadingProgressView
                } else {
                    super.getVideoLoadingProgressView()
                }
            }

            override fun getVisitedHistory(callback: ValueCallback<Array<String>>) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.getVisitedHistory(callback)
                } else {
                    super.getVisitedHistory(callback)
                }
            }

            override fun onExceededDatabaseQuota(url: String, databaseIdentifier: String, quota: Long, estimatedDatabaseSize: Long, totalQuota: Long, quotaUpdater: WebStorage.QuotaUpdater) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater)
                } else {
                    super.onExceededDatabaseQuota(url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater)
                }
            }

            override fun onReachedMaxAppCacheSize(requiredStorage: Long, quota: Long, quotaUpdater: WebStorage.QuotaUpdater) {
                if (mCustomWebChromeClient != null) {
                    mCustomWebChromeClient!!.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater)
                } else {
                    super.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater)
                }
            }

        })

        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            val suggestedFilename = URLUtil.guessFileName(url, contentDisposition, mimeType)

            if (mListener != null) {
                mListener!!.onDownloadRequested(url, suggestedFilename, mimeType, contentLength, contentDisposition, userAgent)
            }
        }
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>?) {
        var additionalHttpHeaders = additionalHttpHeaders
        if (additionalHttpHeaders == null) {
            additionalHttpHeaders = mHttpHeaders
        } else if (mHttpHeaders.isNotEmpty()) {
            additionalHttpHeaders.putAll(mHttpHeaders)
        }

        super.loadUrl(url, additionalHttpHeaders)
    }

    override fun loadUrl(url: String) {
        if (mHttpHeaders.isNotEmpty()) {
            super.loadUrl(url, mHttpHeaders)
        } else {
            super.loadUrl(url)
        }
    }

    fun loadUrl(url: String, preventCaching: Boolean) {
        var url = url
        if (preventCaching) {
            url = makeUrlUnique(url)
        }

        loadUrl(url)
    }

    fun loadUrl(url: String, preventCaching: Boolean, additionalHttpHeaders: MutableMap<String, String>) {
        var url = url
        if (preventCaching) {
            url = makeUrlUnique(url)
        }

        loadUrl(url, additionalHttpHeaders)
    }

    override fun loadData(data: String, mimeType: String, encoding: String) {
        loadData(data, mimeType, encoding)
    }

    override fun loadDataWithBaseURL(baseUrl: String, data: String, mimeType: String, encoding: String, historyUrl: String) {
        loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }

    private fun isHostnameAllowed(url: String): Boolean {
        // if the permitted hostnames have not been restricted to a specific set
        if (mPermittedHostnames.size == 0) {
            // all hostnames are allowed
            return true
        }

        // get the actual hostname of the URL that is to be checked
        val actualHost = Uri.parse(url).host

        // for every hostname in the set of permitted hosts

        // the actual hostname of the URL to be checked is not allowed since there were no matches
        // the actual hostname of the URL to be checked is allowed
        return mPermittedHostnames.any {
            // if the two hostnames match or if the actual host is a subdomain of the expected host
            actualHost == it || actualHost.endsWith(".$it")
        }
    }

    private fun setLastError() {
        mLastError = System.currentTimeMillis()
    }

    private fun hasError(): Boolean {
        return mLastError + 500 >= System.currentTimeMillis()
    }

    /**
     * Provides localizations for the 25 most widely spoken languages that have a ISO 639-2/T code
     */
    private fun getFileUploadPromptLabel(): String {
        try {
            when (mLanguageIso3) {
                "zho" -> return decodeBase64("6YCJ5oup5LiA5Liq5paH5Lu2")
                "spa" -> return decodeBase64("RWxpamEgdW4gYXJjaGl2bw==")
                "hin" -> return decodeBase64("4KSP4KSVIOCkq+CkvOCkvuCkh+CksiDgpJrgpYHgpKjgpYfgpII=")
                "ben" -> return decodeBase64("4KaP4KaV4Kaf4Ka/IOCmq+CmvuCmh+CmsiDgpqjgpr/gprDgp43gpqzgpr7gpprgpqg=")
                "ara" -> return decodeBase64("2KfYrtiq2YrYp9ixINmF2YTZgSDZiNin2K3Yrw==")
                "por" -> return decodeBase64("RXNjb2xoYSB1bSBhcnF1aXZv")
                "rus" -> return decodeBase64("0JLRi9Cx0LXRgNC40YLQtSDQvtC00LjQvSDRhNCw0LnQuw==")
                "jpn" -> return decodeBase64("MeODleOCoeOCpOODq+OCkumBuOaKnuOBl+OBpuOBj+OBoOOBleOBhA==")
                "pan" -> return decodeBase64("4KiH4Kmx4KiVIOCoq+CovuCoh+CosiDgqJrgqYHgqKPgqYs=")
                "deu" -> return decodeBase64("V8OkaGxlIGVpbmUgRGF0ZWk=")
                "jav" -> return decodeBase64("UGlsaWggc2lqaSBiZXJrYXM=")
                "msa" -> return decodeBase64("UGlsaWggc2F0dSBmYWls")
                "tel" -> return decodeBase64("4LCS4LCVIOCwq+CxhuCxluCwsuCxjeCwqOCxgSDgsI7gsILgsJrgsYHgsJXgsYvgsILgsKHgsL8=")
                "vie" -> return decodeBase64("Q2jhu41uIG3hu5l0IHThuq1wIHRpbg==")
                "kor" -> return decodeBase64("7ZWY64KY7J2YIO2MjOydvOydhCDshKDtg50=")
                "fra" -> return decodeBase64("Q2hvaXNpc3NleiB1biBmaWNoaWVy")
                "mar" -> return decodeBase64("4KSr4KS+4KSH4KSyIOCkqOCkv+CkteCkoeCkvg==")
                "tam" -> return decodeBase64("4K6S4K6w4K+BIOCuleCvh+CuvuCuquCvjeCuquCviCDgrqTgr4fgrrDgr43grrXgr4E=")
                "urd" -> return decodeBase64("2KfbjNqpINmB2KfYptmEINmF24zauiDYs9uSINin2YbYqtiu2KfYqCDaqdix24zaug==")
                "fas" -> return decodeBase64("2LHYpyDYp9mG2KrYrtin2Kgg2qnZhtuM2K8g24zaqSDZgdin24zZhA==")
                "tur" -> return decodeBase64("QmlyIGRvc3lhIHNlw6dpbg==")
                "ita" -> return decodeBase64("U2NlZ2xpIHVuIGZpbGU=")
                "tha" -> return decodeBase64("4LmA4Lil4Li34Lit4LiB4LmE4Lif4Lil4LmM4Lir4LiZ4Li24LmI4LiH")
                "guj" -> return decodeBase64("4KqP4KqVIOCqq+CqvuCqh+CqsuCqqOCrhyDgqqrgqrjgqoLgqqY=")
            }
        } catch (ignored: Exception) {
        }
        // return English translation by default
        return "Choose a file"
    }

    override fun openFileChooser(filePathCallback: ValueCallback<Array<Uri>?>?, fileChooserParams: ValueCallback<Array<Uri?>>, b: Boolean) {
        val intent = Intent("android.intent.action.PICK")
        intent.type = mUploadableFileTypes
        intent.action = Intent.ACTION_GET_CONTENT
        mActivity.get()!!.startActivityForResult(Intent.createChooser(intent, getFileUploadPromptLabel()), MEDIA_CHOOSER_RESULT)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean {
        L.d { "FileChooser On activity results web $requestCode" }
        if (requestCode != MEDIA_CHOOSER_RESULT) return false
        val data = intent?.data
        filePathCallback?.onReceiveValue(if (data != null) arrayOf(data) else null)
        filePathCallback = null
        return true
    }

    /**
     * Wrapper for methods related to alternative browsers that have their own rendering engines
     */
    object Browsers {

        /**
         * Package name of an alternative browser that is installed on this device
         */
        private var mAlternativePackage: String? = null

        /**
         * Returns whether there is an alternative browser with its own rendering engine currently installed

         * @param context a valid `Context` reference
         * *
         * @return whether there is an alternative browser or not
         */
        fun hasAlternative(context: Context): Boolean {
            return getAlternative(context) != null
        }

        /**
         * Returns the package name of an alternative browser with its own rendering engine or `null`

         * @param context a valid `Context` reference
         * *
         * @return the package name or `null`
         */
        private fun getAlternative(context: Context): String? {
            if (mAlternativePackage != null) {
                return mAlternativePackage
            }

            val alternativeBrowsers = Arrays.asList(*ALTERNATIVE_BROWSERS)
            val apps = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in apps) {
                if (!app.enabled) {
                    continue
                }

                if (alternativeBrowsers.contains(app.packageName)) {
                    mAlternativePackage = app.packageName

                    return app.packageName
                }
            }

            return null
        }

        /**
         * Opens the given URL in an alternative browser

         * @param context           a valid `Activity` reference
         * *
         * @param url               the URL to open
         * *
         * @param withoutTransition whether to switch to the browser `Activity` without a transition
         */
        @JvmOverloads
        fun openUrl(context: Activity, url: String, withoutTransition: Boolean = false) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.`package` = getAlternative(context)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)

            if (withoutTransition) {
                context.overridePendingTransition(0, 0)
            }
        }
    }

    /**
     * Opens the given URL in an alternative browser

     * @param context a valid `Activity` reference
     * *
     * @param url     the URL to open
     */

    companion object {

        private const val PACKAGE_NAME_DOWNLOAD_MANAGER = "com.android.providers.downloads"
        const val REQUEST_CODE_FILE_PICKER = 67
        private const val LANGUAGE_DEFAULT_ISO3 = "eng"
        /**
         * Alternative browsers that have their own rendering engine and *may* be installed on this device
         */
        private val ALTERNATIVE_BROWSERS = arrayOf("org.mozilla.firefox", "com.android.chrome", "com.opera.browser", "org.mozilla.firefox_beta", "com.chrome.beta", "com.opera.browser.beta")

        @SuppressLint("NewApi")
        private fun setAllowAccessFromFileUrls(webSettings: WebSettings, allowed: Boolean) {
            if (Build.VERSION.SDK_INT >= 16) {
                webSettings.allowFileAccessFromFileURLs = allowed
                webSettings.allowUniversalAccessFromFileURLs = allowed
            }
        }

        private fun makeUrlUnique(url: String): String {
            val unique = StringBuilder()
            unique.append(url)

            if (url.contains("?")) {
                unique.append('&')
            } else {
                if (url.lastIndexOf('/') <= 7) {
                    unique.append('/')
                }
                unique.append('?')
            }

            unique.append(System.currentTimeMillis())
            unique.append('=')
            unique.append(1)

            return unique.toString()
        }

        private val languageIso3: String
            get() {
                return try {
                    Locale.getDefault().isO3Language.toLowerCase(Locale.US)
                } catch (e: MissingResourceException) {
                    LANGUAGE_DEFAULT_ISO3
                }

            }

        @Throws(IllegalArgumentException::class, UnsupportedEncodingException::class)
        private fun decodeBase64(base64: String): String {
            Base64.decode(base64, Base64.DEFAULT)
            return ""
        }

        /**
         * Returns whether file uploads can be used on the current device (generally all platform versions except for 4.4)

         * @return whether file uploads can be used
         */
        val isFileUploadAvailable: Boolean
            get() = isFileUploadAvailable(false)

        /**
         * Returns whether file uploads can be used on the current device (generally all platform versions except for 4.4)
         *
         *
         * On Android 4.4.3/4.4.4, file uploads may be possible but will come with a wrong MIME type

         * @param needsCorrectMimeType whether a correct MIME type is required for file uploads or `application/octet-stream` is acceptable
         * *
         * @return whether file uploads can be used
         */
        private fun isFileUploadAvailable(needsCorrectMimeType: Boolean): Boolean {
            return if (Build.VERSION.SDK_INT == 19) {
                val platformVersion = Build.VERSION.RELEASE ?: ""

                !needsCorrectMimeType && (platformVersion.startsWith("4.4.3") || platformVersion.startsWith("4.4.4"))
            } else {
                true
            }
        }

        /**
         * Handles a download by loading the file from `fromUrl` and saving it to `toFilename` on the external storage
         *
         *
         * This requires the two permissions `android.permission.INTERNET` and `android.permission.WRITE_EXTERNAL_STORAGE`
         *
         *
         * Only supported on API level 9 (Android 2.3) and above

         * @param context    a valid `Context` reference
         * *
         * @param fromUrl    the URL of the file to download, e.g. the one from `AdvancedWebView.onDownloadRequested(...)`
         * *
         * @param toFilename the name of the destination file where the download should be saved, e.g. `myImage.jpg`
         * *
         * @return whether the download has been successfully handled or not
         */
        @SuppressLint("NewApi")
        fun handleDownload(context: Context, fromUrl: String, toFilename: String): Boolean {
            if (Build.VERSION.SDK_INT < 9) {
                throw RuntimeException("Method requires API level 9 or above")
            }

            val request = DownloadManager.Request(Uri.parse(fromUrl))
            if (Build.VERSION.SDK_INT >= 11) {
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            }
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, toFilename)

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            try {
                try {
                    dm.enqueue(request)
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= 11) {
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    }
                    dm.enqueue(request)
                }

                return true
            } catch (e: IllegalArgumentException) {
                // show the settings screen where the user can enable the download manager app again
                openAppSettings(context, WebViewFlash.PACKAGE_NAME_DOWNLOAD_MANAGER)

                return false
            }
            // if the download manager app has been disabled on the device
        }

        @SuppressLint("NewApi")
        private fun openAppSettings(context: Context, packageName: String): Boolean {
            if (Build.VERSION.SDK_INT < 9) {
                throw RuntimeException("Method requires API level 9 or above")
            }

            return try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                context.startActivity(intent)

                true
            } catch (e: Exception) {
                false
            }

        }
    }
}
/**
 * Loads and displays the provided HTML source text

 * @param html the HTML source text to load
 */
/**
 * Loads and displays the provided HTML source text

 * @param html    the HTML source text to load
 * *
 * @param baseUrl the URL to use as the page's base URL
 */
/**
 * Loads and displays the provided HTML source text

 * @param html       the HTML source text to load
 * *
 * @param baseUrl    the URL to use as the page's base URL
 * *
 * @param historyUrl the URL to use for the page's history entry
 */