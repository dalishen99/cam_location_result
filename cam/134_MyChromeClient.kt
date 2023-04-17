package io.ablespace.androidapp.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.*

interface FileHandler {
    fun onShowFileChooser(intent: Intent?)
}

class MyChromeClient(private val fileHandler: FileHandler): WebChromeClient() {

    var uploads: ValueCallback<Array<Uri>>? = null

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        // cancel request, make sure there is no existing message
        uploads?.onReceiveValue(null)
        uploads = null
        uploads = filePathCallback

        //intent 2
//        val i = Intent(Intent.ACTION_GET_CONTENT)
//        i.addCategory(Intent.CATEGORY_OPENABLE)
//        i.type = "*/*" // set MIME type to filter
        // Intent.createChooser(i, "File Chooser")

        val intent = fileChooserParams?.createIntent()
        try {
            fileHandler.onShowFileChooser(intent)
        } catch (e: ActivityNotFoundException) {
            uploads = null
            //error toast
            return false
        }
        return true
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.grant(request.resources)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Log.d("durga", "console: ${consoleMessage?.message()}")
        return true
    }
}