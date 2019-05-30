package di_griz.vkinfiles;

import android.content.Context;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;

public class DownloadThread extends Thread {
    static int counter = 0;
    private String url;
    private  Context context;
    private File file;

    DownloadThread(Context context, File downloadedFile, String url) {
        counter++;
        this.context = context;
        this.url = url;
        this.file = downloadedFile;
    }

    @Override
    public void run() {
        try {
            FileUtils.copyURLToFile(new URL(url), file);
            Log.d("Download", "Complete " + file.getName());
        } catch (Exception error) {
            Log.e("Download", error.getMessage() + "; " + error.getClass().toString());
        }
        counter--;
    }
}
