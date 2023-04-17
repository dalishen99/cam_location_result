package com.kayu.business_car_owner.ui

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.hjq.toast.ToastUtils
import com.kayu.business_car_owner.*
import com.kayu.business_car_owner.R
import com.kayu.business_car_owner.activity.BaseActivity
import com.kayu.business_car_owner.activity.WebViewActivity
import com.kayu.business_car_owner.http.*
import com.kayu.business_car_owner.http.parser.NormalStringParse
import com.kayu.utils.*
import com.kayu.utils.callback.Callback
import com.kayu.utils.location.LocationManagerUtil
import com.kongzue.dialog.interfaces.OnDismissListener
import com.kongzue.dialog.interfaces.OnMenuItemClickListener
import com.kongzue.dialog.v3.BottomMenu
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshListener
import java.io.File
import java.lang.Exception
import java.util.HashMap

class ShopFragment : Fragment() {
    var wvWebView: WebView? = null
//    private val titleName: String = "加载中..."
    private var title_name: TextView? = null
    private var title_back_btu: LinearLayout? = null
    var headMap: MutableMap<String, String?> = HashMap()

    @SuppressLint("HandlerLeak")
    private val jsHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {

            when (msg.what) {
                2 -> {   //关闭加载框
//                    isOpenDialog = msg.obj as String
//                    when (isOpenDialog) {
//                        "1" -> {
//                            TipGifDialog.show(
//                                requireContext() as AppCompatActivity?,
//                                "加载中...",
//                                TipGifDialog.TYPE.OTHER,
//                                R.drawable.loading_gif
//                            )
//                        }
//                        "0" -> {
//                            TipGifDialog.dismiss()
//                        }
//                    }

                }
                3 -> {   //返回按键是否需要全部关闭
                    jsCloseStatus = msg.obj as String
                }
                4 -> {  //js调用本地定位点并回传

                    val location = LocationManagerUtil.self?.loccation
                    LogUtil.e(
                        "js调用方法",
                        " getLocation==" + location?.longitude + "," + location?.latitude
                    )
                    if (null != location) {
                        wvWebView?.evaluateJavascript(
                            "window.CurrentLocation(" + location.latitude + "," + location.longitude + ")",
                            null
                        )
                    }
                }
            }
            super.handleMessage(msg)
        }
    }

    private var jsCloseStatus: String = ""
    private var isOpenDialog: String = ""
    private var mHasLoadedOnce = false // 页面已经加载过
    private var isCreated = false

    public override fun onConfigurationChanged(newConfig: Configuration) {
        //非默认值
        if (newConfig.fontScale != 1f) {
            getResources()
        }
        super.onConfigurationChanged(newConfig)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        LogUtil.e("ShopFragment----", "----onCreateView---")
//        StatusBarUtil.setStatusBarColor(requireActivity(), getResources().getColor(R.color.white))
        return inflater.inflate(R.layout.fragment_shop, container, false)
    }

    private var refreshLayout: RefreshLayout? = null
    var isRefresh = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        AndroidBug5497Workaround.assistActivity(requireActivity())
//        val webLay: LinearLayout = view.findViewById(R.id.llWebView)
//        if (AppUtil.hasNavBar(requireContext())) {
//            val bottom: Int = AppUtil.getNavigationBarHeight(requireActivity())
//            val lp: LinearLayout.LayoutParams = LinearLayout.LayoutParams(webLay.getLayoutParams())
//            lp.setMargins(0, 0, 0, bottom + 80)
//            webLay.setLayoutParams(lp)
//        }
        title_back_btu = view.findViewById(R.id.title_back_btu)
        title_back_btu?.visibility =View.GONE
        title_back_btu?.setOnClickListener(object : NoMoreClickListener() {
            override fun OnMoreClick(view: View) {
                onBackPressed()
            }

            override fun OnMoreErrorClick() {}
        })
        title_name = view.findViewById(R.id.title_name_tv)
        title_name?.text = ""

        wvWebView = view.findViewById(R.id.wvWebView)

        refreshLayout = view.findViewById<View>(R.id.refreshLayout) as RefreshLayout
        refreshLayout!!.setEnableAutoLoadMore(false)
        refreshLayout!!.setEnableLoadMore(false)
        refreshLayout!!.setEnableLoadMoreWhenContentNotFull(false) //是否在列表不满一页时候开启上拉加载功能
        refreshLayout!!.setEnableOverScrollBounce(false) //是否启用越界回弹
        refreshLayout!!.setEnableOverScrollDrag(true)
        refreshLayout!!.setOnRefreshListener(OnRefreshListener {
            if (isRefresh ) return@OnRefreshListener
            isRefresh = true

            wvWebView!!.loadUrl((URL))
            refreshLayout!!.finishRefresh()
//            loadServiceUrl(requireContext())
        })
        initData()
        loadServiceUrl(requireContext())
    //        refreshLayout!!.autoRefresh()

        isCreated = true
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initData() {
        val webSettings: WebSettings = wvWebView!!.settings
        webSettings.javaScriptEnabled = true
        webSettings.blockNetworkImage = false
        //支持插件
//        webSettings.setPluginsEnabled(true);

//设置自适应屏幕，两者合用
        webSettings.textZoom = 100
        webSettings.useWideViewPort = true //将图片调整到适合webview的大小
        webSettings.loadWithOverviewMode = true // //和setUseWideViewPort(true)一起解决网页自适应问题

//缩放操作
        webSettings.setSupportZoom(true) //支持缩放，默认为true。是下面那个的前提。
        webSettings.builtInZoomControls = true //设置内置的缩放控件。若为false，则该WebView不可缩放

        webSettings.displayZoomControls = false //隐藏原生的缩放控件
        webSettings.domStorageEnabled = true
        webSettings.setSupportMultipleWindows(false)


        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL

        val screenDensity = resources.displayMetrics.densityDpi
        var zoomDensity = WebSettings.ZoomDensity.MEDIUM
        when (screenDensity) {
            DisplayMetrics.DENSITY_LOW -> zoomDensity = WebSettings.ZoomDensity.CLOSE
            DisplayMetrics.DENSITY_MEDIUM -> zoomDensity = WebSettings.ZoomDensity.MEDIUM
            DisplayMetrics.DENSITY_HIGH -> zoomDensity = WebSettings.ZoomDensity.FAR
        }
        webSettings.defaultZoom = zoomDensity

//其他细节操作
        webSettings.setAllowFileAccess(true) //设置可以访问文件
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true) //支持通过JS打开新窗口
        webSettings.setLoadsImagesAutomatically(true) //支持自动加载图片
        webSettings.setDefaultTextEncodingName("utf-8") //设置编码格式

//        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setAppCacheEnabled(true) //是否使用缓存

        //启用数据库
        webSettings.setDatabaseEnabled(true)

//设置定位的数据库路径
        val dir: String = requireActivity().getApplicationContext().getDir("database", AppCompatActivity.MODE_PRIVATE).getPath()
        webSettings.setGeolocationDatabasePath(dir)

//启用地理定位
        webSettings.setGeolocationEnabled(true)
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT)
        //开启DomStorage缓存

        // android 5.0及以上默认不支持Mixed Content
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW)
        }
        wvWebView!!.addJavascriptInterface(
            LocalJavascriptInterface(requireContext(), jsHandler),
            "androidMethod"
        )
        wvWebView!!.requestFocus()
        wvWebView!!.clearCache(true)
        wvWebView!!.clearHistory()
        /**
         * 当下载文件时打开系统自带的浏览器进行下载，当然也可以对捕获到的 url 进行处理在应用内下载。
         */
//        wvWebView.getSettings().setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:54.0) Gecko/20100101 Firefox/54.0");
        wvWebView!!.setDownloadListener(FileDownLoadListener())
        wvWebView!!.setWebChromeClient(object : WebChromeClient() {
            public override fun onJsPrompt(
                view: WebView,
                url: String,
                message: String,
                defaultValue: String,
                result: JsPromptResult
            ): Boolean {
                return super.onJsPrompt(view, url, message, defaultValue, result)
            }

            public override fun onJsConfirm(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                val builder: AlertDialog.Builder = AlertDialog.Builder(view.getContext())
                builder.setTitle("提示").setMessage(message)
                    .setPositiveButton("确定", object : DialogInterface.OnClickListener {
                        public override fun onClick(dialog: DialogInterface, which: Int) {
//                                if (!StringUtil.isEmpty(url)){
//                                    Intent intent=new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                    intent.setComponent(new ComponentName("com.android.browser","com.android.browser.BrowserActivity"));
//                                    startActivity(intent);
//                                }
                            result.confirm()
                        }
                    }).setNeutralButton("取消", object : DialogInterface.OnClickListener {
                        public override fun onClick(dialog: DialogInterface, which: Int) {
                            result.cancel()
                        }
                    }).create().show()
                return true
            }

            public override fun onJsAlert(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                val b2: AlertDialog.Builder = AlertDialog.Builder(requireContext())
                    .setTitle("提示")
                    .setMessage(message)
                    .setPositiveButton("确定",
                        object : DialogInterface.OnClickListener {
                            public override fun onClick(dialog: DialogInterface, which: Int) {
                                result.confirm()
                            }
                        }).setNeutralButton("取消", object : DialogInterface.OnClickListener {
                        public override fun onClick(dialog: DialogInterface, which: Int) {
                            result.cancel()
                        }
                    })
                b2.setCancelable(true)
                b2.create()
                b2.show()
                return true
            }

            public override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
//                title_name!!.setText(titleName)
            }

//            public override fun onProgressChanged(view: WebView, newProgress: Int) {
//                super.onProgressChanged(view, newProgress)
//                if (newProgress == 100) {
//                    if (isOpenDialog == "") {
//                        TipGifDialog.dismiss()
//                    }
//                }
//
//            }

            public override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                callback.invoke(origin, true, false)
                super.onGeolocationPermissionsShowPrompt(origin, callback)
            }

            // For Android 3.0+
            fun openFileChooser(uploadMsg: ValueCallback<Uri?>?) {
                mUploadMessage = uploadMsg
                openImageChooserActivity()
            }

            // For Android 3.0+
            fun openFileChooser(uploadMsg: ValueCallback<*>?, acceptType: String?) {
                mUploadMessage = uploadMsg as ValueCallback<Uri?>?
                openImageChooserActivity()
            }

            // For Android 4.1
            fun openFileChooser(
                uploadMsg: ValueCallback<Uri?>?,
                acceptType: String?,
                capture: String?
            ) {
                mUploadMessage = uploadMsg
                openImageChooserActivity()
            }

            // For Android 5.0+
            public override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri?>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                mUploadCallbackAboveL = filePathCallback
                openImageChooserActivity()
                return true
            }
        })
        wvWebView!!.webViewClient = object : WebViewClient() {
            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
//                LogUtil.e("webview", "errorResponse=" + errorResponse.toString())
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
//                LogUtil.e("webview", "description=" + description + "  failingUrl=" + failingUrl)
            }

//            override fun onLoadResource(view: WebView?, url: String?) {
//                LogUtil.e("ShopFragment", "onLoadResource: view：----------- url=$url")
//                super.onLoadResource(view, url)
//            }
//            override fun shouldOverrideKeyEvent(view: WebView?, event: KeyEvent?): Boolean {
//                LogUtil.e("ShopFragment", "shouldOverrideKeyEvent: view：----------- url=${view?.url}-------KeyEvent=${event?.keyCode}")
//                return super.shouldOverrideKeyEvent(view, event)
//            }
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                LogUtil.e("ShopFragment", "shouldOverrideUrlLoading: view："+view.url + "----------- url=$url")
                if (url != URL) {
                    val intent = Intent(requireContext(), WebViewActivity::class.java)
                    intent.putExtra("url", url)
                    intent.putExtra("from", title_name?.text)
                    startActivity(intent)
                    return true
                }
                if (url.startsWith("http:") || url.startsWith("https:")) {
                    return if ((url == HttpConfig.CLOSE_WEB_VIEW) || url == HttpConfig.CLOSE_WEB_VIEW1) {
//                        requireContext().finish()
                        wvWebView!!.clearCache(true)
                        wvWebView!!.clearHistory()
                        refreshLayout?.autoRefresh()
                        true
                    } else {
                        view.loadUrl(url, headMap)
                        false
                    }
                } else {
                    try {
                        // 以下固定写法,表示跳转到第三方应用
                        val intent: Intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                    } catch (e: Exception) {
                        // 防止没有安装的情况
                        e.printStackTrace()
                        ToastUtils.show("未安装相应的客户端")
                    }
                    return true
                }
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {

//                    TipGifDialog.show(
//                        requireContext() as AppCompatActivity?,
//                        "加载中...",
//                        TipGifDialog.TYPE.OTHER,
//                        R.drawable.loading_gif

                LogUtil.e("ShopFragment", "onPageStarted: " + url)
                if (url != URL) {
                    val intent = Intent(requireContext(), WebViewActivity::class.java)
                    intent.putExtra("url", url)
                    intent.putExtra("from", title_name?.text)
                    startActivity(intent)
                }
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView, url: String) {
                view.getSettings().setLoadsImagesAutomatically(true)
                title_name!!.postDelayed({ title_name!!.text = view.title }, 1)
                lastOpenUrl = url
                if (url.contains("#/tour")) {
                    URL = url
                    hasBack = false
                    title_back_btu?.visibility =View.GONE
//                    navigation.visibility = View.VISIBLE

                } else {
                    hasBack = true
                    title_back_btu?.visibility =View.VISIBLE
//                    navigation.visibility = View.GONE
                }
                if (url != URL) {
                    val intent = Intent(requireContext(), WebViewActivity::class.java)
                    intent.putExtra("url", url)
                    intent.putExtra("from", title_name?.text)
                    startActivity(intent)
                    wvWebView?.goBack()
                    return
                }
                LogUtil.e("ShopFragment", "onPageFinished: " + url)
                super.onPageFinished(view, url)
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                handler.proceed()
            }
        }

        wvWebView!!.loadUrl("javascript:window.location.reload( true )")
        if (isRefresh) {
            refreshLayout!!.finishRefresh()
            isRefresh = false
        }
    }

    private var lastOpenUrl: String? = null

    /**
     * 是否是支付宝的 url
     *
     * @param checkUrl url
     * @return 结果
     */
    private fun isHaveAliPayLink(checkUrl: String): Boolean {
        return !TextUtils.isEmpty(checkUrl) && (checkUrl.startsWith("alipays:") || checkUrl.startsWith(
            "alipay"
        ))
    }

    private var isDownload: Boolean = true

    internal inner class FileDownLoadListener constructor() : DownloadListener {
        public override fun onDownloadStart(
            url: String,
            userAgent: String,
            contentDisposition: String,
            mimetype: String,
            contentLength: Long
        ) {
            if (isDownload) {
                val intent = Intent(Intent.ACTION_VIEW)
                val uri: Uri = Uri.parse(url)
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.data = uri
                startActivity(intent)
            }
            isDownload = true //重置为初始状态
        }
    }

    private var cameraFielPath: String? = null
    private val FILE_CHOOSER_RESULT_CODE: Int = 1
    private var mUploadCallbackAboveL: ValueCallback<Array<Uri?>>? = null
    private var mUploadMessage: ValueCallback<Uri?>? = null

    //选择拍照还是相册
    fun openImageChooserActivity() {
        showCustomDialog()
    }


    //拍照
    private fun takeCamera() {
        KWApplication.instance.permissionsCheck(
            requireActivity() as BaseActivity,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            R.string.permiss_take_phone,
            object : Callback {
                override fun onSuccess() {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraFielPath = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        .toString() + "//" + System.currentTimeMillis() + ".jpg"
                    val outputImage: File = File(cameraFielPath)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //7.0及以上
                        val photoUri: Uri = FileProvider.getUriForFile(
                            requireContext(), Constants.authority,
                            outputImage
                        )
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        intent.putExtra("return-data", true)
                    } else {
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputImage))
                    }
                    startActivityForResult(intent, FILE_CAMERA_RESULT_CODE)
                }

                override fun onError() {

                }
            })
    }

    //选择图片
    private fun takePhoto() {
        KWApplication.instance.permissionsCheck(
            requireActivity() as BaseActivity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            R.string.permiss_write_stor2,
            object : Callback {
                override fun onSuccess() {
                    val i = Intent(Intent.ACTION_GET_CONTENT)
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    i.setType("image/*")
                    startActivityForResult(Intent.createChooser(i, "Image Chooser"), FILE_CHOOSER_RESULT_CODE)
                }

                override fun onError() {

                }
            })

    }

    var isClickBottomMenu = false
    private fun showCustomDialog() {
        BottomMenu.show(
            requireContext() as AppCompatActivity,
            arrayOf("拍照", "从相册选择"),
            object : OnMenuItemClickListener {
                public override fun onClick(text: String, index: Int) {
                    if (index == 0) {
                        // 2018/12/10 拍照
//                    requestCode = FILE_CAMERA_RESULT_CODE;
                        takeCamera()
                        isClickBottomMenu = true
                    } else if (index == 1) {
//                    requestCode = FILE_CHOOSER_RESULT_CODE;
                        // 2018/12/10 从相册选择
                        takePhoto()
                        isClickBottomMenu = true
                    } else {
//                    mUploadCallbackAboveL = null;
//                    mUploadMessage = null;
                        isClickBottomMenu = false
                    }
                }
            }).setOnDismissListener(object : OnDismissListener {
            public override fun onDismiss() {
                if (isClickBottomMenu) return
                if (mUploadCallbackAboveL != null) {
                    mUploadCallbackAboveL!!.onReceiveValue(null)
                    mUploadCallbackAboveL = null
                }
                if (mUploadMessage != null) {
                    mUploadMessage!!.onReceiveValue(null)
                    mUploadMessage = null
                }
            }
        })
    }

    //    private int requestCode = -2;
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        isClickBottomMenu = false
        if (null == mUploadMessage && null == mUploadCallbackAboveL) return
        if (resultCode != AppCompatActivity.RESULT_OK) { //同上所说需要回调onReceiveValue方法防止下次无法响应js方法
            if (mUploadCallbackAboveL != null) {
                mUploadCallbackAboveL!!.onReceiveValue(null)
                mUploadCallbackAboveL = null
            }
            if (mUploadMessage != null) {
                mUploadMessage!!.onReceiveValue(null)
                mUploadMessage = null
            }
            return
        }
        var result: Uri? = null
        if (requestCode == FILE_CAMERA_RESULT_CODE) {
            if (null != data && null != data.getData()) {
                result = data.getData()
            }
            if (result == null && hasFile(cameraFielPath)) {
                result = Uri.fromFile(File(cameraFielPath))
            }
            if (mUploadCallbackAboveL != null) {
                mUploadCallbackAboveL!!.onReceiveValue(arrayOf(result))
                mUploadCallbackAboveL = null
            } else if (mUploadMessage != null) {
                mUploadMessage!!.onReceiveValue(result)
                mUploadMessage = null
            }
        } else if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (data != null) {
                result = data.getData()
            }
            if (mUploadCallbackAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data)
            } else if (mUploadMessage != null) {
                mUploadMessage!!.onReceiveValue(result)
                mUploadMessage = null
            }
        }
    }

    /**
     * 判断文件是否存在
     */
    fun hasFile(path: String?): Boolean {
        try {
            val f: File = File(path)
            if (!f.exists()) {
                return false
            }
        } catch (e: Exception) {
            Log.i("error", e.toString())
            return false
        }
        return true
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun onActivityResultAboveL(requestCode: Int, resultCode: Int, intent: Intent?) {
        if ((requestCode != FILE_CAMERA_RESULT_CODE && requestCode != FILE_CHOOSER_RESULT_CODE
                    || mUploadCallbackAboveL == null)
        ) {
            return
        }
        var results: Array<Uri?>? = null
        if (intent != null) {
            val dataString: String? = intent.dataString
            val clipData: ClipData? = intent.clipData
            if (clipData != null) {
                results = arrayOfNulls(clipData.itemCount)
                for (i in 0 until clipData.itemCount) {
                    val item: ClipData.Item = clipData.getItemAt(i)
                    results[i] = item.uri
                }
            }
            if (dataString != null) results = arrayOf(Uri.parse(dataString))
        }
        mUploadCallbackAboveL!!.onReceiveValue(results)
        mUploadCallbackAboveL = null
    }

    var hasBack: Boolean = false    //是否有返回栈

    //系统自带监听方法
    public fun onBackPressed() : Boolean{
        if (wvWebView!!.canGoBack()) {
            wvWebView!!.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            wvWebView!!.goBack()
            hasBack = true
            return true
        } else {
            wvWebView!!.clearCache(true)
            wvWebView!!.clearHistory()
            hasBack = false
            title_back_btu?.visibility = View.GONE
            return false
        }
    }

    //类相关监听
    override fun onPause() {
        super.onPause()
        wvWebView!!.onPause()
    }

    override fun onResume() {
        wvWebView!!.onResume()
        super.onResume()
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (isVisibleToUser && isCreated && !mHasLoadedOnce) {
            wvWebView!!.loadUrl(URL)
            mHasLoadedOnce = true
        }
    }

    override fun onDestroy() {
        LogUtil.e("ShopFragment--------","======onDestroy")
        if (wvWebView != null) {
            wvWebView?.stopLoading()
            wvWebView?.destroy()
        }
        super.onDestroy()
    }

    companion object {
        //    ProgressBar pbWebView;
        var URL: String = "http://www.sslm03.com/static/llvy/index.html#/tour"
        //http://www.sslm03.com/static/llvy/index.html#/tour?locationName=%E5%94%90%E5%B1%B1%E5%B8%82

        private val FILE_CAMERA_RESULT_CODE: Int = 0
    }

    @SuppressLint("HandlerLeak")
    private fun loadServiceUrl(context: Context) {
        val request = RequestInfo()
        request.context = context
        request.reqUrl = HttpConfig.HOST + HttpConfig.INTERFACE_SHOP_URL
        val dataMap: HashMap<String, Any> = HashMap()
        request.reqDataMap = dataMap
        request.parser = NormalStringParse()
        request.handler = object : Handler() {
            override fun handleMessage(msg: Message) {
                val response: ResponseInfo = msg.obj as ResponseInfo
                val serviceUrl: String?
                if (response.status == 1) {
                    serviceUrl = response.responseData as String?
//                    val jsonContent = systemParam?.content
                    if (!serviceUrl.isNullOrEmpty()) {
                        URL = serviceUrl
                    } else {
                        ToastUtils.show("链接地址不存在！")
                    }


                } else {
                    ToastUtils.show(response.msg)
                }

                super.handleMessage(msg)
            }
        }
        ReqUtil.setReqInfo(request)
        ReqUtil.requestGetJSON(ResponseCallback(request))
    }

}