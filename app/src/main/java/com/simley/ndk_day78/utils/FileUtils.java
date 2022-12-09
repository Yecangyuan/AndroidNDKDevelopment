package com.simley.ndk_day78.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {


//    public static void copyAssetsToSdcard(String[] fileNames, String oldPath, String newPath) {
//        try {
//            if (fileNames.length > 0) {// 如果是目录
//                File file = new File(newPath);
//                file.mkdirs();// 如果文件夹不存在，则递归
//                for (String fileName : fileNames) {
//                    copyAssetsToSdcard(oldPath + "/" + fileName, newPath + "/" + fileName);
//                }
//            } else {// 如果是文件
//
//                FileOutputStream fos = new FileOutputStream(new File(newPath));
//                byte[] buffer = new byte[1024];
//                int byteCount = 0;
//                while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
//                    // buffer字节
//                    fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
//                }
//                fos.flush();// 刷新缓冲区
//                is.close();
//                fos.close();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

}
