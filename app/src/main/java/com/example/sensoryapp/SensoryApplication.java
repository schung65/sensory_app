package com.example.sensoryapp;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.ViewModelProvider;

public class SensoryApplication extends Application {
    private static AlgorithmViewModel _algorithmViewModel;

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
}
