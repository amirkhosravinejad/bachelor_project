package com.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.Before;
import org.junit.Test;

public class PortScannerTest {

    private PortScanner portScanner;
    private PortScanner.PortScanListener listener;

    @Before
    public void setUp() {
        listener = mock(PortScanner.PortScanListener.class);

        portScanner = new PortScanner("192.168.1.1", listener);
    }

//    @Test
//    public void testScanPortOnNetworkNotifiesListenerOnPortOpenAndScanComplete() {
//        // Mock the onPortOpen method to simulate a port being open
//        when(listener.onPortOpen(anyString())).thenReturn("Open");
//
//        // Call the scanPortOnNetwork method
//        String result = portScanner.scanPortOnNetwork(8123);
//
//        // Verify that onPortOpen was called
//        verify(listener, times(1)).onPortOpen(anyString());
//
//        // Verify that onScanComplete was called
//        verify(listener, times(1)).onScanComplete();
//
//        // Optionally, verify the result or numberOfIPsChecked if needed
//        // For example:
        // assertEquals("Open", result);
//        // assertEquals(256, portScanner.getNumberOfIPsChecked());
//    }
    @Test
    public void testOnlyConnect() {
//        portScanner.onlyConnect("192.168.1.4", 8123, listener);
        // Verify that onPortOpen was called
        verify(listener, times(1)).onPortOpen(anyString());
        // Verify that onScanComplete was called
        verify(listener, times(1)).onScanComplete();
    }
}
