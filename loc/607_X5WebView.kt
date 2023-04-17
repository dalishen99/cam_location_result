package com.tristana.customViewWithToolsLibrary.view.webView

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.BarUtils.setStatusBarVisibility
import com.blankj.utilcode.util.LogUtils
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient
import com.tencent.smtt.sdk.*
import com.tristana.customViewWithToolsLibrary.customInterface.IOnPageFinishedInterface
import com.tristana.customViewWithToolsLibrary.data.DataModel
import com.tristana.customViewWithToolsLibrary.tools.sharedPreferences.SpUtils
import com.tristana.customViewWithToolsLibrary.R

class X5WebView(context: Context?, attributeSet: AttributeSet?) : WebView(context, attributeSet) {

    private var customViewCallback: IX5WebChromeClient.CustomViewCallback? = null
    private lateinit var fullscreenContainer: FullscreenHolder
    private var customView: View? = null
    private lateinit var activity: FragmentActivity
    private lateinit var progressBar: ProgressBar
    var onLoadFinishListener: IOnPageFinishedInterface? = null
    private var enableShowProgressBar: Boolean = true
    private val allowThirdPartApp = SpUtils.get(context, DataModel.X5_ALLOW_THIRD_PART_APP_SP, false) as Boolean

    private val client: WebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(p0: WebView?, p1: String?): Boolean {
            if (!allowThirdPartApp) {
                return false
            }
            LogUtils.d("shouldOverrideUrlLoading:$p1")
            if (p1.isNullOrEmpty())
                return true
            return if (p1.startsWith("http") || p1.startsWith("https") || p1.startsWith("ftp")) {
                false
            } else if (p1.startsWith("qqmap:")) {
                //do nothing
                true
            } else {
                try {
                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    intent.data = Uri.parse(p1)
                    view.context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    LogUtils.d("异常URL：$p1")
                    Toast.makeText(view.context, "手机还没有安装支持打开此网页的应用！", Toast.LENGTH_SHORT).show()
                }
                true
            }
        }

        @SuppressLint("ObsoleteSdkInt")
        override fun onPageFinished(p0: WebView?, p1: String?) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            val endCookie = cookieManager.getCookie(p1)
            LogUtils.d("onPageFinished: endCookie : $endCookie")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                CookieSyncManager.getInstance().sync() //同步cookie
            } else {
                CookieManager.getInstance().flush()
            }
            super.onPageFinished(p0, p1)
            onLoadFinishListener?.onPageFinished(p1)
        }
    }

    private val mWebChromeClient = object : WebChromeClient() {
        override fun getVideoLoadingProgressView(): View {
            LogUtils.d("getVideoLoadingProgressView")
            val frameLayout = FrameLayout(activity)
            frameLayout.layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            return frameLayout
        }

        override fun onShowCustomView(p0: View?, p1: IX5WebChromeClient.CustomViewCallback?) {
            showCustomView(p0, p1)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
            // activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            // activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED;
            // super.onShowCustomView(p0, p1)
            LogUtils.d("onShowCustomView")
        }

        @SuppressLint("SourceLockedOrientationActivity")
        override fun onHideCustomView() {
            hideCustomView()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            // super.onHideCustomView()
            LogUtils.d("onHideCustomView")
        }

        override fun onProgressChanged(p0: WebView?, p1: Int) {
            super.onProgressChanged(p0, p1)
            if (enableShowProgressBar) {
                progressBar.progress = p1
                if (p1 != 100) {
                    //Webview加载没有完成 就显示我们自定义的加载图
                    progressBar.visibility = View.VISIBLE
                } else {
                    //Webview加载完成 就隐藏进度条,显示Webview
                    progressBar.visibility = View.GONE
                }
            } else {
                progressBar.visibility = View.GONE
            }
        }

        override fun onGeolocationPermissionsShowPrompt(
            p0: String?,
            p1: GeolocationPermissionsCallback?
        ) {
            p1?.invoke(p0, true, false)
            super.onGeolocationPermissionsShowPrompt(p0, p1)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun initWebViewSettings() {
        setBackgroundColor(resources.getColor(R.color.white))
        isClickable = true
        setOnTouchListener { _, _ -> false }
        val webSetting = settings
        webSetting.javaScriptEnabled = true
        webSetting.builtInZoomControls = true
        webSetting.javaScriptCanOpenWindowsAutomatically = true
        webSetting.domStorageEnabled = true
        webSetting.allowFileAccess = true
        webSetting.layoutAlgorithm = WebSettings.LayoutAlgorithm.NARROW_COLUMNS
        webSetting.setSupportZoom(true)
        //设置自适应屏幕，两者合用
        webSetting.useWideViewPort = true //将图片调整到适合webview的大小
        webSetting.loadWithOverviewMode = true // 缩放至屏幕的大小
        webSetting.setSupportMultipleWindows(true)
        webSetting.setAppCacheEnabled(true)
        webSetting.setGeolocationEnabled(true)
        webSetting.setAppCacheMaxSize(Long.MAX_VALUE)
        webSetting.pluginState = WebSettings.PluginState.ON_DEMAND
        webSetting.setRenderPriority(WebSettings.RenderPriority.HIGH)
        //android 默认是可以打开_bank的，是因为它默认设置了WebSettings.setSupportMultipleWindows(false)
        //在false状态下，_bank也会在当前页面打开……
        //而x5浏览器，默认开启了WebSettings.setSupportMultipleWindows(true)，
        // 所以打不开……主动设置成false就可以打开了
        //需要支持多窗体还需要重写WebChromeClient.onCreateWindow
        webSetting.setSupportMultipleWindows(false)
//        webSetting.setCacheMode(WebSettings.LOAD_NORMAL);
//        getSettingsExtension().setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY);//extension
        //启用数据库
        webSetting.databaseEnabled = true
        val dir: String = context.getDir("database", Context.MODE_PRIVATE).path
        //启用地理定位
        webSetting.setGeolocationEnabled(true)
        //设置定位的数据库路径
        webSetting.setGeolocationDatabasePath(dir)
        //最重要的方法，一定要设置，这就是出不来的主要原因
        webSetting.domStorageEnabled = true
        // settings 的设计
        //兼容视频
        try {
            if (x5WebViewExtension != null) {
                val data = Bundle()
                data.putBoolean("standardFullScreen", false)
                //true表示标准全屏，false表示X5全屏；不设置默认false，
                data.putBoolean("supportLiteWnd", false)
                //false：关闭小窗；true：开启小窗；不设置默认true，
                data.putInt("DefaultVideoScreen", 1)
                //1：以页面内开始播放，2：以全屏开始播放；不设置默认：1
                x5WebViewExtension.invokeMiscMethod("setVideoParams", data)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun initPermission() {
        //权限检查,编辑器自动添加
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1
            )
            return
        }
    }

    fun enableShowProgressBar(enableShowProgressBar: Boolean) {
        this.enableShowProgressBar = enableShowProgressBar
    }

    private fun initUi() {
        try {
            x5WebViewExtension.setScrollBarFadingEnabled(false)
        } catch (e: java.lang.Exception) {
            LogUtils.d("Error:$e")
        }
        isHorizontalScrollBarEnabled = false //水平不显示小方块
        isVerticalScrollBarEnabled = false //垂直不显示小方块
//      setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);//滚动条在WebView内侧显示
//      setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);//滚动条在WebView外侧显示
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.max = 100
        //progressBar.setProgressDrawable(this.getResources().getDrawable(R.drawable.color_progressbar));
        addView(progressBar, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 6))
    }

    @SuppressLint("ObsoleteSdkInt")
    fun syncCookie(url: String?, cookie: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context)
        }
        if (!TextUtils.isEmpty(url)) {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.removeSessionCookies { p0 -> LogUtils.d("syncCookiesStatus:$p0") } // 移除
            cookieManager.removeAllCookies { p0 -> LogUtils.d("removeAllCookiesStatus:$p0") }
            //这里的拼接方式是伪代码
            val split = cookie.split(";".toRegex()).toTypedArray()
            for (string in split) {
                //为url设置cookie
                // ajax方式下  cookie后面的分号会丢失
                cookieManager.setCookie(url, string)
            }
            val newCookie = cookieManager.getCookie(url)
            LogUtils.d("syncCookie: newCookie == $newCookie")
            //sdk21之后CookieSyncManager被抛弃了，换成了CookieManager来进行管理。
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                CookieSyncManager.getInstance().sync() //同步cookie
            } else {
                CookieManager.getInstance().flush()
            }
        }
    }

    //删除Cookie
    @SuppressLint("ObsoleteSdkInt")
    private fun removeCookie() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context)
        }
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies { p0 -> LogUtils.d("syncCookiesStatus:$p0") } // 移除
        cookieManager.removeAllCookies { p0 -> LogUtils.d("removeAllCookiesStatus:$p0") }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().sync()
        } else {
            CookieManager.getInstance().flush()
        }
    }

    fun getDomain(url: String): String? {
        var domain = ""
        val start = url.indexOf(".")
        if (start >= 0) {
            val end = url.indexOf("/", start)
            domain = if (end < 0) {
                url.substring(start)
            } else {
                url.substring(start, end)
            }
        }
        return domain
    }

    override fun drawChild(canvas: Canvas, child: View?, drawingTime: Long): Boolean {
        val status = SpUtils.get(context, DataModel.X5_DEBUG_MODE_SP, false) as Boolean
        val ret: Boolean = super.drawChild(canvas, child, drawingTime)
        canvas.save()
        val paint = Paint()
        paint.color = 0x7fff0000
        paint.textSize = 24f
        paint.isAntiAlias = true
        if (status) {
            if (x5WebViewExtension != null) {
                canvas.drawText(
                    this.context.packageName + "-pid:"
                            + Process.myPid(), 10F, 50F, paint
                )
                canvas.drawText(
                    "X5  Core:" + QbSdk.getTbsVersion(this.context), 10F,
                    100F, paint
                )
            } else {
                canvas.drawText(
                    this.context.packageName + "-pid:"
                            + Process.myPid(), 10F, 50F, paint
                )
                canvas.drawText("Sys Core", 10F, 100F, paint)
            }
            canvas.drawText(Build.MANUFACTURER, 10F, 150F, paint)
            canvas.drawText(Build.MODEL, 10F, 200F, paint)
        }
        canvas.restore()
        return ret
    }

    fun init(activity: FragmentActivity) {
        initPermission()
        initUi()
        setBackgroundColor(85621)
        this.activity = activity
        this.webViewClient = this.client
        this.webChromeClient = mWebChromeClient
        // WebStorage webStorage = WebStorage.getInstance();
        initWebViewSettings()
        this.view.isClickable = true
    }

    private fun hideCustomView() {
        if (customView == null) {
            return
        }
        val decor = activity.window.decorView as FrameLayout
        decor.removeView(fullscreenContainer)
        fullscreenContainer.removeAllViews()
        customView = null
        customViewCallback = null
        setStatusBarVisibility(activity, true)
        // activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }

    private fun showCustomView(view: View?, callback: IX5WebChromeClient.CustomViewCallback?) {
        if (customView != null) {
            callback?.onCustomViewHidden()
            return
        }
        val decor = activity.window.decorView as FrameLayout
        fullscreenContainer = FullscreenHolder(activity)
        fullscreenContainer.addView(
            view,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        decor.addView(
            fullscreenContainer,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        customView = view
        customViewCallback = callback
        setStatusBarVisibility(activity, false)
        // activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
    }

}

internal class FullscreenHolder(ctx: Context) : FrameLayout(ctx) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(evt: MotionEvent): Boolean {
        return true
    }

    init {
        setBackgroundColor(ctx.resources.getColor(android.R.color.transparent))
    }
}