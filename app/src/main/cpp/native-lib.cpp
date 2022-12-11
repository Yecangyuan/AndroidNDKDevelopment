#include <jni.h>
#include <string>
#include "ye_log.h"
#include <vector>
#include "opencv2/opencv.hpp"
#include "opencv2/face.hpp"
#include <android/log.h>

#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "cv_helper.h"
#include "cardocr.h"


using namespace cv;
using namespace std;
using namespace face;

#define IMAGE_SIZE 24

// 加载人脸识别的级联分类器
CascadeClassifier faceCascadeClassifier;
// 加载眼睛识别的级联分类器
CascadeClassifier eyeCascadeClassifier;
// 人脸识别模型
Ptr<BasicFaceRecognizer> model = EigenFaceRecognizer::create();

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_face_FaceDetection_faceDetection(JNIEnv *env, jobject thiz,
                                                            jlong native_obj) {
    Mat *src = reinterpret_cast<Mat *>(native_obj);
    if (src->empty()) {
        LOGE("--(!) 摄像头未捕获到图像帧 -- return!");
        return;
    }
    int width = src->rows;
    int height = src->cols;

    Mat grayMat;
    // 转成灰度图，提升运算速度，灰度图所对应的CV_8UC1 单颜色通道， 信息量少 0~255 1u
    cvtColor(*src, grayMat, COLOR_BGR2GRAY);
    // 直方图均衡，对图像进行增强
    equalizeHist(grayMat, grayMat);

    // 4. 检测人脸
    std::vector<Rect> faces;
    // 多尺寸检测人脸
    faceCascadeClassifier.detectMultiScale(grayMat, faces, 1.1, 5, 0 | CASCADE_SCALE_IMAGE,
                                           Size(30, 30));
    LOGE("人脸size = %lu", faces.size());
//    faceCascadeClassifier.detectMultiScale(grayMat, faces, 1.1, 3, 0, Size(width / 4, height / 4));

    // 没有人脸 或者 有多张人脸
    if (faces.size() != 1) {
//        mosaicFace(*src);
        return;
    }

    for (const auto &face: faces) {
//        Point center(face.x + face.width * 0.5, face.y + face.height * 0.5);
//        ellipse(*src, center, Size(face.width * 0.5, face.height * 0.5), 0, 0, 360,
//                Scalar(255, 0, 255), 4, 8, 0);
        rectangle(*src, face, Scalar(255, 0, 0, 255), 4, LINE_AA);
        Mat faceROI = (*src)(face).clone();
        std::vector<Rect> eyes;
        //-- 在每张人脸上检测双眼
        eyeCascadeClassifier.detectMultiScale(faceROI, eyes, 1.1, 2, 0 | CASCADE_SCALE_IMAGE,
                                              Size(30, 30));

        for (const auto &eye: eyes) {
            Point center(face.x + eye.x + eye.width * 0.5, face.y + eye.y + eye.height * 0.5);
            int radius = cvRound((eye.width + eye.height) * 0.25);
            circle(*src, center, radius, Scalar(255, 0, 0, 255), 4, LINE_AA, 0);
        }

        // 用一个计数器，这里我们做及时的
        resize(faceROI, faceROI, Size(IMAGE_SIZE, IMAGE_SIZE));
        cvtColor(faceROI, faceROI, COLOR_BGRA2GRAY);
        int label = model->predict(faceROI);
        if (label == 11) {
            // 识别到了自己
            putText(*src, format("Simley:label=%d", label), Point(face.x + 20, face.y - 20),
                    HersheyFonts::FONT_HERSHEY_TRIPLEX, 1, Scalar(255, 0, 0, 255), 1, LINE_AA);
        } else {
            // 不是自己
            putText(*src, format("UnKnow:label=%d", label), Point(face.x + 20, face.y - 20),
                    HersheyFonts::FONT_HERSHEY_TRIPLEX, 1, Scalar(255, 0, 0, 255), 1, LINE_AA);
        }
    }

    // 把脸框出来
//    Rect faceRect = faces[0];
//    rectangle(*src, faceRect, Scalar(255, 0, 0, 255), 4, LINE_AA);
//    mosaicFace((*src)(faceRect));
    // 与服务端对比，是否是本人
    // 不断检测，录入 10 张，张张嘴巴，眨眨眼睛 ，保证准确率
    // 还需要注意一点，确保人脸大小一致，reSize(128,128) ,确保收集到的人脸眼睛尽量在一条线上


}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_face_FaceDetection_loadFaceCascade(JNIEnv *env, jobject thiz,
                                                              jstring file_path) {
    const char *filePath = env->GetStringUTFChars(file_path, 0);
    bool ret = faceCascadeClassifier.load(filePath);
    if (ret) {
        LOGE("人脸识别级联分类器加载成功");
    } else {
        LOGE("人脸识别级联分类器加载失败");
    }
    env->ReleaseStringUTFChars(file_path, filePath);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_face_FaceDetection_trainingPattern(JNIEnv *env, jobject thiz) {
    // 训练样本，这一步是在数据采集做的
    // train it
    vector<Mat> faces;
    vector<int> labels;

    // 样本比较少
    for (int i = 1; i <= 5; ++i) {
        for (int j = 1; j <= 5; ++j) {
            Mat face = imread(format("/storage/emulated/0/s%d/%d.pgm", i, j), 0);
            if (face.empty()) {
                LOGE("face mat is empty");
                continue;
            }
            // 将人脸的数据集图片大小设置为48
            resize(face, face, Size(IMAGE_SIZE, IMAGE_SIZE));
            faces.push_back(face);
            labels.push_back(i);
        }
    }

    for (int i = 1; i <= 7; ++i) {
        // 读取的是灰度图，灰度图的通道是单通道 通道数为1
        Mat gray = imread(format("/storage/emulated/0/face_%d.jpg", i), IMREAD_GRAYSCALE);
        if (gray.empty()) {
            LOGE("face mat is empty");
            continue;
        }
//        int channels = face.channels();
//        LOGD("%d", channels);
//        Mat gray;
//        cvtColor(face, gray, COLOR_BGR2GRAY);
        std::vector<Rect> faces2;
        faceCascadeClassifier.detectMultiScale(gray, faces2, 1.1, 3, 0 | CASCADE_SCALE_IMAGE,
                                               Size(30, 30));
        int b = 0;
        for (auto &item: faces2) {
            // roi:region of interest
            Mat m = gray(item).clone();
            equalizeHist(m, m);
            imwrite(format("/storage/emulated/0/gray%d.jpg", b), m);
            resize(m, m, Size(IMAGE_SIZE, IMAGE_SIZE));
            faces.push_back(m);
            labels.push_back(11);
            b++;
        }
    }
    // 训练方法
//    Ptr<BasicFaceRecognizer> model = EigenFaceRecognizer::create();
    // 采集了八张， 同一个人label一样
    model->train(faces, labels);
    model->save("/storage/emulated/0/face_simley_pattern.xml");// 存的是处理的特征数据
    LOGE("样本训练成功");
}



extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_face_FaceDetection_loadPattern(JNIEnv *env, jobject thiz,
                                                          jstring pattern_path) {
    const char *patternPath = env->GetStringUTFChars(pattern_path, 0);
    // 加载样本数据，Error:(142, 12) error: no matching member function for call to 'load' 怎么搞？
    FileStorage fs(patternPath, FileStorage::READ);
    // 报错，没有提示
    if (!fs.isOpened()) {
        LOGE("训练样本文件不存在");
        return;
    }
    // 抛个 java 异常
    FileNode fn = fs.getFirstTopLevelNode();
    model->read(fn);
    env->ReleaseStringUTFChars(pattern_path, patternPath);
    LOGE("训练样本加载成功");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_face_FaceDetection_loadEyeCascade(JNIEnv *env, jobject thiz,
                                                             jstring file_path) {
    const char *filePath = env->GetStringUTFChars(file_path, nullptr);
    bool ret = eyeCascadeClassifier.load(filePath);
    if (ret) {
        LOGE("眼睛识别级联分类器加载成功");
    } else {
        LOGE("眼睛识别级联分类器加载失败");
    }
    env->ReleaseStringUTFChars(file_path, filePath);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_simley_ndk_1day78_bandcard_BankCardRecognition_cardOcr(JNIEnv *env, jobject thiz,
                                                                jobject bitmap) {
    Mat mat;
    cv_helper::bitmap2mat(env, bitmap, mat);

    //  轮廓增强（梯度增强）
//     Rect card_area;
//     co1::find_card_area(mat,card_area);
    // 对我们过滤到的银行卡区域进行裁剪
    // Mat card_mat(mat,card_area);
    // imwrite("/storage/emulated/0/ocr/card_n.jpg",card_mat);

    // 截取到卡号区域
//    Rect bank_card_area;
//    co1::find_card_area(mat, bank_card_area);
//    imwrite("/storage/emulated/0/ocr/bank_card_area.jpg", mat(bank_card_area));
    Rect card_number_area;
    co1::find_card_number_area(mat, card_number_area);
    imwrite("/storage/emulated/0/ocr/slice_card_number.jpg", mat(card_number_area));
    Mat card_number_mat(mat, card_number_area);
    bool ret = imwrite("/storage/emulated/0/ocr/card_number_n.jpg", card_number_mat);
    if (ret) {
        LOGE("截取银行卡区域图片成功");
    } else {
        LOGE("截取银行卡区域图片失败");
    }
    vector<Mat> numbers;
    // 获取银行卡卡号
    // 识别到部分银行卡卡号
    co1::find_card_numbers(card_number_mat, numbers);
    int i = 1;
    for (const auto &item: numbers) {
        imwrite(format("/storage/emulated/0/mat%d.jpg", i), item);
        i++;
    }
    return env->NewStringUTF("123123");
}