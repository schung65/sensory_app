package com.example.sensoryapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.HashSet;
import java.util.Set;

public class BluetoothWorker {

    private static final int SCAN_DURATION = 5000; // 5 seconds
    private static final int SCAN_INTERVAL = 60000; // 1 minute (60000 milliseconds)

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Set<String> discoveredDevices;
    private Set<String> allowedMacAddresses;
    private Handler handler;
    private Context mContext;

    private int deviceCount;

    // Add a list of allowed MAC addresses
    private final String[] DEVICE_OUI_PREFIXES = {
            "00:1C:B3", "00:1E:C2", "00:1A:2B", "D8:5D:4C", "F8:4F:EE", "F8:6A:34", // Apple (Phone, Watch)
            "00:1E:6A", "18:3D:A2", "00:14:22", "24:54:99", "60:9E:4C", "D0:8D:1C", // Samsung (Phone, Watch)
            "18:66:DC", "AC:8D:3A", // Google Pixel (Phone)
            "00:1A:2B", "00:23:71", "88:88:88", // Huawei (Phone, Watch)

            "C0:38:96", "60:38:E0", "C8:2D:72", // Fitbit (Smartwatch)
            "00:02:5B", "00:12:4B", "80:A2:58", // Garmin (Smartwatch)

            "00:21:6A", "00:25:56", "38:2C:4A", // Dell (Laptop)
            "00:1C:B3", "00:1E:C2", "D8:5D:4C", // Apple (Laptop)

            "5C:8C:CF", "00:0F:61", "40:1D:F1", // JBL (Earphones)
            "00:1F:5B", "00:1A:7D", "F0:8C:00", // Sony (Headphones)
            "00:0C:8E", "18:1F:77", "F8:52:1C", // Bose (Headphones)
            "00:1B:DC", "00:1B:7A", "00:14:91", // Sennheiser (Headphones)

            "00:13:10", "00:24:8C", "00:23:14", // HP (Laptops)
            "00:16:6C", "00:1C:42", "68:7F:74", // Lenovo (Laptops)
            "F4:4D:30", "6C:72:D9",             // Microsoft Surface (Laptop)
            "B8:8D:12", "D4:5A:69", "E4:5F:01", "F0:34:E5", // Apple AirPods (Earphones)
            "40:1D:F1", "00:6F:76",             // Jaybird (Earphones)

            "CE:10"                             // Realtek Bluetooth (headphones/earphones)
    };

    public BluetoothWorker(Context context) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        discoveredDevices = new HashSet<>();
        allowedMacAddresses = new HashSet<>();
        mContext = context;
        handler = new Handler();

        // Add allowed MAC addresses to the set
        for (String mac : DEVICE_OUI_PREFIXES) {
            allowedMacAddresses.add(mac.toUpperCase());
        }
    }

    public void startScanning(BleScanCallback callback) {
        Log.d("BluetoothWorker", "starting scan");
        discoveredDevices.clear();
        deviceCount = 0;
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.d("BluetoothWorker", "Need permission: BLUETOOTH_SCAN");
        }
        bluetoothLeScanner.startScan(scanCallback);
        handler.postDelayed(() -> {
            bluetoothLeScanner.stopScan(scanCallback);
            callback.onBleScanResultsAvailable(discoveredDevices.size());
        }, SCAN_DURATION);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String deviceAddress = result.getDevice().toString().toUpperCase();
            boolean matchesPrefix = allowedMacAddresses.stream().anyMatch(deviceAddress::startsWith);
            // Check if device is unique and either has a valid name or belongs to allowed MAC addresses
            if (!discoveredDevices.contains(deviceAddress) && matchesPrefix) {
                discoveredDevices.add(deviceAddress);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public interface BleScanCallback {
        void onBleScanResultsAvailable(int numPeople);
    }
}
