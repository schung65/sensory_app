package com.example.sensoryapp;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.CircularBounds;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchNearbyRequest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

public class MapWorker {
    private Location lastKnownLocation;
    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private List<Place> recommendations;

    public void init(Context context) {
        Places.initialize(context, BuildConfig.MAPS_API_KEY);
        placesClient = Places.createClient(context);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void getDeviceLocation(LocationCallback callback) {
        try {
            Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    lastKnownLocation = task.getResult();
                    callback.onLocationAvailable(lastKnownLocation);
                } else {
                    Log.e("MapWorker", "Location not available");
                }
            });
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    public void searchNearby(String[] keywords, Location location, PlacesCallback callback) {
        final List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.LOCATION);
        LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
        CircularBounds circle = CircularBounds.newInstance(center, 1000);
        final List<String> includedTypes = Arrays.asList(keywords);

        final SearchNearbyRequest searchNearbyRequest =
                SearchNearbyRequest.builder(circle, placeFields)
                                    .setIncludedTypes(includedTypes)
                                    .setMaxResultCount(10)
                                    .build();
        placesClient.searchNearby(searchNearbyRequest)
                    .addOnSuccessListener(response -> {
                        recommendations = response.getPlaces();
                        callback.onNearbyPlacesAvailable(recommendations);
                    });
    }

    public interface LocationCallback {
        void onLocationAvailable(Location location);
    }

    public interface PlacesCallback {
        void onNearbyPlacesAvailable(List<Place> places);
    }
}
