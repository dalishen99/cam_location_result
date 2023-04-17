package es.plainconcepts.webviewtest

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var webView : WebView
    private val webcamTestsURL = "https://webcamtests.com"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Permissions
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 1)
        }

        webView = findViewById(R.id.webview)

        // At least for webcamtests.com, JS is mandatory
        webView.settings.javaScriptEnabled = true

        // Set WebViewClient() doesn't seem to be needed

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {

                //super.onPermissionRequest(request) // This is denying them!!

                if (request == null) {
                    // TODO log it
                    return
                }
                val requestedResources = request.resources
                for (resourceRequest in requestedResources) {
                    if (resourceRequest.equals((PermissionRequest.RESOURCE_VIDEO_CAPTURE))) {
                        request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    } else {
                      // TODO warn!!
                    }
                }
            }
        }

        webView.loadUrl(webcamTestsURL)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Camera permission --NOT-- granted", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                webView.reload()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}