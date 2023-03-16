package com.simley.ndk_day78

import android.content.Context
import androidx.multidex.MultiDexApplication
import com.tencent.bugly.crashreport.CrashReport

/**
 * 当dex文件中的方法数超过65535个的时候，就需要将dex文件分成多个
 */
class App : MultiDexApplication() {

    init {
        /*
        extern "C"
        JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) { }

        当在Java层调用loadLibrary()方法的时候会调用到JNI层的JNI_OnLoad()函数
         */
        System.loadLibrary("ndk_day78")
    }

    override fun onCreate() {
        super.onCreate()
        CrashReport.initCrashReport(applicationContext, "5c157f40e9", false)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

}