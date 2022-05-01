package com.tutk.IOTC.util;

import android.util.Log;

public class LoadLibrary {
    public static final boolean TRACE = false;
    public static final String TAG = "LoadLibrary";

    public static void load(String library) {
        try {
            System.loadLibrary(TRACE ? library + "T" : library);
        } catch (UnsatisfiedLinkError ule) {
            Log.e(TAG, ule.getMessage());
        }
    }
}