package com.china.activity

import android.graphics.BitmapFactory
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.webkit.*
import android.webkit.WebSettings.LayoutAlgorithm
import android.widget.Toast
import com.china.R
import com.china.common.CONST
import com.china.dto.NewsDto
import com.china.manager.MyCollectManager
import com.china.utils.CommonUtil
import kotlinx.android.synthetic.main.fragment_webview.*
import kotlinx.android.synthetic.main.layout_title.*
import java.util.*

/**
 * 天气资讯详情，底部带有分享、收藏等功能
 */
class WebviewActivity : BaseActivity(), OnClickListener {

	private var dataUrl : String? = null
	private var isCollected : Boolean = false//是否已收藏
	private val collectList : ArrayList<NewsDto> = ArrayList()//存放收藏数据的list
	private var newsDto: NewsDto? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_webview)
		initRefreshLayout()
		initWidget()
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
	 * 初始化控件
	 */
	private fun initWidget() {
		llBack.setOnClickListener(this)
		ivText.setOnClickListener(this)
		ivShareImg.setOnClickListener(this)
		ivCollect.setOnClickListener(this)

		val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
		if (!TextUtils.isEmpty(title)) {
			tvTitle.text = title
		}else {
			tvTitle.text = getString(R.string.detail)
		}

		if (intent.hasExtra(CONST.WEB_URL)) {
			dataUrl = intent.getStringExtra(CONST.WEB_URL)
			isCollect(dataUrl)
		}
	}
	
	/**
	 * 判断是否是已收藏
	 */
	private fun isCollect(url : String?) {
		val size = MyCollectManager.readCollect(this, collectList)
		if (size != 0) {
			for (i in 0 until collectList.size) {
				if (TextUtils.equals(url, collectList[i].detailUrl)) {
					ivCollect.setImageResource(R.drawable.icon_collection_blue_press)
					isCollected = true
					break
				}
			}
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
				if (view!!.title != null) {
					tvTitle.text = view.title
				}
				dataUrl = itemUrl
				loadUrl()
				return true
			}

			override fun onPageFinished(view: WebView?, url: String?) {
				super.onPageFinished(view, url)
				refreshLayout.isRefreshing = false
				if (view!!.title != null) {
					tvTitle.text = view.title
				}
				if (intent.hasExtra("data")) {
					newsDto = intent.extras.getParcelable("data")
					if (newsDto != null) {
						clBottom.visibility = View.VISIBLE
					} else {
						clBottom.visibility = View.GONE
					}
				}
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
	
	/**
	 * 刷新我的收藏界面数据
	 */
	private fun resultIntentCollect() {
		setResult(RESULT_OK)
		finish()
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (webView != null && webView.canGoBack()) {
				webView.goBack()
				return true
			}else {
				resultIntentCollect()
			}
		}
		return super.onKeyDown(keyCode, event)
	}

	override fun onClick(v: View?) {
		when(v!!.id) {
			R.id.llBack -> resultIntentCollect()
			R.id.ivText -> {
				val webSettings = webView.settings
				when (webSettings.textSize) {
					WebSettings.TextSize.NORMAL -> {
						webSettings.textSize = WebSettings.TextSize.LARGER
					}
					WebSettings.TextSize.LARGER -> {
						webSettings.textSize = WebSettings.TextSize.LARGEST
					}
					WebSettings.TextSize.LARGEST -> {
						webSettings.textSize = WebSettings.TextSize.NORMAL
					}
				}
			}
			R.id.ivCollect -> {
				if (!isCollected) {
					if (newsDto != null) {
						collectList.add(0, newsDto!!)
						ivCollect.setImageResource(R.drawable.icon_collection_blue_press)
						Toast.makeText(this, getString(R.string.collect_success), Toast.LENGTH_SHORT).show()
						isCollected = true
					}
				}else {
					for (i in 0 until collectList.size) {
						if (TextUtils.equals(dataUrl, collectList[i].detailUrl)) {
							collectList.removeAt(i)
							ivCollect.setImageResource(R.drawable.icon_collection_blue)
							Toast.makeText(this, getString(R.string.collect_cancel), Toast.LENGTH_SHORT).show()
							isCollected = false
							break
						}
					}
				}
				MyCollectManager.clearCollectData(this)
				MyCollectManager.writeCollect(this, collectList)
			}
			R.id.ivShareImg -> {
				CommonUtil.share(this, tvTitle.text.toString(), tvTitle.text.toString(), newsDto!!.imgUrl, dataUrl)
			}
		}
	}

}
