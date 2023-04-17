package com.hlj.activity

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebChromeClient.CustomViewCallback
import android.webkit.WebSettings.LayoutAlgorithm
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.hlj.common.CONST
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import kotlinx.android.synthetic.main.activity_pdf.*
import kotlinx.android.synthetic.main.activity_webview.*
import kotlinx.android.synthetic.main.layout_title2.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import shawn.cxwl.com.hlj.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * 普通网页
 */
class WebviewActivity : BaseActivity(), OnClickListener{

    private var mCustomView: View? = null //用于全屏渲染视频的View
    private var mCustomViewCallback: CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw()
        }
        setContentView(R.layout.activity_webview)
        initWidget()
        initWebView()
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (title != null) {
            tvTitle!!.text = title
        }
    }

    /**
     * 初始化webview
     */
    private fun initWebView() {
        val url = intent.getStringExtra(CONST.WEB_URL)
        val webSettings = webView!!.settings

        //支持javascript
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.domStorageEnabled = true
        webSettings.setGeolocationEnabled(true)
        // 设置可以支持缩放
        webSettings.setSupportZoom(true)
        // 设置出现缩放工具
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        //扩大比例的缩放
        webSettings.useWideViewPort = true
        //自适应屏幕
        webSettings.layoutAlgorithm = LayoutAlgorithm.SINGLE_COLUMN
        webSettings.loadWithOverviewMode = true
        webView!!.loadUrl(url)

        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
            }
        }
        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                callback.invoke(origin, true, false)
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                super.onShowCustomView(view, callback)
                //如果view 已经存在，则隐藏
                if (mCustomView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                mCustomView = view
                mCustomView!!.visibility = View.VISIBLE
                mCustomViewCallback = callback
                mLayout.addView(mCustomView)
                mLayout.visibility = View.VISIBLE
                mLayout.bringToFront()

                //设置横屏
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }

            override fun onHideCustomView() {
                super.onHideCustomView()
                if (mCustomView == null) {
                    return
                }
                mCustomView!!.visibility = View.GONE
                mLayout.removeView(mCustomView)
                mCustomView = null
                mLayout.visibility = View.GONE
                try {
                    mCustomViewCallback!!.onCustomViewHidden()
                } catch (e: Exception) {
                }
                //                titleView.setVisibility(View.VISIBLE);
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT //竖屏
            }
        }

        webView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, itemUrl: String): Boolean {
                webView!!.loadUrl(itemUrl)
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val fileName = contentDisposition.substring(contentDisposition.indexOf("\"") + 1, contentDisposition.lastIndexOf("\""))
            checkStorageAuthority(object : StorageCallback {
                override fun grantedStorage(isGranted: Boolean) {
                    if (isGranted) {
                        okHttpFile(url, fileName)
                    } else {
                        Toast.makeText(this@WebviewActivity, "需要开启存储权限", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView != null && webView!!.canGoBack()) {
                webView!!.goBack()
                return true
            } else {
                finish()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.llBack) {
            finish()
        }
    }

    /**
     * 横竖屏切换监听
     */
    override fun onConfigurationChanged(config: Configuration) {
        super.onConfigurationChanged(config)
        when (config.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
//                mToolbar.setVisibility(View.GONE)
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
//                mToolbar.setVisibility(View.VISIBLE)
            }
        }
    }

    private fun okHttpFile(url: String, fileName: String) {
        if (TextUtils.isEmpty(url)) {
            return
        }
        showDialog()
        Thread {
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    var filePath: String? = null
                    var inputStream: InputStream? = null
                    var fos: FileOutputStream? = null
                    try {
                        inputStream = response.body!!.byteStream() //获取输入流
                        val total = response.body!!.contentLength().toFloat() //获取文件大小
                        if (inputStream != null) {
                            val files = File("${getExternalFilesDir(null)}/HLJ")
                            if (!files.exists()) {
                                files.mkdirs()
                            }
                            filePath = files.absolutePath + "/" + fileName
                            fos = FileOutputStream(filePath)
                            val buf = ByteArray(1024)
                            var ch = -1
                            var process = 0
                            while (inputStream.read(buf).also { ch = it } != -1) {
                                fos.write(buf, 0, ch)
                                process += ch

//									int percent = (int) Math.floor((process / total * 100));
//									Log.e("percent", process+"--"+total+"--"+percent);
//									Message msg = handler.obtainMessage(1001);
//									msg.what = 1001;
//									msg.obj = filePath;
//									msg.arg1 = percent;
//									handler.sendMessage(msg);
                            }
                        }
                        fos!!.flush()
                        fos.close() // 下载完成
                        val msg = handler.obtainMessage(1001)
                        msg.what = 1001
                        msg.obj = filePath
                        handler.sendMessage(msg)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    } finally {
                        inputStream?.close()
                        fos?.close()
                    }
                }
            })
        }.start()
    }

    @SuppressLint("HandlerLeak")
    private val handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (msg.what == 1001) {
                cancelDialog()
                val filePath = msg.obj.toString()
                if (TextUtils.isEmpty(filePath)) {
                    Toast.makeText(this@WebviewActivity, "文件下载失败，请点击重新下载", Toast.LENGTH_LONG).show()
                } else {
                    CommonUtil.intentWPSOffice(this@WebviewActivity, filePath)
                }
            }
        }
    }
	
}
