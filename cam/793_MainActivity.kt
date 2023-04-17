package dev.affan.sinergi.activity

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.tapadoo.alerter.Alerter
import dev.affan.sinergi.R
import dev.affan.sinergi.data.Constant
import dev.affan.sinergi.utils.NetworkCheck
import kotlinx.android.synthetic.main.activity_main.*
import pl.droidsonroids.gif.GifImageView
import android.webkit.ValueCallback
import android.content.ActivityNotFoundException
import androidx.core.app.ActivityCompat.startActivityForResult
import android.content.Intent
import android.app.Activity
import dev.affan.sinergi.User
import dev.affan.sinergi.data.SharedPref
import es.dmoral.toasty.Toasty

class MainActivity : BaseActivity() {
    init {
        isFullScreen = false
    }

    private var isAllowedToExit = false
    private var url = ""
    private val webChromeClient = ChromeClient(this)
    private lateinit var sharedPref: SharedPref

    override fun initContentView() {
        setContentView(R.layout.activity_main)
    }

    override fun initComponents() {
        sharedPref = SharedPref(this)
        loading.visibility = View.VISIBLE
        if (NetworkCheck.isConnect(this)) {
            initWebView()
        } else {
            Alerter.create(this@MainActivity)
                .setTitle("NET ERR")
                .setText("Tidak dapat terhubung ke jaringan")
                .setBackgroundColorRes(R.color.danger)
                .setIcon(R.drawable.ic_sentiment_dissatisfied_black_24dp)
                .setIconColorFilter(Color.WHITE)
                .addButton(
                    getString(R.string.label_coba_lagi),
                    R.style.AlerterButtonDefault,
                    View.OnClickListener {
                        if (NetworkCheck.isConnect(this)) {
                            Alerter.clearCurrent(this)
                            initWebView()
                        }
                    })
                .addButton("Tutup", R.style.AlerterButtonWarning, View.OnClickListener { finish() })
                .enableSwipeToDismiss()
                .setDuration(3000)
                .show()
        }
        swipeLayout.setOnRefreshListener {
            loading.visibility = View.VISIBLE
            webview.reload()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webview.settings.javaScriptEnabled = true
        webview.settings.databaseEnabled = true
        webview.settings.domStorageEnabled = true
        webview.settings.builtInZoomControls = false
        webview.settings.displayZoomControls = false
        webview.settings.loadsImagesAutomatically = true
        webview.settings.useWideViewPort = true
        webview.settings.minimumFontSize = 1
        webview.settings.minimumLogicalFontSize = 1
        webview.settings.allowContentAccess = true
        webview.settings.allowFileAccess = true
        webview.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webview.setInitialScale(90)
        val client = WebClient(loading)
        webview.addJavascriptInterface(WebAppInterface(this, this).apply {
            setOnUrlChangedListener(object : OnUrlChangedListener {
                override fun onUrlChanged(url: String?) {
                    Log.d("url", url)
                    this@MainActivity.url = url!!
                }
            })
        }, "android")
        client.injectScript(
            """
                android.changeUrl(window.location.href);
                console.log(navigator.userAgent);
            """.trimIndent()
        )
        client.injectScript(
            """
                (function() {
                    if(window.location.href.includes('login')){
                        var login = document.getElementById('login');
                        login.onclick = function(){
                            var user = document.getElementById('user').value;
                            var pass = document.getElementById('pass').value;
                            if(user != '' && pass != ''){
                                var xhr = new XMLHttpRequest();
                                xhr.open('POST', "http://sinergi.sumenepkab.go.id/ceck_log.php", true);
                                xhr.onreadystatechange = function () {
                                    if (this.readyState != 4) return;
    
                                    if (this.status == 200) {
                                        var data = JSON.parse(this.responseText);
                                        if(data.login === 'success'){
                                            android.saveUser(user,pass);
                                            android.login();
                                            document.location = "http://sinergi.sumenepkab.go.id/index.php";    
                                        } else{
                                            android.showAlerterError("tidak dapat login, username atau password salah");
                                        }
                                    } else {
                                        android.showAlerterError("tidak dapat login, username atau password salah");
                                    }
                                };
    
                                xhr.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');;
                                xhr.send('username='+user+'&pass='+pass);
                            } else {
                                android.showAlerterError("username dan password tidak boleh kosong");
                            }
                        };
                    }
                })();
            """.trimIndent()
        )
        client.setOnPageLoadedListener(object : OnPageLoaded {
            override fun onPageLoaded(url: String) {
                swipeLayout.isRefreshing = false
                loading.visibility = View.GONE
                if (url.contains("logout"))
                    sharedPref.isLogIn = false
                if (url.contains("login")) {
                    webview?.loadUrl(
                        "javascript:(function() {" +
                                """
                                        document.getElementById('user').value = '${sharedPref.user?.nip}';
                                        document.getElementById('pass').value = '${sharedPref.user?.pass}';
                                        """.trimIndent() +
                                "})()"
                    )
                }
            }
        })

        client.setOnPageErrorListener(object : OnPageError {
            override fun onPageErrorListener(errorCode: Int, title: String, message: String) {
                Log.d("errCode", errorCode.toString())
                Log.d("title", title)
                Log.d("message", message)
                webview.loadUrl("file:///android_asset/html/error.html")
                webview.clearHistory()
            }
        })
        webview.webViewClient = client
        webview.webChromeClient = webChromeClient
        webview.isScrollbarFadingEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webview, true)
        }
        webview.loadUrl(Constant.DOWNLOAD_URL)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data?.let {
            webChromeClient.handleResult(requestCode, resultCode, it)
        }
    }

    override fun onBackPressed() {
        if (isAllowedToExit)
            finish()
        else {
            if (webview.canGoBack() && !(this.url.contains("login", true) || this.url.contains("dashboard", true)))
                webview.goBack()
            else {
                isAllowedToExit = true
                Handler().postDelayed({ isAllowedToExit = false }, 2000)
                Toasty.normal(this, "Tekan sekali lagi untuk keluar").show()
            }
        }
    }

    override fun onDestroy() {
        webview.destroy()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        webview.onResume()
    }

    override fun onPause() {
        super.onPause()
        webview.onPause()
    }
}

class WebAppInterface(private val mContext: Context, private val activity: Activity) {
    private var onUrlChangedListener: OnUrlChangedListener? = null
    private val sharedPref: SharedPref = SharedPref(mContext)

    fun setOnUrlChangedListener(onUrlChangedListener: OnUrlChangedListener) {
        this.onUrlChangedListener = onUrlChangedListener
    }

    @JavascriptInterface
    fun showToast(toast: String) {
        Toasty.info(mContext, toast, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun saveUser(user: String, pass: String) {
        Log.d("saveUser", "$user $pass")
        sharedPref.user = User(user, pass)
    }

    @JavascriptInterface
    fun login() {
        Log.d("isLogin", "true")
        sharedPref.isLogIn = true
    }

    @JavascriptInterface
    fun logout() {
        sharedPref.isLogIn = false
    }

    @JavascriptInterface
    fun showAlerterError(msg: String) {
        Alerter.create(activity)
            .setTitle("Galat....!")
            .setText(msg)
            .setBackgroundColorRes(R.color.danger)
            .setIcon(R.drawable.ic_sentiment_dissatisfied_black_24dp)
            .setIconColorFilter(Color.WHITE)
            .addButton("OK", R.style.AlerterButtonWarning, View.OnClickListener { Alerter.hide() })
            .enableSwipeToDismiss()
            .setDuration(3000)
            .show()
    }

    @JavascriptInterface
    fun changeUrl(url: String) {
        onUrlChangedListener?.onUrlChanged(url)
    }
}

interface OnUrlChangedListener {
    fun onUrlChanged(url: String?)
}

interface OnPageLoaded {
    fun onPageLoaded(url: String)
}

interface OnPageError {
    fun onPageErrorListener(errorCode: Int, title: String, message: String)
}

class WebClient(private val loading: GifImageView) : WebViewClient() {
    private var jsCode: String = ""
    private var cssCode: String = ""
    private var headers: MutableMap<String, String> = mutableMapOf()
    private var onPageLoaded: OnPageLoaded? = null
    private var onPageError: OnPageError? = null

    fun setOnPageLoadedListener(onPageLoaded: OnPageLoaded) {
        this.onPageLoaded = onPageLoaded
    }

    fun setOnPageErrorListener(onPageError: OnPageError) {
        this.onPageError = onPageError
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        if (url?.contains("dashboard", false)!!.or(url.removeSuffix("/") == Constant.DOWNLOAD_URL))
            view?.clearHistory()
        onPageLoaded?.onPageLoaded(url)
        try {
            view?.loadUrl(
                "javascript:(function() {" +
                        "var parent = document.getElementsByTagName('body').item(0);" +
                        "var script = document.createElement('script');" +
                        "script.type = 'application/javascript';" +
                        // Tell the browser to BASE64-decode the string into your script !!!
                        "script.innerHTML = '${jsCode.replace("'", "\"")}';" +
                        "parent.appendChild(script);" +
                        "})()"
            )
            view?.loadUrl(
                "javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        // Tell the browser to BASE64-decode the string into your script !!!
                        "style.innerHTML = '${cssCode.replace("'", "\"")}';" +
                        "parent.appendChild(style);" +
                        "})()"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onPageFinished(view, url)
    }

    @TargetApi(Build.VERSION_CODES.M)
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        Log.d("webclient error", "${error?.errorCode}: ${error?.description}")
        onPageError?.onPageErrorListener(error?.errorCode!!, "Kesalahan", error.description.toString())
        super.onReceivedError(view, request, error)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?
    ) {
        Log.d("webclient error", "${errorResponse?.statusCode}: ${errorResponse?.reasonPhrase}")
        super.onReceivedHttpError(view, request, errorResponse)
    }

    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        Log.d("webclient error", "$errorCode: $description <$failingUrl>")
        super.onReceivedError(view, errorCode, description, failingUrl)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        loading.visibility = View.VISIBLE
        view?.loadUrl(request?.url.toString(), headers)
        return true
    }

    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        loading.visibility = View.VISIBLE
        view?.loadUrl(url, headers)
        return true
    }

    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
        Log.d("dontResend", dontResend?.data.toString())
        Log.d("resend", resend?.data.toString())
        super.onFormResubmission(view, dontResend, resend)
    }

    fun injectScript(script: String) {
        this.jsCode += script
    }

    fun injectStyle(style: String) {
        this.cssCode += style
    }

    fun setHeaders(headers: Map<String, String>) {
        this.headers.putAll(headers)
    }

    fun addHeader(name: String, value: String) {
        this.headers[name] = value
    }
}

class ChromeClient(private val activity: BaseActivity) : WebChromeClient() {
    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private var mUploadMessage: ValueCallback<Uri>? = null

    companion object {
        const val REQUEST_SELECT_FILE = 100
        const val FILECHOOSER_RESULTCODE = 1
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Log.d("console", consoleMessage?.message())
        return super.onConsoleMessage(consoleMessage)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onPermissionRequest(request: PermissionRequest?) {
        Log.d("permission", request?.resources.toString())
        request?.grant(request.resources)
        super.onPermissionRequest(request)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        uploadMessage?.let {
            it.onReceiveValue(null)
            uploadMessage = null
        }
        uploadMessage = filePathCallback
        val intent = fileChooserParams?.createIntent()
        try {
            startActivityForResult(activity, intent!!, REQUEST_SELECT_FILE, null)
        } catch (e: ActivityNotFoundException) {
            uploadMessage = null
            Toasty.error(activity, "Cannot Open File Chooser", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
//        return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    fun handleResult(requestCode: Int, resultCode: Int, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null)
                    return
                uploadMessage?.onReceiveValue(FileChooserParams.parseResult(resultCode, intent))
                uploadMessage = null
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage)
                return
            val result = if (resultCode != Activity.RESULT_OK) null else intent.data
            mUploadMessage?.onReceiveValue(result)
            mUploadMessage = null
        } else
            Toasty.error(activity, "Failed to Upload Image", Toast.LENGTH_LONG).show()
    }

}