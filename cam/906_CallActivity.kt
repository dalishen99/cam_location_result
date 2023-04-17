package com.android.chat.ui.call

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.android.chat.Constants
import com.android.chat.JavascriptInterface
import com.android.chat.R

import com.android.chat.databinding.ActivityCallBinding
import com.android.chat.models.CallModel
import com.android.chat.models.CurRecUser
import com.android.chat.ui.chat.ChatActivityViewModel
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.socket.client.IO
import kotlinx.android.synthetic.main.activity_call.*
import java.util.*

class CallActivity : AppCompatActivity() {

    private var socketIO = IO.socket(Constants.SOCKETIO_URL)
    lateinit var customViewModel: CallActivityViewModel

    lateinit var binding:  ActivityCallBinding

    lateinit var group_id :String
    lateinit var receiver_id: String
    private var caller_photo_url: String = ""
    private var caller_full_name: String = ""
    private var answerer_full_name: String = ""
    private var answerer_photo_url: String = ""

    lateinit var current_user_id:String
    private var incoming: String = ""
    private var isPeerConnected = false
    private var audioEnabled = true
    private var videoEnabled = true
    lateinit var mediaPlayerMessage:MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customViewModel = ViewModelProviders.of(this)[CallActivityViewModel::class.java]
        customViewModel.connectSocket(socketIO)

        val sh = getSharedPreferences("onlineState", Context.MODE_PRIVATE).edit()
        sh.putBoolean("onlineState", false)
        sh.apply()

        if(intent.hasExtra("group_id")) {
            group_id = intent.getStringExtra("group_id")!!
        }

        if(intent.hasExtra("receiver_id")) {
            receiver_id = intent.getStringExtra("receiver_id")!!
        }

        if(intent.hasExtra("current_user_id")) {
            current_user_id = intent.getStringExtra("current_user_id")!!
        }

        if(intent.hasExtra("incoming")) {
            incoming = intent.getStringExtra("incoming")!!
        }

        if (intent.hasExtra("answerer_full_name")){
            answerer_full_name = intent.getStringExtra("answerer_full_name")!!
        }

        if (intent.hasExtra("answerer_photo_url")){
            answerer_photo_url = intent.getStringExtra("answerer_photo_url")!!
        }

        if (intent.hasExtra("caller_full_name")){
            caller_full_name = intent.getStringExtra("caller_full_name")!!
        }

        if (intent.hasExtra("caller_photo_url")){
            caller_photo_url = intent.getStringExtra("caller_photo_url")!!
        }

        if(incoming == "fromHim"){
            incoming == ""
            binding.constraintIncomingCall.visibility = View.VISIBLE
            binding.constraintWebView.visibility = View.GONE
            binding.incomingName.text = caller_full_name
            Glide.with(this).load(caller_photo_url).into(binding.incomingImg)




        }else{
            if(incoming == "fromMe"){
                binding.constraintIncomingCall.visibility = View.GONE
                binding.constraintWebView.visibility = View.VISIBLE
                binding.receiverName.text = answerer_full_name
                Glide.with(this).load(answerer_photo_url).into(binding.receiverImg)
                setupWebView()
            }
        }

        binding.btnAcceptCall.setOnClickListener {
            binding.constraintIncomingCall.visibility = View.GONE
            binding.constraintWebView.visibility = View.VISIBLE
            setupWebView()
        }

        binding.btnEndCall.setOnClickListener {
            customViewModel.endCall(current_user_id, receiver_id)
            finish()
        }

        binding.receiverBtnEndCall.setOnClickListener {
            customViewModel.endCall(current_user_id, receiver_id)
            if(incoming == "fromMe"){
                try {
                    //mediaPlayerMessage.stop()
                    mediaPlayerMessage.release()
                }catch (e:Exception){
                    Log.e("mediaPlayerMessage", e.message.toString())
                }
            }

            finish()
        }


        customViewModel.otherAnsweredStatusMutableLiveData.observe(this){
            if(current_user_id == it.receiver_id && receiver_id == it.current_user_id){
                runOnUiThread {
                    binding.receiverImgCard.visibility = View.GONE
                    binding.receiverName.visibility = View.GONE
                    binding.receiverStatus.visibility = View.GONE
                    binding.constraintWebView.visibility = View.VISIBLE
                    binding.constraintIncomingCall.visibility = View.GONE
                }
//                mediaPlayerMessage.pause()
//                mediaPlayerMessage.release()
            }else{
                if (current_user_id == it.current_user_id && receiver_id == it.receiver_id){
                    runOnUiThread {
                        binding.receiverImgCard.visibility = View.GONE
                        binding.receiverName.visibility = View.GONE
                        binding.receiverStatus.visibility = View.GONE
                        binding.constraintWebView.visibility = View.VISIBLE
                        binding.constraintIncomingCall.visibility = View.GONE
                    }
                    try {
                        //mediaPlayerMessage.stop()
                        mediaPlayerMessage.release()
                    }catch (e:Exception){
                        Log.e("mediaPlayerMessage", e.message.toString())
                    }

                }
            }
        }
        customViewModel.otherAnsweredStatus()


        customViewModel.endCallStatusMutableLiveData.observe(this){
            if(current_user_id == it.receiver_id && receiver_id == it.current_user_id){
                finish()
            }else{
                if(receiver_id == it.receiver_id && current_user_id == it.current_user_id){
                    try {
                        //mediaPlayerMessage.stop()
                        mediaPlayerMessage.release()
                    }catch (e:Exception){
                        Log.e("mediaPlayerMessage", e.message.toString())
                    }
                }
            }
        }
        customViewModel.endCallStatus()


        binding.receiverBtnVoiceStatus.setOnClickListener {
            audioEnabledStatus()
        }

        binding.receiverBtnVideoCamStatus.setOnClickListener {
            videoEnabledStatus()
        }

        customViewModel.userAtAnotherCall()
        customViewModel.userAtAnotherMutableLiveData.observe(this){
            binding.receiverName.text = "At another call"
            binding.receiverName.setTextColor(Color.BLACK)
            Log.e("binding.receiverName.text", "At another call")
        }

    }

    private fun videoEnabledStatus() {
        videoEnabled = if(videoEnabled){
            callJavascriptFunction("javascript:toggleVideo(\"${!videoEnabled}\")")
            binding.receiverBtnVideoCamStatus.background = ContextCompat.getDrawable(this, R.drawable.tap_background)
            !videoEnabled
        }else{
            callJavascriptFunction("javascript:toggleVideo(\"${!videoEnabled}\")")
            binding.receiverBtnVideoCamStatus.background = null
            !videoEnabled
        }
    }

    private fun audioEnabledStatus() {
        audioEnabled = if(audioEnabled){
            callJavascriptFunction("javascript:toggleAudio(\"${!audioEnabled}\")")
            binding.receiverBtnVoiceStatus.background = ContextCompat.getDrawable(this, R.drawable.tap_background)
            !audioEnabled
        }else{
            callJavascriptFunction("javascript:toggleAudio(\"${!audioEnabled}\")")
            binding.receiverBtnVoiceStatus.background = null
            !audioEnabled
        }
    }

    private fun setupWebView() {

        binding.webView.webChromeClient = object: WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)

            }

            override fun onPermissionRequestCanceled(request: PermissionRequest?) {
                super.onPermissionRequestCanceled(request)
            }
        }
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.mediaPlaybackRequiresUserGesture = false
        binding.webView.addJavascriptInterface(JavascriptInterface(this), "Android")
        loadVideoCall()
    }

    private fun loadVideoCall() {
        val filePath = "file:android_asset/call.html"
        binding.webView.loadUrl(filePath)

        binding.webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                initializePeer()
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

            }
        }
    }

    private fun initializePeer() {
        callJavascriptFunction("javascript:init(\"${group_id+current_user_id}\")")
    }

    fun onPeerConnected() {
        isPeerConnected = true
        if(incoming != "fromHim"){
            customViewModel.makeCall(current_user_id, receiver_id, group_id, caller_photo_url, caller_full_name, answerer_photo_url, answerer_full_name)
             mediaPlayerMessage = MediaPlayer.create(this, R.raw.call)
            try {
                mediaPlayerMessage.isLooping = true
                mediaPlayerMessage.start()

            }catch (e:Exception){

            }
        }
    }

    fun callOther() {
        callHim()
    }

    private fun callHim() {
        if(!isPeerConnected){
            Toast.makeText(this, "Try again later", Toast.LENGTH_SHORT).show()
            return
        }
        callJavascriptFunction("javascript:startCall(\"${group_id+receiver_id}\")")
    }

    private fun callJavascriptFunction(functionString: String) {
        binding.webView.post { binding.webView.evaluateJavascript(functionString, null) }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        binding.webView.loadUrl("about:blank")
        super.onDestroy()
    }

    fun onCallAnswer() {
        customViewModel.answerCall(current_user_id, receiver_id)
    }


    override fun onResume() {
        super.onResume()
        val sh = getSharedPreferences("onlineState", Context.MODE_PRIVATE).edit()
        sh.putBoolean("onlineState", false)
        sh.apply()
        val currentUser = getSharedPreferences("CurrentUser", Context.MODE_PRIVATE).getString("_id", "Empty")!!
        customViewModel.joinChat(currentUser)
    }

    override fun onPause() {
        super.onPause()
        val sh = getSharedPreferences("onlineState", Context.MODE_PRIVATE).edit()
        sh.putBoolean("onlineState", true)
        sh.apply()
    }

    override fun onStop() {
        super.onStop()
        Handler().postDelayed({
            val sh = getSharedPreferences("onlineState", Context.MODE_PRIVATE).getBoolean("onlineState", true)
            if (sh){
                val currentUser = getSharedPreferences("CurrentUser", Context.MODE_PRIVATE).getString("_id", "Empty")!!
                customViewModel.MethodLogOutMutableLiveData(currentUser)
            }
        }, 7000)
    }

}

