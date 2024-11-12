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
                Log.e("MyMediaRecorder", "getMaxAmplitude", e);
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

    public boolean startRecording(){
        if (myRecAudioFile == null) {
            return false;
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
            return true;
        } catch(IOException exception) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            isRecording = false ;
            Log.e("MyMediaRecorder", "startRecording", exception);
        }catch(IllegalStateException e){
            stopRecording();
            Log.e("MyMediaRecorder", "startRecording", e);
            isRecording = false ;
        }
        return false;
    }




    public void stopRecording() {
        if (mMediaRecorder != null){
            if(isRecording){
                try{
                    mMediaRecorder.stop();
                    mMediaRecorder.release();
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            mMediaRecorder = null;
            isRecording = false ;
        }
    }




    public void delete() {
        stopRecording();
        if (myRecAudioFile != null) {
            myRecAudioFile.delete();
            myRecAudioFile = null;
        }
    }
}