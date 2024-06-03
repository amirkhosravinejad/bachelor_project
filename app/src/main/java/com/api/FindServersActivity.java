package com.api;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FindServersActivity extends AppCompatActivity implements
        mDNSServiceDiscovery.OnHostnameListener, PortScanner.PortScanListener {
    private Button gotoLogin;
    private TextView welcomeMessage;
    private View searchAnimationView;
    private String former_server_ip;
    private String refresh_t;
    private TextView showFindingServers;
    private HandlerThread handlerThread;
    private Handler handler;
    private String server_ip;
    private boolean connectedToServer;
    private String expiry, access_t;
    private int count;
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
                Intent toLogin = new Intent(FindServersActivity.this, LoginActivity.class);
                toLogin.putExtra("server_ip", server_ip);
                // Start LoginActivity
                startActivity(toLogin);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    private void checkIfTokenExists() {
        try (TokenDatabaseHelper dbhelper = new TokenDatabaseHelper(this.getApplicationContext())) {
            String[] fetchedServerToken = dbhelper.fetchLastRow(dbhelper);
            if (fetchedServerToken != null) {
                former_server_ip = fetchedServerToken[0];
                access_t = fetchedServerToken[1];
                refresh_t = fetchedServerToken[2];
                expiry = fetchedServerToken[3];
                count = Integer.parseInt(fetchedServerToken[4]);
            }
            else {
                find_HAServer();
                return;
            }
            connectToFormerIP();

        } catch (Exception e) {
            // if there is an exception in the token extraction phase,
            // we should try to find HA servers and the former IPs not working.
            Log.e("bach-prj", e.toString());
            find_HAServer();
        }
    }
    private void connectToFormerIP() {
        String IP = getLocalIpAddress();
        assert IP != null;
        if (!IP.split("\\.")[0].equals(former_server_ip.split("\\.")[0]) ||
                !IP.split("\\.")[1].equals(former_server_ip.split("\\.")[1]) ||
                !IP.split("\\.")[2].equals(former_server_ip.split("\\.")[2])) {
            Log.d("bach-prj",
                    "current mobile IP is not in the same network with former server IP");
            find_HAServer();
        }
        runPortScanner(former_server_ip);
    }

    private boolean checkValidityOfToken(String expiry) {
        SimpleDateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss 'GMT+03:30' yyyy");
        Date expiryDate;
        try {
            expiryDate = formatter.parse(expiry);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Date new_date = new Date();
        Log.d("bach-prj", "expiry date: " + expiryDate + " now: " + new_date);
        if (new_date.before(expiryDate)) {
            Log.d("bach-prj", "token still valid");
            return true;
        }
        Log.d("bach-prj", "token not valid anymore!");
        return false;
    }

    private void updateTokensInTable(int index) {
        try (TokenDatabaseHelper helper = new TokenDatabaseHelper(getApplicationContext())) {
            helper.setDbReader(helper.getReadableDatabase());
            helper.selectAllRows();
            HA_Authenticator authenticator = new HA_Authenticator();
            String[] token_row = authenticator.getRefreshToken(former_server_ip, refresh_t);
            // if obtaining refresh token was successful
            if (token_row[0] != null && token_row[1] != null && token_row[2] != null) {
                helper.setDbWriter(helper.getWritableDatabase());
                helper.updateRow(new ContentValues(), index, token_row[0], token_row[1], token_row[2]);
                helper.selectAllRows();
                intentToLightControl(token_row[1]);
            }
            // if failed to get refresh token
            else {
                Log.d("bach-prj", "bad request: " + token_row[0] + token_row[1] + token_row[2]);
                find_HAServer();
            }
        }
        catch (Exception e) {
            Log.d("bach-prj", "refresh token exception: " + e);
        }
    }

    private void intentToLightControl(String access_token) {
        Intent toLightControl = new Intent(FindServersActivity.this, ServiceActivity.class);
        toLightControl.putExtra("server_ip", former_server_ip);
        toLightControl.putExtra("token", access_token);
        // Start ServiceActivity
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
//        runPortScanner(null);
    }

    @Override
    public void onHostnameFound(String hostname) {
        Log.d("bach-prj", "Zeroconf discovery host: " + hostname);
        server_ip = hostname;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gotoLogin.setVisibility(View.VISIBLE);
                searchAnimationView.setVisibility(View.INVISIBLE);
            }
        });

    }
    @Override
    public void onHostnameNotFound() {
        Log.d("bach-prj", "Zeroconf discovery can't find service");
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showFindingServers.setVisibility(View.VISIBLE);
                showFindingServers.setText("Please wait for a few time to discover servers.");
            }
        });
        runPortScanner(null);
    }

    private String getLocalIpAddress() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            // Get the IP address of the Wi-Fi connection
            LinkProperties linkProperties = connectivityManager.getLinkProperties
                                            (connectivityManager.getActiveNetwork());
            InetAddress inetAddress;
            assert linkProperties != null;
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
        return null;
    }
    private void runPortScanner(String cachedServerIP) {
        String IP = getLocalIpAddress();
        PortScanner portScanner = new PortScanner(IP, this);
        Log.d("bach-prj", "on runPortScanner cached IP: " + cachedServerIP);
        if (cachedServerIP == null)
            portScanner.scanPortOnNetwork(8123);
        else
           portScanner.onlyConnect(cachedServerIP, 8123);
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
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showVPNWarningPopup("Server not found!");
                showFindingServers.setVisibility(View.INVISIBLE);
                searchAnimationView.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onConnect() {
        Log.d("bach-prj", "connection to former server IP was successful");
        if (checkValidityOfToken(expiry))
            intentToLightControl(access_t);
        else
            updateTokensInTable(count);
    }

    @Override
    public void cannotConnect() {
        Log.d("bach-prj", "failed to connect to former server IP");
        find_HAServer();
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't forget to stop the HandlerThread when the activity is destroyed
        handlerThread.quitSafely();
    }

}

