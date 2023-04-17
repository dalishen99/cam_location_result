package com.hbl.amc.ui.VideoCall

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.hbl.amc.databinding.FragmentVideoCallBinding
import com.hbl.amc.ui.CustomerInformation.ProfessionalInformationFragment


class VideoCallWebviewFragment : Fragment() {

    private var _binding: FragmentVideoCallBinding? = null
    private val binding get() = _binding!!

    var url: String? = null
    var redirecturl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event
                // block backward navigation from this section
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentVideoCallBinding.inflate(inflater, container, false)

        initView()

        return binding.root
    }

    fun initView() {

        url = arguments?.getString("Video_call_url") ?: null
        redirecturl = arguments?.getString("Redirect_url") ?: null

        binding.closeBtn.setOnClickListener {
            val intent = Intent(requireContext(), ProfessionalInformationFragment::class.java)
            startActivity(intent)
            requireActivity().finish()
        }

        initWebview()
    }

    fun initWebview() {
        binding.webview.webViewClient = Browser_Home()
//        binding.webview.webChromeClient = ChromeClient()
        binding.webview.settings.javaScriptEnabled = true
        binding.webview.settings.pluginState = WebSettings.PluginState.ON
        binding.webview.settings.mediaPlaybackRequiresUserGesture = false

        binding.webview.webChromeClient = object : WebChromeClient() {
            // Need to accept permissions to use the camera
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }
        val webSettings: WebSettings = binding.webview.settings
        webSettings.allowFileAccess = true
        webSettings.setAppCacheEnabled(true)

        loadWebSite()
    }

    private fun loadWebSite() {
        url?.let { binding.webview.loadUrl(it) }
    }

    private class Browser_Home : WebViewClient() {
        val parent = VideoCallWebviewFragment()
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            if (url == parent.redirecturl) {
//                parent.requireActivity().finish()
                val intent = Intent(parent.requireContext(), ProfessionalInformationFragment::class.java)
                parent.startActivity(intent)
                parent.requireActivity().finish()
            }
            super.doUpdateVisitedHistory(view, url, isReload)
        }

    }

    private class ChromeClient() : WebChromeClient() {
        private var mCustomView: View? = null
        private var mCustomViewCallback: CustomViewCallback? = null
        protected var mFullscreenContainer: FrameLayout? = null
        private var mOriginalOrientation = 0
        private var mOriginalSystemUiVisibility = 0
        val parent = VideoCallWebviewFragment()
        override fun getDefaultVideoPoster(): Bitmap? {
            return if (mCustomView == null) {
                null
            } else BitmapFactory.decodeResource(Application().resources, 2130837573)
        }

        override fun onHideCustomView() {

            (parent.requireActivity().window.decorView as FrameLayout).removeView(mCustomView)
            mCustomView = null
            parent.requireActivity().window.decorView.systemUiVisibility =
                mOriginalSystemUiVisibility
            parent.requireActivity().requestedOrientation = mOriginalOrientation
            mCustomViewCallback!!.onCustomViewHidden()
            mCustomViewCallback = null
        }

        override fun onShowCustomView(
            paramView: View,
            paramCustomViewCallback: CustomViewCallback
        ) {
            if (mCustomView != null) {
                onHideCustomView()
                return
            }
            mCustomView = paramView
            mOriginalSystemUiVisibility =
                parent.requireActivity().window.decorView.systemUiVisibility
            mOriginalOrientation = parent.requireActivity().requestedOrientation
            mCustomViewCallback = paramCustomViewCallback
            (parent.requireActivity().window.decorView as FrameLayout).addView(
                mCustomView,
                FrameLayout.LayoutParams(-1, -1)
            )
            parent.requireActivity().window.decorView.systemUiVisibility = 3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }
}