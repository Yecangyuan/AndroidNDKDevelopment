//
// Created by Simle Y on 2022/12/9.
//

#include "YECallJava.h"


YECallJava::YECallJava(_JavaVM *javaVm, JNIEnv *jniEnv, jobject *jObj) {
    this->javaVm = javaVm;
    this->jniEnv = jniEnv;
    this->jObj = *jObj;
    // 在这里将jobj（YEPlayer）对象提升为全局变量（默认是局部变量）
    // 因为这个变量将跨线程传递，所以必须这样做，否则会程序崩溃
    this->jObj = jniEnv->NewGlobalRef(*jObj);

    jclass jlz = jniEnv->GetObjectClass(*jObj);
    if (!jlz) return;

    jmidPrepared = jniEnv->GetMethodID(jlz, "onCallPrepared", "()V");
    jmidTimeInfo = jniEnv->GetMethodID(jlz, "onCallTimeInfo", "(II)V");
    jmidLoad = jniEnv->GetMethodID(jlz, "onCallLoad", "(Z)V");
    jmidRenderYuv = jniEnv->GetMethodID(jlz, "onCallRenderYUV", "(II[B[B[B)V");
    jmidOnError = jniEnv->GetMethodID(jlz, "onError", "(ILjava/lang/String;)V");
}

YECallJava::~YECallJava() = default;

void YECallJava::onCallPrepared(int type) {
    // 该段代码放在主线程中执行
    if (type == MAIN_THREAD) {
        jniEnv->CallVoidMethod(jObj, jmidPrepared);
    }
        // 该段代码在C++ 中放在子线程中执行
    else if (type == CHILD_THREAD) {
        JNIEnv *jniEnv;
        result = javaVm->AttachCurrentThread(&jniEnv, 0);
        if (result != JNI_OK) {
            LOGE("调用Java层的onCallPrepared()方法错误，错误码：%d", result);
            return;
        }

        jniEnv->CallVoidMethod(jObj, jmidPrepared);
        javaVm->DetachCurrentThread();
    }
}

void YECallJava::onCallTimeInfo(int type, int curTime, int totalTime) {
    if (type == MAIN_THREAD) {
        jniEnv->CallVoidMethod(jObj, jmidTimeInfo);
    } else if (type == CHILD_THREAD) {
        JNIEnv *jniEnv;
        result = javaVm->AttachCurrentThread(&jniEnv, NULL);
        if (result != JNI_OK) {
            LOGE("调用Java层的onCallTimeInfo()方法错误，错误码：%d", result);
            return;
        }
        jniEnv->CallVoidMethod(jObj, jmidTimeInfo, curTime, totalTime);
        javaVm->DetachCurrentThread();
    }

}

void YECallJava::onCallLoad(int type, bool load) {
    if (type == MAIN_THREAD) {
        jniEnv->CallVoidMethod(jObj, jmidLoad, load);
    } else if (type == CHILD_THREAD) {
        JNIEnv *jniEnv;
        result = javaVm->AttachCurrentThread(&jniEnv, NULL);
        if (result != JNI_OK) {
            LOGE("调用Java层的onCallLoad()方法错误，错误码：%d", result);
            return;
        }
        jniEnv->CallVoidMethod(jObj, jmidLoad, load);
        javaVm->DetachCurrentThread();
    }
}

void YECallJava::onCallRenderYuv(int width, int height, uint8_t *fy, uint8_t *fu, uint8_t *fv) {
    JNIEnv *jniEnv;
    result = javaVm->AttachCurrentThread(&jniEnv, NULL);
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

    jniEnv->CallVoidMethod(jObj, jmidRenderYuv, width, height, y, u, v);

    jniEnv->DeleteLocalRef(y);
    jniEnv->DeleteLocalRef(u);
    jniEnv->DeleteLocalRef(v);
    javaVm->DetachCurrentThread();
}

void YECallJava::onCallError(int type, int code, const char *msg) {
    if (type == MAIN_THREAD) {
        jstring err_msg = jniEnv->NewStringUTF(msg);
        jniEnv->CallVoidMethod(jObj, jmidOnError, code, err_msg);
        jniEnv->DeleteLocalRef(err_msg);
    } else if (type == CHILD_THREAD) {
        JNIEnv *jniEnv;
        result = javaVm->AttachCurrentThread(&jniEnv, NULL);
        if (result != JNI_OK) {
            LOGE("调用Java层的onCallError()方法错误，错误码：%d", result);
            return;
        }
        jstring errorMsg = jniEnv->NewStringUTF(msg);
        jniEnv->CallVoidMethod(jObj, jmidOnError, code, errorMsg);
        jniEnv->DeleteLocalRef(errorMsg);
        javaVm->DetachCurrentThread();
    }
}
