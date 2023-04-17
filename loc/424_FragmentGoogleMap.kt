package ph.sanpablocitygov.iSanPablo

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient


class FragmentGoogleMap :
    Fragment() {


        @SuppressLint("SetJavaScriptEnabled")
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

            val view: View = inflater.inflate(
                R.layout.fragment_fragment_google_map, container,
                false)


            val  mWebView = view.findViewById(R.id.web_googlemap) as WebView
            mWebView.loadUrl("https://www.google.com/maps/")

            val webSettings = mWebView.settings
            webSettings.javaScriptEnabled = true

// Force links and redirects to open in the WebView instead of in a browser
            mWebView.webViewClient = WebViewClient()
            mWebView.webChromeClient = object: WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(origin:String, callback: GeolocationPermissions.Callback) {
                    val perm: String = android.Manifest.permission.ACCESS_FINE_LOCATION
//
                    // ask the user for permission
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(perm), 1)
                    // we will use these when user responds
                    callback.invoke(origin, true, false)
                }
            }

            return  view

        }

}

