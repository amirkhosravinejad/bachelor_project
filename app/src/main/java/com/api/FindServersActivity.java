package com.api;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.api.database.TokenDatabaseHelper;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FindServersActivity extends AppCompatActivity implements
        mDNSServiceDiscovery.OnHostnameListener, PortScanner.PortScanListener {

    private Button gotoLogin;

    private TextView welcomeMessage;

    private View searchAnimationView;
    private String server_ip;
    private HandlerThread handlerThread;
    private Handler handler;
    private String former_server_ip;
    private String refresh_t;
    private TextView showFindingServers;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // initializing the widgets in the UI
        setContentView(R.layout.activity_find_servers);
        gotoLogin = findViewById(R.id.GotoLogin);
        searchAnimationView = findViewById(R.id.SearchAnimationView);
        showFindingServers = findViewById(R.id.portScannerSearchTextView);

        gotoLogin.setVisibility(View.GONE);
        searchAnimationView.setVisibility(View.VISIBLE);

        // Initially hide the Button
        findViewById(R.id.GotoLogin).setVisibility(View.GONE);

        // Show the welcome message
        welcomeMessage = findViewById(R.id.welcomeMessage);
        welcomeMessage.setVisibility(View.VISIBLE);

        // check if VPN active; if it's active, show the popup
        // and if it is not try to find Home Assistant server
        if (isVpnActive())
            showVPNWarningPopup("Please make sure no VPN is on");
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
            if (count == 0){
                find_HAServer();
                return;
            }
            cursor.move(count);
            former_server_ip = cursor.getString(1);
            String access_t = cursor.getString(2);
            refresh_t = cursor.getString(3);
            String expiry = cursor.getString(4);
            Log.d("bach-prj", "row " + cursor.getPosition() + " serverIP: " + former_server_ip +
                    " access token: " + access_t + " refresh token: " + refresh_t + " expiry : " + expiry);

            String IP = getLocalIpAddress();
            if (!IP.split("\\.")[0].equals(former_server_ip.split("\\.")[0]) ||
                !IP.split("\\.")[1].equals(former_server_ip.split("\\.")[1]) ||
                !IP.split("\\.")[2].equals(former_server_ip.split("\\.")[2])) {
                Log.d("bach-prj",
                        "current mobile IP is not in the same network with former server IP");
                find_HAServer();
                return;
            }
            ConnectTask task = new ConnectTask();
            int return_ = task.execute().get();

            // check if Access Token is still valid or not;
            // if expired, we should get refresh token
            // and if it's still valid, we can pass Login and authorization phase.
            SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss 'GMT+03:30' yyyy");
            Date expiryDate = formatter.parse(expiry);
            Date new_date = new Date();
            Log.d("bach-prj", "expiry date: " + expiryDate + " now: " + new_date);
            if (new_date.before(expiryDate)){
                Log.d("bach-prj", "token still valid");
                intentToLightControl(access_t);
            }
            else {
                Log.d("bach-prj", "token not valid anymore!");
                // if connection to the former server IP retrieved from database
                // is ok (return_ == 0)
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
                    else {
                        Log.d("bach-prj", "bad request: " + token_row[0] +
                                token_row[1] + token_row[2]);
                        find_HAServer();
                    }

                }
                // if failed to connect to former Server IP
                // return_ == 1
                else {
                    Log.d("bach-prj", "failed to connect to former server IP");
                    find_HAServer();
                }
            }

        } catch (Exception e){
            // if there is an exception, means socket is not connected,
            // so we should find HA servers again.
            Log.e("bach-prj", e.toString());
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
        if (connectivityManager != null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (networkCapabilities != null) {
                    return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
                }
            }
        }
        return false;
    }

    private void find_HAServer() {
        mDNSServiceDiscovery mDNSDiscovery = new mDNSServiceDiscovery(this, this);
        Log.d("bach-prj", "we're in finally after try catch in findHAServer()");
    }

    @Override
    public void onHostnameFound(String hostname) {
        Log.d("bach-prj", "Zeroconf discovery host: " + hostname);
        server_ip = hostname;
        gotoLogin.setVisibility(View.VISIBLE);
        searchAnimationView.setVisibility(View.INVISIBLE);
    }
    @Override
    public void onHostnameNotFound() {
        Log.d("bach-prj", "Zeroconf discovery can't find service");
        showFindingServers.setVisibility(View.VISIBLE);
        showFindingServers.setText("Please wait for a few time to discover servers.");
        runPortScanner();
    }

    private String getLocalIpAddress() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            // Get the IP address of the Wi-Fi connection
            LinkProperties linkProperties = connectivityManager.getLinkProperties
                                            (connectivityManager.getActiveNetwork());
            InetAddress inetAddress;
            for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                inetAddress = linkAddress.getAddress();
                if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()
                        && inetAddress.isSiteLocalAddress()) {
                    Log.d("bach-prj", "in getLocal: " + inetAddress.getHostAddress());
                    return inetAddress.getHostAddress();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // If Wi-Fi is not available, try to get the IP address from the mobile data connection
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.isUp())
                    Log.d("bach-prj", "interface name: " + intf.getDisplayName());
                if (intf.getName().equalsIgnoreCase("swlan0")) {
                    byte[] ipAddr = intf.getInterfaceAddresses().get(1).getAddress().getAddress();
                    Log.d("bach-prj", "salap: " + InetAddress.getByAddress(ipAddr).getHostAddress());
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
        PortScanner portScanner = new PortScanner(IP, this);
        portScanner.scanPortOnNetwork(8123);
    }
    @Override
    public String onPortOpen(String ipAddress) {
        Log.d("bach-prj", "Port 8123 is open on device with IP: " + ipAddress);
        server_ip = ipAddress;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("bach-prj", "arze adab");
                showFindingServers.setVisibility(View.INVISIBLE);
                searchAnimationView.setVisibility(View.INVISIBLE);
                gotoLogin.setVisibility(View.VISIBLE);
            }
        });
        return ipAddress;
    }
    @Override
    public void onScanComplete() {
        Log.d("bach-prj", "Port scan complete");
        showVPNWarningPopup("Server not found!");
        searchAnimationView.setVisibility(View.INVISIBLE);
    }

    private void showVPNWarningPopup(String messageToShow) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning")
                .setMessage(messageToShow)
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
                Log.d("bach-prj", "connection to former server IP was successful.");
                return 0;
            } catch (Exception e) {
                Log.e("bach-prj", e.getMessage());
                return 1;
            }

        }
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            isConnectedToFormerIP = result;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to stop the HandlerThread when the activity is destroyed
        handlerThread.quitSafely();
    }

}

