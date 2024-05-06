package com.api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HomeAssistantAuthenticator {

    private String access_token;
    private String refresh_token;

    public interface AuthenticationListener {
        void onAuthenticationSuccess(String token);
        void onAuthenticationFailure(String errorMessage);
    }

    public void authenticate(String ipAddress, String username, String password, final AuthenticationListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String Base_URL = "http://" + ipAddress + ":8123/";
                try {
                    String authorize_url = Base_URL + "auth/authorize";
                    String client_id = "http://127.0.0.1:5000";
                    String redirect_uri = "http://127.0.0.1:5000/hass/auth_callback";
                    String state = "http://hassio.local:8123";

                    // Redirect user to the authorize URL
                    String redirect_to_authorize = authorize_url + "?client_id=" + client_id + "&redirect_uri=" + redirect_uri;
                    Log.d("zaneto", "Redirect to authorize: " + redirect_to_authorize);
                    // Here, you may redirect the user to the URL or handle it in your Android UI

                    // For demonstration purposes, I'll skip the HTTP request and just assume
                    // that the user successfully authenticated and was redirected back to the
                    // redirect_uri with an authorization code in the query parameters.
                    String auth_code = "12345"; // This would be extracted from the redirect URI in reality

                    // Now, we'll exchange the authorization code for tokens
                    String token_endpoint = Base_URL + "auth/token";
                    Map<String, String> data = new HashMap<>();
                    data.put("grant_type", "authorization_code");
                    data.put("code", auth_code);
                    data.put("client_id", client_id);

                    // Making the POST request
                    JSONObject token_response = makePostRequest(token_endpoint, data);

                    // Checking the response status
                    if (token_response != null) {
                        // Access token and refresh token received successfully
                        access_token = token_response.optString("access_token");
                        refresh_token = token_response.optString("refresh_token");
                        listener.onAuthenticationSuccess(access_token);
                    } else {
                        // Error occurred
                        listener.onAuthenticationFailure("Failed to obtain tokens");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onAuthenticationFailure("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    private JSONObject makePostRequest(String urlString, Map<String, String> data) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Write JSON data to the connection output stream
            JSONObject jsonData = new JSONObject(data);
            connection.getOutputStream().write(jsonData.toString().getBytes());

            // Read response from the connection
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();

            // Convert response to JSON object
            return new JSONObject(responseBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAccess_token() {
        return access_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }
}
