package di_griz.vkinfiles;

import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;

public class DownloadThread extends Thread {
    static int counter = 0;
    private String url;
    private File file;

    DownloadThread(File downloadedFile, String url) {
        counter++;
        this.url = url;
        this.file = downloadedFile;
    }

    @Override
    public void run() {
        try {
            if (file.exists())
                Log.d("Download", "Файл " + file.getName() + " уже существует");
            else {
                FileUtils.copyURLToFile(new URL(url), file);
                Log.d("Download", "Complete " + file.getName());
            }
        } catch (Exception error) {
            Log.e("Download", String.format("File: %s (%s; %s)", file.getName(),
                    error.getMessage(), error.getClass().toString()));
            if (file.delete())
                Log.e("File", file.getName() + " was deleted");
        }
        counter--;
        MainActivity.onDownloadingFinish();
    }
}
