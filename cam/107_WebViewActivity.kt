package com.net.routee.videoCall

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.net.routee.R
import com.net.routee.preference.SharedPreference
import com.net.routee.services.Authenticator
import kotlinx.android.synthetic.main.activity_web_view.*
import java.util.*

class WebViewActivity : AppCompatActivity() {
    private var myRequest: PermissionRequest? = null
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            myRequest?.grant(myRequest!!.resources)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)


        val url = intent.extras?.getString("roomName")
        val videoConferenceType = intent.extras?.getString("videoConferenceType") ?: ""
        val isVerified = videoConferenceType.contains("verifyAuth")
        intent.putExtra("authTypes", "")

        positiveButton.visibility = if (isVerified) View.VISIBLE else View.INVISIBLE
        negativeButton.visibility = if (isVerified) View.VISIBLE else View.INVISIBLE
        closeButton.visibility = if (isVerified) View.INVISIBLE else View.VISIBLE
//        val progress = ProgressDialog(this)
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(R.layout.custom_progress_dialog)
        val progress = builder.create()

        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        var loadingFinished = false
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, urlString: String?) {
                loadingFinished = true
                progress.dismiss()
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (loadingFinished) {
                    Authenticator.publishResult(Authenticator.success,
                        this@WebViewActivity, intent)
                    this@WebViewActivity.finish()
                }
//                progress.setTitle("Loading")
                progress.setCancelable(false) // disable dismiss by tapping outside of the dialog

                progress.show()

                loadingFinished = false
            }
        }

//        webView.settings.saveFormData = true
        webView.settings.setSupportZoom(false)
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
//        webView.settings.pluginState = WebSettings.PluginState.ON

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                myRequest = request

                for (permission in request?.resources!!)
                    when (permission) {
                        "android.webkit.resource.AUDIO_CAPTURE" -> {
                            askForPermission(Manifest.permission.RECORD_AUDIO)
                            break
                        }
                        Manifest.permission.RECORD_AUDIO -> {
                            askForPermission(Manifest.permission.RECORD_AUDIO)
                            break
                        }
                        "android.webkit.resource.VIDEO_CAPTURE" -> {
                            askForPermission(Manifest.permission.CAMERA)
                            break
                        }
                    }
            }
        }
        if (url?.isNotEmpty() == true) {
            webView.loadUrl(url)
        }


        positiveButton.setOnClickListener {
            Authenticator.publishResult(Authenticator.success,
                this, intent)
            this.finish()
        }
        closeButton.setOnClickListener {
            Authenticator.publishResult(Authenticator.failed,
                this, intent)
            this.finish()
        }
        negativeButton.setOnClickListener {
            Authenticator.publishResult(Authenticator.failed,
                this, intent)
            this.finish()
        }

        val filter = IntentFilter()
        filter.addAction("videoConferenceStatus")
        registerReceiver(videoConferenceStatusReceiver, filter)

        val preference = SharedPreference(this)
        preference.storeStatusUpdate(null, null)
    }

    /**
     * This function is used to request permission from the user
     **/
    private fun askForPermission(permission: String) {

        requestPermission.launch(permission)
    }

    /**
     * This function will handle the authentication when the application comes from background
     **/
    override fun onResume() {
        super.onResume()
        val preference = SharedPreference(this)
        val pair = preference.getStoreStatusUpdate()

        if (pair.first ==
            Authenticator.success.lowercase(Locale.getDefault()) && pair.second == intent.extras?.getString(
                "actionToken")
        ) {
            Authenticator.publishResult(Authenticator.success,
                this@WebViewActivity, intent)
            this@WebViewActivity.finish()
        } else {
            if (pair.first ==
                Authenticator.failed.lowercase(Locale.getDefault()) && pair.second == intent.extras?.getString(
                    "actionToken")
            ) {
                Authenticator.publishResult(Authenticator.failed,
                    this@WebViewActivity, intent)
                this@WebViewActivity.finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(videoConferenceStatusReceiver)
        if (webView != null)
            webView.destroy()
    }


    /**
     * This receiver get the response from the server
     */
    private val videoConferenceStatusReceiver: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, receiverIntent: Intent?) {
                if (receiverIntent != null) {
                    if (receiverIntent.extras?.getString("actionToken") ==
                        intent.extras?.getString("actionToken")
                    ) {
                        if (receiverIntent.extras?.getString("status") ==
                            Authenticator.success.lowercase(Locale.getDefault())
                        ) {
                            Authenticator.publishResult(Authenticator.success,
                                this@WebViewActivity, intent)
                            this@WebViewActivity.finish()
                        } else {
                            Authenticator.publishResult(Authenticator.failed,
                                this@WebViewActivity, intent)
                            this@WebViewActivity.finish()
                        }
                    }
                }
            }
        }
}