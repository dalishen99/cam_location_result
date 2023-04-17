package com.katahiromz.pillbugmaze

import android.Manifest
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.PermissionChecker
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

const val MY_WEBVIEW_REQUEST_CODE_01 = 999
// TODO: Add more request

class MyWebChromeClient(private val activity: AppCompatActivity, private val listener: Listener) :
    WebChromeClient() {

    interface Listener {
        fun onChromePermissionRequest(permissions: Array<String>, requestCode: Int)
        fun onShowToast(text: String, typeOfToast: Int)
        fun onShowSnackbar(text: String, typeOfSnack: Int)
        fun onChromeProgressChanged(view: WebView?, newProgress: Int)
    }

    private fun getResString(resId: Int): String {
        return activity.getString(resId)
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        listener.onChromeProgressChanged(view, newProgress)
    }

    /////////////////////////////////////////////////////////////////////
    // Permissions-related

    override fun onPermissionRequest(request: PermissionRequest?) {
        if (false) {
            // Audio record request
            val audioCheck =
                    PermissionChecker.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
            when (audioCheck) {
                PermissionChecker.PERMISSION_GRANTED,
                PermissionChecker.PERMISSION_DENIED_APP_OP -> {
                    request?.grant(request.resources)
                }
                PermissionChecker.PERMISSION_DENIED -> {
                    val audioRational =
                            shouldShowRequestPermissionRationale(
                                    activity, Manifest.permission.RECORD_AUDIO)
                    if (audioRational) {
                        listener.onChromePermissionRequest(
                                arrayOf(Manifest.permission.RECORD_AUDIO),
                                MY_WEBVIEW_REQUEST_CODE_01)
                    }
                }
                else -> {
                    require(false, { "PermissionChecker" })
                }
            }
        }
        // TODO: Add more request
    }

    /////////////////////////////////////////////////////////////////////
    // JavaScript interface-related

    // Wrap JavaScript alert function
    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        // MaterialAlertDialogを使用して実装する。
        val title = getResString(R.string.app_name)
        val ok_text = getResString(R.string.ok)
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(ok_text) { _, _ ->
                result?.confirm()
            }
            .setCancelable(false)
            .show()
        return true
    }

    // Wrap JavaScript confirm function
    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        // MaterialAlertDialogを使用して実装する。
        val title = getResString(R.string.app_name)
        val ok_text = getResString(R.string.ok)
        val cancel_text = getResString(R.string.cancel)
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(ok_text) { _, _ ->
                result?.confirm()
            }
            .setNegativeButton(cancel_text) { _, _ ->
                result?.cancel()
            }
            .setCancelable(false)
            .show()
        return true
    }

    private var modalDialog: MaterialDialog? = null
    fun onResume() {
        if (modalDialog != null)
            modalDialog?.show()
    }

    // Wrap JavaScript prompt function
    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        val title = getResString(R.string.app_name)
        var inputtedText: String? = null
        modalDialog = MaterialDialog(activity).show {
            title(text = title)
            message(text = message)
            input(hint = getResString(R.string.prompt_hint), prefill = defaultValue) { _, text ->
                inputtedText = text.toString()
            }
            positiveButton(text = getResString(R.string.ok)) {
                result?.confirm(inputtedText ?: "")
                modalDialog = null
            }
            negativeButton(text = getResString(R.string.cancel)) {
                result?.cancel()
                modalDialog = null
            }
            cancelable(false)
            cancelOnTouchOutside(false)
            lifecycleOwner(activity)
        }
        return true
    }

    // JavaScriptのコンソール出力を制御する。
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (BuildConfig.DEBUG) {
            if (consoleMessage != null) {
                val msg = consoleMessage.message()
                val line = consoleMessage.lineNumber()
                val src = consoleMessage.sourceId()
                Timber.d("console: $msg at Line $line of $src")
            }
        }
        return super.onConsoleMessage(consoleMessage)
    }
}
