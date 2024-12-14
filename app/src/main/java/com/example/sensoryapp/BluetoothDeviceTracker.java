package com.example.sensoryapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;

public class BluetoothDeviceTracker {

    private static final int SCAN_DURATION = 5000; // 5 seconds
    private static final int SCAN_INTERVAL = 60000; // 1 minute (60000 milliseconds)

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Set<String> discoveredDevices;
    private TextView uniqueDeviceCountText;
    private Handler handler;

    private int deviceCount;

    // Constructor
    public BluetoothDeviceTracker(Context context, TextView uniqueDeviceCountText) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        this.discoveredDevices = new HashSet<>();
        this.uniqueDeviceCountText = uniqueDeviceCountText;
        this.handler = new Handler();

        // Start scanning automatically when the class is initialized
        startScanning();
    }

    private void startScanning() {
        // Clear previous discovered devices
        discoveredDevices.clear();
        deviceCount = 0;
        updateDeviceCount();

        // Start scanning for Bluetooth devices
        bluetoothLeScanner.startScan(scanCallback);

        // Stop scanning after 5 seconds (SCAN_DURATION)
        handler.postDelayed(() -> {
            bluetoothLeScanner.stopScan(scanCallback);
            updateDeviceCount();
            // Wait for 1 minute (SCAN_INTERVAL) before starting the next scan
            handler.postDelayed(this::startScanning, SCAN_INTERVAL);
        }, SCAN_DURATION);
    }

    private void updateDeviceCount() {
        uniqueDeviceCountText.setText("Devices Found: " + deviceCount);  // Update the TextView with the count
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String deviceAddress = result.getDevice().getAddress();
            if (!discoveredDevices.contains(deviceAddress)) {
                discoveredDevices.add(deviceAddress);
                deviceCount++;
                updateDeviceCount();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };
}
