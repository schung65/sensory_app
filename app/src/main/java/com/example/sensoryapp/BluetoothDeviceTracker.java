package com.example.sensoryapp;

import android.Manifest;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BluetoothDeviceTracker {
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

}



