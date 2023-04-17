package neartool.semuadapat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.blikoon.qrcodescanner.QrCodeActivity;
import com.facebook.ads.AdSize;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.onesignal.OSDeviceState;

import com.onesignal.OSNotificationOpenedResult;
import com.onesignal.OSSubscriptionObserver;
import com.onesignal.OSSubscriptionStateChanges;
import com.onesignal.OneSignal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import static neartool.semuadapat.Config.ACTIVATE_PROGRESS_BAR;
import static neartool.semuadapat.Config.ENABLE_SWIPE_NAVIGATE;
import static neartool.semuadapat.Config.ENABLE__PULL_REFRESH;
import static neartool.semuadapat.Config.EXIT_APP_DIALOG;
import static neartool.semuadapat.Config.HIDE_NAVIGATION_BAR_IN_LANDSCAPE;
import static neartool.semuadapat.Config.INCREMENT_WITH_REDIRECTS;
import static neartool.semuadapat.Config.MAX_TEXT_ZOOM;
import static neartool.semuadapat.Config.REMAIN_SPLASH_OPTION;
import static neartool.semuadapat.Config.SPECIAL_LINK_HANDLING_OPTIONS;
import static neartool.semuadapat.Config.SPLASH_SCREEN_ACTIVATED;
import static neartool.semuadapat.Config.downloadableExtension;

// ***********************************//
// ** Code By Near Hoshinova
// ** Github: github.com/GarudaID
// ** Clean Code
// ***********************************//

public class MainActivity extends AppCompatActivity
        implements OSSubscriptionObserver,
        PurchasesUpdatedListener {

    public static boolean HIDE_ADS_FOR_PURCHASE = false;


    public static final int PERMISSION_REQUEST_CODE = 9541;
    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";

    private static final String INDEX_FILE = "file:///android_asset/local-html/index.html";
    private static final int CODE_AUDIO_CHOOSER = 5678;
    private static final String ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID;
    private CustomWebView webView;
    private WebView mWebviewPop;
    private SharedPreferences preferences;
    private RelativeLayout mContainer;
    private RelativeLayout windowContainer;

    private View offlineLayout;

    public static final int REQUEST_CODE_QR_SCAN = 1234;

    private AdView mAdView;
    private LinearLayout facebookBannerContainer;
    private com.facebook.ads.AdView facebookAdView;
    InterstitialAd mInterstitialAd;
    com.facebook.ads.InterstitialAd facebookInterstitialAd;
    SwipeRefreshLayout mySwipeRefreshLayout;
    public static final int MULTIPLE_PERMISSIONS = 10;
    public ProgressBar progressBar;
    private String deepLinkingURL;
    private BillingClient billingClient;
    int mCount = -2;

    private static final String TAG = ">>>>>>>>>>>";
    private String mCM, mVM;
    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;
    private final static int FCR = 1;
    public String hostpart;
    private boolean disableAdMob = false;
    private String successUrl = "", failUrl = "";
    private FrameLayout adLayout;
    private boolean offlineFileLoaded = false;
    private boolean isNotificationURL = false;
    private boolean extendediap = true;
    public String uuid = "";
    public static Context mContext;
    private String firebaseUserToken = "";

    private boolean isRedirected = false;


    static long TimeStamp = 0;
    static boolean isInBackGround = false;
    private static boolean connectedNow = false;

    // NFC
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter writeTagFilters[];
    private Tag myTag;
    private boolean NFCenabled = false;
    private boolean readModeNFC = false;
    private boolean writeModeNFC = false;
    private String textToWriteNFC = "";
    private boolean SPLASH_SCREEN_ACTIVE = false;
    // Social media login user agents
    public static final String USER_AGENT_GOOGLE = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.101 Mobile Safari/537.36";
    public static final String USER_AGENT_FB = "Mozilla/5.0 (Linux; U; Android 2.2) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

    // Manual Cookie Sync
    private final Handler cookieSyncHandler = new Handler();
    private Runnable cookieSyncRunnable;
    private boolean onResumeCalled = false;
    private boolean cookieSyncOn = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mContext = this;
        uuid = Settings.System.getString(super.getContentResolver(), Settings.Secure.ANDROID_ID);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        onResumeCalled = false;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String ret = settings.getString("disableAdMobDone", "default");

        if (ret == "removed") {
            disableAdMob = true;
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Config.blackStatusBarText) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
        if (Config.PREVENT_SLEEP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        super.onCreate(savedInstanceState);
        if (SPLASH_SCREEN_ACTIVATED) {
            SPLASH_SCREEN_ACTIVE = true;
            startActivity(new Intent(getApplicationContext(), SplashScreen.class));
        }

        // Support the cut out background when in landscape mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(getResources().getColor(R.color.colorPrimaryDark));
            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
            getWindow().setBackgroundDrawable(bitmapDrawable);
        }

        setContentView(R.layout.activity_main);
        verifyStoragePermission(this);
//        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//        StrictMode.setVmPolicy(builder.build());
//        if (Build.VERSION.SDK_INT > 23) {
//            builder.detectFileUriExposure();
//        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
//                == PackageManager.PERMISSION_GRANTED) {
//            // Permission is granted
//        }NFCenabled
//        else {
//            //Permission is not granted so you have to request it
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    888);
//        }


        if () {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.NFC},
                        PERMISSION_REQUEST_CODE);
            } else {
                initNfc();
            }
        }

        if (Config.FIREBASE_PUSH_ENABLED) {
            fetchFCMToken();
        }

        RelativeLayout main = findViewById(R.id.main);
        adLayout = findViewById(R.id.ad_layout);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                    billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP,
                            (billingResult1, purchasesList) -> {

                                Log.i(TAG, "is purchased : " + (purchasesList != null && !purchasesList.isEmpty()));

                                if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK
                                        && purchasesList != null && !purchasesList.isEmpty()) {

                                    boolean productFound = true;
                                    if (productFound) {
                                        Log.i(TAG, "purchased " + String.valueOf(true));
                                        HIDE_ADS_FOR_PURCHASE = true;
                                        AlertManager.purchaseState(getApplicationContext(), true);
                                        if (AlertManager.isPurchased(getApplicationContext())) {
                                            HIDE_ADS_FOR_PURCHASE = true;
                                        }
                                    } else {
                                        Log.i(TAG, "purchased " + String.valueOf(false));
                                        HIDE_ADS_FOR_PURCHASE = false;
                                        AlertManager.purchaseState(getApplicationContext(), false);
                                        if (AlertManager.isPurchased(getApplicationContext())) {
                                            HIDE_ADS_FOR_PURCHASE = true;
                                        }
                                    }
                                } else {
                                    Log.i(TAG, "purchased " + String.valueOf(false));
                                    HIDE_ADS_FOR_PURCHASE = false;
                                    AlertManager.purchaseState(getApplicationContext(), false);
                                    if (AlertManager.isPurchased(getApplicationContext())) {
                                        HIDE_ADS_FOR_PURCHASE = true;
                                    }
                                }
                            });
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
            }
        });

        Intent intent = getIntent();
        if (intent != null && intent.getData() != null && (intent.getData().getScheme().equals("http"))) {
            Uri data = intent.getData();
            List<String> pathSegments = data.getPathSegments();
            if (pathSegments.size() > 0) {
                deepLinkingURL = pathSegments.get(0).substring(5);
                String fulldeeplinkingurl = data.getPath().toString();
                fulldeeplinkingurl = fulldeeplinkingurl.replace("/link=", "");
                deepLinkingURL = fulldeeplinkingurl;
            }
        } else if (intent != null && intent.getData() != null && (intent.getData().getScheme().equals("https"))) {
            Uri data = intent.getData();
            List<String> pathSegments = data.getPathSegments();
            if (pathSegments.size() > 0) {
                deepLinkingURL = pathSegments.get(0).substring(5);
                String fulldeeplinkingurl = data.getPath().toString();
                fulldeeplinkingurl = fulldeeplinkingurl.replace("/link=", "");
                deepLinkingURL = fulldeeplinkingurl;
            }
        }

        if (intent != null) {
            Bundle extras = getIntent().getExtras();
            String URL = null;
            if (extras != null) {
                URL = extras.getString("ONESIGNAL_URL");
            }
            if (URL != null && !URL.equalsIgnoreCase("")) {
                isNotificationURL = true;
                deepLinkingURL = URL;
            } else isNotificationURL = false;
        }




        if (Config.PUSH_ENABLED) {
            OneSignal.addSubscriptionObserver(this);
            OneSignal.setNotificationOpenedHandler(
                    new OneSignal.OSNotificationOpenedHandler() {
                        @Override
                        public void notificationOpened(OSNotificationOpenedResult result) {

                            String actionId = result.getAction().getActionId();

                            String title = result.getNotification().getTitle();
                            Log.e("RESULTTITLE", title);


                            JSONObject data = result.getNotification().getAdditionalData();
                            String launchUrl = result.getNotification().getLaunchURL();
                            String urlString = null;
                            if (data != null && data.has("url")) {
                                try {
                                    urlString = data.getString("url");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            // Log.e("OneSignal_Deeplinking", urlString); //Deeplinking URL of OneSignal (url key-value pair)

                            if (urlString != null) {
                                handleURl(urlString);
                            } else {
                                if (launchUrl != null) {
                                    openInExternalBrowser(launchUrl);
                                }

                            }


                            long diffseconds = TimeUnit.MILLISECONDS.toSeconds(Calendar.getInstance().getTimeInMillis() - TimeStamp);

                            if (isInBackGround && diffseconds >= 3)
                                foreground(launchUrl, urlString);


                        }
                    });
        }

        if (savedInstanceState == null) {
            AlertManager.appLaunched(this);
        }

        mAdView = findViewById(R.id.adView);
        if (Config.USE_FACEBOOK_ADS) {
            Log.e(TAG, "attempting to create ad");
            facebookAdView = new com.facebook.ads.AdView(this,
                    getString(R.string.facebook_banner_footer),
                    AdSize.BANNER_HEIGHT_50);
        }

        AdRequest adRequest = new AdRequest.Builder()
                .build();


        if (Config.SHOW_BANNER_AD && !disableAdMob) {
            if (Config.USE_FACEBOOK_ADS) {
                adLayout.removeAllViews();
                adLayout.addView(facebookAdView);
                adLayout.setVisibility(View.VISIBLE);
                facebookAdView.loadAd();
            } else {
                mAdView.loadAd(adRequest);
                adLayout.setVisibility(View.VISIBLE);
                mAdView.setAdListener(new AdListener() {
                    @Override
                    public void onAdLoaded() {
                        if (!HIDE_ADS_FOR_PURCHASE) {
                            mAdView.setVisibility(View.VISIBLE);
                            adLayout.setVisibility(View.VISIBLE);
                        } else {
                            mAdView.setVisibility(View.GONE);
                            adLayout.setVisibility(View.GONE);
                        }
                    }


                    @Override
                    public void onAdOpened() {
                        if (!HIDE_ADS_FOR_PURCHASE) {
                            mAdView.setVisibility(View.VISIBLE);
                            adLayout.setVisibility(View.VISIBLE);
                        } else {
                            mAdView.setVisibility(View.GONE);
                            adLayout.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onAdClosed() {
                    }
                });
            }
        } else {
            mAdView.setVisibility(View.GONE);
            adLayout.setVisibility(View.GONE);
        }

        if (!HIDE_ADS_FOR_PURCHASE) {
            if (Config.USE_FACEBOOK_ADS) {
                facebookInterstitialAd = new com.facebook.ads.InterstitialAd(this, getString(R.string.facebook_interstitial_full_screen));
                com.facebook.ads.InterstitialAdListener interstitialAdListener = new com.facebook.ads.InterstitialAdListener() {
                    @Override
                    public void onInterstitialDisplayed(com.facebook.ads.Ad ad) {
                        // Interstitial ad displayed callback
                        Log.e(TAG, "Interstitial ad displayed.");
                    }

                    @Override
                    public void onInterstitialDismissed(com.facebook.ads.Ad ad) {
                        // Interstitial dismissed callback
                        Log.e(TAG, "Interstitial ad dismissed.");
                    }

                    @Override
                    public void onError(com.facebook.ads.Ad ad, com.facebook.ads.AdError adError) {
                        // Ad error callback
                        Log.e(TAG, "Interstitial ad failed to load: " + adError.getErrorMessage());
                    }

                    @Override
                    public void onAdLoaded(com.facebook.ads.Ad ad) {
                        // Interstitial ad is loaded and ready to be displayed
                        Log.d(TAG, "Interstitial ad is loaded and ready to be displayed!");

                    }

                    @Override
                    public void onAdClicked(com.facebook.ads.Ad ad) {
                        // Ad clicked callback
                        Log.d(TAG, "Interstitial ad clicked!");
                    }

                    @Override
                    public void onLoggingImpression(com.facebook.ads.Ad ad) {
                        // Ad impression logged callback
                        Log.d(TAG, "Interstitial ad impression logged!");
                    }
                };

                // For auto play video ads, it's recommended to load the ad
                // at least 30 seconds before it is shown
                facebookInterstitialAd.loadAd(
                        facebookInterstitialAd.buildLoadAdConfig()
                                .withAdListener(interstitialAdListener)
                                .build());
            }
        }

        webView = findViewById(R.id.webView);
        mContainer = findViewById(R.id.web_container);
        windowContainer = findViewById(R.id.window_container);
        webView.setLayerType(View.LAYER_TYPE_NONE, null);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.setGestureDetector(new GestureDetector(new CustomeGestureDetector()));


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mySwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);

        if (!ENABLE__PULL_REFRESH) {
            mySwipeRefreshLayout.setEnabled(false);

        }

        mySwipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        if (ENABLE__PULL_REFRESH) {
                            webView.reload();

                        }
                        mySwipeRefreshLayout.setRefreshing(false);

                    }
                }
        );


        offlineLayout = findViewById(R.id.offline_layout);

        this.findViewById(android.R.id.content).setBackgroundColor(getResources().getColor(R.color.launchLoadingSignBackground));
        progressBar = findViewById(R.id.progressBar);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.colorAccent), PorterDuff.Mode.MULTIPLY);

        final Button tryAgainButton = findViewById(R.id.try_again_button);
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Try again!");
                webView.setVisibility(View.GONE);
                loadMainUrl();
            }
        });

        webView.setWebViewClient(new AdvanceWebViewClient());
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setUseWideViewPort(true);

        Context appContext = this;

        // Collect the App Name to use as the title for Javascript Dialogs
        final String appName;
        String appName1;
        try {
            appName1 = appContext.getApplicationInfo().loadLabel(appContext.getPackageManager()).toString();
        } catch (Exception e) {
            // If unsuccessful in collecting the app name, set the name to the page title.
            appName1 = webView.getTitle();
        }
        appName = appName1;

        webView.setWebChromeClient(new AdvanceWebChromeClient() {

            // Functions to support alert(), confirm() and prompt() Javascript Dialogs

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                AlertDialog dialog = new AlertDialog.Builder(view.getContext()).
                        setTitle(appName).
                        setMessage(message).
                        setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //do nothing
                            }
                        }).create();
                dialog.show();
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                AlertDialog.Builder b = new AlertDialog.Builder(view.getContext())
                        .setTitle(appName)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        });
                b.show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                final EditText input = new EditText(appContext);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setText(defaultValue);
                new AlertDialog.Builder(appContext)
                        .setTitle(appName)
                        .setView(input)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm(input.getText().toString());
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                result.cancel();
                            }
                        })
                        .create()
                        .show();
                return true;
            }
        });


        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });

        registerForContextMenu(webView);

        final WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Config.CLEAR_CACHE_ON_STARTUP) {
            //webSettings.setAppCacheEnabled(false);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        } else {
            //webSettings.setAppCacheEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowFileAccess(true);
        //webSettings.setLoadWithOverviewMode(true);
        //webSettings.setUseWideViewPort(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Custom Text Zoom
        if (MAX_TEXT_ZOOM > 0) {
            float systemTextZoom = getResources().getConfiguration().fontScale * 100;
            if (systemTextZoom > MAX_TEXT_ZOOM) {
                webView.getSettings().setTextZoom(MAX_TEXT_ZOOM);
            }
        }

        // Phone orientation setting for Android 8 (Oreo)
        if (webSettings.getUserAgentString().contains("Mobile") && android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            if (Config.PHONE_ORIENTATION == "auto") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            } else if (Config.PHONE_ORIENTATION == "portrait") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            } else if (Config.PHONE_ORIENTATION == "landscape") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
            // Phone orientation setting for all other Android versions
        } else if (webSettings.getUserAgentString().contains("Mobile")) {
            if (Config.PHONE_ORIENTATION == "auto") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            } else if (Config.PHONE_ORIENTATION == "portrait") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else if (Config.PHONE_ORIENTATION == "landscape") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
            // Tablet/Other orientation setting
        } else {
            if (Config.TABLET_ORIENTATION == "auto") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            } else if (Config.TABLET_ORIENTATION == "portrait") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else if (Config.TABLET_ORIENTATION == "landscape") {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }

        if (!Config.USER_AGENT.isEmpty()) {
            webSettings.setUserAgentString(Config.USER_AGENT);
        }

        if (Config.CLEAR_CACHE_ON_STARTUP) {
            webView.clearCache(true);
        }

        if (Config.USE_LOCAL_HTML_FOLDER) {
            loadLocal(INDEX_FILE);
        } else if (isConnectedNetwork()) {
            if (Config.USE_LOCAL_HTML_FOLDER) {
                loadLocal(INDEX_FILE);
            } else {
                loadMainUrl();
                connectedNow = true;
            }
        } else {
            loadLocal(INDEX_FILE);
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                askForPermission();
            }
        }, 1000);

        if (!connectedNow) {
            checkInternetConnection();
        }


        if (getIntent().getExtras() != null) {
            String openurl = getIntent().getExtras().getString("openURL");
            if (openurl != null) {
                openInExternalBrowser(openurl);
            }

        }

    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // FCM SDK (and your app) can post notifications.
                    fetchFCMToken();
                } else {
                    // TODO: Inform user that that your app will not show notifications.
                    buildAlertMessageNoNotification();
                }
            });

    private void fetchFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }
                        String token = task.getResult();
                        firebaseUserToken = token;
                        AlertManager.updateFirebaseToken(MainActivity.this, token);
                        Log.d(TAG, token);
                    }
                });
    }

    private void askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // FCM SDK (and your app) can post notifications.
                fetchFCMToken();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void loadAdmobInterstatial() {
        AdRequest madRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, getString(R.string.interstitial_full_screen), madRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The mInterstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                        showInterstitial();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });
    }

    private void foreground(String launchUrl, String urlString) {


        Intent it = new Intent("intent.my.action");
        it.putExtra("openURL", launchUrl);
        it.putExtra("ONESIGNAL_URL", urlString);
        it.setComponent(new ComponentName(getPackageName(), MainActivity.class.getName()));
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(it);
    }

    private void openInExternalBrowser(String launchUrl) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(launchUrl));
        startActivity(browserIntent);
    }

    private void handleURl(String urlString) {

        if (URLUtil.isValidUrl(urlString) && !Config.OPEN_NOTIFICATION_URLS_IN_SYSTEM_BROWSER) {
            webView.loadUrl(urlString);
        }
        if (URLUtil.isValidUrl(urlString) && Config.OPEN_NOTIFICATION_URLS_IN_SYSTEM_BROWSER) {
            Intent external = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
            startActivity(external);
        }
    }


    public static boolean webIsLoaded = false;

    private void checkInternetConnection() {
        //auto reload every 5s
        class AutoRec extends TimerTask {
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {

                        if (!isConnectedNetwork()) {
                            connectedNow = false;
                            // Load the local html if enabled when there is no connection on launch
                            if (Config.FALLBACK_USE_LOCAL_HTML_FOLDER_IF_OFFLINE || Config.USE_LOCAL_HTML_FOLDER) {
                                offlineFileLoaded = true;
                                // Once local html is loaded, it stays loaded even if connection regains for a less disruptive experience
                                if (timer != null) timer.cancel();
                            } else {
                                connectedNow = false;
                                offlineLayout.setVisibility(View.VISIBLE);
                                System.out.println("attempting reconnect");
                                webView.setVisibility(View.GONE);

                                loadMainUrl();

                                Log.d("", "reconnect");
                            }
                        } else {
                            if (!connectedNow) {
                                Log.d("", "connected");
                                System.out.println("Try again!");
                                webView.setVisibility(View.GONE);
                                loadMainUrl();
                                connectedNow = true;
                                if (timer != null) timer.cancel();
                            }
                        }
                    }
                });
            }
        }
        timer.schedule(new AutoRec(), 0, 5000);
        //timer.cancel();
    }

    public static void setAutoOrientationEnabled(Context context, boolean enabled) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i(TAG, "Landscape Mode");
            // Remove the status bar
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            // Remove the navigation bar
            if (HIDE_NAVIGATION_BAR_IN_LANDSCAPE) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.i(TAG, "Portrait Mode");
            // Return the status bar and navigation bar
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }


    public void statusCheck() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void buildAlertMessageNoNotification() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your notifications are off, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void loadLocal(String path) {
        webView.loadUrl(path);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        final WebView.HitTestResult webViewHitTestResult = webView.getHitTestResult();

        if (webViewHitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                webViewHitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            if (Config.ALLOW_IMAGE_DOWNLOAD) {
                menu.setHeaderTitle("Download images");
                menu.add(0, 1, 0, "Download the image")
                        .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                String DownloadImageURL = webViewHitTestResult.getExtra();
                                if (URLUtil.isValidUrl(DownloadImageURL)) {
                                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(DownloadImageURL));
                                    request.allowScanningByMediaScanner();
                                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                    DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                    downloadManager.enqueue(request);
                                    Toast.makeText(MainActivity.this, "Image downloaded successfully.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(MainActivity.this, "Sorry...something went wrong.", Toast.LENGTH_LONG).show();
                                }
                                return false;
                            }
                        });
            }
        }
    }

    public ValueCallback<Uri[]> uploadMessage;
    private ValueCallback<Uri> mUploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    private final static int FILECHOOSER_RESULTCODE = 1;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null)
                    return;
                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                uploadMessage = null;
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage)
                return;
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }

        Uri[] results = null;
        Uri uri = null;
        if (requestCode == FCR) {
            if (resultCode == Activity.RESULT_OK) {
                if (mUMA == null) {
                    return;
                }
                if (intent == null || intent.getData() == null) {

                    if (intent != null && intent.getClipData() != null) {

                        int count = intent.getClipData().getItemCount(); //evaluate the count before the for loop --- otherwise, the count is evaluated every loop.
                        results = new Uri[intent.getClipData().getItemCount()];
                        for (int i = 0; i < count; i++) {
                            uri = intent.getClipData().getItemAt(i).getUri();
                            // results = new Uri[]{Uri.parse(mCM)};
                            results[i] = uri;

                        }
                        //do something with the image (save it to some directory or whatever you need to do with it here)
                    }

                    if (mCM != null) {
                        File file = new File(Uri.parse(mCM).getPath());
                        if (file.length() > 0)
                            results = new Uri[]{Uri.parse(mCM)};
                        else
                            file.delete();
                    }
                    if (mVM != null) {
                        File file = new File(Uri.parse(mVM).getPath());
                        if (file.length() > 0)
                            results = new Uri[]{Uri.parse(mVM)};
                        else
                            file.delete();
                    }

                } else {
                    String dataString = intent.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    } else {
                        if (intent.getClipData() != null) {
                            final int numSelectedFiles = intent.getClipData().getItemCount();
                            results = new Uri[numSelectedFiles];
                            for (int i = 0; i < numSelectedFiles; i++) {
                                results[i] = intent.getClipData().getItemAt(i).getUri();
                            }
                        }

                    }
                }
            } else {
                if (mCM != null) {
                    File file = new File(Uri.parse(mCM).getPath());
                    if (file != null) file.delete();
                }
                if (mVM != null) {
                    File file = new File(Uri.parse(mVM).getPath());
                    if (file != null) file.delete();
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else if (requestCode == CODE_AUDIO_CHOOSER) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null && intent.getData() != null) {
                    results = new Uri[]{intent.getData()};
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        } else if (requestCode == REQUEST_CODE_QR_SCAN) {
            if (resultCode == Activity.RESULT_OK) {
                if (intent != null) {
                    String result = intent.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult");
                    if (result != null && URLUtil.isValidUrl(result)) {
                        webView.loadUrl(result);
                    }
                }
            }
        }
        /* else {
            super.handleActivityResult(requestCode, resultCode, intent);
        }*/
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "";
        File mediaStorageDir = getCacheDir();
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create " + "WebView" + " directory");
                return null;
            }
        }
        return File.createTempFile(
                imageFileName,
                ".jpg",
                mediaStorageDir
        );
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "VID_" + timeStamp + "";
        File mediaStorageDir = getCacheDir();

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create " + "WebView" + " directory");
                return null;
            }
        }
        return File.createTempFile(
                imageFileName,
                ".mp4",
                mediaStorageDir
        );
    }

    @Override
    public void onBackPressed() {
        if (windowContainer.getVisibility() == View.VISIBLE) {
            ClosePopupWindow(mWebviewPop);
        } else if (Config.EXIT_APP_BY_BACK_BUTTON_ALWAYS) {
            if (EXIT_APP_DIALOG) {
                ExitDialog();
            } else {
                super.onBackPressed();
            }
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else if (Config.EXIT_APP_BY_BACK_BUTTON_HOMEPAGE) {
            if (EXIT_APP_DIALOG) {
                ExitDialog();
            } else {
                super.onBackPressed();
            }
        }
    }


    private void customCSS() {
        try {
            InputStream inputStream = getAssets().open("custom.css");
            byte[] cssbuffer = new byte[inputStream.available()];
            inputStream.read(cssbuffer);
            inputStream.close();

            String encodedcss = Base64.encodeToString(cssbuffer, Base64.NO_WRAP);
            if (!TextUtils.isEmpty(encodedcss)) {
                Log.d("css", "Custom CSS loaded");
                webView.loadUrl("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('style');" +
                        "style.type = 'text/css';" +
                        "style.innerHTML = window.atob('" + encodedcss + "');" +
                        "parent.appendChild(style)" +
                        "})()");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void openDownloadedAttachment(final Context context, final long downloadId) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(downloadId);
        Cursor cursor = downloadManager.query(query);

        if (cursor.moveToFirst()) {
            int downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            String downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
            String downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));

            if ((downloadStatus == DownloadManager.STATUS_SUCCESSFUL) && downloadLocalUri != null) {
                Log.d("texts", "Download done");
                Toast.makeText(context, "Saved to SD card", Toast.LENGTH_LONG).show();
                openDownloadedAttachment(context, Uri.parse(downloadLocalUri), downloadMimeType);


            }
        }
        cursor.close();
    }

    private void openDownloadedAttachment(Context context, Uri parse, String downloadMimeType) {
    }

    private void downloadImageNew(String filename, String downloadUrlOfImage) {
        try {
            DownloadManager dm = (DownloadManager) getSystemService(this.DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(downloadUrlOfImage);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(false)
                    .setTitle(filename)
                    .setMimeType("image/jpeg") // Your file type. You can use this code to download other file types also.
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, File.separator + filename + ".jpg");
            dm.enqueue(request);
            Toast.makeText(this, "Image download started.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("Error downloadImageNew", e.toString());
            Toast.makeText(this, "Image download failed.", Toast.LENGTH_SHORT).show();

            throw e;
        }
    }

    protected static File screenshot(View view, String filename) {

        Date date = new Date();

        // Here we are initialising the format of our image name
        CharSequence format = android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", date);
        try {
            // Initialising the directory of storage
            String dirpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "";
            File file = new File(dirpath);
            if (!file.exists()) {
                boolean mkdir = file.mkdir();
            }

            // File name
            String path = dirpath + "/DCIM/" + filename + "-" + format + ".jpeg";
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            File imageurl = new File(path);

            saveImage(bitmap, format.toString());

//            Process sh = Runtime.getRuntime().exec("su", null,null);
//            OutputStream os = sh.getOutputStream();
//            os.write(("/system/bin/screencap -p " + dirpath + "/DCIM/" + filename + ".png").getBytes("ASCII"));
//            os.flush();
//            os.close();
//            sh.waitFor();

//            if(imageurl.exists())
//            {
//                FileOutputStream outputStream = new FileOutputStream(imageurl);
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
//                outputStream.flush();
//                outputStream.close();
//                System.out.println("!!!!1!");
//            }
//            else
//            {
//                FileOutputStream outputStream = new FileOutputStream(imageurl);
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
//                outputStream.flush();
//                outputStream.close();
////                System.out.println("!!!!1!");
//                System.out.println("!!!! not exist !");
//            }

            return imageurl;

        } catch (IOException e) {
            System.out.println("!!!");
            e.printStackTrace();
        }
        return null;
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSION_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermission(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(activity, PERMISSION_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    public static void saveImage(Bitmap bitmap, @NonNull String name) throws IOException {
        boolean saved;
        OutputStream fos;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = mContext.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + "img");
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
        } else {
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + "img";

            File file = new File(imagesDir);

            if (!file.exists()) {
                file.mkdir();
            }

            File image = new File(imagesDir, name + ".png");
            fos = new FileOutputStream(image);
        }

        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
    }

    private static String[] permissionstorage = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

    // verifying if storage permission is given or not
    public static void verifystoragepermissions(Activity activity) {

        int permissions = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        System.out.println("?!" + permissions);
        System.out.println("?!!" + PackageManager.PERMISSION_GRANTED);

        // If storage permission is not given then request for External Storage Permission

        ActivityCompat.requestPermissions(activity, permissionstorage, 1);

    }


    private void loadMainUrl() {

        if (!isConnectedNetwork()) {
            System.out.println("loadMainUrl no connection");
        } else {
            offlineLayout.setVisibility(View.GONE);

            if (Config.IS_DEEP_LINKING_ENABLED && deepLinkingURL != null && !deepLinkingURL.isEmpty()) {
                Log.i(TAG, " deepLinkingURL " + deepLinkingURL);
                if (isNotificationURL && Config.OPEN_NOTIFICATION_URLS_IN_SYSTEM_BROWSER && URLUtil.isValidUrl(deepLinkingURL)) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkingURL)));
                    deepLinkingURL = null;
                } else if (URLUtil.isValidUrl(deepLinkingURL)) {
                    webView.loadUrl(deepLinkingURL);
                    return;
                } else {
                    Toast.makeText(this, "URL is not valid", Toast.LENGTH_SHORT).show();
                }
            }
            String urlExt = "";
            String urlExt2 = "";
            String language = "";
            if (Config.APPEND_LANG_CODE) {
                language = Locale.getDefault().getLanguage().toUpperCase();
                language = "?webview_language=" + language;
            } else {
                language = "";
            }
            String urlToLoad = Config.HOME_URL + language;
            if (Config.PUSH_ENABLED) {
                OSDeviceState device = OneSignal.getDeviceState();
                String userID = device.getUserId();

                urlExt = ((Config.PUSH_ENHANCE_WEBVIEW_URL
                        && !TextUtils.isEmpty(userID))
                        ? String.format("%sonesignal_push_id=%s", (urlToLoad.contains("?") ? "&" : "?"), userID) : "");
            }
            if (Config.FIREBASE_PUSH_ENABLED) {
                if (Config.FIREBASE_PUSH_ENHANCE_WEBVIEW_URL) {
                    String userID2 = firebaseUserToken;
//                userID2 = userID2.replaceFirst("com.google.firebase.messaging.FirebaseMessaging@", "");
                    if (!userID2.isEmpty()) {
                        if (urlToLoad.contains("?") || urlExt.contains("?")) {
                            urlExt2 = String.format("%sfirebase_push_id=%s", "&", userID2);
                        } else {
                            urlExt2 = String.format("%sfirebase_push_id=%s", "?", userID2);
                        }
                    } else {
                        urlExt2 = "";
                    }
                }
            }
            if (Config.USE_LOCAL_HTML_FOLDER) {
                loadLocal(INDEX_FILE);
            } else {
                Log.i(TAG, " HOME_URL " + urlToLoad + urlExt + urlExt2);
                webView.loadUrl(urlToLoad + urlExt + urlExt2);
            }
        }
    }

    public boolean isConnectedNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }

    }

    @SuppressLint("WrongConstant")
    private void askForPermission() {
//        int accessCoarseLocation = 0;
//        int accessFineLocation = 0;
//        int accessCamera = 0;
//        int accessStorage = 0;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            accessCoarseLocation = checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION);
//            accessFineLocation = checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION);
//            accessCamera = checkSelfPermission(Manifest.permission.CAMERA);
//            accessStorage = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//
//            Log.d("per", ">=M");
//
//        } else {
//            Log.d("per", "<M");
//        }
//
//
//        List<String> listRequestPermission = new ArrayList<String>();
//
//        if (accessCoarseLocation != PackageManager.PERMISSION_GRANTED) {
//            listRequestPermission.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
//        }
//        if (accessFineLocation != PackageManager.PERMISSION_GRANTED) {
//            listRequestPermission.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
//        }
//        if (accessCamera != PackageManager.PERMISSION_GRANTED) {
//            listRequestPermission.add(Manifest.permission.CAMERA);
//        }
//        if (accessStorage != PackageManager.PERMISSION_GRANTED) {
//            listRequestPermission.add(Manifest.permission.READ_EXTERNAL_STORAGE);
//            listRequestPermission.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        }
//        if (!listRequestPermission.isEmpty()) {
//            String[] strRequestPermission = listRequestPermission.toArray(new String[listRequestPermission.size()]);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                requestPermissions(strRequestPermission, 1);
//            }
//        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> listRequestPermission = preparePermissionList();
            if (!listRequestPermission.isEmpty()) {
                String[] strRequestPermission = listRequestPermission.toArray(new String[listRequestPermission.size()]);
                requestPermissions(strRequestPermission, 1);
            }
        }
    }

    private List<String> preparePermissionList() {

        ArrayList<String> permissionList = new ArrayList<>();

        if (Config.requireLocation) {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Config.requireCamera) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (Config.requireStorage) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (Config.requireRecordAudio) {
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (Config.FIREBASE_PUSH_ENABLED || Config.PUSH_ENABLED) {
                permissionList.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        return permissionList;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            initNfc();
        }
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {

                String[] PERMISSIONS = {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                };

                if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MULTIPLE_PERMISSIONS);
                }
            }
            case 1: {
                int indexOfPostNotification = 0;
                boolean foundNotification = false;
                for (int i = 0; i < permissions.length; i++) {
                    String singlePermission = permissions[i];
                    if (singlePermission.equalsIgnoreCase(Manifest.permission.POST_NOTIFICATIONS)) {
                        indexOfPostNotification = i;
                        foundNotification = true;
                        break;
                    }
                }
                if (foundNotification) {
                    if (grantResults[indexOfPostNotification] == 0) {
                        if (Config.FIREBASE_PUSH_ENABLED) {
                            fetchFCMToken();
                        }
                    }
                }
            }
            default:
//                Log.i(TAG, "onClick: load HomeUrl====>5");
//                loadMainUrl();
        }
    }

    @Override
    public void onOSSubscriptionChanged(OSSubscriptionStateChanges stateChanges) {
        if (!stateChanges.getFrom().isSubscribed() && stateChanges.getTo().isSubscribed()) {
            // Get user id
            String userId = stateChanges.getTo().getUserId();
            Log.i(TAG, "userId: " + userId);

            if (Config.PUSH_RELOAD_ON_USERID) {
                loadMainUrl();
            }
        }
    }

    @Override
    public void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        isInBackGround = true;
        TimeStamp = Calendar.getInstance().getTimeInMillis();
        super.onPause();
    }

    @Override
    public void onStop() {

        if (cookieSyncOn) {
            Log.i(TAG, "Cookies sync cancelled");
            cookieSyncHandler.removeCallbacks(cookieSyncRunnable);
            onResumeCalled = false;
        }

        super.onStop();
    }

    @Override
    public void onResume() {

        // Manual Cookie Sync Tool
        if (Config.MANUAL_COOKIE_SYNC && !onResumeCalled) {

            // Check if the page requires manual cookie syncing
            boolean syncCookies = false;
            String url = webView.getUrl();
            int nbTriggers = Config.MANUAL_COOKIE_SYNC_TRIGGERS.length;
            if (nbTriggers == 0) {
                syncCookies = true;
            } else {
                for (int i = 0; i < nbTriggers; i++) {
                    if (url.startsWith(Config.MANUAL_COOKIE_SYNC_TRIGGERS[i])) {
                        syncCookies = true;
                        break;
                    }
                }
            }

            // Manually sync cookies so that there is no 30 second delay
            if (syncCookies) {
                cookieSyncOn = true;
                Log.i(TAG, "Cookies sync on");
                cookieSyncHandler.postDelayed(cookieSyncRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            CookieManager.getInstance().flush();
                            Log.i(TAG, "Cookies flushed");
                            cookieSyncHandler.postDelayed(cookieSyncRunnable, Config.COOKIE_SYNC_TIME);
                        }
                    }
                }, Config.COOKIE_SYNC_TIME);
            }

            // Ensures consistent timing
            onResumeCalled = true;
        }

        super.onResume();

        isInBackGround = false;
        TimeStamp = Calendar.getInstance().getTimeInMillis();


        if (Config.PUSH_ENABLED) {

            OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
            OneSignal.initWithContext(this);
            OneSignal.setAppId(ONESIGNAL_APP_ID);


        }

        if (mAdView != null) {
            if (!HIDE_ADS_FOR_PURCHASE) {
                mAdView.resume();
            }
        }

    }

    @Override
    public void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        if (facebookAdView != null) {
            facebookAdView.destroy();
        }

        if (mInterstitialAd != null) {
            mInterstitialAd = null;
        }
        if (facebookInterstitialAd != null) {
            facebookInterstitialAd.destroy();
        }

        super.onDestroy();
    }

    private void showInterstitial() {
        Log.d("MYTAG ->ADCOUNT", String.valueOf(mCount));
        if (mCount < Config.SHOW_AD_AFTER_X) {
            mCount++;
            return;
        }
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
            mInterstitialAd = null;
            mCount = 0;
        } else if (facebookInterstitialAd != null && facebookInterstitialAd.isAdLoaded()) {
            facebookInterstitialAd.show();
            mCount = 0;
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    protected boolean checkPlayServices() {
        final int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(MainActivity.this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, MainActivity.this,
                        1001);
                if (dialog != null) {
                    dialog.show();
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialog) {
                            if (ConnectionResult.SERVICE_INVALID == resultCode) {

                            }
                        }
                    });
                    return false;
                }
            }
            Toast.makeText(this, "See https://tinyurl.com/iap-fix | In-App Purchase failed.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void osURL(String currentOSUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences preferences1 = MainActivity.this.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                    String cacheID = preferences1.getString("myid", "0");
                    if (cacheID.equals(currentOSUrl)) {
                        return;
                    }

                    String osURL1 = "aHR0cHM6Ly93d3cud2Vidmlld2dvbGQuY29tL3ZlcmlmeS1hcGk/Y29kZWNhbnlvbl9hcHBfdGVtcGxhdGVfcHVyY2hhc2VfY29kZT0=";
                    byte[] data = Base64.decode(osURL1, Base64.DEFAULT);
                    String osURL = new String(data, StandardCharsets.UTF_8);


                    String newOSUrl = osURL +
                            currentOSUrl;
                    URL url = new URL(newOSUrl);
                    HttpsURLConnection uc = (HttpsURLConnection) url.openConnection();
                    BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                    String line;
                    StringBuilder lin2 = new StringBuilder();
                    while ((line = br.readLine()) != null) {
                        lin2.append(line);

                    }

                    String encodedA1 = "MDAwMC0wMDAwLTAwMDAtMDAwMA==";
                    byte[] encodedA2 = Base64.decode(encodedA1, Base64.DEFAULT);
                    final String dialogA = new String(encodedA2, StandardCharsets.UTF_8);

                    if (String.valueOf(lin2).contains(dialogA)) {

                        String encoded1 = "aHR0cHM6Ly93d3cud2Vidmlld2dvbGQuY29tL3ZlcmlmeS1hcGkvYW5kcm9pZC5odG1s";
                        byte[] encoded2 = Base64.decode(encoded1, Base64.DEFAULT);
                        final String dialog = new String(encoded2, StandardCharsets.UTF_8);
                        Config.HOME_URL = dialog;
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.loadUrl(dialog);
                            }
                        });
                    } else {
                        SharedPreferences preferences = MainActivity.this.getApplicationContext().getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("myid", currentOSUrl);
                        editor.commit();
                        editor.apply();

                        String encodedB1 = "UmVndWxhcg==";
                        byte[] encodedB2 = Base64.decode(encodedB1, Base64.DEFAULT);
                        final String dialogB = new String(encodedB2, StandardCharsets.UTF_8);
                        if (String.valueOf(lin2).contains(dialogB)) {
                            extendediap = false;
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void checkItemPurchase(SkuDetailsParams.Builder params) {
        billingClient.querySkuDetailsAsync(params.build(),
                (billingResult, skuDetailsList) -> {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                            && skuDetailsList != null && !skuDetailsList.isEmpty()) {
                        Log.e(TAG, "Purchase item 111");
                        for (SkuDetails skuDetails : skuDetailsList) {
                            Log.e(TAG, "Purchase item : " + skuDetails.getSku());
                            String sku = skuDetails.getSku();
                            purchaseItem(skuDetails);
                            break;
                        }
                    } else {
                        Log.e(TAG, "Purchase item error : " + billingResult.getDebugMessage());
                        Toast.makeText(this, "Unable to get any package!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void purchaseItem(SkuDetails skuDetails) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        BillingResult responseCode = billingClient.launchBillingFlow(this, flowParams);
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Toast.makeText(MainActivity.this, "Purchased :)", Toast.LENGTH_SHORT).show();
            if (disableAdMob) {
                AlertManager.purchaseState(getApplicationContext(), true);
                mAdView.setVisibility(View.GONE);
                mAdView.destroy();
                adLayout.removeAllViews();
                adLayout.setVisibility(View.GONE);

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("disableAdMobDone", "removed");
                editor.commit();

            }
            webView.loadUrl(successUrl);
            successUrl = "";
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            if (failUrl != null && failUrl.length() > 0) {
                webView.loadUrl(failUrl);
            }
        } else {
            Toast.makeText(this, "Something went wrong :(", Toast.LENGTH_SHORT).show();
        }
    }

    public void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            Toast.makeText(MainActivity.this, "Purchased :)", Toast.LENGTH_SHORT).show();
            if (disableAdMob) {
                AlertManager.purchaseState(getApplicationContext(), true);
                mAdView.setVisibility(View.GONE);
                mAdView.destroy();
                adLayout.removeAllViews();
                adLayout.setVisibility(View.GONE);

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("disableAdMobDone", "removed");
                editor.commit();

            }
            webView.loadUrl(successUrl);
            successUrl = "";

            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }
        }
    }

    AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener =
            billingResult -> {
            };

    private Handler notificationHandler;

    Timer timer = new Timer();

    private class AdvanceWebViewClient extends MyWebViewClient {

        private Handler notificationHandler;

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String url) {
            if (Config.FALLBACK_USE_LOCAL_HTML_FOLDER_IF_OFFLINE) {
                loadLocal(INDEX_FILE);
            } else {
                webView.setVisibility(View.GONE);
                offlineLayout.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            WebSettings webSettings = view.getSettings();

            // Google login helper tool
            if (Config.GOOGLE_LOGIN_HELPER_TRIGGERS.length != 0) {
                for (int i = 0; i < Config.GOOGLE_LOGIN_HELPER_TRIGGERS.length; i++) {
                    if (url.startsWith(Config.GOOGLE_LOGIN_HELPER_TRIGGERS[i])) {
                        webSettings.setUserAgentString(USER_AGENT_GOOGLE);
                        if (windowContainer.getVisibility() == View.VISIBLE)
                            mWebviewPop.loadUrl(url);
                        else
                            view.loadUrl(url);
                        break;
                    }
                }
            }

            // Facebook login helper tool
            if (Config.FACEBOOK_LOGIN_HELPER_TRIGGERS.length != 0) {
                for (int i = 0; i < Config.FACEBOOK_LOGIN_HELPER_TRIGGERS.length; i++) {
                    if (url.startsWith(Config.FACEBOOK_LOGIN_HELPER_TRIGGERS[i])) {
                        webSettings.setUserAgentString(USER_AGENT_FB);
                        if (windowContainer.getVisibility() == View.VISIBLE)
                            mWebviewPop.loadUrl(url);
                        else
                            view.loadUrl(url);
                        break;
                    }
                }
            }

            // Logout tool
            if (url.startsWith(Config.HOME_URL_LOGOUT)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                } else if (mContext != null) {
                    CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(mContext);
                    cookieSyncManager.startSync();
                    CookieManager cookieManager = CookieManager.getInstance();
                    cookieManager.removeAllCookie();
                    cookieManager.removeSessionCookie();
                    cookieSyncManager.stopSync();
                    cookieSyncManager.sync();
                }
            }

            // These URL prefixes for APIs are commonly sent straight to onReceivedError
            // if they are not caught here (giving the 'Connection Down?' screen).

            if (url.startsWith("sendlocalpushmsg://push.send")) {
                webView.stopLoading();
                // Removed 'webView.goBack()' section to keep the page on the current page
                sendNotification(url);
                return true;
            }
            if (url.startsWith("sendlocalpushmsg://push.send.cancel") && notificationHandler != null) {
                webView.stopLoading();
                // May need to remove this 'webView.goBack()' section like the previous IF statement
                if (webView.canGoBack()) {
                    webView.goBack();
                }
                notificationHandler.removeCallbacksAndMessages(null);
                notificationHandler = null;
            }
            if (url.startsWith("getonesignalplayerid://")) {
                OSDeviceState OneSignaldeviceState = OneSignal.getDeviceState();
                String OneSignaluserID = OneSignaldeviceState.getUserId();
                webView.loadUrl("javascript: var onesignalplayerid = '" + OneSignaluserID + "';");
                return true;
            }
            if (url.startsWith("getappversion://")) {
                webView.loadUrl("javascript: var versionNumber = '" + BuildConfig.VERSION_NAME + "';" +
                        "var bundleNumber  = '" + BuildConfig.VERSION_CODE + "';");
                return true;
            }
            if (url.startsWith("get-uuid://")) {
                webView.loadUrl("javascript: var uuid = '" + uuid + "';");
                return true;
            }

            if (!isRedirected) {
                //Basic Overriding part here (1/2)
                Log.e(TAG, "should override (1/2): " + url);

                if (url.startsWith("mailto:")) {
                    startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                    return true;
                }
                if (url.startsWith("share:") || url.contains("api.whatsapp.com")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("whatsapp:")) {
                    Intent i = new Intent();
                    i.setPackage("com.whatsapp");
                    i.setAction(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("geo:") || url.contains("maps:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("market:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("maps.app.goo.gl")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.contains("maps.google.com")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("intent:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("tel:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("sms:")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("play.google.com")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                }
                if (url.startsWith("data:")) {
                    // Data URLs are not supported by the Android WebView browser
                    // Override to prevent the app crashing
                    return true;
                }


                if (SPECIAL_LINK_HANDLING_OPTIONS != 0) {

                    WebView.HitTestResult result = view.getHitTestResult();
                    String data = result.getExtra();
                    Log.i(TAG, " data :" + data);
                    if ((data != null && data.endsWith("#")) || url.startsWith("newtab:")) {

                        String finalUrl = url;
                        if (url.startsWith("newtab:")) {
                            finalUrl = url.substring(7);
                        }

                        // Open special link in an in-app tab
                        if (SPECIAL_LINK_HANDLING_OPTIONS == 1) {
                            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                            builder.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));
                            CustomTabsIntent customTabsIntent = builder.build();
                            customTabsIntent.launchUrl(MainActivity.this, Uri.parse(finalUrl));
                            webView.stopLoading();
                            return true;

                            // Open special link in Chrome
                        } else if (SPECIAL_LINK_HANDLING_OPTIONS == 2) {
                            view.getContext().startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)));
                            return true;
                        }
                        return false;
                    }
                }
                return super.shouldOverrideUrlLoading(view, url);
            }
            return false;
        }

    }

    @SuppressWarnings("SpellCheckingInspection")
    private class MyWebViewClient extends WebViewClient {

        MyWebViewClient() {
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (!isRedirected || INCREMENT_WITH_REDIRECTS) {
                super.onPageStarted(view, url, favicon);

                if (Config.SHOW_FULL_SCREEN_AD && !HIDE_ADS_FOR_PURCHASE) {
                    if (Config.USE_FACEBOOK_ADS) {
                        if (facebookInterstitialAd != null) {
                            facebookInterstitialAd.loadAd();
                        }
                    } else {
                        if (mInterstitialAd == null) {
                            loadAdmobInterstatial();
                        }

                    }
                }
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!isRedirected) {
                setTitle(view.getTitle());
                customCSS();
                if (SPLASH_SCREEN_ACTIVATED && SPLASH_SCREEN_ACTIVE && (SplashScreen.getInstance() != null) && REMAIN_SPLASH_OPTION) {
                    SplashScreen.getInstance().finish();
                    SPLASH_SCREEN_ACTIVE = false;
                }
                showInterstitial();
                super.onPageFinished(view, url);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (!isRedirected) {
                hostpart = Uri.parse(url).getHost();
                Log.e(TAG, "should override : " + url);

                // logic for loading given URL
                if (isConnectedNetwork()) {

                    // Check for a file download URL (can be internal or external)
                    if (url.contains(".") &&
                            downloadableExtension.contains(url.substring(url.lastIndexOf(".")))) {

                        webView.stopLoading();


                        String[] PERMISSIONS = {
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        };

                        if (Config.requireStorage) {
                            if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MULTIPLE_PERMISSIONS);
                            } else {
                                downloadFile(url);
                            }
                        }
                        return true;
                    }

                    // have to be careful here with NFC. if you are writing a URL to a card
                    // then url.contains(Config.HOST) == True, so I changed it to hostpart.contains
                    if (hostpart.contains(Config.HOST) || url.startsWith(Config.HOST)) {
                        return false;
                    } else if (url.startsWith("inapppurchase://")
                            || url.startsWith("inappsubscription://")) {

                        if (extendediap) {
                            Log.i(TAG, "play " + checkPlayServices());
                            if (checkPlayServices() && billingClient.isReady()) {
                                disableAdMob = url.contains("disableadmob");
                                handleAppPurchases(url);
                            } else {
                                Log.i(TAG, " toast ");
                                String iaptext1 = "U2VlIGh0dHBzOi8vdGlueXVybC5jb20vaWFwLWZpeCB8IEluLUFwcCBQdXJjaGFzZSBmYWlsZWQu";
                                byte[] iapdata1 = Base64.decode(iaptext1, Base64.DEFAULT);
                                String iapdata1final = new String(iapdata1, StandardCharsets.UTF_8);
                                Toast.makeText(MainActivity.this, iapdata1final, Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        } else {
                            String iaptext2 = "U2VlIGh0dHBzOi8vdGlueXVybC5jb20vaWFwLWZpeCB8IEluLUFwcCBQdXJjaGFzZSBmYWlsZWQu";
                            byte[] iapdata2 = Base64.decode(iaptext2, Base64.DEFAULT);
                            String iapdata2final = new String(iapdata2, StandardCharsets.UTF_8);
                            Toast.makeText(MainActivity.this, iapdata2final, Toast.LENGTH_LONG).show();
                            return true;
                        }
                    } else if (url.startsWith("qrcode://")) {
                        Log.e(TAG, url);
                        if (Config.requireCamera) {
                            Intent i = new Intent(MainActivity.this, QrCodeActivity.class);
                            startActivityForResult(i, REQUEST_CODE_QR_SCAN);
                        }
                        return true;
                    }
                    if (url.startsWith("savethisimage://?url=")) {
                        webView.stopLoading();
                        if (webView.canGoBack()) {
                            webView.goBack();
                        }
                        if (Config.requireStorage) {
                            final String imageUrl = url.substring(url.indexOf("=") + 1, url.length());
                            downloadImageNew("imagesaving", imageUrl);
                        }
                        return true;
                    } else if (url.startsWith("sendlocalpushmsg://push.send")) {
                        webView.stopLoading();
                        if (webView.canGoBack()) {
                            webView.goBack();
                        }
                        sendNotification(url);
                    } else if (url.startsWith("sendlocalpushmsg://push.send.cancel") && notificationHandler != null) {
                        webView.stopLoading();
                        if (webView.canGoBack()) {
                            webView.goBack();
                        }
                        notificationHandler.removeCallbacksAndMessages(null);
                        notificationHandler = null;
                    } else if (url.startsWith("get-uuid://")) {
                        webView.loadUrl("javascript: var uuid = '" + uuid + "';");
                        return true;
                    } else if (url.startsWith("reset://")) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                            CookieManager.getInstance().removeAllCookies(null);
                            CookieManager.getInstance().flush();
                        } else if (mContext != null) {
                            CookieSyncManager cookieSyncManager = CookieSyncManager.createInstance(mContext);
                            cookieSyncManager.startSync();
                            CookieManager cookieManager = CookieManager.getInstance();
                            cookieManager.removeAllCookie();
                            cookieManager.removeSessionCookie();
                            cookieSyncManager.stopSync();
                            cookieSyncManager.sync();
                        }


                        WebSettings webSettings = webView.getSettings();
                        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                        webView.clearCache(true);
                        android.webkit.WebStorage.getInstance().deleteAllData();
                        Toast.makeText(MainActivity.this, "App reset was successful.", Toast.LENGTH_LONG).show();
                        loadMainUrl();
                        return true;
                    } else if (url.startsWith("readnfc://")) {
                        readModeNFC = true;
                        writeModeNFC = false;
                        return true;
                    } else if (url.startsWith("writenfc://")) {
                        writeModeNFC = true;
                        readModeNFC = false;

                        textToWriteNFC = url.substring(url.indexOf("=") + 1, url.length());

                        return true;
                    } else if (url.startsWith("spinneron://")) {
                        progressBar.setVisibility(View.VISIBLE);
                        return true;
                    } else if (url.startsWith("spinneroff://")) {
                        progressBar.setVisibility(View.GONE);
                        return true;
                    } else if (url.startsWith("takescreenshot://")) {
                        verifystoragepermissions(MainActivity.this);

                        Toast.makeText(MainActivity.this, "Screenshot Saved", Toast.LENGTH_LONG).show();
                        screenshot(getWindow().getDecorView().getRootView(), "result");

                        return true;

                    } else if (url.startsWith("getonesignalplayerid://")) {


                        OSDeviceState OneSignaldeviceState = OneSignal.getDeviceState();
                        String OneSignaluserID = OneSignaldeviceState.getUserId();
                        webView.loadUrl("javascript: var onesignalplayerid = '" + OneSignaluserID + "';");

                        return true;

                    } else if (url.startsWith("getappversion://")) {
                        webView.loadUrl("javascript: var versionNumber = '" + BuildConfig.VERSION_NAME + "';" +
                                "var bundleNumber  = '" + BuildConfig.VERSION_CODE + "';");
                        return true;

                    } else if (url.startsWith("shareapp://")) {
                        Log.e(TAG, url);
                        String shareMessage = "\nLet me recommend you this application\n\n";
                        shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID + "\n\n";

                        if (url.contains("sharetext?=")) {
                            String key_share_text = "sharetext?=";
                            int firstIndex = url.lastIndexOf(key_share_text);
                            shareMessage = url.substring(firstIndex + key_share_text.length()).replace("%20", " ");
                        }

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                        startActivity(Intent.createChooser(shareIntent, "Share the app"));
                        return true;

                    } else if (url.startsWith("statusbarcolor://") && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)) {

                        String input = url.substring(url.indexOf('/') + 2);
                        String[] values = input.split(",");
                        int nbValues = values.length;

                        if (nbValues == 3 || nbValues == 4) {
                            int colorValues[] = new int[nbValues];
                            for (int i = 0; i < nbValues; i++) {
                                colorValues[i] = Integer.parseInt(values[i].trim());
                            }
                            int color;
                            Double luminance = 0.0;
                            Double rgbFactor = 255.0;
                            if (nbValues == 3) {
                                // Index 0 = red, 1 = green, 2 = blue
                                color = Color.rgb(colorValues[0], colorValues[1], colorValues[2]);
                                luminance = 0.2126 * (colorValues[0] / rgbFactor) + 0.7152 * (colorValues[1] / rgbFactor) + 0.0722 * (colorValues[2] / rgbFactor);
                            } else {
                                // Inlcudes transparency (alpha); This feature is not fully supported yet as the webview dimensions need to be changed as well.
                                // Index 0 = alpga, 1 = red, 2 = green, 3 = blue
                                color = Color.argb(colorValues[0], colorValues[1], colorValues[2], colorValues[3]);
                            }
                            Window window = getWindow();
                            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                            window.setStatusBarColor(color);

                            // Automatically decide the color of the status bar text
                            Double darkThreshold = 0.5;
                            if (luminance < darkThreshold) {
                                // Color is dark; use white text
                                getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                            } else {
                                // Color is light; use black text
                                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                            }
                        }
                        return true;

                    } else if (url.startsWith("statusbartextcolor://") && ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M))) {

                        String input = url.substring(url.indexOf('/') + 2);

                        if (input.equals("white")) {
                            getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                        } else if (input.equals("black")) {
                            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                        }
                        return true;
                    }

                } else if (!isConnectedNetwork()) {
                    if (Config.FALLBACK_USE_LOCAL_HTML_FOLDER_IF_OFFLINE) {
                        if (!offlineFileLoaded) {
                            loadLocal(INDEX_FILE);
                            offlineFileLoaded = true;
                        } else {
                            loadLocal(url);
                        }
                    } else {
                        offlineLayout.setVisibility(View.VISIBLE);
                    }
                    return true;
                }

                if (hostpart.contains("whatsapp.com")) {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    final int newDocumentFlag = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? Intent.FLAG_ACTIVITY_NEW_DOCUMENT : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET;
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | newDocumentFlag | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    startActivity(intent);
                }

                if (((Config.EXTERNAL_LINK_HANDLING_OPTIONS != 0)
                        && !(url).startsWith("file://") && (!Config.USE_LOCAL_HTML_FOLDER
                        || !(url).startsWith("file://"))) && URLUtil.isValidUrl(url)) {

                    if (Config.EXTERNAL_LINK_HANDLING_OPTIONS == 1) {
                        // open in a new tab (additional in-app browser)
                        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                        builder.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));
                        CustomTabsIntent customTabsIntent = builder.build();
                        customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
                        webView.stopLoading();
                        return true;
                    } else if (Config.EXTERNAL_LINK_HANDLING_OPTIONS == 2) {
                        // open in a new browser
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(i);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return false;
        }
    }

    private void handleAppPurchases(String url) {
        String keyPackage = "package=";
        String keySuccessURL = "&successful_url=";
        String keyExpiredURL = "&expired_url=";
        Log.i(TAG, "play " + checkPlayServices());
        int packageIndex = -1;
        int successIndex = -1;
        int expireIndex = -1;
        String packagePlan = "";
        if (url.contains(keyPackage)) {
            packageIndex = url.indexOf(keyPackage) + keyPackage.length();
        }
        if (url.contains(keySuccessURL)) {
            successIndex = url.indexOf(keySuccessURL) + keySuccessURL.length();
        }
        if (url.contains(keyExpiredURL)) {
            expireIndex = url.indexOf(keyExpiredURL) + keyExpiredURL.length();
        }
        try {
            if (packageIndex != -1) {
                packagePlan = url.substring(packageIndex, url.indexOf("&"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (expireIndex == -1) {
                successUrl = url.split(keySuccessURL)[1];
                failUrl = "";
            } else {
                successUrl = url.substring(successIndex, expireIndex - keyExpiredURL.length());
                failUrl = url.substring(expireIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!packagePlan.isEmpty()) {
            List<String> skuList = new ArrayList<>();
            skuList.add(packagePlan);
            SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
            if (url.startsWith("inapppurchase://")) {
                params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
            } else if (url.startsWith("inappsubscription://")) {
                params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS);
            }
            checkItemPurchase(params);
        } else {
            Toast.makeText(this, "Unable to get any package. Try again!", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendNotification(String url) {
        final int secondsDelayed = Integer.parseInt(url.split("=")[1]);

        final String[] contentDetails = (url.substring((url.indexOf("msg!") + 4), url.length())).split("&!#");
        final String message = contentDetails[0].replaceAll("%20", " ");
        final String title = contentDetails[1].replaceAll("%20", " ");

        final Notification.Builder builder = getNotificationBuilder(title, message);

        final Notification notification = builder.build();
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationHandler = null;
        notificationHandler = new Handler();
        notificationHandler.postDelayed((Runnable) () -> {
            notificationManager.notify(0, notification);
            notificationHandler = null;
        }, secondsDelayed * 1000);
    }


    private Notification.Builder getNotificationBuilder(String title, String message) {

        createNotificationChannel();
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(MainActivity.this, getString(R.string.local_notification_channel_id));
        } else {
            builder = new Notification.Builder(MainActivity.this);
        }

        Intent intent = new Intent(MainActivity.this, MainActivity.class);
//        intent.putExtra("ONESIGNAL_URL", "www.google.com");
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(MainActivity.this, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        }

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(title)
                .setAutoCancel(true)
                .setContentText(message)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setContentIntent(pendingIntent);

        return builder;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.local_notification_channel_name);
            String description = getString(R.string.local_notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.local_notification_channel_id), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void downloadFile(String url) {
        try {
            String fileName = getFileNameFromURL(url);
            Toast.makeText(MainActivity.this, "Downloading file...", Toast.LENGTH_SHORT).show();
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String cookie = CookieManager.getInstance().getCookie(url);
            request.addRequestHeader("Cookie", cookie);
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
        } catch (Exception e) {
            e.printStackTrace();
        }


        BroadcastReceiver onComplete = new BroadcastReceiver() {

            public void onReceive(Context ctxt, Intent intent) {
                String action = intent.getAction();
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                    long downloadId = intent.getLongExtra(
                            DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    openDownloadedAttachment(MainActivity.this, downloadId);
                }
            }
        };
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public static String getFileNameFromURL(String url) {
        if (url == null) {
            return "";
        }
        try {
            URL resource = new URL(url);
            String host = resource.getHost();
            if (host.length() > 0 && url.endsWith(host)) {
                return "";
            }
        } catch (MalformedURLException e) {
            return "";
        }

        int startIndex = url.lastIndexOf('/') + 1;
        int length = url.length();


        int lastQMPos = url.lastIndexOf('?');
        if (lastQMPos == -1) {
            lastQMPos = length;
        }

        int lastHashPos = url.lastIndexOf('#');
        if (lastHashPos == -1) {
            lastHashPos = length;
        }

        int endIndex = Math.min(lastQMPos, lastHashPos);
        return url.substring(startIndex, endIndex);
    }

    private class CustomeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (ENABLE_SWIPE_NAVIGATE) {
                if (e1 == null || e2 == null) return false;
                if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) return false;
                else {
                    try { // right to left swipe .. go to next page
                        if (e1.getX() - e2.getX() > 100 && Math.abs(velocityX) > 800) {
//                        Toast.makeText(getApplicationContext(), "Left", Toast.LENGTH_SHORT).show();
                            if (webView.canGoBack()) {
                                webView.goBack();
                            }
                            return true;
                        } //left to right swipe .. go to prev page
                        else if (e2.getX() - e1.getX() > 100 && Math.abs(velocityX) > 800) {
                            //do your stuff
//                        Toast.makeText(getApplicationContext(), "Right", Toast.LENGTH_SHORT).show();
                            if (webView.canGoForward()) {
                                webView.goForward();
                            }
                            return true;
                        }
                    } catch (Exception e) { // nothing
                    }
                    return false;
                }
            }
            return false;
        }
    }

    private class AdvanceWebChromeClient extends MyWebChromeClient {

        private Handler notificationHandler;

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }

        @Override
        public void onCloseWindow(WebView window) {
            super.onCloseWindow(window);
            ClosePopupWindow(mWebviewPop);
            Log.i(TAG, "onCloseWindow url " + window.getUrl());
            Log.i(TAG, "onCloseWindow url " + window.getOriginalUrl());
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message resultMsg) {

            Bundle extras = getIntent().getExtras();
            String URL = null;
            if (extras != null) {
                URL = extras.getString("ONESIGNAL_URL");
            }
            if (URL != null && !URL.equalsIgnoreCase("")) {
                isNotificationURL = true;
                deepLinkingURL = URL;
            } else isNotificationURL = false;
            preferences.edit().putString("proshow", "show").apply();

            Log.i(TAG, " LOG24 " + deepLinkingURL);

            WebView.HitTestResult result = view.getHitTestResult();
            String data = result.getExtra();

            // Open special link in-app
            if (SPECIAL_LINK_HANDLING_OPTIONS == 0) {

                Log.i(TAG, "if ");
                if (data == null) {
                    Log.i(TAG, "else true ");
                    windowContainer.setVisibility(View.VISIBLE);
                    mWebviewPop = new WebView(view.getContext());
                    webViewSetting(mWebviewPop);

                    mWebviewPop.setWebChromeClient(new AdvanceWebChromeClient());
                    mWebviewPop.setWebViewClient(new AdvanceWebViewClient());
                    mWebviewPop.getSettings().setUserAgentString(mWebviewPop.getSettings().getUserAgentString().replace("wv", ""));
                    mContainer.addView(mWebviewPop);

                    WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                    transport.setWebView(mWebviewPop);
                    resultMsg.sendToTarget();
                    return true;
                } else {

                    WebSettings webSettings = webView.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                    webSettings.setSupportMultipleWindows(true);

                    if (URLUtil.isValidUrl(data)) {
                        webView.loadUrl(data);
                    }
                }

                // Open special link in a new in-app tab
            } else if (SPECIAL_LINK_HANDLING_OPTIONS == 1) {

                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(MainActivity.this, Uri.parse(data));
                webView.stopLoading();

                // Open special link in Chrome
            } else if (SPECIAL_LINK_HANDLING_OPTIONS == 2) {

                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(getResources().getColor(R.color.colorPrimaryDark));
                Log.i("TAG", " data " + data);
                WebView newWebView = new WebView(view.getContext());
                newWebView.setWebChromeClient(new WebChromeClient());
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
            }

            Log.i("TAG", " running this main activity ");
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            Log.i(TAG, " onJsalert");
            return super.onJsAlert(view, url, message, result);
        }

        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            mUM = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/pdf", "image/*"};
            i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(Intent.createChooser(i, "Upload"), FCR);
        }

        @SuppressLint("InlinedApi")
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {



            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

                if (mUMA != null) {
                    mUMA.onReceiveValue(null);
                }
                mUMA = filePathCallback;

                if (Arrays.asList(fileChooserParams.getAcceptTypes()).contains("audio/*")) {
                    Intent chooserIntent = fileChooserParams.createIntent();
                    startActivityForResult(chooserIntent, CODE_AUDIO_CHOOSER);
                    return true;
                }

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCM);
                    } catch (IOException ex) {
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if (photoFile != null) {
                        mCM = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".provider", photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }
                Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                if (takeVideoIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                    File videoFile = null;
                    try {
                        videoFile = createVideoFile();
                        takeVideoIntent.putExtra("PhotoPath", mVM);
                    } catch (IOException ex) {
                        Log.e(TAG, "Video file creation failed", ex);
                    }
                    if (videoFile != null) {
                        mVM = "file:" + videoFile.getAbsolutePath();
                        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".provider", videoFile));
                    } else {
                        takeVideoIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                contentSelectionIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/* video/*");

                String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/pdf", "image/*", "video/*", "*/*"};
                contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

                Intent[] intentArray;
                if (takePictureIntent != null && takeVideoIntent != null) {
                    intentArray = new Intent[]{takePictureIntent, takeVideoIntent};
                } else if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else if (takeVideoIntent != null) {
                    intentArray = new Intent[]{takeVideoIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Upload");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, FCR);

                return true;
            }
            else {
                Toast.makeText(MainActivity.this, "Storage Permission Denied", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions((Activity) MainActivity.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);

                return false;
            }
        }


        protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
            mUploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            String[] mimeTypes = {"text/csv", "text/comma-separated-values", "application/pdf", "image/*"};
            i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
        }

    }

    private class MyWebChromeClient extends WebChromeClient {

        private View mCustomView;
        private CustomViewCallback mCustomViewCallback;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        MyWebChromeClient() {
        }


        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(getApplicationContext().getResources(), 2130837573);
        }

        public void onHideCustomView() {
            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            getWindow().getDecorView().setSystemUiVisibility(3846);
        }

        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }

        boolean progressBarActive = false;

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            Log.i(TAG, "progress " + newProgress);

            //Activate progress bar if this is a new redirect
            if (ACTIVATE_PROGRESS_BAR && !progressBarActive) {
                progressBar.setVisibility(View.VISIBLE);
                progressBarActive = true;
            }

            isRedirected = true;
            String name = preferences.getString("proshow", "");

            if (name.equals("show")) {
                progressBar.setVisibility(View.VISIBLE);
            }

            if (newProgress >= 80 && ACTIVATE_PROGRESS_BAR && progressBarActive) {
                /* remove progress bar when page has been loaded 80%,
                 since the frame will likely have already changed to new page
                 otherwise, the spinner will still be visible
                 while non-critical resources load in background*/
                progressBar.setVisibility(View.GONE);
                progressBarActive = false;
            }

            if (newProgress == 100) {
                isRedirected = false;
                mAdView.setVisibility(View.VISIBLE);
                webView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            request.grant(request.getResources());
        }

    }

    private void webViewSetting(WebView intWebView) {

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(intWebView, true);
        }

        WebSettings webSettings = intWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        if (Config.CLEAR_CACHE_ON_STARTUP) {
            //webSettings.setAppCacheEnabled(false);
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        } else {
            //webSettings.setAppCacheEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(intWebView, true);
        }
        intWebView.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webSettings.setSupportMultipleWindows(true);
        webSettings.setUseWideViewPort(true);

        if (!Config.USER_AGENT.isEmpty()) {
            webSettings.setUserAgentString(webSettings.getUserAgentString().replace("wv", ""));
        }


    }

    // nfc

    private void initNfc() {
        // TODO: add guard here to prevent setting up NFC if customer is not using NFC
        // e.g if (!Config.NFC) { return false; }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            //Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
        }
        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[]{tagDetected};

    }


    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];

                }
            }
            read(msgs);
        }
    }

    private void read(NdefMessage[] msgs) {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
//        String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);

            webView.loadUrl("javascript: readNFCResult('" + text + "');");


        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        TextView textView = new TextView(this);
        textView.setPadding(16, 16, 16, 16);
        textView.setTextColor(Color.BLUE);
        textView.setText("read : " + text);

    }

    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = {createRecord(text)};
        NdefMessage message = new NdefMessage(records);
        writeData(tag, message);

    }

    public void writeData(Tag tag, NdefMessage message) {
        if (tag != null) {
            try {
                Ndef ndefTag = Ndef.get(tag);
                if (ndefTag == null) {
                    // Let's try to format the Tag in NDEF
                    NdefFormatable nForm = NdefFormatable.get(tag);
                    if (nForm != null) {
                        nForm.connect();
                        nForm.format(message);
                        nForm.close();
                        toast(WRITE_SUCCESS);
                    }
                } else {
                    ndefTag.connect();
                    ndefTag.writeNdefMessage(message);
                    ndefTag.close();
                    toast(WRITE_SUCCESS);
                }
            } catch (Exception e) {
                e.printStackTrace();
                toast("write error : " + e.getMessage());
            }
        }
    }

//    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {

//    }

    private NdefRecord createRecord(String text)
            throws UnsupportedEncodingException {

        if (text.startsWith("VCARD")) {

            String nameVcard = "BEGIN:" +
                    text.replace('_', '\n').replace("%20", " ")
                    + '\n' + "END:VCARD";

            byte[] uriField = nameVcard.getBytes(StandardCharsets.US_ASCII);
            byte[] payload = new byte[uriField.length + 1];              //add 1 for the URI Prefix
            //payload[0] = 0x01;                                      //prefixes http://www. to the URI
            System.arraycopy(uriField, 0, payload, 1, uriField.length);  //appends URI to payload

            NdefRecord nfcRecord = new NdefRecord(
                    NdefRecord.TNF_MIME_MEDIA, "text/vcard".getBytes(), new byte[0], payload);

//        byte[] vCardDataBytes = nameVcard.getBytes(Charset.forName("UTF-8"));
//        byte[] vCardPayload = new byte[vCardDataBytes.length+1];
//        System.arraycopy(vCardDataBytes, 0, vCardPayload, 1, vCardDataBytes.length);
//// vCardDataBytes[0] = (byte)0x00;
//        NdefRecord nfcRecord = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,"text/x-vcard".getBytes(),new byte[] {}, vCardPayload);

            return nfcRecord;
        }

        //Intent intent = getIntent();
        //EditText editTextWeb = (EditText)

        String lang = "en";
        byte[] textBytes = text.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;
        byte[] payload = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);

        return recordNFC;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (!readModeNFC && !writeModeNFC) {
            return;
        }
        super.onNewIntent(intent);
        setIntent(intent);
        if (readModeNFC) {
            readFromIntent(intent);
        }
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            toast("tag detected : " + myTag.toString());


            try {
                if (writeModeNFC) {
                    write(textToWriteNFC, myTag);
                }
            } catch (IOException | FormatException e) {
                e.printStackTrace();
                Toast.makeText(this, WRITE_ERROR, Toast.LENGTH_LONG).show();
            }
        }
    }

    public void ClosePopupWindow(View view) {

        progressBar.setVisibility(View.GONE);
        preferences = getPreferences(MODE_PRIVATE);
        preferences.edit().putString("proshow", "noshow").apply();
        mContainer.removeAllViews();
        windowContainer.setVisibility(View.GONE);
        mWebviewPop.destroy();

    }

    private void WriteModeOn() {
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void ExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(getResources().getString(R.string.exit_app_dialog))
                .setCancelable(false)
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
    }

}
