package com.api;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        // check if VPN active; if it's active, show the popup
        // and if it is not try to find Home Assistant server
        if (isVpnActive())
            showWarningPopup();
        else
            find_HAServer();

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

    // check if any vpn is on in the host or not.
    private boolean isVpnActive() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager!= null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork!= null) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (networkCapabilities!= null) {
                    return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
                }
            }
        }
        return false;
    }

    private void find_HAServer() {
//        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
//        String IP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        String IP = getLocalIpAddress();
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

    private String getLocalIpAddress() {
        try {
            Context context = FindServersActivity.this.getApplicationContext();
            // Get the IP address of the Wi-Fi connection
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int ipAddress = wifiInfo.getIpAddress();

            // Convert the IP address to a string
            if (ipAddress!= 0) {
                return InetAddress.getByAddress(new byte[]{(byte) (ipAddress & 0xff),
                        (byte) ((ipAddress >> 8) & 0xff),
                        (byte) ((ipAddress >> 16) & 0xff),
                        (byte) ((ipAddress >> 24) & 0xff)}).getHostAddress();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If Wi-Fi is not available, try to get the IP address from the mobile data connection
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.isUp())
                    Log.d("zaneto", "interface name: " + intf.getDisplayName());
                if (intf.getName().equalsIgnoreCase("wlan0") ||
                        intf.getName().equalsIgnoreCase("swlan0")) {

//                    byte[] ipAddr = intf.getInetAddresses().nextElement().getAddress();
                    byte[] ipAddr = intf.getInterfaceAddresses().get(0).getAddress().getAddress();
                    return InetAddress.getByAddress(ipAddr).getHostAddress();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
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

