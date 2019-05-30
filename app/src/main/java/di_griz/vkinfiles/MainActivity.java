package di_griz.vkinfiles;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import retrofit2.Response;
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
                if (permissionGranted) {
                    if (cursor.getCount() != 0) {
                        File dir = new File(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS), "VK in Files");

                        progressDialog = ProgressDialog.show(this, "Downloading...",
                                "Ваши файлы скачиваются, подождите");

                        new Thread(new Downloading(cursor, dir)).start();
                    }
                } else {
                    checkPermissions();
                    Toast.makeText(this, "Нет прав на запись файлов!",
                            Toast.LENGTH_SHORT).show();
                }

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
                                            "и именно их Вы сможете скачать на своё устройство. " +
                                            "Скачанные файлы Вы найдёте в папке «Download/VK in Files»" +
                                            "Также рекомендуем отключить уведомления о новых сообщениях.";
                                    vk.sendMessage(vkUserId, message, 1, token, versionAPI).execute();
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

        new UpdatingDataBase().execute(vk, ProgressDialog.show(this, "Loading...",
                "Идёт обновление базы данных, пожалуйста, подождите"));
    }



    private static ProgressDialog progressDialog = null;

    class Downloading implements Runnable {
        Cursor cursor;
        File downloadingDir;

        Downloading(Cursor cursor, File downloadingDir) {
            this.cursor = cursor;
            this.downloadingDir = downloadingDir;
        }

        @Override
        public void run() {
            DownloadThread.counter++;
            while (cursor.moveToNext()) {
                String type = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_TYPE)),
                        title = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_TITLE)),
                        url = cursor.getString(cursor.getColumnIndex(SQLiteHelper.COLUMN_URL));
                DownloadThread thread;

                if (type.equals("link")) {
                    try {
                        Document html = Jsoup.connect(url).get();
                        StringBuilder audioIds = new StringBuilder();
                        StringBuilder messageIds = new StringBuilder();
                        int counter = 0;
                        for (Element item: html.select("div.audio_item")) {
                            audioIds.append(item.id().substring(0, item.id().indexOf("_playlist")));
                            audioIds.append(",");
                            if (++counter == 10) {
                                audioIds.deleteCharAt(audioIds.length()-1);
                                SendResponse response = vk.sendAttachments(
                                        vkUserId, audioIds.toString(), 0,
                                        token, versionAPI).execute().body();
                                if (response != null) {
                                    messageIds.append(response.response);
                                    messageIds.append(",");
                                }

                                counter = 0; audioIds.delete(0, audioIds.length());
                            }
                        }
                        if (counter != 0) {
                            audioIds.deleteCharAt(audioIds.length()-1);
                            SendResponse response = vk.sendAttachments(
                                    vkUserId, audioIds.toString(), 0,
                                    token, versionAPI).execute().body();
                            if (response != null) {
                                messageIds.append(response.response);
                                messageIds.append(",");
                            }
                        }
                        messageIds.deleteCharAt(messageIds.length()-1);

                        GetByIdResponse response = vk.getById(messageIds.toString(),
                                token, versionAPI).execute().body();
                        LinkedList<Attachment> attachments = new LinkedList<>();
                        if (response != null)
                            for (Message message: response.response.items)
                                attachments.addAll(Arrays.asList(message.attachments));

                        vk.delete(messageIds.toString(), 1, token, versionAPI).execute();

                        for (Attachment attachment: attachments) {
                            Audio audio = attachment.audio;
                            File dir = new File(downloadingDir, "audio/"+title);
                            if (dir.mkdirs())
                                Log.d("Download", "MakeDir " + dir.getPath());

                            thread = new DownloadThread(new File(dir,
                                    MainActivity.titleCorrecting(audio.title)+".mp3"), audio.url);
                            thread.start();

                            if (DownloadThread.counter > 7)
                                try {
                                    thread.join();
                                } catch (InterruptedException error) {
                                    Log.e("Files", error.getMessage());
                                }
                        }
                    } catch (Exception error) {
                        Log.e("Download", error.getMessage());
                    }
                } else {
                    File dir = new File(downloadingDir, type);
                    if (dir.mkdirs())
                        Log.d("Files", "MakeDir " + dir.getPath());

                    thread = new DownloadThread(new File(dir, title), url);
                    thread.start();

                    if (DownloadThread.counter > 7)
                        try {
                            thread.join();
                        } catch (InterruptedException error) {
                            Log.e("Files", error.getMessage());
                        }
                }
            }
            DownloadThread.counter--;
            onDownloadingFinish();
            cursor.close();
        }
    }

    static void onDownloadingFinish() {
        if (DownloadThread.counter == 0 && MainActivity.progressDialog != null)
            progressDialog.dismiss();
    }

    static String titleCorrecting(String title) {
        StringBuilder correct = new StringBuilder();
        for (char symbol: title.trim().toCharArray()) {
            switch (symbol) {
                case '/': correct.append('¦'); break;
                case '\\': correct.append('¦'); break;
                case '|': correct.append('¦'); break;
                case ':': correct.append('¦'); break;
                case '<': correct.append('«'); break;
                case '>': correct.append('»'); break;
                case '"': correct.append('\''); break;
                case '*': correct.append('×'); break;
                case '?': correct.append('‽'); break;
                case '!': correct.append('‽'); break;
                case '%': correct.append('‰'); break;
                case '@': correct.append('©'); break;
                case '+': correct.append('±'); break;

                default: correct.append(symbol);
            }
        }

        return correct.toString();
    }



    private static final int REQUEST_PERMISSION_WRITE = 1001;
    private boolean permissionGranted;
    // проверяем, доступно ли внешнее хранилище для чтения и записи
    public boolean isExternalStorageWritable(){
        String state = Environment.getExternalStorageState();
        return  Environment.MEDIA_MOUNTED.equals(state);
    }
    private void checkPermissions(){

        if(!isExternalStorageWritable()){
            Toast.makeText(this, "Внешнее хранилище не доступно", Toast.LENGTH_LONG).show();
        }
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_WRITE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults){
        if (requestCode == REQUEST_PERMISSION_WRITE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                permissionGranted = true;
        }
    }

}
