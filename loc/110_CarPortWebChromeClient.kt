package com.farkhodkhaknazarov.carport.interfaceComponents

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import ru.carport.app.MainActivity
import ru.carport.app.presenters.WebViewPresenter
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


class CarPortWebChromeClient() : WebChromeClient() {

    @Inject
    lateinit var mainActivity: MainActivity

    lateinit var webViewPresenter: WebViewPresenter

    var mCameraPhotoPath: String? = null

    override fun onShowFileChooser(
        mWebView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ): Boolean {

        webViewPresenter.uploadMessage?.onReceiveValue(null)
        webViewPresenter.uploadMessage = null
        webViewPresenter.uploadMessage = filePathCallback

        var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent!!.resolveActivity(mainActivity.getPackageManager()) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
                takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
            } catch (ex: IOException) {
                val g = 0
            }

            if (photoFile != null) {
                mCameraPhotoPath = "file:" + photoFile.absolutePath
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
            } else {
                takePictureIntent = null
            }
        }

        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        contentSelectionIntent.type = "image/*"

        val intentArray: Array<Intent>
        if (takePictureIntent != null) {
            intentArray = arrayOf(takePictureIntent)
        } else {
            intentArray = arrayOf<Intent>()
        }

        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        mainActivity.startActivityForResult(Intent.createChooser(chooserIntent, "Select images"), 1)

        return true
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {

        @SuppressLint("SimpleDateFormat") val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        callback?.invoke(origin, true, false);
    }

}