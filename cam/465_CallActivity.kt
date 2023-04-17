package com.example.vidcallapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.util.Log.i
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.vidcallapp.R.id.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

class CallActivity : AppCompatActivity() {

    var username = ""
    var friendsUsername = ""

    var isPeerConnected = false
    var firebaseRef = Firebase.database.getReference("users")

    var isAudio=true
    var isVideo=true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        username= intent.getStringExtra("username")!!
        val callBtn = findViewById<Button>(R.id.callBtn)
        callBtn.setOnClickListener {
            val friendNameEdit = findViewById<EditText>(R.id.friendNameEdit)
            friendsUsername = friendNameEdit.text.toString()

            sendCallRequest()

        }
        val toggleAudioBtn = findViewById<ImageView>(R.id.toggleAudioBtn)
        toggleAudioBtn.setOnClickListener{
            isAudio = !isAudio
            callJavascriptFunction("javascript:toggleAudio(\"${isAudio}\")")
            toggleAudioBtn.setImageResource(if(isAudio)R.drawable.ic_baseline_mic_24 else R.drawable.ic_baseline_mic_off_24)
        }
        val toggleVideoBtn = findViewById<ImageView>(R.id.toggleVideoBtn)
        toggleVideoBtn.setOnClickListener{
            isVideo = !isVideo
            callJavascriptFunction("javascript:toggleAudio(\"${isVideo}\")")
            toggleVideoBtn.setImageResource(if(isVideo)R.drawable.ic_baseline_videocam_24 else R.drawable.ic_baseline_videocam_off_24)
        }
        setupWebView()

    }

    private fun sendCallRequest() {
        if (!isPeerConnected){
            Toast.makeText(this, "You are not connected. Check your internet",Toast.LENGTH_LONG).show()
            return
        }
        firebaseRef.child(friendsUsername).child("incoming").setValue(username)
        firebaseRef.child(friendsUsername).child("isAvailable").addValueEventListener(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value.toString() == "true"){
                    listenForConnId()
                }
            }

        })
    }

    private fun listenForConnId() {
        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {

            }
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == null)
                    return
                switchToControls()
                callJavascriptFunction("javascriptstartCall(\"${snapshot.value}\")")

            }

        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val webView = findViewById<WebView>(R.id.webView)
        webView.webChromeClient = object: WebChromeClient(){
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(com.example.vidcallapp.JavascriptInterface(this),"Android")
        loadVideoCall()

    }
    private fun loadVideoCall() {
        val webView = findViewById<WebView>(R.id.webView)
        val filePath = "file:android_asset/call.html"
        webView.loadUrl(filePath)
        webView.webViewClient = object: WebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                initializePeer()
            }
        }
    }
    private var uniqueId = ""
    private fun initializePeer() {
        uniqueId = getUniqueID()
        callJavascriptFunction("javascript:init(\"${uniqueId}\")")
        firebaseRef.child(username).child("incoming").addValueEventListener(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {

            }
            override fun onDataChange(snapshot: DataSnapshot) {
                onCallRequest(snapshot.value as? String)
            }

        })

    }
    @SuppressLint("WrongViewCast")
    private fun onCallRequest(caller: String?) {
        if (caller == null) return
        val callLayout = findViewById<WebView>(callLayout)
        val incomingCallTxt = findViewById<TextView>(incomingCallTxt)
        val acceptBtn = findViewById<Button>(acceptBtn)
        callLayout.visibility = VISIBLE
        incomingCallTxt.text = "$caller is calling..."
        acceptBtn.setOnClickListener{
            firebaseRef.child(username).child("connId").setValue(uniqueId)
            firebaseRef.child(username).child("isAvailable").setValue(true)
            callLayout.visibility = GONE
            switchToControls()
        }
        val rejectBtn = findViewById<Button>(R.id.rejectBtn)
        rejectBtn.setOnClickListener{
            firebaseRef.child(username).child("incoming").setValue(null)
        }
    }
    @SuppressLint("WrongViewCast")
    private fun switchToControls() {
        val inputLayout = findViewById<WebView>(inputLayout)
        val callControlLayout = findViewById<WebView>(callControlLayout)
        inputLayout.visibility = GONE
        callControlLayout.visibility = VISIBLE
    }
    private fun getUniqueID(): String{
        return UUID.randomUUID().toString()
    }
    private fun callJavascriptFunction(functionString: String){
        val webView = findViewById<WebView>(R.id.webView)
        webView.post {webView.evaluateJavascript(functionString,null)}
    }
    fun onPeerConnected() {
        isPeerConnected = true
    }
    override fun onBackPressed() {
        finish()
    }
    override fun onDestroy() {
        firebaseRef.child(username).setValue(null)
        val webView = findViewById<WebView>(R.id.webView)
        webView.loadUrl("about:blank")
        super.onDestroy()
    }
}



