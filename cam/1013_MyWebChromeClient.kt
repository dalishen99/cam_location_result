package com.sirius.driverlicense.utils.webview

import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.*
import android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE
import androidx.appcompat.app.AppCompatActivity
import com.sirius.driverlicense.utils.PermissionHelper

class MyWebChromeClient(val activity: AppCompatActivity) : WebChromeClient() {

    override fun onHideCustomView() {
        Log.d("mylog200","onHideCustomView=")
        super.onHideCustomView()
    }

    override fun onCreateWindow(
        view: WebView?,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message?
    ): Boolean {
        Log.d("mylog200","onCreateWindow=")
        return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        Log.d("mylog200","onShowFileChooser=")
        return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    override fun onJsPrompt(
        view: WebView?,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult?
    ): Boolean {
        Log.d("mylog200","onJsPrompt="+message)
        return super.onJsPrompt(view, url, message, defaultValue, result)
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        Log.d("mylog200","onJsAlert="+message)
        return super.onJsAlert(view, url, message, result)
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        Log.d("mylog200","onJsConfirm="+message)
        return super.onJsConfirm(view, url, message, result)
    }


    override fun onPermissionRequest(request: PermissionRequest?) {
        Log.d("mylog200","onPermissionRequest="+request)
        Log.d("mylog200","onPermissionRequest="+request?.origin)
        try{
            request?.grant(request.getResources())
        }catch (e : Exception){
            e.printStackTrace()
        }
        request?.resources?.forEach {
            if(RESOURCE_VIDEO_CAPTURE == it){
                PermissionHelper.checkPermissionsForCamera(activity,1009)
            }
            Log.d("mylog200","onPermissionRequest res="+it)
        }

        //Log.d("mylog200","onPermissionRequest="+request?.grant(request?.resources))
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        Log.d("mylog200","onShowCustomView=")
        super.onShowCustomView(view, callback)
    }
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Log.d("mylog200","consoleMessage="+consoleMessage)
        return super.onConsoleMessage(consoleMessage)
    }
}