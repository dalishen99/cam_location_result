package com.apro.cereal.ui.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.apro.cereal.R
import com.apro.cereal.common.Constants
import com.apro.cereal.ui.hybrid.HybridView
import com.apro.cereal.ui.dialogs.CustomAlertDialog

class ChangeNicknameFragment : Fragment() {

    val args: ChangeNicknameFragmentArgs by navArgs()
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private lateinit var hybridView: HybridView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {}
        }

        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.webview_frag, container, false)

        with(root) {
            hybridView = findViewById<HybridView>(R.id.hybridView)
                .apply {

                    webChromeClient = object : WebChromeClient() {

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
                                { dialogInterface: DialogInterface, _: Int ->
                                    result.confirm()
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
                }
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (args.url.isNotEmpty()) {
            hybridView.loadUrl(Constants.SERVER_URL + args.url)
        }
    }
}