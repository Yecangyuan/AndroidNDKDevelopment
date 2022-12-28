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

/**
 * java回调层
 */
class YECallJava {
public:
    _JavaVM *java_vm = NULL;
    JNIEnv *jni_env = NULL;
    jobject jobj;

    jmethodID jmid_prepared;
    jmethodID jmid_time_info;
    jmethodID jmid_load;
    jmethodID jmid_render_yuv;
    jmethodID jmid_on_error;

public:
    YECallJava(_JavaVM *java_vm, JNIEnv *jni_env, jobject *jobj);

    ~YECallJava();

    /**
     * 表示相关解码器设置、相关参数已经完成，可以进行解码播放
     * 回调方法，回调到java层的onCallPrepared()方法
     * @param type
     */
    void on_call_prepared(int type);

    /**
     * 数据的播放时间    回调方法
     * 在这个方法中，将会调用java层的onCallTimeInfo()方法将数据回传到应用层
     * @param type 执行线程是主线程还是子线程
     * @param cur_time 当前时间
     * @param total_time  总时间
     */
    void on_call_time_info(int type, int cur_time, int total_time);

    void on_call_load(int type, bool load);

    /**
     * 渲染yuv
     * @param width
     * @param height
     * @param fy
     * @param fu
     * @param fv
     */
    void on_call_render_yuv(int width, int height, uint8_t *fy, uint8_t *fu, uint8_t *fv);

    /**
     * 出现错误的回调java层方法
     * @param code
     * @param msg
     */
    void on_call_error(int type, int code, const char *msg);
};


#endif //NDK_DAY78_YECALLJAVA_H
