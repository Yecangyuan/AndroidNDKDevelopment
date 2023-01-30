package com.simley.ndk_day78;

import android.app.Application;
import android.content.Context;

import com.tencent.bugly.crashreport.CrashReport;

public class App extends Application {

    static {
        System.loadLibrary("ndk_day78");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CrashReport.initCrashReport(getApplicationContext(), "5c157f40e9", false);
//        CrashReport.testJavaCrash();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}
