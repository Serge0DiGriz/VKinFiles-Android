package di_griz.vkinfiles;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;

import retrofit2.Call;

public class UpdatingDataBase extends AsyncTask<Object, Integer, Boolean> {
    private  VkService vk;
    private ProgressDialog progressDialog;

    @Override
    protected Boolean doInBackground(Object... args) {
        vk = (VkService)args[0];
        progressDialog = (ProgressDialog)args[1];

        int count = getCountMessages();
        Log.d("DataBase", "Count Messages: " + count);

        if (count == -1)
            return false;

        Cursor cursor = MainActivity.userDB.query(SQLiteHelper.TABLE_NAME,
                new String[]{SQLiteHelper.COLUMN_MESSAGE_ID},
                null, null, null, null, null);

        HashSet<Integer> idsInDataBase = new HashSet<>();
        int columnIndex = cursor.getColumnIndex(SQLiteHelper.COLUMN_MESSAGE_ID);
        while(cursor.moveToNext())
            idsInDataBase.add(cursor.getInt(columnIndex));
        cursor.close();

        for (Message message: getMessages(count)) {
            if (!idsInDataBase.contains(message.id))
                messageProcessing(message, message.id);
        }

        return true;
    }

    private int getCountMessages() {
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

    private LinkedList<Message> getMessages(int count) {
        LinkedList<Message> messages = new LinkedList<>();

        for (int i=0; (i+1)*200 < count+200; i++) {
            try {
                Call<GetHistoryResponse> call = vk.getHistory(MainActivity.vkUserId,
                        200, i*200,
                        MainActivity.token, MainActivity.versionAPI);
                Log.d("VK_API", call.request().url().toString().split("\\?")[0]);
                GetHistoryResponse response = call.execute().body();

                if (response != null)
                    messages.addAll(response.response.items);

            } catch (IOException error) {
                Log.e("VK_API", error.getMessage());
            }
        }

        return messages;
    }

    private void messageProcessing(Message message, int messageID) {
        attachmentsProcessing(message.attachments, messageID);
        if (message.fwd_messages != null) {
            for (Message fwd: message.fwd_messages)
                messageProcessing(fwd, messageID);
        }
    }

    private void attachmentsProcessing(Attachment[] attachments, int messageID) {
        if (attachments != null) {
            for (Attachment attachment: attachments) {
                String type = attachment.type;

                ContentValues values = new ContentValues();
                values.put(SQLiteHelper.COLUMN_MESSAGE_ID, messageID);
                values.put(SQLiteHelper.COLUMN_TYPE, type);

                String title = null, url = null;

                if (type.equals("photo")) {
                    Photo photo = attachment.photo;
                    url = ""; int size = 0;
                    for (PhotoSize photoSize: photo.sizes) {
                        if (photoSize.width*photoSize.height > size) {
                            size = photoSize.width*photoSize.height;
                            url = photoSize.url;
                        }
                    }
                    String[] partUrl = url.split("/");
                    title = partUrl[partUrl.length-1];
                } else
                    if (type.equals("audio")) {
                    Audio audio = attachment.audio;
                    title = titleCorrecting(audio.title) + ".mp3";
                    url = audio.url;
                } else
                    if (type.equals("doc")) {
                    Doc doc = attachment.doc;
                    String ext = doc.ext; title = doc.title;
                    title += (title.endsWith(ext) ? "" : "."+ext);
                    url = doc.url;
                } else
                    if (type.equals("link")) {
                        Link link = attachment.link;
                        url = link.url;
                        if (url.contains("audio_playlist"))
                            title = titleCorrecting(link.title);
                    }

                if (title != null) {
                    values.put(SQLiteHelper.COLUMN_TITLE, title);
                    values.put(SQLiteHelper.COLUMN_URL, url);
                    MainActivity.userDB.insert(SQLiteHelper.TABLE_NAME, null, values);
                }
            }
        }
    }

    private String titleCorrecting(String title) {
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


    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);

        MainActivity.docAdapter.changeCursor(MainActivity.userDB.rawQuery(
                "SELECT * FROM " + SQLiteHelper.TABLE_NAME + " WHERE " +
                        SQLiteHelper.COLUMN_TYPE + " IN ('doc')", null));
        MainActivity.audioAdapter.changeCursor(MainActivity.userDB.rawQuery(
                "SELECT * FROM " + SQLiteHelper.TABLE_NAME + " WHERE " +
                        SQLiteHelper.COLUMN_TYPE + " IN ('audio', 'link')", null));
        MainActivity.photoAdapter.changeCursor(MainActivity.userDB.rawQuery(
                "SELECT * FROM " + SQLiteHelper.TABLE_NAME + " WHERE " +
                        SQLiteHelper.COLUMN_TYPE + " IN ('photo')", null));

        progressDialog.dismiss();
    }
}
