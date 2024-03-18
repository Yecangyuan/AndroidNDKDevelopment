package com.simley.ndk_day78.opengl2.utils;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.util.Iterator;
import java.util.List;

// 音频 视频 RTMP 推流 的代码，直接Copy过来的，留了个作业：数据旋转
// TODO 同学们，这个Camera是Android的基础，所以节约大家时间，就提前写好了
public class CameraHelper implements Camera.PreviewCallback, SurfaceHolder.Callback {
    private static final String TAG = "CameraHelper";
    public int mCameraID; // 后摄 前摄像头
    public int mWidth;  // 宽
    public int mHeight; // 高
    private byte[] cameraBuffer; // 数据
    private byte[] cameraBuffer_; // 数据 操作旋转 // TODO 新增点 音视频推流 旋转作业

    private final Activity mActivity; // 此Camera需要用到Activity
    private SurfaceHolder mSurfaceHolder; // Surface画面的帮助
    private Camera mCamera; // Camera 1 预览采集图像数据

    private Camera.PreviewCallback mPreviewCallback;  // 后面预览的画面，把此预览的画面 的数据回调
    private OnChangedSizeListener mOnChangedSizeListener;  // 你的宽和高发生改变，就会回调此接口
    private int mRotation; // 增加的旋转标记 // TODO 新增点 音视频推流 旋转作业
    private SurfaceTexture mSurfaceTexture; // 从图像流中捕获帧作为 OpenGL ES 纹理 // TODO 新增点 重要 纹理对象

    // 构造 必须传递基本上参数
    public CameraHelper(Activity activity, int cameraId, int width, int height) {
        mActivity = activity;
        mCameraID = cameraId;
        mWidth = width;
        mHeight = height;
    }

    /**
     * 与Surface绑定 == surfaceView.getHolder()
     *
     * @param surfaceHolder
     */
    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
        mSurfaceHolder.addCallback(this);
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK) {
            mCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            mCameraID = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview(); // 先停止预览
        startPreview(mSurfaceTexture); // 在开启预览
    }

    /**
     * 开始预览
     */
    public void startPreview(SurfaceTexture surfaceTexture) {
        mSurfaceTexture = surfaceTexture; // TODO 新增点 重点
        try {
            // 获得camera对象
            mCamera = Camera.open(mCameraID);
            // 配置camera的属性
            Camera.Parameters parameters = mCamera.getParameters();
            // 设置预览数据格式为nv21
            parameters.setPreviewFormat(ImageFormat.NV21);
            // 这是摄像头宽、高
            setPreviewSize(parameters);
            // 设置摄像头 图像传感器的角度、方向
            setPreviewOrientation(parameters);
            mCamera.setParameters(parameters);
            cameraBuffer = new byte[mWidth * mHeight * 3 / 2];  // 请看之前讲过的的细节
            cameraBuffer_ = new byte[mWidth * mHeight * 3 / 2]; // 请看之前讲过的的细节
            // 数据缓存区
            mCamera.addCallbackBuffer(cameraBuffer);
            mCamera.setPreviewCallbackWithBuffer(this);
            // 设置预览画面（之前的方式：把显示的画面，渲染到SurfaceView屏幕上即可）
            // mCamera.setPreviewDisplay(mSurfaceHolder); // SurfaceView 和 Camera绑定 ，以前音视频推流，就是这个干的

            // 设置预览画面（离屏渲染） surfaceTexture纹理画布（仅仅只是缓存一份画布，不可见的画布） 配合 OpenGL渲染操作
            // Camera相机预览数据 ---->  surfaceTexture纹理画布（不可见的画布） ---> OpenGL TODO 关键点 重点，绑定纹理
            mCamera.setPreviewTexture(surfaceTexture); // OpenGL无法直接访问到 Camera预览数据， 他只能访问 surfaceTexture

            /*if (mOnChangedSizeListener != null) { // 你的宽和高发生改变，就会回调此接口
                mOnChangedSizeListener.onChanged(mWidth, mHeight);
            }*/

            // 开启预览
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止预览
     */
    public void stopPreview() {
        if (mCamera != null) {
            // 预览数据回调接口
            mCamera.setPreviewCallback(null);
            // 停止预览
            mCamera.stopPreview();
            // 释放摄像头
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 在设置宽和高的同时，能够打印 支持的分辨率
     *
     * @param parameters
     */
    private void setPreviewSize(Camera.Parameters parameters) {
        // 获取摄像头支持的宽、高
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size size = supportedPreviewSizes.get(0);
        Log.d(TAG, "Camera支持: " + size.width + "x" + size.height);
        // 选择一个与设置的差距最小的支持分辨率
        int m = Math.abs(size.width * size.height - mWidth * mHeight);
        supportedPreviewSizes.remove(0);
        // 遍历
        for (Camera.Size next : supportedPreviewSizes) {
            Log.d(TAG, "支持 " + next.width + "x" + next.height);
            int n = Math.abs(next.height * next.width - mWidth * mHeight);
            if (n < m) {
                m = n;
                size = next;
            }
        }
        mWidth = size.width;
        mHeight = size.height;
        parameters.setPreviewSize(mWidth, mHeight);
        Log.d(TAG, "当前预览分辨率 width:" + mWidth + " height:" + mHeight);
    }

    /**
     * 旋转画面角度（因为默认预览是歪的，所以就需要旋转画面角度）
     * 这个只是画面的旋转，但是数据不会旋转，你还需要额外处理
     *
     * @param parameters
     */
    private void setPreviewOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraID, info);
        mRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (mRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                if (mOnChangedSizeListener != null) {
                    mOnChangedSizeListener.onChanged(mHeight, mWidth);
                }
                break;
            case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
                degrees = 90;
                if (mOnChangedSizeListener != null) {
                    mOnChangedSizeListener.onChanged(mWidth, mHeight);
                }
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                if (mOnChangedSizeListener != null) {
                    mOnChangedSizeListener.onChanged(mHeight, mWidth);
                }
                break;
            case Surface.ROTATION_270: // 横屏 头部在右边
                degrees = 270;
                if (mOnChangedSizeListener != null) {
                    mOnChangedSizeListener.onChanged(mWidth, mHeight);
                }
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        // 设置角度, 参考源码注释，从源码里面copy出来的，Google给出旋转的解释
        mCamera.setDisplayOrientation(result);
    }

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 下面就是SurfaceView 需要的 start

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 释放摄像头
        stopPreview();
        // 开启摄像头
        startPreview(mSurfaceTexture);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview(); // 只要画面不可见，就必须释放，因为预览耗电 耗资源
    }

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 下面就是SurfaceView 需要的 end

    /**
     * @param data   子集nv21 == YUV420类型的数据
     * @param camera
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // TODO 作业：data没有做旋转处理
        // 这个只是画面的旋转，但是数据不会旋转，你还需要额外处理
        // TODO 新增点 下面就是作业的完成：旋转数据90度
//        switch (mRotation) {
//            case Surface.ROTATION_0:
//                rotation90(data);
//                break;
//            case Surface.ROTATION_90: // 横屏 左边是头部(home键在右边)
//                break;
//            case Surface.ROTATION_270:// 横屏 头部在右边
//                break;
//        }

        // TODO 注意：你把上面的代码给注释掉了，意味着：data数据依然是颠倒的，再把颠倒的数据交给OpenCV内置函数旋转更简单
        if (mPreviewCallback != null) {
            // mPreviewCallback.onPreviewFrame(data, camera); // 以前的代码：byte[] data == nv21 ===> C++层 ---> 流媒体服务器
            mPreviewCallback.onPreviewFrame(cameraBuffer_, camera); // 现在的代码
        }
        camera.addCallbackBuffer(cameraBuffer);
    }

    /**
     * TODO 新增，把之前的作业给写了
     * 把nv21的数据进行旋转90度操作
     *
     * @param data Camera画面预览的原始数据
     */
    private void rotation90(byte[] data) {
        int index = 0;
        int ySize = mWidth * mHeight;
        // u和v
        int uvHeight = mHeight / 2;
        // 后置摄像头顺时针旋转90度
        if (mCameraID == Camera.CameraInfo.CAMERA_FACING_BACK) {
            //将y的数据旋转之后 放入新的byte数组
            for (int i = 0; i < mWidth; i++) {
                for (int j = mHeight - 1; j >= 0; j--) {
                    cameraBuffer_[index++] = data[mWidth * j + i];
                }
            }

            // 每次处理两个数据
            for (int i = 0; i < mWidth; i += 2) {
                for (int j = uvHeight - 1; j >= 0; j--) {
                    // v
                    cameraBuffer_[index++] = data[ySize + mWidth * j + i];
                    // u
                    cameraBuffer_[index++] = data[ySize + mWidth * j + i + 1];
                }
            }
        } else {
            // 逆时针旋转90度
            for (int i = 0; i < mWidth; i++) {
                for (int j = 0; j < mHeight; j++) {
                    cameraBuffer_[index++] = data[mWidth * j + mWidth - 1 - i];
                }
            }
            //  u v
            for (int i = 0; i < mWidth; i += 2) {
                for (int j = 0; j < uvHeight; j++) {
                    cameraBuffer_[index++] = data[ySize + mWidth * j + mWidth - 1 - i - 1];
                    cameraBuffer_[index++] = data[ySize + mWidth * j + mWidth - 1 - i];
                }
            }
        }
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) { // 此函数没有外界调用，代表外界没有使用到Camera预览的数据NV21
        mPreviewCallback = previewCallback;
    }

    public void setOnChangedSizeListener(OnChangedSizeListener listener) { // 此函数没有外界调用，代表外界没有用到发生改变后的宽和高处理
        mOnChangedSizeListener = listener;
    }

    public int getCameraID() {
        return mCameraID;
    }

    public interface OnChangedSizeListener { // 发生改变后的宽和高处理 专用接口
        void onChanged(int width, int height);
    }
}

