package com.api;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
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

public class LightControlActivity extends AppCompatActivity {

    // Update base URL to point to your Flask server
    private String BASE_URL = "http://192.168.36.239:5000";

    private StringBuilder server_ip = new StringBuilder();
    private static final String TAG = LightControlActivity.class.getSimpleName();
    private Switch lightSwitch;
    private TextView lightStatusLabel;
    private String lightState = "off";
    private Button SunsetButton, SunriseButton;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_control);
        find_HAServer();
//        authenticate_HA(IP);
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

//    @Nullable
    private void find_HAServer() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String IP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Log.d("zaneto", IP);
        PortScanner portScanner = new PortScanner(IP);
        portScanner.scanPortOnNetwork(8123, new PortScanner.PortScanListener() {
            @Override
            public String onPortOpen(String ipAddress) {
                Log.d("zaneto", "Port 8123 is open on device with IP: " + ipAddress);
                authenticate_HA(ipAddress);
                server_ip.append(ipAddress);
                return ipAddress;
            }
            @Override
            public void onScanComplete() {
                Log.d("zaneto", "Port scan complete");
            }

        });
    }

    private void authenticate_HA(String server_ip){
        HomeAssistantAuthenticator authenticator = new HomeAssistantAuthenticator();
        authenticator.authenticate(server_ip, "amir-t", "tiop8925",
                new HomeAssistantAuthenticator.AuthenticationListener() {
            @Override
            public void onAuthenticationSuccess(String token) {
                // Authentication successful, token received
                Log.d("zaneto", "Authentication successful. Token: " + token);
            }

            @Override
            public void onAuthenticationFailure(String errorMessage) {
                // Authentication failed
                Log.e("zaneto", "Authentication failed: " + errorMessage);
            }
        });
    }

    private void createAutomation(String event) {
        String automationUrl = BASE_URL + "/api/test/automation/" + event;
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

//            String lightControlUrl = BASE_URL + "/api/lights/1";
            String lightControlUrl = "http://" + server_ip.toString() + ":8123/api/services/light/turn_" + state;
            JSONObject requestData = new JSONObject();
//            requestData.put("state", lightState);
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
                            Log.e(TAG, "Error: " + error.toString());
                            String failed_message = "Failed to turn " + lightState + " the light";
                            Toast.makeText(LightControlActivity.this, failed_message, Toast.LENGTH_SHORT).show();
                        }
                    }
            ){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    String LONG_LIVED_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJiYzViZDU4ZmYwMTA0YmY3YTNiMjgyNWVkMzBjMWU5MCIsImlhdCI6MTcxMTYzMTk1NywiZXhwIjoyMDI2OTkxOTU3fQ.rbhk4V17LmbmwwUX8iopqLWCdzr71hq8VRrawINEpw0";
                    headers.put("Authorization", "Bearer " + LONG_LIVED_ACCESS_TOKEN);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            // Add the request to the request queue
            Volley.newRequestQueue(this).add(jsonObjectRequest);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
