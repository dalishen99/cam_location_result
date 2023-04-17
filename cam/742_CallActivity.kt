package au.edu.jcu.cp3406.designthinkingiii

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DatabaseError
import kotlinx.android.synthetic.main.activity_call.*
import java.util.*


class CallActivity : AppCompatActivity() {

    var phoneNumber = ""

    var friendsPhonenumber = ""

    var isPeerConnected = false

    var firebaseRef = Firebase.database.getReference("users")

    var isAudio = true
    var isVideo = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        phoneNumber = intent.getStringExtra("phoneNumber")!!

        callBtn.setOnClickListener{
            friendsPhonenumber = friendNumberEdit.text.toString()
            sendCallRequest()
        }
        toggleAudioBtn.setOnClickListener {
            isAudio = !isAudio
            callJavascriptFunction("javascript:toggleAudio(\"${isAudio}\")")
            toggleAudioBtn.setImageResource(if (isAudio) R.drawable.ic_baseline_mic_24 else R.drawable.ic_baseline_mic_off_24)
        }
        toggleVideoBtn.setOnClickListener {
            isVideo = !isVideo
            callJavascriptFunction("javascript:toggleVideo(\"${isVideo}\")")
            toggleVideoBtn.setImageResource(if (isVideo) R.drawable.ic_baseline_videocam_24 else R.drawable.ic_baseline_videocam_off_24 )
        }

        setupWebView()
    }
    private fun sendCallRequest() {
        if(!isPeerConnected) {
            Toast.makeText(this,"You're not connected. Check your internet", Toast.LENGTH_LONG).show()
        }

        firebaseRef.child(friendsPhonenumber).child("incoming").setValue(phoneNumber)
        firebaseRef.child(friendsPhonenumber).child("isAvailable").addValueEventListener(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.value.toString() == "true") {
                    listenForConnId()
                }

            }
        })
    }

    private fun listenForConnId() {
        firebaseRef.child(friendsPhonenumber).child("connId").addValueEventListener(object: ValueEventListener{
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == null)
                    return
                switchToControls()
                callJavascriptFunction("javascript:startCall(\"${snapshot.value}\")")
            }

        })
    }

    private fun setupWebView() {
        webView.webChromeClient = object: WebChromeClient() {

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
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
                initializePeer()
            }
        }
    }
    var uniqueId = ""
    private fun initializePeer() {
        uniqueId = getUniqueID()
        callJavascriptFunction("javascript:init(\"${uniqueId}\")")
        firebaseRef.child(phoneNumber).child("incoming").addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                onCallRequest(snapshot.value as? String)
            }

        })
    }
    private fun onCallRequest(caller: String?){
        if (caller == null) return

        callLayout.visibility = View.VISIBLE
        incomingCallTxt.text = "$caller is calling..."

        acceptBtn.setOnClickListener {
            firebaseRef.child(phoneNumber).child("connId").setValue(uniqueId)
            firebaseRef.child(phoneNumber).child("isAvailable").setValue(true)

            callLayout.visibility = View.GONE
            switchToControls()
        }
        rejectBtn.setOnClickListener {
            firebaseRef.child(phoneNumber).child("incoming").setValue(null)
            callLayout.visibility= View.GONE
        }

    }
    private fun switchToControls() {
        inputLayout.visibility= View.GONE
        callControlLayout.visibility = View.VISIBLE
    }

    private fun getUniqueID(): String {
        return UUID.randomUUID().toString()
    }

    private fun callJavascriptFunction(functionString: String){
        webView.post{webView.evaluateJavascript(functionString, null)}
    }

    fun onPeerConnected() {
        isPeerConnected = true
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        firebaseRef.child(phoneNumber).setValue(null)
        webView.loadUrl("about:blank")
        super.onDestroy()

    }

}