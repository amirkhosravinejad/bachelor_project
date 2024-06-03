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
        void onConnect();
        void cannotConnect();
    }
    public PortScanner (String IP, PortScanListener listener){
        this.localIPAddress = IP;
        this.listener = listener;
    }

    public String scanPortOnNetwork(final int port) {
        String localIpAddress = localIPAddress;
        if (localIpAddress == null) {
            // Error obtaining local IP address
            if (listener != null)
                listener.onScanComplete();
            return null;
        }
        StringBuilder server_IP = new StringBuilder();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String[] parts = localIpAddress.split("\\.");
                for (int i = 0; i <= 255; i++) {
                    String ipAddress = parts[0] + "." + parts[1] + "." + parts[2] + "." + i;
                    try {
                        Socket socket = new Socket();
                        Log.d("Port Scanner current address", ipAddress);
                        socket.connect(new InetSocketAddress(ipAddress, port), 300);
                        socket.close();
                        if (listener != null)
                            server_IP.append(listener.onPortOpen(ipAddress));
                    } catch (IOException e) {// Port is closed
                    }
                }
                if (listener != null)
                    listener.onScanComplete();
            }
        }).start();
        return server_IP.toString();
    }

    public void onlyConnect(String destIP, int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket();
                    Log.d("bach-prj", "only connect");
                    socket.connect(new InetSocketAddress(destIP, port), 300);
                    socket.close();
                    listener.onConnect();
                } catch (Exception e) {
                    Log.d("bach-prj", "only connect exception " + e.toString());
                    listener.cannotConnect();
                }
            }
        }).start();
    }

    public PortScanListener getListener() {
        return listener;
    }

}