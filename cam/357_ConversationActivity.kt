package com.zellycookies.pineapple.conversation

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zellycookies.pineapple.conversation.Adapter.ConversationAdapter
import com.zellycookies.pineapple.conversation.Object.GroupObject
import com.zellycookies.pineapple.conversation.Object.MessageObject
import com.zellycookies.pineapple.login.Login
import com.zellycookies.pineapple.matched.ProfileCheckinMatched
import com.zellycookies.pineapple.utils.GPS
import com.zellycookies.pineapple.utils.User
import com.bumptech.glide.Glide
import com.bumptech.glide.util.Util
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import com.zellycookies.pineapple.R
import com.zellycookies.pineapple.conversation.Object.MessageType
import java.io.ByteArrayOutputStream

//
class ConversationActivity : AppCompatActivity() {
    var action = "INIT"
    private var mGroupObject: GroupObject? = null
    private var userMatched: User? = null
    private var userId: String? = null
    private var gps: GPS? = null
    var messageList: ArrayList<MessageObject>? = null
    private var mChat: RecyclerView? = null
    private var mChatAdapter: RecyclerView.Adapter<*>? = null
    private var mChatLayoutManager: RecyclerView.LayoutManager? = null
    private var mFirestore: FirebaseFirestore? = null
    private var mAuth: FirebaseAuth? = null
    private var mAuthListener: AuthStateListener? = null
    private var mGroupMessageDb: DocumentReference? = null
    private var mMessageId: CollectionReference? = null
    private var imagePerson: ImageView? = null
    private var tvNamePerson: TextView? = null
    private var mMessage: EditText? = null
    private var btnSend: ImageButton? = null
    private var btnVideoCall: ImageButton? = null
    private var btnAddImage: ImageButton? = null
    private var btnBack: ImageButton? = null
    private var btnInfo: ImageButton? = null
    private val latitude = 37.349642
    private val longtitude = -121.938987
    private val RC_SELECT_IMAGE = 2

    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private val requestcode = 1


    var isPeerConnected = false
    var firebaseRef = FirebaseDatabase.getInstance().reference.child("videocall")
    var isAudio = true
    var isVideo = true
    private var toggleAudioBtn: ImageView? = null
    private var toggleVideoBtn: ImageView? = null
    private lateinit var webView: WebView
    private var rejectBtn: ImageView? = null
    private var callLayout: LinearLayout? = null
    private var callBtn: Button? = null
    private var incomingCallTxt: TextView? = null
    private var callControlLayout: LinearLayout? = null
    private var acceptBtn: ImageView? = null
    private var messageLayout: RelativeLayout? = null
    private var endCallBtn: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.conversation_activity)
        gps = GPS(this)
        setupFirebaseAuth()
        mFirestore = FirebaseFirestore.getInstance()


        //load user data
        mGroupObject = intent.getSerializableExtra("groupObject") as GroupObject?
        userMatched = mGroupObject!!.userMatch
        userId = mAuth!!.currentUser!!.uid
        mGroupMessageDb = mFirestore!!.collection("message").document(mGroupObject!!.chatId!!)


        //setup display info
        btnInfo = findViewById(R.id.checkInfoUserMatched)
        btnBack = findViewById(R.id.back_matched_activity)
        imagePerson = findViewById(R.id.image_user_group)
        tvNamePerson = findViewById(R.id.name_user_group)
        btnSend = findViewById(R.id.send)
        btnAddImage = findViewById(R.id.insertImage)
        btnVideoCall = findViewById(R.id.videoCallBtn)
        mMessage = findViewById(R.id.messageInput)
        btnSend?.setOnClickListener(View.OnClickListener { sendMessage() })
        btnAddImage?.setOnClickListener(View.OnClickListener { sendImage() })
        btnVideoCall?.setOnClickListener(View.OnClickListener { videoCall() })
        btnBack?.setOnClickListener(View.OnClickListener { onBackPressed() })
        btnInfo?.setOnClickListener(View.OnClickListener {
            val distance = gps!!.calculateDistance(
                latitude,
                longtitude,
                userMatched!!.latitude,
                userMatched!!.longtitude
            )
            val intent = Intent(this@ConversationActivity, ProfileCheckinMatched::class.java)
            intent.putExtra("classUser", userMatched)
            intent.putExtra("distance", distance)
            startActivity(intent)
        })
        initializeMessage()
        chatMessage
        initTopBar()

        toggleAudioBtn = findViewById(R.id.toggleAudioBtn)
        toggleVideoBtn = findViewById(R.id.toggleVideoBtn)
        webView = findViewById(R.id.webView)
        rejectBtn = findViewById(R.id.rejectBtn)
        callLayout = findViewById(R.id.callLayout)
        incomingCallTxt = findViewById(R.id.incomingCallTxt)
        callControlLayout = findViewById(R.id.callControlLayout)
        acceptBtn = findViewById(R.id.acceptBtn)
        messageLayout = findViewById(R.id.messageLayout)
        endCallBtn = findViewById(R.id.endCallBtn)




        toggleAudioBtn!!.setOnClickListener {
            isAudio = !isAudio
            callJavascriptFunction("javascript:toggleAudio(\"${isAudio}\")")
            toggleAudioBtn!!.setImageResource(if (isAudio) R.drawable.ic_baseline_mic_24 else R.drawable.ic_baseline_mic_off_24 )
        }

        toggleVideoBtn!!.setOnClickListener {
            isVideo = !isVideo
            callJavascriptFunction("javascript:toggleVideo(\"${isVideo}\")")
            toggleVideoBtn!!.setImageResource(if (isVideo) R.drawable.ic_baseline_videocam_24 else R.drawable.ic_baseline_videocam_off_24 )
        }

        endCallBtn!!.setOnClickListener{
            firebaseRef.child(userId!!).setValue(null)
            webView?.visibility = View.GONE
            messageLayout?.visibility = View.VISIBLE
            callControlLayout?.visibility = View.GONE
        }


        setupWebView()

    }

    var messageIdList: MutableList<DocumentSnapshot>? = null
    private val chatMessage: Unit
        private get() {
            mMessageId = mGroupMessageDb!!.collection("messages")
            mMessageId!!.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { value, error ->
                    if (value != null) {
                        messageIdList = value.documents
                        Log.d(
                            TAG,
                            "getChatMessage: messageIdList " + messageIdList!!.size + "-" + messageIdList.toString()
                        )
                        if (action == "INIT" || action == SEND_MESSAGE) {
                            for (i in messageIdList!!) {
                                var messageText: String? = ""
                                var sendBy: String? = ""
                                var datetime: String? = ""
                                if (i.getString("messageText") != null) {
                                    messageText = i.getString("messageText")
                                }
                                if (i.getString("sendBy") != null) {
                                    sendBy = i.getString("sendBy")
                                }
                                if (i.getString("sendAt") != null) {
                                    datetime = i.getString("sendAt")
                                }
                                var type: String? = MessageType.TEXT
                                if (i.getString("type") == MessageType.IMAGE)
                                    type = MessageType.IMAGE
                                val mMessage = MessageObject(i.id, sendBy, messageText, datetime, type)
                                messageList!!.add(mMessage)
                                mChatLayoutManager!!.scrollToPosition(messageList!!.size - 1)
                                mChatAdapter!!.notifyDataSetChanged()
                                action = UPDATED_MESSAGE

                                val abc = MessageObject(i.id, sendBy, messageText, datetime)
                            }
                            messageIdList!!.clear()
                        }
                    }
                }
        }

    private fun initTopBar() {
        tvNamePerson!!.text = userMatched!!.username
        val profileImageUrl = userMatched!!.profileImageUrl
        if (Util.isOnMainThread()) {
            when (profileImageUrl) {
                "defaultFemale" -> imagePerson?.let {
                    Glide.with(applicationContext).load(R.drawable.img_ava_female)
                        .into(it)
                }
                "defaultMale" -> imagePerson?.let {
                    Glide.with(applicationContext).load(R.drawable.img_ava_male)
                        .into(it)
                }
                else -> Glide.with(applicationContext).load(profileImageUrl).into(imagePerson!!)
            }
        }
    }

    private fun initializeMessage() {
        messageList = ArrayList()
        mChat = findViewById(R.id.messageList)
        mChat?.isNestedScrollingEnabled = false
        mChat?.setHasFixedSize(false)
        mChatLayoutManager =
            LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        mChat?.layoutManager = mChatLayoutManager
        mChatAdapter = ConversationAdapter(messageList!!)
        mChat?.adapter = mChatAdapter
    }

    private fun sendMessage() {
        val messageId = mGroupMessageDb!!.collection("messages").document().id
        val mMessageDb = mGroupMessageDb!!.collection("messages").document(messageId)
        val cal = Calendar.getInstance(Locale.ENGLISH)
        val dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()
        var newMessageMap: MutableMap<Any, Any> = mutableMapOf()
        userId?.let { newMessageMap.put("sendBy", it) }
        newMessageMap.put("sendAt", dateTime)
        newMessageMap.put("timestamp", FieldValue.serverTimestamp())
        newMessageMap.put("type", MessageType.TEXT)

        if (!mMessage!!.text.toString().isEmpty()) {
            newMessageMap["messageText"] = mMessage!!.text.toString()
            updateDatabaseWithNewMessage(mMessageDb, newMessageMap)
            action = SEND_MESSAGE
            messageList!!.clear()
            Log.d(TAG, " sendMessage over")
        }
    }

    private fun sendImage() {

        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
        }

        resultLauncher.launch(Intent.createChooser(intent, "Select Image"))

    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val selectedImagePath = data?.data
            val selectedImageBmp = MediaStore.Images.Media.getBitmap(contentResolver, selectedImagePath)
            val outputStream = ByteArrayOutputStream()
            selectedImageBmp.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val selectedImageBytes = outputStream.toByteArray()


            val messageId = mGroupMessageDb!!.collection("messages").document().id
            val mMessageDb = mGroupMessageDb!!.collection("messages").document(messageId)
            val cal = Calendar.getInstance(Locale.ENGLISH)
            val dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()
            var newMessageMap: MutableMap<Any, Any> = mutableMapOf()
            userId?.let { newMessageMap.put("sendBy", it) }
            newMessageMap.put("sendAt", dateTime)
            newMessageMap.put("timestamp", FieldValue.serverTimestamp())

            val filepath = FirebaseStorage.getInstance().reference.child("messageImages").child(
                messageId
            )

            val uploadTask = filepath.putBytes(selectedImageBytes)

            uploadTask.continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                filepath.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    //                        FriendlyMessage friendlyMessage = new FriendlyMessage(null, mUsername, downloadUri.toString());
                    //                        mMessagesDatabaseReference.push().setValue(friendlyMessage);
                    //             var userInfo = mutableMapOf<String, Any>()
//                        userInfo.put("profileImageUrl", downloadUri.toString())
//                        mPhotoDB!!.updateChildren(userInfo)
                    newMessageMap["messageText"] = downloadUri.toString()
                    newMessageMap["type"] = MessageType.IMAGE
                    updateDatabaseWithNewMessage(mMessageDb, newMessageMap)
                }
            }

            action = SEND_MESSAGE
        }
    }

    private fun videoCall(){
        if (!isPermissionGranted()) {
            askPermissions()
        }
        messageLayout!!.visibility = View.GONE
        sendCallRequest()
    }

    private fun updateDatabaseWithNewMessage(
        mMessageDb: DocumentReference,
        newMessageMap: Map<*, *>
    ) {
        mMessageDb.set(newMessageMap)
        mMessage!!.setText(null)
        //Need to make notification
    }

    private fun setupFirebaseAuth() {
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                // user is signed in
                Log.d(TAG, "onAuthStateChanged: signed_in:" + user.uid)
            } else {
                //user is signed out
                Log.d(TAG, "onAuthStateChanged: signed_out")
                Log.d(TAG, "onAuthStateChanged: navigating back to login screen.")
                val intent = Intent(this@ConversationActivity, Login::class.java)

                //clear the activity stack， in case when sign out, the back button will bring the user back to the previous activity
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
    }

    companion object {
        //initialize variables and constants
        private const val TAG = "Conversation_Activity"
        private const val SEND_MESSAGE = "SEND_MESSAGE"
        private const val UPDATED_MESSAGE = "UPDATED_MESSAGE"
    }


    //calling function
    private fun askPermissions() {
        ActivityCompat.requestPermissions(this, permissions, requestcode)
    }

    private fun isPermissionGranted(): Boolean {

        permissions.forEach {
            if (ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED)
                return false
        }

        return true
    }

    private fun sendCallRequest() {
        if (!isPeerConnected) {
            Toast.makeText(this, "You're not connected. Check your internet", Toast.LENGTH_LONG).show()
            // return
        }

        webView?.visibility = View.VISIBLE

        firebaseRef.child(userMatched?.user_id!!).child("incoming").setValue(userMatched!!.username)
        firebaseRef.child(userMatched?.user_id!!).child("isAvailable").addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.value.toString() == "true") {
                    listenForConnId()
                }

            }

        })

    }

    private fun listenForConnId() {
        firebaseRef.child(userMatched?.user_id!!).child("connId").addValueEventListener(object: ValueEventListener {
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
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
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
        firebaseRef.child(userId!!).child("incoming").addValueEventListener(object: ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                onCallRequest(snapshot.value as? String)
            }

        })

    }

    private fun onCallRequest(caller: String?) {
        if (caller == null) return

        messageLayout?.visibility = View.GONE
        callLayout?.visibility = View.VISIBLE
        incomingCallTxt?.text = "$caller is calling..."

        acceptBtn?.setOnClickListener {
            firebaseRef.child(userId!!).child("connId").setValue(uniqueId)
            firebaseRef.child(userId!!).child("isAvailable").setValue(true)

            callLayout?.visibility = View.GONE
            webView?.visibility = View.VISIBLE
            switchToControls()
        }

        rejectBtn?.setOnClickListener {
            firebaseRef.child(userId!!).child("incoming").setValue(null)
            callLayout?.visibility = View.GONE
            messageLayout?.visibility = View.VISIBLE
        }

    }

    private fun switchToControls() {
        // inputLayout.visibility = View.GONE
        callControlLayout?.visibility   = View.VISIBLE
    }


    private fun getUniqueID(): String {
        return UUID.randomUUID().toString()
    }

    private fun callJavascriptFunction(functionString: String) {
        webView.post { webView.evaluateJavascript(functionString, null) }
    }


    fun onPeerConnected() {
        isPeerConnected = true
    }

    override fun onBackPressed() {
        //firebaseRef.child(userId!!).child("incoming").setValue(null)
        firebaseRef.child(userId!!).setValue(null)
        finish()
    }

    override fun onDestroy() {
        firebaseRef.child(userId!!).setValue(null)
        webView?.loadUrl("about:blank")
        super.onDestroy()
    }
}