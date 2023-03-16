package com.simley.lib_serialport.bean

import android.util.Log
import java.io.File

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
</File> */
class Driver constructor(val name: String, private val mDeviceRoot: String) {

    private val TAG = Driver::class.java.simpleName

    val devices: List<File>
        get() {
            val devices: MutableList<File> = ArrayList()
            val dev = File("/dev")
            if (!dev.exists()) {
                Log.i(TAG, "getDevices: " + dev.absolutePath + " 不存在")
                return devices
            }
            if (!dev.canRead()) {
                Log.i(TAG, "getDevices: " + dev.absolutePath + " 没有读取权限")
                return devices
            }
            val files = dev.listFiles()
            if (files == null || files.isEmpty()) {
                return devices
            }
            for (file in files) {
                if (file.absolutePath.startsWith(mDeviceRoot)) {
                    devices.add(file)
                }
                Log.d(TAG, "Found new device: $file")
            }
            return devices
        }

}