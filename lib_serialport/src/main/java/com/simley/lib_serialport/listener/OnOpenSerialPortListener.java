package com.simley.lib_serialport.listener;

import java.io.File;

/**
 * 同学们：打开串口监听
 */
public interface OnOpenSerialPortListener {

    /**
     * 打开串口 openSerialPort 调用的当前此函数-打开串口成功
     * @param device 串口文件
     */
    void onSuccess(File device);

    /**
     * 打开串口 openSerialPort 调用的当前此函数-打开串口失败
     * @param device 串口文件
     * @param status 失败：状态
     */
    void onFail(File device, Status status);

    enum Status {
        NO_READ_WRITE_PERMISSION, // 无读写权限
        OPEN_FAIL // 打开失败
    }
}
