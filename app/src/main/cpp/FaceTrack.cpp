//
// Created by Administrator on 2023-02-05.
//

#include "FaceTrack.h"

// point_detector 人脸关键点模型 关联起来

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

void FaceTrack::detector(Mat src, vector<Rect2f> &rects) {
    vector<Rect> faces;
    // src :灰度图（去除 不需要的色彩信息）
    tracker->process(src); // 处理灰度图(OpenCV的东西，灰度，色彩 影响我们人脸追踪)
    tracker->getObjects(faces); // 得到人脸框框的Rect - OpenCV的东西
    if (!faces.empty()) { // 判断true，说明非零，有人脸
        Rect face = faces[0]; // 有人脸就去第一个人脸，我没有去管，多个人脸了哦
        // 然后把跟踪出来的这个人脸，保存到rects里面去
        rects.emplace_back(face.x, face.y, face.width, face.height);

        // TODO 根据前面的OpenCV人脸最终成果， 做 人脸关键点定位
        seeta::ImageData image_data(src.cols, src.rows); // image_data就是图像数据
        image_data.data = src.data; // (人脸的信息 要送去检测的) = (把待检测图像)

        // 人脸追踪框 信息绑定  人脸关键点定位
        seeta::FaceInfo face_info; // 人脸的信息 要送去检测的
        seeta::Rect bbox; // 人脸框框的信息
        bbox.x = face.x;           // 把人脸信息的x 给 face_info
        bbox.y = face.y;           // 把人脸信息的y 给 face_info
        bbox.width = face.width;   // 把人脸信息的width 给 face_info
        bbox.height = face.height; // 把人脸信息的height 给 face_info
        face_info.bbox = bbox;     // 把人脸信息的bbox 给 face_info

        seeta::FacialLandmark points[5]; // 特征点的检测，固定了5个点

        // 执行采集出 五个点
        faceAlignment->PointDetectLandmarks(image_data, face_info, points);

        // 把五个点 转换 ，因为第二个参数需要 Rect2f
        for (auto &point: points) { // 为何不需要宽和高，只需要保存点就够了
            rects.emplace_back(point.x, point.y, 0, 0);
        }
    }
}


extern "C"
JNIEXPORT jlong JNICALL
Java_com_simley_ndk_1day78_opengl2_face_FaceTrack_native_1create(JNIEnv *env, jobject thiz,
                                                                 jstring model_, jstring seeta_) {
    const char *model = env->GetStringUTFChars(model_, 0);
    const char *seeta = env->GetStringUTFChars(seeta_, 0);

    auto *faceTrack = new FaceTrack(model, seeta);

    env->ReleaseStringUTFChars(model_, model);
    env->ReleaseStringUTFChars(seeta_, seeta);
    return reinterpret_cast<jlong>(faceTrack);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_opengl2_face_FaceTrack_native_1start(JNIEnv *env, jobject thiz,
                                                                jlong self) {
    if (self == 0) {
        return;
    }
    auto *faceTrack = reinterpret_cast<FaceTrack *>(self);
    faceTrack->startTracking();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_opengl2_face_FaceTrack_native_1stop(JNIEnv *env, jobject thiz,
                                                               jlong self) {
    if (self == 0) {
        return;
    }
    auto *faceTrack = reinterpret_cast<FaceTrack *>(self);
    faceTrack->stopTracking();
    delete faceTrack;
}

/**
 * 执行真正的人脸追踪 + 人脸关键点定位 的核心函数
 * @param env
 * @param thiz
 * @param self
 * @param data_
 * @param camera_id
 * @param width
 * @param height
 * @return
 */
extern "C"
JNIEXPORT jobject JNICALL
Java_com_simley_ndk_1day78_opengl2_face_FaceTrack_native_1detector(JNIEnv *env, jobject thiz,
                                                                   jlong self, jbyteArray data_,
                                                                   jint camera_id, jint width,
                                                                   jint height) {
    if (self == 0) {
        return nullptr;
    }

    jbyte *data = env->GetByteArrayElements(data_, 0);
    auto *faceTrack = reinterpret_cast<FaceTrack *>(self);  // 通过地址反转CPP对象


    // OpenCV旋转数据操作
    Mat src(height + height / 2, width, CV_8UC1, data); // 摄像头数据data 转成 OpenCv的 Mat
    imwrite("/sdcard/camera.jpg", src); // 做调试的时候用的（方便查看：有没有摆正，有没有灰度化 等）
    cvtColor(src, src, CV_YUV2RGBA_NV21); // 把YUV转成RGBA
    if (camera_id == 1) { // 前摄
        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE); // 逆时针90度
        flip(src, src, 1); // y 轴 翻转（镜像操作）
    } else {  // 后摄
        rotate(src, src, ROTATE_90_CLOCKWISE);
    }

    // OpenCV基础操作
    cvtColor(src, src, COLOR_RGBA2GRAY); // 灰度化
    equalizeHist(src, src); // 均衡化处理（直方图均衡化，增强对比效果）
    vector<Rect2f> rects;
    faceTrack->detector(src, rects); // 送去定位，要去做人脸的检测跟踪了
    env->ReleaseByteArrayElements(data_, data, 0);

    // rects 他已经有丰富的人脸框框的信息，接下来就是，关键点定位封装操作Face.java

    // TODO 注意：上面的代码执行完成后，就拿到了 人脸检测的成果 放置在rects中

    // C++ 反射 实例化 Face.java 并且保证 Face.java有值

    int imgWidth = src.cols; // 构建 Face.java的 int imgWidth; 送去检测图片的宽
    int imgHeight = src.rows; // 构建 Face.java的 int imgHeight; 送去检测图片的高
    int ret = rects.size(); // 如果有一个人脸，那么size肯定大于0
    if (ret) { // 注意：有人脸，才会进if
        jclass clazz = env->FindClass("com/simley/ndk_day78/opengl2/face/Face");
        jmethodID construct = env->GetMethodID(clazz, "<init>", "(IIII[F)V");
        // int width, int height,int imgWidth,int imgHeight, float[] landmark
        int size = ret * 2; // 乘以2是因为，有x与y， 其实size===2，因为rects就一个人脸

        // 构建 Face.java的 float[] landmarks;
        jfloatArray floatArray = env->NewFloatArray(size);
        for (int i = 0, j = 0; i < size; ++j) {  // 前两个就是人脸的x与y
            float f[2] = {rects[j].x, rects[j].y};
            env->SetFloatArrayRegion(floatArray, i, 2, f);
            i += 2;
        }

        Rect2f faceRect = rects[0];
        int faceWidth = faceRect.width; // 构建 Face.java的 int width; 保存人脸的宽
        int faceHeight = faceRect.height; // 构建 Face.java的 int height; 保存人脸的高
        // 实例化Face.java对象，都是前面JNI课程的基础
        jobject face = env->NewObject(clazz, construct, faceWidth, faceHeight, imgWidth, imgHeight,
                                      floatArray);
        rectangle(src, faceRect, Scalar(0, 0, 255)); // OpenCV内容，你们之前学过的
        for (int i = 1; i < ret; ++i) { // OpenCV内容，你们之前学过的
            circle(src, Point2f(rects[i].x, rects[i].y), 5, Scalar(0, 255, 0));
        }
        imwrite("/sdcard/src.jpg", src); // 做调试的时候用的（方便查看：有没有摆正，有没有灰度化 等）
        return face; // 返回 jobject == Face.java（已经有值了，有人脸所有的信息了，那么就可以开心，放大眼睛）
    }

    src.release(); // Mat释放工作
    return nullptr;
}






