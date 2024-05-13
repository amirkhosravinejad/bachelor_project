package com.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class HomeAssistantAuthenticator {

    private String access_token;
    private String refresh_token;

    public interface AuthenticationListener {
        void onAuthenticationSuccess(String access_token, String refresh_token, long expiry_time);
        void onAuthenticationFailure(String errorMessage);
    }

    public void authenticate(String ipAddress, String auth_code, final AuthenticationListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String Base_URL = "http://" + ipAddress + ":8123/";
                try {
                    String client_id = "http://127.0.0.1:5000";

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
                        long expiry_time = (System.currentTimeMillis() + 1800);
                        access_token = token_response.optString("access_token");
                        refresh_token = token_response.optString("refresh_token");
                        listener.onAuthenticationSuccess(access_token, refresh_token, expiry_time);
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
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);

            // Write data to the connection output stream
            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (postData.length() > 0) {
                    postData.append("&");
                }
                postData.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                postData.append("=");
                postData.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes("UTF-8");
            connection.getOutputStream().write(postDataBytes);

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
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getAccess_token() {
        return access_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }
}
