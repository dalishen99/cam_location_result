package com.vahan.mitra_playstore.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.webkit.*
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.blitzllama.androidSDK.BlitzLlamaSDK
import com.freshchat.consumer.sdk.Freshchat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.moe.pushlibrary.MoEHelper
import com.moengage.core.Properties
import com.uxcam.UXCam
import com.vahan.mitra_playstore.BuildConfig
import com.vahan.mitra_playstore.R
import com.vahan.mitra_playstore.databinding.ActivityHomeBinding
import com.vahan.mitra_playstore.models.ChromeModel
import com.vahan.mitra_playstore.models.UserResponse
import com.vahan.mitra_playstore.models.WebViewModifyModel
import com.vahan.mitra_playstore.network.SharedViewModel
import com.vahan.mitra_playstore.utils.Constants
import com.vahan.mitra_playstore.utils.PrefrenceUtils
import com.vahan.mitra_playstore.view.activities.enternumberactivity.view.ui.EnterNumberActivity


class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding
    private var fa: FirebaseAnalytics? = null
    private lateinit var viewModel: SharedViewModel
    lateinit var webModelModify: WebViewModifyModel
    private var mFirebaseRemoteConfig: FirebaseRemoteConfig? = null
    private var urlhanlder: String? = null
    private var webModelChrome: ChromeModel? = null
    private var audioRequest: PermissionRequest? = null
    private val MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 101
    override fun onCreate(
        savedInstanceState: Bundle?,
    ) {
        super.onCreate(savedInstanceState)
        fa = FirebaseAnalytics.getInstance(this)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        Thread.setDefaultUncaughtExceptionHandler(
            ExceptionHandlingActivity(
                this@HomeActivity, this
            )
        )
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        initView()

    }

    private fun getUserData() {
        val viewSharedViewModel: SharedViewModel =
            ViewModelProvider(this)[SharedViewModel::class.java]
        if (PrefrenceUtils.retriveData(this, Constants.USERTYPE).equals(Constants.NEW_LEAD)) {
            viewSharedViewModel.userData.observe(this) { userResponse ->
                try {
                    if (userResponse.success == true) {
                        PrefrenceUtils.insertData(this, Constants.USERTYPE, userResponse.userType)
                        PrefrenceUtils.insertData(
                            this, Constants.REDIRECTION_URL, userResponse.redirectURL
//                            Constants.REDIRECTION_URL_STRING
                        )
                        when {
                            userResponse.userType.equals(Constants.NON_PAYROLL) -> {
                                //show popup
                                //load redirect url after user presses ok in popup
                                // New lead -------> non-payroll user (using redirect url)
                                PrefrenceUtils.insertData(
                                    this, Constants.REDIRECTION_URL, userResponse.redirectURL
                                )
                                userResponse.redirectURL?.let {
//                                    showDialogNonPayroll(it)
                                    setInstrumentationUserTranscViewedClicked(Constants.NON_PAYROLL)
                                    openWebView(userResponse.redirectURL)
                                }
                            }
                            userResponse.userType.equals(Constants.PAYROLL) -> {
                                //show popup
                                //send to mainActivity---(either new lead->payroll or non-payroll->payroll)
                                PrefrenceUtils.insertDataInBoolean(
                                    MainActivity.context, Constants.CHECKFORFIRSTTIME, true
                                )
                                PrefrenceUtils.insertDataInBoolean(
                                    this, Constants.CHECKFORPAYROLL, true
                                )
                                userResponse.redirectURL?.let { showDialogPayroll(it) }
                            }
                        }
                    }
                } catch (e: Exception) {

                }
            }
        } else if (PrefrenceUtils.retriveData(this, Constants.USERTYPE)
                .equals(Constants.NON_PAYROLL)
        ) {
            viewSharedViewModel.userData.observe(this) { userResponse ->
                try {
                    redirectToJobsMarketplace(userResponse)
                } catch (e: Exception) {

                }
            }
        }
    }

    private fun redirectToJobsMarketplace(userResponse: UserResponse) {
        if (userResponse.success == true) {
            PrefrenceUtils.insertData(this, Constants.USERTYPE, userResponse.userType)
            var redirectionUrlVal = userResponse.redirectURL
            if (!PrefrenceUtils.retriveDataInBoolean(
                    context, Constants.CHECKFORFIRSTTIMESLOTSCREEN
                )
            ) {
                if (PrefrenceUtils.retriveData(this, Constants.REDIRECTION_URL)
                        .startsWith("https")
                ) {
                    redirectionUrlVal = PrefrenceUtils.retriveData(this, Constants.REDIRECTION_URL)
                }
            }
            PrefrenceUtils.insertData(
                this, Constants.REDIRECTION_URL, redirectionUrlVal
//                            Constants.REDIRECTION_URL_STRING
            )
            when {
                userResponse.userType.equals(Constants.PAYROLL) -> {
                    //show popup
                    //send to mainActivity---(either new lead->payroll or non-payroll->payroll)
                    PrefrenceUtils.insertDataInBoolean(
                        this, Constants.CHECKFORPAYROLL, true
                    )
                    userResponse.redirectURL?.let {
                        showDialogPayroll(it)
                    }
                }
            }
        }
    }

    private fun initView() {
        getUserData()
        Log.d("HA PN", "initView: ${PrefrenceUtils.retriveData(this, Constants.PHONENUMBER)}")
        PrefrenceUtils.insertDataInBoolean(MainActivity.context, Constants.CHECKFORFIRSTTIME, false)
        webModelChrome = Gson().fromJson(
            PrefrenceUtils.retriveData(
                this, Constants.CHROME_URL_REMOTE_CONFIG
            ), ChromeModel::class.java
        )
        webModelModify = Gson().fromJson(
            PrefrenceUtils.retriveData(
                this, Constants.WEBVIEW_HANDLER_REMOTE_CONFIG_HOME_PAGE
            ), WebViewModifyModel::class.java
        )

        // If User is new_lead_referral ---> Redirection Link open in native App
        if (PrefrenceUtils.retriveData(this@HomeActivity, Constants.REDIRECTION_URL)
                .equals(Constants.NEW_LEAD_REFERRAL)
        ) {
            binding.incLayoutOne.containerOne.visibility = View.GONE
            binding.incLayoutTwo.containerOne.visibility = View.VISIBLE
        } else {
            // If User is New LEAD or Non Payroll ---> Redirection Link open in web app
            binding.incLayoutOne.containerOne.visibility = View.VISIBLE
            binding.incLayoutTwo.containerOne.visibility = View.GONE
            if (intent.hasExtra(Constants.TYPE)) {
                // NOTIFICATION CHECK
                if (intent.getStringExtra(Constants.TYPE) != "") {
                    webModelModify.webviewHandler!!.forEach {
                        if (intent.getStringExtra(Constants.TYPE)
                                .equals(Constants.REFERRAL_NON_PAYROLL)
                        ) {
                            redirectToHome(it.destination, it.url)
                        } else {
                            startActivity(Intent(this@HomeActivity, HomeActivity::class.java))
                            finish()
                            return@forEach
                        }
                    }
                } else {
                    openWebView(PrefrenceUtils.retriveData(this, Constants.REDIRECTION_URL))
                }
            } else {
                openWebView(PrefrenceUtils.retriveData(this, Constants.REDIRECTION_URL))
            }
        }

    }


    @SuppressLint("SetJavaScriptEnabled")
    private fun openWebView(url: String?) {
        Log.d(
            "Views", "openWebView: ${PrefrenceUtils.retriveData(this, Constants.REDIRECTION_URL)}"
        )
        val properties = Properties()
        properties.addAttribute(Constants.REDIRECTION_URL, url)
        properties.addAttribute(
            Constants.TYPE, PrefrenceUtils.retriveData(this, Constants.USERTYPE)
        )
        MoEHelper.getInstance(this).trackEvent(Constants.WEBVIEW_OPENED, properties)
        binding.incLayoutOne.pgBar.visibility = View.VISIBLE
        val webSettings = binding.incLayoutOne.webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.domStorageEnabled = true;
        binding.incLayoutOne.webView.isVerticalScrollBarEnabled = true
        binding.incLayoutOne.webView.isHorizontalScrollBarEnabled = true;
        webSettings.pluginState = WebSettings.PluginState.ON
        binding.incLayoutOne.webView.visibility = View.VISIBLE
        binding.incLayoutOne.webView.setDownloadListener { _, _, _, _, _ ->
            Log.d(
                "WebView", "onDownloadStart"
            )
        }
        val onlyonce = arrayOf(true)

        // make this url
        val finalUrl = url
        binding.incLayoutOne.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                try {
                    for (i in 0 until webModelChrome?.urls?.size!!) {
                        when {
                            url.startsWith("tel:") -> {
                                setInstrumentationWebView(Constants.TEL_INTENT)
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(url)
                                startActivity(intent)
                                return true

                            }
                            url.startsWith("whatsapp:") -> {
                                setInstrumentationWebView(Constants.WHATSAPP_INTENT)
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(url)
                                startActivity(intent)
                                return true
                            }
                            url.startsWith("mailto:") -> {
                                setInstrumentationWebView(Constants.MAIL_INTENT)
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = Uri.parse(url)
                                startActivity(intent)
                                return true
                            }
                            url.startsWith("https://play.google.com") -> {
                                val packageName =
                                    url.substringAfter("?id=").substringBefore("&", "")
                                val playstoreURL =
                                    "https://play.app.goo.gl/?link=https://play.google.com/store/apps/details?id=${packageName}&ddl=1&pcampaignid=web_ddl_1"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playstoreURL))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.setPackage("com.android.chrome")
                                return try {
                                    startActivity(intent)
                                    true
                                } catch (ex: ActivityNotFoundException) {
                                    // Chrome browser presumably not installed and open Kindle Browser
                                    intent.setPackage("com.amazon.cloud9")
                                    startActivity(intent)
                                    true
                                }
                            }
                            //Instrumentation need to added
                            webModelChrome?.urls!![i].chromeUrl.equals(url) -> {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.setPackage("com.android.chrome")
                                return try {
                                    startActivity(intent)
                                    true
                                } catch (ex: ActivityNotFoundException) {
                                    // Chrome browser presumably not installed and open Kindle Browser
                                    intent.setPackage("com.amazon.cloud9")
                                    startActivity(intent)
                                    true
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    fa?.logEvent("web_model_chrome_error", Bundle())
                    Toast.makeText(
                        this@HomeActivity, e.message, Toast.LENGTH_SHORT
                    ).show()
                }
                try {
                    for (i in 0 until webModelModify?.webviewHandler?.size!!) {
                        val urlValBefore = url.substringBefore("?", "")
                        var urlValAfter = ""
                        if (url.contains("applicationId")) {
                            urlValAfter = getJobSeekerID(url)
                        }
                        val completeUrl = constructRedirectionUrl(url)
//                        Constants.FE_BASE_URL + "/application-status?applicationId=$urlValAfter"
//                        Log.d("URL_FROM_PWA", url)
//                        Log.d("URL_FORMED_IN_APP", completeUrl)

                        PrefrenceUtils.insertData(
                            context, Constants.REDIRECTION_URL, completeUrl
                        )
                        PrefrenceUtils.insertData(
                            context, Constants.JOB_SEEKER_ID, urlValAfter
                        )
                        if (webModelModify?.webviewHandler?.get(i)?.url.equals(urlValBefore)) {
                            if (webModelModify?.webviewHandler?.get(i)?.destination.equals(
                                    Constants.LOGOUT
                                )
                            ) {
                                logoutFunction()
                                return true
                            } else if (webModelModify?.webviewHandler?.get(i)?.destination.equals(
                                    Constants.UPLOAD
                                ) || webModelModify?.webviewHandler?.get(i)?.destination.equals(
                                    Constants.UPLOAD_JOB_SEEKER
                                ) || webModelModify?.webviewHandler?.get(i)?.destination.equals(
                                    Constants.REFERRAL_NON_PAYROLL
                                )
                            ) {
                                redirectToHome(
                                    webModelModify?.webviewHandler?.get(i)?.destination,
                                    webModelModify?.webviewHandler?.get(i)?.url
                                )
                                PrefrenceUtils.insertData(
                                    context, Constants.JOB_SEEKER_REDIRECTION_URL, url
                                )
                                return true
                            } else if (webModelModify?.webviewHandler?.get(i)?.destination.equals(
                                    Constants.VERIFYBANK
                                )
                            ) {
                                redirectToHome(
                                    webModelModify?.webviewHandler?.get(i)?.destination,
                                    webModelModify?.webviewHandler?.get(i)?.url
                                )
                                return true
                            } else if (webModelModify?.webviewHandler?.get(i)?.destination.equals(
                                    Constants.PROFILE_PIC
                                ) || webModelModify?.webviewHandler?.get(i)?.destination.equals(
                                    Constants.BANK_DOC_UPLOAD
                                )
                            ) {
                                redirectToHome(
                                    webModelModify?.webviewHandler?.get(i)?.destination,
                                    webModelModify?.webviewHandler?.get(i)?.url
                                )
                                return true
                            }
                        }
                    }
                } catch (e: Exception) {
                    fa?.logEvent("web_model_modify_error", Bundle())
                    Toast.makeText(
                        this@HomeActivity,
                        getString(R.string.please_try_after_sometime),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return false
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError,
            ) {
                super.onReceivedError(view, request, error)
                Log.d("WebView", "onReceivedError")
                binding.incLayoutOne.pgBar.visibility = View.GONE
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse,
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (onlyonce[0]) {
                    onlyonce[0] = false
                    if (finalUrl != null) {
//                        binding.incLayoutOne.webView.loadUrl(finalUrl)
                    }
                }
            }

            override fun onPageStarted(view: WebView, url: String, favIcon: Bitmap?) {
                super.onPageStarted(view, url, favIcon)
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (view.contentHeight == 0) {
                    view.reload()
                } else {
                    binding.incLayoutOne.pgBar.visibility = View.GONE
                }
                Log.d("WebView", "onPageFinished" + view.contentHeight)
            }
        }
        if (url != null) {
            var urlToLoad = url
            if (urlToLoad.contains("additional-details")) {
                urlToLoad = if (urlToLoad.contains("?")) {
                    "$url&appVersion=${BuildConfig.VERSION_CODE}"
                } else {
                    "$url?appVersion=${BuildConfig.VERSION_CODE}"
                }
            }
            binding.incLayoutOne.webView.loadUrl(urlToLoad)
        }

        binding.incLayoutOne.webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                binding.incLayoutOne.webView.visibility = View.GONE
                binding.incLayoutOne.customViewForFullscreen.visibility = View.VISIBLE
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                binding.incLayoutOne.customViewForFullscreen.addView(view)
            }

            @SuppressLint("SourceLockedOrientationActivity")
            override fun onHideCustomView() {
                super.onHideCustomView()
                binding.incLayoutOne.webView.visibility = View.VISIBLE
                binding.incLayoutOne.customViewForFullscreen.visibility = View.GONE
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            override fun onPermissionRequest(request: PermissionRequest) {
                audioRequest = request
                for (permission in request.resources) {
                    when (permission) {
                        "android.webkit.resource.AUDIO_CAPTURE" -> {
                            askForPermission(
                                request.origin.toString(),
                                Manifest.permission.RECORD_AUDIO,
                                MY_PERMISSIONS_REQUEST_RECORD_AUDIO
                            )
                        }
                    }
                }
            }
        }
    }

    fun constructRedirectionUrl(url: String): String {
//        val url = "https://jsonplaceholder.typicode.com/photos/1?siId=3fb1d400-57e7-40af-84e7-15c8618174eb?userId=2efb99f6-bfcf-48e5-a9f6-4a45476ad7a0&entryVia=application-status"
        var queryParams = url.substringAfter("?", "")
        if (url.contains("entryVia")) {
            queryParams = queryParams.substringBeforeLast('&', "")
        }
        val path = url.substringAfterLast("&", "").substringAfterLast('=', "")
        Log.d("CONSTRUCTED_URL", "params: $queryParams : path $path")
        var completeUrl = Constants.FE_BASE_URL
        if (path !== "") {
            completeUrl = "$completeUrl/$path"
        }
        if (queryParams != "") {
            completeUrl = "$completeUrl?$queryParams"
        }
        Log.d("COMPLETED_URL", completeUrl)
        return completeUrl
    }

    fun getJobSeekerID(url: String): String {
        var urlValAfterAppId = url.substringAfter("applicationId=", "")
        if (urlValAfterAppId.contains("?") || urlValAfterAppId.contains("&")) {
            urlValAfterAppId = if (urlValAfterAppId.contains("?")) {
                url.substringAfter("applicationId=", "").substringBefore("?")
            } else {
                url.substringAfter("applicationId=", "").substringBefore("&")
            }
        }
        return urlValAfterAppId
    }

    fun askForPermission(origin: String, permission: String, requestCode: Int) {
        Log.d("WebView", "inside askForPermission for" + origin + "with" + permission)
        if (ContextCompat.checkSelfPermission(
                applicationContext, permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@HomeActivity, permission
                )
            ) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this@HomeActivity, arrayOf(permission), requestCode
                )
            }
        } else {
            audioRequest?.grant(audioRequest?.resources)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_RECORD_AUDIO -> {
                Log.d("WebView", "PERMISSION FOR AUDIO")
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    audioRequest?.grant(audioRequest!!.resources)
//                    audioRequest.webView.loadUrl("<your url>")
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    private fun redirectToHome(destination: String?, url: String?) {
        Log.d("HA PN", "initView RTH: ${PrefrenceUtils.retriveData(this, Constants.PHONENUMBER)}")
        startActivity(
            Intent(
                this@HomeActivity, ReactActivity::class.java
            ).putExtra("Navigation", destination).putExtra("link", url)
        )
        overridePendingTransition(R.anim.slide_out_bottom, R.anim.slide_in_bottom)
        finishAffinity()
    }


    private fun setInstrumentationWebView(type: String) {
        val bundle = Bundle()
        val properties = Properties()
        val data = HashMap<String, String>()
        val attribute = HashMap<String, Any>()
        Freshchat.trackEvent(this, type, attribute)
        fa?.logEvent(type, bundle)
        MoEHelper.getInstance(this).trackEvent(type, properties)
        UXCam.logEvent(type, data)
        BlitzLlamaSDK.getSdkManager(this).triggerEvent(type)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (binding.incLayoutOne.webView.canGoBack()) {
                        binding.incLayoutOne.webView.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun showDialogPayroll(redirectURL: String) {
        setInstrumentationUserTranscViewed("payroll")
        val dialog = Dialog(this, R.style.CustomAlertDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.payroll_dialogbox)

        val btn_letsGo = dialog.findViewById<Button>(R.id.btn_letsGo)
        val checkbox = dialog.findViewById<CheckBox>(R.id.checkBox)
        btn_letsGo.isEnabled = false
        btn_letsGo.background.alpha = 64;

        checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                btn_letsGo.isEnabled = true
                btn_letsGo.background.alpha = 255;
                btn_letsGo.setOnClickListener {
                    setInstrumentationUserTranscViewedClicked("payroll")
                    val intent = Intent(this@HomeActivity, MainActivity::class.java)
                    startActivity(intent)
                    dialog.dismiss()
                }
            } else {
                btn_letsGo.isEnabled = false
                btn_letsGo.background.alpha = 64;
            }
        }
        dialog.setCancelable(false)
        dialog.show()


    }

    private fun setInstrumentationUserTranscViewedClicked(type: String) {
        val bundle = Bundle()
        val attribute = HashMap<String, Any>()
        val data = HashMap<String, String>()
        val properties = Properties()
        bundle.putString(Constants.TYPE, type)
        attribute[Constants.TYPE] = type
        data[Constants.TYPE] = type
        properties.addAttribute(Constants.TYPE, type)
        fa!!.logEvent(Constants.USER_TRANSITION_TAPPED, bundle)
        MoEHelper.getInstance(this).trackEvent(Constants.USER_TRANSITION_TAPPED, properties)
        Freshchat.trackEvent(this, Constants.USER_TRANSITION_TAPPED, attribute)
        UXCam.logEvent(Constants.USER_TRANSITION_TAPPED, data)
        BlitzLlamaSDK.getSdkManager(this).triggerEvent(Constants.USER_TRANSITION_TAPPED)
    }

    private fun setInstrumentationUserTranscViewed(type: String) {
        val bundle = Bundle()
        val attribute = HashMap<String, Any>()
        val data = HashMap<String, String>()
        val properties = Properties()
        bundle.putString(Constants.TYPE, type)
        attribute[Constants.TYPE] = type
        data[Constants.TYPE] = type
        properties.addAttribute(Constants.TYPE, type)
        fa!!.logEvent(Constants.USER_TRANSITION_VIEWED, bundle)
        MoEHelper.getInstance(this).trackEvent(Constants.USER_TRANSITION_VIEWED, properties)
        Freshchat.trackEvent(this, Constants.USER_TRANSITION_VIEWED, attribute)
        UXCam.logEvent(Constants.USER_TRANSITION_VIEWED, data)
        BlitzLlamaSDK.getSdkManager(this).triggerEvent(Constants.USER_TRANSITION_VIEWED)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showDialogNonPayroll(redirectURL: String) {
        setInstrumentationUserTranscViewed(Constants.NON_PAYROLL)
        val dialog = Dialog(this, R.style.CustomAlertDialog)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.nonpayroll_dialogbox)

        val btn_letsGo = dialog.findViewById<Button>(R.id.btn_letsGo)
        val checkbox = dialog.findViewById<CheckBox>(R.id.checkBox)
        btn_letsGo.isEnabled = false
        btn_letsGo.background.alpha = 64;

        checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                btn_letsGo.isEnabled = true
                btn_letsGo.background.alpha = 255;
                btn_letsGo.setOnClickListener {
                    setInstrumentationUserTranscViewedClicked(Constants.NON_PAYROLL)
                    openWebView(redirectURL)
                    dialog.dismiss()
                }
            } else {
                btn_letsGo.isEnabled = false
                btn_letsGo.background.alpha = 64;
            }
        }
        dialog.setCancelable(false)
        dialog.show()


    }

    private fun logoutFunction() =
        AlertDialog.Builder(this).setTitle(this.getString(R.string.alert_logout))
            .setMessage(this.getString(R.string.alert_logout_message)).setCancelable(true)
            .setNegativeButton(
                this.getString(R.string.alert_no)
            ) { dialog: DialogInterface, _: Int -> dialog.cancel() }.setPositiveButton(
                this.getString(R.string.alert_yes)
            ) { _: DialogInterface?, _: Int ->
                Log.d(
                    "HA PN",
                    "initView LO: ${PrefrenceUtils.retriveData(this, Constants.PHONENUMBER)}"
                )
                PrefrenceUtils.logoutUser(this)
                startActivity(Intent(this, EnterNumberActivity::class.java))
                finishAffinity()
            }.create().show()
}