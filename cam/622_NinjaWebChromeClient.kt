package web.browser.dragon.utils.other.browser

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.preference.PreferenceManager
import android.view.View
import android.webkit.*
import android.webkit.WebView.WebViewTransport
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import web.browser.dragon.R
import web.browser.dragon.utils.other.unit.BrowserUnit
import web.browser.dragon.utils.other.unit.HelperUnit
import web.browser.dragon.utils.other.view.NinjaWebView
import java.util.*

class NinjaWebChromeClient(ninjaWebView: NinjaWebView) : WebChromeClient() {

    private val ninjaWebView: NinjaWebView

    override fun onProgressChanged(view: WebView, progress: Int) {
        super.onProgressChanged(view, progress)
        ninjaWebView.updateTitle(progress)
        if (Objects.requireNonNull(view.title)?.isEmpty() == true) {
            ninjaWebView.updateTitle(view.url)
        } else {
            ninjaWebView.updateTitle(view.title)
        }
        ninjaWebView.updateFavicon(view.url)
    }

    override fun onCreateWindow(
        view: WebView,
        dialog: Boolean,
        userGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        val context = view.context
        val newWebView = NinjaWebView(context)
        view.addView(newWebView)
        val transport = resultMsg.obj as WebViewTransport
        transport.webView = newWebView
        resultMsg.sendToTarget()
        newWebView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                newWebView.initPreferences(request.url.toString())
                BrowserUnit.intentURL(context, request.url)
                return true
            }
        })
        return true
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        ninjaWebView.getBrowserController()?.onShowCustomView(view, callback)
        super.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        ninjaWebView.getBrowserController()?.onHideCustomView()
        super.onHideCustomView()
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        ninjaWebView.getBrowserController()?.showFileChooser(filePathCallback)
        return true
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback
    ) {
        val activity = ninjaWebView.getContext() as Activity
        HelperUnit.grantPermissionsLoc(activity)
        callback.invoke(origin, true, false)
        super.onGeolocationPermissionsShowPrompt(origin, callback)
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        val sp: SharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(ninjaWebView.getContext())
        val activity = ninjaWebView.getContext() as Activity
        val resources = request.resources
        for (resource in resources) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE == resource) {
                if (sp.getBoolean(ninjaWebView.profile.toString() + "_camera", false)) {
                    HelperUnit.grantPermissionsCamera(activity)
                    if (ninjaWebView.getSettings().getMediaPlaybackRequiresUserGesture()) {
                        ninjaWebView.getSettings()
                            .setMediaPlaybackRequiresUserGesture(false) //fix conflict with save data option. Temporarily switch off setMediaPlaybackRequiresUserGesture
                        ninjaWebView.reloadWithoutInit()
                    }
                    request.grant(request.resources)
                }
            } else if (PermissionRequest.RESOURCE_AUDIO_CAPTURE == resource) {
                if (sp.getBoolean(ninjaWebView.profile.toString() + "_microphone", false)) {
                    HelperUnit.grantPermissionsMic(activity)
                    request.grant(request.resources)
                }
            } else if (PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID == resource) {
                val builder = MaterialAlertDialogBuilder(ninjaWebView.getContext())
                builder.setIcon(R.drawable.icon_alert)
                builder.setTitle(R.string.app_warning)
                builder.setMessage(R.string.hint_DRM_Media)
                builder.setPositiveButton(R.string.app_ok) { dialog, whichButton ->
                    request.grant(
                        request.resources
                    )
                }
                builder.setNegativeButton(R.string.app_cancel) { dialog, whichButton -> request.deny() }
                val dialog = builder.create()
                dialog.show()
                HelperUnit.setupDialog(ninjaWebView.getContext(), dialog)
            }
        }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        ninjaWebView.setFavicon(icon)
        super.onReceivedIcon(view, icon)
    }

    override fun onReceivedTitle(view: WebView, sTitle: String) {
        super.onReceivedTitle(view, sTitle)
    }

    init {
        this.ninjaWebView = ninjaWebView
    }
}