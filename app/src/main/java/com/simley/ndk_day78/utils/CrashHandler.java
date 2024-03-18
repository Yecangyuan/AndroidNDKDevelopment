package com.simley.ndk_day78.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * 异常处理类
 * <p>
 * 这是一个用来处理未捕获异常的处理类
 */
public final class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String FILE_NAME_SUFFIX = ".trace";
    private static final String FORMATTER_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static Thread.UncaughtExceptionHandler mDefaultCrashHandler;
    private static Context mContext;

    private CrashHandler() {
    }

    /**
     * 初始化
     *
     * @param context
     */
    public static void init(Context context) {
        mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
        mContext = context.getApplicationContext();
    }

    /**
     * 当程序中有未捕获的异常，系统将调用这个方法
     * <p>
     * 处理未捕获的异常
     *
     * @param t the thread
     * @param e the exception
     */
    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        try {
            // 自行处理：保存本地
            File file = handleException(t, e);
            Log.d("CrashHandler", "crash file location: " + file.getAbsolutePath());
            // 或者上传到服务器
            uploadException();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (mDefaultCrashHandler != null) {
                mDefaultCrashHandler.uncaughtException(t, e);
            }
        }
    }

    /**
     * 将异常上传到服务器
     */
    private void uploadException() {

    }

    /**
     * 具体处理异常
     *
     * @return
     * @throws Exception
     */
    private File handleException(Thread t, Throwable e) throws Exception {
        String time = new SimpleDateFormat(FORMATTER_PATTERN).format(new Date());

        File f = new File(mContext.getExternalCacheDir().getAbsoluteFile(), "crash_info");
        if (!f.exists()) {
            f.mkdirs();
        }

        File crashFile = new File(f, time + FILE_NAME_SUFFIX);
        File file = new File(crashFile + File.separator + time + FILE_NAME_SUFFIX);

        // 往文件中写入数据
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        pw.println(time);
        pw.println("Thread: " + t.getName());
        pw.println(getPhoneInfo());
        e.printStackTrace(pw);

        // 写入crash堆栈
        pw.close();
        return file;
    }

    private String getPhoneInfo() throws PackageManager.NameNotFoundException {
        PackageManager pm = mContext.getPackageManager();
        PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
        StringBuilder sb = new StringBuilder();

        // App版本
        sb.append("App Version: ")
                .append(pi.versionName)
                .append("_")
                .append(pi.versionCode).append("\n");

        // Android版本号
        sb.append("OS Version: ")
                .append(Build.VERSION.RELEASE)
                .append("_")
                .append(Build.VERSION.SDK_INT)
                .append("\n");

        // 手机制造商
        sb.append("Vendor: ")
                .append(Build.MANUFACTURER)
                .append("\n");

        // 手机型号
        sb.append("Model: ")
                .append(Build.MODEL)
                .append("\n");

        // CPU架构
        sb.append("CPU: ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sb.append(Arrays.toString(Build.SUPPORTED_ABIS));
        } else {
            sb.append(Build.CPU_ABI);
        }
        return sb.toString();
    }
}
