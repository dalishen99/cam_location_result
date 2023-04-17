package com.apro.cereal.ui.hybrid

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslCertificate
import android.net.http.SslError
import android.os.Build
import android.util.AttributeSet
import android.view.WindowManager
import android.webkit.*
import com.apro.cereal.R
import com.apro.cereal.common.Constants
import com.apro.cereal.ui.hybrid.plugin.Plugin
import com.apro.cereal.net.CerealSSLSocketFactory
import com.apro.cereal.net.ServiceConfig
import com.apro.cereal.network.cerealware.network.CerealURLSession
import com.apro.cereal.ui.activities.MainActivity
import com.apro.cereal.ui.dialogs.CustomAlertDialog
import com.apro.cereal.utils.LogUtil
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * 하이브리드 화면용 웹뷰
 * 플러그인 로드
 */
class HybridView : WebView {
    var activity: Activity? = null
    var viewId: String? = null
    lateinit var plugin: Plugin

    constructor(context: Context?) : super(context!!) {
        if (!isInEditMode) {
            initialize()
        }
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        if (!isInEditMode) {
            initialize()
        }
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    ) {
        if (!isInEditMode) {
            initialize()
        }
    }

    @SuppressLint("NewApi")
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context!!, attrs, defStyleAttr, defStyleRes
    ) {
        if (!isInEditMode) {
            initialize()
        }
    }

    var onLoadFinished: OnLoadFinishedListener? = null
    fun callback(callback: String?, s: String?) {}

    interface OnLoadFinishedListener {
        fun onLoadFinished()
    }

    interface OnProgressChangeListener {
        fun onProgressChanged(hybridView: HybridView?, webView: WebView?, progress: Int)
    }

    @SuppressLint("JavascriptInterface")
    private fun initialize() {
        webChromeClient = HybridViewWebChromeClient()
        webViewClient = HybridViewWebViewClient()

        with(settings) {
            loadsImagesAutomatically = true
            blockNetworkImage = false
            domStorageEnabled = true
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            savePassword = false
            javaScriptEnabled = true
            allowUniversalAccessFromFileURLs = true
            textZoom = 100
            setSupportZoom(true)
            builtInZoomControls = true
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            allowFileAccess = true
            setSupportMultipleWindows(false)
            //setSupportMultipleWindows(true);
            //setJavaScriptCanOpenWindowsAutomatically(true);
            pluginState = WebSettings.PluginState.ON
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            userAgentString += ServiceConfig.USER_AGENT
        }
        setWebContentsDebuggingEnabled(true)
        addJavascriptInterface(Plugin(this).also { plugin = it }, "CerealPluginJSNI")
    }

    inner class HybridViewWebChromeClient : WebChromeClient() {
        override fun onJsAlert(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            LogUtil.d("Hybrid", "onJsAlert($url, $message, $result)")
            CustomAlertDialog.show(
                activity,
                message,
                context.getString(R.string.confirm),
                { dialogInterface: DialogInterface, i: Int ->
                    result.confirm()
                    dialogInterface.dismiss()
                },
                null,
                null,
                { dialogInterface: DialogInterface, i: Int ->
                    result.cancel()
                    dialogInterface.dismiss()
                },
                false
            )
            return true
        }

        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
            return if (Constants.IS_DEBUG) super.onConsoleMessage(consoleMessage) else true
        }

        override fun onJsConfirm(
            view: WebView,
            url: String,
            message: String,
            result: JsResult
        ): Boolean {
            LogUtil.d("Hybrid", "onJsConfirm($url, $message, $result)")
            CustomAlertDialog.show(
                activity,
                message,
                context.getString(R.string.confirm),
                { dialogInterface: DialogInterface, i: Int ->
                    result.confirm()
                    dialogInterface.dismiss()
                },
                context.getString(R.string.cancel),
                { dialogInterface: DialogInterface, i: Int ->
                    result.cancel()
                    dialogInterface.dismiss()
                },
                { dialogInterface: DialogInterface, i: Int ->
                    result.cancel()
                    dialogInterface.dismiss()
                },
                false
            )
            return true
        }

        override fun onJsPrompt(
            view: WebView,
            url: String,
            message: String,
            defaultValue: String,
            result: JsPromptResult
        ): Boolean {
            result.cancel()
            return true
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            callback.invoke(origin, true, false)
        } /*        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg)
        {
            WebView newWebView = new WebView(getActivity());
            WebSettings webSettings = newWebView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            final Dialog dialog = new Dialog(getActivity());
            dialog.setContentView(newWebView);
            dialog.show();
            newWebView.setWebChromeClient(new WebChromeClient()
            {
                @Override
                public void onCloseWindow(WebView window)
                {
                    dialog.dismiss();
                }
            });
            ((WebView.WebViewTransport) resultMsg.obj).setWebView(newWebView);
            resultMsg.sendToTarget();

            return true;
        }*/
    }

    val commonPassword = Constants.SERVER_URL + "/common/commonPassword.do"

    inner class HybridViewWebViewClient : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            if (url?.startsWith(Constants.MAIN_URL) == true && activity is MainActivity) {
                when ((activity as MainActivity).navPosition) {
                    0 -> {
                    }
                    4 -> (activity as MainActivity).navigateHybridToMore()
                    else -> (activity as MainActivity).onSupportNavigateUp()
                }
                return
            }

            LogUtil.d("Hybrid", "HybridViewWebViewClient onPageStarted($view, $url)")
            LogUtil.d(
                "Cookie",
                " -->> Hybrid CookieManager setCookie ? " + CerealURLSession.m_cookies
            )
            Timber.d("url: %s", url)
            CookieManager.getInstance().setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 오픈뱅킹 관련 추가
                // 인증서 관련 쿠키 설정(크로스사이트 문제). 인증서를 사용하는 경우, 이 옵션이 없으면 클라우드 서비스 연결안됨
                activity?.runOnUiThread {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
                }
            }
            if (url?.contains(ServiceConfig.HOSTNAME_OPENBANKING) == false
                && !url.contains(ServiceConfig.HOSTNAME_KMCERT)
            ) {
                CookieManager.getInstance().setCookie(url, CerealURLSession.m_cookies)
            }
            if (activity is HybridActivity) {
                (activity as HybridActivity).url = url
            } else if (activity is HybridSubActivity) {
                (activity as HybridSubActivity).url = url
            }
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            LogUtil.d("Hybrid", "HybridViewWebViewClient onPageFinished($view, $url)")

            onLoadFinished?.onLoadFinished()

            if (url == commonPassword) {
                /*kosama*/
                try {
                    activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                /*kosama*/
                try {
                    activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

//OJKIM TEST START - 추가팝업창처리 테스트
//            final WebView aView = view;
//            if (url != null && url.equals("https://m.mycereal.co.kr:8443/matcs/inv/invSubDetail.do")) {
//                String testScript = "CerealPluginObj.prototype.callHybridKindPopup = function (url, param, callback) {";
//                testScript += "if(OS_TYPE===OS_AND){";
//                testScript += "CerealPluginJSNI.callHybridKindPopup(url, param, CerealRegisterCallback(callback));";
//                testScript += "}else if(OS_TYPE===OS_IOS){";
//                testScript += "var callbackArray = [callback];";
//                testScript += "var args = [url, param];";
//                testScript += "iOSPluginJSNI.exec(callbackArray, 'callHybridKindPopup', args)";
//                testScript += "}";
//                testScript += "};";
//                testScript += "function testCallback() {";
//                testScript += "alert('callback callback');";
//                testScript += "}";
//                view.evaluateJavascript(testScript, new ValueCallback<String>() {
//                    @Override
//                    public void onReceiveValue(String s) {
//                        String ts = "";
//                        ts += "function callHybridKindpopup(url, params, callback) {\n";
//                        ts += "    CerealPlugin.callHybridKindPopup(url, params, callback);\n";
//                        ts += "}\n";
//                        ts += "var tParam = {};\n";
//                        ts += "var returnParameter = {};\n";
//                        ts += "returnParameter.title = \"상품설명\";\n";
//                        ts += "tParam.parameter = returnParameter;\n";
//                        ts += "tParam.titleType = \"1\";\n";
//                        ts += "tParam.callType = \"3\";\n";
//                        ts += "callHybridKindpopup(\"http://www.daum.net\", JSON.stringify(tParam));\n";
////                        String ts = "CerealPlugin.callHybridKindPopup(\"http://www.daum.net\", '{\"titleType\":\"1\",\"title\":\"TestTitle\",\"callType\":\"1\"}', \"testCallback\")";
//                        aView.evaluateJavascript(ts, new ValueCallback<String>() {
//                            @Override
//                            public void onReceiveValue(String s) {
//                            }
//                        });
//                    }
//                });
//            }

//            final WebView aView = view;
//            if (url != null && url.equals("https://dm.mycereal.co.kr:8443/matcs/inv/invSubDetail.do")) {
//                String testScript = "CerealPluginObj.prototype.callHybridKindPopup = function (url, param, callback) {";
//                testScript += "if(OS_TYPE===OS_AND){";
//                testScript += "CerealPluginJSNI.callHybridKindPopup(url, param, CerealRegisterCallback(callback));";
//                testScript += "}else if(OS_TYPE===OS_IOS){";
//                testScript += "var callbackArray = [callback];";
//                testScript += "var args = [url, param];";
//                testScript += "iOSPluginJSNI.exec(callbackArray, 'callHybridKindPopup', args)";
//                testScript += "}";
//                testScript += "};";
//                testScript += "function testCallback() {";
//                testScript += "alert('callback callback');";
//                testScript += "}";
//                view.evaluateJavascript(testScript, new ValueCallback<String>() {
//                    @Override
//                    public void onReceiveValue(String s) {
//                        String ts = "";
//                        ts += "function callHybridKindpopup(url, params, callback) {\n";
//                        ts += "    CerealPlugin.callHybridKindPopup(url, params, callback);\n";
//                        ts += "}\n";
//                        ts += "var tParam = {};\n";
//                        ts += "var returnParameter = {};\n";
//                        ts += "returnParameter.title = \"상품설명\";\n";
//                        ts += "tParam.parameter = returnParameter;\n";
//                        ts += "tParam.titleType = \"1\";\n";
//                        ts += "tParam.callType = \"3\";\n";
////                        ts += "callHybridKindpopup(\"http://www.daum.net\", JSON.stringify(tParam));\n";
//                        ts += "CerealPluginJSNI.showHybridLevelPopup(\"Lv.99가 되셨습니다.<br>축하합니다. \",99,9);\n";
////                        String ts = "CerealPlugin.callHybridKindPopup(\"http://www.daum.net\", '{\"titleType\":\"1\",\"title\":\"TestTitle\",\"callType\":\"1\"}', \"testCallback\")";
//                        aView.evaluateJavascript(ts, new ValueCallback<String>() {
//                            @Override
//                            public void onReceiveValue(String s) {
//                            }
//                        });
//                    }
//                });
//            }
//OJKIM TEST END
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
        }

        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            LogUtil.d(
                "Hybrid",
                "HybridViewWebViewClient onReceivedUrlError($view, $description, $failingUrl)"
            )
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            val sslCert = error?.certificate.toString()
            val sslSocketFactory = CerealSSLSocketFactory.Builder(context).build()
            val cerealCert = SslCertificate(sslSocketFactory.cert).toString()
            val sslSocketFactory2 =
                CerealSSLSocketFactory.Builder(context).setCert(R.raw.okfngroup).build()
            val okfngroupCert = SslCertificate(sslSocketFactory2.cert).toString()
            val sslSocketFactory3 =
                CerealSSLSocketFactory.Builder(context).setCert(R.raw.cert).build()
            val cerealCert2 = SslCertificate(sslSocketFactory3.cert).toString()
            val isContain = listOf(cerealCert, okfngroupCert, cerealCert2).contains(sslCert)
            if (isContain) {
                handler?.proceed()
            } else {
                handler?.cancel()
            }
        }

        @SuppressWarnings("deprecation")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url?.startsWith("tel:") == true
                || url?.startsWith("sms:") == true
                || url?.startsWith("mailto:") == true
            ) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                return true
            }
            if (url == "#") return true
            if (url?.startsWith("intent://") == true) {
                try {
                    val insideIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    val existPackage = context.packageManager.getLaunchIntentForPackage(
                        insideIntent.getPackage()!!
                    )
                    if (existPackage != null) {
                        context.startActivity(insideIntent)
                    } else {
                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        marketIntent.data =
                            Uri.parse("market://details?id=" + insideIntent.getPackage())
                        context.startActivity(marketIntent)
                    }
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return super.shouldOverrideUrlLoading(view, url)
        }

        // 이전 버전 단말기들에서 발생하는 오류 해결하기 위해 넣었으나
        // SSL 정책 심사 위반되어 주석 처리.
        // 이전 버전 단말기 접근을 위해 해결 필요
        //        @Override
        //        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        //
        //            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
        //                handler.proceed();
        //            } else {
        //                handler.cancel();
        //            }
        //        }
        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url: String = request?.url.toString()
            if (url == "#") return true
            if (url.startsWith("intent://")) {
                try {
                    val insideIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    val existPackage =
                        context.packageManager.getLaunchIntentForPackage(insideIntent.getPackage()!!)
                    if (existPackage != null) {
                        context.startActivity(insideIntent)
                    } else {
                        val marketIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("market://details?id=" + insideIntent.getPackage())
                        }
                        context.startActivity(marketIntent)
                    }
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return super.shouldOverrideUrlLoading(view, request)
        }

        override fun onLoadResource(view: WebView?, url: String?) {
            super.onLoadResource(view, url)
        }

        @SuppressLint("NewApi")
        override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
            return super.shouldInterceptRequest(view, url)
        }
    }

    fun callScript(functionName: String?, vararg arguments: Any) {
        val buffer = StringBuilder()
        buffer.append("try {")
        buffer.append(functionName).append("(")
        for (i in arguments.indices) {
            when (val `object` = arguments[i]) {
                is JSONObject -> {
                    buffer.append(`object`.toString())
                }
                is JSONArray -> {
                    buffer.append(`object`.toString())
                }
                else -> {
                    buffer.append("\"")
                    buffer.append(`object`.toString())
                    buffer.append("\"")
                }
            }
            if (i < arguments.size - 1) buffer.append(",")
        }
        buffer.append(");} catch(e) { console.log(e); }")
        val script = buffer.toString()
        activity?.runOnUiThread(CallbackRunnable(script))
        LogUtil.e("script", script)
        LogUtil.d("Hybrid", script)
    }

    internal inner class CallbackRunnable(val script: String) : Runnable {
        override fun run() {
            evaluateJavascript(script, null)
        }
    }
}