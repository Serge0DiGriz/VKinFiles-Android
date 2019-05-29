package di_griz.vkinfiles;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedList;

import retrofit2.Call;

public class UpdatingDataBase extends AsyncTask<VkService, Integer, Boolean> {
    @Override
    protected Boolean doInBackground(VkService... args) {
        VkService vk = args[0];

        int count = getCountMessages(vk);
        Log.d("DataBase", "Count Messages: " + count);

        if (count == -1)
            return false;

        return true;
    }

    private int getCountMessages(VkService vk) {
        int count = -1;

        try {
            Call<GetHistoryResponse> call = vk.getHistory(MainActivity.vkUserId, 0, 0,
                    MainActivity.token, MainActivity.versionAPI);
            Log.d("VK_API", call.request().url().toString().split("\\?")[0]);
            GetHistoryResponse response = call.execute().body();

            if (response != null)
                count = response.response.count;

        } catch (IOException error) {
            Log.e("VK_API", error.getMessage());
        }

        return count;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
    }
}
