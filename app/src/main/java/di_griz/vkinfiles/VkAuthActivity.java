package di_griz.vkinfiles;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class VkAuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vk_auth);

        String authURL = "https://oauth.vk.com/authorize?" +
                "client_id=" + getResources().getString(R.string.APP_ID) + "&" +
                "display=mobile&" +
                "redirect_uri=https://oauth.vk.com/blank.html&" +
                "response_type=token&" +
                "v=" + getResources().getString(R.string.API_V);

        WebView vkAuth = findViewById(R.id.vkAuth);
        vkAuth.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("https://oauth.vk.com/blank.html")) {
                    MainActivity.vkUserId = Integer.parseInt(
                            url.split("user_id=")[1].split("&")[0]);
                    setResult(RESULT_OK);
                    finish();
                }
                return false;
            }
        });
        vkAuth.loadUrl(authURL);
    }
}
