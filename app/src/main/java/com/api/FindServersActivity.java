package com.api;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class FindServersActivity extends AppCompatActivity {
    private String server_ip;
    private Button gotoLogin;
    private ListView serverListView;
    private ArrayList<String> serverList;

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_servers);
        gotoLogin = findViewById(R.id.GotoLogin);
        serverListView = findViewById(R.id.serverListView);
        serverList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, serverList);
        serverListView.setAdapter(adapter);

        find_HAServer();
//        if (server_ip != null) {
//            // on below line we are adding item to our list.
//            serverList.add(server_ip);
//
//            // on below line we are notifying adapter
//            // that data in list is updated to
//            // update our list view.
//            adapter.notifyDataSetChanged();
//        }
        showWarningPopup();

        gotoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent toMain = new Intent(FindServersActivity.this, MainActivity.class);
                toMain.putExtra("server_ip", server_ip);
                // Start MainActivity
                startActivity(toMain);
            }
        });
    }

    private void find_HAServer() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String IP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Log.d("zaneto", IP);
        PortScanner portScanner = new PortScanner(IP);
        portScanner.scanPortOnNetwork(8123, new PortScanner.PortScanListener() {
            @Override
            public String onPortOpen(String ipAddress) {
                Log.d("zaneto", "Port 8123 is open on device with IP: " + ipAddress);
                // Run UI updates on the main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        server_ip = ipAddress;
                        serverList.add(server_ip);
                        adapter.notifyDataSetChanged();
                    }
                });
                return ipAddress;
            }
            @Override
            public void onScanComplete() {
                Log.d("zaneto", "Port scan complete");
            }

        });
    }

    private void showWarningPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning")
                .setMessage("Please make sure your mobile is connected to the same network with your server and no VPN is on.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}

