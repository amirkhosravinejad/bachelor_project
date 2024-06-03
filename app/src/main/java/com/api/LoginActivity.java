package com.api;

import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.api.database.TokenDatabaseHelper;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "bach-prj";
    private static final String AUTH_CALLBACK_URL = "http://127.0.0.1:5000/hass/auth_callback";
    private static final String AUTH_URL_FORMAT = "http://%s:8123/auth/authorize?client_id=http://127.0.0.1:5000&redirect_uri=%s";

    private String serverIP;
    private WebView webView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = findViewById(R.id.progressBar);

        initWebView();
        handleIntent(getIntent());
    }
    // this is when an intent is made from another activity to this one
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void initWebView() {
        webView = findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                progressBar.setVisibility(View.VISIBLE);
                Uri uri = request.getUrl();
                if (uri != null && uri.toString().startsWith(AUTH_CALLBACK_URL)) {
                    String code = uri.getQueryParameter("code");
                    authenticate_HA(code);
                    Log.d(TAG, "Auth code: " + code);
                    return true;
                }
                return false;
            }
        });


        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
    }

    private void authenticate_HA(String auth_code){
        HA_Authenticator authenticator = new HA_Authenticator();
        authenticator.authenticate(serverIP, auth_code,
            new HA_Authenticator.AuthenticationListener() {
                @Override
                public void onAuthenticationSuccess(String access_token, String refresh_token, String expiry_time) {
                    // Authentication successful, token received
                    Log.d("bach-prj", "Authentication successful. Access token: "
                            + access_token + " refresh token: " + refresh_token + " expire: " + expiry_time);
                    try (TokenDatabaseHelper databaseHelper =
                                 new TokenDatabaseHelper(LoginActivity.this.getApplicationContext())) {
                        ContentValues values = new ContentValues();
                        databaseHelper.setDbWriter(databaseHelper.getWritableDatabase());
                        databaseHelper.insertTokens(values, serverIP, access_token, refresh_token, expiry_time);
                    }
                    onSuccessfulToken(access_token);
                }
                @Override
                public void onAuthenticationFailure(String errorMessage) {
                    // Authentication failed
                    Log.e("bach-prj", "Authentication failed: " + errorMessage);
                }
            }
        );
    }

    private void onSuccessfulToken(String token) {
        // this is for working with the main view because
        // authenticator class is working in another thread
        // So we need a Handler run in main thread
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                Intent toLightControlActivity = new Intent(LoginActivity.this,
                        ServiceActivity.class);
                toLightControlActivity.putExtra("token", token);
                toLightControlActivity.putExtra("server_ip", serverIP);
                startActivity(toLightControlActivity);
            }
        });
    }

    // the function checks if the server_ip is found or it's null.
    private void handleIntent(Intent intent) {
        if (intent != null) {
            serverIP = intent.getStringExtra("server_ip");
            if (serverIP != null) {
                loadAuthorizeUrl();
            } else {
                Log.e(TAG, "Server IP is null");
                Toast.makeText(this, "Server IP is null", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadAuthorizeUrl() {
        String authorizeUrl = String.format(AUTH_URL_FORMAT, serverIP, AUTH_CALLBACK_URL);
        Log.d(TAG, "Loading authorize URL: " + authorizeUrl);
        webView.loadUrl(authorizeUrl);
    }
}