package di_griz.vkinfiles;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    static int vkUserId;
    static String token, versionAPI;
    static SQLiteDatabase userDB;
    static LinkedList<Long> selectedItems = new LinkedList<>();

    SharedPreferences vkSettings;
    ViewPager pager;
    VkService vk;

    static ItemCursorAdapter photoAdapter;
    static ItemCursorAdapter audioAdapter;
    static ItemCursorAdapter docAdapter;
    static ListAdapter emptyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = getResources();
        versionAPI = res.getString(R.string.API_V);
        token = res.getString(R.string.TOKEN);

        vkSettings = getSharedPreferences("vkSettings", MODE_PRIVATE);
        vkUserId = vkSettings.getInt("userID", -1);

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.vk.com")
                .addConverterFactory(GsonConverterFactory.create()).build();
        vk = retrofit.create(VkService.class);

        if (vkUserId == -1) setVkUserId();
        else inLogin();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.downloading_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (vkUserId != -1) {
            int id = item.getItemId();
            Cursor cursor = null;

            switch (id) {
                case R.id.downloadAll:
                    cursor = userDB.query(SQLiteHelper.TABLE_NAME, null, null,
                            null, null, null, null);
                    break;
                case R.id.downloadType:
                    String typeSelector;
                    switch (pager.getCurrentItem()) {
                        case 0:
                            typeSelector =
                                    " WHERE " + SQLiteHelper.COLUMN_TYPE + " IN ('photo')";
                            break;
                        case 1:
                            typeSelector =
                                    " WHERE " + SQLiteHelper.COLUMN_TYPE + " IN ('audio', 'link')";
                            break;
                        case 2:
                            typeSelector =
                                    " WHERE " + SQLiteHelper.COLUMN_TYPE + " IN ('doc')";
                            break;
                        default:
                            typeSelector = "";
                    }
                    cursor = userDB.rawQuery("SELECT * FROM " + SQLiteHelper.TABLE_NAME +
                            typeSelector, null);
                    break;
                case R.id.downloadSelected:
                    StringBuilder selector;
                    if (selectedItems.size() == 0)
                        selector = null;
                    else {
                        selector = new StringBuilder(" WHERE " + SQLiteHelper.COLUMN_ID +
                                " IN (");
                        for (long selectId : selectedItems)
                            selector.append(String.format("'%s',", selectId));
                        selector.deleteCharAt(selector.length() - 1);
                        selector.append(")");
                    }
                    cursor = userDB.rawQuery("SELECT * FROM " + SQLiteHelper.TABLE_NAME +
                            (selector != null ? selector.toString() : ""), null);
                    break;
            }

            if (cursor != null) {
                Log.d("Download", "Count: " + cursor.getCount());
                if (isExternalStorageWritable()) {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), "VK in Files");
                    new Thread(new Downloading(cursor, dir)).start();
                } else
                    Toast.makeText(this, "Для выполнения этой операции необходимо " +
                            "отключить устройство от компьютера", Toast.LENGTH_LONG).show();

            }
        }

        return super.onOptionsItemSelected(item);
    }



    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
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

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String message = "Благодарим Вас за установку приложения.\n" +
                                            "В приложении будут отображаться вложения, отправленные в беседу сообщества, " +
                                            "и именно их Вы сможете скачать на своё устройство.\n" +
                                            "Также рекомендуем отключить уведомления о новых сообщениях.";
                                    vk.sendMessage(vkUserId, message, 0, token, versionAPI).execute();
                                } catch (IOException error) {
                                    Log.e("VK_API", error.getMessage());
                                }
                            }
                        }).start();

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

        photoAdapter = new ItemCursorAdapter(this, userDB.rawQuery(
                "SELECT * FROM " + SQLiteHelper.TABLE_NAME +
                        " WHERE " + SQLiteHelper.COLUMN_TYPE + " IN ('photo')",
                null), 0);
        audioAdapter = new ItemCursorAdapter(this, userDB.rawQuery(
                "SELECT * FROM " + SQLiteHelper.TABLE_NAME +
                        " WHERE " + SQLiteHelper.COLUMN_TYPE + " IN ('audio', 'link')",
                null), 0);
        docAdapter = new ItemCursorAdapter(this, userDB.rawQuery(
                "SELECT * FROM " + SQLiteHelper.TABLE_NAME +
                        " WHERE " + SQLiteHelper.COLUMN_TYPE + " IN ('doc')",
                null), 0);

        pager = findViewById(R.id.pager);
        pager.setAdapter(new PageAdapter(getSupportFragmentManager()));
        pager.setCurrentItem(1);

        Log.d("Auth", "User ID: " + vkUserId);

        ProgressDialog progressDialog = ProgressDialog.show(this, "Loading...",
                "Идёт обновление базы данных, пожалуйста, подождите");

        new UpdatingDataBase().execute(vk, progressDialog);
    }



    class Downloading implements Runnable {
        Cursor cursor;
        File downloadingDir;

        Downloading(Cursor cursor, File downloadingDir) {
            this.cursor = cursor;
            if (cursor.getCount() != 0)
                this.downloadingDir = downloadingDir;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Скачивание началось...", Toast.LENGTH_LONG).show();
                    }
                });
        }

        @Override
        public void run() {
            while (cursor.moveToNext()) {
                String type = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_TYPE)),
                        title = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_TITLE)),
                        url = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_URL));
                DownloadThread thread;

                if (type.equals("link")) {
                    Log.d("Download", "Playlist");
                } else {
                    File dir = new File(downloadingDir, type);
                    if (dir.mkdirs())
                        Log.d("Download", "MakeDir " + dir.getPath());

                    thread = new DownloadThread(getBaseContext(),
                            new File(dir, title), url);
                    thread.start();

                    if (DownloadThread.counter > 13)
                        try {
                            thread.join();
                        } catch (InterruptedException error) {
                            Log.e("Files", error.getMessage());
                        }
                }
            }
            cursor.close();
        }
    }

}
