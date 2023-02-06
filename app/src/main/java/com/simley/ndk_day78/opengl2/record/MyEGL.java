package com.simley.ndk_day78.opengl2.record;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import com.simley.ndk_day78.opengl2.filter.effects.ScreenFilter;

import static android.opengl.EGL14.*; // 静态导入一次就行了，方便些

// 管理EGL环境的工具类
public class MyEGL {

    private EGLDisplay mEGLDisplay; // EGL显示链接
    private EGLConfig mEGLConfig; // EGL最终选择配置的成果
    private EGLContext mEGLContext; // EGL的上下文
    private final EGLSurface mEGLSurface; // EGL的独有画布【重要】

    private final ScreenFilter mScreenFilter; // 最终显示的过滤器

    /**
     * 创建EGL重要环节（EGL显示链接，EGL配置项，EGL上下文）
     *
     * @param share_eglContext EGL共享上下文， 绘制线程 GLThread 中 EGL上下文，达到资源共享 【重要】
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createEGL(EGLContext share_eglContext) {
        // 1.获取EGL显示设备: EGL_DEFAULT_DISPLAY(代表 默认的设备 手机屏幕)
        mEGLDisplay = eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

        // 2.初始化设备
        int[] version = new int[2]; // 主版本号，主版本号的位置下标，副版本号，副版本号的位置下标
        if (!eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw new RuntimeException("eglInitialize fail");
        }

        // 3.选择配置
        int[] attrib_list = {
                // key 像素格式 rgba
                EGL_RED_SIZE, 8,   // value 颜色深度都设置为八位
                EGL_GREEN_SIZE, 8, // value 颜色深度都设置为八位
                EGL_BLUE_SIZE, 8,  // value 颜色深度都设置为八位
                EGL_ALPHA_SIZE, 8, // value 颜色深度都设置为八位
                // key 指定渲染api类型
                EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, // value EGL 2.0版本号
                EGLExt.EGL_RECORDABLE_ANDROID, 1, // 告诉egl 以android兼容方式创建 surface
                EGL_NONE // 一定要有结尾符
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] num_config = new int[1];
        if (!eglChooseConfig(
                mEGLDisplay,      // EGL显示链接
                attrib_list,      // 属性列表
                0,  // attrib_list 从数组第零个下标开始找
                configs,          // 输出的配置选项成果
                0,     // configs 从数组第零个下标开始找
                configs.length,   // 配置的数量，只有一个
                num_config,       // 需要的配置int数组，他需要什么就给他什么
                0  // num_config 从数组第零个下标开始找
        )) {
            throw new RuntimeException("eglChooseConfig fail");
        }
        mEGLConfig = configs[0]; // 最终EGL选择配置的成果 保存起来

        // 4.创建上下文
        int[] ctx_attrib_list = {
                EGL_CONTEXT_CLIENT_VERSION, 2, // EGL 上下文客户端版本 2.0
                EGL_NONE // 一定要有结尾符
        };
        mEGLContext = eglCreateContext(
                mEGLDisplay, // EGL显示链接
                mEGLConfig,  // EGL最终选择配置的成果
                share_eglContext, // 共享上下文， 绘制线程 GLThread 中 EGL上下文，达到资源共享
                ctx_attrib_list, // 传入上面的属性配置项
                0);
        if (null == mEGLContext || mEGLContext == EGL_NO_CONTEXT) {
            mEGLContext = null;
            throw new RuntimeException("eglCreateContext fail");
        }
    }

    /**
     * @param eglContext EGL的上下文
     * @param surface    MediaCodec的输入Surface画布
     * @param context    常规上下文
     * @param width      宽度
     * @param height     高度
     */
    public MyEGL(EGLContext eglContext, Surface surface, Context context, int width, int height) {
        // 第一大步：创建EGL环境
        createEGL(eglContext);

        // 第二大步：创建窗口（画布），绘制线程中的图像，直接往这里创建的mEGLSurface上面画
        int[] attrib_list = {EGL_NONE}; // 一定要有结尾符
        mEGLSurface = eglCreateWindowSurface(
                mEGLDisplay, // EGL显示链接
                mEGLConfig,  // EGL最终选择配置的成果
                surface,     // MediaCodec的输入Surface画布
                attrib_list, // 无任何配置，但也必须要传递 结尾符，否则人家没法玩
                0           // attrib_list的零下标开始读取
        ); // 【关联的关键操作，关联（EGL显示链接）（EGL配置）（MediaCodec的输入Surface画布）】

        // 第三大步：让 画布 盖住屏幕( 让 mEGLDisplay(EGL显示链接) 和 mEGLSurface(EGL的独有画布) 发生绑定关系)
        if (!eglMakeCurrent(
                mEGLDisplay, // EGL显示链接
                mEGLSurface, // EGL的独有画布 用来画
                mEGLSurface, // EGL的独有画布 用来读
                mEGLContext  // EGL的上下文
        )) {
            throw new RuntimeException("eglMakeCurrent fail");
        }

        // 4，往虚拟屏幕上画画
        mScreenFilter = new ScreenFilter(context);
        mScreenFilter.onReady(width, height);
    }

    /**
     * 画画
     *
     * @param textureId 纹理ID
     * @param timestamp 时间搓
     */
    public void draw(int textureId, long timestamp) {
        // 在虚拟屏幕上渲染(为什么还要写一次同样的代码？答：这个是在EGL的专属线程中的)
        mScreenFilter.onDrawFrame(textureId);

        // 刷新时间戳(如果设置不合理，编码时会采取丢帧或降低视频质量方式进行编码)
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, timestamp);

        // 交换缓冲区数据
        eglSwapBuffers(mEGLDisplay, mEGLSurface); // 绘制操作
    }

    /**
     * 释放资源
     */
    public void release() {
        eglMakeCurrent(mEGLDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        eglDestroySurface(mEGLDisplay, mEGLSurface);
        eglDestroyContext(mEGLDisplay, mEGLContext);
        eglReleaseThread();
        eglTerminate(mEGLDisplay);
    }
}
