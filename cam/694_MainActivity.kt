package com.neocartek.webview

import android.Manifest
import android.R.id
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.webkit.*
import android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebExtension
import org.json.JSONObject

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import org.mozilla.geckoview.GeckoResult
import org.json.JSONException

import android.R.id.message
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import org.mozilla.geckoview.WebExtension.*
import java.lang.RuntimeException
import kotlin.reflect.typeOf


const val HOST_URL = "https://www.neocartek-sf.cf/camera"
const val PERMISSIONS_REQUEST_CODE = 100

class MainActivity : AppCompatActivity(), WebExtension.MessageDelegate{
    private var mContext: Context? = null
    private var mWebView // 웹뷰 선언
            : WebView? = null
    private var mWebSettings //웹뷰세팅
            : WebSettings? = null
    private var mAddressText
            : TextView? = null

    private var mTestButton: Button? = null

    private var mCaptureImage: ImageView? = null

    private var mGeckoView: GeckoView? = null
    private var mSession: GeckoSession? = null
    private var mRuntime: GeckoRuntime? = null
    private var mExtension: WebExtension? = null

    private var mPort: WebExtension.Port? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContext = this
        mAddressText = findViewById<TextView>(R.id.address)

        mTestButton = findViewById(R.id.test_button)
        mTestButton?.setOnClickListener {
            if (mPort == null) {
                return@setOnClickListener
            }

            val message = JSONObject()
            try {
                Log.e("push", "button")
                message.put("resolution", "720")
//                message.put("keyCode", keyCode)
//                message.put("event", KeyEvent.keyCodeToString(event.getKeyCode()))
            } catch (ex: JSONException) {
                throw RuntimeException(ex)
            }

            mPort!!.postMessage(message)
        }
        mCaptureImage = findViewById(R.id.cap_image)

        mGeckoView = findViewById<GeckoView>(R.id.geckoview)
        mSession = GeckoSession()
        mRuntime = GeckoRuntime.create(this)

        mSession!!.permissionDelegate = WebViewPermissionDelegate(this)

        mRuntime!!.webExtensionController.list().accept {
            if (it != null) {
                for (extension in it){
                    Log.e("extension", extension.toString())
                }
            }
        }
        Log.e("extensionv","asfafasf");

        Log.e("extensionv", mRuntime!!.webExtensionController.list().toString())
//        mRuntime!!.webExtensionController.update(mExtension!!)
//            .map { newExtension ->
//                runOnUiThread{
//                    newExtension!!.setMessageDelegate(this , "browser")
//                }
//            }

        val downloadDelegate: DownloadDelegate = object : DownloadDelegate {
            override fun onDownload(
                source: WebExtension,
                request: DownloadRequest
            ): GeckoResult<DownloadInitData>? {
                Log.e("download", "??????????")
                return super.onDownload(source, request)
            }
        }

        val portDelegate: PortDelegate = object : PortDelegate {
            var port: Port? = null
            override fun onPortMessage(
                message: Any,
                port: Port
            ) {
                // This method will be called every time a message is sent from the
                // extension through this port. For now, let's just log a
                // message.

                //data:image/png;base64,
                val data = message.toString().drop(23)
                val imageBytes = Base64.decode(data, Base64.DEFAULT)
                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                mCaptureImage?.setImageBitmap(decodedImage)

                Log.e(
                    "PortDelegate", "Received message from WebExtension: " +
                            decodedImage
                )
            }

            override fun onDisconnect(port: Port) {
                // After this method is called, this port is not usable anymore.
                if (port === mPort) {
                    mPort = null
                }
            }
        }
        val messageDelegate: MessageDelegate = object : MessageDelegate {
            override fun onConnect(port: Port) {
                Log.e("connected", port.name)
                mPort = port;

                // Registering the delegate will allow us to receive messages sent
                // through this port.
                mPort!!.setDelegate(portDelegate);
            }

            @Nullable
            override fun onMessage(
                nativeApp: String,
                message: Any,
                sender: MessageSender
            ): GeckoResult<Any>? {
                Log.e("MessageDelegate", message.toString())

                if (message is JSONObject) {
                    val json = message as JSONObject
                    try {
                        if (json.has("type") && "WPAManifest" == json.getString("type")) {
                            val manifest = json.getJSONObject("manifest")
                            Log.e("MessageDelegate", "Found WPA manifest: $manifest")
                        }
                    } catch (ex: JSONException) {
                        Log.e("MessageDelegate", "Invalid manifest", ex)
                    }
                }
                return null
            }
        }

        mRuntime!!.webExtensionController
            .ensureBuiltIn("resource://android/assets/messaging/", "messaging@example.com")
            .accept(
                { extension ->
                    runOnUiThread {
                        Log.e("MessageDelegate", "Extension installed: $extension")
                        mSession!!.webExtensionController
                            .setMessageDelegate(extension!!, messageDelegate, "browser")

                        extension.setMessageDelegate(messageDelegate, "browser")
                        extension.downloadDelegate = downloadDelegate
                    }
                }
            ) { e -> Log.e("MessageDelegate", "Error registering WebExtension", e) }

        mSession!!.settings.allowJavascript = true
        mSession!!.open(mRuntime!!)
        mGeckoView!!.setSession(mSession!!)
        mSession!!.loadUri(HOST_URL)

/*      WebView Code
//        // 웹뷰 시작
//        mWebView = findViewById<WebView>(R.id.webView)
//        mWebView!!.webViewClient = MyWebViewClient()
//        mWebView!!.webChromeClient = object : WebChromeClient() {
//            // Grant permissions for cam
//            override fun onPermissionRequest(request: PermissionRequest) {
//                Log.d("wcc", "onPermissionRequest")
//                runOnUiThread {
//                    Log.d("wcc", request.origin.toString())
//                    if (request.origin.toString() == HOST_URL) {
//                        Log.d("wcc", "GRANTED")
//                        request.grant(request.resources)
//                    } else {
//                        Log.d("wcc", "DENIED")
//                        request.deny()
//                    }
//                }
//            }
//        }
//
//        mWebSettings = mWebView!!.settings //세부 세팅 등록
//        mWebSettings!!.javaScriptEnabled = true // 웹페이지 자바스크립트 허용 여부
//        mWebSettings!!.javaScriptCanOpenWindowsAutomatically = true // 자바스크립트 새창 띄우기(멀티뷰) 허용 여부
//        mWebSettings!!.mixedContentMode = MIXED_CONTENT_ALWAYS_ALLOW
////        mWebSettings!!.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK // 브라우저 캐시 허용 여부
//
//        mWebSettings!!.loadWithOverviewMode = true // 메타태그 허용 여부
//        mWebSettings!!.builtInZoomControls = false // 화면 확대 축소 허용 여부
//        mWebSettings!!.allowContentAccess = true
//        mWebSettings!!.domStorageEnabled = true // 로컬저장소 허용 여부
//        mWebSettings!!.allowFileAccess = true
//        mWebSettings!!.loadsImagesAutomatically = true
//        mWebSettings!!.mediaPlaybackRequiresUserGesture = false
//
//        checkVerify()
//        requestPermissions(
//            arrayOf(Manifest.permission.CAMERA),
//            CAMERA_PERMISSIONS_REQUEST_CODE
//        );
 */
    }

    override fun onApplyThemeResource(theme: Resources.Theme?, resid: Int, first: Boolean) {
        super.onApplyThemeResource(theme, resid, first)
    }
    override fun onBackPressed() {
//        if (mWebView?.canGoBack() == true) {
//            mWebView?.goBack()
//        }
    }

    private fun checkVerify() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //카메라 또는 저장공간 권한 획득 여부 확인
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            ) {
                Toast.makeText(
                    applicationContext,
                    "권한 관련 요청을 허용해 주셔야 카메라 캡처이미지 사용등의 서비스를 이용가능합니다.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.INTERNET,
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ), 1
                )
            }
        }
    }
    //권한 획득 여부에 따른 결과 반환

    /*
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //Log.d("onRequestPermissionsResult() : ","들어옴");
        if (requestCode == CAMERA_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty()) {
                for (i in grantResults.indices) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        // 카메라, 저장소 중 하나라도 거부한다면 앱실행 불가 메세지 띄움
                        AlertDialog.Builder(this).setTitle("알림")
                            .setMessage("권한을 허용해주셔야 앱을 이용할 수 있습니다.")
                            .setPositiveButton(
                                "종료"
                            ) { dialog, which ->
                                dialog.dismiss()
                                finish()
                            }.setNegativeButton(
                                "권한 설정"
                            ) { dialog, which ->
                                dialog.dismiss()
                                val intent: Intent =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.parse("package:" + applicationContext.packageName))
                                applicationContext.startActivity(intent)
                            }.setCancelable(false).show()
                        return
                    }
                }
                Toast.makeText(this, "Succeed Read/Write external storage !", Toast.LENGTH_SHORT).show()
                //mWebView!!.loadUrl("https://192.168.0.98:3000/room/25d7b090-2806-11ec-92ae-2b45786ac0b8")

                val header = HashMap<String, String>()
                header["Bypass-Tunnel-Reminder"] = "true"
                header["User-Agent"] = "true"

//                mWebView!!.loadUrl(HOST_URL, header)
            }
        }
    }
     */

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_CODE
        ) {
            val permission: WebViewPermissionDelegate =
                mSession!!.permissionDelegate as WebViewPermissionDelegate
            permission.onRequestPermissionsResult(permissions, grantResults)
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler,
            error: SslError?
        ) {
            val builder: AlertDialog.Builder = AlertDialog.Builder(mContext)
            builder.setMessage("이 사이트의 보안 인증서는 신뢰하는 보안 인증서가 아닙니다. 계속하시겠습니까?")
            builder.setPositiveButton("계속하기",
                DialogInterface.OnClickListener { dialog, which -> handler.proceed() })
            builder.setNegativeButton("취소",
                DialogInterface.OnClickListener { dialog, which -> handler.cancel() })
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            mAddressText?.text = mWebView!!.url
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            mAddressText?.text = mWebView!!.url
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
            val header = HashMap<String, String>()
            header["Bypass-Tunnel-Reminder"] = "true"
            header["User-Agent"] = "true"
            view!!.loadUrl(url, header)

            Log.d("WebView", "호출 URL : $url")
            if (URLUtil.isNetworkUrl(url)) {
                Log.d("WebView", "호출 url false")
                return false
            }
            return true
        }
    }
}