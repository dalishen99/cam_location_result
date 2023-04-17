package com.mohammadkk.mybrowser

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.net.http.SslCertificate
import android.os.*
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.SpannableStringBuilder
import android.util.Base64
import android.util.Patterns
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.TextUtilsCompat
import androidx.core.text.bold
import androidx.core.view.*
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mohammadkk.mybrowser.adapters.SearchAdapter
import com.mohammadkk.mybrowser.adapters.TabsAdapter
import com.mohammadkk.mybrowser.databinding.ActivityMainBinding
import com.mohammadkk.mybrowser.databinding.PopupMenuWindowBinding
import com.mohammadkk.mybrowser.databinding.TabsViewBinding
import com.mohammadkk.mybrowser.databinding.TopSearchBarBinding
import com.mohammadkk.mybrowser.models.StartFileData
import com.mohammadkk.mybrowser.models.WebTab
import com.mohammadkk.mybrowser.utils.*
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.util.*


class MainActivity : AppCompatActivity(), RuntimeHandler.OnActivityIntent, RuntimeHandler.OnPermissionCallback {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bind1: TopSearchBarBinding
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var runtimeHandler: RuntimeHandler
    private val tabs: ArrayList<WebTab> = ArrayList()
    private val tabsAdapter: TabsAdapter by lazy { TabsAdapter(this, tabs) }
    private val storagePermission: String get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Manifest.permission.READ_EXTERNAL_STORAGE
    } else Manifest.permission.WRITE_EXTERNAL_STORAGE
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var originGps: String? = null
    private var callbackGps: GeolocationPermissions.Callback? = null
    private var downloadInfo = arrayOfNulls<String>(4)
    private val fullScreenView = arrayOfNulls<View>(1)
    private val fullScreenCallback = arrayOfNulls<WebChromeClient.CustomViewCallback>(1)
    private var printJob: PrintJob? = null
    private var isDesktopMode = false
    private var isFullscreenMode = false
    private var isNightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        bind1 = binding.topSearch
        btnBack = if (isRtlLocale()) binding.btnForward else binding.btnBack
        btnForward = if (isRtlLocale()) binding.btnBack else binding.btnForward
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setContentView(binding.root)
        runtimeHandler = RuntimeHandler(this)
        binding.webProgress.max = 100
        binding.webProgress.progress = 0
        currentTabIndex = 0
        val initUrl = getUrlByIntent(intent)
        newTab(initUrl ?: GOOGLE_URL)
        switchToTab(tabs.size - 1)
        val searchAdapter = SearchAdapter(this) { result ->
            bind1.edtUrl.setText(result)
            bind1.edtUrl.setSelection(result.length)
        }
        bind1.edtUrl.isSelected = false
        bind1.edtUrl.setAdapter(searchAdapter)
        bind1.edtUrl.setOnEditorActionListener { v, action, keyEvent ->
            val actions = action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE
                    || action == EditorInfo.IME_ACTION_UNSPECIFIED
            if (actions || keyEvent.action == KeyEvent.KEYCODE_ENTER) {
                onLoadUrl(currentWebView, v.text?.toString())
                hideKeyboard(v)
                bind1.edtUrl.clearFocus()
                return@setOnEditorActionListener true
            }

            return@setOnEditorActionListener false
        }
        bind1.edtUrl.setOnItemClickListener { _, _, position, _ ->
            bind1.edtUrl.setText(searchAdapter.getResult(position))
            onLoadUrl(currentWebView, searchAdapter.getResult(position))
            hideKeyboard(bind1.edtUrl)
            bind1.edtUrl.clearFocus()
        }
        bind1.btnRefresh.setOnClickListener {
            val tag = it?.tag?.toString() ?: ""
            if (tag == "stop")
                currentWebView.stopLoading()
            else if (tag == "refresh")
                currentWebView.reload()
        }
        bind1.btnMore.setOnClickListener { onCreatePopupWindow() }
        initOnFindInPage()
        bind1.btnMic.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")
            try {
                runtimeHandler.onRequestActivityResult(MICROPHONE_REQUEST_CODE, intent, this)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnTabs.setOnClickListener { onCreateTabDialog() }
        btnBack.setOnClickListener {
            if (currentWebView.canGoBack())
                currentWebView.goBack()
        }
        btnForward.setOnClickListener {
            if (currentWebView.canGoForward())
                currentWebView.goForward()
        }
        binding.btnHome.setOnClickListener { currentWebView.loadUrl(GOOGLE_URL) }
    }
    private fun createWebView(): WebView {
        val wv = WebView(this)
        wv.isFocusable = true
        wv.isFocusableInTouchMode = true
        wv.overScrollMode = WebView.OVER_SCROLL_NEVER
        wv.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        true.also { wv.settings.javaScriptEnabled = it }
        wv.settings.apply {
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(true)
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.webProgress.progress = 0
                binding.webProgress.isVisible = true
                bind1.btnRefresh.setImageResource(R.drawable.ic_stop)
                bind1.btnRefresh.tag = "stop"
                if (view == currentWebView) {
                    bind1.edtUrl.setText(url ?: "")
                    bind1.edtUrl.setSelection(0)
                    changeForceNightWeb(isNightMode)
                }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                bind1.btnRefresh.setImageResource(R.drawable.ic_refresh)
                bind1.btnRefresh.tag = "refresh"
                if (view == currentWebView) {
                    if (bind1.edtUrl.selectionStart == 0 && bind1.edtUrl.selectionEnd == 0 &&
                        bind1.edtUrl.text.toString() == view.url) {
                        view.requestFocus()
                    }
                    btnForward.isEnabled = view.canGoForward()
                    btnBack.isEnabled = view.canGoBack()
                }
            }
            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
                if (tabs[currentTabIndex].isDestopMode && view == currentWebView) {
                    val jsDesktop = "document.querySelector('meta[name=\"viewport\"]').setAttribute('content'," +
                            " 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));"
                    view.evaluateJavascript(jsDesktop) {}
                }
            }
        }
        wv.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                tabsAdapter.notifyChange()
            }
            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                tabsAdapter.notifyChange()
            }
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress == 100) {
                    binding.webProgress.isVisible = false
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        binding.webProgress.setProgress(newProgress, true)
                    } else binding.webProgress.progress = newProgress
                }
            }
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (hasPermissions(permissions)) {
                    callback?.invoke(origin, true, false)
                } else {
                    callbackGps = callback
                    originGps = origin
                    runtimeHandler.onRequestPermission(GEO_LOCATION_REQUEST_CODE[0], permissions, this@MainActivity)
                }
            }
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                mFilePathCallback?.onReceiveValue(null)
                mFilePathCallback = filePathCallback
                val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                contentSelectionIntent.type = "*/*"
                val intentArray: Array<Intent?> = arrayOfNulls(0)
                val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                runtimeHandler.onRequestActivityResult(INPUT_FILE_REQUEST_CODE, chooserIntent, this@MainActivity)
                return true
            }
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                super.onShowCustomView(view, callback)
                fullScreenView[0] = view
                fullScreenCallback[0] = callback
                binding.mainLayout.isInvisible = true
                binding.fullScreenLayout.addView(view)
                binding.fullScreenLayout.isVisible = true
            }
            override fun onHideCustomView() {
                super.onHideCustomView()
                if (fullScreenView[0] == null) return
                binding.fullScreenLayout.removeView(fullScreenView[0])
                binding.fullScreenLayout.isVisible = false
                fullScreenView[0] = null
                fullScreenCallback[0] = null
                binding.mainLayout.isInvisible = false
            }
            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                newTab(resultMsg)
                switchToTab(currentTabIndex + 1)
                return true
            }
        }
        wv.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            val c = activeMatchOrdinal + 1
            val str = if (numberOfMatches > 0) {
                String.format(Locale.getDefault(), "%d/%d", c, numberOfMatches)
            } else ""
            bind1.tvCountFind.text = str
        }
        wv.setOnLongClickListener { v ->
            if (v == null) return@setOnLongClickListener false
            val urls: Array<String?> = arrayOf(null, null)
            val r = (v as WebView).hitTestResult
            when (r.type) {
                WebView.HitTestResult.SRC_ANCHOR_TYPE -> urls[0] = r.extra
                WebView.HitTestResult.IMAGE_TYPE -> urls[1] = r.extra
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE, WebView.HitTestResult.EMAIL_TYPE, WebView.HitTestResult.UNKNOWN_TYPE -> {
                    val handler = Handler(Looper.getMainLooper())
                    val message = handler.obtainMessage()
                    v.requestFocusNodeHref(message)
                    urls[0] = message.data.getString("url")
                    if (urls[0] == "") urls[0] = null
                    urls[1] = message.data.getString("src")
                    if (urls[1] == "") urls[1] = null

                    if (urls[0] == null && urls[1] == null) {
                        return@setOnLongClickListener false
                    }
                }
                else -> return@setOnLongClickListener false
            }
            onCreateOptionsWebView(urls[0], urls[1])
            return@setOnLongClickListener true
        }
        wv.setDownloadListener { url, userAgent, _, mimetype, contentLength ->
            val filename = getFilenameByUrl(url)
            val spannableMsg = SpannableStringBuilder()
            spannableMsg.bold { append("File name: ") }.append("${filename}\n")
                .bold { append("Size: ") }.append(contentLength.formatSize(false) + "\n")
                .bold { append("URL: ") }.append(url)
            MaterialAlertDialogBuilder(this, R.style.ThemeRoundedDialog)
                .setTitle("Download")
                .setMessage(spannableMsg)
                .setPositiveButton(android.R.string.ok) { dialog, _ ->
                    onStartDownloader(filename, url, userAgent, mimetype)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .show()
        }
        return wv
    }
    private fun newTab(uri: String?) {
        val webView = createWebView()
        webView.isVisible = false
        tabs.add(WebTab(webView))
        binding.tabContainer.addView(webView)
        binding.tvTabCount.text = tabs.size.toString()
        onLoadUrl(webView, uri)
    }
    private fun newTab(resultMsg: Message?) {
        if (resultMsg == null) return
        val webView = createWebView()
        webView.isVisible = false
        val tab = WebTab(webView)
        tabs.add(currentTabIndex + 1, tab)
        binding.tvTabCount.text = tabs.size.toString()
        binding.tabContainer.addView(webView)
        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = webView
        resultMsg.sendToTarget()
    }
    private fun switchToTab(index: Int) {
        currentWebView.isVisible = false
        currentTabIndex = index
        currentWebView.isVisible = true
        bind1.edtUrl.setText(currentWebView.url ?: "")
        btnBack.isEnabled = currentWebView.canGoBack()
        btnForward.isEnabled = currentWebView.canGoForward()
        currentWebView.requestFocus()
    }
    private fun closeCurrentTab() {
        binding.tabContainer.removeView(currentWebView)
        currentWebView.destroy()
        tabs.removeAt(currentTabIndex)
        if (currentTabIndex >= tabs.size)
            currentTabIndex = tabs.size - 1

        if (currentTabIndex == -1) {
            newTab(GOOGLE_URL)
            currentTabIndex = 0
        }
        currentWebView.isVisible = true
        bind1.edtUrl.setText(currentWebView.url ?: "")
        binding.tvTabCount.text = tabs.size.toString()
        currentWebView.requestFocus()
    }
    private val currentWebView: WebView get() = tabs[currentTabIndex].view
    private fun onCreateTabDialog() {
        val view = layoutInflater.inflate(R.layout.tabs_view, binding.root, false)
        val bind = TabsViewBinding.bind(view)
        val tabsDialog = MaterialAlertDialogBuilder(this, R.style.ThemeRoundedDialog)
            .setView(view)
            .setTitle("Select Tab")
            .setPositiveButton("Add Tab") { d, _ ->
                newTab(GOOGLE_URL)
                switchToTab(tabs.size - 1)
                tabsAdapter.notifyChange()
                d.dismiss()
            }
            .setNeutralButton("Kill Tab") { d, _ ->
                closeCurrentTab()
                tabsAdapter.notifyChange()
                d.dismiss()
            }
            .create()
        bind.rvTabs.setHasFixedSize(true)
        bind.rvTabs.layoutManager = LinearLayoutManager(this)
        bind.rvTabs.adapter = tabsAdapter
        bind.rvTabs.layoutManager?.scrollToPosition(currentTabIndex)
        tabsAdapter.setOnItemClickListener { position ->
            if (position != currentTabIndex) switchToTab(position)
            tabsDialog.dismiss()
        }
        tabsDialog.show()
        val btnPositive = tabsDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNeutral = tabsDialog.getButton(AlertDialog.BUTTON_NEUTRAL)
        btnPositive.isAllCaps = false
        btnNeutral.isAllCaps = false
        btnPositive.setTextColor(ResourcesCompat.getColor(resources, R.color.grey_900, theme))
        btnNeutral.setTextColor(ResourcesCompat.getColor(resources, R.color.grey_900, theme))
        val d1 = resDrawable(R.drawable.ic_add, theme)
        val d2 = resDrawable(R.drawable.ic_remove, theme)
        if (d1 != null && d2 != null) {
            DrawableCompat.setTint(DrawableCompat.wrap(d1), resColor(R.color.blue_500, theme))
            DrawableCompat.setTint(DrawableCompat.wrap(d2), resColor(R.color.blue_500, theme))
        }
        btnPositive.setCompoundDrawablesWithIntrinsicBounds(null, null, d1, null)
        btnNeutral.setCompoundDrawablesWithIntrinsicBounds(null, null, d2, null)
    }
    private fun showToast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }
    private fun createSnackBar(text: String?, length: Int = Snackbar.LENGTH_SHORT, isActionCaps: Boolean = false): Snackbar {
        val snack = Snackbar.make(binding.root, text ?: "null", length)
        snack.view.translationY = -(resources.displayMetrics.density * 48)
        if (isActionCaps) {
            val tvSnack: TextView = snack.view.findViewById(com.google.android.material.R.id.snackbar_action)
            tvSnack.isAllCaps = false
        }
        return snack
    }
    override fun onResume() {
        super.onResume()
        if (printJob != null) {
            if (printJob!!.isCompleted) {
                createSnackBar("Successful -> ${printJob!!.info.label}", Snackbar.LENGTH_LONG)
                    .setAction(android.R.string.ok) {
                        startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
                    }.show()
            } else if (printJob!!.isFailed) {
                createSnackBar("Failed -> ${printJob!!.info.label}").show()
            }
            printJob = null
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val url = getUrlByIntent(intent)
        if (!url.isNullOrEmpty()) {
            newTab(url)
            switchToTab(tabs.size - 1)
        }
    }
    override fun activityForResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == MICROPHONE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val result = intent?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!result.isNullOrEmpty()) onLoadUrl(currentWebView, result[0])
            }
        } else if (requestCode == GEO_LOCATION_REQUEST_CODE[1]) {
            if (hasPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))) {
                callbackGps?.invoke(originGps, true, false)
            }
        } else if (requestCode == INPUT_FILE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                val dataString = intent.dataString
                if (dataString != null) {
                    val results = arrayOf(Uri.parse(dataString))
                    mFilePathCallback!!.onReceiveValue(results)
                    mFilePathCallback = null
                }
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: Array<Boolean>) {
        if (requestCode == GEO_LOCATION_REQUEST_CODE[0] && grantResults.isNotEmpty()) {
            if (hasPermissions(permissions)) {
                callbackGps?.invoke(originGps, true, false)
            } else {
                permissions.forEach {
                    if (shouldShowRequestPermission(it)) {
                        runtimeHandler.onRequestPermission(requestCode, permissions, this@MainActivity)
                    } else {
                        createSnackBar("Permission denied!", Snackbar.LENGTH_LONG, true)
                            .setAction("settings") {
                                runtimeHandler.onRequestActivityResult(
                                    GEO_LOCATION_REQUEST_CODE[1],
                                    getLoadAppInfo(),
                                    this@MainActivity
                                )
                            }.show()
                    }
                }
            }
        } else if (requestCode == DOWNLOAD_REQUEST_PERMISSION && grantResults.isNotEmpty()) {
            if (hasPermission(permissions[0])) {
                onStartDownloader(downloadInfo[0], downloadInfo[1], downloadInfo[2], downloadInfo[3])
            } else {
                if (shouldShowRequestPermission(permissions[0])) {
                    runtimeHandler.onRequestPermission(requestCode, permissions, this@MainActivity)
                } else {
                    createSnackBar("Permission denied!").show()
                }
            }
        }
    }
    override fun onBackPressed() {
        if (!bind1.searchLayout.isVisible || bind1.findPageLayout.isVisible) {
            onClearOnFindPage()
        } else if (binding.fullScreenLayout.isVisible && fullScreenCallback[0] != null) {
            fullScreenCallback[0]!!.onCustomViewHidden()
        } else if (currentWebView.canGoBack()) {
            currentWebView.goBack()
        } else if (tabs.size > 1) {
            closeCurrentTab()
        } else {
            super.onBackPressed()
        }
    }
    private fun onCreateOptionsWebView(linkUrl: String?, imageUrl: String?) {
        val urlTitle: Array<String?> = arrayOf(null, null)
        val options = arrayListOf("Open in new tab", "Copy URL", "Show full URL", "Download")
        if (imageUrl == null) {
            if (linkUrl != null) {
                urlTitle[0] = linkUrl
                urlTitle[1] = linkUrl
            } else {
                showToast("Bad null arguments in showLongPressMenu")
                return
            }
        } else {
            if (linkUrl == null) {
                urlTitle[0] = imageUrl
                urlTitle[1] = "Image: $imageUrl"
            } else {
                urlTitle[0] = linkUrl
                urlTitle[1] = linkUrl
                options.add("Image Options")
            }
        }
        val alert = MaterialAlertDialogBuilder(this, R.style.ThemeRoundedDialog).setTitle(urlTitle[1])
            .setItems(options.toTypedArray()) { dialog, which ->
                when (which) {
                    0 -> {
                        newTab(urlTitle[0])
                        switchToTab(tabs.size - 1)
                        if (imageUrl != null) currentWebView.zoomOut()
                    }
                    1 -> {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("URL", urlTitle[0])
                        clipboard.setPrimaryClip(clipData)
                        showToast("copied url address")
                    }
                    2 -> {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Full URL")
                            .setMessage(urlTitle[0])
                            .setPositiveButton(android.R.string.ok) { d, _ ->
                                d.dismiss()
                            }
                            .create().show()
                    }
                    3 -> {
                        onStartDownloader(
                            null, urlTitle[0], null, null
                        )
                    }
                    4 -> onCreateOptionsWebView(null, imageUrl)
                }
                dialog.dismiss()
            }
        alert.show()
    }
    private fun onCreatePopupWindow() {
        val pixel = resources.displayMetrics.density
        val v = layoutInflater.inflate(R.layout.popup_menu_window, binding.root, false)
        val vb = PopupMenuWindowBinding.bind(v)
        val width = resources.getDimensionPixelSize(R.dimen.width_popup_menu_window)
        val popup = PopupWindow(v, width, WindowManager.LayoutParams.WRAP_CONTENT)
        popup.setBackgroundDrawable(resDrawable(R.drawable.bg_popup_window, theme))
        popup.elevation = pixel * 8
        popup.isFocusable = true
        popup.isOutsideTouchable = true
        popup.width = resources.getDimensionPixelSize(R.dimen.width_popup_menu_window)
        vb.checkboxNightPage.isChecked = isNightMode
        vb.checkboxFullscreen.isChecked = isFullscreenMode
        vb.checkboxDesktop.isChecked = tabs[currentTabIndex].isDestopMode
        popup.showAsDropDown(bind1.btnMore, (pixel * 8).toInt(), -((pixel * 35).toInt()), Gravity.END)
        vb.llShareLink.setOnClickListener {
            if (currentWebView.url != EMPTY_URL) {
                shareLink(currentWebView.title, currentWebView.url)
            } else showToast("empty blank page!")
            popup.dismiss()
        }
        vb.llFindOnPage.setOnClickListener {
            onFindPage()
            popup.dismiss()
        }
        vb.llSaveAsPdf.setOnClickListener {
            printPdf()
            popup.dismiss()
        }
        vb.llFullScreen.setOnClickListener {
            isFullscreenMode = !isFullscreenMode
            vb.checkboxFullscreen.isChecked = isFullscreenMode
            changeFullscreenActivity(isFullscreenMode)
            popup.dismiss()
        }
        vb.llNightPage.setOnClickListener {
            isNightMode = !isNightMode
            vb.checkboxNightPage.isChecked = isNightMode
            changeForceNightWeb(isNightMode)
            popup.dismiss()
        }
        vb.llDesktop.setOnClickListener {
            isDesktopMode = !isDesktopMode
            vb.checkboxDesktop.isChecked = isDesktopMode
            changeDesktopModePage(isDesktopMode)
            popup.dismiss()
        }
        vb.checkboxNightPage.setOnCheckedChangeListener { buttonView, isChecked ->
            isNightMode = isChecked
            buttonView.isChecked = isNightMode
            changeForceNightWeb(isNightMode)
            popup.dismiss()
        }
        vb.checkboxFullscreen.setOnCheckedChangeListener { buttonView, isChecked ->
            isFullscreenMode = isChecked
            buttonView.isChecked = isFullscreenMode
            changeFullscreenActivity(isFullscreenMode)
            popup.dismiss()
        }
        vb.checkboxDesktop.setOnCheckedChangeListener { buttonView, isChecked ->
            isDesktopMode = isChecked
            buttonView.isChecked = isDesktopMode
            changeDesktopModePage(isDesktopMode)
            popup.dismiss()
        }
        vb.llPageInfo.setOnClickListener {
            val certificate = currentWebView.certificate
            val message = SpannableStringBuilder()
            message.bold { append("URL: ") }.append("${currentWebView.url}\n")
                .bold { append("Title: ") }.append("${currentWebView.title}\n")
            if (certificate == null) {
                message.bold { append("Certificate: ") }.append("Not secure")
            } else {
                message.bold { append("Certificate:\n" ) }.append(certificateToStr(certificate))
            }
            MaterialAlertDialogBuilder(this, R.style.ThemeRoundedDialog)
                .setTitle("Page Info")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { d, _ ->
                    d.dismiss()
                }
                .show()
            popup.dismiss()
        }
        vb.llClearAllCache.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.ThemeRoundedDialog)
                .setTitle("Clear all history")
                .setMessage("Are you sure you want to delete all your history?")
                .setPositiveButton(android.R.string.ok) { d, _ ->
                    currentWebView.clearCache(true)
                    currentWebView.clearFormData()
                    currentWebView.clearHistory()
                    val cookieManager = CookieManager.getInstance()
                    cookieManager?.flush()
                    cookieManager?.removeAllCookies { }
                    WebStorage.getInstance()?.deleteAllData()
                    d.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { d, _ ->
                    d.dismiss()
                }
                .show()
            popup.dismiss()
        }
    }
    private fun shareLink(title: String?, url: String?) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, url)
            startActivity(Intent.createChooser(this, "Share link:"))
        }
    }
    private fun onFindPage() {
        bind1.searchLayout.isVisible = false
        bind1.findPageLayout.isVisible = true
        bind1.edtFind.setText("")
        bind1.edtFind.requestFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                imm.showSoftInput(bind1.edtFind,InputMethodManager.SHOW_FORCED)
                handler.removeCallbacks(this)
            }
        }, 100)
    }
    private fun initOnFindInPage() {
        bind1.btnClose.setOnClickListener { onClearOnFindPage() }
        bind1.btnNext.setOnClickListener {
            hideKeyboard(bind1.edtFind)
            currentWebView.findNext(true)
        }
        bind1.btnPrev.setOnClickListener {
            hideKeyboard(bind1.edtFind)
            currentWebView.findNext(false)
        }
        bind1.edtFind.addTextChangedListener {
            currentWebView.findAllAsync(it.toString())
        }
    }
    private fun onClearOnFindPage() {
        bind1.edtFind.setText("")
        hideKeyboard(bind1.edtFind)
        bind1.edtFind.clearFocus()
        currentWebView.clearMatches()
        bind1.searchLayout.isVisible = true
        bind1.findPageLayout.isVisible = false
        currentWebView.requestFocus()
    }
    private fun printPdf() {
        var domain = Uri.parse(currentWebView.url).host?.replace("www.", "")?.trim() ?: ""
        domain = domain.replace(".", "_").trim() + "_" + getCurrentDate()
        val printAdapter = currentWebView.createPrintDocumentAdapter(domain)
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        printJob = printManager.print(domain, printAdapter,  PrintAttributes.Builder().build())
    }
    private fun changeFullscreenActivity(enable: Boolean) {
        if(enable) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, binding.root).let { controller ->
                controller.hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else{
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, binding.root).show(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars()
            )
        }
    }
    private fun changeDesktopModePage(enable: Boolean) {
        val tab = tabs[currentTabIndex]
        tab.isDestopMode = enable
        val settings = currentWebView.settings
        val strings = arrayOf("Mobile", "Android", "Mobile".reversed(), "Android".reversed())
        val newUserAgent = if (enable) {
            settings.userAgentString.replace(strings[0], strings[2]).replace(strings[1], strings[3])
        } else {
            settings.userAgentString.replace(strings[2], strings[0]).replace(strings[3], strings[1])
        }
        settings.userAgentString = newUserAgent
        settings.useWideViewPort = enable
        settings.loadWithOverviewMode = enable
        currentWebView.reload()
    }
    private fun changeForceNightWeb(enabled: Boolean, view: WebView? = null) {
        if (view != null) {
            appendToNightMode(enabled, view)
        } else {
            for (w in tabs) {
                appendToNightMode(enabled, w.view)
            }
        }
    }
    @Suppress("DEPRECATION")
    private fun appendToNightMode(enabled: Boolean, view: WebView) {
        if (enabled) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(view.settings, WebSettingsCompat.FORCE_DARK_ON)
            } else {
                val paint = Paint()
                val matrix = ColorMatrix()
                matrix.set(NEGATIVE_COLOR)
                val gcm = ColorMatrix()
                gcm.setSaturation(0f)
                val concat = ColorMatrix()
                concat.setConcat(matrix, gcm)
                val filter = ColorMatrixColorFilter(concat)
                paint.colorFilter = filter
                view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
            }
        } else {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(view.settings, WebSettingsCompat.FORCE_DARK_OFF)
            } else view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }
    private fun onStartDownloader(filename: String?, url: String?, userAgent: String?, mimetype: String?) {
        if (!hasPermission(storagePermission)) {
            downloadInfo[0] = filename
            downloadInfo[1] = url
            downloadInfo[2] = userAgent
            downloadInfo[3] = mimetype
            runtimeHandler.onRequestPermission(DOWNLOAD_REQUEST_PERMISSION, arrayOf(storagePermission), this)
            return
        }
        val mFilename = filename ?: getFilenameByUrl(url)
        try {
            if (url!!.startsWith("data:")) {
                val dataParser = getDataLink(url)
                val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mFilename)
                val fos = FileOutputStream(file)
                fos.write(dataParser.fileData)
                rescanPaths(arrayOf(file.absolutePath)) {
                    showToast("finished download file")
                }
                fos.close()
            } else if (mFilename.endsWith(".html") && !url.endLink.contains(".html")) {
                if (isCheckInternet()) {
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), mFilename)
                    ensureBackgroundThread {
                        val html = getDataHtml(url)
                        val stream = FileOutputStream(file)
                        stream.write(html.toByteArray())
                        stream.close()
                        runOnUiThread {
                            rescanPaths(arrayOf(file.absolutePath))
                            showToast("finished download file")
                        }
                    }
                }
            } else {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimetype)
                val cookies = CookieManager.getInstance().getCookie(url)
                if (cookies != null) CookieManager.getInstance().getCookie(url)
                if (userAgent != null) request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading file...")
                request.setTitle(mFilename)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, mFilename)
                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(applicationContext, "Downloading File", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(this@MainActivity)
                .setTitle("Can't Download URL")
                .setMessage(url)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
    private fun getDataHtml(url: String): String {
        var addresses: URL? = null
        try {
            addresses = URL(url)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        var br: BufferedReader? = null
        try {
            br = BufferedReader(InputStreamReader(addresses?.openStream()))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var input: String? = null
        val stringBuffer = StringBuffer()
        while (true) {
            try {
                if (br?.readLine().also { input = it } == null) break
            } catch (e: IOException) {
                e.printStackTrace()
            }
            stringBuffer.append(input)
        }
        try {
            br?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stringBuffer.toString()
    }
    private fun getFilenameByUrl(url: String?): String {
        if (url == null) return ""
        if (url.startsWith("data:")) {
            return try {
                getDataLink(url, false).filename ?: ""
            } catch (e: Exception) {
                ""
            }
        }
        var linkName = URLUtil.guessFileName(url, null, null)
        var extension = linkName.extension
        extension = extension.substringBeforeLast('?')
        extension = extension.substringBeforeLast('#')
        if (url.endLink.extension.isEmpty() && url.contains("/zip/")) {
            if (extension == "bin") extension = "zip"
        }
        linkName = linkName.substringBeforeLast('.') + "." + extension
        if (extension == "bin" || extension == "") {
            val host = if (extension != "") (Uri.parse(url)?.host ?: "") else ""
            linkName = host + "_${getCurrentDate()}.html"
        }
        return linkName
    }
    private fun getDataLink(url: String?, isFileDateCalc: Boolean = true): StartFileData {
        val map = StartFileData(null, null)
        if (!url.isNullOrEmpty()) {
            val data = url.substring(url.indexOf(",") + 1)
            val mimeType = url.substring(url.indexOf(":") + 1, url.indexOf(";"))
            val fileType = url.substring(url.indexOf(":") + 1, url.indexOf("/"))
            val suffix = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            map.filename = fileType + "_" + "${getCurrentDate()}.${suffix}"
            if (isFileDateCalc) map.fileData = Base64.decode(data, Base64.DEFAULT)
        }
        return map
    }
    private fun certificateToStr(certificate: SslCertificate?): String? {
        if (certificate == null) return null
        var s = ""
        val issuedTo = certificate.issuedTo
        if (issuedTo != null) s += "Issued to: " + issuedTo.dName + "\n"
        val issuedBy = certificate.issuedBy
        if (issuedBy != null) s += "Issued by: " + issuedBy.dName + "\n"
        val issueDate = certificate.validNotBeforeDate
        if (issueDate != null) {
            s += String.format(Locale.ROOT, "Issued on: %tF %tT %tz\n", issueDate, issueDate, issueDate)
        }
        val expiryDate = certificate.validNotAfterDate
        if (expiryDate != null) {
            s += String.format(Locale.ROOT, "Expires on: %tF %tT %tz", expiryDate, expiryDate, expiryDate)
        }
        return s
    }
    private fun getUrlByIntent(intent: Intent?): String? {
        if (intent != null) {
            if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
                return intent.dataString
            } else if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                return intent.getStringExtra(Intent.EXTRA_TEXT)
            } else if (intent.action == Intent.ACTION_WEB_SEARCH && intent.getStringExtra("query") != null) {
                return intent.getStringExtra("query")
            }
        }
        return null
    }
    private fun hideKeyboard(v: View?) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v?.windowToken ?: return, 0)
    }
    private fun onLoadUrl(webView: WebView, url: String?) {
        var mUrl = url?.trim() ?: ""
        if (mUrl.isEmpty()) mUrl = EMPTY_URL
        if (!URLUtil.isValidUrl(url)) {
            val validStart = mUrl.startsWith("about:") || mUrl.startsWith("javascript:") || mUrl.startsWith("file:")
                    || mUrl.startsWith("data:")
            mUrl = if (validStart || (mUrl.indexOf(' ') == -1 && Patterns.WEB_URL.matcher(mUrl).matches())) {
                val indexOfHash = mUrl.indexOf('#')
                val guess = URLUtil.guessUrl(url)
                if (indexOfHash != -1 && guess.indexOf('#') == -1) {
                    guess + mUrl.substring(indexOfHash)
                } else guess
            } else {
                URLUtil.composeSearchUrl(url, GOOGLE_SEARCH, "%s")
            }
        }
        webView.loadUrl(mUrl)
    }
    private fun isRtlLocale(): Boolean {
        val dir = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault())
        return dir == ViewCompat.LAYOUT_DIRECTION_RTL
    }
    private fun getLoadAppInfo(): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        return intent
    }
    companion object {
        internal var currentTabIndex: Int = 0
        const val EMPTY_URL = "about:blank"
        private const val GOOGLE_URL = "https://www.google.com/"
        const val GOOGLE_SEARCH = GOOGLE_URL + "search?q=%s"
        const val GOOGLE_COMPLETE = GOOGLE_URL + "complete/search?client=firefox&q=%s"
        private val NEGATIVE_COLOR = floatArrayOf(
            -1.0f, 0f, 0f, 0f, 255f, // red
            0f, -1.0f, 0f, 0f, 255f, // green
            0f, 0f, -1.0f, 0f, 255f, // blue
            0f, 0f, 0f, 1.0f, 0f // alpha
        )
        private const val MICROPHONE_REQUEST_CODE = 2
        private const val DOWNLOAD_REQUEST_PERMISSION = 22
        private const val INPUT_FILE_REQUEST_CODE = 1
        //[ 0 => permission_code, 1 => activity code]
        private val GEO_LOCATION_REQUEST_CODE = intArrayOf(33, 3)
    }
}