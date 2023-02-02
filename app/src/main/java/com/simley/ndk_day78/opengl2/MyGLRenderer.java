package com.simley.ndk_day78.opengl2;

import static android.opengl.GLES10.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES10.glClear;
import static android.opengl.GLES10.glClearColor;
import static android.opengl.GLES10.glGenTextures;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;

import com.simley.ndk_day78.opengl2.filter.CameraFilter;
import com.simley.ndk_day78.opengl2.filter.ScreenFilter;
import com.simley.ndk_day78.opengl2.utils.CameraHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements
        GLSurfaceView.Renderer,  // 渲染器的三个函数 onSurfaceCreated onSurfaceChanged onDrawFrame
        SurfaceTexture.OnFrameAvailableListener // 有可用的数据时，回调此函数，效率高，麻烦，后面需要手动调用一次才行
{
    private final MyGLSurfaceView myGLSurfaceView;
    private CameraHelper mCameraHelper;
    private int[] mTextureID;
    private SurfaceTexture mSurfaceTexture;
    private ScreenFilter mScreenFilter;
    float[] mtx = new float[16]; // 矩阵数据，变换矩阵
    private CameraFilter mCameraFilter;

    public MyGLRenderer(MyGLSurfaceView myGLSurfaceView) {
        this.myGLSurfaceView = myGLSurfaceView;
    }

    /**
     * Surface创建时 回调此函数
     *
     * @param gl     OpenGL1.0 和 OpenGL2.0 差距很大，无法兼容下面了，遗留下来了之前的 1.0
     * @param config 配置项 目前用不到，不管
     */
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraHelper = new CameraHelper((Activity) myGLSurfaceView.getContext(),
                Camera.CameraInfo.CAMERA_FACING_FRONT,
                800, 480); // 前置摄像头

        // 获取纹理ID【先理解成画布】
        mTextureID = new int[1];
        /**
         * 1.长度 只有一个 1
         * 2.纹理ID，是一个数组
         * 3.offset:0 使用数组的0下标
         */
        glGenTextures(mTextureID.length, mTextureID, 0);
        mSurfaceTexture = new SurfaceTexture(mTextureID[0]);// 实例化纹理对象
        mSurfaceTexture.setOnFrameAvailableListener(this); // 绑定好此监听 SurfaceTexture.OnFrameAvailableListener

        mCameraFilter = new CameraFilter(myGLSurfaceView.getContext()); // 首先 FBO
        mScreenFilter = new ScreenFilter(myGLSurfaceView.getContext()); // 其次 渲染屏幕
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

        /*textureId = 美白.onDrawFrame(textureId);
        textureId = 大眼.onDrawFrame(textureId);
        textureId = xxx.onDrawFrame(textureId);*/

        // 最终直接显示的，他是调用了 BaseFilter的onDrawFrame渲染的（简单的显示就行了）
        // 核心在这里：1：画布==纹理ID，  2：mtx矩阵数据
        mScreenFilter.onDrawFrame(textureId);
    }

    // 有可用的数据时，回调此函数，效率高，麻烦，后面需要手动调用一次才行
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        myGLSurfaceView.requestRender(); // setRenderMode(RENDERMODE_WHEN_DIRTY); 配合用
    }
}
