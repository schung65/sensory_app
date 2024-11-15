package com.example.sensoryapp;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.Manifest;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private List<Place> recommendations;
    private GoogleMap map;

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initPlaces();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        ((Button)findViewById(R.id.searchButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchKeywords = ((EditText)findViewById(R.id.placeTypeInput)).getText().toString();
                searchNearby(searchKeywords.split(", "));
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            double lat = location.getLatitude();
                            double lng = location.getLongitude();
                            LatLng center = new LatLng(lat, lng);
                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(center));
                            googleMap.setMinZoomPreference(15);
                        }
                    });
        };
    }

    private void initPlaces() {
        // Define a variable to hold the Places API key.
        String apiKey = BuildConfig.PLACES_API_KEY;

        // Log an error if apiKey is not set.
        if (TextUtils.isEmpty(apiKey) || apiKey.equals("DEFAULT_API_KEY")) {
            Log.e("Places test", "No api key");
            finish();
            return;
        }

        // Initialize the SDK
        Places.initializeWithNewPlacesApiEnabled(getApplicationContext(), apiKey);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Location permission denied");
            }
        }
    }

    private void searchNearby(String[] keywords) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            LatLng center = new LatLng(0,0);
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                double lat = location.getLatitude();
                                double lng = location.getLongitude();
                                center = new LatLng(lat, lng);
                            }
                            CircularBounds circle = CircularBounds.newInstance(center, 1000);
                            final List<String> includedTypes = Arrays.asList(keywords);

                            final List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION);
                            final SearchNearbyRequest searchNearbyRequest =
                                    SearchNearbyRequest.builder(/* location restriction = */ circle, placeFields)
                                            .setIncludedTypes(includedTypes)
                                            .setMaxResultCount(10)
                                            .build();

                            PlacesClient placesClient = Places.createClient(getApplicationContext());
                            placesClient.searchNearby(searchNearbyRequest)
                                    .addOnSuccessListener(response -> {
                                        recommendations = response.getPlaces();
                                        refreshMap();
                                    });
                        }
                    });
        }
    }

    private void refreshMap() {
        if (map != null) {
            for (Place place : recommendations) {
                map.addMarker(new MarkerOptions()
                    .position(place.getLocation())
                    .title("Marker"));
            }
        }
    }

}