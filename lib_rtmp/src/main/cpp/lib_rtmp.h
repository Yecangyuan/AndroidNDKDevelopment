//
// Created by Simle Y on 2023/3/15.
//

#ifndef NDK_DAY78_LIB_RTMP_H
#define NDK_DAY78_LIB_RTMP_H

#include <jni.h>
#include <stddef.h>
#include <malloc.h>
#include "rtmp.h"
#include "flvmuxer/xiecc_rtmp.h"


#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL
Java_com_simley_lib_1rtmp_RtmpClient_nativePause(JNIEnv *env, jobject thiz, jboolean pause,
                                                 jlong rtmp_pointer);

JNIEXPORT jint JNICALL
Java_com_simley_lib_1rtmp_RtmpClient_nativeOpen(JNIEnv *env, jobject thiz, jstring url,
                                                jboolean is_publish_mode, jlong rtmp_pointer,
                                                jint send_timeout_in_ms,
                                                jint receive_timeout_in_ms);

JNIEXPORT jint JNICALL
Java_com_simley_lib_1rtmp_RtmpClient_nativeRead(JNIEnv *env, jobject thiz, jbyteArray data,
                                                jint offset, jint size, jlong rtmp_pointer);

JNIEXPORT jint JNICALL
Java_com_simley_lib_1rtmp_RtmpClient_nativeWrite(JNIEnv *env, jobject thiz, jbyteArray data,
                                                 jint offset, jint size, jlong rtmp_pointer);


JNIEXPORT void JNICALL
Java_com_simley_lib_1rtmp_RtmpClient_nativeClose(JNIEnv *env, jobject thiz, jlong rtmp_pointer);


JNIEXPORT jboolean JNICALL
Java_com_simley_lib_1rtmp_RtmpClient_nativeIsConnected(JNIEnv *env, jobject thiz,
                                                       jlong rtmp_pointer);

JNIEXPORT jlong JNICALL
Java_com_simley_lib_1rtmp_RtmpClient_nativeAlloc(JNIEnv *env, jobject thiz);

jint throwIllegalStateException(JNIEnv *env, char *message);

JNIEXPORT void JNICALL
Java_com_simley_lib_1rtmp_RTMPMuxer_write_1flv_1header(JNIEnv *env, jobject thiz,
                                                       jboolean is_have_audio,
                                                       jboolean is_have_video) {
    write_flv_header(is_have_audio, is_have_video);
}

JNIEXPORT void JNICALL
Java_com_simley_lib_1rtmp_RTMPMuxer_file_1open(JNIEnv *env, jobject thiz, jstring filename) {
    const char *cfilename = (*env)->GetStringUTFChars(env, filename, NULL);
    flv_file_open(cfilename);
    (*env)->ReleaseStringUTFChars(env, filename, cfilename);
}

JNIEXPORT void JNICALL
Java_com_simley_lib_1rtmp_RTMPMuxer_file_1close(JNIEnv *env, jobject thiz) {
    flv_file_close();
}

#ifdef __cplusplus
}
#endif
#endif //NDK_DAY78_LIB_RTMP_H



