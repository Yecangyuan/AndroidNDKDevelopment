package com.simley.ndk_day78.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class YEGLSurfaceView extends GLSurfaceView {

    private final YERender mYERender;

    public YEGLSurfaceView(Context context) {
        this(context, null);
    }

    public YEGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        mYERender = new YERender(context);
        setRenderer(mYERender);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setYUVData(int width, int height, byte[] y, byte[] u, byte[] v) {
        if (mYERender != null) {
            mYERender.setYUVRenderData(width, height, y, u, v);
            requestRender();
        }
    }
}
