package com.api;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HomeAssistantAuthenticator {

    public interface AuthenticationListener {
        void onAuthenticationSuccess(String token);
        void onAuthenticationFailure(String errorMessage);
    }

    public void authenticate(String ipAddress, String username, String password, final AuthenticationListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String redirect_uri = "redirect_uri=http%3A%2F%2F" + ipAddress;
                String AUTH_ENDPOINT = ":8123/auth/authorize?response_type=code&redirect_uri=http%3A%2F%2F" + ipAddress;
                try {
                    String authUrl = "http://" + ipAddress + AUTH_ENDPOINT;

                    // Build JSON request body with username and password
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("username", username);
                    jsonBody.put("password", password);

                    // Establish HTTP connection
                    URL url = new URL(authUrl);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; utf-8");
                    connection.setDoOutput(true);

                    // Write JSON request body to the connection
                    connection.getOutputStream().write(jsonBody.toString().getBytes());

                    // Read response from the connection
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();

                    // Check HTTP response code
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Extract token from response body
                        String responseBody = responseBuilder.toString();
                        String token = extractToken(responseBody);
                        if (token != null) {
                            listener.onAuthenticationSuccess(token);
                        } else {
                            listener.onAuthenticationFailure("Failed to extract token from response");
                        }
                    } else {
                        listener.onAuthenticationFailure("HTTP error: " + responseCode);
                    }

                    // Close connection
                    connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                    listener.onAuthenticationFailure("Network error: " + e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    listener.onAuthenticationFailure("Error: " + e.getMessage());
                }
            }
        }).start();
    }

    private String extractToken(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            if (jsonResponse.has("access_token")) {
                return jsonResponse.getString("access_token");
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

