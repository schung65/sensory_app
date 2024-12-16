package com.example.sensoryapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.CoroutineWorker;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.IOException;

import kotlin.coroutines.Continuation;

public class SoundWorker extends CoroutineWorker {
    private static final int RECORDING_DURATION = 3000; // 3 seconds
    private static final int SAMPLE_INTERVAL = 100; // Sample every 100 ms (0.1 seconds)
    private static final String CHANNEL_ID = "SoundWorkerChannel";
    private static final int NOTIFICATION_ID = 1;

    private MediaRecorder mMediaRecorder;
    private PowerManager.WakeLock wakeLock;
    Context mContext;

    public SoundWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        Log.d("SoundWorker", "Constructor");
        mContext = context;
        initializeRecFile(mContext);
    }

    @NonNull
    @Override
    public Object doWork(@NonNull Continuation<? super Result> continuation) {
        Log.d("SoundWorker", "doWork");
        setForegroundAsync(createForegroundInfo());

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
                    if (amplitude == 0) {
                        Log.d("SoundWorker", "failed");
                    } else {
                        if(amplitude > 0 && amplitude < 1000000) {
                            SensoryApplication.setDbCount(20 * (float)(Math.log10(amplitude)));
                            totalAmplitude[0] += SensoryApplication.dbCount;
                            sampleCount[0]++;
                        }
                    }
                    handler.postDelayed(this, SAMPLE_INTERVAL);
                } else {
                    stopRecording();
                    float averageDb = sampleCount[0] > 0 ? totalAmplitude[0] / sampleCount[0] : 0;
                    Data outputData = new Data.Builder()
                            .putFloat("averageDb", averageDb)
                            .build();
                    Log.d("SoundWorker", "Average dB level: " + averageDb);
                    SensoryApplication.setAverageDbCount(averageDb);
                }
            }
        };
        handler.post(collectSampleRunnable);
        releaseWakeLock();
        return Result.success();
    }

    private ForegroundInfo createForegroundInfo() {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sound Worker",
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Create the Notification
        Notification notification = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setContentTitle("Sound Worker")
                .setContentText("Measuring sound levels...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }

    public float getMaxAmplitude() {
        if (mMediaRecorder != null) {
            try {
                return mMediaRecorder.getMaxAmplitude();
            } catch (IllegalArgumentException e) {
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
            Log.e("SoundWorker", "prepare() failed");
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
                Log.e("SoundWorker", "initialize: ", new Exception("Failed to make recording file directory"));
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
            Log.d("SoundWorker", "WakeLock acquired");
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d("SoundWorker", "WakeLock released");
        }
    }

    public interface SoundCallback {
        void onAverageDecibelAvailable(float averageDb);
    }
}