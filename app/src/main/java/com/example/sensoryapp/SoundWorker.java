package com.example.sensoryapp;

import android.content.Context;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class SoundWorker extends Worker {
    private MediaRecorder mMediaRecorder;
    float volume = 10000;
    Context mContext;

    public SoundWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.d("SoundWorker", "Constructor");
        mContext = context;
        mMediaRecorder = new MediaRecorder();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SoundWorker", "doWork");
        volume = getMaxAmplitude();
        // Indicate whether the work finished successfully with the Result
        if (volume == 0) {
            Log.d("SoundWorker", "failed");
            return Result.failure();
        } else {
            if(volume > 0 && volume < 1000000) {
                World.setDbCount(20 * (float)(Math.log10(volume)));
                Log.d("SoundWorker", "got volume " + String.valueOf(World.dbCount));
            }
            return Result.success();
        }
    }

    public float getMaxAmplitude() {
        if (mMediaRecorder != null) {
            try {
                return mMediaRecorder.getMaxAmplitude();
            } catch (IllegalArgumentException e) {
                Log.e("MyMediaRecorder", "getMaxAmplitude", e);
                return 0;
            }
        } else {
            return 5;
        }
    }

    private void scheduleNextWork() {
        OneTimeWorkRequest nextWorkRequest = new OneTimeWorkRequest.Builder(SoundWorker.class)
                .setInitialDelay(5, TimeUnit.MINUTES) // 5-minute delay
                .build();
        WorkManager.getInstance(mContext).enqueue(nextWorkRequest);
    }
}
