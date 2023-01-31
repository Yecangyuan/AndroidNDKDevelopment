//
// Created by Simle Y on 2022/12/9.
//

#include "YECallJava.h"


YECallJava::YECallJava(_JavaVM *java_vm, JNIEnv *jni_env, jobject *jobj) {
    this->java_vm = java_vm;
    this->jni_env = jni_env;
    this->jobj = *jobj;
    this->jobj = jni_env->NewGlobalRef(*jobj);

    jclass jlz = jni_env->GetObjectClass(*jobj);
    if (!jlz) return;

    jmid_prepared = jni_env->GetMethodID(jlz, "onCallPrepared", "()V");
    jmid_time_info = jni_env->GetMethodID(jlz, "onCallTimeInfo", "(II)V");
    jmid_load = jni_env->GetMethodID(jlz, "onCallLoad", "(Z)V");
    jmid_render_yuv = jni_env->GetMethodID(jlz, "onCallRenderYUV", "(II[B[B[B)V");
    jmid_on_error = jni_env->GetMethodID(jlz, "onError", "(ILjava/lang/String;)V");
}

YECallJava::~YECallJava() {}

void YECallJava::on_call_prepared(int type) {
    if (type == MAIN_THREAD) {
        jni_env->CallVoidMethod(jobj, jmid_prepared);
    } else if (type == CHILD_THREAD) {

        JNIEnv *jni_env;
        if (java_vm->AttachCurrentThread(&jni_env, 0) != JNI_OK) {
            LOGE("get child thread jnienv wrong.");
            return;
        }

        jni_env->CallVoidMethod(jobj, jmid_prepared);
        java_vm->DetachCurrentThread();
    }

}

void YECallJava::on_call_time_info(int type, int cur_time, int total_time) {
    if (type == MAIN_THREAD) {
        jni_env->CallVoidMethod(jobj, jmid_time_info);
    } else if (type == CHILD_THREAD) {
        JNIEnv *jni_env;
        if (java_vm->AttachCurrentThread(&jni_env, NULL) != JNI_OK) {
            LOGE("call java layer method ${onCallTimeInfo} wrong.");
            return;
        }
        jni_env->CallVoidMethod(jobj, jmid_time_info, cur_time, total_time);
        java_vm->DetachCurrentThread();
    }

}

void YECallJava::on_call_load(int type, bool load) {
    if (type == MAIN_THREAD) {
        jni_env->CallVoidMethod(jobj, jmid_load, load);
    } else if (type == CHILD_THREAD) {
        JNIEnv *jniEnv;
        if (java_vm->AttachCurrentThread(&jniEnv, NULL) != JNI_OK) {
            LOGE("call onCallLoad wrong");
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_load, load);
        java_vm->DetachCurrentThread();
    }
}

void YECallJava::on_call_render_yuv(int width, int height, uint8_t *fy, uint8_t *fu, uint8_t *fv) {
    JNIEnv *jniEnv;
    if (java_vm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
        LOGE("call onCallComplete wrong");
        return;
    }

    jbyteArray y = jniEnv->NewByteArray(width * height);
    jniEnv->SetByteArrayRegion(y, 0, width * height, reinterpret_cast<const jbyte *>(fy));

    jbyteArray u = jniEnv->NewByteArray(width * height / 4);
    jniEnv->SetByteArrayRegion(u, 0, width * height / 4, reinterpret_cast<const jbyte *>(fu));

    jbyteArray v = jniEnv->NewByteArray(width * height / 4);
    jniEnv->SetByteArrayRegion(v, 0, width * height / 4, reinterpret_cast<const jbyte *>(fv));

    jniEnv->CallVoidMethod(jobj, jmid_render_yuv, width, height, y, u, v);

    jniEnv->DeleteLocalRef(y);
    jniEnv->DeleteLocalRef(u);
    jniEnv->DeleteLocalRef(v);
    java_vm->DetachCurrentThread();
}

void YECallJava::on_call_error(int type, int code, const char *msg) {
    if (type == MAIN_THREAD) {
        jstring errorMsg = jni_env->NewStringUTF(msg);
        jni_env->CallVoidMethod(jobj, jmid_on_error, code, errorMsg);
        jni_env->DeleteLocalRef(errorMsg);
    } else if (type == CHILD_THREAD) {
        JNIEnv *jniEnv;
        if (java_vm->AttachCurrentThread(&jniEnv, NULL) != JNI_OK) {
            LOGE("call onCallError wrong");
            return;
        }
        jstring errorMsg = jniEnv->NewStringUTF(msg);
        jniEnv->CallVoidMethod(jobj, jmid_on_error, code, errorMsg);
        jniEnv->DeleteLocalRef(errorMsg);
        java_vm->DetachCurrentThread();
    }

}
