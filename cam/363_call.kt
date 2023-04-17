package com.unfpa.appsistenciamaternaunfpa

import android.annotation.SuppressLint
import android.content.Intent
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.unfpa.appsistenciamaternaunfpa.activities.DialogChatFragment
import com.unfpa.appsistenciamaternaunfpa.activities.my_health.ResumenEndCallActivity
import com.unfpa.appsistenciamaternaunfpa.api_controller.EndPoints
import com.unfpa.appsistenciamaternaunfpa.utils.AppUtils
import kotlinx.android.synthetic.main.activity_call.*
import java.text.SimpleDateFormat
import java.util.*

public class call : AppCompatActivity() {
    var username = ""
    var calluser = ""
    var friendsUsername = ""
    var uniqueId = ""
    var IdAppointment = ""
    var birth = ""
    var gestationWeeks = ""
    var gestationWeeksDate = ""
    var doctor = ""

    var isPeerConnected = false

    var firebaseRef = Firebase.database.getReference("users")

    var isAudio = true
    var isVideo = true

    var count = 0
    var is_added = false
    var horas = ""
    var fechaApp = ""
    var nombreCompleto = ""
    var doctorName = ""
    var bootomsheetFragment = DialogChatFragment()
    var getUserDetail = EndPoints.URL_HEROKU + "/api/v1/user/getuserdetail";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)
        try {
            username = intent.getStringExtra("username")!!
            calluser = intent.getStringExtra("calluser")!!
            uniqueId = intent.getStringExtra("uniqueId")!!
            doctor = intent.getStringExtra("doctor")!!

            val typeUser = AppUtils.getTypeUser(this)
            if (typeUser == "enfermera") {
                nombreCompleto = intent.getStringExtra("patient_name")
                horas = intent.getStringExtra("hour")
                fechaApp = intent.getStringExtra("date")
                IdAppointment = intent.getStringExtra("IdAppointment")!!
                birth = intent.getStringExtra("birth")!!
                gestationWeeks = intent.getStringExtra("gestationWeeks")!!
                gestationWeeksDate = intent.getStringExtra("gestationWeeksDate")
                doctorName = intent.getStringExtra("doctor_name")
            } else if (typeUser != "paciente") {
                IdAppointment = intent.getStringExtra("IdAppointment")!!
                birth = intent.getStringExtra("birth")!!
                gestationWeeks = intent.getStringExtra("gestationWeeks")!!
                gestationWeeksDate = intent.getStringExtra("gestationWeeksDate")!!
            }



            if (calluser.toString() != "") {
                Handler().postDelayed({
                    friendsUsername = calluser.toString()
                    sendCallRequest()

                    if (typeUser == "medico" || typeUser == "enfermera") {
                        firebaseRef.child(friendsUsername).child("incoming").addValueEventListener(
                            object : ValueEventListener {
                                override fun onCancelled(error: DatabaseError) {}

                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.value == null) {
                                        desconection()
                                    }
                                }
                            })
                    }
                }, 3000)
            }
            toggleAudioBtn.setOnClickListener {
                isAudio = !isAudio
                callJavascriptFunction("javascript:toggleAudio(\"${isAudio}\")")
                toggleAudioBtn.setImageResource(if (isAudio) R.drawable.mic else R.drawable.mic_off)
            }

            toggleVideoBtn.setOnClickListener {
                isVideo = !isVideo
                callJavascriptFunction("javascript:toggleVideo(\"${isVideo}\")")
                toggleVideoBtn.setImageResource(if (isVideo) R.drawable.videocam else R.drawable.videocam_off)
            }

            toggleChatBtn.setOnClickListener {
                val args = Bundle()
//            var getMyUserId = AppUtils.getDataUser(applicationContext)
                var getMyUserName = AppUtils.getDataCompleteNomUser(applicationContext)
//            Log.d("cargo identificadores",username+getMyUserId)
                var receive: String
                if (!calluser.isNullOrEmpty()) {
                    receive = calluser
                } else {
                    receive = doctor
                }
                var roonName: String

                if (typeUser.equals("medico") || typeUser.equals("enfermera")) {
                    roonName = "$username,$receive"
                } else {
                    roonName = "$receive,$username"

                }
                args.putString("receive", receive)
                args.putString("roomName", roonName)
                args.putString("userName", getMyUserName)

                bootomsheetFragment.arguments = args;

                bootomsheetFragment.show(supportFragmentManager, "tagdialogchat")
            }


            toggleVideoClose.setOnClickListener {
                if (typeUser == "paciente") {
                    startService(Intent(this@call, callService::class.java))
                }
                finish()
            }

            closeCall.setOnClickListener {
                if (typeUser == "paciente") {
                    startService(Intent(this@call, callService::class.java))
                }
                finish()
            }

            listenDesconection()
            setUpWebView(uniqueId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun sendCallRequest() {
        if (!isPeerConnected) {
            Toast.makeText(this, "No estas conectado, verifica tu internet", Toast.LENGTH_LONG)
                .show()
            return
        }
        firebaseRef.child(friendsUsername).child("incoming").setValue(username)
        firebaseRef.child(friendsUsername).child("isAvailable").addValueEventListener(object :
            ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value.toString() == "true") {
                    listenForConnId()
                    desconection()
                }
            }
        })
    }


    private fun listenForConnId() {
        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(object :
            ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}


            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == null)
                    return
                switchToControls()
                callJavascriptFunction("javascript:startCall(\"${snapshot.value}\")")
            }
        })
    }

    private fun desconection() {
        val typeUser = AppUtils.getTypeUser(applicationContext)

        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(object :
            ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}


            override fun onDataChange(snapshot: DataSnapshot) {
//                Toast.makeText(applicationContext,snapshot.value.toString(), Toast.LENGTH_LONG).show()


                if (snapshot.value == null) {

                    //after increment and befor setValue()

                    if (!is_added && count == 0) {
                        if (typeUser == "paciente") {
                            startService(Intent(this@call, callService::class.java))
                            finish()
                        } else if (typeUser == "enfermera") {
                            val dateFormat = SimpleDateFormat("hh:mm a")
                            val formattedDate = dateFormat.format(Date()).toString()
                            AppUtils.setCallTime(this@call, formattedDate, "End")
                            val intent = Intent(
                                this@call,
                                com.unfpa.appsistenciamaternaunfpa.activities.nurse.appointment.ResumenEndCallActivity::class.java
                            )
                            intent.putExtra("IdAppointment", IdAppointment)
                            intent.putExtra("birth", birth)
                            intent.putExtra("gestationWeeks", gestationWeeks)
                            intent.putExtra("gestationWeeksDate", gestationWeeksDate)
                            intent.putExtra("patient_name", nombreCompleto)
                            intent.putExtra("hour", horas)
                            intent.putExtra("date", fechaApp)
                            intent.putExtra("doctor_name", doctorName)
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent)
                        } else if (typeUser != "paciente") {
                            val intent = Intent(this@call, ResumenEndCallActivity::class.java)
                            intent.putExtra("IdAppointment", IdAppointment)
                            intent.putExtra("birth", birth)
                            intent.putExtra("gestationWeeks", gestationWeeks)
                            intent.putExtra("gestationWeeksDate", gestationWeeksDate)
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent)
                        }
                        is_added = true;

                    }
                    count++

                }
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView(uniId: String?) {
        webView.webChromeClient = object : WebChromeClient() {

            override fun onPermissionRequest(request: PermissionRequest?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request?.grant(request.resources)
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.addJavascriptInterface(JavascriptInterface(this), "Android")

        loadVideoCall(uniId)
    }

    private fun loadVideoCall(uniqId: String?) {
        val filePath = "file:android_asset/call.html"
        webView.loadUrl(filePath)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
//                uniqueId = getUniqueID()

                callJavascriptFunction("javascript:init(\"${uniqId}\")")
                if (doctor.toString() != "") {
                    switchToControls()
                }
            }
        }
    }

    private fun initializePeer() {
        uniqueId = getUniqueID()
        callJavascriptFunction("javascript:init(\"${uniqueId}\")")
        firebaseRef.child(username).child("incoming").addValueEventListener(object :
            ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}

            override fun onDataChange(snapshot: DataSnapshot) {
                onCallRequest(snapshot.value as? String)
            }
        })
    }

    private fun onCallRequest(caller: String?) {
        if (caller == null) return

        callLayout.visibility = View.VISIBLE
        incomingCallTxt.text = "$caller is calling..."
        acceptBtn.setOnClickListener {
            firebaseRef.child(username).child("connId").setValue(uniqueId)
            firebaseRef.child(username).child("isAvailable").setValue(true)
            callLayout.visibility = View.GONE
            switchToControls()
        }
        rejectBtn.setOnClickListener {
            firebaseRef.child(username).child("incoming").setValue(null)
            callLayout.visibility = View.GONE
        }

    }


    private fun switchToControls() {
        inputLayout.visibility = View.GONE
        callControlLayout.visibility = View.VISIBLE
    }

    private fun getUniqueID(): String {
        return UUID.randomUUID().toString()
    }

    fun callJavascriptFunction(functionString: String) {
        webView.post { webView.evaluateJavascript(functionString, null) }
    }

    fun onPeerConnected() {
        isPeerConnected = true
    }

    override fun onBackPressed() {
        val typeUser = AppUtils.getTypeUser(applicationContext)
        if (typeUser == "paciente") {
            startService(Intent(this@call, callService::class.java))
        }
        finish()
    }

    override fun onDestroy() {
        firebaseRef.child(friendsUsername).child("connId").setValue(null)
        firebaseRef.child(friendsUsername).child("incoming").setValue(null)
        firebaseRef.child(friendsUsername).child("isAvailable").setValue(null)

        firebaseRef.child(username).child("connId").setValue(null)
        firebaseRef.child(username).child("incoming").setValue(null)
        firebaseRef.child(username).child("isAvailable").setValue(null)
        webView.loadUrl("about:blank")
        super.onDestroy()
    }

    private fun listenDesconection() {
        val typeUser = AppUtils.getTypeUser(applicationContext)
        if (typeUser == "paciente") {
            firebaseRef.child(username).child("incoming").addValueEventListener(object :
                ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.value == null) {
                        startService(Intent(this@call, callService::class.java))
                        desconection()
                    }
                }
            })
        }
    }
}