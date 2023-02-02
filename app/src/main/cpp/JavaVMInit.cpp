////
//// Created by Simle Y on 2023/2/2.
////
//
//#include "jni.h"
//#include "ye_log.h"
//
//
////_JavaVM *java_vm;
//
///*
// * 这个JNI_OnLoad()函数就相当于Java层的构造方法，会在Java层调用loadLibrary()方法的时候被调用
// * 如果不重写JNI_OnLoad()方法，则
// */
//extern "C"
//JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *javaVm, void *reserved) {
//    ::java_vm = javaVm;
//
//    JNIEnv *env = NULL;
//    jint result = ::java_vm->GetEnv((void **) &env, JNI_VERSION_1_6);
//    if (result != JNI_OK) {
//        LOGD("加载JNIEnv失败");
//        return result;
//    }
//    LOGD("加载JNIEnv成功");
//    return JNI_VERSION_1_6; // 在AS的JDK在JNI默认最高是1.6    在Java的JDK在JNI可以是1.8或者更高
//}
//
//static _JavaVM *getJavaVm() {
//    return java_vm;
//}
