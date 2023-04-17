package com.android.tgsmf.view.webview

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.collection.arraySetOf
import androidx.core.content.PermissionChecker.checkSelfPermission
import com.android.tgsmf.R
import com.android.tgsmf.data.TGSWebCommand
import com.android.tgsmf.databinding.TgsViewWebviewBinding
import com.android.tgsmf.util.TGSFile
import com.android.tgsmf.util.TGSLog
import com.android.tgsmf.util.TGSToast
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener
import com.karumi.dexter.listener.multi.DialogOnAnyDeniedMultiplePermissionsListener
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.File
import java.net.URLDecoder

/*********************************************************************
 *
 *
 *********************************************************************/

private const val TGS_WEBVIEW_JAVASCRIPT_ENABLED = true
private const val TGS_WEBVIEW_SCRIPT_NAME = "TGSFW"

interface TGSJavascriptListener: Parcelable {
    fun onMessage(msg:String)
    fun onTitle(title:String)
    //fun onMessage(msg:String, params: String)

    override fun describeContents(): Int  = 0
    override fun writeToParcel(dest: Parcel?, flags: Int) {}
}

class TGSWebview : RelativeLayout {
    private val TAG = javaClass.simpleName

    private lateinit var mWebview:WebView
    private lateinit var mProgress: ProgressBar

    var mJavascriptListener : TGSJavascriptListener? = null
        set(_listener) {
            field = _listener
        }
    inner class JavascriptBridge {
        @JavascriptInterface
        fun onMessage(msg:String) {
            TGSLog.d("[${TAG}_TGSJavascriptBridge] onMessage : ${msg}")

            if(mJavascriptListener != null)
                mJavascriptListener!!.onMessage(msg)
        }

    }

    //-----------------------------------------------------------------
    constructor(context: Context): super(context) {
        initView(context)
    }
    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        initView(context)
        getAttrs(attrs)
    }
    constructor(context: Context, attrs: AttributeSet, defStyle: Int): super(context, attrs) {
        initView(context)
        getAttrs(attrs, defStyle)
    }

    //-----------------------------------------------------------------
    private fun initView(context:Context) {
        val binding = TgsViewWebviewBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)

        mWebview = binding.tgsWebview
        mWebview.run {
            webViewClient = mWebviewClient
            webChromeClient = mWebChromeClient
        }
        mProgress = binding.tgsProgressWebview
        mProgress.visibility = View.GONE

        initJavascript(TGS_WEBVIEW_JAVASCRIPT_ENABLED, TGS_WEBVIEW_SCRIPT_NAME)

        initDownloadListener(context)
    }


    //-----------------------------------------------------------------
    private fun getAttrs(attrs:AttributeSet){
        //아까 만들어뒀던 속성 attrs 를 참조함
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TGSMF_MENU_LIST)
        setTypeArray(typedArray)
    }
    private fun getAttrs(attrs: AttributeSet, defStyle: Int) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TGSMF_MENU_LIST, defStyle, 0)
        setTypeArray(typedArray)
    }
    //-----------------------------------------------------------------
    //디폴트 설정
    private fun setTypeArray(typedArray : TypedArray){
        typedArray.recycle()
    }

    //-----------------------------------------------------------------
    fun loadUrl(_url:String) {
        if(_url.isEmpty())
            return
        mWebview.loadUrl(_url)
    }

    //-----------------------------------------------------------------
    @SuppressLint("JavascriptInterface")
    fun initJavascript(javaScriptEnabled:Boolean, scriptName:String="") {

        mWebview.settings.javaScriptEnabled = javaScriptEnabled

        if(javaScriptEnabled && !scriptName.isEmpty()){
            mWebview.addJavascriptInterface(JavascriptBridge(), scriptName)
        }
    }

    //-----------------------------------------------------------------
    var mArrayDownloadFileIds = arraySetOf<Long>()
    private fun initDownloadListener(context:Context) {
        mWebview.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            // 저장소 접근 권한 체크
            if(context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                var downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                var _contentDisposition = URLDecoder.decode(contentDisposition, "UTF-8")

                // 파일명, mimetype 추출
                var _mimetype = mimetype
                var fileName = _contentDisposition.replace("attachment; filename=", "")
                if (fileName.trim().isEmpty()) {
                    fileName = URLDecoder.decode(TGSFile.GetFileNameToString(url), "UTF-8")
                }

                if (fileName != null && fileName.trim().isNotEmpty()) {
                    _mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(mimetype)

                    if (fileName.endsWith(";")) {
                        fileName = fileName.substring(0, fileName.length - 1)
                    }

                    if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                        fileName = fileName.substring(1, fileName.length - 1)
                    }
                }

                var outputFile = Environment.getExternalStoragePublicDirectory( "${Environment.DIRECTORY_DOWNLOADS}/${fileName}")

                // 파일 다운로드 시작
                var request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(_mimetype)
                    addRequestHeader("User-Agent", userAgent)
                    setDescription("Downloading File")
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                    setTitle(fileName)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        setRequiresCharging(false)
                    }

                    allowScanningByMediaScanner()
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationUri(Uri.fromFile(outputFile))
                }

                registerDownloadReceiver(downloadManager, context)
                var downloadId = downloadManager.enqueue(request)
                mArrayDownloadFileIds.add(downloadId)

            } else {

                val baseMultiplePermissionsListener: MultiplePermissionsListener = object :
                    MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if(report.areAllPermissionsGranted()) {
                            }
                        }
                    }
                    override fun onPermissionRationaleShouldBeShown(permissions: List<com.karumi.dexter.listener.PermissionRequest?>?, token: PermissionToken?) {
                        token?.continuePermissionRequest()
                    }
                }

                val dialogMultiplePermissionsListener: MultiplePermissionsListener = DialogOnAnyDeniedMultiplePermissionsListener.Builder
                    .withContext(context)
                    .withTitle("파일 다운로드 permission")
                    .withMessage("[파일 다운로드] 기능을 사용하기 위해 저장소 접근 권한이 필요합니다.")
                    .withButtonText("OK")
                    //.withIcon(R.drawable.ic_menu_camera)
                    .build()
                val compositePermissionsListener: MultiplePermissionsListener = CompositeMultiplePermissionsListener(dialogMultiplePermissionsListener, baseMultiplePermissionsListener)

                Dexter.withContext(context)
                    .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .withListener(compositePermissionsListener)
                    //.onSameThread()
                    .check()
            }

        }
    }

    private fun registerDownloadReceiver(downloadManager: DownloadManager, context:Context) {
        var downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                var id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: -1

                when (intent?.action) {
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {

                        if(mArrayDownloadFileIds.contains(id)){
                            mArrayDownloadFileIds.remove(id)

                            val query: DownloadManager.Query = DownloadManager.Query()
                            query.setFilterById(id)
                            var cursor = downloadManager.query(query)
                            if (!cursor.moveToFirst()) {
                                return
                            }

                            var columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            var status = cursor.getInt(columnIndex)
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                if(mJavascriptListener != null)
                                    mJavascriptListener!!.onMessage("TGSFW_COMD://show_toast?message=다운로드가 완료되었습니다.")

//                                activity.showToast("다운로드가 완료됐습니다.")
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                if(mJavascriptListener != null)
                                    mJavascriptListener!!.onMessage("TGSFW_COMD://show_toast?message=다운로드를 실패했습니다.")
//                                activity.showToast("다운로드가 실패했습니다.")
                            }
                        }
                    }
                }
            }
        }

        IntentFilter().run {
            addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            context.registerReceiver(downloadReceiver, this)
        }
    }


    //-----------------------------------------------------------------
    fun onGoBack():Boolean {
        if(mWebview.canGoBack()) {
            mWebview.goBack()
            return true
        }
        return false
    }

    //-----------------------------------------------------------------
    // 웹페이지를 로딩할때 생기는 콜백함수들로 구성
    private val mWebviewClient = object : WebViewClient() {
        private val TAG = javaClass.simpleName
        private val writeLog = true

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            if(writeLog) TGSLog.d("[${TAG} _onPageStarted] ${url}")
            super.onPageStarted(view, url, favicon)
            mProgress.visibility = View.VISIBLE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            if(writeLog) TGSLog.d("[${TAG} _onPageFinished] ${url}")
            println("onPageFinishedonPageFinishedonPageFinishedonPageFinishedonPageFinished")
            super.onPageFinished(view, url)

            if (view != null) {
                println(view.toString())
                ////view.loadUrl("javascript:scwin.appMenuClick('btnMnuBACH636')")
                view.loadUrl("javascript:$('#navbar_toggler').remove()")
            }
            mProgress.visibility = View.GONE
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            if(writeLog) TGSLog.d("[${TAG} _onLoadResource] ${url}")
            super.onLoadResource(view, url)
        }

        @TargetApi(Build.VERSION_CODES.M)
        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            if(writeLog) TGSLog.d("[${TAG} _onReceivedError] ${error?.description.toString()}")
            super.onReceivedError(view, request, error)
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldInterceptRequest(
            view: WebView?,
            request: WebResourceRequest?
        ): WebResourceResponse? {
            if(writeLog) TGSLog.d("[${TAG} _shouldInterceptRequest] ${request?.url.toString()}")
            return super.shouldInterceptRequest(view, request)
        }

        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            TGSLog.d("[${TAG} _shouldOverrideUrlLoading] ${request?.url.toString()}")
            if(view != null && request != null) {
                view.loadUrl(request.url.toString())
            }
            return true
        }


    }

    //-----------------------------------------------------------------
    // 웹페이지에서 일어나는 액션들에 관한 콜백함수들로 구성
    private val mWebChromeClient = object : WebChromeClient() {
        private val TAG = javaClass.simpleName
        private val writeLog = true

        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            if(title != null && !title.isEmpty()) {
                mJavascriptListener?.let { it.onTitle(title) }
            }
        }

        override fun onCreateWindow(view: WebView?,isDialog: Boolean,isUserGesture: Boolean,resultMsg: Message?): Boolean {
            TGSLog.i( "onCreateWindow url")
            val url = view?.url
            if (url != null) {
                TGSLog.i("[${TAG} _onCreateWindow] ${url}")
            }

            val newWebView = WebView(context).apply {
                settings.run {
                    javaScriptEnabled = true
                    setSupportMultipleWindows(false)
                }
            }

            newWebView.webChromeClient = object : WebChromeClient() {
                override fun onCloseWindow(window: WebView?) {}
            }
            (resultMsg?.obj as WebView.WebViewTransport).webView = newWebView
            resultMsg.sendToTarget()
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            if(writeLog) TGSLog.i("[${TAG} _onPermissionRequest]")

            if(writeLog) TGSLog.e( "onPermissionRequest")
            try {
                request?.grant(request.resources)
            } catch (e: Exception) {
                if(writeLog) TGSLog.e( "permissionRequest: $e")
            }
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri?>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
//            if(uploadMessage != null){ // 값이 존재하면 널값을 넣어 초기화해주어야 한다.
//                uploadMessage!!.onReceiveValue(null)
//            }
//            uploadMessage = filePathCallback
//
//            val intent = Intent()
//            intent.apply {
//                action = Intent.ACTION_GET_CONTENT
//                addCategory(Intent.CATEGORY_OPENABLE)
//                type = "*/*"
//            }
//            (this@WebViewSetting.context as MainActivity).requestActivity.launch(Intent.createChooser(intent, "File Chooser"))
            return true
        }

        override fun getDefaultVideoPoster(): Bitmap? {
            return Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        }
    }

}