package com.simley.ndk_day78.opengl2.face;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.simley.ndk_day78.opengl2.utils.CameraHelper;

public class FaceTrack {


    private CameraHelper mCameraHelper; // 手机相机预览工具类（之前的内容）
    private Handler mHandler; // 此Handler方便开启一个线程
    private HandlerThread mHandlerThread; // 此HandlerThread方便开启一个线程

    private long self; // FaceTrack.cpp对象的地址指向long值
    private Face mFace; // 最终人脸跟踪的结果


    /**
     * @param model        OpenCV人脸的模型的文件路径
     * @param seeta        中科院的那个模型（五个关键点的特征点的文件路径）
     * @param cameraHelper 需要把CameraID传递给C++层
     */
    public FaceTrack(String model, String seeta, CameraHelper cameraHelper) {
        mCameraHelper = cameraHelper;
        self = native_create(model, seeta); // 传入人脸检测模型到C++层处理，返回FaceTrack.cpp的地址指向

        // 开启一个线程：去执行 人脸检测定位
        mHandlerThread = new HandlerThread("FaceTrack");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                // 子线程 耗时再久 也不会对其他地方 (如：opengl绘制线程) 产生影响
                synchronized (FaceTrack.this) {
                    // 定位 线程中检测
                    mFace = native_detector(self, (byte[]) msg.obj, mCameraHelper.getCameraID(), 800, 480);
                    if (mFace != null) {
                        Log.e("拍摄了人脸mFace.toString:", mFace.toString()); // 看看打印效果
                        // Face{landmarks=[10.0, 251.0], width=356, height=356, imgWidth=480, imgHeight=800}
                        // Face{landmarks=[24.0, 234.0], width=350, height=350, imgWidth=480, imgHeight=800}
                        // Face{landmarks=[35.0, 221.0], width=354, height=354, imgWidth=480, imgHeight=800}
                        // 人脸宽度 和 人脸高度 基本上都是正方形，   送去检测的宽/高 就是屏幕的所有数据
                        //{landmarks=[人脸框x, 人框脸y], width=人脸宽度, height=人脸高度, imgWidth=送去检测的宽, imgHeight=送去检测的高}

                        // TODO 加入 人脸关键点定位的第二版后：
                        /*
                         Face{landmarks=[30.0, 381.0, 169.14955, 511.92078, 331.29724, 540.34656, 281.4803, 639.0843, 191.44511, 711.19543, 297.10767, 733.30853], width=415, height=415, imgWidth=480, imgHeight=800}
                         Face{landmarks=[30.0, 381.0, 166.85965, 489.3989, 363.05258, 519.52637, 293.39655, 622.8394, 155.98112, 721.06415, 293.46216, 739.6296], width=415, height=415, imgWidth=480, imgHeight=800}
                         Face{landmarks=[30.0, 381.0, 189.70148, 487.68216, 375.04114, 476.3848, 331.1995, 576.4273, 215.51007, 691.9523, 373.76227, 675.8291], width=415, height=415, imgWidth=480, imgHeight=800}
                         */
                        // landmarks=[人脸框x, 人脸框y, 左眼x, 左眼y, 右眼x, 右眼y, 鼻尖x, 鼻尖y, 左边嘴角x, 左边嘴角y, 右边嘴角x, 右边嘴角y]
                        // width=415, height=415       人脸的框框 宽和高
                        // imgWidth=480, imgHeight=800 原始屏幕要送去检测的 宽和高
                    }
                }
            }
        };

    }

    public void startTrack() { // 启动跟踪器 OpenCV
        native_start(self);
    }

    public void stopTrack() { // 停止跟踪器 OpenCV
        synchronized (this) {
            mHandlerThread.quitSafely();
            mHandler.removeCallbacksAndMessages(null);
            native_stop(self);
            self = 0;
        }
    }
    // byte[] data == NV21 Camera的数据 byte[]
    public void detector(byte[] data) { // 要把相机的数据，给C++层做人脸追踪
        // 把积压的 11号任务移除掉
        mHandler.removeMessages(11);
        // 加入新的11号任务
        Message message = mHandler.obtainMessage(11);
        message.obj = data;
        mHandler.sendMessage(message);
    }
    public Face getFace() { // 这个函数很重要
        return mFace; // 如果能拿到mFace，就证明 有人脸最终信息 和 5个关键点信息
    }
    /**
     * 传入人脸检测模型到C++层处理
     * @param model OpenCV人脸模型
     * @param seeta Seeta中科院的人脸关键点模型
     * @return FaceTrack.cpp地址指向long值
     */
    private native long native_create(String model, String seeta);

    private native void native_start(long self); // 开始追踪

    private native void native_stop(long self); // 停止追踪

    /**
     * 执行真正的人脸探测工作
     * @param self     Face.java对象的地址指向long值
     * @param data     Camera相机 byte[] data NV21摄像头的数据
     * @param cameraId Camera相机ID，前置摄像头，后置摄像头
     * @param width    宽度
     * @param height   高度
     * @return         若Face==null：代表没有人脸信息+人脸5特征，  若Face有值：人脸框x/y，+ 5个特侦点（本次只需要 人脸框x/y + 双眼关键点）
     */
    private native Face native_detector(long self, byte[] data, int cameraId, int width, int height);
}
