package com.example.sensoryapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.Manifest;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.collection.CircularArray;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomappbar.BottomAppBar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQUEST_PERMISSION_CODE = 200;
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private static final String CHANNEL_ID = "sensory_app_notif_channel";

    private boolean permissionsAccepted = false;
    private final String [] permissions = {Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION};

    private AlgorithmViewModel algorithmViewModel;
    private NotificationManager mNotificationManager;

    private GoogleMap map;
    private CircularArray<Place> recommendedPlaces;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CODE);
        initViewModel();
        createNotificationChannel();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button learnMoreButton = findViewById(R.id.learnMoreButton);
        Button declineButton = findViewById(R.id.declineButton);
        Button acceptButton = findViewById(R.id.acceptButton);
        MaterialToolbar appBar = findViewById(R.id.topAppBar);

        learnMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WellnessDataActivity.class);
                startActivity(intent);
            }
        });

        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNextPlace();
            }
        });

        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGoogleMaps();
            }
        });

        appBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.settings) {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getItemId() == R.id.data) {
                    Intent intent = new Intent(MainActivity.this, WellnessDataActivity.class);
                    startActivity(intent);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
    }

    private void initViewModel() {
        algorithmViewModel = SensoryApplication.getAlgorithmViewModel();
        algorithmViewModel.init();
        algorithmViewModel.getPlacesData().observe(this, new Observer<List<Place>>() {
            @Override
            public void onChanged(List<Place> places) {
                CircularArray<Place> placeCircularArray = new CircularArray<>(places.size());
                for (Place place : places) {
                    placeCircularArray.addLast(place);
                }
                recommendedPlaces = placeCircularArray;
            }
        });
        algorithmViewModel.getMood().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                showNotification(s);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            permissionsAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionsAccepted ) finish();

    }

    private void showNextPlace() {
        map.clear();
        Place place = recommendedPlaces.popFirst();
        recommendedPlaces.addLast(place);

        LatLng location = place.getLocation();
        if (location != null) {
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(place.getLocation())
                    .title(place.getDisplayName())
                    .snippet("Click here for directions"));
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
            marker.showInfoWindow();
        }
    }

    private void openGoogleMaps() {
        double lat = recommendedPlaces.getLast().getLocation().latitude;
        double lon = recommendedPlaces.getLast().getLocation().longitude;
        String label = recommendedPlaces.getLast().getDisplayName();
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + String.valueOf(lat) + "," + String.valueOf(lon) + "(" + label + ")");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }

    private void createNotificationChannel() {
        CharSequence name = "Notification Channel";
        String description = "Channel for sending mood notifications from Sensory Application";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotificationManager = getSystemService(NotificationManager.class);
        mNotificationManager.createNotificationChannel(channel);
    }

    private void showNotification(String s) {
        Intent intent = new Intent(this, MainActivity.class);
        // Update this line to add FLAG_IMMUTABLE (since we don't need to modify the PendingIntent)
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_exclamation_mark)
                .setContentTitle("You might be feeling " + s.toLowerCase())
                .setContentText("Click here for more info!")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mNotificationManager.notify(100, builder.build());
    }
}