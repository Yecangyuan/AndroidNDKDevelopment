package com.simley.ndk_day78.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import androidx.annotation.RawRes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件工具类
 */
public final class FileUtil {

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
     * 把 assets目录里面的文件 Copy 到 SDCard目录下
     *
     * @param context 上下文
     * @param src     源文件名
     * @param dst     目标文件路径
     */
    public static void copyAssets2SDCard(Context context, String src, String dst) {
        // 使用正确的方法获取SD卡路径
        String sdCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String dstPath = sdCardPath + File.separator + dst;

        try {
            File file = new File(dstPath);
            if (!file.exists()) {
                InputStream is = context.getAssets().open(src);
                FileOutputStream fos = new FileOutputStream(file);
                int len;
                byte[] buffer = new byte[2048];
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
