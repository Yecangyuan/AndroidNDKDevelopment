package com.simley.ndk_day78.serialport.bean;

import java.io.File;
import java.io.Serializable;

/**
 * 同学们：这个是串口文件设备描述的JavaBean Device
 * 1|root@android:/dev # ls ttyS 【目前一共提供的串口文件如下】
 * ttyS0 ttyS1 ttyS2 ttyS3
 */
public class Device implements Serializable {

    private static final String TAG = Device.class.getSimpleName();

    private String name;
    private String root;
    private File file;

    public Device(String name, String root, File file) {
        this.name = name;
        this.root = root;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File path) {
        this.file = file;
    }

    @Override
    public String toString() {
        return "Device{" +
                "name='" + name + '\'' +
                ", root='" + root + '\'' +
                ", file=" + file +
                '}';
    }
}
