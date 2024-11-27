package com.example.sensoryapp;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import java.util.concurrent.TimeUnit;

public class AlgorithmViewModel extends AndroidViewModel {
    private final WorkManager workManager;
    private final MutableLiveData<Boolean> isWorkerRunning = new MutableLiveData<>(false);

    public AlgorithmViewModel(@NonNull Application application) {
        super(application);
        workManager = WorkManager.getInstance(application);
    }

    public void startSoundWorker() {
        OneTimeWorkRequest soundWorkRequest = new OneTimeWorkRequest.Builder(SoundLvlWorker.class)
                .setInitialDelay(5, TimeUnit.SECONDS)
                .addTag("SoundLvlWorker")
                .build();
        WorkManager.getInstance(getApplication().getApplicationContext()).enqueue(soundWorkRequest);

        isWorkerRunning.setValue(true);
        Log.d("AlgorithmViewModel", "SoundLvlWorker started");
    }

    public void stopSoundWorker() {
        workManager.cancelAllWorkByTag("SoundLvlWorker");
        isWorkerRunning.setValue(false);
        Log.d("AlgorithmViewModel", "SoundLvlWorker stopped");
    }

    public LiveData<Boolean> getWorkerStatus() {
        return isWorkerRunning;
    }
}

