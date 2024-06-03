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
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.api.datamodel.AutomationData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public class ServiceActivity extends AppCompatActivity {
    private String server_ip, token;
    private static final String TAG = "bach-prj";
    private Switch lightSwitch;
    private TextView lightStatusLabel;
    private String lightState = "off";
    private Button SunsetButton, SunriseButton;
    public interface ApiService {
        @POST("api/config/automation/config/{randomNumber}")
        Call<AutomationData> createAutomation(@Header("Authorization") String token,
                                              @Header("Content-Type") String contentType,
                                              @Body AutomationData automationData,
                                              @Path("randomNumber") int randomNumber);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_control);
        handleIntent(getIntent());

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

    private void createAutomation(String event) {
        String baseURl = "http://" + server_ip + ":8123/";
        String offset, service, alias;
        if ("sunset".equals(event)) {
            alias = "Sunset Lights";
            offset = "-00:11:00";
            service = "light.turn_on";
        }
        else {
            alias = "Sunrise Lights off";
            offset = "+00:30:00";
            service = "light.turn_off";
        }
        AutomationData data = setAutomationData(event, offset, service, alias);
        postAutomationData(baseURl, data);
    }

    private AutomationData setAutomationData(String event,
                                             String offset, String service, String alias) {
        AutomationData.Trigger trigger = new AutomationData().new Trigger();
        trigger.setPlatform("sun");
        trigger.setEvent(event);
        trigger.setOffset(offset);

        AutomationData.Action action = new AutomationData().new Action();
        action.setService(service);
        AutomationData.Action.Target target = new AutomationData().new Action().new Target();
        target.setEntity_id("light.virtual_light_1");
        action.setData(target);

        AutomationData automationData = new AutomationData();
        // Set properties of automationData here
        automationData.setDescription("30 minutes after sunrise, the main light is turned off");
        automationData.setMode("single");
        automationData.setAlias(alias);
        automationData.setTriggers(Collections.singletonList(trigger));
        automationData.setActions(Collections.singletonList(action));
        return automationData;
    }

    private void postAutomationData(String baseURl, AutomationData automationData) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseURl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        String entity_id = "automation." + automationData.getAlias();
        int randomNumber = new Random().nextInt(10000000) + 1;
//        int randomNumber = 3;
        while (checkIfNumberExists(randomNumber, entity_id))
            randomNumber = new Random().nextInt(10000000) + 1;
        Call<AutomationData> call = apiService.createAutomation("Bearer " + token,
                "application/json", automationData, randomNumber);
        call.enqueue(new Callback<AutomationData>() {
            @Override
            public void onResponse(Call<AutomationData> call, retrofit2.Response<AutomationData> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "Automation created successfully.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "response ok");
                } else {
                    // Handle error
                    Toast.makeText(getApplicationContext(), "There was a problem creating automation", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "response is not successful. code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<AutomationData> call, Throwable t) {
                // Handle failure
                Toast.makeText(getApplicationContext(), "Creating automation failed.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failure in automation");
            }
        });
    }

    private boolean checkIfNumberExists(int randomNumber, String entity_id) {
        final boolean[] found = new boolean[1];
      String url = "http://%s:8123/api/states/%s";
      try {
          String checkExistenceURL = String.format(url, server_ip, entity_id);
          JsonObjectRequest checkExistenceRequest = new JsonObjectRequest(
                  checkExistenceURL,
                  jsonObject -> {
                      try {
                          JSONObject attrs = (JSONObject) jsonObject.get("attributes");
                          Integer id = (Integer) attrs.get("id");
                          if (randomNumber == id){
                              Log.d(TAG, "random number "+ randomNumber + "exists");
                              found[0] = true;
                          }

                      } catch (Exception e) {
                          Log.d(TAG, "some exception occurred.");
                          found[0] = false;
                      }

                  },
                  volleyError -> found[0] = false
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
          Volley.newRequestQueue(this).add(checkExistenceRequest);

      } catch (Exception e) {
          e.printStackTrace();
      }
      return found[0];
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
                    response -> {
                        // Handle successful response
                        Log.d(TAG, "Response: " + response.toString());
                        String success_message = "Light turned " + lightState + " successfully";
                        Toast.makeText(ServiceActivity.this, success_message, Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        // Handle error response
                        String errorCause = Objects.requireNonNull(error.getCause()).toString();
                        if (!checkIfJSONArrayException(errorCause)) {
                            Log.e(TAG, "Error: " + error);
                            String failed_message = "Failed to turn " + lightState + " the light";
                            Toast.makeText(ServiceActivity.this, failed_message, Toast.LENGTH_SHORT).show();
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
            Log.d("bach-prj", "salap");
        }
    }

    private Boolean checkIfJSONArrayException(String errorCause) {
        if (errorCause.contains("type org.json.JSONArray cannot be converted to JSONObject")){
            String success_message = "Light turned " + lightState + " successfully";
            Toast.makeText(ServiceActivity.this, success_message, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

}
