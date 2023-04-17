package io.okhi.android_okcollect.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import io.okhi.android_core.OkHi;
import io.okhi.android_core.models.OkHiException;
import io.okhi.android_core.models.OkHiLocation;
import io.okhi.android_core.models.OkHiMode;
import io.okhi.android_core.models.OkHiPermissionService;
import io.okhi.android_core.models.OkHiUser;
import io.okhi.android_core.models.OkPreference;
import io.okhi.android_okcollect.OkCollect;
import io.okhi.android_okcollect.R;
import io.okhi.android_okcollect.callbacks.OkCollectCallback;
import io.okhi.android_okcollect.interfaces.WebAppInterface;
import io.okhi.android_okcollect.models.OkCollectLaunchMode;

import static io.okhi.android_okcollect.utilities.Constants.DEV_HEART_URL_POST_22;
import static io.okhi.android_okcollect.utilities.Constants.DEV_HEART_URL_PRE_22;
import static io.okhi.android_okcollect.utilities.Constants.PROD_HEART_URL_POST_22;
import static io.okhi.android_okcollect.utilities.Constants.PROD_HEART_URL_PRE_22;
import static io.okhi.android_okcollect.utilities.Constants.SANDBOX_HEART_URL_POST_22;
import static io.okhi.android_okcollect.utilities.Constants.SANDBOX_HEART_URL_PRE_22;


public class OkHeartActivity extends AppCompatActivity {
    private static WebView myWebView;
    private Boolean enableStreetView, enableAppBar, homeAddressTypeEnabled, workAddressTypeEnabled;
    private String phone, firstName, lastName,environment,developerName,
            authorizationToken,primaryColor,logoUrl,appBarColor, organisationName, email;
    private static OkCollectCallback<OkHiUser, OkHiLocation> okCollectCallback;
    private static String authorization;
    private static OkHiException okHiException;
    private String params;
    private String mode;

    private String launchMode = OkCollectLaunchMode.SELECT.name();

    private static Context appContext;
    private String webViewUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_okheart);
        appContext = this;
        myWebView = findViewById(R.id.webview);
        Bundle bundle = getIntent().getExtras();
        processBundle(bundle);
        setupWebView();
    }

    private void processBundle(Bundle bundle){
        try {
            params = bundle.getString("params");
            mode = bundle.getString("mode");
            launchMode = bundle.getString("launchMode", OkCollectLaunchMode.SELECT.name());
            processParams(params);
        }
        catch (Exception e){
            runCallback(new OkHiException( OkHiException.UNKNOWN_ERROR_CODE, e.getMessage()));
            finish();
        }
    }

    private void processParams(String params){
        try{
            JSONObject paramsObject = new JSONObject(params);
            phone = paramsObject.optString("phone");
            firstName = paramsObject.optString("firstName");
            lastName = paramsObject.optString("lastName");
            email = paramsObject.optString("email");
            environment = paramsObject.optString("environment");
            primaryColor = paramsObject.optString("primaryColor");
            logoUrl = paramsObject.optString("logoUrl");
            appBarColor = paramsObject.optString("appBarColor");
            enableStreetView = paramsObject.optBoolean("enableStreetView");
            enableAppBar = paramsObject.optBoolean("enableAppBar");
            developerName = paramsObject.optString("developerName");
            organisationName = paramsObject.optString("organisationName");
            homeAddressTypeEnabled = paramsObject.optBoolean("homeAddressTypeEnabled", true);
            workAddressTypeEnabled = paramsObject.optBoolean("workAddressTypeEnabled", true);
        } catch (Exception e){
            runCallback(new OkHiException( OkHiException.UNKNOWN_ERROR_CODE, e.getMessage()));
            finish();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(){
        myWebView.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        myWebView.setWebContentsDebuggingEnabled(false);
        myWebView.addJavascriptInterface(new WebAppInterface(OkHeartActivity.this), "Android");
        myWebView.loadUrl(getWebUrl());
        myWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });
    }

    private String getWebUrl(){
        String webViewUrl;
        if(environment.equalsIgnoreCase(OkHiMode.PROD)){
            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M){
                webViewUrl = PROD_HEART_URL_PRE_22;
            } else {
                webViewUrl = PROD_HEART_URL_POST_22;
            };
        }
        else if(environment.equalsIgnoreCase(OkHiMode.SANDBOX)){
            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M){
                webViewUrl = SANDBOX_HEART_URL_PRE_22;
            } else {
                webViewUrl = SANDBOX_HEART_URL_POST_22;
            };
        }
        else{
            if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M){
                webViewUrl = DEV_HEART_URL_PRE_22;
            } else {
                webViewUrl = DEV_HEART_URL_POST_22;
            };
        }
        
        return webViewUrl;
    };

    public void receiveMessage(String results) {
        try {
            final JSONObject jsonObject = new JSONObject(results);
            String message = jsonObject.optString("message");
            JSONObject payload = jsonObject.optJSONObject("payload");
            switch (message) {
                case "app_state":
                    checkAuthToken();
                    break;
                case "location_created":
                    processResponse(results);
                    break;
                case "location_updated":
                    processResponse(results);
                    break;
                case "location_selected":
                    processResponse(results);
                    break;
                case "request_enable_protected_apps":
                    processEnableProtectedApps();
                    break;
                case "fatal_exit":
                    processError(results);
                    break;
                case "exit_app":
                    exitApp(results);
                    break;
                default:
                    processError(results);
                    break;
            }
        } catch (JSONException e) {
            runCallback(new OkHiException( OkHiException.UNKNOWN_ERROR_CODE, e.getMessage()));
            finish();
        }
    }

    private void processEnableProtectedApps() {
        try {
            OkHiPermissionService.openProtectedAppsSettings(this, 8678);
        } catch (OkHiException e) {
            e.printStackTrace();
        }
    }

    private void checkAuthToken(){
        try {
            while(getAuthorization() == null){
                Thread.sleep(1000);
            }
            if(getAuthorization().equalsIgnoreCase("error")){
                runCallback(getOkHiException());
                finish();
            }
            else{
                authorizationToken = getAuthorization();
                launchHeart();
            }
        } catch (InterruptedException e) {
            launchHeart();
        }
    }

    private void launchHeart(){
        try{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("message", launchMode.equals(OkCollectLaunchMode.CREATE.name()) ? "start_app" : "select_location");
                        JSONObject payload1 = new JSONObject();
                        JSONObject style = new JSONObject();
                        JSONObject base = new JSONObject();
                        if (primaryColor != null) {
                            if (primaryColor.length() > 0) {
                                base.put("color", primaryColor);
                            }
                        }
                        if (organisationName != null) {
                            if (organisationName.length() > 0) {
                                base.put("name", organisationName);
                            }
                        }
                        if (logoUrl != null) {
                            if (logoUrl.length() > 0) {
                                base.put("logo", logoUrl);
                            }
                        }
                        style.put("base", base);
                        payload1.put("style", style);

                        JSONObject user = new JSONObject();
                        user.put("firstName", firstName);
                        user.put("lastName", lastName);
                        user.put("phone", phone);
                        user.put("email", email);
                        payload1.put("user", user);

                        JSONObject auth = new JSONObject();
                        auth.put("authToken", authorizationToken);
                        payload1.put("auth", auth);

                        JSONObject context = new JSONObject();
                        JSONObject container = new JSONObject();
                        container.put("name", "okCollectMobileAndroid");
                        container.put("version", "version");
                        context.put("container", container);

                        JSONObject developer = new JSONObject();
                        developer.put("name", developerName);
                        context.put("developer", developer);

                        JSONObject library = new JSONObject();
                        library.put("name", "okCollectMobileAndroid");
                        context.put("library", library);
                        library.put("version", "version");

                        JSONObject platform = new JSONObject();
                        platform.put("name", "android");
                        context.put("platform", platform);
                        payload1.put("context", context);

                        JSONObject permissions = new JSONObject();
                        Boolean hasBackgroundLocationPermission = OkHi.isBackgroundLocationPermissionGranted(getApplicationContext());
                        Boolean hasLocationPermission = OkHi.isLocationPermissionGranted(getApplicationContext());
                        permissions.put("location", hasBackgroundLocationPermission ? "always" : hasLocationPermission ? "whenInUse" : "denied");

                        context.put("permissions", permissions);

                        JSONObject device = new JSONObject();
                        device.put("manufacturer", Build.MANUFACTURER);
                        device.put("model", Build.MODEL);
                        context.put("device", device);

                        JSONObject config = new JSONObject();
                        JSONObject appBar = new JSONObject();
                        JSONObject addressTypes = new JSONObject();
                        if (enableStreetView != null) {
                            config.put("streetView", enableStreetView);
                        }
                        else {
                            config.put("streetView", false);
                        }

                        if (enableAppBar != null) {
                            appBar.put("visible", enableAppBar);
                        }
                        else {
                            appBar.put("visible", false);
                        }
                        if (appBarColor != null) {
                            if (appBarColor.length() > 0) {
                                appBar.put("color", appBarColor);
                            }
                        }
                        addressTypes.put("work", workAddressTypeEnabled != null ? workAddressTypeEnabled : true);
                        addressTypes.put("home", homeAddressTypeEnabled != null ? homeAddressTypeEnabled : true);

                        config.put("protectedApps", OkHiPermissionService.canOpenProtectedApps(appContext));
                        config.put("addressTypes", addressTypes);
                        config.put("appBar", appBar);
                        payload1.put("config", config);
                        jsonObject.put("payload", payload1);
                        jsonObject.put("url", getWebUrl());
                        String payload = jsonObject.toString().replace("\\", "");
                        OkPreference.setItem("okcollect-launch-payload", payload, appContext);
                        myWebView.evaluateJavascript("javascript:receiveAndroidMessage(" + payload + ")", null);
                    } catch (Exception e) {
                        runCallback(new OkHiException( OkHiException.UNKNOWN_ERROR_CODE, e.getMessage()));
                        finish();
                    }
                }
            });
        }
        catch (Exception e){
            runCallback(new OkHiException( OkHiException.UNKNOWN_ERROR_CODE, e.getMessage()));
            finish();
        }
    }

    private void processResponse(String response){
        try{
            JSONObject responseObject = new JSONObject(response);
            JSONObject payloadObject = responseObject.optJSONObject("payload");
            JSONObject locationObject = payloadObject.optJSONObject("location");
            JSONObject userObject = payloadObject.optJSONObject("user");
            final OkHiUser user = new OkHiUser.Builder(userObject.optString("phone",null))
                    .withFirstName(userObject.optString("first_name",null))
                    .withLastName(userObject.optString("last_name",null))
                    .withOkHiUserId(userObject.optString("id",null))
                    .withEmail(userObject.optString("email", null))
                    .build();
            String ualId = locationObject.optString("id", null);
            JSONObject geoPointObject = locationObject.optJSONObject("geo_point");
            Double lat = null;
            Double lng = null;
            if(geoPointObject != null){
                lat = locationObject.optJSONObject("geo_point").optDouble("lat");
                lng = locationObject.optJSONObject("geo_point").optDouble("lon");
            }
            String streetName = locationObject.optString("street_name",null);
            String propertyName = locationObject.optString("property_name",null);
            String propertyNumber = locationObject.optString("property_number",null);
            String directions = locationObject.optString("directions",null);
            String placeId = locationObject.optString("place_id",null);
            String url = locationObject.optString("url",null);
            String title = locationObject.optString("title",null);
            String photo = locationObject.optString("photo",null);
            String subtitle = locationObject.optString("subtitle",null);
            String plusCode = locationObject.optString("plus_code",null);
            String displayTitle = locationObject.optString("display_title",null);
            String country = locationObject.optString("country",null);
            String city = locationObject.optString("city",null);
            String state = locationObject.optString("state",null);
            String countryCode = locationObject.optString("country_code",null);
            String neighborhood = locationObject.optString("neighborhood",null);
            JSONObject streetViewObject = locationObject.optJSONObject("street_view");
            String streetViewUrl = null;
            String streetViewPanoId = null;
            if(streetViewObject != null){
                streetViewUrl = streetViewObject.optString("url",null);
                streetViewPanoId = streetViewObject.optString("pano_id" ,null);
            }
            final OkHiLocation location = new OkHiLocation.Builder(ualId,lat,lng)
                    .setDirections(directions)
                    .setUrl(url)
                    .setPlaceId(placeId)
                    .setPlusCode(plusCode)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setPropertyName(propertyName)
                    .setStreetName(streetName)
                    .setStreetViewPanoId(streetViewPanoId)
                    .setStreetViewPanoUrl(streetViewUrl)
                    .setPhoto(photo)
                    .setUrl(url)
                    .setPropertyNumber(propertyNumber)
                    .setDisplayTitle(displayTitle)
                    .setCountry(country)
                    .setCity(city)
                    .setState(state)
                    .setCountryCode(countryCode)
                    .setNeighborhood(neighborhood)
                    .build();
            runCallback(user,location);
            finish();
        }
        catch (Exception e){
            runCallback(new OkHiException( OkHiException.UNKNOWN_ERROR_CODE, e.getMessage()));
            finish();
        }
    }

    private void runCallback(final OkHiUser user, final OkHiLocation location) {
        OkCollect.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                okCollectCallback.onSuccess(user, location);
            }
        });
        finish();
    }

    private void runCallback(final OkHiException exception) {
        OkCollect.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                okCollectCallback.onError(exception);
            }
        });
        finish();
    }

    private void runOnCloseCallback() {
        OkCollect.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                okCollectCallback.onClose();
            }
        });
        finish();
    }

    private void processError(String response){
        try {
            final JSONObject jsonObject = new JSONObject(response);
            String message = jsonObject.optString("message");
            JSONObject payload = jsonObject.optJSONObject("payload");
            if (payload == null) {
                String backuppayload = jsonObject.optString("payload");
                if (backuppayload != null) {
                    payload = new JSONObject();
                    payload.put("error", backuppayload);
                }
            }
            runCallback(new OkHiException( OkHiException.UNKNOWN_ERROR_CODE, payload.toString()));
            finish();
        }
        catch (Exception e){
            runCallback(new OkHiException( OkHiException.UNKNOWN_ERROR_CODE, e.getMessage()));
            finish();
        }
    }

    private void exitApp(String response){
        runOnCloseCallback();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class MyWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            // TODO Auto-generated method stub
            super.onPageStarted(view, url, favicon);
            if (!mode.equals("dev")) {
                view.evaluateJavascript("window.console.log = function () {};", null);
            }
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (error.getErrorCode() == -1) {
                    return;
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(error.getErrorCode() != -2) {
                    runCallback(new OkHiException(OkHiException.UNKNOWN_ERROR_CODE, error.getDescription().toString()));
                    finish();
                }
                else if(error.getDescription().toString().equalsIgnoreCase("net::ERR_NAME_NOT_RESOLVED")){
                    runCallback(new OkHiException(OkHiException.NETWORK_ERROR_CODE, OkHiException.NETWORK_ERROR_MESSAGE));
                    finish();
                }
            } else {
                runCallback(new OkHiException(OkHiException.UNKNOWN_ERROR_CODE, error.toString()));
                finish();
            }
        }
    }

    public static String getAuthorization() {
        return authorization;
    }

    public static void setAuthorization(String authorization) {
        OkHeartActivity.authorization = authorization;
    }

    public OkCollectCallback<OkHiUser, OkHiLocation> getOkCollectCallback() {
        return okCollectCallback;
    }

    public static void setOkCollectCallback(OkCollectCallback<OkHiUser, OkHiLocation> okCollectCallback) {
        OkHeartActivity.okCollectCallback = okCollectCallback;
    }

    public static OkHiException getOkHiException() {
        return okHiException;
    }

    public static void setOkHiException(OkHiException okHiException) {
        OkHeartActivity.okHiException = okHiException;
    }

    private void displayLog(String log){
        Log.i("OkHeartActivity", log);
    }

    @Override
    public void onBackPressed() {
        if(myWebView.canGoBack()){
            myWebView.goBack();
        } else{
            runOnCloseCallback();
        }
    }
}