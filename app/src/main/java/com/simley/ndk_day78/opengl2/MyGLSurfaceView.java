package com.simley.ndk_day78.opengl2;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import androidx.annotation.RequiresApi;

public class MyGLSurfaceView extends GLSurfaceView {
    private MyGLRenderer mRenderer;

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        // 一：设置EGL版本
        // 2 代表是 OpenGLES 2.0
        setEGLContextClientVersion(2);

        // 二：设置渲染器（工作原理 下面稍微说到了）
        // 注意：
        // EGL 开启一个 GLThread.start  run { Renderer.onSurfaceCreated ...onSurfaceChanged  onDrawFrame }
        // 如果这三个函数，不让GLThread调用，会崩溃，所以他内部的设计，必须通过GLThread调用来调用三个函数
        mRenderer = new MyGLRenderer(this);
        setRenderer(mRenderer);// this 自定义渲染器 会回调回来做处理，所有传递this

        // 三：设置渲染器模式
        // RENDERMODE_WHEN_DIRTY 按需渲染，有帧数据的时候，才会去渲染（ 效率高，麻烦，后面需要手动调用一次才行）
        // RENDERMODE_CONTINUOUSLY 每隔16毫秒，读取更新一次，（如果没有显示上一帧）
        setRenderMode(RENDERMODE_WHEN_DIRTY); // 手动模式 - 效率高，麻烦，后面需要手动调用一次才行
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        mRenderer.surfaceDestroyed();
    }

    // >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> 下面是极快 极慢 模式 相关的代码区域

    private Speed mSpeed = Speed.MODE_NORMAL;

    public enum Speed {
        MODE_EXTRA_SLOW, MODE_SLOW, MODE_NORMAL, MODE_FAST, MODE_EXTRA_FAST
    }

    /**
     * 圆形红色按钮的 按住拍 的  开始录制
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void startRecording() {
        float speed = 1.0f;
        switch (mSpeed) {
            case MODE_EXTRA_SLOW:
                speed = 0.3f;
                break;
            case MODE_SLOW:
                speed = 0.5f;
                break;
            case MODE_NORMAL:
                speed = 1.0f;
                break;
            case MODE_FAST:
                speed = 1.5f;
                break;
            case MODE_EXTRA_FAST:
                speed = 3.0f;
                break;
        }
        mRenderer.startRecording(speed);
    }

    /**
     * 圆形红色按钮的 按住拍 的 录制完成
     */
    public void stopRecording() {
        mRenderer.stopRecording();
    }

    /**
     * 极慢 慢 标准 快 极快 模式的设置函数
     *
     * @param modeExtraSlow 此枚举就是：极慢 慢 标准 快 极快 选项
     */
    public void setSpeed(Speed modeExtraSlow) {
        mSpeed = modeExtraSlow;
    }

    /**
     * 开启大眼特效
     *
     * @param isChecked
     */
    public void enableBigEye(boolean isChecked) {
        mRenderer.enableBigEye(isChecked);
    }
}
