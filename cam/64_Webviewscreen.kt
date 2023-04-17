package com.example.blinkbuttonbytechnotoil

import android.annotation.TargetApi
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis


class Webviewscreen : Activity() {

    var text = "<?php ?>\n" +
            "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n" +
            "<style>\n" +
            "#iFrameID {\n" +
            "    opacity:1;\n" +
            "\tbackground-color: transparent;\n" +
            "\tposition:absolute;\n" +
            "\twidth:100%;\t\t\t\t\t\t\t\t\n" +
            "\theight:100%;\t\t\t\t\t\t\t\t\t\n" +
            "\ttop:0px;\n" +
            "\tleft:0px;\n" +
            "\tmargin-left: 0%;\n" +
            "  \tmargin-top: 0%;\n" +
            "\tz-index:100;\n" +
            "\tdisplay: none;\n" +
            "\tborder-width: 0px;\n" +
            "}\n" +
            "\n" +
            "#tryonBtnId{\n" +
            "\tbackground-color: black;\n" +
            "\tcolor:white;\n" +
            "\twidth: 100px;\n" +
            "\tcursor: pointer;\n" +
            "}\n" +
            "\n" +
            "</style>\n" +
            "</head>\n" +
            "<script type=\"text/javascript\" src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.6.0/jquery.min.js\"></script>\n" +
            "\n" +
            "<body oncontextmenu=\"return false;\">\n" +
            "\n" +
            "<iframe id=\"iFrameID\" src=\"about:blank\" allow=\"camera\"></iframe>\n" +
            "<script type=\"text/javascript\">\n" +
            "\n" +
            "function onTryonClick(allskus, companyName) {\n" +
            "\t\t\n" +
            "\tlet hostURL = window.location;\n" +
            "\tlet tempArr = hostURL.href.split(\"?\");\n" +
            "\n" +
            "\tvar url = \"https://cdn.camweara.com/camweara_jewelry_client/index.php?skus=\" + allskus + \"&company_name=\" + companyName + \"\";\n" +
            "\n" +
            "\tif(tempArr.length > 1){\n" +
            "\t\turl = url + \"?\"+ tempArr[1];\n" +
            "\t}\n" +
            "\tvar iframe = document.getElementById(\"iFrameID\");\n" +
            "\tiframe.style.display = \"block\";\n" +
            "\tiframe.setAttribute(\"src\", url); \n" +
            "}\n" +
            "\n" +
            " window.addEventListener('message', function(event) { \n" +
            "Android.showToast();\n" +

            "    // IMPORTANT: Check the origin of the data! \n" +
            "    if (event.origin.indexOf('https://cdn.camweara.com') || event.origin.indexOf('https://camweara.com')) { \n" +
            "      console.log(\"message: \"+event.data);\n" +
            "      if(event.data == \"closeIframe\"){\n" +
            "        var iframe = document.getElementById(\"iFrameID\");  \n" +
            "        iframe.contentWindow.location.replace(\"\");\n" +
            "        iframe.style.display = \"none\";\n" +
            "      }\n" +
            "      else if(event.data.includes(\"buynow\")){\n" +
            "        console.log(\"buynow message\");\n" +
            "        var iframe = document.getElementById(\"iFrameID\");  \n" +
            "        iframe.contentWindow.location.replace(\"\");\n" +
            "        iframe.style.display = \"none\";\n" +
            "\t\tlet tempArr = event.data.split(\"-\");\n" +
            "\t\tlet sku = tempArr[1];\n" +
            "\t\t//Redirect to Product page or add item to cart using SKU\n" +
            "      \n" +
            "      }\n" +
            "\n" +
            "    } else { \n" +
            "      // The data hasn't been sent from your site! \n" +
            "      // Be careful! Do not use it. \n" +
            "      return; \n" +
            "    } \n" +
            "  }); \n" +
            "\n" +
            "   function load_tryonbutton(skuPassed, camwearaKeyPassed, tryonButtonId){\n" +
            "          productSKU = skuPassed;\n" +
            "\t\t  camwearaKey = camwearaKeyPassed; \n" +
            "\n" +
            "          var today = new Date();\n" +
            "          var dd = String(today.getDate()).padStart(2, '0');\n" +
            "          var mm = String(today.getMonth() + 1).padStart(2, '0'); //January is 0!\n" +
            "          var yyyy = today.getFullYear();\n" +
            "          today = dd + mm + yyyy;\n" +
            "\n" +
            "            let tryon_file_url= \"https://camweara-customers.s3.ap-south-1.amazonaws.com/\" + camwearaKey + \"/\" + camwearaKey + \"_tryonbutton.json?\" + today;\n" +
            "            \n" +
            "          \t  fetch(tryon_file_url)\n" +
            "              .then(function(response){\n" +
            "                  return response.json();\n" +
            "              })\n" +
            "              .then(function(data){\n" +
            "                if(data.sku.includes(productSKU)){\n" +
            "               document.getElementById(tryonButtonId).style.display = 'block';\n" +
            "                }else{\n" +
            "                  document.getElementById(tryonButtonId).style.display = 'none';\n" +
            "                  console.log(productSKU, \"this sku doesn't exist on camweara dashboard\")\n" +
            "                }\n" +
            "                  \n" +
            "              })\n" +
            "        \n" +
            "        }\n" +
            "\n" +
            "</script>\n" +
            "\n" +
            "</body>\n" +
            "</html>\n"


    @JavascriptInterface
    fun showToast() {
        //Toast.makeText(this, "Back button", Toast.LENGTH_SHORT).show()
        finish()
    }
    var isNo = 0
   // var isNo = 0
    var skus = ""
    var companyName = ""
    var message=""
    private lateinit var theWebPage: WebView



    fun check_skus(companyName: String,skus:String, myCallback: (result: Boolean?) -> Unit) {

      //  skus = "demo_1020054"  //intent.getStringExtra("skus").toString().trim()
       //intent.getStringExtra("message").toString().trim()
        val strs = skus?.split(",")

        thread {
            val result = try {
                URL("https://camweara-customers.s3.ap-south-1.amazonaws.com/$companyName/Teststore_tryonbutton.json").readText()
            } catch (e: Exception) {
                return@thread
            }
            var obj = JSONObject(result)
            var sessionArray: JSONArray = obj.optJSONArray("sku")
            System.out.println("obj1: $sessionArray")
            var firstObject = sessionArray[0]
            System.out.println("obj1: $firstObject")


            val jsonObj = JSONObject(
                result.substring(
                    result.indexOf("{"),
                    result.lastIndexOf("}") + 1
                )
            )
            val skusJson = jsonObj.getJSONArray("sku")
            var language = arrayOf(skusJson)

            /////var .
            // isNo = 0

            var arrayString = strs;


            for (i in 0 until sessionArray.length()) {

                var item = sessionArray[i];
                for (skuItem in arrayString) {

                    println(item)
                    if (item.toString().equals(skuItem)) {
                        isNo = isNo + 1;
                    }
                }
            }

            if (isNo == 0) {
                myCallback.invoke(false)

            } else {
                myCallback.invoke(true)
            }
        }}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        skus = intent.getStringExtra("skus").toString().trim()
        companyName = intent.getStringExtra("companyName").toString().trim()
        message = intent.getStringExtra("message").toString().trim()
        val strs = skus?.split(",")
        theWebPage = WebView(this)

        thread {
            val result = try {
                URL("https://camweara-customers.s3.ap-south-1.amazonaws.com/$companyName/Teststore_tryonbutton.json").readText()
            } catch (e: Exception) {
                return@thread
            }
            runOnUiThread {
                var obj = JSONObject(result)
                var sessionArray: JSONArray = obj.optJSONArray("sku")
                System.out.println("obj1: $sessionArray")
                var firstObject = sessionArray[0]
                System.out.println("obj1: $firstObject")


                val jsonObj = JSONObject(
                    result.substring(
                        result.indexOf("{"),
                        result.lastIndexOf("}") + 1
                    )
                )
                val skusJson = jsonObj.getJSONArray("sku")
                var language = arrayOf(skusJson)

                var isNo = 0

                var arrayString = strs;


                for (i in 0 until sessionArray.length()) {

                    var item = sessionArray[i];
                    for (skuItem in arrayString) {
                        println(item)
                        if (item.toString().equals(skuItem)) {
                            showwebview();
                            isNo = isNo + 1;
                        }
                    }
                }
                if (isNo == 0) {
                    Toast.makeText(
                        this,
                        "sku not present on Camweara dashboard",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
              }
            }
        }
   }


    fun showwebview() {

        theWebPage.settings.javaScriptEnabled = true
        theWebPage.settings.pluginState = WebSettings.PluginState.ON
        theWebPage.setWebChromeClient(object : WebChromeClient() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        })
        theWebPage.loadDataWithBaseURL(
            "https://cdn.camweara.com/",
            text,
            "text/html",
            "UTF-8",
            null
        )

        theWebPage.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView, weburl: String) {
                var url = "javascript:onTryonClick('$skus', '$companyName');"
                theWebPage.loadUrl(
                    url
                )
                // theWebPage.loadUrl("javascript:onTryonClick('earring6');")
            }
        })
        setContentView(theWebPage)

        theWebPage.addJavascriptInterface(this, "Android")
    }
}
