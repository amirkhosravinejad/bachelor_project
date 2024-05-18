package com.api;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class HomeAssistantAuthenticator {

    private String access_token;
    private String refresh_token;
    private final String client_id = "http://127.0.0.1:5000";

    public interface AuthenticationListener {
        void onAuthenticationSuccess(String access_token, String refresh_token, String expiry_time);
        void onAuthenticationFailure(String errorMessage);
    }

    public interface RefreshTokenListener {

    }

    public void authenticate(String ipAddress, String auth_code, final AuthenticationListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String Base_URL = "http://" + ipAddress + ":8123/";
                try {

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
                        Date newDate = new Date();
                        long exp = newDate.getTime() + 1800 * 1000;
                        newDate.setTime(exp);
                        access_token = token_response.optString("access_token");
                        refresh_token = token_response.optString("refresh_token");
                        listener.onAuthenticationSuccess(access_token, refresh_token, newDate.toString());
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

    public String[] getRefreshToken(String ipAddress, String ref_token) {
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
        String [] returnString = new String[3];
             try {
                 refresh_token = ref_token;
                 String token_endpoint = String.format("http://%s:8123/auth/token", ipAddress);

                 Map<String, String> data = new HashMap<>();
                 data.put("grant_type", "refresh_token");
                 data.put("refresh_token", refresh_token);
                 data.put("client_id", client_id);
                 Log.d("bach-prj", "data: " + data + " endpoint: " + token_endpoint);

                 // Making the POST request
                 GetTokenTask task = new GetTokenTask();
                 task.data = data;
                 task.token_endpoint = token_endpoint;
                 JSONObject token_response = task.execute().get();
                 // Checking the response status
                 if (token_response != null) {
                     String [] badRequest = new String[3];
                     if (token_response.optString("resCode").equals("400"))
                         return badRequest;
                     Date newDate = new Date();
                     long exp = newDate.getTime() + 1800 * 1000;
                     newDate.setTime(exp);
                     access_token = token_response.optString("access_token");
                     refresh_token = access_token;
                     Log.d("bach-prj", "new refresh token granted: " + refresh_token);
                     returnString[0] = access_token;
                     returnString[1] = refresh_token;
                     returnString[2] = newDate.toString();
                 }
                 else {
                     Log.d("bach-prj", "failed to obtain new refresh tokens");
                 }

             }
             catch (Exception e) {
                 Log.e("bach-prj", "Error occurred: " + e);
             }
//            }
//        });
        return returnString;
    }

    private class GetTokenTask extends AsyncTask<Void, Void, JSONObject> {
        private String token_endpoint;
        private Map<String, String> data;
        @Override
        protected JSONObject doInBackground(Void... voids) {
            try {
                // Making the POST request
                JSONObject token_response = makePostRequest(token_endpoint, data);
                Log.d("bach-prj", "making post request in get refresh token was successful.");
                return token_response;
            } catch (Exception e) {
                Log.e("bach-prj", e.getMessage());
                return null;
            }

        }
        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
//            isConnectedToFormerIP = result;
        }

    }

    private JSONObject makePostRequest(String urlString, Map<String, String> data) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            Log.d("bach-prj", "before send data in makePostRequest function");
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
            StringBuilder responseBuilder = new StringBuilder();
            byte[] postDataBytes = new byte[0];

            postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);

            connection.getOutputStream().write(postDataBytes);
            if (connection.getResponseCode() == 400){
                JSONObject badRequestJSON = new JSONObject();
                badRequestJSON.put("resCode", "400");
                return  badRequestJSON;
            }
            // Read response from the connection
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
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

}
