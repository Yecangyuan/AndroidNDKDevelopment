//
// Created by Simle Y on 2023/8/29.
//

// EGL相关的头文件
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <jni.h>
#include "YeLog.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>

#ifndef NDK_DAY78_EGL_H
#define NDK_DAY78_EGL_H

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_egl_Egl__0003cinit_0003e(JNIEnv *env, jobject thiz);

//
//class EGL {
//
//};


#endif //NDK_DAY78_EGL_H

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_egl_Egl_createSurface(JNIEnv *env, jobject thiz, jobject surface);