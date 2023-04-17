package com.caressa.common.base

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.caressa.common.R
import com.caressa.common.extension.setupSnackbar
import com.caressa.common.extension.setupSnackbarMessenger
import com.caressa.common.utils.Event
import com.caressa.common.utils.SmartWebViewHelper
import com.caressa.common.utils.Utilities
import com.caressa.navigation.NavigationCommand
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

abstract class BaseWebViewFragment : Fragment() {

    private val progressDialog by lazy {
        ProgressDialog(context)
    }
    internal var asw_view: WebView? = null
    internal var loadingImageView: ImageView? = null
    private var asw_cam_message: String? = null
    private var asw_file_message: ValueCallback<Uri>? = null
    private var asw_file_path: ValueCallback<Array<Uri>>? = null
    private val asw_file_req = 1
    private var WEB_URL = ""
    private var COOKIE_STRING = ""
    var TAG = BaseFragment::class.java.simpleName

    private val loc_perm = 1
    private val file_perm = 2

    fun setAsw_view(asw_view: WebView) {
        this.asw_view = asw_view
    }

    fun getLoadingImageView(): ImageView {
        return loadingImageView!!
    }

    fun setLoadingImageView(loadingImageView: ImageView) {
        this.loadingImageView = loadingImageView
    }

    fun setWEB_URL(WEB_URL: String) {
        Timber.i("WEB_URL_Fragment-----> " + WEB_URL)
        this.WEB_URL = WEB_URL
    }

    fun setCOOKIE_STRING(COOKIE_STRING: String) {
        this.COOKIE_STRING = COOKIE_STRING
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeNavigation(getViewModel())
        setupSnackbar(this, getViewModel().snackBarError, Snackbar.LENGTH_LONG)
        setupSnackbarMessenger(this, getViewModel().snackMessenger, Snackbar.LENGTH_LONG)
        setUpToast(this, getViewModel().toastMessage)
        setUpProgressBar(this, getViewModel().progressBar)
        if (Build.VERSION.SDK_INT > 9) {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            val builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
        }
        //**********************************
        val animation = AnimationUtils.loadAnimation(context, R.anim.rotate_forward)
        loadingImageView!!.startAnimation(animation)

        //Getting basic device information
        get_info()

        //Getting GPS location of device if given permission
        if (SmartWebViewHelper.ASWP_LOCATION && !check_permission(1)) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                loc_perm
            )
        }
        //Webview settings; defaults are customized for best performance
        val webSettings = asw_view!!.getSettings()

        if (!SmartWebViewHelper.ASWP_OFFLINE) {
            webSettings.javaScriptEnabled = SmartWebViewHelper.ASWP_JSCRIPT
        }
        webSettings.saveFormData = SmartWebViewHelper.ASWP_SFORM
        webSettings.setSupportZoom(SmartWebViewHelper.ASWP_ZOOM)
        webSettings.setGeolocationEnabled(SmartWebViewHelper.ASWP_LOCATION)
        webSettings.allowFileAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.useWideViewPort = false
        webSettings.domStorageEnabled = true

        try {
            CookieSyncManager.createInstance(context)
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.removeSessionCookie()

            cookieManager.setCookie(WEB_URL, COOKIE_STRING)
            CookieSyncManager.getInstance().sync()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        asw_view!!.setOnLongClickListener(View.OnLongClickListener { true })
        asw_view!!.setHapticFeedbackEnabled(false)

        asw_view!!.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            if (!check_permission(2)) {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ), file_perm
                )
            } else {
                val request = DownloadManager.Request(Uri.parse(url))

                request.setMimeType(mimeType)
                val cookies = CookieManager.getInstance().getCookie(url)
                request.addRequestHeader("cookie", cookies)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading")
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
                )
                val dm =
                    requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Utilities.toastMessageLong(context, "Downloading...")
            }
        })

        if (Build.VERSION.SDK_INT >= 21) {
            asw_view!!.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        } else if (Build.VERSION.SDK_INT >= 19) {
            asw_view!!.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        asw_view!!.setVerticalScrollBarEnabled(false)
        asw_view!!.setWebViewClient(CallbackWebViewClient())

        //Rendering the default URL
        Log.d("ADVANCE_WEB_VIEW", "WEB_URL $WEB_URL")
        aswm_view(WEB_URL, false)

        asw_view!!.setWebChromeClient(object : WebChromeClient() {
            //Handling input[type="file"] requests for android API 16+
            fun openFileChooser(
                uploadMsg: ValueCallback<Uri>,
                acceptType: String,
                capture: String
            ) {
                if (SmartWebViewHelper.ASWP_FUPLOAD) {
                    asw_file_message = uploadMsg
                    val i = Intent(Intent.ACTION_GET_CONTENT)
                    i.addCategory(Intent.CATEGORY_OPENABLE)
                    i.type = SmartWebViewHelper.ASWV_F_TYPE
                    if (SmartWebViewHelper.ASWP_MULFILE) {
                        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    startActivityForResult(Intent.createChooser(i, "File Chooser"), asw_file_req)
                }
            }

            //Handling input[type="file"] requests for android API 21+
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {
                if (check_permission(2) && check_permission(3)) {
                    if (SmartWebViewHelper.ASWP_FUPLOAD) {
                        if (asw_file_path != null) {
                            asw_file_path!!.onReceiveValue(null)
                        }
                        asw_file_path = filePathCallback
                        var takePictureIntent: Intent? = null
                        if (SmartWebViewHelper.ASWP_CAMUPLOAD) {
                            takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            if (takePictureIntent.resolveActivity(requireContext().packageManager) != null) {
                                var photoFile: File? = null
                                try {
                                    photoFile = create_image()
                                    takePictureIntent.putExtra("PhotoPath", asw_cam_message)
                                } catch (ex: IOException) {
                                    Log.e(TAG, "Image file creation failed", ex)
                                }

                                if (photoFile != null) {
                                    asw_cam_message = "file:" + photoFile.absolutePath
                                    takePictureIntent.putExtra(
                                        MediaStore.EXTRA_OUTPUT,
                                        Uri.fromFile(photoFile)
                                    )
                                } else {
                                    takePictureIntent = null
                                }
                            }
                        }
                        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                        if (!SmartWebViewHelper.ASWP_ONLYCAM) {
                            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                            contentSelectionIntent.type = SmartWebViewHelper.ASWV_F_TYPE
                            if (SmartWebViewHelper.ASWP_MULFILE) {
                                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }
                        }
                        val intentArray: Array<Intent?>
                        if (takePictureIntent != null) {
                            intentArray = arrayOf(takePictureIntent)
                        } else {
                            intentArray = arrayOfNulls(0)
                        }

                        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, "File Chooser")
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                        startActivityForResult(chooserIntent, asw_file_req)
                    }
                    return true
                } else {
                    get_file()
                    return false
                }
            }

            //Getting webview rendering progress
            override fun onProgressChanged(view: WebView, p: Int) {

            }

            // overload the geoLocations permissions prompt to always allow instantly as app permission was granted previously
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                if (Build.VERSION.SDK_INT < 23 || Build.VERSION.SDK_INT >= 23 && check_permission(1)) {
                    // location permissions were granted previously so auto-approve
                    callback.invoke(origin, true, false)
                } else {
                    // location permissions not granted so request them
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        loc_perm
                    )
                }
            }
        })

        if (requireActivity().intent.data != null) {
            val path = requireActivity().intent.dataString
            /*
            If you want to check or use specific directories or schemes or hosts

            Uri data        = getIntent().getData();
            String scheme   = data.getScheme();
            String host     = data.getHost();
            List<String> pr = data.getPathSegments();
            String param1   = pr.get(0);
            */
            aswm_view(path!!, false)
        }
        //**********************************
    }

    fun setUpToast(
        lifecycleOwner: LifecycleOwner,
        toastEvent: LiveData<Event<String>>,
    ) {
        toastEvent.observe(lifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { data ->
                showToast(data)
            }
        })
    }

    private fun showToast(data: String) {
        Utilities.toastMessageShort(requireContext(), data)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (Build.VERSION.SDK_INT >= 21) {
            var results: Array<Uri>? = null
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == asw_file_req) {
                    if (null == asw_file_path) {
                        return
                    }
                    if (intent == null || intent.data == null) {
                        if (asw_cam_message != null) {
                            results = arrayOf(Uri.parse(asw_cam_message))
                        }
                    } else {
                        val dataString = intent.dataString
                        if (dataString != null) {
                            results = arrayOf(Uri.parse(dataString))
                        } else {
                            if (SmartWebViewHelper.ASWP_MULFILE) {
                                if (intent.clipData != null) {
                                    //val numSelectedFiles = intent.clipData!!.itemCount
                                    //results = arrayOfNulls(numSelectedFiles)
                                    val numSelectedFiles = intent.data
                                    results = arrayOf<Uri>(numSelectedFiles!!)
                                    for (i in 0 until intent.clipData!!.itemCount) {
                                        results[i] = intent.clipData!!.getItemAt(i).uri
                                    }
                                }
                            }
                        }
                    }
                }
            }
            asw_file_path!!.onReceiveValue(results)
            asw_file_path = null
        } else {
            if (requestCode == asw_file_req) {
                if (null == asw_file_message) return
                val result =
                    if (intent == null || resultCode != Activity.RESULT_OK) null else intent!!.data
                asw_file_message!!.onReceiveValue(result)
                asw_file_message = null
            }
        }
    }

    fun setUpProgressBar(lifecycleOwner: LifecycleOwner, progressBar: LiveData<Event<String>>) {
        progressBar.observe(lifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it.equals(Event.HIDE_PROGRESS, true))
                    showProgress(false)
                else
                    showProgress(true, it)
            }
        })
    }

    private fun showProgress(showProgress: Boolean, message: String = "Loading...") {
        try {
            if (showProgress) {
                progressDialog.setMessage(message)
                progressDialog.isIndeterminate = false
                progressDialog.setCancelable(true)
                progressDialog.setCanceledOnTouchOutside(false)
                progressDialog.show()
            } else {
                if (progressDialog.isShowing) run {
                    progressDialog.cancel()
                    progressDialog.dismiss()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeNavigation(viewModel: BaseViewModel) {
        viewModel.navigation.observe(viewLifecycleOwner, {
            it?.getContentIfNotHandled()?.let { command ->
                when (command) {
                    is NavigationCommand.To -> findNavController().navigate(
                        command.directions,
                        getExtras()
                    )
                    is NavigationCommand.Back -> findNavController().navigateUp()
                }
            }
        })
    }

    abstract fun getViewModel(): BaseViewModel

    /**
     * [FragmentNavigatorExtras] mainly used to enable Shared Element transition
     */
    open fun getExtras(): FragmentNavigator.Extras = FragmentNavigatorExtras()

    //Opening URLs inside webview with request
    internal fun aswm_view(url: String, tab: Boolean) {
        var url = url
        if (tab) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } else {
            if (url.contains("?")) { // check to see whether the url already has query parameters and handle appropriately.
                url += "&"
            } else {
                url += "?"
            }
            if (asw_view != null) {
                asw_view!!.loadUrl(url)
            }
        }
    }

    //Getting device basic information
    fun get_info() {
        try {
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setCookie(WEB_URL, "DEVICE=android")
            cookieManager.setCookie(WEB_URL, "DEV_API=" + Build.VERSION.SDK_INT)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //Checking permission for storage and camera for writing and uploading images
    fun get_file() {
        val perms = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        //Checking for storage permission to write images for upload
        if (SmartWebViewHelper.ASWP_FUPLOAD && SmartWebViewHelper.ASWP_CAMUPLOAD && !check_permission(
                2
            ) && !check_permission(3)
        ) {
            ActivityCompat.requestPermissions(requireActivity(), perms, file_perm)

            //Checking for WRITE_EXTERNAL_STORAGE permission
        } else if (SmartWebViewHelper.ASWP_FUPLOAD && !check_permission(2)) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                file_perm
            )

            //Checking for CAMERA permissions
        } else if (SmartWebViewHelper.ASWP_CAMUPLOAD && !check_permission(3)) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                file_perm
            )
        }
    }

    //Checking if particular permission is given or not
    fun check_permission(permission: Int): Boolean {
        when (permission) {
            1 -> return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) === PackageManager.PERMISSION_GRANTED

            2 -> return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) === PackageManager.PERMISSION_GRANTED

            3 -> return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) === PackageManager.PERMISSION_GRANTED
        }
        return false
    }

    //Creating image file for upload
    @Throws(IOException::class)
    private fun create_image(): File {
        @SuppressLint("SimpleDateFormat")
        val file_name = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val new_name = "file_" + file_name + "_"
        val sd_directory =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(new_name, ".jpg", sd_directory)
    }

    //Actions based on shouldOverrideUrlLoading
    fun url_actions(view: WebView, url: String): Boolean {
        var url = url
        var isAction = true
        //Show toast error if not connected to the network
        if (url.startsWith("refresh:")) {
            aswm_view(WEB_URL, false)

            //Use this in a hyperlink to launch default phone dialer for specific number :: href="tel:+919876543210"
        } else if (url.startsWith("tel:")) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
            startActivity(intent)

        } else if (url.startsWith("mailto:")) {
            url = url.substring(7)
            val mail = Intent(Intent.ACTION_SEND)
            mail.type = "application/octet-stream"
            mail.putExtra(Intent.EXTRA_EMAIL, arrayOf(url))
            startActivity(mail)
            return true
        } else if (url.startsWith("rate:")) {
            val app_package =
                requireContext().packageName//requesting app package name from Context or Activity object
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$app_package")
                    )
                )
            } catch (anfe: ActivityNotFoundException) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$app_package")
                    )
                )
            }

            //Sharing content from your webview to external apps :: href="share:URL" and remember to place the URL you want to share after share:___
        } else if (url.startsWith("share:")) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, view.title)
            intent.putExtra(
                Intent.EXTRA_TEXT,
                view.title + "\nVisit: " + Uri.parse(url).toString().replace("share:", "")
            )
            startActivity(Intent.createChooser(intent, "Share"))

            //Use this in a hyperlink to exit your app :: href="exit:android"
        } else if (url.startsWith("exit:")) {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } else {
            isAction = false
        }
        return isAction
    }

/*    fun onKeyDown(keyCode: Int, @NonNull event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (asw_view!!.canGoBack()) {
                        asw_view!!.goBack()
                    } else {
                        requireActivity().finish()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }*/

    //Setting activity layout visibility
    private inner class CallbackWebViewClient : WebViewClient() {

        override fun onPageFinished(view: WebView, url: String) {
            if (loadingImageView != null) {
                loadingImageView!!.clearAnimation()
                loadingImageView!!.visibility = View.GONE
            }
        }

        //For android below API 23
        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String,
            failingUrl: String
        ) {
            Utilities.toastMessageShort(context, getString(R.string.UNEXPECTED_ERROR))
        }

        //Overriding webview URLs
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            return url_actions(view, url)
        }

        //Overriding webview URLs for API 23+ [suggested by github.com/JakePou]
        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return url_actions(view, request.url.toString())
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            if (SmartWebViewHelper.ASWP_CERT_VERIFICATION) {
                super.onReceivedSslError(view, handler, error)
            } else {
                handler.proceed() // Ignore SSL certificate errors
            }
        }
    }

}