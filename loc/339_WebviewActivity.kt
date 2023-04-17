package com.warning.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings.LayoutAlgorithm
import android.webkit.WebView
import android.webkit.WebViewClient
import com.umeng.socialize.UMShareAPI
import com.warning.R
import com.warning.dto.NewsDto
import com.warning.util.CommonUtil
import kotlinx.android.synthetic.main.activity_webview.*
import kotlinx.android.synthetic.main.shawn_layout_title2.*

/**
 * 资讯详情
 */
class WebviewActivity : BaseActivity(), OnClickListener {
	
	private var data : NewsDto? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			WebView.enableSlowWholeDocumentDraw()
		}
		setContentView(R.layout.activity_webview)
		initWidget()
		initWebView()
	}
	
	/**
	 * 初始化控件
	 */
	private fun initWidget() {
		llBack.setOnClickListener(this)
		ivShare.setOnClickListener(this)

		data = intent.extras.getParcelable("data")
		if (!TextUtils.isEmpty(data!!.title)) {
			tvTitle.text = data!!.title
		}
	}
	
	/**
	 * 初始化webview
	 */
	private fun initWebView() {
		val webSettings = webView.settings
		//支持javascript
		webSettings.javaScriptEnabled = true
		webSettings.javaScriptCanOpenWindowsAutomatically = true
		webSettings.domStorageEnabled = true
		webSettings.setGeolocationEnabled(true)
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
		if (!TextUtils.isEmpty(data!!.url)) {
			webView.loadUrl(data!!.url)
		}

		webView.webChromeClient = object : WebChromeClient() {
			override fun onReceivedTitle(view: WebView?, title: String?) {
				super.onReceivedTitle(view, title)
			}

			override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
				callback!!.invoke(origin, true, false)
			}
		}

		webView.webViewClient = object : WebViewClient() {
			override fun shouldOverrideUrlLoading(view: WebView?, itemUrl: String?): Boolean {
				return if (itemUrl!!.startsWith("http:") || itemUrl.startsWith("https:")) {
					webView.loadUrl(itemUrl)
					false
				} else {
					try {
						startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(itemUrl)))
					} catch (e : ActivityNotFoundException) {
						e.printStackTrace()
					}
					true
				}
			}

			override fun onPageFinished(view: WebView?, url: String?) {
				super.onPageFinished(view, url)
				ivShare.visibility = View.VISIBLE
			}
		}
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (webView != null && webView.canGoBack()) {
				webView.goBack()
				return true
			}else {
				finish()
			}
		}
		return super.onKeyDown(keyCode, event)
	}

	override fun onClick(p0: View?) {
		when(p0!!.id) {
			R.id.llBack -> finish()
			R.id.ivShare -> CommonUtil.share(this, data!!.title, "", data!!.url)
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		//umeng分享回调
		UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data)
	}

}
