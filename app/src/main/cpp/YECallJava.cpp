//
// Created by Simle Y on 2022/12/9.
//

#include "YECallJava.h"


YECallJava::YECallJava(_JavaVM *java_vm, JNIEnv *jni_env, jobject *jobj) {
    this->java_vm = java_vm;
    this->jni_env = jni_env;
    this->jobj = *jobj;
    // 在这里将jobj（YEPlayer）对象提升为全局变量（默认是局部变量）
    // 因为这个变量将跨线程传递，所以必须这样做，否则会程序崩溃
    this->jobj = jni_env->NewGlobalRef(*jobj);

    jclass jlz = jni_env->GetObjectClass(*jobj);
    if (!jlz) return;

    jmid_prepared = jni_env->GetMethodID(jlz, "onCallPrepared", "()V");
    jmid_time_info = jni_env->GetMethodID(jlz, "onCallTimeInfo", "(II)V");
    jmid_load = jni_env->GetMethodID(jlz, "onCallLoad", "(Z)V");
    jmid_render_yuv = jni_env->GetMethodID(jlz, "onCallRenderYUV", "(II[B[B[B)V");
    jmid_on_error = jni_env->GetMethodID(jlz, "onError", "(ILjava/lang/String;)V");
}

YECallJava::~YECallJava() = default;

void YECallJava::on_call_prepared(int type) {
    // 该段代码放在主线程中执行
    if (type == MAIN_THREAD) {
        jni_env->CallVoidMethod(jobj, jmid_prepared);
    }
        // 该段代码在C++ 中放在子线程中执行
    else if (type == CHILD_THREAD) {
        JNIEnv *jniEnv;
        result = java_vm->AttachCurrentThread(&jniEnv, 0);
        if (result != JNI_OK) {
            LOGE("调用Java层的onCallPrepared()方法错误，错误码：%d", result);
            return;
        }

        jniEnv->CallVoidMethod(jobj, jmid_prepared);
        java_vm->DetachCurrentThread();
    }
}

void YECallJava::on_call_time_info(int type, int cur_time, int total_time) {
    if (type == MAIN_THREAD) {
        jni_env->CallVoidMethod(jobj, jmid_time_info);
    } else if (type == CHILD_THREAD) {
        JNIEnv *jniEnv;
        result = java_vm->AttachCurrentThread(&jniEnv, NULL);
        if (result != JNI_OK) {
            LOGE("调用Java层的onCallTimeInfo()方法错误，错误码：%d", result);
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_time_info, cur_time, total_time);
        java_vm->DetachCurrentThread();
    }

}

void YECallJava::on_call_load(int type, bool load) {
    if (type == MAIN_THREAD) {
        jni_env->CallVoidMethod(jobj, jmid_load, load);
    } else if (type == CHILD_THREAD) {
        JNIEnv *jniEnv;
        result = java_vm->AttachCurrentThread(&jniEnv, NULL);
        if (result != JNI_OK) {
            LOGE("调用Java层的onCallLoad()方法错误，错误码：%d", result);
            return;
        }
        jniEnv->CallVoidMethod(jobj, jmid_load, load);
        java_vm->DetachCurrentThread();
    }
}

void YECallJava::on_call_render_yuv(int width, int height, uint8_t *fy, uint8_t *fu, uint8_t *fv) {
    JNIEnv *jniEnv;
    result = java_vm->AttachCurrentThread(&jniEnv, NULL);
    if (result != JNI_OK) {
        LOGE("调用Java层的onCallComplete()方法错误，错误码：%d", result);
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
        jstring err_msg = jni_env->NewStringUTF(msg);
        jni_env->CallVoidMethod(jobj, jmid_on_error, code, err_msg);
        jni_env->DeleteLocalRef(err_msg);
    } else if (type == CHILD_THREAD) {
        JNIEnv *jniEnv;
        result = java_vm->AttachCurrentThread(&jniEnv, NULL);
        if (result != JNI_OK) {
            LOGE("调用Java层的onCallError()方法错误，错误码：%d", result);
            return;
        }
        jstring errorMsg = jniEnv->NewStringUTF(msg);
        jniEnv->CallVoidMethod(jobj, jmid_on_error, code, errorMsg);
        jniEnv->DeleteLocalRef(errorMsg);
        java_vm->DetachCurrentThread();
    }
}
