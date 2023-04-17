package com.hlj.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.webkit.*
import com.amap.api.maps.AMap
import com.amap.api.maps.AMap.OnMarkerClickListener
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.hlj.adapter.FylzAdapter
import com.hlj.common.CONST
import com.hlj.dto.WeatherStaticsDto
import com.hlj.utils.CommonUtil
import com.hlj.utils.OkHttpUtil
import com.hlj.view.wheelview.NumericWheelAdapter
import com.hlj.view.wheelview.OnWheelScrollListener
import com.hlj.view.wheelview.WheelView
import kotlinx.android.synthetic.main.activity_fylz.*
import kotlinx.android.synthetic.main.layout_date.*
import kotlinx.android.synthetic.main.layout_marker_statistic.view.*
import kotlinx.android.synthetic.main.layout_title.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import shawn.cxwl.com.hlj.R
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * 负氧离子检测
 */
class FylzActivity : BaseActivity(), OnClickListener, OnMarkerClickListener {

    private var aMap: AMap? = null
    private val zoom = 5.5f
    private val dataList: ArrayList<WeatherStaticsDto> = ArrayList()
    private val markers: MutableList<Marker> = ArrayList()
    private var mAdapter: FylzAdapter? = null
    private var startTime = ""
    private val sdf1 = SimpleDateFormat("yyyyMMddHH", Locale.CHINA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fylz)
        initMap(savedInstanceState)
        initWidget()
        initWheelView()
        initListView()
        initWebView()
    }

    /**
     * 初始化控件
     */
    private fun initWidget() {
        llBack!!.setOnClickListener(this)
        clTime.setOnClickListener(this)
        tvMap.setOnClickListener(this)
        tvList.setOnClickListener(this)
        tvNegtive.setOnClickListener(this)
        tvPositive.setOnClickListener(this)

        val title = intent.getStringExtra(CONST.ACTIVITY_NAME)
        if (title != null) {
            tvTitle!!.text = title
        }
        startTime = sdf1.format(Date())
    }

    /**
     * 初始化地图
     */
    private fun initMap(bundle: Bundle?) {
        mapView!!.onCreate(bundle)
        if (aMap == null) {
            aMap = mapView!!.map
        }
        aMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(CONST.guizhouLatLng, zoom))
        aMap!!.mapType = AMap.MAP_TYPE_SATELLITE
        aMap!!.uiSettings.isZoomControlsEnabled = false
        aMap!!.uiSettings.isRotateGesturesEnabled = false
        aMap!!.setOnMarkerClickListener(this)
        aMap!!.setOnMapLoadedListener {
            tvMapNumber.text = aMap!!.mapContentApprovalNumber
            CommonUtil.drawHLJJson(this, aMap)
            okHttpList()
        }
    }

    private fun initListView() {
        mAdapter = FylzAdapter(this, dataList)
        listView.adapter = mAdapter
        listView.setOnItemClickListener { parent, view, position, id ->
            val data = dataList[position]
            val intent = Intent(this, WebviewActivity::class.java)
            intent.putExtra(CONST.ACTIVITY_NAME, data.name)
            intent.putExtra(CONST.WEB_URL, "http://decision-admin.tianqi.cn/Public/htmls/hlj/fylz/detail.html?code=${data.stationId}")
            startActivity(intent)
        }
    }

    private fun okHttpList() {
        showDialog()
        Thread {
            val url = "http://hlj-wx.tianqi.cn/Public/hlj_gasa?type=1"
            OkHttpUtil.enqueue(Request.Builder().url(url).build(), object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        return
                    }
                    val result = response.body!!.string()
                    runOnUiThread {
                        cancelDialog()
                        if (!TextUtils.isEmpty(result)) {
                            try {
                                dataList.clear()
                                val array = JSONArray(result)
                                for (i in 0 until array.length()) {
                                    val dto = WeatherStaticsDto()
                                    val itemObj = array.getJSONObject(i)
                                    if (!itemObj.isNull("name")) {
                                        dto.name = itemObj.getString("name")
                                    }
                                    if (!itemObj.isNull("code")) {
                                        dto.stationId = itemObj.getString("code")
                                    }
                                    if (!itemObj.isNull("tm")) {
                                        dto.tm = itemObj.getString("tm")
                                    }
                                    if (!itemObj.isNull("gasa")) {
                                        dto.gasa = itemObj.getString("gasa")
                                    }
                                    if (!itemObj.isNull("lat")) {
                                        dto.latitude = itemObj.getString("lat")
                                    }
                                    if (!itemObj.isNull("lon")) {
                                        dto.longitude = itemObj.getString("lon")
                                    }
                                    dataList.add(dto)
                                }
                                addMarkers()
                                if (mAdapter != null) {
                                    mAdapter!!.notifyDataSetChanged()
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            })
        }.start()
    }

    private fun removeMarkers() {
        for (i in markers.indices) {
            val marker = markers[i]
            marker.remove()
        }
        markers.clear()
    }

    /**
     * 添加marker
     */
    private fun addMarkers() {
        removeMarkers()
        if (dataList.isEmpty()) {
            return
        }
        for (i in dataList.indices) {
            val dto = dataList[i]
            val lat = java.lang.Double.valueOf(dto.latitude)
            val lng = java.lang.Double.valueOf(dto.longitude)
            val options = MarkerOptions()
            options.title(dto.name)
            options.snippet(dto.stationId)
            options.anchor(0.5f, 1.0f)
            options.position(LatLng(lat, lng))
            options.icon(BitmapDescriptorFactory.fromView(getTextBitmap(dto.name)))
            val marker = aMap!!.addMarker(options)
            markers.add(marker)
        }
    }

    /**
     * 给marker添加文字
     * @param name 城市名称
     * @return
     */
    private fun getTextBitmap(name: String): View? {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.layout_marker_statistic, null) ?: return null
        view.tvName.text = name
        view.tvName.setBgColor(Color.YELLOW)
        return view
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val intent = Intent(this, WebviewActivity::class.java)
        intent.putExtra(CONST.ACTIVITY_NAME, marker.title)
        intent.putExtra(CONST.WEB_URL, "http://decision-admin.tianqi.cn/Public/htmls/hlj/fylz/detail.html?code=${marker.snippet}")
        startActivity(intent)
        return true
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.llBack -> finish()
            R.id.tvMap -> {
                tvMap.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                tvList.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                webView.visibility = View.GONE
//                clTime.visibility = View.GONE
//                listTitle.visibility = View.GONE
//                listView.visibility = View.GONE
            }
            R.id.tvList -> {
                tvMap.setTextColor(ContextCompat.getColor(this, R.color.text_color4))
                tvList.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                webView.visibility = View.VISIBLE
//                clTime.visibility = View.VISIBLE
//                listTitle.visibility = View.VISIBLE
//                listView.visibility = View.VISIBLE
            }
            R.id.clTime -> bootTimeLayoutAnimation()
            R.id.tvNegtive -> bootTimeLayoutAnimation()
            R.id.tvPositive -> {
                setTextViewValue()
                bootTimeLayoutAnimation()
            }
        }
    }

    private fun initWheelView() {
        val c = Calendar.getInstance()
        val curYear = c[Calendar.YEAR]
        val curMonth = c[Calendar.MONTH] + 1 //通过Calendar算出的月数要+1
        val curDate = c[Calendar.DATE]
        val curHour = c[Calendar.HOUR_OF_DAY]

        year.visibility = View.VISIBLE
        month.visibility = View.VISIBLE
        day.visibility = View.VISIBLE
        hour.visibility = View.VISIBLE

        val numericWheelAdapter1 = NumericWheelAdapter(this, 1950, curYear)
        numericWheelAdapter1.setLabel("年")
        year.viewAdapter = numericWheelAdapter1
        year.isCyclic = false //是否可循环滑动
        year.addScrollingListener(scrollListener)
        year.visibleItems = 7

        val numericWheelAdapter2 = NumericWheelAdapter(this, 1, 12, "%02d")
        numericWheelAdapter2.setLabel("月")
        month.viewAdapter = numericWheelAdapter2
        month.isCyclic = false
        month.addScrollingListener(scrollListener)
        month.visibleItems = 7

        initDay(curYear, curMonth)
        day.isCyclic = false
        day.visibleItems = 7

        val numericWheelAdapter3 = NumericWheelAdapter(this, 0, 23, "%02d")
        numericWheelAdapter3.setLabel("时")
        hour.viewAdapter = numericWheelAdapter3
        hour.isCyclic = false
        hour.addScrollingListener(scrollListener)
        hour.visibleItems = 7

        year.currentItem = curYear - 1950
        month.currentItem = curMonth - 1
        day.currentItem = curDate - 1
        hour.currentItem = curHour
    }

    private val scrollListener: OnWheelScrollListener = object : OnWheelScrollListener {
        override fun onScrollingStarted(wheel: WheelView) {}
        override fun onScrollingFinished(wheel: WheelView) {
            val nYear = year!!.currentItem + 1950 //年
            val nMonth: Int = month.currentItem + 1 //月
            initDay(nYear, nMonth)
        }
    }

    /**
     */
    private fun initDay(arg1: Int, arg2: Int) {
        val numericWheelAdapter = NumericWheelAdapter(this, 1, getDay(arg1, arg2), "%02d")
        numericWheelAdapter.setLabel("日")
        day.viewAdapter = numericWheelAdapter
    }

    /**
     *
     * @param year
     * @param month
     * @return
     */
    private fun getDay(year: Int, month: Int): Int {
        var day = 30
        var flag = false
        flag = when (year % 4) {
            0 -> true
            else -> false
        }
        day = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            2 -> if (flag) 29 else 28
            else -> 30
        }
        return day
    }

    /**
     */
    private fun setTextViewValue() {
        val yearStr = (year!!.currentItem + 1950).toString()
        val monthStr = if (month.currentItem + 1 < 10) "0" + (month.currentItem + 1) else (month.currentItem + 1).toString()
        val dayStr = if (day.currentItem + 1 < 10) "0" + (day.currentItem + 1) else (day.currentItem + 1).toString()
        val hourStr = if (hour.currentItem + 1 < 10) "0" + (hour.currentItem) else (hour.currentItem).toString()
        tvStartTime.text = "${yearStr}年${monthStr}月${dayStr}日 ${hourStr}时"
        startTime = "${yearStr}${monthStr}${dayStr}${hourStr}"
    }

    private fun bootTimeLayoutAnimation() {
        if (layoutDate!!.visibility == View.GONE) {
            timeLayoutAnimation(true, layoutDate)
            layoutDate!!.visibility = View.VISIBLE
        } else {
            timeLayoutAnimation(false, layoutDate)
            layoutDate!!.visibility = View.GONE
        }
    }

    /**
     * 时间图层动画
     * @param flag
     * @param view
     */
    private fun timeLayoutAnimation(flag: Boolean, view: View?) {
        //列表动画
        val animationSet = AnimationSet(true)
        val animation: TranslateAnimation = if (!flag) {
            TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 1f)
        } else {
            TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 0f,
                    Animation.RELATIVE_TO_SELF, 1f,
                    Animation.RELATIVE_TO_SELF, 0f)
        }
        animation.duration = 200
        animationSet.addAnimation(animation)
        animationSet.fillAfter = true
        view!!.startAnimation(animationSet)
        animationSet.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(arg0: Animation) {}
            override fun onAnimationRepeat(arg0: Animation) {}
            override fun onAnimationEnd(arg0: Animation) {
                view.clearAnimation()
            }
        })
    }

    /**
     * 初始化webview
     */
    private fun initWebView() {
        val url = "http://decision-admin.tianqi.cn/Public/htmls/hlj/fylz/"
        val webSettings = webView!!.settings
        //支持javascript

        //支持javascript
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.domStorageEnabled = true
        webSettings.setGeolocationEnabled(true)
        // 设置可以支持缩放
        webSettings.setSupportZoom(true)
        // 设置出现缩放工具
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        //扩大比例的缩放
        webSettings.useWideViewPort = true
        //自适应屏幕
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        webSettings.loadWithOverviewMode = true
        webView!!.loadUrl(url)

        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String) {
                super.onReceivedTitle(view, title)
            }
        }
        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                callback.invoke(origin, true, false)
            }
        }

        webView!!.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, itemUrl: String): Boolean {
                webView!!.loadUrl(itemUrl)
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
            }
        }
    }

    /**
     * 方法必须重写
     */
    override fun onResume() {
        super.onResume()
        if (mapView != null) {
            mapView!!.onResume()
        }
    }

    /**
     * 方法必须重写
     */
    override fun onPause() {
        super.onPause()
        if (mapView != null) {
            mapView!!.onPause()
        }
    }

    /**
     * 方法必须重写
     */
    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (mapView != null) {
            mapView!!.onSaveInstanceState(outState)
        }
    }

    /**
     * 方法必须重写
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mapView != null) {
            mapView!!.onDestroy()
        }
    }

}
