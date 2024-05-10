package com.api;

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

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "zaneto";
    private static final String AUTH_CALLBACK_URL = "http://127.0.0.1:5000/hass/auth_callback";
    private static final String AUTH_URL_FORMAT = "http://%s:8123/auth/authorize?client_id=http://127.0.0.1:5000&redirect_uri=%s";

    private String serverIP;
    private WebView webView;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
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
                    // Handle the authorization code here
                    return true;
                }
                return false;
            }
        });

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
    }

    private void authenticate_HA(String auth_code){
        HomeAssistantAuthenticator authenticator = new HomeAssistantAuthenticator();
        authenticator.authenticate(serverIP, auth_code,
                new HomeAssistantAuthenticator.AuthenticationListener() {
                    @Override
                    public void onAuthenticationSuccess(String token) {
                        // Authentication successful, token received
                        Log.d("zaneto", "Authentication successful. Token: " + token);
                        onSuccessfulToken(token);
                    }

                    @Override
                    public void onAuthenticationFailure(String errorMessage) {
                        // Authentication failed
                        Log.e("zaneto", "Authentication failed: " + errorMessage);
                    }
                });
    }

    private void onSuccessfulToken(String token) {
        // this is for working with the main view because
        // authenticator class is working in another thread
        // So we need a Handler run in main thread
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "salap");
                Intent toLightControlActivity = new Intent(MainActivity.this, LightControlActivity.class);
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