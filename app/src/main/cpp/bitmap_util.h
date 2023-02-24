//
// Created by hcDarren on 2019/2/24.
//

#ifndef NDK_DAY71_AS_BITMAP_UTIL_H
#define NDK_DAY71_AS_BITMAP_UTIL_H


#include <jni.h>

class bitmap_util {
public:
    static jobject createBitmap(JNIEnv *env, int width, int height, int type);
};


#endif //NDK_DAY71_AS_BITMAP_UTIL_H
