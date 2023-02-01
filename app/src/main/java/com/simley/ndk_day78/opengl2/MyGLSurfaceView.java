package com.simley.ndk_day78.opengl2;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class MyGLSurfaceView extends GLSurfaceView {

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
        setRenderer(new MyGLRenderer(this));// this 自定义渲染器 会回调回来做处理，所有传递this

        // 三：设置渲染器模式
        // RENDERMODE_WHEN_DIRTY 按需渲染，有帧数据的时候，才会去渲染（ 效率高，麻烦，后面需要手动调用一次才行）
        // RENDERMODE_CONTINUOUSLY 每隔16毫秒，读取更新一次，（如果没有显示上一帧）
        setRenderMode(RENDERMODE_WHEN_DIRTY); // 手动模式 - 效率高，麻烦，后面需要手动调用一次才行
    }


}
