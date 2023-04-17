package com.shwaeki.heseim;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Timer;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    WebView myWebView;
    private ValueCallback<Uri[]> mFilePathCallback;
    public static MainActivity instantForFindingWebView;

    private boolean isTripStarted = false;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = Double.parseDouble(intent.getStringExtra("latitude"));
            double longitude = Double.parseDouble(intent.getStringExtra("longitude"));
            String str = latitude + "," + longitude;
            //   Log.d("TEST", "Msg = " + str);
            callJsWebView("javascript:getLocationJS('" + str + "');");
        }
    };


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myWebView = findViewById(R.id.webview);

        myWebView.addJavascriptInterface(new WebAppExecutor(), "NativeApp");
        myWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.getSettings().setAppCacheEnabled(true);
        myWebView.getSettings().setAllowFileAccess(true);
        myWebView.getSettings().setGeolocationEnabled(true);
        myWebView.getSettings().setUseWideViewPort(true);
        myWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        myWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        myWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager.getInstance().setAcceptThirdPartyCookies(myWebView, true);
        myWebView.getSettings().setGeolocationDatabasePath(getFilesDir().getPath());

        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(myWebView, url);
                if (checkPermission()) {
                    LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        hideSplashScreen();
                    } else {
                        showGPSDisabledAlertToUser();
                    }
                } else {
                    requestPermission();
                }

            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView webView, String str) {

                if (str.startsWith("tel:")) {
                    startActivity(new Intent("android.intent.action.DIAL", Uri.parse(str)));
                    return true;
                } else if (str.startsWith("http:") || str.startsWith("https:")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(str)));
                    return true;
                } else {
                    webView.loadUrl(str);
                    return true;
                }
            }

        });
        myWebView.setWebChromeClient(new WebChromeClient() {
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePath;

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, contentSelectionIntent);
                startActivityForResult(chooserIntent, 1332);
                return true;
            }
        });

        //  myWebView.loadUrl("https://heseim.academia-jerusalem.com/");
        myWebView.loadUrl("file:///android_asset/heseim/index.html");

        instantForFindingWebView = this;
    }

    public static MainActivity getInstace() {
        return instantForFindingWebView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != 1332 || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }
        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i("TEST", "onResume");
        registerReceiver(this.broadcastReceiver, new IntentFilter("LOCATION"));
      //  callJsWebView("javascript:clearGPSStatus();");
        if (isTripStarted){
            startLocationService();
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.i("TEST", "onPause");
        unregisterReceiver(this.broadcastReceiver);
     //   callJsWebView("javascript:clearGPSStatus();");
    }

    @Override
    public void onBackPressed() {
        if (this.myWebView.canGoBack()) {
            myWebView.goBack();
        } else {
            finish();
        }
    }

    public void callJsWebView(final String str) {
      //  Log.i("TEST", str);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                MainActivity.this.myWebView.post(new Runnable() {
                    String str;

                    @Override
                    public void run() {
                        MainActivity.this.myWebView.loadUrl(this.str);
                    }

                    public Runnable init(String str2) {
                        this.str = str2;
                        return this;
                    }
                }.init(str));
            }
        });
    }


    private void hideSplashScreen() {
        findViewById(R.id.splash).setVisibility(View.GONE);
        myWebView.setVisibility(View.VISIBLE);


    }


    public void startLocationService() {
        if (!isMyServiceRunning(LocationService.class)) {
            startService(new Intent(getApplicationContext(), LocationService.class));
        }
        isTripStarted = true;
    }

    public void stopLocationService() {
        if (isMyServiceRunning(LocationService.class)) {
            stopService(new Intent(getApplicationContext(), LocationService.class));
        }
        isTripStarted = false;
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_COARSE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }


    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {

                boolean locationAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean cameraAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (locationAccepted && cameraAccepted) {
                    hideSplashScreen();
                } else {
                    if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                        showMessageOKCancel((dialog, which) -> requestPermission());
                    } else {
                        showGPSAlertPermission();
                    }

                }
            }
        }
    }

    private void showGPSAlertPermission() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("تم تعطيل نظام تحديد المواقع (GPS) في جهازك. لا يمكن المتابعة دون تفعيله؟")
                .setCancelable(false)
                .setPositiveButton("انتقل إلى الإعدادات لتفعيل GPS",
                        (dialog, id) -> {
                            Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                            myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
                            myAppSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(myAppSettings);
                        });
        alertDialogBuilder.setNegativeButton("اغلاق", (dialog, id) -> {
            dialog.cancel();
            finish();
        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("لا يمكن استخدام التطبيق دون تفعيل GPS")
                .setCancelable(false)
                .setPositiveButton("انتقل إلى الإعدادات لتفعيل GPS",
                        (dialog, id) -> {
                            Intent callGPSSettingIntent = new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(callGPSSettingIntent);
                        });
        alertDialogBuilder.setNegativeButton("اغلاق",
                (dialog, id) -> {
                    dialog.cancel();
                    finish();
                });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("يجب عليك قبول صلاحيات استخدام الموقع للمتابعة")
                .setPositiveButton("قبول", okListener)
                .setNegativeButton("اغلاق", null)
                .create()
                .show();
    }

}
