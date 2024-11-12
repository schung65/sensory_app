package com.example.sensoryapp;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class FileUtil {

    private static final String TAG = "FileUtil";

    public static final String LOCAL = "SoundMeter";

    public static String getRecPath(Context context) {
        return context.getFilesDir().getPath() + File.separator + LOCAL + File.separator;
    }

    public static void initialize(Context context) {
        String recPath = getRecPath(context);
        File recDir = new File(recPath);

        if (!recDir.exists()) {
            boolean makeRecDir = recDir.mkdirs();
            if (!makeRecDir) {
                Log.e(TAG, "initialize: ", new Exception("Failed to make recording file directory"));
            }
        }
    }

    private FileUtil() {
    }

    private static boolean hasFile(Context context, String fileName) {
        File f = createFile(context, fileName);
        return null != f && f.exists();
    }

    public static File createFile(Context context, String fileName) {

        File myCaptureFile = new File(getRecPath(context) + fileName);
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


}