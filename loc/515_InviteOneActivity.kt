package com.huotu.android.mifang.activity

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.provider.MediaStore
import android.provider.SyncStateContract
import android.security.keystore.KeyNotYetValidException
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.View
import android.webkit.*
import com.huotu.android.mifang.R
import com.huotu.android.mifang.base.BaseActivity
import com.huotu.android.mifang.bean.Constants
import com.huotu.android.mifang.bean.IdId
import com.huotu.android.mifang.bean.KeyValue
import com.huotu.android.mifang.bean.ShareBean
import com.huotu.android.mifang.mvp.IPresenter
import com.huotu.android.mifang.newIntent
import com.huotu.android.mifang.util.ImageUtils
import com.huotu.android.mifang.util.WechatShareUtil
import com.huotu.android.mifang.utils.UrlInterceptor
import com.huotu.android.mifang.widget.ShareDialog
import com.huotu.android.mifang.widget.TipAlertDialog
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloadQueueSet
import com.liulishuo.filedownloader.FileDownloader
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import kotlinx.android.synthetic.main.activity_invite_one.*
import kotlinx.android.synthetic.main.activity_web.*
import kotlinx.android.synthetic.main.layout_header.*
import java.io.File
import java.util.*

class InviteOneActivity : BaseActivity<IPresenter>()
        ,ShareDialog.OnOperateListener
        ,View.OnClickListener {
    val urlIntercepter : UrlInterceptor = UrlInterceptor(this)
    var url:String?=null
    var type :Int=1
    var wechatShareUtil=WechatShareUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_invite_one)

        header_title.text="邀请入驻"
        header_left_image.setOnClickListener(this)
        invite_one_buy.setOnClickListener(this)

        initWebView()

        if(intent.hasExtra(Constants.INTENT_URL)){
            url = intent.getStringExtra(Constants.INTENT_URL)
            invite_one_webView.loadUrl(url)
        }
        if(intent.hasExtra(Constants.INTENT_OPERATE_TYPE)){
            type = intent.getIntExtra(Constants.INTENT_OPERATE_TYPE,1)
        }

        if(type == 1){
            invite_one_buy.text="购买开店账号"
            invite_one_buy.setBackgroundColor( ContextCompat.getColor(this , R.color.bgcolor))
        }else if(type ==2){
            invite_one_buy.text = "申请代理商"
            invite_one_buy.setBackgroundColor( ContextCompat.getColor( this , R.color.bgcolor2))
        }else if(type ==3){
            invite_one_buy.text="转发邀请函"
            invite_one_buy.setBackgroundColor(ContextCompat.getColor(this,R.color.bgcolor2))
        }
    }

    private fun initWebView(){
        invite_one_webView.webViewClient =
                object : WebViewClient() {

                    override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
                        if (url != null && url.startsWith("tel:")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                            return true
                        }

                        return if (urlIntercepter.shouldOverrideUrlLoading(view, url)) {
                            true
                        } else {
                            super.shouldOverrideUrlLoading(view, url)
                        }
                    }


                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (view.url != null && view.url.startsWith("tel:")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(view.url))
                            startActivity(intent)
                            return true
                        }

                        return if (urlIntercepter.shouldOverrideUrlLoading(view, url)) {
                            true
                        } else {
                            super.shouldOverrideUrlLoading(view, request)
                        }

                        //return super.shouldOverrideUrlLoading(view, request)
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)

                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (header_title == null) return
                        header_title.text =view?.title
                    }


                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        if (web_progressBar == null) return
                        web_progressBar.visibility = View.GONE
                    }


                    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                        super.onReceivedSslError(view, handler, error)
                        //当访问https网页，发生错误时，继续浏览网页
                        handler?.proceed()
                    }

                }


        invite_one_webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView, title: String?) {
                super.onReceivedTitle(view, title)
                if (header_title == null) {
                    return
                }
                if (title == null) {
                    return
                }
                header_title.text =title
            }

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if ( web_progressBar == null) return
                if (100 == newProgress) {
                    web_progressBar.visibility =View.GONE
                } else {
                    if (web_progressBar.visibility == View.GONE) web_progressBar.visibility = View.VISIBLE
                    web_progressBar.progress = newProgress
                }
                super.onProgressChanged(view, newProgress)
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
//                mUploadMessage = uploadMsg
//                val i = Intent(Intent.ACTION_GET_CONTENT)
//                i.addCategory(Intent.CATEGORY_OPENABLE)
//                i.type = "*/*"
//                this@WebActivity.startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE)
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String) {
                openFileChooser(uploadMsg)
            }

            //For Android 4.1
            fun openFileChooser(uploadMsg: ValueCallback<Uri>, acceptType: String, capture: String) {
                openFileChooser(uploadMsg)
            }

            override fun onShowFileChooser(webView: WebView, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
            }

            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                callback.invoke(origin, true, false)
                super.onGeolocationPermissionsShowPrompt(origin, callback)
            }

            override fun onJsConfirm(view: WebView?, url: String, message: String, result: JsResult): Boolean {
                //return super.onJsConfirm(view, url, message, result);
                if (view == null || view.context == null) return true

                val tipAlertDialog = TipAlertDialog(view.context, false)
                tipAlertDialog.show("询问", message, View.OnClickListener {
                    tipAlertDialog.dismiss()
                    result.cancel()
                }, View.OnClickListener {
                    tipAlertDialog.dismiss()
                    result.confirm()
                })

                return true
            }
        }

    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.header_left_image->{finish()}
            R.id.invite_one_buy->{
                operate()

//                var bgUrl= intent.getStringExtra(Constants.INTENT_URL1)
//                var qrCodeUrl = intent.getStringExtra(Constants.INTENT_URL2)
//                downloadImages(bgUrl , qrCodeUrl, -1)
            }
        }
    }

    fun operate(){
        if(type==1){
            newIntent<BuyActivity>(Constants.INTENT_OPERATE_TYPE , 0)
        }else if(type==2){
            newIntent<ApplyAgentActivity>()
        }else if(type==3){
            share()
        }
    }

    fun share(){

//        var kv=ArrayList<KeyValue>()
//        kv.add(KeyValue(R.mipmap.ssdk_oks_classic_wechat,"微信好友"))
//        kv.add(KeyValue(R.mipmap.ssdk_oks_classic_wechatmoments,"微信朋友圈"))
//        kv.add(KeyValue(R.mipmap.ssdk_oks_classic_wechatfavorite,"微信收藏夹"))
//        var shareDialog=ShareDialog(this, this, kv )
//        shareDialog.show()

        var bgUrl= intent.getStringExtra(Constants.INTENT_URL1)
        var qrCodeUrl = intent.getStringExtra(Constants.INTENT_URL2)
        downloadImages(bgUrl , qrCodeUrl, -1)

    }

    override fun operate(keyValue: KeyValue) {
//        var bean = ShareBean("title","desciption",
//                "http://www.baidu.com",
//                "https://images0.cnblogs.com/news_topic/apple.png",
//                "",null,-1)
        var shareType = SendMessageToWX.Req.WXSceneSession
        if(keyValue.code == R.mipmap.ssdk_oks_classic_wechat){
            //wechatShareUtil.shareWechat(bean)
            shareType = SendMessageToWX.Req.WXSceneSession
        }else if(keyValue.code==R.mipmap.ssdk_oks_classic_wechatmoments){
            //wechatShareUtil.shareWechatMoments(bean)
            shareType = SendMessageToWX.Req.WXSceneTimeline
        }else if(keyValue.code==R.mipmap.ssdk_oks_classic_wechatfavorite){
            //wechatShareUtil.shareWechatFavorite(bean)
            shareType = SendMessageToWX.Req.WXSceneFavorite
        }
        var bgUrl= intent.getStringExtra(Constants.INTENT_URL1)
        var qrCodeUrl = intent.getStringExtra(Constants.INTENT_URL2)
        downloadImages(bgUrl , qrCodeUrl, shareType)
    }


    private fun mergePicture( bgPicturePath:String , qrCodePath:String , shareType:Int)  {

        var bgFileName = File(bgPicturePath).name
        var bgFile = File( Constants.ImageDirPath+ bgFileName)
        var qrCodeFileName = File(qrCodePath).name
        var qrCodeFile = File( Constants.ImageDirPath + qrCodeFileName)

        if( bgFile.exists() && qrCodeFile.exists() ){
            var bgBitmap = ImageUtils.getBitmap(bgFile)
            var qrCodeBitmap = ImageUtils.getBitmap(qrCodeFile)
            if(bgBitmap==null ){
                toast("打开图片失败，请重新!")
                File(bgPicturePath).delete()
                return
            }
            if(qrCodeBitmap==null){
                toast("打开图片失败，请重试!")
                File(qrCodePath).delete()
                return
            }

            var sw = bgBitmap.width
            var sh = bgBitmap.height
            var w = qrCodeBitmap.width
            var h = qrCodeBitmap.height
            var x = 0
            var y = 0
            var qrw = sw/3
            var qrh = sh/3
            if( qrw >= w ){
                x = (sw- w )/2
                y = (sh- h )/2 + h
            }else{
                x = (sw- qrw)/2
                y = (sh - qrh)/2 + qrh
            }
            var mergeBitmap = ImageUtils.addImageWatermark(bgBitmap , qrCodeBitmap , x ,y ,255 )
            var mergePicturePath = Constants.ImageDirPath+Constants.SHARE_UPGRADE_AGENT_PICTURE_NAME
            ImageUtils.save(mergeBitmap , mergePicturePath , Bitmap.CompressFormat.PNG , true )
            //( qrCodeBitmap , mergeBitmap , shareType)
            //WechatShareUtil().shareWechatOfOneImage(qrCodeBitmap , mergeBitmap ,shareType )
            share(mergePicturePath)
            return
        }else{
            toast("分享图片下载失败,请重试!")
            return
        }
    }


    private fun share( bitmapPath:String  ){

        //WechatShareUtil().shareWechatOfOneImage(thumbBitmap, bitmap ,shareType )
        var images = java.util.ArrayList<Uri>()

        var image = File(bitmapPath)
        val values = ContentValues(7)

        values.put(MediaStore.Images.Media.TITLE, image.name)

        values.put(MediaStore.Images.Media.DISPLAY_NAME, image.name)

        values.put(MediaStore.Images.Media.DATE_TAKEN, Date().time)

        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")

        values.put(MediaStore.Images.ImageColumns.BUCKET_ID, image.hashCode())

        values.put(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME, image.name)

        values.put("_data", bitmapPath)
        val uri  = ImageUtils.getUriByFile(this, bitmapPath) //Uri.fromFile(image)

        images.add( uri )

        //var image = Uri.parse( bitmapPath )
        var intent = Intent(Intent.ACTION_SEND_MULTIPLE)
        intent.type = "image/*"

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM , images )
        //intent.putExtra(Intent.EXTRA_STREAM , image)
        //intent.putExtra(Intent.EXTRA_SUBJECT, "邀请好友开店" )
        //intent.putExtra(Intent.EXTRA_TEXT, "邀请好友开店" )
        //intent.putExtra(Intent.EXTRA_TITLE, "邀请好友开店")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        var shareIntent = Intent.createChooser(intent, "分享")
        startActivity( shareIntent )
    }




    private fun downloadImages(urlbg:String? , urlqrcode:String? , shareType: Int ) {
        val type = shareType
        if (TextUtils.isEmpty(urlbg) || TextUtils.isEmpty(urlqrcode)) {
            toast("图片地址空!")
            return
        }

        var fileNameBg = File(urlbg).name
        var filePathBg = Constants.ImageDirPath + fileNameBg
        var fileBg = File(filePathBg)
        var fileNameQrcode = File(urlqrcode).name
        var filePathQrcode = Constants.ImageDirPath + fileNameQrcode
        var fileQrcode = File(filePathQrcode)
        if (fileBg.exists() && fileQrcode.exists()) {
            mergePicture(filePathBg, filePathQrcode , type )
            return
        }



        var dir = Constants.ImageDirPath
        var f = File(dir)
        if (!f.exists()) {
            f.parentFile.mkdir()
        }

        var downLoadQueueSet = FileDownloadQueueSet(object : FileDownloadListener() {
            override fun warn(task: BaseDownloadTask?) {
                hideProgress()
                toast("warn")
            }

            override fun completed(task: BaseDownloadTask?) {
                hideProgress()
                if ((task!!.tag as IdId).id == (task!!.tag as IdId).total) {
                    //toast("图片已经保存在"+dir)

                    mergePicture(filePathBg , filePathQrcode , shareType )

                }
            }

            override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                hideProgress()
            }

            override fun error(task: BaseDownloadTask?, e: Throwable?) {
                hideProgress()
                toast("error")
                e!!.printStackTrace()
            }

            override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                showProgress("")
            }

            override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                hideProgress()
            }
        })

        var tasks = ArrayList<BaseDownloadTask>()


        if(!fileBg.exists() && fileQrcode.exists()){
            var idId = IdId(1, 1)
            tasks.add(FileDownloader.getImpl().create(urlbg).setTag(1).setPath(filePathBg).setTag(idId))
        }else if( fileBg.exists() && !fileQrcode.exists()){
            var idId = IdId(1, 1)
            tasks.add(FileDownloader.getImpl().create(urlqrcode).setTag(1).setPath(filePathQrcode).setTag(idId))
        }else if( !fileBg.exists() && !fileQrcode.exists()){
            var idId = IdId(1, 2)
            tasks.add(FileDownloader.getImpl().create(urlbg).setTag(1).setPath(filePathBg).setTag(idId))
            idId = IdId(2, 2)
            tasks.add(FileDownloader.getImpl().create(urlqrcode).setTag(2).setPath(filePathQrcode).setTag(idId))
        }

        downLoadQueueSet.disableCallbackProgressTimes()
        downLoadQueueSet.setAutoRetryTimes(1)
        downLoadQueueSet.downloadSequentially(tasks)//串行下载
        downLoadQueueSet.start()
        showProgress("")
    }

}
