//
// Created by Simle Y on 2022/12/1.
//


#include <jni.h>
#include <string>
#include <android/log.h>
#include "opencv2/face.hpp"
#include <android/log.h>
#include "utils.h"
#include "cv_helper.h"
#include "bitmap_util.h"
//#include "tesseract/baseapi.h"
#include "fstream"
#include "libavcodec/avcodec.h"


#define TAG "FACE_TAG"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jobject JNICALL
Java_com_simley_ndk_1day78_textrecognition_TextRecognition_recognize(JNIEnv *env, jobject thiz,
                                                                     jobject input) {
    Mat mat;
    cv_helper::bitmap2mat(env, input, mat);

//    Mat gray;
//    cvtColor(mat, gray, cv::COLOR_BGR2GRAY);
//    Mat sobel;
//    Sobel(mat, sobel, CV_8U, 1, 0, 3);
    Mat binary = binarize(mat);
    vector<RotatedRect> regions = findTextAreas(binary);

    int i = 1;
    for (const auto &region: regions) {
        Mat cropped = deskewAndCrop(binary, region);
        imwrite(format("/storage/emulated/0/ocr/region%d.jpg", i), cropped);
        i++;
    }
    return nullptr;
//    bool result = imwrite("/storage/emulated/0/ocr/binary.jpg", binary);
//    if (result) {
//        LOGE("写入成功");
//    }
//    jobject bitmap = bitmap_util::createBitmap(env, mat.cols, mat.cols, CV_8UC4);
//    cv_helper::mat2bitmap(env, mat, bitmap);
//    return nullptr;
}