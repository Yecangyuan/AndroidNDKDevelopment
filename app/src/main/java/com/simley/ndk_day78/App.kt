package com.simley.ndk_day78

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.multidex.MultiDexApplication
import com.simley.ndk_day78.utils.ActivityManager
import com.simley.ndk_day78.utils.SignCheck
import com.tencent.bugly.crashreport.CrashReport

/**
 * 当dex文件中的方法数超过65535个的时候，就需要将dex文件分成多个
 */
class App : MultiDexApplication(), Application.ActivityLifecycleCallbacks {

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

        // 校验签名，java和native层双重验签
        // 堪比情比金坚锁 过滤部分逆向小白的破解行为
        // 想破解我？哼
        // 偷偷告诉你哦：在.so文件中修改破解哦
        // 1. 在Java层验证签名
        val signCheck = SignCheck(this, "")
        signCheck.javaCheckSign()
        // 2. 在Native层验证签名
        signCheck.nativeCheckSign(this, packageName)

        registerActivityLifecycleCallbacks(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        ActivityManager
    }

    override fun onActivityStarted(p0: Activity) {
    }

    override fun onActivityResumed(p0: Activity) {
    }

    override fun onActivityPaused(p0: Activity) {
    }

    override fun onActivityStopped(p0: Activity) {
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    }

    override fun onActivityDestroyed(p0: Activity) {
    }

}