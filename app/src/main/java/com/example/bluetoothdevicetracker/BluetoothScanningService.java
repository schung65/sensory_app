package com.example.bluetoothdevicetracker;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class BluetoothScanningService extends Service {
    private static final String TAG = "BluetoothScanningService";
    private android.bluetooth.le.BluetoothLeScanner bluetoothLeScanner;
    private BluetoothAdapter bluetoothAdapter;
    private PowerManager.WakeLock wakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Debug log to check service starting
        Log.d(TAG, "BluetoothScanningService started");

        acquireWakeLock();  // Ensure the device doesn't go to sleep during scanning

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device");
            stopSelf();
            return START_NOT_STICKY;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if (bluetoothLeScanner == null) {
            Log.e(TAG, "BluetoothLeScanner not available");
            stopSelf();
            return START_NOT_STICKY;
        }

        startScanning();  // Start scanning for Bluetooth devices
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopScanning();  // Stop scanning when service is destroyed
        releaseWakeLock();
    }

    private void startScanning() {
        // Debug log to confirm scanning has started
        Log.d(TAG, "Starting Bluetooth scan...");

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
        Log.d(TAG, "Bluetooth scan started");
    }

    private void stopScanning() {
        // Debug log to confirm scanning has stopped
        Log.d(TAG, "Stopping Bluetooth scan...");

        bluetoothLeScanner.stopScan(scanCallback);
        Log.d(TAG, "Bluetooth scan stopped");
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BluetoothDeviceTracker::ScanningWakeLock");
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.d(TAG, "WakeLock acquired to prevent device from going to sleep");
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "WakeLock released");
        }
    }

    // Scan callback for handling Bluetooth scan results
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String deviceName = result.getDevice().getName();
            String deviceAddress = result.getDevice().getAddress();

            // Debug log for each device found
            Log.d(TAG, "Device found: " + deviceName + " (" + deviceAddress + ")");

            // Send device information to MainActivity via broadcast
            Intent broadcastIntent = new Intent("BLUETOOTH_DEVICE_FOUND");
            broadcastIntent.putExtra("deviceInfo", deviceName + " - " + deviceAddress);
            sendBroadcast(broadcastIntent);  // Broadcast the device info to MainActivity
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            // Debug log for scan failure
            Log.e(TAG, "Scan failed with error code: " + errorCode);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // No binding needed for this service
        return null;
    }
}
