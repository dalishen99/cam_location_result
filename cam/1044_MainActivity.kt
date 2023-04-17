package com.example.webcam

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.webcam.databinding.ActivityMainBinding

private const val RC_CAMERA = 100
private const val RC_RECORD_AUDIO = 101

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var mRequest: PermissionRequest? = null
    private var cameraWhiteList = arrayListOf<String>()
    private var micWhiteList = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding.webView) {
            webChromeClient = setChromeClient()
            webViewClient = WebViewClient()
            configureWebSettings(settings)
            loadUrl("https://www.google.com/search?q=test+webcam")
        }

        binding.btnTestMic.setOnClickListener {
            binding.webView.loadUrl("https://www.google.com/search?q=test+mic")
        }

        binding.btnTestWebcam.setOnClickListener {
            binding.webView.loadUrl("https://www.google.com/search?q=test+webcam")
        }

        /*if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), RC_CAMERA)
        }*/

    }

    private fun configureWebSettings(settings: WebSettings) {
        settings.javaScriptEnabled = true
        /*settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_NO_CACHE*/
    }

    private fun setChromeClient() = object : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest?) {
            for (r in request!!.resources) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE == r) {
                    mRequest = request
                    //mRequest!!.grant(mRequest!!.resources)
                    askForPermission(
                        arrayOf(Manifest.permission.CAMERA),
                        "Camera",
                        RC_CAMERA,
                        cameraWhiteList
                    )
                }

                if (PermissionRequest.RESOURCE_AUDIO_CAPTURE == r) {
                    mRequest = request
                    //mRequest!!.grant(mRequest!!.resources)
                    askForPermission(
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        "Microphone",
                        RC_RECORD_AUDIO,
                        micWhiteList
                    )
                }
            }
        }
    }

    private fun askForPermission(
        permission: Array<String>,
        hardware: String,
        requestCode: Int,
        whiteList: ArrayList<String>
    ) {
        if (whiteList.contains(mRequest!!.origin.toString()))
            requestPermissions(
                permission, requestCode
            )
        else {
            val builder = AlertDialog.Builder(this@MainActivity).setTitle("$hardware permission")
                .setMessage("${mRequest!!.origin} wants to access ${hardware.lowercase()}")
                .setPositiveButton("Allow") { i, which ->
                    requestPermissions(
                        permission, requestCode
                    )
                }.setNegativeButton("Deny") { i, which ->
                    mRequest!!.deny()
                    i.dismiss()
                }
            builder.create().show()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        when (requestCode) {
            RC_CAMERA -> {
                if (isResultsGranted(grantResults) && mRequest != null) {
                    mRequest!!.grant(mRequest!!.resources)
                    cameraWhiteList.add(mRequest!!.origin.toString())
                }
            }

            RC_RECORD_AUDIO -> {
                if (isResultsGranted(grantResults) && mRequest != null) {
                    mRequest!!.grant(mRequest!!.resources)
                    micWhiteList.add(mRequest!!.origin.toString())
                }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun isResultsGranted(grantResults: IntArray): Boolean {
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) return false
        }
        return true
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) binding.webView.goBack()
        else super.onBackPressed()
    }
}