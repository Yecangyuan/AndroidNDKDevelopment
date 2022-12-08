//
// Created by 曾辉 on 2018/12/20.
//

#ifndef STUDY_CV_HELPER_H
#define STUDY_CV_HELPER_H

#include <jni.h>
#include "opencv2/opencv.hpp"
#include <android/bitmap.h>

using namespace cv;
using namespace std;



class cv_helper {
public:
    static int bitmap2mat(JNIEnv *env, jobject &bitmap, Mat &dst);

    static int mat2bitmap(JNIEnv *env, Mat &src, jobject &bitmap);

    static jobject createBitmap(JNIEnv *env, jint width, jint height, int type);
};


#endif //STUDY_CV_HELPER_H
