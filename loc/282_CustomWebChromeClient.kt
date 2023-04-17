package id.go.dephub.itjen.skemaraja

import android.app.Activity
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient

class CustomWebChromeClient(activity: Activity) : WebChromeClient() {
    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        callback?.invoke(origin, true, false)
    }
}