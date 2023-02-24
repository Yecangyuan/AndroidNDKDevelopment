package com.simley.lib_serialport.bean

import java.io.File
import java.io.Serializable

/**
 * 同学们：这个是串口文件设备描述的JavaBean Device
 * 1|root@android:/dev # ls ttyS 【目前一共提供的串口文件如下】
 * ttyS0 ttyS1 ttyS2 ttyS3
 */
class Device constructor(var name: String, var root: String, private var file: File) :
    Serializable {

    fun getFile(): File {
        return file
    }

    fun setFile(file: File) {
        this.file = file
    }

    override fun toString(): String {
        return "Device{" +
                "name='" + name + '\'' +
                ", root='" + root + '\'' +
                ", file=" + file +
                '}'
    }
}