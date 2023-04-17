package com.supremehyo.locationsns.View

import android.annotation.TargetApi
import android.content.Intent
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.supremehyo.locationsns.R
import kotlinx.android.synthetic.main.activity_user_address.*
class UserAddressActivity : AppCompatActivity() {


    inner class MyJavaScriptInterface {
        @JavascriptInterface @SuppressWarnings("unused")
        fun  processDATA( data :String) {
            var extra =  Bundle()
            var intent = Intent()
            intent.putExtra("data", data);
            setResult(RESULT_OK, intent);
            finish()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_address)


        wv_search_address.getSettings().setJavaScriptEnabled(true);
        wv_search_address.getSettings().setDomStorageEnabled(true);
        wv_search_address.addJavascriptInterface( MyJavaScriptInterface(), "Android");
        wv_search_address.setWebViewClient(object : WebViewClient() {
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                handler?.proceed()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    view?.loadUrl(url)
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                web_progress.setVisibility(View.GONE);
                wv_search_address.loadUrl("javascript:sample2_execDaumPostcode();");
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                web_progress.setVisibility(View.VISIBLE);

            }
        })

        wv_search_address.webChromeClient =object : WebChromeClient(){
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        wv_search_address.loadUrl("http://54.180.138.77/static/client/daum_address.html")
    }





}