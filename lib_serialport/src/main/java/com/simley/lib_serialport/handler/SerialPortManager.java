package com.simley.lib_serialport.handler;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.simley.lib_serialport.bean.T;
import com.simley.lib_serialport.listener.OnOpenSerialPortListener;
import com.simley.lib_serialport.listener.OnSerialPortDataListener;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * SerialPortManager 串口大管家 - 核心
 * 打开串口 - openSerialPort(File device, int baudRate) 串口设备文件，波特率
 * 关闭串口 - closeSerialPort()
 * 添加打开串口监听 - setOnOpenSerialPortListener(打开串口成功，打开串口失败)
 * 添加数据通信监听 - setOnSerialPortDataListener(发送串口数据，接收串口数据)
 * 发送串口数据 - sendBytes(byte[] sendBytes)
 */
public class SerialPortManager extends SerialPort {

    // private static final String TAG = SerialPortManager.class.getSimpleName();
    private FileInputStream mFileInputStream; // 读取的流 绑定了 (mFd文件句柄)
    private FileOutputStream mFileOutputStream; // 写入的流 绑定了 (mFd文件句柄)
    private FileDescriptor mFd; // 文件句柄，对应native的fd
    private OnOpenSerialPortListener mOnOpenSerialPortListener; // 打开串口的监听（打开成功，打开失败）
    private OnSerialPortDataListener mOnSerialPortDataListener; // 串口消息的监听（读取串口消息，写入串口消息）

    private HandlerThread mSendingHandlerThread; // 开启发送消息的线程
    private Handler mSendingHandler; // 发送串口数据的Handler
    private SerialPortReadThread mSerialPortReadThread; // 接收消息的线程


    /**
     * 打开串口
     *
     * @param device   串口设备文件-串口文件的路径
     * @param baudRate 波特率-（相当于：Android系统设备层 与 硬件层 通讯所共识的频率）
     * @return 打开串口是否成功
     */
    public boolean openSerialPort(File device, int baudRate) {
        // 打开串口 /dev/ttyS0  波特率 9600 【1】
        Log.i(T.TAG, "SerialPortManager openSerialPort: " + String.format("打开串口 %s  波特率 %s", device.getPath(), baudRate));

        // 校验串口权限
        if (!device.canRead() || !device.canWrite()) {
            boolean hasChanged = chmod777(device);
            if (!hasChanged) {
                Log.i(T.TAG, "SerialPortManager openSerialPort: 没有读写权限");
                if (null != mOnOpenSerialPortListener) {
                    mOnOpenSerialPortListener.onFail(device, OnOpenSerialPortListener.Status.NO_READ_WRITE_PERMISSION);
                }
                return false;
            }
        }
        try {
            mFd = openNative(device.getAbsolutePath(), baudRate, 0);// 打开串口-native函数
            mFileInputStream = new FileInputStream(mFd); // 读取的流 绑定了 (mFd文件句柄)-通过文件句柄(mFd)包装出 输入流
            mFileOutputStream = new FileOutputStream(mFd); // 写入的流 绑定了 (mFd文件句柄)-通过文件句柄(mFd)包装出 输出流
            Log.i(T.TAG, "SerialPortManager openSerialPort: 串口已经打开 " + mFd); // 串口已经打开 FileDescriptor[35] 【2】    }

            if (null != mOnOpenSerialPortListener) {
                mOnOpenSerialPortListener.onSuccess(device);
            }
            startSendThread(); // 开启发送消息的线程
            startReadThread(); // 开启接收消息的线程
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (null != mOnOpenSerialPortListener) {
                mOnOpenSerialPortListener.onFail(device, OnOpenSerialPortListener.Status.OPEN_FAIL);
            }
        }
        return false;
    }

    /**
     * 打开串口 - 调用 - 开启接收消息的线程
     */
    private void startReadThread() {
        mSerialPortReadThread = new SerialPortReadThread(mFileInputStream) {
            @Override
            public void onDataReceived(byte[] bytes) {
                if (null != mOnSerialPortDataListener) {
                    mOnSerialPortDataListener.onDataReceived(bytes);
                }
            }
        };
        mSerialPortReadThread.start();
    }

    /**
     * 关闭串口 - 调用 - 停止接收消息的线程
     */
    private void stopReadThread() {
        if (null != mSerialPortReadThread) {
            mSerialPortReadThread.release();
        }
    }

    /**
     * 布局上的按钮点击事件 - 调用 - 发送串口数据
     *
     * @param sendBytes 发送数据
     * @return 发送是否成功
     */
    public boolean sendBytes(byte[] sendBytes) {
        if (null != mFd && null != mFileInputStream && null != mFileOutputStream) {
            if (null != mSendingHandler) {
                Message message = Message.obtain();
                message.obj = sendBytes;
                return mSendingHandler.sendMessage(message);
            }
        }
        return false;
    }

    /**
     * 添加打开串口监听
     *
     * @param listener listener
     * @return SerialPortManager
     */
    public SerialPortManager setOnOpenSerialPortListener(OnOpenSerialPortListener listener) {
        mOnOpenSerialPortListener = listener;
        return this;
    }

    /**
     * 添加数据通信监听
     *
     * @param listener listener
     * @return SerialPortManager
     */
    public SerialPortManager setOnSerialPortDataListener(OnSerialPortDataListener listener) {
        mOnSerialPortDataListener = listener;
        return this;
    }

    /**
     * 关闭串口 - 调用 - 停止发送消息线程
     */
    private void stopSendThread() {
        mSendingHandler = null;
        if (null != mSendingHandlerThread) {
            mSendingHandlerThread.interrupt(); // 先中断
            mSendingHandlerThread.quit(); // 并退出
            mSendingHandlerThread = null; // 后置为null
        }
    }

    /**
     * 打开串口 - 调用 - 开启发送消息的线程
     */
    private void startSendThread() {
        // 开启发送消息的线程
        mSendingHandlerThread = new HandlerThread("mSendingHandlerThread");
        mSendingHandlerThread.start();
        // Handler
        mSendingHandler = new Handler(mSendingHandler.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                byte[] sendBytes = (byte[]) msg.obj;

                if (null != mFileOutputStream && null != sendBytes && 0 < sendBytes.length) {
                    try {
                        mFileOutputStream.write(sendBytes);
                        if (null != mOnSerialPortDataListener) {
                            mOnSerialPortDataListener.onDataSent(sendBytes); // 【发送 1】
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        if (null != mFd) {
            closeNative(); // 关闭串口-native函数
            mFd = null;
        }
        stopSendThread(); // 停止发送消息的线程
        stopReadThread(); // 停止接收消息的线程

        if (null != mFileInputStream) {
            try {
                mFileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mFileInputStream = null;
        }

        if (null != mFileOutputStream) {
            try {
                mFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mFileOutputStream = null;
        }
        mOnOpenSerialPortListener = null;
        mOnSerialPortDataListener = null;
    }

}
