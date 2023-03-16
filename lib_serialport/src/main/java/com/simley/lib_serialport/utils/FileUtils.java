package com.simley.lib_serialport.utils;

import java.io.File;
import java.io.IOException;

public final class FileUtils {
    /**
     * 文件设置最高权限 777 可读 可写 可执行
     *
     * @param file 你要对那个文件，获取root权限
     * @return 权限修改是否成功- 返回：成功 与 失败 结果
     */
    public static boolean chmod777(File file) {
        if (null == file || !file.exists()) {
            // 文件不存在
            return false;
        }
        try {
            // 获取ROOT权限
            Process su = Runtime.getRuntime().exec("/system/bin/su");
            // 修改文件属性为 [可读 可写 可执行]
            String cmd = "chmod 777 " + file.getAbsolutePath() + "\n" + "exit\n";
            su.getOutputStream().write(cmd.getBytes());
            if (0 == su.waitFor() && file.canRead() && file.canWrite() && file.canExecute()) {
                return true;
            }
        } catch (IOException | InterruptedException e) {
            // 没有ROOT权限
            e.printStackTrace();
        }
        return false;
    }

}
