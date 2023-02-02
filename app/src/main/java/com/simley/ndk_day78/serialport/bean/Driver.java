package com.simley.ndk_day78.serialport.bean;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;

/**
 * 同学们：获取Android系统设备中的Driver集，getDevices ArrayList<File>
 * 注意：每一个driver就是下面的一行信息
 * root@android:/proc/tty # cat drivers 【查看串口清单文件内容】
 * /dev/tty             /dev/tty        5       0 system:/dev/tty
 * /dev/console         /dev/console    5       1 system:console
 * /dev/ptmx            /dev/ptmx       5       2 system
 * /dev/vc/0            /dev/vc/0       4       0 system:vtmaster
 * serial               /dev/ttyS       4 64-67 serial  【一般跳线开发都是这个，否则vga转usb就是:ttySAC(ttySAC1,ttySAC2,ttySAC3 等等)】
 * pty_slave            /dev/pts      136 0-1048575 pty:slave
 * pty_master           /dev/ptm      128 0-1048575 pty:master
 * unknown              /dev/tty        4 1-63 console
 */
public class Driver {

    private static final String TAG = Driver.class.getSimpleName();

    private final String mDriverName;
    private final String mDeviceRoot;

    public Driver(String name, String root) {
        mDriverName = name;
        mDeviceRoot = root;
    }

    public ArrayList<File> getDevices() {
        ArrayList<File> devices = new ArrayList<>();
        File dev = new File("/dev");

        if (!dev.exists()) {
            Log.i(TAG, "getDevices: " + dev.getAbsolutePath() + " 不存在");
            return devices;
        }
        if (!dev.canRead()) {
            Log.i(TAG, "getDevices: " + dev.getAbsolutePath() + " 没有读取权限");
            return devices;
        }

        File[] files = dev.listFiles();
        if (files == null || files.length == 0) {
            return devices;
        }

        int i;
        for (i = 0; i < files.length; i++) {
            if (files[i].getAbsolutePath().startsWith(mDeviceRoot)) {
                Log.d(TAG, "Found new device: " + files[i]);
                devices.add(files[i]);
            }
        }
        return devices;
    }

    public String getName() {
        return mDriverName;
    }
}
