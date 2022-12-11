//
// Created by Simle Y on 2022/12/10.
//

#ifndef NDK_DAY78_YE_LOG_H
#define NDK_DAY78_YE_LOG_H

#include <android/log.h>

#define TAG "NDK_TAG"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

#endif //NDK_DAY78_YE_LOG_H
