package com.example.sensoryapp;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.Manifest;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    float volume = 10000;
    private MyMediaRecorder mRecorder;
    private static final int msgWhat = 0x1001;
    private static final int refreshTime = 100;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean isRecording = false;

    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        FileUtil.initialize(this);
        mRecorder = new MyMediaRecorder();
    }


    private  final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (this.hasMessages(msgWhat)) {
                return;
            }
            volume = mRecorder.getMaxAmplitude();
            if(volume > 0 && volume < 1000000) {
                World.setDbCount(20 * (float)(Math.log10(volume)));
                ((TextView)findViewById(R.id.decibelValue)).setText(String.valueOf(World.dbCount));
            }
            handler.sendEmptyMessageDelayed(msgWhat, refreshTime);
        }
    };

    private void startListenAudio() {
        handler.sendEmptyMessageDelayed(msgWhat, refreshTime);
    }

    public void startRecord(File fFile){
        try{
            mRecorder.setMyRecAudioFile(fFile);
            if (mRecorder.startRecording()) {
                startListenAudio();
            } else {
                Log.e("MainActivity", "startRecord", new Exception("Failed to start recording"));
            }
        }catch(Exception e){
            Log.e("MainActivity", "startRecord", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        File file = FileUtil.createFile(getApplicationContext(), "temp.amr");
        if (file != null) {
            Log.v("file", "file =" + file.getAbsolutePath());
            startRecord(file);
        } else {
            Log.e("MainActivity", "onResume", new Exception("Failed to create file"));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRecorder.delete();
        handler.removeMessages(msgWhat);
    }

    @Override
    protected void onDestroy() {
        handler.removeMessages(msgWhat);
        mRecorder.delete();
        super.onDestroy();
    }
}