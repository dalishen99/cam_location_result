package com.rc.voip

import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.*
import android.telephony.TelephonyManager
import android.util.Log
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.objects.Update
import com.rc.voip.databinding.ActivityVoipBinding
import com.rc.voip.receiver.DownloadReceiver
import com.rc.voip.receiver.PermissionCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PermissionDelegate
import java.io.File


class VoipActivity : AppCompatActivity(),ServiceConnection {
    companion object {
        const val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1
       // const val URL = "https://jc400-audio.web.app/?token=af5db706-43d2-405a-9f13-965b86a6f39c"
    }
    private var url :String ?=null

    private lateinit var binding: ActivityVoipBinding
    private var myRequest: PermissionRequest? = null
    private var webView: WebView? = null
    private var session: GeckoSession? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoipBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.xVersion.text = BuildConfig.VERSION_NAME
        PermissionCheck.readPhoneState(this)
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            url = "https://e32proxier8000.theletstream.com/websocket/?imei=${telephonyManager.imei}"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setGecko()
        binding.include.xReload.setOnClickListener {
            session?.loadUri(url.toString())
        }
        binding.include.xBtnOpen.setOnClickListener {
           // openInBrowser()
        }

        startAutoUpdate()
    }

    private fun setGecko() {
        val permission = ExamplePermissionDelegate(this)

        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        am.isSpeakerphoneOn = true
        val view = binding.include.webView
        session = GeckoSession()

        val mWebSetting = GeckoRuntime.getDefault(this)
        mWebSetting.settings.javaScriptEnabled = true

        session?.permissionDelegate = object : PermissionDelegate {
            override fun onAndroidPermissionsRequest(
                session: GeckoSession,
                permissions: Array<out String>?,
                callback: PermissionDelegate.Callback
            ) {
                super.onAndroidPermissionsRequest(session, permissions, callback)

            }
        }

        session?.permissionDelegate = permission
        session?.open(mWebSetting)
        view.setSession(session!!)
        session?.loadUri(url.toString())
    }


    private fun startAutoUpdate() {
        val listener = object : AppUpdaterUtils.UpdateListener {
            override fun onSuccess(update: Update?, isUpdateAvailable: Boolean?) {
                Log.d("Latest Version", update?.latestVersion.toString())
                Log.d("Latest Version Code", update?.latestVersionCode.toString())
                Log.d("Release notes", update?.releaseNotes.toString())

                if (update?.latestVersionCode?.toInt()!! > BuildConfig.VERSION_CODE){
                    val downloadUrl =
                        update?.urlToDownload.toString() + "/download/${update.latestVersion}/app-release.apk"
                    var path = Environment.getExternalStorageDirectory().absolutePath;
                    var folder = File("$path/Download/jcupdate/")
                    Log.d("dpath", folder.absolutePath)

                    deleteRecursive(folder)
                    if (PermissionCheck.readAndWriteExternalStorage(this@VoipActivity)) {

                    }
                    Utils.startDownload(
                        downloadUrl,
                        "/jcupdate/",
                        applicationContext,
                        "app-release.apk"
                    )
                }

            }

            override fun onFailed(error: AppUpdaterError?) {
                Log.d("TAG", "onFailed: ")
            }
        }

        val appUpdater = AppUpdaterUtils(this)
            // .setDisplay(Display.DIALOG)
            .withListener(listener)
            .setUpdateFrom(UpdateFrom.GITHUB)
            .setGitHubUserAndRepo("sidrxd", "Voip-Android")
            .setUpdateFrom(UpdateFrom.JSON)
            .setUpdateJSON("https://raw.githubusercontent.com/sidrxd/Voip-Android/master/app/update_changelog.json")
        appUpdater.start()

        registerReceiver(
            DownloadReceiver(),
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        );


    }

    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) for (child in fileOrDirectory.listFiles()) deleteRecursive(
            child
        )
        fileOrDirectory.delete()
    }

    private fun setWebView() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        am.isSpeakerphoneOn = true
        webView = WebView(this)
        WebView.setWebContentsDebuggingEnabled(true)

        webView?.settings?.javaScriptEnabled = true

        if (webView?.settings?.javaScriptEnabled == true) {
            Log.e("TAG", "setWebView: javascript enabled")
        } else {
            Log.e(
                "TAG", "setWebView: javascript false" +
                        ""
            )

        }
        webView?.settings?.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36"
        webView?.settings?.javaScriptCanOpenWindowsAutomatically = true
        webView?.settings?.mediaPlaybackRequiresUserGesture = false;
        webView?.settings?.domStorageEnabled = true
        webView?.webViewClient = WebViewClient()
        webView?.settings?.saveFormData = true
        webView?.settings?.setSupportZoom(true)
        webView?.settings?.cacheMode = WebSettings.LOAD_NO_CACHE
        webView?.settings?.pluginState = WebSettings.PluginState.ON
        webView?.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                myRequest = request
                for (permission in request.resources) {
                    when (permission) {
                        "android.webkit.resource.AUDIO_CAPTURE" -> {
                            askForPermission(
                                request.origin.toString(),
                                android.Manifest.permission.RECORD_AUDIO,
                                MY_PERMISSIONS_REQUEST_RECORD_AUDIO
                            )
                        }
                    }
                }
            }
        }
        Log.i("WebViewActivity", "UA: " + webView?.getSettings()?.getUserAgentString());

        webView?.loadUrl(url.toString())

        //webView.loadUrl("https://jc400-audio.web.app?token=af5db706-43d2-405a-9f13-965b86a6f39c")
    }

    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                Log.d("WebView", "PERMISSION FOR AUDIO")
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {


                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    myRequest?.grant(myRequest?.resources)
                    webView?.loadUrl(url.toString())
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }


    fun askForPermission(origin: String, permission: String, requestCode: Int) {
        Log.d("WebView", "inside askForPermission for" + origin + "with" + permission)
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                permission
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@VoipActivity,
                    permission
                )
            ) {
            } else {

                ActivityCompat.requestPermissions(
                    this@VoipActivity, arrayOf(permission),
                    requestCode
                )
            }
        } else {
            myRequest?.grant(myRequest?.resources)
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        webView?.destroy()
    }

    private var messenger :Messenger ?=null
    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        messenger = Messenger(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {


    }


}