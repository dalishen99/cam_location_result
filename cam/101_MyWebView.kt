package com.app.androidinitialproject.ui.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.androidinitialproject.R
import com.app.androidinitialproject.databinding.FragmentWebviewBinding
import com.app.androidinitialproject.ui.home.base.HomeBaseFragment

class MyWebView : HomeBaseFragment<MyWebviewModel, FragmentWebviewBinding>() {
    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101
    private val MY_PERMISSIONS_REQUEST1 = 102
    private val MY_PERMISSIONS_REQUEST2 = 103

    companion object {
        fun newInstance() = MyWebView()
    }

    override fun getToolbarMenuHandler(): ToolbarMenuHandler? = null
    override fun getLayoutResource(): Int = R.layout.fragment_webview
    override fun getViewModelClass(): Class<MyWebviewModel> = MyWebviewModel::class.java

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /*viewBinding.webView.webViewClient = MyWebViewClient(activity!!)
        viewBinding.webView.settings.javaScriptEnabled = true*/
        //       AppManager.deviceID
        viewBinding.webView.settings.javaScriptEnabled = true
        viewBinding.webView.settings.javaScriptCanOpenWindowsAutomatically = true
        viewBinding.webView.webViewClient = WebViewClient()
        viewBinding.webView.settings.saveFormData = true
        viewBinding.webView.settings.setSupportZoom(false)
        viewBinding.webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        viewBinding.webView.settings.pluginState = WebSettings.PluginState.ON
        val settings: WebSettings = viewBinding.webView.settings
        settings.userAgentString =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.96 Safari/537.36"
        viewBinding.webView.settings.mediaPlaybackRequiresUserGesture = false
        viewBinding.webView.setWebChromeClient(object : WebChromeClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onPermissionRequest(request: PermissionRequest) {
               /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    request.grant(request.resources);
                }*/
              //  myRequest = request
                myRequest = request
                /*request?.resources?.forEach {
                    when(it){
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE-> {
                            askForWebkitPermission(it, Manifest.permission.RECORD_AUDIO, MY_PERMISSIONS_REQUEST1)
                        }
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE->{
                            askForWebkitPermission(it, Manifest.permission.CAMERA, MY_PERMISSIONS_REQUEST2)
                        }
                    }
                }*/

                for (permission in request.resources) {
                    when (permission) {
                        "android.webkit.resource.AUDIO_CAPTURE" -> {
                            askForPermission(
                                request.origin.toString(),
                                Manifest.permission.RECORD_AUDIO,
                                MY_PERMISSIONS_REQUEST_RECORD_AUDIO
                            )
                        }
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                            askForPermission(
                                request.origin.toString(),
                                Manifest.permission.CAMERA,
                                MY_PERMISSIONS_REQUEST1
                            )
                        }
                    }
                }
            }
        })

        viewBinding.webView.loadUrl("https://demo.bigbluebutton.org/gl/dot-2vk-avm")
    }

  /*  private fun askForWebkitPermission(webkitPermission: String, androidPermission: String, requestCode: Int){
        val context = activity?: return
        if (activity.hasPermission(androidPermission)){
            webkitPermissionRequest!!.grant(arrayOf(webkitPermission))
        }else{
            requestPermissions(arrayOf(androidPermission), requestCode)
        }
    }*/

    class MyWebViewClient internal constructor(private val activity: Activity) : WebViewClient() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url: String = request?.url.toString();
            view?.loadUrl(url)
            return true
        }

        override fun shouldOverrideUrlLoading(webView: WebView, url: String): Boolean {
            webView.loadUrl(url)
            return true
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError
        ) {
            Toast.makeText(activity, "Got Error! $error", Toast.LENGTH_SHORT).show()
        }
    }

    private var myRequest: PermissionRequest? = null

    fun askForPermission(
        origin: String,
        permission: String,
        requestCode: Int
    ) {
        Log.d("WebView", "inside askForPermission for" + origin + "with" + permission)
        if (ContextCompat.checkSelfPermission(
                activity?.applicationContext!!,
                permission
            )
            != PackageManager.PERMISSION_GRANTED
        ) { // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity!!,
                    permission
                )
            ) { // Show an expanation to the user *asynchronously* -- don't block
// this thread waiting for the user's response! After the user
// sees the explanation, try again to request the permission.
            } else { // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    activity!!, arrayOf(permission),
                    requestCode
                )
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                myRequest?.grant(myRequest?.resources)
            }
        }
    }

}