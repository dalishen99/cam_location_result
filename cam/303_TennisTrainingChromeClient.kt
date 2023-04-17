package com.example.fitnesstraining

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity

class TennisTrainingChromeClient(private val context: FragmentActivity, private var valueCallback: ValueCallback<Array<Uri>?>?, private var register: ActivityResultLauncher<Intent>) : WebChromeClient() {

    // Добавляем доступ к камере и микрофону из вебвью
    override fun onPermissionRequest(request: PermissionRequest?) {
        if (
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context, arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ), 102
            )
        } else {
            request?.grant(request.resources)
        }
    }

    // Добавляем доступ к файлам из вебвью
    override fun onShowFileChooser(
        vw: WebView?, filePathCallback: ValueCallback<Array<Uri>?>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        valueCallback?.onReceiveValue(null)
        valueCallback = filePathCallback

        val i = Intent(Intent.ACTION_GET_CONTENT).also { intent ->
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
        }

        register.launch(Intent.createChooser(i, "File"))
//        context.startActivityForResult(Intent.createChooser(i, "File"), 12)

        return true
    }
}