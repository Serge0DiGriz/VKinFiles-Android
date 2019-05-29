package di_griz.vkinfiles;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    static int vkUserId;
    static String token, versionAPI;
    SQLiteDatabase userDB;

    SharedPreferences vkSettings;

    static ItemCursorAdapter photoAdapter;
    static ItemCursorAdapter audioAdapter;
    static ItemCursorAdapter docAdapter;
    static ListAdapter emptyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vkSettings = getSharedPreferences("vkSettings", MODE_PRIVATE);
        vkUserId = vkSettings.getInt("userID", -1);

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
                .setMessage("Нет соединения, проверьте подключение к интернету и попробуйте снова.")
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

        SQLiteHelper helper = new SQLiteHelper(this);
        userDB = helper.getWritableDatabase();

        photoAdapter = new ItemCursorAdapter(this, userDB.query(SQLiteHelper.TABLE_NAME,
                null, SQLiteHelper.COLUMN_TYPE + " = ?", new String[]{"photo"},
                null, null, null), 0);
        audioAdapter = new ItemCursorAdapter(this, userDB.query(SQLiteHelper.TABLE_NAME,
                null, "type = ? or type = ?", new String[]{"audio", "link"},
                null, null, null), 0);
        docAdapter = new ItemCursorAdapter(this, userDB.query(SQLiteHelper.TABLE_NAME,
                null, SQLiteHelper.COLUMN_TYPE + " = ?", new String[]{"doc"},
                null, null, null), 0);

        ViewPager pager = findViewById(R.id.pager);
        pager.setAdapter(new PageAdapter(getSupportFragmentManager()));
        pager.setCurrentItem(1);

        Log.d("Auth", "User ID: " + vkUserId);

        Resources res = getResources();
        versionAPI = res.getString(R.string.API_V);
        token = res.getString(R.string.TOKEN);

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.vk.com")
                .addConverterFactory(GsonConverterFactory.create()).build();
        VkService vk = retrofit.create(VkService.class);

        ProgressDialog progressDialog = ProgressDialog.show(this, "Loading...",
                "Идёт обновление базы данных, пожалуйста, подождите");

        new UpdatingDataBase().execute(vk, userDB, progressDialog);
    }

}
