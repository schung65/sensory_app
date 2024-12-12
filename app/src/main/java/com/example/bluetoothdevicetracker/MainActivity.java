package com.example.bluetoothdevicetracker;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
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
    private TextView uniqueDeviceCountText;
    private RecyclerView recyclerView;
    private DeviceAdapter deviceAdapter;
    private List<String> deviceList = new ArrayList<>();
    private Button btnScan;

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
        uniqueDeviceCountText = findViewById(R.id.uniqueDeviceCountText);

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
        discoveredDevices.clear(); // Reset discovered devices set
        deviceAdapter.notifyDataSetChanged(); // Notify adapter to update the RecyclerView

        // Reset the device count display
        uniqueDeviceCountText.setText("Unique Devices: 0");

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
                Log.d("BluetoothDevice", "Device found: " + deviceName + " (" + deviceAddress + ")");
                updateDeviceCount();
                deviceList.add(deviceName + " - " + deviceAddress);  // Add to device list for RecyclerView
                deviceAdapter.notifyDataSetChanged();  // Update RecyclerView
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
}

