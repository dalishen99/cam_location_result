package com.hjg.base.manager;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.hjg.base.util.IntentUtils;
import com.hjg.base.util.log.androidlog.L;
import com.hjg.base.view.dialog.LoadingDialog;


/**
 * Created by Administrator on 2017/9/29 0029.
 */

public class WebViewManage {
    private WebView mWebView;
    private Context context;
    private WebSettings settings;
    private MyWebChromeClient mWebChromeClient;
    private static final String TAG = "WebViewManage";

    private LoadingDialog loadingDialog;

    public WebViewManage(Context context, WebView mWebView) {
        this.context = context;
        this.mWebView = mWebView;
        initWebSettings();
        loadingDialog = new LoadingDialog(context);
    }

    private void initWebSettings() {
        mWebChromeClient = new MyWebChromeClient();
        mWebView.setWebChromeClient(mWebChromeClient);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);// 去掉底部和右边的滚动条
        mWebView.setWebViewClient(new MyWebViewClient());
        // 设置webview
        settings = mWebView.getSettings();
        //是否显示网页缩放按钮
        settings.setBuiltInZoomControls(false);

        settings.setAllowContentAccess(true); // 是否可访问Content Provider的资源，默认值 true
        settings.setAllowFileAccess(true);    // 是否可访问本地文件，默认值 true
        settings.setAllowFileAccessFromFileURLs(true);
        // 是否允许通过file url加载的Javascript读取全部资源(包括文件,http,https)，默认值 false
        settings.setAllowUniversalAccessFromFileURLs(true);

        //可能的话使所有列的宽度不超过屏幕宽度，NORMAL  、  SINGLE_COLUMN
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        //设置加载进来的页面自适应手机屏幕
        settings.setUseWideViewPort(true);
        //// 缩放至屏幕的大小
        settings.setLoadWithOverviewMode(true);
        settings.setSavePassword(true);
        settings.setSaveFormData(true);
        //打开javasc
        settings.setJavaScriptEnabled(true);
        //允许网页获取地理位置
        settings.setGeolocationEnabled(true);
        settings.setGeolocationDatabasePath("/data/data/org.itri.html5webview/databases/");
        settings.setDomStorageEnabled(true);
        //网页加载速度慢，将图片阻塞，等到页面加载结束之后再加载图片 加载图片放在最后加载渲染
//        settings.setBlockNetworkImage(true);
        settings.setNeedInitialFocus(false);// 设置是否可以访问文件

        settings.setJavaScriptEnabled(true);//允许使用js
        settings.setDomStorageEnabled(true);
        settings.setSupportMultipleWindows(true);//允许开发多个窗口
        settings.setJavaScriptCanOpenWindowsAutomatically(true); //设置允许JS弹窗



    }


    private class MyWebChromeClient extends WebChromeClient {
        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            L.d(TAG, "here in on ShowCustomView");
        }

        @Override
        public void onHideCustomView() {
            L.d(TAG, "set it to webVew");
        }

        // Html中，视频（video）控件在没有播放的时候将给用户展示一张“海报”图片（预览图）。
//        其预览图是由Html中video标签的poster属性来指定的。如果开发者没有设置poster属性, 则可以通过这个方法来设置默认的预览图。
//        @Override
//        public Bitmap getDefaultVideoPoster() {
//            Log.i(TAG, "here in on getDefaultVideoPoster");
//            if (mDefaultVideoPoster == null) {
//                mDefaultVideoPoster = BitmapFactory.decodeResource(
//                        getResources(), R.drawable.default_video_poster);
//            }
//            return mDefaultVideoPoster;
//        }


        //    播放视频时，在第一帧呈现之前，需要花一定的时间来进行数据缓冲。ChromeClient可以使用这个函数来提供一个在数据缓冲时显示的视图。
//      例如,ChromeClient可以在缓冲时显示一个转轮动画。
//        @Override
//        public View getVideoLoadingProgressView() {
//            Log.i(TAG, "here in on getVideoLoadingPregressView");
//            return mVideoProgressView;
//        }

        //获得所有访问历史项目的列表，用于链接着色。
        @Override
        public void getVisitedHistory(ValueCallback<String[]> callback) {
            L.d(TAG, "callback:" + callback);
            super.getVisitedHistory(callback);
        }


        @Override
        public void onReceivedTitle(WebView view, String title) {
//            ((Activity) context).setTitle(title);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
//            ((Activity) context).getWindow().setFeatureInt(Window.FEATURE_PROGRESS, newProgress * 100);
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }


        /*监听input file字段*/
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> uploadMsg, FileChooserParams fileChooserParams) {
            String[] acceptTypes = fileChooserParams.getAcceptTypes();
            L.d(acceptTypes);

            if (onPicChooserListener != null) {
                onPicChooserListener.onCapture(webView, uploadMsg, fileChooserParams);
            }
            return true;

//            uploadMsg.onReceiveValue();
//            return super.onShowFileChooser(webView, uploadMsg, fileChooserParams);

        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                request.grant(request.getResources());
                request.getOrigin();
            }
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.i(TAG, "shouldOverrideUrlLoading=====" + url);
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            //开启等待层
            loadingDialog.show();
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            //关闭等待层
            loadingDialog.dismiss();
            settings.setBlockNetworkImage(false);
            super.onPageFinished(view, url);
        }

    }


    /**
     * 添加javascript插件
     *
     * @param plugin
     * @param name
     */
    @SuppressLint("JavascriptInterface")
    public void addJavascriptInterface(Object plugin, String name) {
        mWebView.addJavascriptInterface(plugin, name);
    }

    public void loadUrl(String url) {
        mWebView.loadUrl(url);
    }


    private OnPicChooserListener onPicChooserListener;

    /**
     * h5使用 <input type="file" id="pic" name="camera" accept="image/*" 调用相机拍照
     */
    public interface OnPicChooserListener {
        void onCapture(WebView webView, ValueCallback<Uri[]> uploadMsg, WebChromeClient.FileChooserParams fileChooserParams);
    }

    public void setOnPicChooserListener(OnPicChooserListener onPicChooserListener) {
        this.onPicChooserListener = onPicChooserListener;
    }

}