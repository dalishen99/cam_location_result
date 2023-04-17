package com.neat.offsbrasil.uploader

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.fragment.app.FragmentActivity
import com.google.accompanist.web.AccompanistWebChromeClient

private const val REQUEST_CODE = 1999


class UploaderWrapper(private val activity: Activity) {

	private var mUploadMessage: ValueCallback<Array<Uri>>? = null

	fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
		if (requestCode == REQUEST_CODE) {
			if (null == mUploadMessage) return
			val result =
				if (intent == null || resultCode != FragmentActivity.RESULT_OK) null else intent.data
			mUploadMessage!!.onReceiveValue(arrayOf(result!!))
			mUploadMessage = null
		}
	}

	val webChromeClient: AccompanistWebChromeClient
		get() = object : AccompanistWebChromeClient() {

			override fun onPermissionRequest(request: PermissionRequest) {
				request.grant(request.resources)
			}

			override fun onShowFileChooser(
				webView: WebView?,
				filePathCallback: ValueCallback<Array<Uri>>?,
				fileChooserParams: FileChooserParams?
			): Boolean {
				try {
					mUploadMessage?.let {
						it.onReceiveValue(null)
						mUploadMessage = null
					}

					mUploadMessage = filePathCallback

					fileChooserParams?.let {
						activity.startActivityForResult(
							fileChooserParams.createIntent().apply {
								type = "audio/*"
							},
							REQUEST_CODE
						)
					}
				} catch (e: ActivityNotFoundException) {
					Log.e("ChromeClient Error", e.toString())
				}

				return true
			}
		}
}