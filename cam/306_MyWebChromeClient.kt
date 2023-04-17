package com.yuanquan.common.ui.webview

import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient

open class MyWebChromeClient: WebChromeClient() {
    override fun onPermissionRequest(request: PermissionRequest) {
        Log.e("VideoChat", "onPermissionRequest")
        request.grant(request.resources)
        request.origin
    }
}