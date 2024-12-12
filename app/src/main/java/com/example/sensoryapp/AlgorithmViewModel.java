package com.example.sensoryapp;

import static android.provider.Settings.System.getString;

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
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.android.libraries.places.api.model.Place;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlgorithmViewModel extends AndroidViewModel {
    private final WorkManager workManager;
    private final MutableLiveData<Boolean> isWorkerRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Location> locationLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Place>> _placesLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> averageDecibelLiveData = new MutableLiveData<>();
    public final MutableLiveData<String> mood = new MutableLiveData<>("Good");
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
        predictMood(100, 200);
    }

    private void startSoundWorker() {
        OneTimeWorkRequest soundWorkRequest = new OneTimeWorkRequest.Builder(SoundLvlWorker.class)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .addTag("SoundLvlWorker")
                .build();
        WorkManager.getInstance(getApplication().getApplicationContext()).enqueue(soundWorkRequest);

        WorkManager.getInstance(getApplication().getApplicationContext()).getWorkInfoByIdLiveData(soundWorkRequest.getId()).observeForever(workInfo -> {
            if (workInfo != null) {
                WorkInfo.State state = workInfo.getState();
                if (state == WorkInfo.State.SUCCEEDED) {
                    startSoundWorker();
                } else if (state == WorkInfo.State.FAILED) {
                    Log.e("WorkStatus", "Work failed");
                }
            }
        });

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

    private void predictMood(double averageSound, double numPeople) {
        mapWorker.detectCurrentPlaceType(new MapWorker.PlaceTypeCallback() {
            @Override
            public void onPlaceTypeDetected(String type) {
                double w = getSoundWeight(type);
                double soundIdx = w * (averageSound / SensoryApplication.soundPref);
                double peopleIdx = (1 - w) * (numPeople / SensoryApplication.numPeoplePref);
                double index = soundIdx + peopleIdx;
                if (index > 1.3 && !getApplication().getString(R.string.overstimulated).equals(mood.getValue())) {
                    mood.setValue(getApplication().getString(R.string.overstimulated));
                } else if (index < 0.7 && !getApplication().getString(R.string.understimulated).equals(mood.getValue())) {
                    mood.setValue(getApplication().getString(R.string.understimulated));
                } else {
                    if (!(getApplication().getString(R.string.good)).equals(mood.getValue())) {
                        mood.setValue(getApplication().getString(R.string.good));
                    }
                }
            }
        });
    }

    public LiveData<String> getMood() { return mood; }

    private double getSoundWeight(String type) {
        String[] quiterPlaces = new String[] {"art_gallery", "museum", "library", "bank", "cafe", "bakery", "coffee_shop", "cat_cafe", "tea_house", "spa"};
        if (Arrays.asList(quiterPlaces).contains(type)) {
            return 0.7;
        } else {
            return 0.5;
        }
    }
}

