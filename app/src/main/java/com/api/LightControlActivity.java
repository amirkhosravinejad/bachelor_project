package com.api;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LightControlActivity extends AppCompatActivity {

    private String server_ip, token;
    private static final String TAG = "zaneto";
    private Switch lightSwitch;
    private TextView lightStatusLabel;
    private String lightState = "off";
    private Button SunsetButton, SunriseButton;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_control);
        handleIntent(getIntent());
//        android.net.wifi.WifiManager wifi =
//                (android.net.wifi.WifiManager)
//                        getSystemService(android.content.Context.WIFI_SERVICE);
//        WifiManager.MulticastLock lock = wifi.createMulticastLock("Multicast-lock");
//        lock.setReferenceCounted(true);
//        lock.acquire();

        // Initialize UI elements
        lightSwitch = findViewById(R.id.lightSwitch);
        lightStatusLabel = findViewById(R.id.lightStatusLabel);
        SunsetButton = findViewById(R.id.Sunset_Button);
        SunriseButton = findViewById(R.id.Sunrise_Button);

        // Set listener for the Switch
        lightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Send request to turn on light
                // Update UI accordingly
                lightStatusLabel.setText("Light is ON");
                lightState = "on";
            } else {
                // Send request to turn off light
                // Update UI accordingly
                lightStatusLabel.setText("Light is OFF");
                lightState = "off";
            }
            sendRequestLightState(lightState);
        });

        SunsetButton.setOnClickListener(view -> createAutomation("sunset"));
        SunriseButton.setOnClickListener(view -> createAutomation("sunrise"));

//        UdpBroadcastClient udp_client = new UdpBroadcastClient();
//        udp_client.discoverHomeAssistantServer();

        // Initialize NetworkDiscovery
//        networkDiscovery = new NetworkDiscovery();
//        // Start service discovery
//        networkDiscovery.discoverServer();

    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            server_ip = intent.getStringExtra("server_ip");
            token = intent.getStringExtra("token");
            if (server_ip != null && token != null) {
                Log.d(TAG, "ServerIP: " + server_ip + " token: " + token);
            } else {
                Log.e(TAG, "Server IP is null");
                Toast.makeText(this, "Server IP is null", Toast.LENGTH_LONG).show();
            }
        }
    }

//    @Nullable

    private void createAutomation(String event) {
        String automationUrl = server_ip + "/api/test/automation/";
        JSONObject requestData = new JSONObject();
        JsonObjectRequest jor = new JsonObjectRequest(
                Request.Method.POST,
                automationUrl,
                requestData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // Handle successful response
                        Log.d(TAG, "Response: " + response.toString());
                        String success_message = event + "automation created successfully";
                        Toast.makeText(LightControlActivity.this, success_message, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error response
                        Log.e(TAG, "Error: " + error.toString());
                        String failed_message = "Failed to create " + event + " automation";
                        Toast.makeText(LightControlActivity.this, failed_message, Toast.LENGTH_SHORT).show();
                    }
                }
        );
        Volley.newRequestQueue(this).add(jor);
    }

    private void sendRequestLightState(String state) {
        try {
            String lightControlUrl = "http://" + server_ip + ":8123/api/services/light/turn_" + state;
            JSONObject requestData = new JSONObject();
            requestData.put("entity_id", "light.virtual_light_1");

            // Update endpoint URL
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    lightControlUrl,
                    requestData,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // Handle successful response
                            Log.d(TAG, "Response: " + response.toString());
                            String success_message = "Light turned " + lightState + " successfully";
                            Toast.makeText(LightControlActivity.this, success_message, Toast.LENGTH_SHORT).show();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // Handle error response
                            String errorCause = Objects.requireNonNull(error.getCause()).toString();
                            if (!checkIfJSONArrayException(errorCause)) {
                                Log.e(TAG, "Error: " + error.toString());
                                String failed_message = "Failed to turn " + lightState + " the light";
                                Toast.makeText(LightControlActivity.this, failed_message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
            ){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + token);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            // Add the request to the request queue
            Volley.newRequestQueue(this).add(jsonObjectRequest);

        } catch (JSONException e) {
            Log.d("zaneto", "salap");
        }
    }

    private Boolean checkIfJSONArrayException(String errorCause) {
        if (errorCause.contains("type org.json.JSONArray cannot be converted to JSONObject")){
            String success_message = "Light turned " + lightState + " successfully";
            Toast.makeText(LightControlActivity.this, success_message, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

}
