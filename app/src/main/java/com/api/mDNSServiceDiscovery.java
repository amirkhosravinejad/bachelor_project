package com.api;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class mDNSServiceDiscovery {
    private final String TAG = "zaneto";
    private NsdManager nsdManager;
    private StringBuilder host;
    public mDNSServiceDiscovery (Context context) {
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        String serviceType = "_home-assistant._tcp";
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        host = new StringBuilder();
    }
    private final NsdManager.DiscoveryListener discoveryListener = new NsdManager.DiscoveryListener() {
        @Override
        public void onDiscoveryStarted(String regType) {
            // Discovery has started
            Log.d(TAG, "Discovery started.");
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            // A service was found Do something with it!
            // A service was found
            // Initiate service resolution here
            nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                    Log.d(TAG, "Resolve failed");
                }

                @Override
                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    // Service resolved successfully
                    Log.d(TAG, "Service resolved: " + serviceInfo.getServiceName());
                    String hostName = serviceInfo.getHost().getHostName();
                    int port = serviceInfo.getPort();
                    host.append(hostName);
                    Log.d(TAG, "Host: " + host);
                    Log.d(TAG, "Port: " + port);
                }

            });
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            // A service was lost; update state as necessary
            Log.d(TAG, "service lost ");
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            // Discovery has stopped
            Log.d(TAG, "discovery stopped " + serviceType);
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            // Discovery failed to start
            Log.d(TAG, "discovery start failed! error code: " + errorCode);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            // Discovery failed to stop
            Log.d(TAG, "discovery stop failed! error code" + errorCode);
        }

    };

    public String getHostAndPort() {
        return host.toString();
    }
}
