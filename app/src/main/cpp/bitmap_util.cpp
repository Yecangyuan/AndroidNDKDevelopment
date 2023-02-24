//
// Created by hcDarren on 2019/2/24.
//

#include "bitmap_util.h"
#include "opencv2/opencv.hpp"
#include <android/log.h>

jobject bitmap_util::createBitmap(JNIEnv *env, int width, int height, int type) {
    // 根据 type 来获取 Config
    // Bitmap.Config.valueOf("ARGB_8888");
    char *config_name;

    if (type == CV_8UC4) {
        // 8888
        config_name = (char *) "ARGB_8888";
    }/*else if(){
    }*/
    jstring configName = env->NewStringUTF(config_name);
    const char *bitmap_config_class_name = "android/graphics/Bitmap$Config";
    jclass bitmap_config_class = env->FindClass(bitmap_config_class_name);
    const char *valueOf_sig = "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;";
    jmethodID create_bitmap_config_mid = env->GetStaticMethodID(bitmap_config_class, "valueOf",
                                                                valueOf_sig);
    jobject config = env->CallStaticObjectMethod(bitmap_config_class, create_bitmap_config_mid,
                                                 configName);

    // 怎么去创建 bitmap？
    // 获取 java 层的方法
    // public static Bitmap createBitmap(int width, int height, @NonNull Config config)
    const char *bitmap_class_name = "android/graphics/Bitmap";
    jclass bitmap_class = env->FindClass(bitmap_class_name);
    const char *create_bitmap_sig = "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;";
    jmethodID create_bitmap_mid = env->GetStaticMethodID(bitmap_class, "createBitmap",
                                                         create_bitmap_sig);
    jobject bitmap = env->CallStaticObjectMethod(bitmap_class, create_bitmap_mid, width, height,
                                                 config);
    return bitmap;
}