package com.ljp.module_web

import android.app.AlertDialog
import android.content.*
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ljp.lib_base.utils.showToast
import com.ljp.module_web.BuildConfig
import com.tencent.smtt.export.external.interfaces.*
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient


/*
 *@创建者       L_jp
 *@创建时间     2022/7/20 17:24.
 *@描述
 */
class WebViewHelper constructor(private val webView: WebView) : DefaultLifecycleObserver {

    private var onPageChangedListener: OnPageChangedListener? = null
    private var injectState = false
    private var originalUrl = "about:blank"
    private var sslErrorDialog: AlertDialog? = null
    private var sslErrorHandler: SslErrorHandler? = null


    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        webView.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        webView.onPause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        onPageChangedListener = null
        WebViewManager.recycle(webView)
    }

    init {
        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                webView: WebView?,
                webResourceRequest: WebResourceRequest?,
            ): Boolean {
                val uri = webResourceRequest?.url ?: return true
                if (!("http" == uri.scheme || "https" == uri.scheme)) {
                    handleSystemScheme(uri)
                    return true
                }
                return super.shouldOverrideUrlLoading(webView, webResourceRequest)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onPageChangedListener?.onPageStarted(view, url, favicon)
                injectState = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onPageChangedListener?.onPageFinished(view, url)
                injectState = false
            }


            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?,
            ) {
//                super.onReceivedSslError(view, handler, error)
                sslErrorHandler = handler
                if (sslErrorDialog == null) {
                    sslErrorDialog = AlertDialog.Builder(view?.context)
                        .setMessage("SSL认证失败，是否继续访问？")
                        .setPositiveButton("确定") { _: DialogInterface?, _: Int -> sslErrorHandler?.proceed() }
                        .setNegativeButton("取消") { _: DialogInterface?, _: Int -> sslErrorHandler?.cancel() }
                        .create()
                }
                if (sslErrorDialog?.isShowing == false) {
                    try {
                        sslErrorDialog?.show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                webView.context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onPageChangedListener?.onProgressChanged(view, newProgress)
                if (newProgress > 80 && BuildConfig.DEBUG && !injectState) {
                    injectState = true
                    view?.apply { evaluateJavascript(injectVConsoleJs(context)) {} }
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                onPageChangedListener?.onReceivedTitle(view, title)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissionsCallback?,
            ) {
                callback?.invoke(origin, true, true)
                super.onGeolocationPermissionsShowPrompt(origin, callback)
            }
        }
    }

    /**
     * 处理某一些系统scheme
     */
    private fun handleSystemScheme(url: Uri): Boolean {
        return try {
            val scheme = url.scheme ?: ""
            //不是http  和https开头的
            if (scheme.startsWith("phone://")) { //如果是打电话
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:" + scheme.substring(8))
                webView.context.startActivity(intent)
            } else if (scheme.startsWith("email://")) { //发邮件
                try {
                    val data = Intent(Intent.ACTION_SENDTO)
                    data.data = Uri.parse("mailto:" + scheme.substring(8))
                    webView.context.startActivity(data)
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (e is ActivityNotFoundException) {
                        webView.context.showToast("手机中没有安装邮件app")
                    }
                }
            } else if (scheme.startsWith("copy://")) { //拷贝
                val substring = scheme.substring(7)
                val cmb = webView.context
                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cmb.text = substring
                webView.context.showToast("已复制到剪贴板")
            } else {
                //其他scheme
                webView.context.startActivity(Intent(Intent.ACTION_VIEW, url))
            }
            true
        } catch (e: Exception) {
            true
        }
    }

    fun injectVConsoleJs(context: Context): String {
        return try {
            val vconsoleJs = context.resources.assets.open("js/vconsole.min.js").use {
                val buffer = ByteArray(it.available())
                it.read(buffer)
                String(buffer)
            }
            """ 
            $vconsoleJs
            var vConsole = new VConsole();
        """.trimIndent()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun canGoForward(): Boolean {
        val canForward = webView.canGoForward()
        if (canForward) webView.goForward()
        return canForward
    }

    fun canGoBack(): Boolean {
        val canBack = webView.canGoBack()
        if (canBack) webView.goBack()
        val backForwardList = webView.copyBackForwardList()
        val currentIndex = backForwardList.currentIndex
        if (currentIndex == 0) {
            val currentUrl = backForwardList.currentItem.url
            val currentHost = Uri.parse(currentUrl).host
            //栈底不是链接则直接返回
            if (currentHost.isNullOrBlank()) return false
            //栈底链接不是原始链接则直接返回
            if (originalUrl != currentUrl) return false
        }
        return canBack
    }


    fun loadUrl(url: String?): WebView {
        if (!url.isNullOrBlank()) {
            webView.loadUrl(url)
            originalUrl = url
        }
        return webView
    }


    fun setOnPageChangedListener(onPageChangedListener: OnPageChangedListener?): WebViewHelper {
        this.onPageChangedListener = onPageChangedListener
        return this
    }

    fun reload() {
        webView.reload()
    }
}


interface OnPageChangedListener {
    fun onReceivedTitle(view: WebView?, title: String?)
    fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?)
    fun onPageFinished(view: WebView?, url: String?)
    fun onProgressChanged(view: WebView?, newProgress: Int)
}