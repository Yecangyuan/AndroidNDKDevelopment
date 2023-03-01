package com.simley.ndk_day78.face;

import org.opencv.core.Mat;

public class FaceDetection {

    /**
     * 检测人脸并保存人脸信息
     *
     * @param mat
     */
    public void faceDetection(Mat mat) {
        faceDetection(mat.nativeObj);
    }

    /**
     * 加载人脸识别的分类器文件
     *
     * @param filePath
     */
    public native void loadFaceCascade(String filePath);

    /**
     * 加载眼睛的分类器文件
     *
     * @param filePath
     */
    public native void loadEyeCascade(String filePath);

    public native void loadDNNFaceDetector(String modelBin, String modelDes);

    public native void faceDetectionDNN(long nativeObj);

    /**
     * 人脸识别
     *
     * @param nativeObj
     */
    private native void faceDetection(long nativeObj);

    /**
     * 训练模型
     */
    public native void trainingPattern();

    /**
     * 加载样本数据
     *
     * @param patternPath
     */
    public native void loadPattern(String patternPath);

}
