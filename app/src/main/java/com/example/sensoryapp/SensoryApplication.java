package com.example.sensoryapp;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

public class SensoryApplication extends Application {
    private static AlgorithmViewModel _algorithmViewModel;
    public static float dbCount = 40;
    public static float minDB =100;
    public static float maxDB =0;
    public static float lastDbCount = dbCount;
    private static float min = 0.5f;  //Set the minimum sound change
    private static float value = 0;   // Sound decibel value
    public static float avgDb = 0;
    public static float soundPref = 40;
    public static float numPeoplePref = 18;

    @Override
    public void onCreate() {
        Log.d("Application", "onCreate()");
        super.onCreate();
        _algorithmViewModel = new ViewModelProvider.AndroidViewModelFactory(this)
                .create(AlgorithmViewModel.class);
    }

    public static AlgorithmViewModel getAlgorithmViewModel() {
        return _algorithmViewModel;
    }

    public static void setDbCount(float dbValue) {
        if (dbValue > lastDbCount) {
            value = Math.max(dbValue - lastDbCount, min);
        }else{
            value = Math.min(dbValue - lastDbCount, -min);
        }
        dbCount = lastDbCount + value * 0.2f ; //To prevent the sound from changing too fast
        lastDbCount = dbCount;
        if(dbCount<minDB) minDB=dbCount;
        if(dbCount>maxDB) maxDB=dbCount;
    }

    public static void setAverageDbCount(float averageDb) {
        avgDb = averageDb;
    }
}
