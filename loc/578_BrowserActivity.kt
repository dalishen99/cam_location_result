package com.ryuxing.bubblebrowser

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.*
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.ryuxing.bubblebrowser.bookmark.Bookmark
import com.ryuxing.bubblebrowser.bookmark.BookmarkDB
import com.ryuxing.bubblebrowser.bookmark.BookmarkViewModel
import com.ryuxing.bubblebrowser.bookmark.BookmarkViewModelFactory


class BrowserActivity : AppCompatActivity() {
    lateinit var webView: WebView
    lateinit var shortcutId:String
    lateinit var notification:BubbleNotification
    lateinit var iconBitmap:Bitmap
    var bookmarks: List<Bookmark> = listOf()
    private val bookmarkViewModel : BookmarkViewModel by viewModels{
        BookmarkViewModelFactory((application as App).repository)
    }
    lateinit var geolocationErrorString: String
    var isLocationGranted=false
    var locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
            } else -> {
            // No location access granted.
                Toast.makeText(this@BrowserActivity,R.string.geolocation_permission_denied,Toast.LENGTH_SHORT)
        }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notification = BubbleNotification(this)
        shortcutId = intent.extras?.getString("shortcutId")?:""
        Log.d("shortcutId",shortcutId)
        if(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                !this.isLaunchedFromBubble
            } else shortcutId == ""
        ){
            this.finishAndRemoveTask()
            return
        }
        setContentView(R.layout.activity_browser)
        val url:String = intent.extras?.getString("url") ?:"https://google.com"
        Log.v("url", url)
        val actionBar = supportActionBar
        if(actionBar!=null){
            actionBar.title = "Bubble Browser"
        }
        webView = findViewById(R.id.webView)
        val settings = webView.settings
        settings.allowContentAccess = true
        settings.javaScriptEnabled = true
        settings.setGeolocationEnabled(true)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if(actionBar!=null) actionBar.title = url.toString()
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if(actionBar!=null) actionBar.title = webView.title
                isLocationGranted = false
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                return false
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                super.onReceivedIcon(view, icon)
                Log.d("IconReceive",icon.toString())
                notification.sendNotification(webView.url?:url,shortcutId,true, icon?.let {
                    IconCompat.createWithBitmap(
                        it
                    )
                }
                    ?:null, title = webView.title.toString())
                iconBitmap = icon ?:getDrawable(R.drawable.ic_baseline_language_24)!!.toBitmap(96,96)
            }
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback
            ) {
                if(isLocationGranted) callback.invoke(origin, true, false)
                else showGeolocationPrompt(origin,callback)
            }
        }

        Log.v("url", url)
        iconBitmap = getDrawable(R.drawable.ic_baseline_language_24)!!.toBitmap(96,96)
        webView.loadUrl(url)
        geolocationErrorString = getString(R.string.geolocation_permission_denied)

        bookmarkViewModel.bookmarks.observe(this){
            bookmarks = it
            invalidateOptionsMenu()
            Log.d("BOOKMARK_OBSERVE_Browser",it.toString())
        }

    }

    private fun showGeolocationPrompt(origin: String?, callback: GeolocationPermissions.Callback) {
        val string = getString(R.string.ask_geolocation,origin?:"")
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.geolocation_title)
            .setMessage(string)
            .setPositiveButton(R.string.button_allow) { _, _ ->
                isLocationGranted = true
                if (checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_DENIED) {
                    locationPermissionRequest.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
                callback.invoke(origin, true, false)
            }
            .setNegativeButton(R.string.button_block) { _, _ ->
                callback.invoke(origin, false, false)
            }
        dialog.show()
    }

    override fun onDestroy() {
        if(shortcutId!=""){
            ShortcutManagerCompat.removeLongLivedShortcuts(this, listOf(shortcutId))
            ShortcutManagerCompat.removeDynamicShortcuts(this, listOf(shortcutId))
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_browser, menu)
        val subMenu = menu?.addSubMenu(R.string.menu_open_bookmark)
        for (bookmark in bookmarks) {
            subMenu?.add(
                1000,
                1000 + bookmark.id,
                Menu.CATEGORY_CONTAINER,
                bookmark.title
            )

        }
        menu?.add(1,-1,Menu.CATEGORY_SYSTEM,R.string.menu_close)
        return super.onCreateOptionsMenu(menu)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId){
            R.id.menu_item_back ->{
                webView.goBack()
                true
            }
            R.id.menu_item_forward ->{
                webView.goBackOrForward(1)
                true
            }
            R.id.menu_item_reload ->{
                webView.reload()
                true
            }
            R.id.menu_item_open_with ->{
                startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(webView.url)))
                true
            }
            R.id.menu_item_add_bookmark ->{
                val view_title = EditText(this)
                val view_URL = EditText(this)
                view_title.setText(webView.title)
                view_title.hint = getString(R.string.add_bookmark_title_hint)
                val dialog = AlertDialog.Builder(this)
                    .setTitle(R.string.menu_add_bookmarks)
                    .setMessage("URL: ${webView.url}")
                    .setIcon(iconBitmap.toDrawable(resources))
                    .setView(view_title)
                    //.setView(view_URL)
                    .setPositiveButton(R.string.add_bookmark_popup_save) { dialogInterface, i ->
                        bookmarkViewModel.add(
                            Bookmark(
                                0,
                                view_title.text.toString(),
                                webView.url.toString(),
                                iconBitmap
                            )
                        )
                        Log.d("row", iconBitmap.rowBytes.toString())
                        Toast.makeText(this, R.string.add_bookmark_toast_saved, Toast.LENGTH_SHORT)
                    }
                dialog.show()

                true
            }
            -1 ->{
                notification.cancelNotification(shortcutId)
                true
            }
            else ->{
                if(item.itemId<1000)super.onOptionsItemSelected(item)
                else{
                    val bookmarks= BookmarkDB.getOne(item.itemId-1000)
                    if(bookmarks.isEmpty())  super.onOptionsItemSelected(item)
                    webView.loadUrl(bookmarks[0].url)

                    true
                }
            }
        }
    }

    override fun onBackPressed() {
        if(webView.canGoBack())webView.goBack()
        else{
            super.onBackPressed()
            notification.cancelNotification(shortcutId)
        }

    }
}