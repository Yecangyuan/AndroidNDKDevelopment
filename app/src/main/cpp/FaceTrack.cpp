//
// Created by Administrator on 2023-02-05.
//

#include "FaceTrack.h"


FaceTrack::FaceTrack(const char *model, const char *seeta) {
    Ptr<CascadeDetectorAdapter> mainDetector = makePtr<CascadeDetectorAdapter>(
            makePtr<CascadeClassifier>(model)); // OpenCV主探测器
    Ptr<CascadeDetectorAdapter> trackingDetector = makePtr<CascadeDetectorAdapter>(
            makePtr<CascadeClassifier>(model)); // OpenCV跟踪探测器
    DetectionBasedTracker::Parameters detectorParams;
    // OpenCV创建追踪器，为了下面的（开始跟踪，停止跟踪）
    tracker = makePtr<DetectionBasedTracker>(mainDetector, trackingDetector, detectorParams);

    // TODO >>>>>>>>>>>>>>>>>>>>>>> 上面是OpenCV模板代码人脸追踪区域， 下面是Seeta人脸关键点代码+OpenCV >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>

    faceAlignment = makePtr<seeta::FaceAlignment>(seeta); // Seeta中科院关键特征点
}

void FaceTrack::startTracking() { // OpenCV开启追踪器
    tracker->run();
}

void FaceTrack::stopTracking() { // OpenCV关闭追踪器
    tracker->stop();
}

void FaceTrack::detec


extern "C"
JNIEXPORT jlong JNICALL
Java_com_simley_ndk_1day78_opengl2_face_FaceTrack_native_1create(JNIEnv *env, jobject thiz,
                                                                 jstring model, jstring seeta) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_opengl2_face_FaceTrack_native_1start(JNIEnv *env, jobject thiz,
                                                                jlong self) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_opengl2_face_FaceTrack_native_1stop(JNIEnv *env, jobject thiz,
                                                               jlong self) {

}
extern "C"
JNIEXPORT jobject JNICALL
Java_com_simley_ndk_1day78_opengl2_face_FaceTrack_native_1detector(JNIEnv *env, jobject thiz,
                                                                   jlong self, jbyteArray data,
                                                                   jint camera_id, jint width,
                                                                   jint height) {

}






