package com.simley.ndk_day78;

import android.app.Application;
import android.content.Context;

import com.tencent.bugly.crashreport.CrashReport;

public class App extends Application {

    static {
        /*
            extern "C"
            JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) { }

            当在Java层调用loadLibrary()方法的时候会调用到JNI层的JNI_OnLoad()函数
         */
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
