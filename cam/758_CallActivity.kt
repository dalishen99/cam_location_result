package com.example.testing.recyclerview.olivia.activities

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.testing.recyclerview.olivia.databinding.ActivityCallBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.annotations.NotNull
import models.InterfaceKt
import models.User
import java.util.*


class CallActivity : AppCompatActivity() {

    private var uniqueId = ""
    var auth: FirebaseAuth? = null
    var username = ""
    var friendsUsername = ""

    var isPeerConnected = false

    var firebaseRef: DatabaseReference? = null

    var isAudio = true
    var isVideo = true
    var createdBy: String? = null

    var pageExit = false

    var selfEnd = false
    var x = true

    lateinit var binding: ActivityCallBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firebaseRef = FirebaseDatabase.getInstance().reference.child("users")

        username = intent.getStringExtra("username")!!
        val incoming = intent.getStringExtra("incoming")
        createdBy = intent.getStringExtra("createdBy")
        friendsUsername = incoming.toString()
        setupWebView()

        binding.micBtn.setOnClickListener {
            isAudio = !isAudio
            callJavaScriptFunction("javascript:toggleAudio(\"$isAudio\")")

//            if (isAudio) {
//                binding.micBtn.setImageResource(R.drawable.numute_n)
//            } else {
//                binding.micBtn.setImageResource(R.drawable.mute_p)
//            }
        }

        binding.videoBtn.setOnClickListener {
            isVideo = !isVideo
            callJavaScriptFunction("javascript:toggleVideo(\"$isVideo\")")
//            if (isVideo) {
//                binding.videoBtn.setImageResource(R.drawable.btn_video_normal)
//            } else {
//                binding.videoBtn.setImageResource(R.drawable.btn_video_muted)
//            }
        }
        if (!selfEnd) {
            try {
                firebaseRef!!.child(createdBy!!).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {

                                finish()

                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }



        binding.endCall.setOnClickListener {
            selfEnd = true;
            finish() }
    }

    fun setupWebView() {
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.addJavascriptInterface(InterfaceKt(this), "Android")
        loadVideoCall()
    }

    fun loadVideoCall() {
        val filePath = "file:android_asset/call.html"
        binding.webView.loadUrl(filePath)
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                initializePeer()
            }
        }
    }


    fun initializePeer() {
        uniqueId = getUniqueId()
        callJavaScriptFunction("javascript:init(\"$uniqueId\")")
        if (createdBy.equals(username, ignoreCase = true)) {
            if (pageExit) return
            firebaseRef!!.child(username).child("connId").setValue(uniqueId)
            firebaseRef!!.child(username).child("isAvailable").setValue(true)
            binding.loadingGroup.visibility = View.GONE
            binding.controls.visibility = View.VISIBLE
            FirebaseDatabase.getInstance().reference
                .child("profiles")
                .child(friendsUsername)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(@NotNull snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        Glide.with(this@CallActivity).load(user!!.getProfile())
                            .into(binding.profile)
                        binding.name.text = user.getName()
                        binding.city.text = user.getCity()
                    }

                    override fun onCancelled(@NotNull error: DatabaseError) {}
                })
        } else {
            Handler().postDelayed({
                friendsUsername = createdBy!!
                FirebaseDatabase.getInstance().reference
                    .child("profiles")
                    .child(friendsUsername)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(@NotNull snapshot: DataSnapshot) {
                            val user = snapshot.getValue(User::class.java)
                            if (x) {
                                Glide.with(this@CallActivity).load(user!!.getProfile())
                                    .into(binding.profile)
                            }
                            binding.name.text = user?.getName()
                            binding.city.text = user?.getCity()
                        }

                        override fun onCancelled(@NotNull error: DatabaseError) {}
                    })
                FirebaseDatabase.getInstance().reference
                    .child("users")
                    .child(friendsUsername)
                    .child("connId")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(@NotNull snapshot: DataSnapshot) {
                            if (snapshot.value != null) {
                                sendCallRequest()
                            }
                        }

                        override fun onCancelled(@NotNull error: DatabaseError) {}
                    })
            }, 3000)
        }
    }

    fun onPeerConnected() {
        isPeerConnected = true
    }

    fun sendCallRequest() {
        if (!isPeerConnected) {
            Toast.makeText(
                this,
                "You are not connected. Please check your internet.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        listenConnId()
    }

    fun listenConnId() {
        firebaseRef!!.child(friendsUsername).child("connId")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(@NotNull snapshot: DataSnapshot) {
                    if (snapshot.value == null) return
                    binding.loadingGroup.visibility = View.GONE
                    binding.controls.visibility = View.VISIBLE
                    val connId = snapshot.getValue(String::class.java)
                    callJavaScriptFunction("javascript:startCall(\"$connId\")")
                }

                override fun onCancelled(@NotNull error: DatabaseError) {}
            })
    }

    fun callJavaScriptFunction(function: String?) {
        binding.webView.post { binding.webView.evaluateJavascript(function!!, null) }
    }

    fun getUniqueId(): String {
        return UUID.randomUUID().toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        x=false
        Toast.makeText(this, "Call ended", Toast.LENGTH_SHORT).show()
        pageExit = true
        firebaseRef!!.child(createdBy!!).setValue(null)
        binding.webView.loadUrl("file:///android_asset/nonexistent.html")
    }

    override fun onBackPressed() {
        super.onBackPressed()
            finish()
        }
    }

