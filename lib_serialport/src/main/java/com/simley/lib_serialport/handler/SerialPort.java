package com.simley.lib_serialport.handler;

import android.util.Log;

import java.io.FileDescriptor;

public class SerialPort {

    private static final String TAG = SerialPort.class.getSimpleName();

    protected String port = "/dev/ttyS1";
    protected int baudRate = 9600;
    protected int stopBits = 1;
    protected int dataBits = 8;
    protected int parity = 0;
    protected int flowCon = 0;
    protected int flags = 0;
    protected boolean isOpen = false;
    protected byte[] bLoopData = {48};
    protected int delay = 500;

    public SerialPort(String port, int baudRate) {
        this.port = port;
        this.baudRate = baudRate;
    }

    /**
     * 通过串口的波特率等信息，构建Java对象-FileDescriptor
     *
     * @param path     串口文件的路径
     * @param baudRate 波特率（相当于：Android系统设备层 与 硬件层 通讯所共识的频率）
     * @param stopBits 停止位
     * @param dataBits 数据位
     * @param parity   奇偶位
     * @param flowCon  流控
     * @param flags    读写标志
     * @return FileDescriptor 文件读写流（文件句柄） InputStream/OutputStream=串口 发/收
     */
    protected native FileDescriptor openNative(String path, int baudRate, int stopBits, int dataBits, int parity, int flowCon, int flags); // 打开串口-native函数

    /**
     * 关闭串口
     */
    protected native void closeNative();

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        if (isOpen) {
            Log.e(TAG, "由于该串口已经打开，设置串口设备号失败，需要关闭后再设置");
            return;
        }
        this.port = port;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(int baudRate) {
        if (isOpen) {
            Log.e(TAG, "由于该串口已经打开，设置波特率失败，需要关闭后再设置");
            return;
        }
        this.baudRate = baudRate;
    }

    public int getStopBits() {
        return stopBits;
    }

    public void setStopBits(int stopBits) {
        if (isOpen) {
            Log.e(TAG, "由于该串口已经打开，设置停止位失败，需要关闭后再设置");
            return;
        }
        this.stopBits = stopBits;
    }

    public int getDataBits() {
        return dataBits;
    }

    public void setDataBits(int dataBits) {
        if (isOpen) {
            Log.e(TAG, "由于该串口已经打开，设置数据位失败，需要关闭后再设置");
            return;
        }
        this.dataBits = dataBits;
    }

    public int getParity() {
        return parity;
    }

    public void setParity(int parity) {
        if (isOpen) {
            Log.e(TAG, "由于该串口已经打开，设置奇偶位失败，需要关闭后再设置");
            return;
        }
        this.parity = parity;
    }

    public int getFlowCon() {
        return flowCon;
    }

    public void setFlowCon(int flowCon) {
        if (isOpen) {
            Log.e(TAG, "由于该串口已经打开，设置流控失败，需要关闭后再设置");
            return;
        }
        this.flowCon = flowCon;
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public byte[] getLoopData() {
        return bLoopData;
    }

    public void setLoopData(byte[] bLoopData) {
        this.bLoopData = bLoopData;
    }

    public void setTxtLoopData(String sTxt) {
        this.bLoopData = sTxt.getBytes();
    }

    public void setHexLoopData(String sHex) {
//        this.bLoopData = ByteUtil.HexToByteArr(sHex);
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    static {
        System.loadLibrary("SerialPort");
    }
}
