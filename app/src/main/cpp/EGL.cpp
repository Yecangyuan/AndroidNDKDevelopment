//
// Created by Simle Y on 2023/8/29.
//

#include "EGL.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_egl_Egl_createSurface(JNIEnv *env, jobject thiz, jobject j_surface) {

    // EGLDisplay就是一个封装系统物理屏幕的数据类型，也就是绘制目标的一个抽象
    EGLDisplay display;
    // 1. 获取一个EGLDisplay对象
    display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display == EGL_NO_DISPLAY) {
        LOGD("eglGetDisplay() returned error %d", eglGetError());
        return;
    }

    // 2. 调用eglInitialize()方法对EGLDisplay对象进行初始化
    // 一旦EGLDisplay初始化成功之后，它就可以将OpenGL ES的输出和设备的屏幕桥接起来
    // eglInitialize：
    //      第一个参数是EGLDisplay
    //      第二个参数是主版本号
    //      第三个参数是子版本号
    if (!eglInitialize(display, 0, 0)) {
        LOGD("eglInitialize() returned error %d", eglGetError());
        return;
    }

    // 3. 配置一些色彩格式、像素格式、OpenGL 版本以及SurfaceType等
    // 不同的系统以及平台使用的EGL标准时不同的，在 Android 平台下一般配置的代码如下所示:
    EGLConfig config;
    EGLint numConfig = 0;
    const EGLint attribs[] = {
            // Buffer大小
            EGL_BUFFER_SIZE, 32,
            // 红绿蓝
            EGL_ALPHA_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_RED_SIZE, 8,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_NONE
    };
    if (!eglChooseConfig(display, attribs, &config, 1, &numConfig)) {
        LOGD("eglChooseConfig() returned error %d", eglGetError());
        return;
    }

    // 4. 由于任何一条OpenGL ES指令（OpenGL ES提供给开发者的接口）都必须运行在自己的OpenGL 上下文环境中，
    // EGL提供了EGLContext来封装上下文，可以按照如下代码构建出OpenGL ES的上下文环境
    /*
        函数eglCreateContext的前两个参数就是我们刚刚创建的两个对象，第三个参数类型也是EGLContext类型的，一般会有两种用法。
            如果想和已经存在的某个上下文共享OpenGL资源（包括纹理ID、frameBuffer以及其他的Buffer），则传入对应的那个上下文变量。
            如果目标仅仅是创建一个独立的上下文，不需要和其他OpenGL ES的上下文共享任何资源，则设置为NULL。
        在一些场景下其实需要多个线程共同执行OpenGL ES的渲染操作，这种情况下就需要用到共享上下文，共享上下文的关键点就在这里，一定要记住。
     */
    EGLContext context;
    EGLint attributes[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
    context = eglCreateContext(display, config, NULL, attributes);
    if (!context) {
        LOGD("eglCreateContext() returned error %d", eglGetError());
        return;
    }

    /*
     * 成功创建出OpenGL ES的上下文，说明我们已经把OpenGL ES的绘制目标搞定了，但是这个绘制目标并没有渲染到我们某个View上
     * ，那如何将这个输出渲染到业务指定的View上呢？答案就在EGLSurface。
     */

    EGLSurface surface = NULL;
    EGLint format;
    if (!eglGetConfigAttrib(display, config, EGL_NATIVE_VISUAL_ID, &format)) {
        LOGD("eglGetConfigAttrib() returned error %d", eglGetError());
        return;
    }

    ANativeWindow *_window = ANativeWindow_fromSurface(env, j_surface);
    ANativeWindow_setBuffersGeometry(_window, 0, 0, format);
    surface = eglCreateWindowSurface(display, config, _window, 0);
    if (!surface) {
        LOGD("eglGetConfigAttrib() returned error %d", eglGetError());
        return;
    }

    // 为绘制线程绑定上下文
    /*
     * OpenGL ES需要开发者自己开辟一个新的线程来执行OpenGL ES的渲染操作，还要求开发者在执行渲染操作前要为这个线程
     * 绑定上下文环境，通过调用eglMakeCurrent()方法来绑定上下文环境
     */
    eglMakeCurrent(display, surface, surface, context);

    // 在绑定了上下文环境以及窗口之后就可以执行 RenderLoop 循环了，每一次循环都是去调用
    // OpenGL ES 指令绘制图像。

    // 执行完这个函数， 用户就可以在屏幕上看到刚刚渲染的图像了
    eglSwapBuffers(display, surface);

    // 销毁资源
    eglDestroySurface(display, surface);
    eglDestroyContext(display, context);

}