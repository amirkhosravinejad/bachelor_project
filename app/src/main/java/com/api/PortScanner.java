package com.api;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class PortScanner {
    private final String localIPAddress;
    private final PortScanListener listener;
    public interface PortScanListener {
        String onPortOpen(String ipAddress);
        void onScanComplete();
    }
    public PortScanner (String IP, PortScanListener listener){
        this.localIPAddress = IP;
        this.listener = listener;
    }

    public String scanPortOnNetwork(final int port) {
        StringBuilder server_IP = new StringBuilder();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String localIpAddress = localIPAddress;
                if (localIpAddress == null) {
                    // Error obtaining local IP address
                    if (listener != null) {
                        listener.onScanComplete();
                    }
                    return;
                }

                String[] parts = localIpAddress.split("\\.");
                for (int i = 0; i <= 255; i++) {
//                    for (int j = 0; j <= 255; j++) {
                        final String ipAddress = parts[0] + "." + parts[1] + "." + parts[2] + "." + i;
                        try {
                            Socket socket = new Socket();
                            Log.d("Port Scanner current address", ipAddress);
                            socket.connect(new InetSocketAddress(ipAddress, port), 300);
                            socket.close();
                            if (listener != null) {
                                server_IP.append(listener.onPortOpen(ipAddress));
                            }
                        } catch (IOException e) {
                        // Port is closed
                        }
//                    }
                }
                if (listener != null) {
                    listener.onScanComplete();
                }
            }
        }).start();
        return server_IP.toString();
    }
}