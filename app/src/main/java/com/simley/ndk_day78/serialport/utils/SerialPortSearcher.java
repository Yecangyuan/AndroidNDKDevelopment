package com.simley.ndk_day78.serialport.utils;

import android.util.Log;

import com.simley.ndk_day78.serialport.bean.Device;
import com.simley.ndk_day78.serialport.bean.Driver;
import com.simley.ndk_day78.serialport.bean.T;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

public class SerialPortSearcher {

    // private static final String TAG = SerialPortFinder.class.getSimpleName();
    private static final String DRIVERS_PATH = "/proc/tty/drivers"; // drivers == 串口配置清单文件
    private static final String SERIAL_FIELD = "serial"; // 在串口配置清单文件里面的内容中，包含serial，才能代表串口

    public SerialPortSearcher() {
        File file = new File(DRIVERS_PATH);
        boolean b = file.canRead();
        Log.i(T.TAG, "SerialPortFinder: file.canRead() = " + b);
    }

    /**
     * 获取 Drivers
     *
     * @return Drivers == /proc/tty/drivers
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
    private List<Driver> getDrivers() throws IOException {
        List<Driver> drivers = new ArrayList<>();
        LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(DRIVERS_PATH));

        String readLine;
        while ((readLine = lineNumberReader.readLine()) != null) {
            String driverName = readLine.substring(0, 0x15).trim();
            String[] fields = readLine.split(" +");


            // driverName:/dev/tty
            // driverName:/dev/console
            // driverName:/dev/ptmx
            // driverName:/dev/vc/0
            // driverName:serial
            // driverName:pty_slave
            // driverName:pty_master
            // driverName:unknown

            Log.d(T.TAG, "SerialPortFinder getDrivers() driverName:" + driverName /*+ " readLine:" + readLine*/);
            if ((fields.length >= 5) && (fields[fields.length - 1].equals(SERIAL_FIELD))) { // 判断第四个等不等于serial
                // 找到了新串口驱动是:serial 此串口系列名是:/dev/ttyS
                Log.d(T.TAG, "SerialPortFinder getDrivers() 找到了新串口驱动是:" + driverName + " 此串口系列名是:" + fields[fields.length - 4]);
                drivers.add(new Driver(driverName, fields[fields.length - 4]));
            }
        }
        return drivers;
    }
    /**
     * 获取所有的串口
     * @return 串口
    1|root@android:/dev # ls ttyS 【目前一共提供的串口文件如下】
    ttyS0 ttyS1 ttyS2 ttyS3

    如果是真实的串口开发版，例如：迅维串口平板开发板
    // dev/ttySAC1，dev/ttySAC2，dev/ttySAC3，dev/ttySAC3，dev/ttySAC4，dev/ttySAC5，dev/ttySAC6
    // 迅维的平板串口插入电脑（平台的第二个VGA口，所以等于 == ttySAC2）
     */
    public List<Device> getDevices() {
        List<Device> devices = new ArrayList<>();
        try {
            for (Driver driver : getDrivers()) {
                String driverName = driver.getName();
                List<File> driverDevices = driver.getDevices();
                for (File file : driverDevices) {
                    String devicesName = file.getName();
                    devices.add(new Device(devicesName, driverName, file));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return devices;
    }

}
