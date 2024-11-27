package com.example.sensoryapp;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.IOException;

public class SoundLevelService extends Worker {
    private MediaRecorder mMediaRecorder;
    private PowerManager.WakeLock wakeLock;
    Context mContext;
    private static final int RECORDING_DURATION = 3000; // 3 seconds
    private static final int SAMPLE_INTERVAL = 100; // Sample every 100 ms (0.1 seconds)

    public SoundLevelService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.d("SoundLevelService", "Constructor");
        mContext = context;
        initializeRecFile(mContext);
        mMediaRecorder = new MediaRecorder();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SoundLevelService", "doWork");
        acquireWakeLock();
        startRecording();
        final long startTime = System.currentTimeMillis();
        final long endTime = startTime + RECORDING_DURATION;
        final float[] totalAmplitude = {0};
        final int[] sampleCount = {0};

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable collectSampleRunnable = new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() < endTime) {
                    float amplitude = getMaxAmplitude();
                    Log.d("SoundLevelService", "Sampled amplitude: " + amplitude);
                    if (amplitude == 0) {
                        Log.d("SoundLevelService", "failed");
                    } else {
                        if(amplitude > 0 && amplitude < 1000000) {
                            World.setDbCount(20 * (float)(Math.log10(amplitude)));
                            totalAmplitude[0] += World.dbCount;
                            sampleCount[0]++;
                            Log.d("SoundLevelService", "got dB " + String.valueOf(World.dbCount));
                        }
                    }
                    handler.postDelayed(this, SAMPLE_INTERVAL);
                } else {
                    float averageDb = sampleCount[0] > 0 ? totalAmplitude[0] / sampleCount[0] : 0;
                    Log.d("SoundLevelService", "Average dB level: " + averageDb);
                    stopRecording();
                }
            }
        };
        handler.post(collectSampleRunnable);
        return Result.success();
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

    private void startRecording() {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setOutputFile(createRecFile(mContext));
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e("SoundLevelService", "prepare() failed");
        }

        mMediaRecorder.start();
    }

    private void stopRecording() {
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
        deleteRecFile(mContext);
    }

    public static String getRecPath(Context context) {
        return context.getFilesDir().getPath() + File.separator + "SensoryApp" + File.separator;
    }

    public static void initializeRecFile(Context context) {
        String recPath = getRecPath(context);
        File recDir = new File(recPath);

        if (!recDir.exists()) {
            boolean makeRecDir = recDir.mkdirs();
            if (!makeRecDir) {
                Log.e("SoundLevelService", "initialize: ", new Exception("Failed to make recording file directory"));
            }
        }
    }

    public static File createRecFile(Context context) {
        File myCaptureFile = new File(getRecPath(context) + "temp.amr");
        if (myCaptureFile.exists()) {
            myCaptureFile.delete();
        }
        try {
            myCaptureFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myCaptureFile;
    }

    private static void deleteRecFile(Context context) {
        File myCaptureFile = new File(getRecPath(context) + "temp.amr");
        if (myCaptureFile.exists()) {
            myCaptureFile.delete();
        }
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensoryApp::SoundWorkerWakeLock");
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
            Log.d("SoundLevelService", "WakeLock acquired");
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d("SoundLevelService", "WakeLock released");
        }
    }
}
