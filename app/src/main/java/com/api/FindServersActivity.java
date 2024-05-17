package com.api;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.api.database.TokenDatabaseHelper;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FindServersActivity extends AppCompatActivity {
    private String server_ip;
    private Button gotoLogin;
    private ListView serverListView;
    private ArrayList<String> serverList;

    private ArrayAdapter<String> adapter;

    private HandlerThread handlerThread;
    private Handler handler;
    private String former_server_ip;
    private String refresh_t;

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
            showVPNWarningPopup();
        else {
            checkIfTokenExists();
//            find_HAServer();
        }

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

    private void checkIfTokenExists(){
        try (TokenDatabaseHelper dbhelper = new TokenDatabaseHelper(this.getApplicationContext())) {
            SQLiteDatabase db = dbhelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM tokens", null);
            int count = cursor.getCount();
            if (count == 0)
                return;
            cursor.move(count);
            former_server_ip = cursor.getString(1);
            String access_t = cursor.getString(2);
            refresh_t = cursor.getString(3);
            String expiry = cursor.getString(4);
            Log.d("zaneto", "row " + cursor.getPosition() + " serverIP: " + former_server_ip +
                    " access token: " + access_t + " refresh token: " + refresh_t + " expiry : " + expiry);

            ConnectTask task = new ConnectTask();
            int return_ = task.execute().get();

            // check if Access Token is still valid or not;
            // if expired, we should get refresh token
            // and if it's still valid, we can pass Login and authorization phase.
            SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss 'GMT+03:30' yyyy");
            Date expiryDate = formatter.parse(expiry);
            Date new_date = new Date();
            Log.d("zaneto", "expiry date: " + expiryDate + " now: " + new_date);
            if (new_date.before(expiryDate)){
                Log.d("zaneto", "token still valid");
                intentToLightControl(access_t);
            }
            else {
                Log.d("zaneto", "token not valid anymore!");
                if (return_ == 0) {
                    TokenDatabaseHelper helper = new TokenDatabaseHelper(getApplicationContext());
                    helper.selectAllRows(helper.getReadableDatabase());
                    HomeAssistantAuthenticator authenticator = new HomeAssistantAuthenticator();
                    String[] token_row = authenticator.getRefreshToken(former_server_ip, refresh_t);
                    if (token_row[0] != null && token_row[1] != null && token_row[2] != null){
                        helper.updateRow(count, token_row[0], token_row[1], token_row[2]);
                        helper.selectAllRows(helper.getReadableDatabase());
                        intentToLightControl(token_row[1]);
                    }
                    else{
                        find_HAServer();
                    }

                }
            }

        } catch (Exception e){
            // if there is an exception, means socket is not connected,
            // so we should find HA servers again.
            Log.e("zaneto", e.toString());
            find_HAServer();
        }
    }

    private void intentToLightControl(String access_token) {
        Intent toLightControl = new Intent(FindServersActivity.this, LightControlActivity.class);
        toLightControl.putExtra("server_ip", former_server_ip);
        toLightControl.putExtra("token", access_token);
        // Start LightControlActivity
        startActivity(toLightControl);
    }

    // check if any vpn is on in the host or not.
    private boolean isVpnActive() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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
        try {
            ZeroconfDiscoveryTask discoveryTask = new ZeroconfDiscoveryTask();
            String host = discoveryTask.execute().get();
            if (discoveryTask.hostName == null)
                runPortScanner();
            else {
                server_ip = discoveryTask.hostName;
                Log.d("zaneto", "host: " + host);
            }
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

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

    private void runPortScanner() {
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
    private void showVPNWarningPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning")
                .setMessage("Please make sure no VPN is on.")
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
    private class ConnectTask extends AsyncTask<Void, Void, Integer> {
        private int isConnectedToFormerIP = -1;
        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                Socket socket = new Socket(former_server_ip, 8123);
                socket.setSoTimeout(2000);
                socket.close();
                Log.d("zaneto", "connection to former server IP was successful.");
                return 0;
            } catch (Exception e) {
                Log.e("zaneto", e.getMessage());
                return 1;
            }

        }
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            isConnectedToFormerIP = result;
        }

    }

    private class ZeroconfDiscoveryTask extends AsyncTask<Void, Void, String>{
        private String hostName;
        @Override
        protected String doInBackground(Void... voids) {
            mDNSServiceDiscovery mDNSServiceDiscovery = new mDNSServiceDiscovery(getApplicationContext());
            String host = mDNSServiceDiscovery.getHostAndPort();
            return host;
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            hostName = result;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to stop the HandlerThread when the activity is destroyed
        handlerThread.quitSafely();
    }
}

