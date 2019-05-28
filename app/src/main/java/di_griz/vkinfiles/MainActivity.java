package di_griz.vkinfiles;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    public static int vkUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences vkSettings = getSharedPreferences("vkSettings", MODE_PRIVATE);
        vkUserId = vkSettings.getInt("userID", -1);
        if (vkUserId == -1)
            setVkUserId();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 13) {
            if (resultCode == RESULT_OK) {
                Log.d("Auth", "user ID: " + vkUserId);
            } else {
                showError("Auth error!", "Sory...",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
            }
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return (netInfo != null && netInfo.isConnected());
    }

    private void showError(String title, String text, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        Dialog dialog = builder.setTitle(title)
                .setMessage(text)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", listener)
                .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void setVkUserId() {
        if (isOnline())
            startActivityForResult(
                    new Intent(this, VkAuthActivity.class), 13);
        else
            showError("Connection error!",
                    "Please, check your internet connection and try again.",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setVkUserId();
                        }
                    });
    }

}
