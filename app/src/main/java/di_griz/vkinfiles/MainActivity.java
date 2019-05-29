package di_griz.vkinfiles;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends AppCompatActivity {

    public static int vkUserId;
    SharedPreferences vkSettings;
    private ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vkSettings = getSharedPreferences("vkSettings", MODE_PRIVATE);
        vkUserId = vkSettings.getInt("userID", -1);

        Log.d("Auth", "User ID: " + vkUserId);
        if (vkUserId == -1) setVkUserId();
        else inLogin();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    private void showConnectionError(DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Dialog dialog = builder.setTitle("Connection error!")
                .setMessage("Please, check your internet connection and try again.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", listener)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void setVkUserId() {
        if (isOnline()) {
            String authURL = "https://oauth.vk.com/authorize?" +
                    "client_id=" + getResources().getString(R.string.APP_ID) + "&" +
                    "display=mobile&" +
                    "redirect_uri=https://oauth.vk.com/blank.html&" +
                    "response_type=token&" +
                    "v=" + getResources().getString(R.string.API_V);

            WebView vkAuth = new WebView(this);
            vkAuth.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.startsWith("https://oauth.vk.com/blank.html")) {
                        vkUserId = Integer.parseInt(
                                url.split("user_id=")[1].split("&")[0]);
                        vkSettings.edit().putInt("userID", vkUserId).apply();
                        inLogin();
                    }
                    return false;
                }
            });

            setContentView(vkAuth);
            vkAuth.loadUrl(authURL);
        } else
            showConnectionError(new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    setVkUserId();
                }
            });
    }

    private void inLogin() {
        setContentView(R.layout.activity_main);

        pager = findViewById(R.id.pager);
        pager.setAdapter(new MyAdapter(getSupportFragmentManager()));
        pager.setCurrentItem(1);

        Log.d("Auth", "User ID: " + vkUserId);
    }

}
