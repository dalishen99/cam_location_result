package activity;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.shaktipumps.shakti.shaktisalesemployee.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class EmployeePortalFragment extends Fragment {

    Context context;

    public EmployeePortalFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this.getActivity();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_employee_portal, container, false);
        View rootView = inflater.inflate(R.layout.fragment_employee_portal, container, false);


        ConnectivityManager cn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo nf = cn.getActiveNetworkInfo();

        if (nf != null && nf.isConnected() == true) {


            WebView browser = (WebView) rootView.findViewById(R.id.webView1);
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

            // Force links and redirects to open in the WebView instead of in a browser
            browser.setWebViewClient(new WebViewClient());

            // browser.loadUrl("https://www.google.co.in/");
            browser.loadUrl("https://spprdsrvr1.shaktipumps.com:8423/sap(bD1lbiZjPTkwMA==)/bc/bsp/sap/zemp_hr_portal/index.htm");


        } else {
            Toast.makeText(context, "No internet connection found", Toast.LENGTH_LONG).show();

        }


        return rootView;
    }


}
