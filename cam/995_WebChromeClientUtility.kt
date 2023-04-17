package com.prongbang.awvpr

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.annotation.NonNull
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class WebChromeClientUtility(
		private val activity: FragmentActivity
) {

	private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
	private var mCameraPhotoPath: String? = null
	private var mPermissionRequest: PermissionRequest? = null

	fun getWebChromeClient(onFinished: () -> Unit): WebChromeClient {
		return object : WebChromeClient() {

			// For Android 5.0+
			@SuppressLint("NewApi")
			override fun onShowFileChooser(webView: WebView?,
			                               filePathCallback: ValueCallback<Array<Uri>>,
			                               fileChooserParams: FileChooserParams): Boolean {
				if (mFilePathCallback != null) {
					mFilePathCallback?.onReceiveValue(null)
				}
				mFilePathCallback = filePathCallback

				var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
				if (takePictureIntent?.resolveActivity(activity.packageManager) != null) {
					// Create the File where the photo should go
					var photoFile: File? = null
					try {
						photoFile = createImageFile()
						takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
					} catch (ex: IOException) {
						Log.e(TAG, "Unable to create Image File", ex)
					}

					// Continue only if the File was successfully created
					if (photoFile != null) {
						mCameraPhotoPath = "file:" + photoFile.absolutePath
						takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
					} else {
						takePictureIntent = null
					}
				}

				val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
				contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
				contentSelectionIntent.type = "image/*"

				val intentArray: Array<Intent> = takePictureIntent?.let { arrayOf(it) } ?: arrayOf()

				val chooserIntent = Intent(Intent.ACTION_CHOOSER)
				chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
				chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
				chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)

				activity.startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
				return true
			}

			@Throws(IOException::class)
			private fun createImageFile(): File? {
				val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(
						Date())
				val imageFileName = "JPEG_" + timeStamp + "_"
				val storageDir: File = Environment.getExternalStoragePublicDirectory(
						Environment.DIRECTORY_PICTURES)
				return File.createTempFile(imageFileName, ".jpg", storageDir)
			}

			override fun onProgressChanged(view: WebView?, newProgress: Int) {
				super.onProgressChanged(view, newProgress)
				if (newProgress >= 100) {
					onFinished.invoke()
				}
			}

			override fun onPermissionRequest(request: PermissionRequest) {
				mPermissionRequest = request
				val requestedResources = request.resources
				for (r in requestedResources) {
					if (r == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
						ConfirmationDialogFragment
								.newInstance(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
								.setConfirmation(::onConfirmation)
								.show(activity.supportFragmentManager, FRAGMENT_DIALOG)
						break
					}
					Log.d(TAG, r)
				}
			}

			override fun onPermissionRequestCanceled(request: PermissionRequest) {
				mPermissionRequest = null
				val fragment = activity.supportFragmentManager.findFragmentByTag(
						FRAGMENT_DIALOG) as? DialogFragment
				fragment?.dismiss()
			}

			override fun onConsoleMessage(@NonNull message: ConsoleMessage): Boolean {
				Log.d(MainActivity::class.java.simpleName, message.message())
				return true
			}
		}
	}

	private fun onConfirmation(allowed: Boolean, resources: Array<String>) {
		if (allowed) {
			mPermissionRequest?.grant(resources)
			Log.d(TAG, "Permission granted.")
		} else {
			mPermissionRequest?.deny()
			Log.d(TAG, "Permission request denied.")
		}
		mPermissionRequest = null
	}

	fun isNotMatchRequestCode(requestCode: Int) =
			requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null

	fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		var results: Array<Uri>? = null

		// Check that the response is a good one
		if (resultCode == Activity.RESULT_OK) {
			if (data == null) {
				// If there is not data, then we may have taken a photo
				if (mCameraPhotoPath != null) {
					results = arrayOf(Uri.parse(mCameraPhotoPath))
				}
			} else {
				val dataString = data.dataString
				if (dataString != null) {
					results = arrayOf(Uri.parse(dataString))
				}
			}
		}
		mFilePathCallback?.onReceiveValue(results)
		mFilePathCallback = null
		return
	}

	companion object {
		private val TAG = WebChromeClientUtility::class.java.simpleName
		private const val FRAGMENT_DIALOG = "dialog"
		private const val REQUEST_SELECT_FILE = 999
		private const val INPUT_FILE_REQUEST_CODE = 999
	}
}