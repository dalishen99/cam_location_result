package com.china.fragment

import android.content.Intent
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.webkit.WebSettings.LayoutAlgorithm
import com.china.R
import com.china.activity.WebviewActivity
import com.china.common.CONST
import kotlinx.android.synthetic.main.fragment_webview.*

/**
 * 天气资讯详情，底部带有分享、收藏等功能
 */
class WebviewFragment : Fragment() {

	private var dataUrl : String? = null

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		return inflater.inflate(R.layout.fragment_webview, null)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		initRefreshLayout()
		initWebView()
	}

	/**
	 * 初始化下拉刷新布局
	 */
	private fun initRefreshLayout() {
        refreshLayout.setColorSchemeResources(CONST.color1, CONST.color2, CONST.color3, CONST.color4)
        refreshLayout.setProgressViewEndTarget(true, 300)
		refreshLayout.isRefreshing = true
		refreshLayout.setOnRefreshListener {
			loadUrl()
		}
	}
	
	/**
	 * 初始化webview
	 */
	private fun initWebView() {
		dataUrl = arguments!!.getString(CONST.WEB_URL)
		if (TextUtils.isEmpty(dataUrl)) {
			return
		}

		val webSettings = webView.settings
		//支持javascript
		webSettings.javaScriptEnabled = true
		webSettings.javaScriptCanOpenWindowsAutomatically = true
		webSettings.domStorageEnabled = true
		webSettings.setGeolocationEnabled(true)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
		}
		// 设置可以支持缩放
		webSettings.setSupportZoom(true)
		// 设置出现缩放工具
		webSettings.builtInZoomControls = true
		webSettings.displayZoomControls = false
		//扩大比例的缩放
		webSettings.useWideViewPort = true
		//自适应屏幕
		webSettings.layoutAlgorithm = LayoutAlgorithm.SINGLE_COLUMN
		webSettings.loadWithOverviewMode = true
		loadUrl()

		webView.webChromeClient = object : WebChromeClient() {
			override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
				callback!!.invoke(origin, true, false)
			}
		}

		webView.webViewClient = object : WebViewClient() {
			override fun shouldOverrideUrlLoading(view: WebView?, itemUrl: String?): Boolean {
				dataUrl = itemUrl
				val intent = Intent(activity, WebviewActivity::class.java)
				intent.putExtra(CONST.ACTIVITY_NAME, "")
				intent.putExtra(CONST.WEB_URL, dataUrl)
				startActivity(intent)
				return true
			}

			override fun onPageFinished(view: WebView?, url: String?) {
				super.onPageFinished(view, url)
				refreshLayout.isRefreshing = false
				clBottom.visibility = View.GONE
			}

			override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
				super.onReceivedSslError(view, handler, error)
				handler!!.proceed()
			}
		}
	}

	private fun loadUrl() {
		if (webView != null && !TextUtils.isEmpty(dataUrl)) {
			webView.loadUrl(dataUrl)
		}
	}

}
