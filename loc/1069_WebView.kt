package com.aksoyhakn.reportplus.extensions

import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.databinding.BindingAdapter


/**
 * Created by hakanaksoy on 12.09.2022.
 * NargileyeFısıldayaAdam
 */


@BindingAdapter("loadHtml", "setTitle")
fun WebView.loadHtml(
    description: String?,
    title: String?
) {
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    isScrollContainer = false
    settings.javaScriptEnabled = true
    settings.loadWithOverviewMode = true
    settings.useWideViewPort = true
    settings.builtInZoomControls = false
    settings.defaultFontSize = 45

    webChromeClient = object : WebChromeClient() {
        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback
        ) {
            callback.invoke(origin, true, false)
        }
    }
    /*webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            Utils.checkLink(url, view?.context as BaseSlideActivity, title)
            return true
        }
    }
    webChromeClient = WebChromeClient()
    description?.let {
        if (it.contains("font-family")) {
            loadDataWithBaseURL(
                null,
                description,
                "text/html",
                "utf-8",
                null
            )
        } else {
            loadDataWithBaseURL(
                "file:///android_asset/",
                getStyledFont(it),
                "text/html; charset=UTF-8",
                null,
                "about:blank"
            )
        }
    }
    }*/
}