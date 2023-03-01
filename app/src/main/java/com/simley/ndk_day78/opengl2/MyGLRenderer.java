package com.simley.ndk_day78.opengl2;

import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;
import static android.opengl.GLES10.glGenTextures;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.simley.ndk_day78.opengl2.face.FaceTrack;
import com.simley.ndk_day78.opengl2.filter.effects.BeautyFilter;
import com.simley.ndk_day78.opengl2.filter.effects.BigEyeFilter;
import com.simley.ndk_day78.opengl2.filter.effects.CameraFilter;
import com.simley.ndk_day78.opengl2.filter.effects.ScreenFilter;
import com.simley.ndk_day78.opengl2.filter.effects.StickFilter;
import com.simley.ndk_day78.opengl2.record.MyMediaRecorder;
import com.simley.ndk_day78.opengl2.utils.CameraHelper;
import com.simley.ndk_day78.utils.FileUtil;

import java.io.IOException;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements
        GLSurfaceView.Renderer,  // 渲染器的三个函数 onSurfaceCreated onSurfaceChanged onDrawFrame
        SurfaceTexture.OnFrameAvailableListener, // 有可用的数据时，回调此函数，效率高，麻烦，后面需要手动调用一次才行
        Camera.PreviewCallback {
    private final MyGLSurfaceView mGLSurfaceView;
    private CameraHelper mCameraHelper;
    private int[] mTextureID;
    private SurfaceTexture mSurfaceTexture;
    private ScreenFilter mScreenFilter;
    float[] mtx = new float[16]; // 矩阵数据，变换矩阵
    private CameraFilter mCameraFilter;
    private MyMediaRecorder mMediaRecorder;

    private int mWidth;
    private int mHeight;

    private BigEyeFilter mBigEyeFilter;
    private FaceTrack mFaceTrack;
    private StickFilter mStickFilter;
    private BeautyFilter mBeautyFilter;


    public MyGLRenderer(MyGLSurfaceView mGLSurfaceView) {
        this.mGLSurfaceView = mGLSurfaceView;
        // 大眼相关代码】  assets Copy到SD卡
        FileUtil.copyAssets2SDCard(mGLSurfaceView.getContext(), "haarcascade_frontalface_alt.xml",
                "/sdcard/haarcascade_frontalface_alt.xml"); // OpenCV的模型
        FileUtil.copyAssets2SDCard(mGLSurfaceView.getContext(), "seeta_fa_v1.1.bin",
                "/sdcard/seeta_fa_v1.1.bin"); // 中科院的模型

    }

    /**
     * Surface创建时 回调此函数
     *
     * @param gl     OpenGL1.0 和 OpenGL2.0 差距很大，无法兼容下面了，遗留下来了之前的 1.0
     * @param config 配置项 目前用不到，不管
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraHelper = new CameraHelper((Activity) mGLSurfaceView.getContext(),
                Camera.CameraInfo.CAMERA_FACING_FRONT,
                800, 480); // 前置摄像头

        mCameraHelper.setPreviewCallback(this);

        // 获取纹理ID【先理解成画布】
        mTextureID = new int[1];
        /*
          1.长度 只有一个 1
          2.纹理ID，是一个数组
          3.offset:0 使用数组的0下标
         */
        glGenTextures(mTextureID.length, mTextureID, 0);
        mSurfaceTexture = new SurfaceTexture(mTextureID[0]);// 实例化纹理对象
        mSurfaceTexture.setOnFrameAvailableListener(this); // 绑定好此监听 SurfaceTexture.OnFrameAvailableListener

        mCameraFilter = new CameraFilter(mGLSurfaceView.getContext()); // 首先 FBO
        mScreenFilter = new ScreenFilter(mGLSurfaceView.getContext()); // 其次 渲染屏幕

        // 初始化录制工具类
        EGLContext eglContext = EGL14.eglGetCurrentContext();
        mMediaRecorder = new MyMediaRecorder(480, 800,
                "/sdcard/Movies" + System.currentTimeMillis() + ".mp4", eglContext,
                mGLSurfaceView.getContext());
    }

    /**
     * Surface 改变时 回调此函数
     *
     * @param gl     OpenGL1.0 和 OpenGL2.0 差距很大，无法兼容下面了，遗留下来了之前的 1.0
     * @param width  宽
     * @param height 高
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mWidth = width;
        mHeight = height;

        // 创建人脸检测跟踪器
        mFaceTrack = new FaceTrack("/sdcard/haarcascade_frontalface_alt.xml", "/sdcard/seeta_fa_v1.1.bin", mCameraHelper);
        mFaceTrack.startTrack(); // 启动跟踪器

        mCameraHelper.startPreview(mSurfaceTexture); // 开始预览
        mCameraFilter.onReady(width, height);
        mScreenFilter.onReady(width, height);
    }

    /**
     * 绘制一帧图像时 回调此函数
     *
     * @param gl OpenGL1.0 和 OpenGL2.0 差距很大，无法兼容下面了，遗留下来了之前的 1.0
     */
    @Override
    public void onDrawFrame(GL10 gl) {
        // 每次清空之前的：例子：上课擦黑板 是一个道理
        glClearColor(255, 0, 0, 0); // 屏幕清理成颜色 红色，清理成红色的黑板一样
        // mask 细节看看此文章：https://blog.csdn.net/z136411501/article/details/83273874
        // GL_COLOR_BUFFER_BIT 颜色缓冲区
        // GL_DEPTH_BUFFER_BIT 深度缓冲区
        // GL_STENCIL_BUFFER_BIT 模型缓冲区
        glClear(GL_COLOR_BUFFER_BIT);
        // 绘制摄像头数据
        mSurfaceTexture.updateTexImage(); // 将纹理图像更新为图像流中最新的帧数据【刷新一下】
        // 画布，矩阵数据
        mSurfaceTexture.getTransformMatrix(mtx);
        mCameraFilter.setMatrix(mtx);

        int textureId = mCameraFilter.onDrawFrame(mTextureID[0]);// 摄像头，矩阵，都已经做了
        textureId = getTextureId(textureId);

        // 最终直接显示的，他是调用了 BaseFilter的onDrawFrame渲染的（简单的显示就行了）
        // 核心在这里：1：画布==纹理ID，  2：mtx矩阵数据
        mScreenFilter.onDrawFrame(textureId);
        // 录制
        mMediaRecorder.encodeFrame(textureId, mSurfaceTexture.getTimestamp());
    }

    /**
     * 获取最终叠加了所有 filter之后的纹理id
     *
     * @param textureId 纹理id
     * @return textureId
     */
    private int getTextureId(int textureId) {
        // textureId = 大眼Filter.onDrawFrame(textureId);
        // 贴纸
        if (null != mBigEyeFilter) {
            mBigEyeFilter.setFace(mFaceTrack.getFace());
            textureId = mBigEyeFilter.onDrawFrame(textureId);
        }

        // 贴纸
        if (null != mStickFilter) {
            mStickFilter.setFace(mFaceTrack.getFace()); // 需要定位人脸，所以需要 JavaBean
            textureId = mStickFilter.onDrawFrame(textureId);
        }

        // 美颜
        if (null != mBeautyFilter) { // 没有不需要 人脸追踪/人脸关键点，整个屏幕美颜
            textureId = mBeautyFilter.onDrawFrame(textureId);
        }

        return textureId;
    }


    public void surfaceDestroyed() {
        mCameraHelper.stopPreview();
        mFaceTrack.stopTrack(); // 停止跟踪器
    }

    /**
     * 圆形红色按钮的 按住拍 开始录制
     *
     * @param speed 录制的：极慢 慢 标准 快 极快
     */
    public void startRecording(float speed) {
        Log.e("MyGLRender", "startRecording speed:" + speed);
        try {
            mMediaRecorder.start(speed);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开启大眼特效
     *
     * @param isChecked
     */
    public void enableBigEye(final boolean isChecked) {
        // BigEyeFilter bigEyeFilter = new BigEyeFilter(); // 这样可以吗  不行，必须在EGL线程里面绘制

        // 把大眼渲染代码，加入到， GLSurfaceView 的 内置EGL 的 GLTHread里面
        mGLSurfaceView.queueEvent(() -> {
            if (isChecked) {
                mBigEyeFilter = new BigEyeFilter(mGLSurfaceView.getContext());
                mBigEyeFilter.onReady(mWidth, mHeight);
            } else {
                mBigEyeFilter.release();
                mBigEyeFilter = null;
            }
        });
    }


    // Camera画面只有有数据，就会回调此函数
    @Override // 要把相机的数据，给C++层做人脸追踪
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d(this.getClass().getSimpleName(), "onPreviewFrame : " + Arrays.toString(data));
        mFaceTrack.detector(data);
    }

    /**
     * 圆形红色按钮的 按住拍 的 录制完成
     */
    public void stopRecording() {
        Log.e("MyGLRender", "stopRecording");
        mMediaRecorder.stop();
    }

    // 有可用的数据时，回调此函数，效率高，麻烦，后面需要手动调用一次才行
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mGLSurfaceView.requestRender(); // setRenderMode(RENDERMODE_WHEN_DIRTY); 配合用
    }


    /**
     * 开启贴纸
     *
     * @param isChecked checkbox复选框是否勾上了
     */
    public void enableStick(final boolean isChecked) {
        // 在EGL线程里面绘制 贴纸工作
        mGLSurfaceView.queueEvent(() -> {
            if (isChecked) {
                mStickFilter = new StickFilter(mGLSurfaceView.getContext());
                mStickFilter.onReady(mWidth, mHeight);
            } else {
                mStickFilter.release();
                mStickFilter = null;
            }
        });
    }

    /**
     * 开启美颜
     *
     * @param isChecked checkbox复选框是否勾上了
     */
    public void enableBeauty(final boolean isChecked) {
        mGLSurfaceView.queueEvent(() -> {
            if (isChecked) {
                mBeautyFilter = new BeautyFilter(mGLSurfaceView.getContext());
                mBeautyFilter.onReady(mWidth, mHeight);
            } else {
                mBeautyFilter.release();
                mBeautyFilter = null;
            }
        });
    }
}
