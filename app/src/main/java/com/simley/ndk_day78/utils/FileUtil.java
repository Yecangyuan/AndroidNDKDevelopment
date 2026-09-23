package com.simley.ndk_day78.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RawRes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件工具类
 */
public final class FileUtil {

    private static final String TAG = "FileUtil";

    // 写一个将bitmap拷贝到sd卡下的方法
    public static void copyRawFileToSDCard(Bitmap bitmap, String dest, String fileName) {
        // 在sd卡创建文件夹


        File dir = new File("/storage/emulated/0" + dest);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, fileName);
        if (file.exists()) return;
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
        } catch (IOException ignored) {
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param context 上下文
     * @param dest    保存的目录位置
     * @return 最终文件保存的绝对路径
     */
    public static String copyRawFileToSDCard(Context context, String dest, String fileName, @RawRes int resId) {
        InputStream is = context.getResources().openRawResource(resId);
        File dir = context.getDir(dest, Context.MODE_PRIVATE);
        File file = new File(dir, fileName);
        if (file.exists()) return file.getAbsolutePath();
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException ignored) {
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file.getAbsolutePath();
    }

    /**
     * 把 assets 中的文件拷贝到指定目标文件，并自动创建父目录。
     * <p>
     * 与 {@link #copyAssets2SDCard} 的区别：失败时抛出 IOException 而不是静默吞掉，
     * 便于调用方感知并给出提示。目标落在应用私有目录时可免权限使用。
     *
     * @param context 上下文
     * @param src     assets 下的源文件名
     * @param dest    目标文件（父目录不存在会被自动创建）
     */
    public static void copyAsset(Context context, String src, File dest) throws IOException {
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("创建目录失败: " + parent.getAbsolutePath());
        }
        InputStream is = null;
        FileOutputStream os = null;
        try {
            is = context.getAssets().open(src);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
    }

    /**
     * 把 assets 目录里面的文件 Copy 到 SDCard目录下
     * <p>
     * 注意：Android 10（API 29）起启用了分区存储（scoped storage），当 targetSdk >= 30 时
     * 应用无法再向共享存储的任意路径写入（即使 WRITE_EXTERNAL_STORAGE 已被授予）。
     * 需要可靠读写时请改用应用私有目录（context.getFilesDir() / getExternalFilesDir()）。
     *
     * @param context 上下文
     * @param src     源文件名
     * @param dst     相对于共享存储根目录的目标路径
     */
    public static void copyAssets2SDCard(Context context, String src, String dst) {
        String sdCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File dest = new File(sdCardPath + File.separator + dst);
        if (dest.exists()) {
            return;
        }
        try {
            copyAsset(context, src, dest);
        } catch (IOException e) {
            // 不再静默吞掉：分区存储被拒时会抛 EACCES/ENOENT，必须留下日志才可定位
            Log.e(TAG, "拷贝 assets/" + src + " 到 " + dest.getAbsolutePath() + " 失败", e);
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            Log.w(TAG, "关闭流失败", e);
        }
    }
}
