package com.example.sensoryapp;

import java.io.File;
import java.io.IOException;
import android.media.MediaRecorder;
import android.util.Log;

public class MyMediaRecorder {

    public  File myRecAudioFile ;
    private MediaRecorder mMediaRecorder ;
    public boolean isRecording = false ;

    public float getMaxAmplitude() {
        if (mMediaRecorder != null) {
            try {
                return mMediaRecorder.getMaxAmplitude();
            } catch (IllegalArgumentException e) {
                Log.e("MyMediaRecorder", "getMaxAmplitude: ", e);
                return 0;
            }
        } else {
            return 5;
        }
    }

    public File getMyRecAudioFile() {
        return myRecAudioFile;
    }

    public void setMyRecAudioFile(File myRecAudioFile) {
        this.myRecAudioFile = myRecAudioFile;
    }

    public void startRecording(){
        if (myRecAudioFile == null) {
            return;
        }
        try {
            mMediaRecorder = new MediaRecorder();

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setOutputFile(myRecAudioFile.getAbsolutePath());

            mMediaRecorder.prepare();
            mMediaRecorder.start();
            isRecording = true;
        } catch(IOException exception) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            isRecording = false ;
            Log.e("MyMediaRecorder", "startRecorder: ", exception);
        }catch(IllegalStateException e){
            stopRecording();
            Log.e("MyMediaRecorder", "startRecorder: ", e);
            isRecording = false ;
        }
    }

    public void stopRecording() {
        if (mMediaRecorder != null){
            if(isRecording){
                try{
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                }catch(Exception e){
                    Log.e("MyMediaRecorder", "stopRecording: ", e);
                }
            }
            mMediaRecorder = null;
            isRecording = false ;
        }
    }

    public void delete() {
        stopRecording();
        if (myRecAudioFile != null) {
            if (myRecAudioFile.delete()) {
                myRecAudioFile = null;
            } else {
                Log.e("MyMediaRecorder", "delete: ", new Exception("Failed to delete audio file"));
            }
        }
    }
}