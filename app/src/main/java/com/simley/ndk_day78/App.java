package com.simley.ndk_day78;

import android.app.Application;

public class App extends Application {

    static {
        System.loadLibrary("ndk_day78");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
