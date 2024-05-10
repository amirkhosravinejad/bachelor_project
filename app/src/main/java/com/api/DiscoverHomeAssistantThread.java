package com.api;

import android.util.Log;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class DiscoverHomeAssistantThread {

    public static void main() {
        try {
            JmDNS jmDNS = JmDNS.create();

            // Define a ServiceListener to listen for service events
            ServiceListener serviceListener = new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    Log.d("salam", "Service added: " + event.getType());
                }
                @Override
                public void serviceRemoved(ServiceEvent event) {}
                @Override
                public void serviceResolved(ServiceEvent event) {}
            };

            // Start service discovery to discover all available service types
            jmDNS.addServiceListener("_services._dns-sd._udp.local.", serviceListener);

            // Wait for a period of time for services to be discovered
            Thread.sleep(5000);

            // Clean up by removing the ServiceListener
            jmDNS.removeServiceListener("_services._dns-sd._udp.local.", serviceListener);

            jmDNS.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
