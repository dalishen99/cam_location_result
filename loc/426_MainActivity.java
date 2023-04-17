package com.love.jswebview;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.ViewTreeObserver;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    WebView webView;
    private Context mContext;
    private RelativeLayout rel_root;

    private boolean isInput = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        webView = (WebView) findViewById(R.id.webView);
        rel_root = (RelativeLayout) findViewById(R.id.rel_root);

        webView.loadUrl("http://insurance.ichezheng.com/zhongAn/getUrl?userId=47718");
        //webView.loadUrl("http://10.8.6.28:8080/tjquery/");

        initWebView();

        initJianting();
    }

    private void initJianting() {
        rel_root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = rel_root.getRootView().getHeight() - rel_root.getHeight();
                if (heightDiff > dpToPx(MainActivity.this, 200)) {
                    Toast.makeText(MainActivity.this, "显示", Toast.LENGTH_SHORT).show();
                    isInput = true;
                } else {
                    if (isInput) {
                        Toast.makeText(MainActivity.this, "消失", Toast.LENGTH_SHORT).show();
                    }
                    isInput = false;
                }
            }
        });
    }

    private void initWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(new JavaScriptInterface(), "android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }


            @Override
            public void onLoadResource(WebView view, String url) {
                Log.e("log-->", "onLoadResource-->>" + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i(TAG, "onPageFinished: js");
                injectionasas();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.i(TAG, "onPageStarted: 借宿");
            }
        });


        webView.setWebChromeClient(new WebChromeClient() {
            //配置定位权限
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress == 100) {
                    Log.i(TAG, "onProgressChanged: success");
                } else {
                    Log.i(TAG, "onProgressChanged: " + newProgress);
                }
            }
        });
    }

    public class JavaScriptInterface {
        @JavascriptInterface
        public void getHTML(final String html) {
            if (!TextUtils.isEmpty(html)) {
                saveUserDataWebView(webView, html);
            }
        }

        @JavascriptInterface
        public void save_username(final String username) {
            Toast.makeText(mContext, "" + username, Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void get_carno(String carNo) {
            Toast.makeText(mContext, "" + carNo, Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void get_citynema(String cityName) {
            Toast.makeText(mContext, "" + cityName, Toast.LENGTH_SHORT).show();
        }


        @JavascriptInterface
        public void test() {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl("javascript:window.android.get_carno(document.getElementById('licenseNo').value)");
                    webView.loadUrl("javascript:window.android.get_citynema(document.getElementById('btnCityName').innerText)");
                }
            });
        }


        @JavascriptInterface
        public void test2() {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.loadUrl("javascript:window.android.get_phone(document.getElementsByName('phone')[0].value)");
                    webView.loadUrl("javascript:window.android.get_name(document.getElementsByName('vehicleOwnerName')[0].value)");
                    webView.loadUrl("javascript:window.android.get_car_model(document.getElementById('vehicleModelNameTxt').innerHTML)");
                    /*webView.loadUrl("javascript:window.android.get_carno(document.getElementById('iptSpecialCarFlagDate').value)");
                    webView.loadUrl("javascript:window.android.get_carno(document.getElementById('iptRegisterDate').value)");*/
                }
            });
        }

        @JavascriptInterface
        public void get_phone(String phone) {
            Log.i(TAG, "from: " + phone);
        }

        @JavascriptInterface
        public void get_name(String name) {
            Log.i(TAG, "from: " + name);
        }

        @JavascriptInterface
        public void get_car_model(String model) {
            Log.i(TAG, "from: " + model);
        }


    }


    public void saveUserDataWebView(final WebView webView, String html) {
        Document document = Jsoup.parseBodyFragment(html);

        Elements elements = document.select("input");
        for (final Element element : elements) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    String id = element.attr("id");

                    int handType = handleType(id);
                    Log.i(TAG, "run: " + handType);
                    switch (handType) {
                        case 10010:
                            getValueById(id, webView);
                            break;
                        case 10011:
                            getValueById2(id, webView, "get_carno");
                            break;
                        case 10012:
                            getValueById2(id, webView, "get_citynema");
                            break;
                    }
                }
            });
        }
    }


    private int handleType(String id) {
        switch (id) {
            case "test":
                return 10010;
            case "licenseNo":
                return 10011;
            case "btnCityName":
                return 10012;
            default:
                return 1;
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();//返回上一页面
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void getValueById(String id, WebView webView) {
        webView.loadUrl("javascript:window.android.save_username(document.getElementById('" + id + "').value)");
    }

    private void getValueById2(String id, WebView webView, String method) {
        webView.loadUrl("javascript:window.android." + method + "(document.getElementById('" + id + "').innerText)");
    }


    private void getHtml() {
        webView.loadUrl("javascript:window.android.getHTML('<html>'+document.body.innerHTML+'</html>');");
    }

    private void injectionasas() {
        //webView.loadUrl("javascript:window.android(document.getElementById('syx').onclick = function(){ alert('xxx');})");
        //webView.loadUrl("javascript:window.android(document.getElementById('syx').onclick = function(){ javascript:window.android.test();})");
        webView.loadUrl("javascript:window.android(document.getElementById('btnGetQuote').onclick = function(){ javascript:window.android.test();})");
        webView.loadUrl("javascript:window.android(document.getElementById('btnSubmit').onclick = function(){ javascript:window.android.test2();})");
    }

    public static float dpToPx(Context context, float valueInDp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }
}
