package com.rootscare.ui.supportmore

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProviders
import com.rootscare.BR
import com.rootscare.R
import com.rootscare.databinding.CommonWebviewScreenBinding
import com.rootscare.ui.base.BaseActivity
import com.rootscare.utilitycommon.LanguageModes
import com.rootscare.utilitycommon.SupportMoreUrls
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

const val OPEN_FOR = "open_for_screen"

const val OPEN_FOR_ABOUT = "OPEN_FOR_ABOUT"
const val OPEN_FOR_TERMS = "OPEN_FOR_TERMS"
const val OPEN_FOR_PRIVACY = "OPEN_FOR_PRIVACY"

class CommonWebviewScreen : BaseActivity<CommonWebviewScreenBinding, CommonViewModel>() {
    private var binding: CommonWebviewScreenBinding? = null
    private var mViewModel: CommonViewModel? = null

    override val bindingVariable: Int
        get() = BR.viewModel
    override val layoutId: Int
        get() = R.layout.common_webview_screen
    override val viewModel: CommonViewModel
        get() {
            mViewModel = ViewModelProviders.of(this).get(CommonViewModel::class.java)
            return mViewModel as CommonViewModel
        }

    private var mCM: String? = null
    private var mUMA: ValueCallback<Array<Uri?>?>? = null
    private val FCR = 1
    private var openFor = ""
    private var urlToView = ""
    private var screenTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = viewDataBinding
        binding?.toolbarBack?.setOnClickListener { finish() }

        initViews()

    }

    private fun initViews() {
        intent?.let { openFor = it.getStringExtra(OPEN_FOR).orEmpty() }

        when {
            openFor.equals(OPEN_FOR_ABOUT, true) -> {
            screenTitle = getString(R.string.about_rootscare)
            urlToView = if (mViewModel?.appSharedPref?.languagePref.equals(LanguageModes.ENG.get(), ignoreCase = true)) {
                    SupportMoreUrls.ABOUTUS.getEngLink() } else SupportMoreUrls.ABOUTUS.getArabicLink()
            }
            openFor.equals(OPEN_FOR_TERMS, true) -> {
                screenTitle = getString(R.string.terms_and_conditions)
                urlToView = if (mViewModel?.appSharedPref?.languagePref.equals(LanguageModes.ENG.get(), ignoreCase = true)) {
                    SupportMoreUrls.TERMS_CONIDTIONS.getEngLink() } else SupportMoreUrls.TERMS_CONIDTIONS.getArabicLink()
            }
            openFor.equals(OPEN_FOR_PRIVACY, true) -> {
                screenTitle = getString(R.string.privacy_policy)
                urlToView = if (mViewModel?.appSharedPref?.languagePref.equals(LanguageModes.ENG.get(), ignoreCase = true)) {
                    SupportMoreUrls.PRIVACY_POLICY.getEngLink() } else SupportMoreUrls.PRIVACY_POLICY.getArabicLink()
            }
            else -> {
                screenTitle = getString(R.string.about_rootscare)
                urlToView = if (mViewModel?.appSharedPref?.languagePref.equals(LanguageModes.ENG.get(), ignoreCase = true)) {
                    SupportMoreUrls.ABOUTUS.getEngLink() } else SupportMoreUrls.ABOUTUS.getArabicLink()
               }
            }

        binding?.toolbarTitle?.text = screenTitle
        loadingWeview(urlToView)
    }

    private fun loadingWeview(urlToLoad: String) {
        try {
            binding?.webview?.settings?.apply {
                javaScriptEnabled = true
                setSupportZoom(false)
                domStorageEnabled = true
                pluginState = WebSettings.PluginState.ON
                allowFileAccess = true
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
            }

            binding?.webview?.apply {
                clearCache(true)
                scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
                webViewClient = object : WebViewClient() {

                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        try {
                            binding?.progressBar?.visibility = View.VISIBLE
                            url?.let { view?.loadUrl(it) }
                        } catch (e: Exception) {

                        }
                        return false
                    }

                    //called when a page starts loading
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        try {
                            binding?.progressBar?.visibility = View.VISIBLE
                        } catch (e: Exception) {

                        }
                        super.onPageStarted(view, url, favicon)
                    }

                    //called when a page is fully loadedd
                    override fun onPageFinished(view: WebView?, url: String?) {
                        try {
                            binding?.progressBar?.visibility = View.GONE
                        } catch (e: Exception) {

                        }
                        super.onPageFinished(view, url)
                    }

                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        try {
                            var message = "SSL Certificate error."
                            when (error?.primaryError) {
                                SslError.SSL_UNTRUSTED -> message =
                                    "The certificate authority is not trusted."
                                SslError.SSL_EXPIRED -> message = "The certificate has expired."
                                SslError.SSL_IDMISMATCH -> message =
                                    "The certificate Hostname mismatch."
                                SslError.SSL_NOTYETVALID -> message =
                                    "The certificate is not yet valid."
                            }
                            message += " Do you want to continue anyway?"
                            val builder = AlertDialog.Builder(this@CommonWebviewScreen)
                                .setCancelable(false)
                                .setTitle("SSL Certificate Error")
                                .setMessage(message)
                                .setNegativeButton("Cancel") { dialog, which ->
                                    try {
                                        handler?.cancel()
                                    } catch (e: Exception) {

                                    }
                                }
                                .setPositiveButton("Ok") { _, _ ->
                                    try {
                                        handler?.proceed()
                                    } catch (e: Exception) {

                                    }

                                }

                            val dialog = builder.create()
                            dialog.show()
                        } catch (e: Exception) {

                        }


                        //     super.onReceivedSslError(view, handler, error);
                    }

                    //called when a error is occurred while loading a URL
                    override fun onReceivedError(
                        view: WebView?, errorCode: Int, description: String?, failingUrl: String?
                    ) {

                    }
                }
            }

            // load the initial webview
            binding?.webview?.loadUrl(urlToLoad)
            binding?.webview?.webChromeClient = object : WebChromeClient() {
                @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                override fun onPermissionRequest(request: PermissionRequest?) {
                    Log.d("TAG", "onPermissionRequest")

                    this@CommonWebviewScreen.runOnUiThread {
                        if (request?.origin.toString() == "file:///") {
                            request?.grant(request.resources)
                        } else {
                            request?.deny()
                        }
                    }
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri?>?>,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    if (mUMA != null) {
                        mUMA?.onReceiveValue(null)
                    }
                    mUMA = filePathCallback
                    var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    if (takePictureIntent!!.resolveActivity(this@CommonWebviewScreen.packageManager) != null) {
                        var photoFile: File? = null
                        try {
                            photoFile = createImageFile()
                            takePictureIntent.putExtra("PhotoPath", mCM)
                        } catch (ex: IOException) {
                            Log.e("Webview", "Image file creation failed", ex)
                        }
                        if (photoFile != null) {
                            mCM = "file:" + photoFile.absolutePath
                            takePictureIntent.putExtra(
                                MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile)
                            )
                        } else {
                            takePictureIntent = null
                        }
                    }
                    val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                    contentSelectionIntent.type = "*/*"
                    val intentArray =
                        takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls<Intent>(0)

                    val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                    startActivityForResult(chooserIntent, FCR)
                    return true
                }
            }

        } catch (e: Exception) {

        }

    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        @SuppressLint("SimpleDateFormat") val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    override fun onBackPressed() {
        try {
            if (binding?.webview?.canGoBack() == true) {
                binding?.webview?.goBack()
            } else {
                super.onBackPressed()
            }

        } catch (e: java.lang.Exception) {

        }
    }

    override fun reloadPageAfterConnectedToInternet() {

    }
}