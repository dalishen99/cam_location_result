package com.useb.kyc_sample_android_kotlin;

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.useb.kyc_sample_android_kotlin.databinding.ActivityWebViewBinding
import org.json.JSONException
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder

class  WebViewActivity : AppCompatActivity() {

    private var binding:ActivityWebViewBinding?=null
    private var handler:Handler = Handler()
    private var webview:WebView? = null
    private var result = ""
    private var detail = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        val url = "https://kyc.useb.co.kr/auth"

        // 바인딩 설정
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // 웹뷰 설정
        webview = binding!!.webview
        webview!!.settings.javaScriptEnabled = true
        webview!!.webViewClient = WebViewClient()
        webview!!.webChromeClient = WebChromeClient()
        webview!!.addJavascriptInterface(this, "alcherakyc")

        // 사용자 데이터 인코딩
        val userInfo = ""
        val encodedUserInfo:String = encodeJson(userInfo)

        // POST
        postUserInfo(url, encodedUserInfo)
    }

    // WebView 액티비티에서 뒤로가기 버튼 막기
    override fun onBackPressed() {
        //super.onBackPressed();
    }

    // webview가 닫히면 result를 보여주는 화면으로 전환
    public override fun onStop() {
        super.onStop()
        val intent = Intent(applicationContext, ReportActivity::class.java)
        intent.putExtra("detail", detail)
        intent.putExtra("result", result)
        startActivity(intent)
    }

    private fun postUserInfo(url : String, encodedUserInfo : String){

        handler.post { // 카메라 권한 요청
            cameraAuthRequest()
            webview!!.loadUrl(url)
            webview!!.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    webview!!.loadUrl("javascript:alcherakycreceive('$encodedUserInfo')")
                }
            }
        }
    }

    private fun getData(): JSONObject {

        var birthday = intent.getStringExtra("birthday")
        var name = intent.getStringExtra("name")
        var phoneNumber = intent.getStringExtra("phoneNumber")
        var email = intent.getStringExtra("email")
        return dataToJson(birthday, name, phoneNumber, email)
    }

    private fun dataToJson (birthday:String?, name:String?, phoneNumber:String?, email:String?) : JSONObject {

        var jsonObject = JSONObject()
        jsonObject.put("customer_id", "12")
        jsonObject.put("id", "demoUser")
        jsonObject.put("key", "demoUser0000!")
        jsonObject.put("name", name)
        jsonObject.put("birthday",  birthday)
        jsonObject.put("phone_number", phoneNumber)
        jsonObject.put("email", email)

        return jsonObject
    }

    private fun encodeURIComponent(encoded:String):String{

        var encodedURI = URLEncoder.encode(encoded, "UTF-8")
        encodedURI = encodedURI.replace("\\+".toRegex(), "%20")
        encodedURI = encodedURI.replace("%21".toRegex(), "!")
        encodedURI = encodedURI.replace("%27".toRegex(), "'")
        encodedURI = encodedURI.replace("%28".toRegex(), "(")
        encodedURI = encodedURI.replace("%29".toRegex(), ")")
        encodedURI = encodedURI.replace("%7E".toRegex(), "~")
        return encodedURI
    }

    private fun encodeJson(data: String): String {

        var data = data
        var encodedData:String? = null
        data = encodeURIComponent(getData().toString())
        encodedData = Base64.encodeToString(data!!.toByteArray(), 0)
        return encodedData
    }

    @JavascriptInterface
    @Throws(JSONException::class)
    fun receive(data: String?) {
        val decodedData = decodedReceiveData(data)
        var JsonObject = JSONObject(decodedData)
        var resultData = ""
        try {
            JsonObject = ModifyReviewResult(JsonObject)
            resultData = JsonObject.getString("result")
        } catch (e: JSONException) {
            resultData = JsonObject.getString("result")
        }
        if (resultData == "success") {
            detail = JsonObject.toString(4)
            result = "KYC 작업이 성공했습니다."
            Log.d("success", "KYC 작업이 성공했습니다.")
        } else if (resultData == "failed") {
            detail = JsonObject.toString(4)
            result = "KYC 작업이 실패했습니다."
            Log.d("failed", "KYC 작업이 실패했습니다.")
        }
        if (resultData == "complete") {
            detail = JsonObject.toString(4)
            result = "KYC가 완료되었습니다."
            Log.d("complete", "KYC가 완료되었습니다.")
        } else if (resultData == "close") {
            detail = JsonObject.toString(4)
            result = "KYC가 완료되지 않았습니다."
            Log.d("close", "KYC가 완료되지 않았습니다.")
        }
        finish()
    }

    @Throws(JSONException::class)
    private fun ModifyReviewResult(JsonObject: JSONObject): JSONObject {
        val reviewResult = JsonObject.getString("review_result")
        val reviewResultJsonObject = JSONObject(reviewResult)
        val image = reviewResultJsonObject.getString("id_card")
        val idCardJsonObject = JSONObject(image)
        var idCardImage = idCardJsonObject.getString("id_card_image")
        var idCardOrigin = idCardJsonObject.getString("id_card_origin")
        var idCropImage = idCardJsonObject.getString("id_crop_image")
        if (idCardImage !== "null") {
            idCardImage = idCardImage.substring(0, 20) + "...생략(omit)..."
            idCardJsonObject.put("id_card_image", idCardImage)
        }
        if (idCardOrigin !== "null") {
            idCardOrigin = idCardOrigin.substring(0, 20) + "...생략(omit)..."
            idCardJsonObject.put("id_card_origin", idCardOrigin)
        }
        if (idCropImage !== "null") {
            idCropImage = idCropImage.substring(0, 20) + "...생략(omit)..."
            idCardJsonObject.put("id_crop_image", idCropImage)
        }
        reviewResultJsonObject.put("id_card", idCardJsonObject)
        val faceCheck = reviewResultJsonObject.getString("face_check")
        val faceCheckObject = JSONObject(faceCheck)
        var faceImage = faceCheckObject.getString("selfie_image")
        if (faceImage !== "null") {
            faceImage = faceImage.substring(0, 20) + "...생략(omit)..."
            faceCheckObject.put("selfie_image", faceImage)
        }
        reviewResultJsonObject.put("face_check", faceCheckObject)
        JsonObject.put("review_result", reviewResultJsonObject)
        return JsonObject
    }


    fun decodedReceiveData(data: String?): String? {
        val decoded = String(Base64.decode(data, 0))
        return decodeURIComponent(decoded)
    }

    private fun decodeURIComponent(decoded: String): String? {
        var decodedURI: String? = null
        try {
            decodedURI = URLDecoder.decode(decoded, "UTF-8")
                .replace("%20".toRegex(), "\\+")
                .replace("!".toRegex(), "\\%21")
                .replace("'".toRegex(), "\\%27")
                .replace("\\(".toRegex(), "\\%28")
                .replace("\\)".toRegex(), "\\%29")
                .replace("~".toRegex(), "\\%7E")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return decodedURI
    }

    private fun cameraAuthRequest(){

        webview = binding!!.webview
        val ws = webview!!.settings
        ws.mediaPlaybackRequiresUserGesture = false
        webview!!.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {

                //API레벨이 21이상인 경우
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val requestedResources = request.resources
                    for (r in requestedResources) {
                        if (r == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                            request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                            break
                        }
                    }
                }
            }
        }
        val cameraPermissionCheck =
            ContextCompat.checkSelfPermission(this@WebViewActivity, Manifest.permission.CAMERA)
        if (cameraPermissionCheck != PackageManager.PERMISSION_GRANTED) { // 권한이 없는 경우
            ActivityCompat.requestPermissions(
                this@WebViewActivity,
                arrayOf(Manifest.permission.CAMERA),
                1000
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this@WebViewActivity,
                    "카메라/갤러리 접근 권한이 없습니다. 권한 허용 후 이용해주세요. no access permission for camera and gallery.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}
