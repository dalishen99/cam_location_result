package com.hccs.app.studentIn;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.hccs.app.studentIn.util.Constants;
import com.hccs.app.studentIn.util.Utils;

/**
 * A placeholder fragment containing a simple view.
 */
public  class PlaceholderFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static int mNumber;
    private static final String TAG = PlaceholderFragment.class.getName();

    private final String mBaseUrl= "https://myeagle.hccs.edu";
    private String mUrl;
    public static WebView mWebView;
    private  ProgressBar progressBar;
    private LinearLayout noInternetLayout;


    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PlaceholderFragment newInstance(int sectionNumber) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public PlaceholderFragment() {
        
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mWebView.destroy();
    }

   // @SuppressWarnings("SetJavaScriptEnabled")
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int number = getArguments().getInt(ARG_SECTION_NUMBER);
        mNumber=number;
        switch (number){
            case 1:
                //Dashboard
                mUrl= Constants.DASHBOARD;
                break;
            case 2:
                //Financials
                mUrl =Constants.FINANCIAL_OWE;
                break;
            case 3:
                //Schedule
                mUrl= Constants.SCHEDULE;
                break;
            case 4:
                //academics
                mUrl = Constants.ACADEMICS_TRANSCRIPT;
                break;
            case 5:
                //Enrollment
                mUrl= Constants.ENROLLMENT_SHOPPING_CART;
                break;
            case 6:
                //My Information
                mUrl =Constants.MY_INFO_ADDRESS;
                break;
            case 7:
                //Widgets
                mUrl= Constants.WIDGET;
                break;
            case 8:
                //EGLS3
                mUrl = Constants.EGLS3;
                break;
            case 9:
                //About is taken care of by the Dashboard class
                break;
            case -1:
                //Logout
                mUrl=Constants.LOGOUT;
                break;
            case 11:
                //course catalog
                mUrl = Constants.CATALOG;
                break;
            case 12:
                //class search
                mUrl = Constants.CLASS_SEARCH;
                break;
            case 13:
                //campus map
                mUrl = Constants.MAP;
                break;
            case 14:
                //news
                mUrl = Constants.NEWS;
                break;
            case 15:
                //library
                mUrl = Constants.LIBRARY;
                break;
            case 16:
                //calendar
                mUrl = Constants.CALENDAR;
                break;
            default:
                mUrl=Constants.DASHBOARD;
        }

        View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        progressBar =(ProgressBar)rootView.findViewById(R.id.progress);
        progressBar.setMax(100);
        noInternetLayout = (LinearLayout)rootView.findViewById(R.id.noInternetLayout);
        Button tryAgain = (Button)rootView.findViewById(R.id.try_again_button);
        tryAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAndLoad(mWebView,mUrl);
            }
        });
        //progressBar.getProgressDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

        mWebView = (WebView)rootView.findViewById(R.id.webView);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new WebViewClient() {

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //if network active and connected
                checkAndLoad(view,url);
                return true; // super.shouldOverrideUrlLoading(view, url);
            }

            public void onPageFinished(WebView view, String url) {
                mWebView.loadUrl("javascript:(function() { "
                        + "document.getElementById('open-sidebar').style.visibility='hidden';"
                        + "})()");

                //If schedule view, leave the setting menu
                if(mNumber != 3){
                    mWebView.loadUrl("javascript:(function() { "
                            + "document.getElementById('setting').style.visibility='hidden';"
                            + "})()");
                }

                mWebView.loadUrl("javascript: var x=document.getElementsByTagName('footer')[0].innerHTML='<span>hello</span>';");


                SharedPreferences mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                //Log.d(TAG,"Shared preference is  :" + mSharedPrefs);
                if(mSharedPrefs.getBoolean(SettingsActivity.PREF_AUTOFILL,false)){
                    if(url.equals("https://psmobile.hccs.edu/index.php/app/profile/loginform")){
                        String user=mSharedPrefs.getString(SettingsActivity.PREF_USERNAME,"");
                        String pwd =mSharedPrefs.getString(SettingsActivity.PREF_PWD,"");
                        mWebView.loadUrl("javascript: var x=document.getElementsByName('username')[0].value ='"+user+"';");
                        mWebView.loadUrl("javascript: var x=document.getElementsByName('password')[0].value ='"+pwd+"';");
                    }
                }
            }

            public void onPageStarted(WebView view, String url, Bitmap favicon){
                mWebView.loadUrl("javascript:(function() { "
                        + "document.getElementById('open-sidebar').style.visibility='hidden';"
                        + "})()");
            }
        });


        mWebView.setWebChromeClient(new WebChromeClient(){
            public void onProgressChanged(WebView webView, int progress){
                if(progress ==100){
                    mWebView.setVisibility(View.VISIBLE);
                }else{
                    mWebView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(progress);
                }
            }

            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        checkAndLoad(mWebView,mUrl);

        return rootView;
    }

    private void checkAndLoad(WebView webView, String url){
        //if the url is phone, or email, don't load it
        if(Utils.check(url,getActivity()))
            return;
        //if network active and connected
        if(isNetworkAvailable()){
            webView.loadUrl(url);
            noInternetLayout.setVisibility(View.GONE);
        }else{
            mWebView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            noInternetLayout.setVisibility(View.VISIBLE);
        }
    }

    private boolean isNetworkAvailable(){
        ConnectivityManager connectivityManager = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
}
