package com.pplink.pagecall.sdk

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.pplink.pagecall.sdk.databinding.FragmentPagecallBinding
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val URL = "url"
private const val HTML = "html"

open class PagecallClient {
    open fun onExit() {}
}

private class PagecallClientInterface(private val pagecallClient: PagecallClient) {
    @JavascriptInterface
    fun onExit() {
        pagecallClient.onExit()
    }
}

class Pagecall : Fragment() {
    companion object {
        @JvmStatic
        fun newInstance(url: String, html: String?) =
            Pagecall().apply {
                arguments = Bundle().apply {
                    putString(URL, url)
                    putString(HTML, html)
                }
            }

        val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    }

    open var pagecallClient: PagecallClient = PagecallClient()
    open var customJavascript: String? = null

    private lateinit var _url: String
    private var _html: String? = null
    private lateinit var _binding: FragmentPagecallBinding
    private lateinit var _webView: WebView
    private var _filePathCallback: ValueCallback<Array<Uri>>? = null

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("PagecallPermission", "${it.key} = ${it.value}")
            }
        }

    private val filterActivityLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // WebView 내부에서 파일 선택자를 열기 위한 로직
            if (it.resultCode == Activity.RESULT_OK && it.data != null) {
                var results: Array<Uri>? = null

                it.data?.let { data ->
                    data.dataString?.let { dataString ->
                        results = arrayOf(Uri.parse(dataString))
                    }
                }
                _filePathCallback!!.onReceiveValue(results)
            } else {
                // 에러 또는 선택된 파일이 없더라도 반드시 초기화 해주어야 한다.
                // 그렇지 않으면 다시 파일 선택자가 열리지 않는다.
                _filePathCallback!!.onReceiveValue(null)
            }

            _filePathCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            _url = it.getString(URL)!!
            _html = it.getString(HTML)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requestMultiplePermissionsLauncher.launch(PERMISSIONS)
        _binding = FragmentPagecallBinding.inflate(layoutInflater, container, false)

        _webView = _binding.webView

        _webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.grant(request.resources)
                }

                // 파일 업로드를 위한 설정
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (_filePathCallback != null) {
                        _filePathCallback!!.onReceiveValue(null)
                        _filePathCallback = null
                    }

                    try {
                        _filePathCallback = filePathCallback
                        val intent = Intent()
                        intent.apply {
                            action = Intent.ACTION_GET_CONTENT
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams!!.acceptTypes)
                        }

                        filterActivityLauncher.launch(intent)
                    } catch (e: Exception) {
                        _filePathCallback!!.onReceiveValue(null)
                        _filePathCallback = null
                    }

                    return true
                }
            }
        }

        CookieManager.getInstance().setAcceptThirdPartyCookies(_webView, true)

        // Pagecall Client 에 객체 주입. Android.something() <- 이런 식으로 네이티브 브릿지에 액세스 가능.
        // 객체 이름을 바꾸고 싶다면, "Android" 를 변경해주면 된다.
        _webView.addJavascriptInterface(PagecallClientInterface(pagecallClient), "Android")

        // 커스텀 자바스크립트 기능을 위해, url, html 여부 상관없이, 모두 html 을 불러와서 렌더링한다.
        lifecycleScope.launch {
            var html = if (_html == null) {
                val urlConnection = URL(_url).openConnection() as HttpURLConnection

                withContext(Dispatchers.IO) {
                    urlConnection.inputStream.bufferedReader().readText()
                }
            } else {
                _html!!
            }

            if (customJavascript != null) {
                html = html.replace(
                    "<head>", "" +
                            "<head>\n" +
                            "<script>\n" +
                            "$customJavascript\n" +
                            "</script>"
                )
            }
            // url 을 넣어주지 않으면, API, Socket 등이 정상적으로 동작하지 않음.
            _webView.loadDataWithBaseURL(_url, html, "text/html", "utf-8", null)
        }

        return _binding.root
    }
}