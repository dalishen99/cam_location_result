package com.apro.cereal.ui.hybrid

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ImageButton
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.apro.cereal.R
import com.apro.cereal.common.Constants
import com.apro.cereal.extensions.isInstalled
import com.apro.cereal.net.ServiceConfig
import com.apro.cereal.ui.dialogs.CustomAlertDialog
import timber.log.Timber


class HybridCengoldFragment : Fragment() {

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private lateinit var hybridView: HybridView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.hybrid_cengold_frag, container, false)

        with(root) {
            findViewById<ImageButton>(R.id.backButton).also {
                it.setOnClickListener {
                    onBackPressed()
                }
            }

            hybridView = findViewById<HybridView>(R.id.hybridView)
                .apply {
                    if (!arguments?.getString(ARGUMENT_URL).isNullOrEmpty()) {
                        loadUrl(arguments?.getString(ARGUMENT_URL)!!)
                    }

                    webChromeClient = object : WebChromeClient() {

                        override fun onJsAlert(
                            view: WebView?,
                            url: String,
                            message: String,
                            result: JsResult
                        ): Boolean {
                            Timber.d("onJsAlert($url, $message, $result)")
                            CustomAlertDialog.show(
                                context,
                                message,
                                context.getString(R.string.confirm),
                                { dialogInterface: DialogInterface, _: Int ->
                                    result.confirm()
                                    dialogInterface.dismiss()
                                },
                                null,
                                null,
                                { dialogInterface: DialogInterface, _: Int ->
                                    result.cancel()
                                    dialogInterface.dismiss()
                                },
                                false
                            )
                            return true
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            return if (Constants.IS_DEBUG) super.onConsoleMessage(consoleMessage) else true
                        }

                        override fun onJsConfirm(
                            view: WebView?,
                            url: String,
                            message: String,
                            result: JsResult
                        ): Boolean {
                            CustomAlertDialog.show(
                                context,
                                message,
                                context.getString(R.string.confirm),
                                { dialogInterface: DialogInterface, i: Int ->
                                    result.confirm()

                                    if (url.toUri().getQueryParameter("serviceCode") == "OKINV") {
                                        // 채우기로 이동
                                        moveToRecharge()
                                    }

                                    dialogInterface.dismiss()
                                },
                                context.getString(R.string.cancel),
                                { dialogInterface: DialogInterface, _: Int ->
                                    result.cancel()
                                    dialogInterface.dismiss()
                                },
                                { dialogInterface: DialogInterface, _: Int ->
                                    result.cancel()
                                    dialogInterface.dismiss()
                                },
                                false
                            )
                            return true
                        }

                        override fun onJsPrompt(
                            view: WebView?,
                            url: String?,
                            message: String?,
                            defaultValue: String?,
                            result: JsPromptResult
                        ): Boolean {
                            result.cancel()
                            return true
                        }

                        override fun onGeolocationPermissionsShowPrompt(
                            origin: String?,
                            callback: GeolocationPermissions.Callback
                        ) {
                            callback.invoke(origin, true, false)
                        }
                    }

                    webViewClient = object : WebViewClient() {

                        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {

                            val url = request?.url
                            if (url?.scheme == "cengold") {
                                val packageName = "com.cengold.korda"
                                val intent =
                                    if (context.isInstalled(packageName)) {
                                        Intent(Intent.ACTION_VIEW, url)
                                    } else {
                                        Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("market://details?id=$packageName")
                                        }
                                    }
                                startActivity(intent)
                                return true
                            }

                            if (url?.host?.startsWith("play.google.com") == true) {
                                return true
                            }

                            return super.shouldOverrideUrlLoading(view, request)
                        }
                    }
                }
        }

        return root
    }

    private fun moveToRecharge() {
        val action =
            HybridCengoldFragmentDirections.actionCengoldFragmentToMypRechargeFragment(url = "${ServiceConfig.URL_MYP_RECHARGE}?param=cengold")
        findNavController().navigate(action)
    }

    private fun onBackPressed() {
        Timber.d("argument url: ${arguments?.getString(ARGUMENT_URL)}")
        Timber.d("url: ${hybridView.url}")
        if (!hybridView.canGoBack() || hybridView.url?.contains("main") == true) {
            finish()
        } else {
            hybridView.goBack()
        }
    }

    private fun finish() {
        activity?.finish()
    }

    companion object {
        const val ARGUMENT_URL = "url"

        fun newInstance(url: String) = HybridCengoldFragment().apply {
            arguments = bundleOf(
                ARGUMENT_URL to url
            )
        }
    }
}