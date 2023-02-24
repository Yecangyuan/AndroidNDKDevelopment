package com.simley.lib_serialport.listener;

/**
 * 同学们：串口消息监听
 */
public interface OnSerialPortDataListener {

    /**
     * 开启接收消息线程startReadThread - 调用 - 数据接收
     * @param bytes 接收到的数据
     */
    void onDataReceived(byte[] bytes);

    /**
     * 开启发生消息线程startSendThread - 调用 - 数据发送
     * @param bytes 发送的数据
     */
    void onDataSent(byte[] bytes);
}
