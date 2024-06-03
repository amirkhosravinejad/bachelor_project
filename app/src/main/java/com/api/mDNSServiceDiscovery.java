package com.api;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class mDNSServiceDiscovery {
    private final String TAG = "bach-prj";
    private final NsdManager nsdManager;
    private final long TIME_OUT = 10000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean discoveryCompleted = false;
    private final OnHostnameListener listener;
    public interface OnHostnameListener {
        void onHostnameFound(String hostname);
        void onHostnameNotFound();
    }
    public mDNSServiceDiscovery (Context context, OnHostnameListener listener) {
        this.listener = listener;
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        String serviceType = "_home-assistant._tcp";
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }
    private final NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onDiscoveryStarted(String regType) {
            // Discovery has started
            Log.d(TAG, "Discovery started.");
            discoveryCompleted = false; // Reset the flag since discovery might still find services
            checkTimeoutAfterServiceDiscovery(); // Start the timeout
        }
        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found so we initiate service resolution here
            nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                    Log.d(TAG, "Resolve failed");
                }
                @Override
                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    // Service resolved successfully
                    Log.d(TAG, "Service resolved: " + serviceInfo.getServiceName());
                    listener.onHostnameFound(serviceInfo.getHost().getHostName());
                    int port = serviceInfo.getPort();
                    Log.d(TAG, "Host: " + serviceInfo.getHost().getHostName());
                    Log.d(TAG, "Port: " + port);
                    discoveryCompleted = true; // Mark discovery as completed
                }
            });
        }
        @Override
        public void onServiceLost(NsdServiceInfo service) {
            Log.d(TAG, "service lost ");
            discoveryCompleted = true; // Mark discovery as completed
            listener.onHostnameNotFound();
        }
        @Override
        public void onDiscoveryStopped(String serviceType) {Log.d(TAG, "discovery stopped " + serviceType);}
        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.d(TAG, "discovery start failed! error code: " + errorCode);
            listener.onHostnameNotFound();
        }
        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {Log.d(TAG, "discovery stop failed! error code" + errorCode);}
    };

    private void checkTimeoutAfterServiceDiscovery(){
        handler.postDelayed(() -> {
            if (!discoveryCompleted) {
                listener.onHostnameNotFound();
            }
        }, TIME_OUT); // Use the existing TIMEOUT value
    }
}
