package com.matrimonymandaps.vendor.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.BindingAdapter
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.matrimonymandaps.vendor.data.model.request.LogoutRequest
import com.matrimonymandaps.vendor.data.model.response.LogoutResponse
import com.matrimonymandaps.vendor.domain.SendLogoutRequest
import com.matrimonymandaps.vendor.helper.*
import com.matrimonymandaps.vendor.ui.view.MainNavigator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class MainViewModel @ViewModelInject constructor(@ApplicationContext context: Context, val networkHelper: NetworkHelper, val sendLogoutRequest: SendLogoutRequest) : BaseViewModel() {

    var progressVisible = MutableLiveData<Boolean>()
    val webViewUrl = MutableLiveData<String>()
    val swipeRefresh = MutableLiveData<Boolean>()
    val goBack = MutableLiveData<Boolean>()
    val canGoBack = MutableLiveData<Boolean>()
    val reload = MutableLiveData<Boolean>()
    var context: Context = context
    var deviceId: String? = ""
    var loggedUserPhone: String? = ""
    var loggedUserKey: String? = ""
    var loggedUserId: String? = ""
    var fcmToken: String? = ""
    var manufacturer: String? = ""

    object mainNavigator {
        var mainNavigator: MainNavigator? = null
    }

    /**
     * LoginREsponse
     */
    val LogoutResponse: MutableLiveData<ViewState<LogoutResponse>> by lazy {
        MutableLiveData<ViewState<LogoutResponse>>()
    }





    /**
     * Refresh the page
     */
    fun onrefreshListener() {
        Looper.myLooper()?.let {
            Handler(it).postDelayed({
                if (networkHelper.isNetworkConnected()) {
                    reload.postValue(true)
                    webViewUrl.postValue(mainNavigator.mainNavigator!!.webUrl())
                } else {
                    reload.postValue(true)
                   // webViewUrl.postValue(mainNavigator.mainNavigator!!.webUrl())
                }
                swipeRefresh.value = false
            }, 1000)
        }
    }


    /**
     * webclient calling
     */
    private class Client : WebViewClient() {
        override fun onReceivedError(view: WebView, request: WebResourceRequest,
                                     error: WebResourceError) {
            super.onReceivedError(view, request, error)
            mainNavigator.mainNavigator!!.visibleStatus(false)
            if (!mainNavigator.mainNavigator!!.checkNetworkConnection()) {
                view.loadUrl("file:///android_asset/error_no_internet_page.html")
                mainNavigator.mainNavigator!!.reload("")
            }
            // setHideProgress(true)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            // progressVisible.value = true
            showLog("started", "started")
            mainNavigator.mainNavigator!!.visibleStatus(true)
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            mainNavigator.mainNavigator!!.visibleStatus(false)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
            showLog("started", "loading")
            if (!mainNavigator.mainNavigator!!.checkNetworkConnection()) {
                mainNavigator.mainNavigator!!.reload("")
                return true
            }
            if (URLUtil.isNetworkUrl(url)) {
                return false
            }

            mainNavigator.mainNavigator!!.visibleStatus(true)
            // Otherwise allow the OS to handle it
            mainNavigator.mainNavigator!!.reload(url)
            return true

            /* view.loadUrl(url!!)
             return true*/
        }
    }

    /**
     * Get webclient
     */
    fun getWebViewClient(): WebViewClient? {
        return Client()
    }

    /**
     * Set websetting
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    fun getWebViewSetting(): WebSettings? {
        return ClientSettings()
    }

    /**
     * Binding the web setting for webview
     */

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private class ClientSettings : WebSettings() {
        override fun setDefaultFontSize(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun setStandardFontFamily(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun getBlockNetworkLoads(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getDefaultFontSize(): Int {
            TODO("Not yet implemented")
        }

        override fun getBuiltInZoomControls(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getSavePassword(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setAppCacheMaxSize(p0: Long) {
            TODO("Not yet implemented")
        }

        override fun setDomStorageEnabled(p0: Boolean) {

        }

        override fun setSerifFontFamily(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun setPluginState(p0: PluginState?) {
            TODO("Not yet implemented")
        }

        override fun setRenderPriority(p0: RenderPriority?) {
            TODO("Not yet implemented")
        }

        override fun getLoadsImagesAutomatically(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getSerifFontFamily(): String {
            TODO("Not yet implemented")
        }

        override fun getDisabledActionModeMenuItems(): Int {
            TODO("Not yet implemented")
        }

        override fun setDisabledActionModeMenuItems(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun getDatabasePath(): String {
            TODO("Not yet implemented")
        }

        override fun setFixedFontFamily(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun setSansSerifFontFamily(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun setLightTouchEnabled(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setSupportMultipleWindows(p0: Boolean) {

        }

        override fun setMinimumFontSize(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun setMediaPlaybackRequiresUserGesture(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun getStandardFontFamily(): String {
            TODO("Not yet implemented")
        }

        override fun setGeolocationDatabasePath(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun getFixedFontFamily(): String {
            TODO("Not yet implemented")
        }

        override fun setOffscreenPreRaster(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setBlockNetworkLoads(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun getDomStorageEnabled(): Boolean {
            TODO("Not yet implemented")
        }

        override fun supportZoom(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setDefaultTextEncodingName(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun getJavaScriptEnabled(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getCursiveFontFamily(): String {
            TODO("Not yet implemented")
        }

        override fun setSaveFormData(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun getBlockNetworkImage(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setSupportZoom(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setSavePassword(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun getAllowFileAccess(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setAllowUniversalAccessFromFileURLs(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setJavaScriptCanOpenWindowsAutomatically(p0: Boolean) {

        }

        override fun getDefaultTextEncodingName(): String {
            TODO("Not yet implemented")
        }

        override fun getDefaultFixedFontSize(): Int {
            TODO("Not yet implemented")
        }

        override fun setDatabasePath(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun enableSmoothTransition(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setAllowFileAccess(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setCacheMode(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun setDefaultZoom(p0: ZoomDensity?) {
            TODO("Not yet implemented")
        }

        override fun getMinimumFontSize(): Int {
            TODO("Not yet implemented")
        }

        override fun getAllowFileAccessFromFileURLs(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setDatabaseEnabled(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setJavaScriptEnabled(p0: Boolean) {

        }

        override fun setLoadsImagesAutomatically(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setEnableSmoothTransition(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setAppCachePath(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun getDatabaseEnabled(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getSansSerifFontFamily(): String {
            TODO("Not yet implemented")
        }

        override fun setLoadWithOverviewMode(p0: Boolean) {

        }

        override fun getAllowContentAccess(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setSafeBrowsingEnabled(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun getPluginState(): PluginState {
            TODO("Not yet implemented")
        }

        override fun setBuiltInZoomControls(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun getDefaultZoom(): ZoomDensity {
            TODO("Not yet implemented")
        }

        override fun getMinimumLogicalFontSize(): Int {
            TODO("Not yet implemented")
        }

        override fun setDisplayZoomControls(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun getCacheMode(): Int {
            TODO("Not yet implemented")
        }

        override fun getUserAgentString(): String {
            TODO("Not yet implemented")
        }

        override fun setUserAgentString(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun getUseWideViewPort(): Boolean {
            TODO("Not yet implemented")
        }

        override fun supportMultipleWindows(): Boolean {
            return true
        }

        override fun setUseWideViewPort(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setTextZoom(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun setFantasyFontFamily(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun setBlockNetworkImage(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun getLayoutAlgorithm(): LayoutAlgorithm {
            TODO("Not yet implemented")
        }

        override fun setCursiveFontFamily(p0: String?) {
            TODO("Not yet implemented")
        }

        override fun getSaveFormData(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setNeedInitialFocus(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setMixedContentMode(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun setLayoutAlgorithm(p0: LayoutAlgorithm?) {
            TODO("Not yet implemented")
        }

        override fun setGeolocationEnabled(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun getMediaPlaybackRequiresUserGesture(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setAppCacheEnabled(p0: Boolean) {

        }

        override fun getLoadWithOverviewMode(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getLightTouchEnabled(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getDisplayZoomControls(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setAllowFileAccessFromFileURLs(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun getTextZoom(): Int {
            TODO("Not yet implemented")
        }

        override fun getOffscreenPreRaster(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getAllowUniversalAccessFromFileURLs(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getSafeBrowsingEnabled(): Boolean {
            TODO("Not yet implemented")
        }

        override fun getMixedContentMode(): Int {
            TODO("Not yet implemented")
        }

        override fun getFantasyFontFamily(): String {
            TODO("Not yet implemented")
        }

        override fun setAllowContentAccess(p0: Boolean) {
            TODO("Not yet implemented")
        }

        override fun setDefaultFixedFontSize(p0: Int) {
            TODO("Not yet implemented")
        }

        override fun getJavaScriptCanOpenWindowsAutomatically(): Boolean {
            TODO("Not yet implemented")
        }

        override fun setMinimumLogicalFontSize(p0: Int) {
            TODO("Not yet implemented")
        }


    }


    object Utilities {
        /* @BindingAdapter("loadUrl")
         @JvmStatic
         fun WebView.setUrl(url: String) {
             this.loadUrl(url)
         }*/

        @BindingAdapter("setWebViewClient")
        @JvmStatic
        fun setWebViewClient(view: WebView, client: WebViewClient?) {
            view.addJavascriptInterface(WebAppInterface(), "AndroidInterface")
            view.addJavascriptInterface(WebServerInterface(), "LoginResponse")
            view.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY)
            view.setWebChromeClient(MyWebChromeClient())
            view.webViewClient = client!!
        }

        @SuppressLint("SetJavaScriptEnabled")
        @BindingAdapter("setWebViewSettings")
        @JvmStatic
        fun setWebViewSettings(view: WebView, ClientSettings: WebSettings) {
            view.settings
            view.settings.setJavaScriptEnabled(true);
            view.settings.setAppCacheEnabled(true)
            view.settings.setLoadWithOverviewMode(true)
            view.settings.setJavaScriptCanOpenWindowsAutomatically(true)
            view.settings.supportMultipleWindows()
            view.settings.setDomStorageEnabled(true)
        }


        class WebAppInterface {
            @JavascriptInterface
            fun reloadPageOnError() {
                // mWebView.post(Runnable { if (mWebView.canGoBack()) mWebView.goBack() else mWebView.loadUrl(BASE_URL_TO_LOAD) })
            }
        }

        class MyWebChromeClient : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress >= 90) {
                    mainNavigator.mainNavigator!!.visibleStatus(false)
                }
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                callback.invoke(origin, true, false)
            }
        }


        @BindingAdapter("loadUrl")
        @JvmStatic
        fun loadUrl(view: WebView, url: String?) {
            view.loadUrl(url!!)
        }


        @BindingAdapter("goBack")
        @JvmStatic
        fun goBack(view: WebView, visible: Boolean) {
            if (visible && view.canGoBack()) {
                view.goBack()
            }
        }

        @BindingAdapter("canGoBack")
        @JvmStatic
        fun canGoBack(view: WebView, canGoBack: Boolean): Boolean {
            if (view.canGoBack()) {
                mainNavigator.mainNavigator!!.canGoBack(true)
                return canGoBack
            }
            mainNavigator.mainNavigator!!.canGoBack(false)
            return canGoBack
        }

        @BindingAdapter("reLoad")
        @JvmStatic
        fun reLoad(view: WebView, visible: Boolean) {
            if (visible) {
                view.reload()
            }
        }


    }

    /***
     * Interface for logout
     */
    class WebServerInterface {
        @JavascriptInterface
        fun logResponse() {
            showLog("started", "logout")
            mainNavigator.mainNavigator!!.response("logout")
        }
    }


    fun showLogoutDialog() {
        //     callLogoutApi();
        if (networkHelper.isNetworkConnected()) {

        } else {
            mainNavigator.mainNavigator!!.response("Please check your internet connection")
        }
    }

    /**
     * Logout API
     */

    @ExperimentalCoroutinesApi
    fun callLogoutApi(autoFailureMessage: String?) {
        if (networkHelper.isNetworkConnected()) {
            mainNavigator.mainNavigator!!.visibleStatus(true)
            viewModelScope.launch(Dispatchers.Main) {
                if (networkHelper.isNetworkConnected()) {
                    getViewStateFlowForNetworkCall {
                        sendLogoutRequest.execute(LogoutRequest(loggedUserKey, fcmToken, deviceId))
                    }.collect {
                        when (it) {
                            is ViewState.Loading -> LogoutResponse.value = it
                            is ViewState.RenderFailure -> LogoutResponse.value = it
                            is ViewState.RenderSuccess<LogoutResponse> -> {
                                val msg = it.output.msg
                                val status = it.output.status
                                Log.v("output", it.toString())
                                if (status.equals(Constants.API_STATUS_SUCCESS)) {
                                    mainNavigator.mainNavigator!!.response(status!!)
                                }
                            }

                        }
                        mainNavigator.mainNavigator!!.visibleStatus(false)
                    }
                }
            }
        } else {
            mainNavigator.mainNavigator!!.response(autoFailureMessage!!)
        }
    }


    /**
     * Check wether boolean true or not
     */
    fun checkNetworkConnection(): Boolean {
        if (networkHelper.isNetworkConnected()) {
            return true
        }
        return false
    }


}