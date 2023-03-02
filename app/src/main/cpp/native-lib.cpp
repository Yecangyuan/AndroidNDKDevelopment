#include <jni.h>
#include <string>
#include "YeLog.h"
#include <vector>
#include "opencv2/opencv.hpp"
#include "opencv2/face.hpp"
#include <android/log.h>

#include "opencv2/objdetect/objdetect.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"
#include "cv_helper.h"
#include "cardocr.h"
#include "opencv2/dnn.hpp"

using namespace cv;
using namespace std;
using namespace face;
using namespace dnn;

#define IMAGE_SIZE 24

// 加载人脸识别的级联分类器
CascadeClassifier faceCascadeClassifier;
// 加载眼睛识别的级联分类器
CascadeClassifier eyeCascadeClassifier;
// 人脸识别模型
Ptr<BasicFaceRecognizer> model = EigenFaceRecognizer::create();

void detectEyes(const Mat *src, const Rect &face, const Mat &faceROI);

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
    // faceCascadeClassifier.detectMultiScale(grayMat, faces, 1.1, 3, 0, Size(width / 4, height / 4));

    // 没有人脸 或者 有多张人脸
    if (faces.size() != 1) {
        // 打上马赛克
        // mosaicFace(*src);
        return;
    }

    for (const auto &face: faces) {
//        Point center(face.x + face.width * 0.5, face.y + face.height * 0.5);
//        ellipse(*src, center, Size(face.width * 0.5, face.height * 0.5), 0, 0, 360,
//                Scalar(255, 0, 255), 4, 8, 0);
        rectangle(*src, face, Scalar(255, 0, 0, 255), 4, LINE_AA);
        Mat faceROI = (*src)(face).clone();

        detectEyes(src, face, faceROI);

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

void detectEyes(const Mat *src, const Rect &face, const Mat &faceROI) {
    vector<Rect> eyes;

    // 在每张人脸上检测双眼
    eyeCascadeClassifier.detectMultiScale(faceROI, eyes, 1.1, 2, 0 | CASCADE_SCALE_IMAGE,
                                          Size(30, 30));

    for (const auto &eye: eyes) {
        Point center(face.x + eye.x + eye.width * 0.5, face.y + eye.y + eye.height * 0.5);
        int radius = cvRound((eye.width + eye.height) * 0.25);
        circle(*src, center, radius, Scalar(255, 0, 0, 255), 4, LINE_AA, 0);
    }
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
//     co1::findCardArea(mat,card_area);
    // 对我们过滤到的银行卡区域进行裁剪
    // Mat card_mat(mat,card_area);
    // imwrite("/storage/emulated/0/ocr/card_n.jpg",card_mat);

    // 截取到卡号区域
//    Rect bank_card_area;
//    co1::findCardArea(mat, bank_card_area);
//    imwrite("/storage/emulated/0/ocr/bank_card_area.jpg", mat(bank_card_area));
    Rect card_number_area;
    co1::findCardNumberArea(mat, card_number_area);
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
    co1::findCardNumbers(card_number_mat, numbers);
    int i = 1;
    for (const auto &item: numbers) {
        imwrite(format("/storage/emulated/0/mat%d.jpg", i), item);
        i++;
    }
    return env->NewStringUTF("123123");
}

Net _net;
Net face_net;

// 初始化DNN
// 初始化置信阈值
float confidenceThreshold;
double inScaleFactor;
int inWidth;
int inHeight;
Scalar meanVal;
vector<vector<float>> face_data;   // 每张人脸的特征向量信息
vector<string> labels;     // 人脸对应名字

int s = 1;

void recognize_face(Mat &face, Net net, vector<float> &fv) {
    Mat blob = blobFromImage(face, 1 / 255.0, Size(96, 96), Scalar(0, 0, 0), true, false);
    imwrite(format("/storage/emulated/0/source%d.jpg", s), face);
    imwrite(format("/storage/emulated/0/training%d.jpg", s++), blob);
    net.setInput(blob);
    Mat probMat = net.forward();
    Mat vec = probMat.reshape(1, 1);

    LOGD("vec宽%d,vec高%d", vec.cols, vec.rows);
    for (int i = 0; i < vec.cols; i++) {
        fv.push_back(vec.at<float>(0, i));
    }
}

float compare(vector<float> &fv1, vector<float> &fv2) {
    float dot = 0;
    float sum2 = 0;
    float sum3 = 0;
    for (int i = 0; i < fv1.size(); i++) {
        dot += fv1[i] * fv2[i];
        sum2 += pow(fv1[i], 2);
        sum3 += pow(fv2[i], 2);
    }
    float norm = sqrt(sum2) * sqrt(sum3);
    float similary = dot / norm;
    float dist = acos(similary) / CV_PI;
    return dist;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_face_FaceDetection_loadDNNFaceDetector(JNIEnv *env, jobject thiz,
                                                                  jstring model_bin,
                                                                  jstring model_des) {
    string sbinary = env->GetStringUTFChars(model_bin, 0);
    string sdesc = env->GetStringUTFChars(model_des, 0);

    confidenceThreshold = 0.6;
    inScaleFactor = 0.5;
    inWidth = 300;
    inHeight = 300;
    meanVal = Scalar(104.0, 177.0, 123.0);

    _net = dnn::readNetFromTensorflow(sbinary, sdesc);
    _net.setPreferableBackend(dnn::DNN_BACKEND_OPENCV);
    _net.setPreferableTarget(dnn::DNN_TARGET_CPU);

    if (!_net.empty()) {
        LOGE("DNN人脸检测模型加载成功");
    } else {
        LOGE("DNN人脸检测模型加载失败");
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_face_FaceDetection_faceDetectionDNN(JNIEnv *env, jobject thiz,
                                                               jlong native_obj) {
    Mat *src = reinterpret_cast<Mat *>(native_obj);
    if (src->empty()) {
        LOGE("--(!) 摄像头未捕获到图像帧 -- return!");
        return;
    }
    int width = src->cols;
    int height = src->rows;
    // 修改通道数
    if ((*src).channels() == 4) {
        cvtColor(*src, *src, COLOR_BGRA2BGR);
    }

//    Mat grayMat;
//    // 修改通道数
//    if ((*src).channels() == 4) {
//        cvtColor(*src, grayMat, COLOR_BGRA2BGR);
//    }
//    LOGD("grayMat通道数为%d", grayMat.channels());

    // 将图像传递给模型进行前向推理
    Mat inputBlob = dnn::blobFromImage(*src, inScaleFactor,
                                       Size(inWidth, inHeight), meanVal, false, false);

    _net.setInput(inputBlob, "data");

    // 人脸检测
    Mat detection = _net.forward("detection_out");

    Mat detectionMat(detection.size[2], detection.size[3], CV_32F, detection.ptr<float>());

//    std::vector<Rect> faces;
    int maxDetections = 1;  // 最多只检测一个人
    for (int i = 0; i < detectionMat.rows; i++) {
        // 置值度获取
        float confidence = detectionMat.at<float>(i, 2);
        // 如果大于阈值说明检测到人脸
        if (confidence > confidenceThreshold) {
            int xLeftBottom = static_cast<int>(detectionMat.at<float>(i, 3) * width);
            int yLeftBottom = static_cast<int>(detectionMat.at<float>(i, 4) * height);
            int xRightTop = static_cast<int>(detectionMat.at<float>(i, 5) * width);
            int yRightTop = static_cast<int>(detectionMat.at<float>(i, 6) * height);

            Rect rect((int) xLeftBottom, (int) yLeftBottom, (int) (xRightTop - xLeftBottom),
                      (int) (yRightTop - yLeftBottom));

//            faces.emplace_back(rect);
//            LOGD("detectionMat的宽%d,detectionMat的高%d", detectionMat.cols, detectionMat.rows);

            // 0 <= roi.x && 0 <= roi.width && roi.x + roi.width <= m.cols && 0 <= roi.y && 0 <= roi.height && roi.y + roi.height <= m.rows
            rectangle(*src, rect, Scalar(255, 0, 0, 255), 4, LINE_AA);
            LOGD("rect的x坐标：%d，rect的y坐标：%d，rect的宽：%d,rect的高：%d", rect.x, rect.y,
                 rect.width, rect.height);
            // 对roi部分进行范围检测，如果小于零 或者 大于原图的大小，则直接break
            if (rect.x < 0 || rect.y < 0 || rect.width < 0 || rect.height < 0 ||
                rect.x + rect.width > src->cols || rect.y + rect.height > src->rows) {
                break;
            }
//            Mat faceROI = (*src)(rect);
//            // 人脸比对与识别
//            vector<float> curr_fv;
//            // 计算当前人脸的特征向量
//            recognize_face(faceROI, face_net, curr_fv);
//
//            // 遍历计算与采样图片余弦相似度最小的人脸照片 并给出索引
//            float minDist = 10;
//            int index = 0;
//            for (int j = 0; j < face_data.size(); j++) {
//                float dist = compare(face_data[j], curr_fv);
//                if (minDist > dist) {
//                    minDist = dist;
//                    index = j;
//                }
//            }
//            if (minDist < 0.19 && index >= 0) { // 阈值限定  显示人名
//                // 识别到了自己
//                putText(*src, format("Simley:label=%s", labels[i].c_str()),
//                        Point(rect.x + 20, rect.y - 20),
//                        HersheyFonts::FONT_HERSHEY_TRIPLEX, 1, Scalar(255, 0, 0, 255), 1,
//                        LINE_AA);
//            } else {
//                // 不是自己
//                putText(*src, format("UnKnow:label=%s", labels[i].c_str()),
//                        Point(rect.x + 20, rect.y - 20),
//                        HersheyFonts::FONT_HERSHEY_TRIPLEX, 1, Scalar(255, 0, 0, 255), 1, LINE_AA);
//            }
//
//            // 限制检测结果的数量
//            if (--maxDetections <= 0) {
//                break;
//            }
        }
    }

}

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_face_FaceDetection_loadDNNFaceRecognition(JNIEnv *env, jobject thiz,
                                                                     jstring face_net_model) {
    const string faceModel = env->GetStringUTFChars(face_net_model, 0);
    face_net = readNetFromTorch(faceModel);      // 人脸识别模型

    face_net.setPreferableBackend(DNN_BACKEND_OPENCV);
    face_net.setPreferableTarget(DNN_TARGET_CPU);

    if (!face_net.empty()) {
        LOGE("DNN人脸识别模型加载成功");
    } else {
        LOGE("DNN人脸识别模型加载失败");
    }

}

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_face_FaceDetection_trainingDNNPattern(JNIEnv *env, jobject thiz) {
    // 载入采集的样本数据
    vector<string> faces;      // 采集的人脸图片名称 （含有路径）

    // 分析每张照片  计算每张人脸照片的特征向量 为每张照片对应名称
    for (int i = 1; i <= 7; ++i) {
        vector<float> fv;
        Mat sample = imread(format("/storage/emulated/0/face_%d.jpg", i), IMREAD_COLOR);
        if (sample.empty()) {
            LOGE("face mat is empty");
            continue;
        }
        recognize_face(sample, face_net, fv);
        LOGD("sample通道数为%d", sample.channels());

        face_data.push_back(fv);

        labels.emplace_back("YCY");
    }
}
