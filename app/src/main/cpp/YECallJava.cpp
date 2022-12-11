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
        if (java_vm->AttachCurrentThread(&jni_env, 0) != JNI_OK) {
            LOGE("call java layer method ${onCallTimeInfo} wrong.");
            return;
        }
        jni_env->CallVoidMethod(jobj, jmid_time_info, cur_time, total_time);
        java_vm->DetachCurrentThread();
    }

}
