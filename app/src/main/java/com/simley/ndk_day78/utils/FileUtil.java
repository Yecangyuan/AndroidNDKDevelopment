package com.simley.ndk_day78.utils;

import android.content.Context;

import androidx.annotation.RawRes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 文件工具类
 */
public class FileUtil {

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
        try {
            File file = new File(dst);
            if (!file.exists()) {
//                boolean mkdirs = file.mkdirs();
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
