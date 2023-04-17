package com.hq.tool.model.webview

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.webkit.*
import android.webkit.WebView
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.View
import android.webkit.SslErrorHandler
import android.content.Intent
import android.net.Uri
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.hq.tool.system.CAMERA_REQUEST
import com.hq.tool.system.openCamera
import com.hq.tool.R
import com.hq.tool.ani.CircularAnim
import com.hq.tool.ani.LoadAnim

import com.hq.tool.toast


/**
 * Created by lenovo on 2017/9/14.
 */

class HtmlActivity : AppCompatActivity() {
    private var overTime: Long = 0
    //    https://dqy.he-n-tax.gov.cn:8090/dqy/login_sjd.jsp
//
//        protected val mBaseUrl = "http://75.16.40.202:8080/wdqy/login_sjd.jsp"
//    private val protocol = "https://"
    private var mBaseUrl = ""
    private var errorUrl: String? = null
    private var build:HtmlBuild?=null
    private var isSingle=false
    private var isAutoStyle=false
    //    private val mStartUrl: String = "login_sjd.jsp"
    private lateinit var webView: WebView
    private var uploadMessageAboveL:ValueCallback<Array<Uri>>?=null
    private var cUri:Uri?=null
    private lateinit var errorLl: LinearLayout
    private lateinit var errorImg: ImageView
    private lateinit var errorText: TextView
    private var loadLog: AlertDialog?=null

    private val jsChangeStyle = "javascript:function myFunction(){\n" +
            "var \$jquery = jQuery.noConflict();\n" +
            "var content=\$jquery('.article');\n" +
            "\$jquery('body').empty();\n" +
            "content.css({\n" +
            "background:\"#fff\",\n" +
            "position:\"absolute\",\n" +
            "top:\"0\",left:\"0\",\n" +
            "});\n" +
            "\$jquery('body').append(content);\n" +
            "\$jquery(\"div\").removeAttr(\"class\").removeAttr(\"style\").removeAttr(\"id\");\n" +
            "\$jquery(\"a\").removeAttr(\"class\").removeAttr(\"style\").removeAttr(\"id\");\n" +
            "\$jquery(\"h1\").removeAttr(\"class\").removeAttr(\"style\").removeAttr(\"id\");\n" +
            "\$jquery(\"h2\").removeAttr(\"class\").removeAttr(\"style\").removeAttr(\"id\");\n" +
            "\$jquery(\"h3\").removeAttr(\"class\").removeAttr(\"style\").removeAttr(\"id\");\n" +
            "\$jquery(\"h4\").removeAttr(\"class\").removeAttr(\"style\").removeAttr(\"id\");\n" +
            "\$jquery(\"h5\").removeAttr(\"class\").removeAttr(\"style\").removeAttr(\"id\");\n" +
            "\$jquery(\"h6\").removeAttr(\"class\").removeAttr(\"style\").removeAttr(\"id\");\n" +
            "\$jquery(\"img\").removeAttr(\"class\").removeAttr(\"style\").removeAttr(\"id\");\n" +
            "\$jquery(\"img\").css({width: \"100%\",height:\"100%\",objecFit:\"cover\"});\n" +
            "\$jquery(\"h1\").css({paddingBottom: \"0.3em\",fontSize:\"2em\",borderBottom:\"1px solid #eaecef\"});\n" +
            "\$jquery(\"h2\").css({paddingBottom: \"0.3em\",fontSize:\"1.5em\",borderBottom:\"1px solid #eaecef\"});\n" +
            "\$jquery(\"h3\").css({fontSize:\"1.25em\"});\n" +
            "\$jquery(\"h4\").css({fontSize:\"1em\"});\n" +
            "\$jquery(\"h5\").css({fontSize:\"0.875em\"});\n" +
            "\$jquery(\"h6\").css({fontSize:\"0.85em\"});\n}"

//    val imgResList = ArrayList<String>().apply {
        //        add("${mBaseUrl}sjd/tzgg/tzggxq.action?")
//        add("${mBaseUrl}sjd/zcsd/zcsdxq.action?")
//        add("${mBaseUrl}sjd/fxts/fxtsxq.action?")
//        add("${mBaseUrl}sjd/Zdsxbg/Zdsxbgxq.action?")
//        add("${mBaseUrl}qyd/sssqs/sssqsck.action?")
//    }

    val newLoad = ArrayList<String>().apply {

    }

    var needClearHistory = false

    val dialog = CircularAnim()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_html)
        mBaseUrl = intent.getStringExtra("URL").toString()
        errorUrl = intent.getStringExtra("ERRORURL")
        isSingle=intent.getBooleanExtra("ISSINGLE",false)
        isAutoStyle=intent.getBooleanExtra("ISAUTOSTYLE",false)
        init()

    }


    private fun init() {
        webView = findViewById(R.id.main_web) as WebView
        errorLl = findViewById(R.id.error_ll) as LinearLayout
        errorImg = findViewById(R.id.error_img) as ImageView
        errorText = findViewById(R.id.error_text) as TextView
        dialog.fullActivity(this, webView)
        webView.settings.setDefaultFontSize(22)
        webView.settings.setMinimumFontSize(16)//设置 WebView 支持的最小字体大小，默认为 8
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT// 不加载缓存
        webView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.setAppCacheMaxSize(1024 * 1024 * 8)
        webView.settings.setDomStorageEnabled(true);//设置适应HTML5的一些方法
        webView.settings.setBlockNetworkImage(false)//是否阻塞加载网络图片  协议http or https
        webView.settings.setAllowFileAccess(true) //允许加载本地文件html  file协议, 这可能会造成不安全 , 建议重写关闭
        webView.settings.setAllowFileAccessFromFileURLs(false) //通过 file url 加载的 Javascript 读取其他的本地文件 .建议关闭
        webView.settings.setAllowUniversalAccessFromFileURLs(false)//允许通过 file url 加载的 Javascript 可以访问其他的源，包括其他的文件和 http，https 等其他的源
        webView.settings.textZoom=100

//        val appCachePath = applicationContext.cacheDir.absolutePath
//        main_web.settings.setAppCachePath(appCachePath)
//        main_web.settings.setAppCacheEnabled(true)
        webView.settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
//        main_web.settings.allowFileAccess = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        } else {
            CookieManager.getInstance().setAcceptCookie(true)
        }

        webView.addJavascriptInterface(object : Object(){
            @JavascriptInterface
            fun jsAndroid(msg : String){

            }
            //第二个参数可以自己随便设置，在html里会用到
        },"androids")

        errorLl.setOnClickListener({
            webView.visibility = View.VISIBLE
            errorLl.visibility = View.GONE
            webView.loadUrl(mBaseUrl)
            needClearHistory = true

        })

        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                message?.toast(this@HtmlActivity)
                result?.confirm()
                return true
            }

            var isNeedExe = true
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress > 95 && isNeedExe&&isAutoStyle) {
                    isNeedExe = !isNeedExe
                    webView.loadUrl(jsChangeStyle)
                    webView.loadUrl("javascript:myFunction()")
                }
                super.onProgressChanged(view, newProgress)

            }

            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams): Boolean {
                uploadMessageAboveL = filePathCallback
                //调用系统相机或者相册
                cUri= openCamera()
                return true
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                callback?.invoke(origin, true, false)
                super.onGeolocationPermissionsShowPrompt(origin, callback)
            }

        }

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {//开始加载
                for (u in newLoad) {//打开外部网站
                    if (url.indexOf(u) != -1) {
                        val intent = Intent()
                        intent.action = "android.intent.action.VIEW"
                        val content_url = Uri.parse(url)
                        intent.data = content_url
                        startActivity(intent)
                        webView.stopLoading()
                    }
                }
                showLoad()
                super.onPageStarted(view, url, favicon)
            }



            override fun onPageFinished(view: WebView, url: String) {//加载完成
                super.onPageFinished(view, url)
                if (needClearHistory) {
                    webView.clearHistory()
                    needClearHistory = false
//                    setName()
                }
                imgReset()
                hideLoad()
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {//加载失败
                super.onReceivedError(view, request, error)
//                main_web.loadUrl(mUrl)
                webView.visibility = View.GONE
                errorLl.visibility = View.VISIBLE

            }

            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                //      super.onReceivedSslError(view, handler, error);//      一定要注释掉！
                handler.proceed()
            }
        }

        webView.loadUrl(mBaseUrl)

    }

//    fun setName(): Unit {
//        var name = getPreferences(Context.MODE_PRIVATE).getString(NAME, "")
//        if (name.length > 2) {
//            name = name.substring(2, name.length)
//        }
//        webView.loadUrl("javascript:(function()\n" +
//                " {\n" +
//                "    document.getElementById(\"yhmc\").value = \"${name}\";\n" +
//                " })()")
//    }

    fun setCook(key:String,v:String): Unit {
        webView.loadUrl("javascript:(function()\n" +
                " {\n" +
                "      document.cookie=\"${key}=\"+\"${v}\";\n" +
                " })()")
    }

    fun setTextSize(key:String,v:String): Unit {
        webView.loadUrl("javascript:(function htmlFontSize(){\n" +
                "    var h = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);\n" +
                "    var w = Math.max(document.documentElement.clientWidth, window.innerWidth || 0);\n" +
                "    var width = w > h ? h : w;\n" +
                "    width = width > 720 ? 720 : width\n" +
                "    var fz = ~~(width*100000/36)/10000\n" +
                "    document.getElementsByTagName(\"html\")[0].style.cssText = 'font-size: ' + fz +\"px\";\n" +
                "    var realfz = ~~(+window.getComputedStyle(document.getElementsByTagName(\"html\")[0]).fontSize.replace('px','')*10000)/10000\n" +
                "    if (fz !== realfz) {\n" +
                "        document.getElementsByTagName(\"html\")[0].style.cssText = 'font-size: ' + fz * (fz / realfz) +\"px\";\n" +
                "    }\n" +
                "})()")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ( uploadMessageAboveL == null) {
            return
        }
        //取消拍照或者图片选择时,返回null,否则<input file> 就是没有反应
        if (resultCode != RESULT_OK) {
            if (uploadMessageAboveL != null) {
                uploadMessageAboveL?.onReceiveValue(null);
                uploadMessageAboveL = null;
            }
        }
        //拍照成功和选取照片时
        if (requestCode== CAMERA_REQUEST) {
            if (uploadMessageAboveL != null) {
                uploadMessageAboveL?.onReceiveValue(arrayOf<Uri>(cUri!!))
                uploadMessageAboveL = null;
            }
        }
    }

    /**
     * 对图片进行重置大小，宽度就是手机屏幕宽度，高度根据宽度比便自动缩放
     */
    private fun imgReset() {
        webView.loadUrl("javascript:(function(){" +
                "var objs = document.getElementsByTagName('img'); " +
                "var tableWidth = window.screen.width; " +
                "for(var i=0;i<objs.length;i++)  " +
                "{"
                + "var img = objs[i];" +
                " var imgWidth = img.width;" +
                "if(imgWidth<tableWidth){ " +
//                "img.style.width = 'auto'; img.style.height = 'auto';" +
                "}else{ " +
                "img.style.maxWidth = '100%'; img.style.height = 'auto';" +
                "} " +
//                "    img.style.maxWidth = '100%'; img.style.height = 'auto';  " +
                "}" +
                "})()")
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (isSingle){
            if ( webView.canGoBack()) {
                //表示按返回键时的操作
                webView.goBack()   //后退
                //webview.goForward();//前进
            } else {
                canFinish()
            }
        }
    }

    private fun canFinish() {
        Toast.makeText(this, "再次点击返回关闭应用", Toast.LENGTH_SHORT).show()
        if (overTime == 0L) {
            overTime = System.currentTimeMillis()
            return
        } else {
            if (System.currentTimeMillis() - overTime < 3000) {
                finish()
            } else {
                overTime = 0L
            }
        }
    }


    protected fun showLoad(): Unit {
        val load = AlertDialog.Builder(this)
        load.setView(LoadAnim(this))
        loadLog = load.show()
    }

    protected fun hideLoad(): Unit {
        if (loadLog != null && loadLog?.isShowing!!) {
            loadLog?.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}

data class HtmlBuild(var isSingle:Boolean, var chengImageSize:Boolean,var newLoad:List<String>)
