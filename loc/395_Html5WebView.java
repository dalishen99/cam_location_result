package com.slzhibo.library.ui.view.widget;



import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.slzhibo.library.http.utils.EncryptUtil;
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;
import java.io.File;

/* renamed from: com.slzhibo.library.ui.view.widget.Html5WebView */
/* loaded from: classes11.dex */
public class Html5WebView extends WebView {
    private boolean isLoad = false;
    private Context mContext;
    private boolean isTouchBoolean = true;

    public Html5WebView(Context context) {
        super(getFixedContext(context));
        init(context);
    }

    public Html5WebView(Context context, AttributeSet attributeSet) {
        super(getFixedContext(context), attributeSet);
        init(context);
    }

    public Html5WebView(Context context, AttributeSet attributeSet, int i) {
        super(getFixedContext(context), attributeSet, i);
        init(context);
    }

    public static Context getFixedContext(Context context) {
        int i = Build.VERSION.SDK_INT;
        return (i < 21 || i >= 23) ? context : context.createConfigurationContext(new Configuration());
    }

    private void init(Context context) {
        this.mContext = context;
        WebSettings settings = getSettings();
        settings.setAllowFileAccess(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        settings.setGeolocationEnabled(true);
        settings.setAppCacheMaxSize(Long.MAX_VALUE);
        settings.setPluginState(WebSettings.PluginState.ON_DEMAND);
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setDefaultTextEncodingName(EncryptUtil.CHARSET);
        settings.setLoadsImagesAutomatically(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        saveData(settings);
        setWebChromeClient(new BaseWebChromeClient());
        setWebViewClient(new BaseWebViewClient());
    }

    @Override // com.tencent.smtt.sdk.WebView
    public void loadUrl(String str) {
        super.loadUrl(str);
        this.isLoad = true;
    }

    public void loadDataWithUrl(String str) {
        super.loadDataWithBaseURL(null, str, "text/html", "UTF-8", null);
    }

    public void onWebViewDestroy() {
        loadDataWithUrl("");
        super.clearHistory();
        super.destroy();
    }

    public boolean isLoadBoolean() {
        return this.isLoad;
    }

    private void saveData(WebSettings webSettings) {
        webSettings.setCacheMode(-1);
        File cacheDir = this.mContext.getCacheDir();
        if (cacheDir != null) {
            String absolutePath = cacheDir.getAbsolutePath();
            webSettings.setDomStorageEnabled(true);
            webSettings.setDatabaseEnabled(true);
            webSettings.setAppCacheEnabled(true);
            webSettings.setAppCachePath(absolutePath);
        }
    }

    /* renamed from: com.slzhibo.library.ui.view.widget.Html5WebView$BaseWebViewClient */
    /* loaded from: classes11.dex */
    public static class BaseWebViewClient extends WebViewClient {
        @Override // com.tencent.smtt.sdk.WebViewClient
        public boolean shouldOverrideUrlLoading(WebView webView, String str) {
            webView.loadUrl(str);
            return true;
        }

        @Override // com.tencent.smtt.sdk.WebViewClient
        public void onReceivedSslError(WebView webView, SslErrorHandler sslErrorHandler, SslError sslError) {
            sslErrorHandler.proceed();
        }
    }

    /* renamed from: com.slzhibo.library.ui.view.widget.Html5WebView$BaseWebChromeClient */
    /* loaded from: classes11.dex */
    public static class BaseWebChromeClient extends WebChromeClient {
        @Override // com.tencent.smtt.sdk.WebChromeClient
        public void onReceivedIcon(WebView webView, Bitmap bitmap) {
            super.onReceivedIcon(webView, bitmap);
        }

        @Override // com.tencent.smtt.sdk.WebChromeClient
        public void onGeolocationPermissionsHidePrompt() {
            super.onGeolocationPermissionsHidePrompt();
        }

        @Override // com.tencent.smtt.sdk.WebChromeClient
        public void onGeolocationPermissionsShowPrompt(String str, GeolocationPermissionsCallback geolocationPermissionsCallback) {
            geolocationPermissionsCallback.invoke(str, true, false);
            super.onGeolocationPermissionsShowPrompt(str, geolocationPermissionsCallback);
        }

        @Override // com.tencent.smtt.sdk.WebChromeClient
        public boolean onCreateWindow(WebView webView, boolean z, boolean z2, Message message) {
            ((WebView.WebViewTransport) message.obj).setWebView(webView);
            message.sendToTarget();
            return true;
        }
    }


    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!this.isTouchBoolean) {
            return false;
        }
        return super.onTouchEvent(motionEvent);
    }

    public void setTouchEnable(boolean z) {
        this.isTouchBoolean = z;
    }

}
