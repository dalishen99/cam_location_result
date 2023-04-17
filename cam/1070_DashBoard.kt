package com.aditech.vcall.ui.mainDashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import android.widget.VideoView
import android.net.Uri
import android.widget.MediaController
import com.aditech.vcall.R
import com.aditech.vcall.network.Constraints

private const val TAG = "DashBoard"

class DashBoard : Fragment() {

    private lateinit var webView: WebView
    private var simpleVideoView: VideoView? = null
    private var mediaControls: MediaController? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_dash_board, container, false)
        simpleVideoView = view.findViewById(R.id.videoView);


        if (mediaControls == null) {
            mediaControls = MediaController(requireContext())
            mediaControls!!.setAnchorView(this.simpleVideoView)
        }

        // set the media controller for video view
        simpleVideoView!!.setMediaController(mediaControls)

        // set the absolute path of the video file which is going to be played
        simpleVideoView!!.setVideoURI(Uri.parse(Constraints.BASE_URL))

        simpleVideoView!!.requestFocus()

        // starting the video
        simpleVideoView!!.start()




        return view
    }

   /* @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun setupWebView() {

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(DashBoard(), "Android")

        loadVideoCall()
    }

    private fun loadVideoCall() {

        webView.loadUrl(Constraints.BASE_URL)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
            }
        }
    }

    private fun callJavascriptFunction(functionString: String) {
        webView.post { webView.evaluateJavascript(functionString, null) }
    }

    fun callclass() {
        Log.e(TAG, "callclass: ")
    }
*/
}