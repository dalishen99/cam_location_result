package com.example.amarbatch.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.amarbatch.R
import com.example.amarbatch.databinding.FragmentWebViewBinding
import com.example.amarbatch.utils.Constant
import com.example.amarbatch.utils.FusedLocation
import com.example.amarbatch.viewmodel.LocationViewModel
import com.google.gson.JsonObject


var lat:Double = 0.0
var long:Double = 0.0
class WebView : Fragment() {
    private lateinit var v: FragmentWebViewBinding
    @SuppressLint("SetJavaScriptEnabled")
    var handler: Handler = Handler()
    var runnable: Runnable? = null
    private var delay = 10000
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        v = FragmentWebViewBinding.inflate(inflater, container, false)
        v.apply {
            webView.webViewClient = WebViewClient()
            webView.loadUrl(Constant.AMAR_BACH_APEX_MAP_PAGE_URL)
            webView.settings.javaScriptEnabled = true
            webView.settings.setGeolocationEnabled(true)
            webView.settings.setSupportZoom(true)

            webView.webChromeClient = object : WebChromeClient(){
                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback?
                ) {
                    showPermissionDialog(origin,callback)
                }
            }
        }
        return v.root
    }

    private fun showPermissionDialog(origin: String?, callback: GeolocationPermissions.Callback?) {
        val permissionDialog = AlertDialog.Builder(requireContext())
            .setIcon(R.drawable.ic_baseline_not_listed_location_24)
            .setTitle("Location Permission")
            .setMessage("Need to access your location. Allow access?\n\n " +
                    "->CLICK RIGHT CORNER BELOW")
            .setPositiveButton("Allow") { _, _ ->
                callback!!.invoke(origin, true, false)
            }
            .setNegativeButton("Deny") { _, _ ->
                callback!!.invoke(origin, false, false)
            }
            .create()
        permissionDialog.show()
        permissionDialog.setCancelable(false)
    }


    override fun onResume() {
        handler.postDelayed(Runnable {
            handler.postDelayed(runnable!!, delay.toLong())
            getLatLong()
        }.also { runnable = it }, delay.toLong())
        super.onResume()
    }
    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable!!)
    }
    private  fun getLatLong()
    {
            val location = FusedLocation.getLocation(requireContext())
            lat = location.first
            long = location.second
            Log.d("nlogLatitude,Longitude", "Lat: ${location.first}, Long: ${location.second}")
            val viewModel: LocationViewModel = ViewModelProvider(this)[LocationViewModel::class.java]

            //val random = (90..100).random()
            val jsonObj = JsonObject()
            jsonObj.addProperty("id",61)
            jsonObj.addProperty("lat", lat)
            jsonObj.addProperty("long", long)
            viewModel.updateBusLocation(jsonObj)
            viewModel.updateBusLocationLiveData.observe(viewLifecycleOwner){
                if (it.RESULT_CODE  == "0"){
                    Log.e("nlogIsUpdateLocation",it.MESSAGE.toString())
                }
                if (it == null){
                    Log.e("nlogError","failed")
                }
            }
    }
}