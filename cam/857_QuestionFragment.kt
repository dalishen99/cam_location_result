package com.daomingedu.talentgame.mvp.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import com.daomingedu.onecp.app.Constant
import com.daomingedu.talentgame.R
import com.daomingedu.talentgame.app.Preference
import com.daomingedu.talentgame.di.component.DaggerQuestionComponent
import com.daomingedu.talentgame.di.module.QuestionModule
import com.daomingedu.talentgame.mvp.contract.QuestionContract
import com.daomingedu.talentgame.mvp.model.api.Api
import com.daomingedu.talentgame.mvp.presenter.QuestionPresenter
import com.daomingedu.talentgame.mvp.ui.LoginActivity
import com.daomingedu.talentgame.mvp.ui.MainActivity
import com.daomingedu.talentgame.mvp.ui.wigets.VideoEnabledWebChromeClient
import com.jess.arms.base.BaseFragment
import com.jess.arms.di.component.AppComponent
import com.jess.arms.integration.AppManager
import com.jess.arms.utils.ArmsUtils
import kotlinx.android.synthetic.main.fragment_question.*
import timber.log.Timber


/**
 * Description
 * Created by jianhongxu on 2021/10/15
 */
class QuestionFragment : BaseFragment<QuestionPresenter>(), QuestionContract.View {

    lateinit var webChromeClient: VideoEnabledWebChromeClient

    val mSessionId by Preference(Constant.SESSIONID, "")

    companion object {
        const val TAG = "QuestionFragment"
        fun newInstance(): QuestionFragment {
            return QuestionFragment()
        }
    }

    override fun setupFragmentComponent(appComponent: AppComponent) {
        DaggerQuestionComponent.builder().appComponent(appComponent)
            .questionModule(QuestionModule((this))).build()
            .inject(this)
    }

    override fun initView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_question, container, false)
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    override fun initData(savedInstanceState: Bundle?) {
        webChromeClient =
            object : VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, null, wv_web) {

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    Timber.d("newProgress:$newProgress")
                    pb_load.progress = newProgress
                }

                override fun onPermissionRequest(request: PermissionRequest) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        request.grant(request.resources)
                    }
                }

            }

        webChromeClient.setOnToggledFullscreen { fullscreen ->
            // Your code to handle the full-screen change, for example showing and hiding the title bar. Example:
            if (fullscreen) {
                if (activity!!.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    activity!!.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                val attrs: WindowManager.LayoutParams = activity!!.window.attributes
                attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
                attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                activity!!.window.attributes = attrs
                activity!!.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE

                //                    toolbar.setVisibility(View.GONE);
            } else {
                if (activity!!.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    activity!!.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                val attrs: WindowManager.LayoutParams = activity!!.window.attributes
                attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
                attrs.flags = attrs.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
                activity!!.window.attributes = attrs
                activity!!.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE

                //                    toolbar.setVisibility(View.VISIBLE);
            }
        }

        wv_web.webChromeClient = webChromeClient

        val setting = wv_web.settings
        setting.useWideViewPort = true //将图片调整至适合Webview大小
        setting.setSupportZoom(true) //支持缩放
        setting.loadWithOverviewMode = true //缩放至屏幕大小
        setting.loadsImagesAutomatically = true //支持自动加载图片
        setting.mediaPlaybackRequiresUserGesture =false

//        setting.javaScriptCanOpenWindowsAutomatically = true
//        setting.domStorageEnabled = true
//
//        setting.blockNetworkImage = true
//        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.LOLLIPOP){
//            setting.mixedContentMode=WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
//        }

        // 加载JS
        setting.javaScriptEnabled = true //支持js

        //WebView加载web资源
        wv_web.loadUrl(Api.APP_DOMAIN + "/questionBank/index.do?sessionId=" + mSessionId)

        //覆盖WebView默认使用第三方或系统默认浏览器打开网页的行为，使网页用WebView打开
        wv_web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                Timber.e(request.toString())
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                pb_load.visibility = View.GONE
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                val cookieManager = CookieManager.getInstance()
                val s = cookieManager.getCookie(url)
                Timber.d("Cookies==$s")
                pb_load.visibility = View.VISIBLE
            }
        }

        //加载js 的 window._VideoEnabledWebView.refreshStudentInfo(); 调用Android
        wv_web.addJavascriptInterface(this, "_VideoEnabledWebView")

    }


    //增加h5调用原生方法“h5LoginOut”。用于h5跳转到原生登录界面
    @JavascriptInterface
    fun h5LogOut() {
//        Timber.e("超时登陆超时登陆超时登陆超时登陆超时登陆超时登陆超时登陆超时登陆")
        val intent = Intent(activity!!, LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
        intent.putExtra(LoginActivity.JUMP_MAIN, true)
        activity!!.startActivity(intent)
        AppManager.getAppManager().killAll()
    }

    //增加h5调用原生方法“h5QuitOut”。用于h5跳转到原生首页。
    @JavascriptInterface
    fun h5QuitOut() {
        (activity!! as MainActivity).onTabSelected(HomeFragment.TAG)
    }


    override fun setData(data: Any?) {

    }

    override fun showMessage(message: String) {
        ArmsUtils.snackbarText(message)
    }
}