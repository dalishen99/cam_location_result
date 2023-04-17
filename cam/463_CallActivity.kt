package com.example.videocallapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_call.*
import java.util.*

class CallActivity : AppCompatActivity() {

    private val TAG = "Jayden_CallActivity"
    private val HIDDEN_ID = "appservice1"

    var username = ""
    var friendsUsername = ""

    var isPeerConnected = false

//    var firebaseRef = Firebase.database.getReference("users")

    var isAudio = true
    var isVideo = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        username = intent.getStringExtra("username")!!
        Log.d(TAG, "jayden username : " + username);

        callBtn.setOnClickListener {
            friendsUsername = friendNameEdit.text.toString() + HIDDEN_ID
            sendCallRequest()
        }

        toggleAudioBtn.setOnClickListener {
            isAudio = !isAudio
            callJavascriptFunction("javascript:toggleAudio(\"${isAudio}\")")
            toggleAudioBtn.setImageResource(if (isAudio) R.drawable.ic_baseline_mic_24 else R.drawable.ic_baseline_mic_off_24 )
        }

        toggleVideoBtn.setOnClickListener {
            isVideo = !isVideo
            callJavascriptFunction("javascript:toggleVideo(\"${isVideo}\")")
            toggleVideoBtn.setImageResource(if (isVideo) R.drawable.ic_baseline_videocam_24 else R.drawable.ic_baseline_videocam_off_24 )
        }

        setupWebView()
    }

    private fun sendCallRequest() {
        if (!isPeerConnected) {
            Toast.makeText(this, "You're not connected. Check your internet", Toast.LENGTH_LONG).show()
            return
        }

        switchToVideoScreens()
        callJavascriptFunction("javascript:startCall(\"${friendsUsername}\")")

//        firebaseRef.child(friendsUsername).child("incoming").setValue(username)
//        firebaseRef.child(friendsUsername).child("isAvailable").addValueEventListener(object: ValueEventListener {
//            override fun onCancelled(error: DatabaseError) {}
//
//            override fun onDataChange(snapshot: DataSnapshot) {
//
//                if (snapshot.value.toString() == "true") {
//                    listenForConnId()
//                }
//
//            }
//
//        })

    }

    private fun listenForConnId() {
//        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(object: ValueEventListener {
//            override fun onCancelled(error: DatabaseError) {}
//
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (snapshot.value == null)
//                    return
//                switchToControls()
//                callJavascriptFunction("javascript:startCall(\"${snapshot.value}\")")
//            }
//
//        })
    }

    private fun setupWebView() {

        webView.webChromeClient = object: WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(JavascriptInterface(this), "Android")

        loadVideoCall()
    }

    private fun loadVideoCall() {
        val filePath = "file:android_asset/call.html"
        webView.loadUrl(filePath)

        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Log.d(TAG, "jayden onPageFinished url " + url);
                initializePeer()
            }
        }
    }

    var uniqueId = ""

    private fun initializePeer() {

//        uniqueId = getUniqueID()
        uniqueId = username
        Log.d(TAG, "jayden initializePeer uniqueId " + uniqueId);

        callJavascriptFunction("javascript:init(\"${uniqueId}\")")
//        firebaseRef.child(username).child("incoming").addValueEventListener(object: ValueEventListener {
//            override fun onCancelled(error: DatabaseError) {}
//
//            override fun onDataChange(snapshot: DataSnapshot) {
//                onCallRequest(snapshot.value as? String)
//            }
//
//        })

    }

    private fun onCallRequest(caller: String?) {
        if (caller == null) return

        callLayout.visibility = View.VISIBLE
        incomingCallTxt.text = "$caller is calling..."

        acceptBtn.setOnClickListener {
//            firebaseRef.child(username).child("connId").setValue(uniqueId)
//            firebaseRef.child(username).child("isAvailable").setValue(true)

            callLayout.visibility = View.GONE
            switchToVideoScreens()
        }

        rejectBtn.setOnClickListener {
//            firebaseRef.child(username).child("incoming").setValue(null)
            callLayout.visibility = View.GONE
        }

    }

    private fun switchToVideoScreens() {
        inputLayout.visibility = View.GONE
        callControlLayout.visibility = View.VISIBLE
    }


    private fun getUniqueID(): String {
        return UUID.randomUUID().toString()
    }

    private fun callJavascriptFunction(functionString: String) {
        webView.post { webView.evaluateJavascript(functionString, null) }
    }


    fun onPeerConnected() {
        Log.d(TAG, "jayden onPeerConnected " + true);
        isPeerConnected = true
    }

    fun onCallReceived() {
        Log.d(TAG, "jayden onCallReceived ");
        runOnUiThread {
            callLayout.visibility = View.GONE
            switchToVideoScreens()
        }
    }

    fun onStreamReceived() {
        Log.d(TAG, "jayden onStreamReceived ");
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
//        firebaseRef.child(username).setValue(null)
        webView.loadUrl("about:blank")
        super.onDestroy()
    }

}