package activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import com.shaktipumps.shakti.shaktisalesemployee.R;

import webservice.WebURL;

public class FileActivity extends AppCompatActivity {
    ImageView my_image;
    //PhotoViewAttacher mAttacher;
    String file_type = null;
    WebView browser;
    private Toolbar mToolbar;
    private ProgressDialog progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_detail);
        //my_image = (ImageView) findViewById(R.id.my_image);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Bundle bundle = getIntent().getExtras();
        file_type = bundle.getString("call_portal");


        getSupportActionBar().setTitle(file_type);


        if (CustomUtility.isOnline(this)) {
            callWebPage();
        } else {
            Toast.makeText(this, "No Internet Connection found", Toast.LENGTH_SHORT).show();
        }


        progressBar = ProgressDialog.show(FileActivity.this, "", "Loading...");


    }


    public void callWebPage() {

        browser = (WebView) findViewById(R.id.webView1);
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setGeolocationEnabled(true);
        browser.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        browser.getSettings().setBuiltInZoomControls(true);
        browser.getSettings().setDomStorageEnabled(true);


        browser.setWebChromeClient(new WebChromeClient() {
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }


        });

//        // Force links and redirects to open in the WebView instead of in a browser
//        browser.setWebViewClient(new WebViewClient()
//
//                                 {
//                                     @Override
//                                     public void onReceivedError(WebView view, int errorCode,
//                                                                 String description, String failingUrl) {
//                                         // TODO Auto-generated method stub
//                                         super.onReceivedError(view, errorCode, description, failingUrl);
//                                         loadError();
//                                     }
//                                 }
//
//        );

//        // browser.loadUrl("https://www.google.co.in/");


        if (file_type.equalsIgnoreCase("Files & Folder")) {
            browser.loadUrl(WebURL.FILES_AND_FOLDER);
        }


        if (file_type.equalsIgnoreCase("Complaint attachment")) {
            browser.loadUrl(WebURL.COMPLAINT_ATTACHMENT);
        }


//
//        Log.d("url",""+file_type + "--"+ WebURL.DASHBOARD);

        if (file_type.equalsIgnoreCase("Reports")) {
            browser.loadUrl(WebURL.DASHBOARD);
        }


        browser.setWebViewClient(new WebViewClientDemo());
        browser.setVerticalScrollBarEnabled(true);
        browser.setHorizontalScrollBarEnabled(true);

        browser.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {


                //  Log.d("url",""+file_type + "--"+ WebURL.DASHBOARD);

                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

            }
        });


    }


    private void loadError() {
        String html = "<html><body><table width=\"100%\" height=\"100%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr>"
                + "<td><div align=\"center\"><font color=\"#8b8b8c\" size=\"4pt\">Your device don't have active internet connection</font></div></td>"
                + "</tr>" + "</table><html><body>";
        System.out.println("html " + html);

        String base64 = android.util.Base64.encodeToString(html.getBytes(),
                android.util.Base64.DEFAULT);
        browser.loadData(base64, "text/html; charset=utf-8", "base64");
        System.out.println("loaded html");
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        switch (id) {

            case android.R.id.home:
                onBackPressed();
                callWebPage();
                return true;

            case R.id.action_file_detail_menu:

                callWebPage();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file_detail, menu);
        return true;
    }

    private class WebViewClientDemo extends WebViewClient {


        @Override
        public void onPageFinished(WebView view, String url) {
//            // TODO Auto-generated method stub
//            super.onPageFinished(view, url);


            if (progressBar.isShowing()) {
                progressBar.dismiss();
            }

        }

        @Override

        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            // TODO Auto-generated method stub
            super.onReceivedError(view, errorCode, description, failingUrl);
            loadError();
        }

    }
}
