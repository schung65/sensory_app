package com.example.bluetoothdevicetracker;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bluetoothdevicetracker.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final int RECORDING_DURATION = 5000; // 5 seconds
    private static final int SAMPLE_INTERVAL = 1000; // Sample every 1000 ms (1 seconds)
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Set<String> discoveredDevices;
    private Set<String> discoveredPhones;
    private TextView uniqueDeviceCountText;
    private TextView uniquePhoneCountText;
    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private List<String> deviceList = new ArrayList<>();
    private Button btnScan;
    private PowerManager.WakeLock wakeLock;

    // Update the device list (called by the service when devices are found)

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize RecyclerView and Adapter
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceAdapter = new DeviceAdapter(deviceList);
        recyclerView.setAdapter(deviceAdapter);

        discoveredDevices = new HashSet<>();
        discoveredPhones = new HashSet<>();
        uniqueDeviceCountText = findViewById(R.id.uniqueDeviceCountText);
        uniquePhoneCountText = findViewById(R.id.uniquePhoneCountText);

        // Initialize Scan Button
        btnScan = findViewById(R.id.btnScan);
        btnScan.setOnClickListener(v -> startScanning());

        // Check for location and Bluetooth permissions
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        // Check permissions for Bluetooth and Location (needed for scanning)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request necessary permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION_CODE);
        } else {
            initializeBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_CODE) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                initializeBluetooth();
            } else {
                Toast.makeText(this, "Permissions Denied! Cannot scan for Bluetooth devices.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Make sure Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);  // Prompt user to enable Bluetooth
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    private void startScanning() {
        // Clear previous results
        deviceList.clear();
        discoveredDevices.clear();
        discoveredPhones.clear();
        deviceAdapter.notifyDataSetChanged(); // Notify adapter to update the RecyclerView

        // Reset the device count display
        uniqueDeviceCountText.setText("Unique Devices: 0");
        uniquePhoneCountText.setText("Unique Phones, Smartwatches, earphones: 0");

        // Set scan mode to low latency
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        // Check for Bluetooth Scan permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    REQUEST_PERMISSION_CODE);
            return;
        }

        bluetoothLeScanner.startScan(null, scanSettings, scanCallback);
        Toast.makeText(this, "Scanning started...", Toast.LENGTH_SHORT).show();

        // Initialize handler and stop scanning after 10 seconds
        Handler handler = new Handler();  // Initialize the handler
        handler.postDelayed(() -> {
            bluetoothLeScanner.stopScan(scanCallback);
            Toast.makeText(this, "Scanning stopped", Toast.LENGTH_SHORT).show();
        }, 10000); // Stop scan after 10 seconds
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String deviceName = result.getDevice().getName();
            String deviceAddress = result.getDevice().getAddress();

            if (!discoveredDevices.contains(deviceAddress)) {
                discoveredDevices.add(deviceAddress);
                boolean isPhone = isPhoneDevice(deviceAddress, deviceName);
                String deviceLabel = isPhone ? "Phone" : "Non-Phone";  // Label the device as Phone or Non-Phone
                Log.d("BluetoothDevice", deviceLabel + " device found: " + deviceName + " (" + deviceAddress + ")");
                updateDeviceCount();
                deviceList.add(deviceName + " - " + deviceAddress + " (" + deviceLabel + ")");  // Add label to the display
                deviceAdapter.notifyDataSetChanged();  // Update RecyclerView
                // Update unique phone count if the device is a phone
                if (isPhone) {
                    discoveredPhones.add(deviceAddress);
                    updatePhoneCount();
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("BluetoothDevice", "Scan failed with error code: " + errorCode);
            Toast.makeText(MainActivity.this, "Scan failed. Try again.", Toast.LENGTH_SHORT).show();
        }
    };

    private void updateDeviceCount() {
        int uniqueDeviceCount = discoveredDevices.size();
        uniqueDeviceCountText.setText("Unique Devices: " + uniqueDeviceCount);
    }

    private void updatePhoneCount() {
        int uniquePhoneCount = discoveredPhones.size();
        uniquePhoneCountText.setText("Unique Phones, Smartwatches, earphones: " + uniquePhoneCount);
    }

    // check if the MAC address belongs to a phone
    private boolean isPhoneDevice(String macAddress, String deviceName) {
        for (String prefix : DEVICE_OUI_PREFIXES) {
            if (macAddress.startsWith(prefix)) {
                return true;
            }
            if (deviceName != null && !deviceName.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public void updateDeviceList(String deviceInfo) {
        deviceList.add(deviceInfo);  // Add the device to the list
        deviceAdapter.notifyDataSetChanged();  // Notify the adapter to refresh the RecyclerView
    }
    private BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String deviceInfo = intent.getStringExtra("deviceInfo");
            if (deviceInfo != null) {
                updateDeviceList(deviceInfo);  // Update the device list in the UI
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // Register the receiver to listen for the broadcast
        IntentFilter filter = new IntentFilter("BLUETOOTH_DEVICE_FOUND");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(deviceFoundReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister the receiver to prevent memory leaks
        unregisterReceiver(deviceFoundReceiver);
    }

}



