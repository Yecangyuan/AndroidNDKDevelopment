package com.simley.lib_serialport.handler;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

public class SerialPort {

//    static {
//        System.loadLibrary("SerialPort");
//    }

    private static final String TAG = SerialPort.class.getSimpleName();

    /**
     * 文件设置最高权限 777 可读 可写 可执行
     * @param  file 你要对那个文件，获取root权限
     * @return 权限修改是否成功- 返回：成功 与 失败 结果
     */
    boolean chmod777(File file) {
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

    /**
     * 通过串口的波特率等信息，构建Java对象-FileDescriptor
     * @param path     串口文件的路径
     * @param baudRate 波特率（相当于：Android系统设备层 与 硬件层 通讯所共识的频率）
     * @return FileDescriptor 文件读写流（文件句柄） InputStream/OutputStream=串口 发/收
     */
    protected native FileDescriptor openNative(String path, int baudRate, int flags); // 打开串口-native函数

    protected native void closeNative(); // 关闭串口-native函数
}
