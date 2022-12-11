//
// Created by Simle Y on 2022/12/9.
//

#ifndef NDK_DAY78_YECALLJAVA_H
#define NDK_DAY78_YECALLJAVA_H

#include <jni.h>
#include <linux/stddef.h>
#include "ye_log.h"

#define MAIN_THREAD 0
#define CHILD_THREAD 1

class YECallJava {
public:
    _JavaVM *java_vm = NULL;
    JNIEnv *jni_env = NULL;
    jobject jobj;

    jmethodID jmid_prepared;
    jmethodID jmid_time_info;

public:
    YECallJava(_JavaVM *java_vm, JNIEnv *jni_env, jobject *jobj);

    ~YECallJava();

    void on_call_prepared(int type);

    void on_call_time_info(int type, int cur_time, int total_time);
};


#endif //NDK_DAY78_YECALLJAVA_H
