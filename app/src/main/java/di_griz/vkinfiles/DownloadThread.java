package di_griz.vkinfiles;

import android.content.Context;
import android.util.Log;

public class DownloadThread extends Thread {
    static int counter = 0;
    private String type, title, url;
    private  Context context;

    DownloadThread(Context context, String type, String title, String url) {
        counter++;
        this.context = context;
        this.type = type; this.title = title; this.url = url;
    }

    @Override
    public void run() {
        try {
            if (type.equals("link")) {
                Log.d("Files", "Playlist");
            } else {
                Log.d("Files", type);
            }
        } catch (Exception error) {
            Log.e("Download", error.getMessage() + ": " + error.getClass().getName());
        }
        counter--;
    }
}
