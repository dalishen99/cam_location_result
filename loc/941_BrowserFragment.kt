package com.browser.webgram.ui.browser

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.view.inputmethod.EditorInfo
import android.webkit.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.browser.webgram.R
import com.browser.webgram.adblock.AdBlocker.blocklist
import com.browser.webgram.adblock.AdBlocker.loadBlockList
import com.browser.webgram.databinding.FragmentBrowserBinding
import com.browser.webgram.utils.Internet.isNetworkAvailable
import com.browser.webgram.utils.Keyboard.hideKeyboard
import com.browser.webgram.database.models.AppNote
import com.browser.webgram.utils.Keyboard.afterTextChangedFlow
import com.browser.webgram.utils.Toast.showMessage
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

class BrowserFragment : Fragment() {

    private var _binding: FragmentBrowserBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: BrowserFragmentViewModel
    var uploadMessage: ValueCallback<Array<Uri>>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        getDataAdBlock()
        setWebBrowserSettings()
        getUrl()
        checkInternet()
    }

    private fun getDataAdBlock() {
        ADBLOCK = arguments?.getInt(key_adblock)
        if (ADBLOCK == 2) {
            loadBlockList(requireActivity(), R.raw.adblockserverlist)
        } else {
            loadBlockList(requireActivity(), R.raw.adblocklistofnull)
        }
    }

    private fun getUrl() {
        URL = arguments?.getString(key_url)
        if (
            URL!!.contains("http://") || URL!!.startsWith("https://")
        ) {
            binding.webBrowser.loadUrl(URL!!)
        } else {
            binding.webBrowser.loadUrl("https://www.google.com/search?q=$URL")
        }
    }

    private fun init() {
        viewModel = ViewModelProvider(this)[BrowserFragmentViewModel::class.java]
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        binding.imageToShare.setOnClickListener {
            shareUrl()
        }

        binding.editTextToSearchBrowser.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                searchOrLoad()
                true
            } else {
                searchOrLoad()
                false
            }
        }

        binding.imageToBackWebView.setOnClickListener {
            if (binding.webBrowser.canGoBack())
                binding.webBrowser.goBack()
        }

        binding.imageToMain.setOnClickListener {
            findNavController().navigate(R.id.action_browserFragment_to_mainFragment)
        }

        binding.imageToForwardWebView.setOnClickListener {
            if (binding.webBrowser.canGoForward())
                binding.webBrowser.goForward()
        }

        binding.imageToClearText.setOnClickListener {
            binding.editTextToSearchBrowser.text.clear()
        }

        lifecycleScope.launch {
            binding.editTextToSearchBrowser.afterTextChangedFlow().collect {
                binding.imageToClearText.visibility = View.GONE
            }
        }

        binding.editTextToSearchBrowser.setOnClickListener {
            binding.imageToClearText.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setWebBrowserSettings() {
        binding.webBrowser.settings.javaScriptEnabled = true
        binding.webBrowser.settings.loadWithOverviewMode = true
        binding.webBrowser.settings.useWideViewPort = true
        binding.webBrowser.settings.domStorageEnabled = true
        binding.webBrowser.settings.databaseEnabled = true
        binding.webBrowser.settings.allowFileAccess = true
        binding.webBrowser.settings.allowContentAccess = true
        binding.webBrowser.settings.javaScriptCanOpenWindowsAutomatically = true
        binding.webBrowser.settings.allowFileAccess
        binding.webBrowser.settings.builtInZoomControls = true
        binding.webBrowser.settings.displayZoomControls = false
        binding.webBrowser.settings.setSupportZoom(true)
        binding.webBrowser.settings.setGeolocationEnabled(true)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webBrowser, true)
        binding.webBrowser.webChromeClient = MyWebChromeClient()
        webViewClient()
        otherSettings()

        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            0
        )
    }

    private fun otherSettings() {
        binding.webBrowser.setOnKeyListener(
            object : View.OnKeyListener {
                override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        val webView = v as WebView
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            if (webView.canGoBack()) {
                                webView.goBack()
                                return true
                            }
                        }
                    }
                    return false
                }
            })

        binding.webBrowser.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val filename = URLUtil.guessFileName(url, contentDisposition, mimetype)
            val builder = AlertDialog.Builder(requireActivity())
            builder.setTitle(getString(R.string.download))
            builder.setMessage(getString(R.string.want_to_save) + filename)
            builder.setPositiveButton(getString(R.string.yes)) { dialog, which ->
                val request = DownloadManager.Request(Uri.parse(url))
                val cookie = CookieManager.getInstance().getCookie(url)
                request.addRequestHeader("Cookie", cookie)
                request.addRequestHeader("User-Agent", userAgent)
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                val downloadable =
                    requireActivity().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
                downloadable.enqueue(request)
            }
            builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
                dialog.cancel()
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    internal inner class MyWebChromeClient : WebChromeClient() {
        override fun onGeolocationPermissionsShowPrompt(
            origin: String?,
            callback: GeolocationPermissions.Callback
        ) {
            callback.invoke(origin, true, false)
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onShowFileChooser(
            mWebView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            if (uploadMessage != null) {
                uploadMessage!!.onReceiveValue(null)
                uploadMessage = null
            }

            uploadMessage = filePathCallback

            val intent = fileChooserParams.createIntent()
            try {
                launchSomeActivity.launch(intent)
            } catch (e: Exception) {
                uploadMessage = null
                showMessage(requireActivity(), getString(R.string.cnt_opn_choose))
                return false
            }

            return true
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            binding.progressBar.progress = newProgress
            super.onProgressChanged(view, newProgress)

            if (newProgress == whole_progress) {
                binding.progressBar.visibility = View.GONE
            }
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
            super.onReceivedIcon(view, icon)
            binding.imageFavicon.setImageBitmap(icon)
        }
    }

    private fun webViewClient() {
        binding.webBrowser.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val EMPTY3 = ByteArrayInputStream("".toByteArray())
                val kk53: String = blocklist.toString() //Load blocklist
                return if (kk53.contains(":::::" + request!!.url.host)) { // If blocklist equals url = Block
                    WebResourceResponse("text/plain", "utf-8", EMPTY3) //Block
                } else super.shouldInterceptRequest(view, request)
            }

            @SuppressLint("WebViewClientOnReceivedSslError")
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.proceed()
            }

            @SuppressLint("QueryPermissionsNeeded")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                if (URLUtil.isNetworkUrl(url)) {
                    return false
                }
                try {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    showMessage(requireActivity(), getString(R.string.no_app_found))
                }
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.visibility = View.VISIBLE

                if ("https://www.google.com/" != url) {
                    binding.editTextToSearchBrowser.setText(url)
                } else {
                    binding.editTextToSearchBrowser.text.clear()
                }

                if (binding.webBrowser.canGoForward()) {
                    binding.imageToForwardWebView.setImageResource(R.drawable.ic_back_forward)
                } else {
                    binding.imageToForwardWebView.setImageResource(R.drawable.ic_right)
                }

                if (binding.webBrowser.canGoBack()) {
                    binding.imageToBackWebView.setImageResource(R.drawable.ic_back_pressed)
                } else {
                    binding.imageToBackWebView.setImageResource(R.drawable.ic_left)
                }

                checkInternet()

                val title: String = view?.title.toString()
                viewModel.insert(AppNote(name = title, text = url.toString()))

                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                URL = url
                super.onPageFinished(view, url)
            }
        }
    }

    var launchSomeActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                when {
                    uploadMessage != null -> {
                        uploadMessage?.onReceiveValue(
                            WebChromeClient.FileChooserParams.parseResult(
                                result.resultCode,
                                data
                            )
                        )
                        uploadMessage = null
                    }
                }
            }
        }

    private fun searchOrLoad() {
        val text = binding.editTextToSearchBrowser.text.toString()
        if (binding.editTextToSearchBrowser.text.toString() != "")
            if (text.contains("http://") || text.contains("https://")) {
                binding.webBrowser.loadUrl(text)
            } else {
                binding.webBrowser.loadUrl("https://www.google.com/search?q=$text")
            }
        hideKeyboard(binding.editTextToSearchBrowser)
    }

    private fun shareUrl() {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, URL)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "URL")
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)))
    }

    private fun checkInternet() {
        if (isNetworkAvailable(requireActivity())) {
            binding.webBrowser.visibility = View.VISIBLE
        } else {
            binding.webBrowser.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        binding.webBrowser.onPause()
        binding.webBrowser.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        binding.webBrowser.onResume()
        binding.webBrowser.resumeTimers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.webBrowser.destroy()
        _binding = null
    }

    companion object {
        const val whole_progress: Int = 100
        var URL: String? = null
        var ADBLOCK: Int? = null
        const val key_url = "amount"
        const val key_adblock = "adblock"
    }
}
