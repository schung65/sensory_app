package com.example.sensoryapp;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.android.libraries.places.api.model.Place;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlgorithmViewModel extends AndroidViewModel {
    private final WorkManager workManager;
    private final MutableLiveData<Boolean> isWorkerRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Location> locationLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Place>> _placesLiveData = new MutableLiveData<>();
    private Set<String> _understimKeywords;
    private Set<String> _overstimKeywords;

    private MapWorker mapWorker;

    public AlgorithmViewModel(@NonNull Application application) {
        super(application);
        workManager = WorkManager.getInstance(application);
        mapWorker = new MapWorker();
    }

    public LiveData<List<Place>> getPlacesData() { return _placesLiveData; }
    public void setUnderstimPreferences(Set<String> preferences) {
        _understimKeywords = preferences;
    }
    public void setOverstimPreferences(Set<String> preferences) {
        _overstimKeywords = preferences;
    }

    public void init() {
        startSoundWorker();
        mapWorker.init(getApplication().getApplicationContext());

        getNearbyPlaces(new String[]{"cafe"});
    }

    private void startSoundWorker() {
        OneTimeWorkRequest soundWorkRequest = new OneTimeWorkRequest.Builder(SoundLvlWorker.class)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .addTag("SoundLvlWorker")
                .build();
        WorkManager.getInstance(getApplication().getApplicationContext()).enqueue(soundWorkRequest);

        isWorkerRunning.setValue(true);
        Log.d("AlgorithmViewModel", "SoundLvlWorker started");
    }

    private void stopSoundWorker() {
        workManager.cancelAllWorkByTag("SoundLvlWorker");
        isWorkerRunning.setValue(false);
        Log.d("AlgorithmViewModel", "SoundLvlWorker stopped");
    }

    public LiveData<Location> getLocation() {
        mapWorker.getDeviceLocation(new MapWorker.LocationCallback() {
            @Override
            public void onLocationAvailable(Location location) {
                locationLiveData.postValue(location);
            }
        });
        return locationLiveData;
    }

    public void getNearbyPlaces(String[] keywords) {
        getLocation().observeForever(new Observer<Location>() {
            @Override
            public void onChanged(Location location) {
                if (location != null) {
                    mapWorker.searchNearby(keywords, location, new MapWorker.PlacesCallback() {
                        @Override
                        public void onNearbyPlacesAvailable(List<Place> places) {
                            _placesLiveData.setValue(places);
                        }
                    });
                } else {
                    Log.d("AlgorithmViewModel", "Failed to get current location in getNearbyPlaces()");
                }
            }
        });
    }
}

