package com.simley.ndk_day78.opengl2.face;


import java.util.Arrays;

/**
 * 人脸的JavaBean，封装了人脸的信息集
 */
public class Face {

    /**
     *  人脸框的x和y，不等于 width，height，所以还是单独定义算了（没啥关联）
     *    float[] landmarks 细化后如下：12个元素
     *      0下标（保存：人脸框的 x）
     *      1下标（保存：人脸框的 y）
     *
     *      2下标（保存：左眼x）
     *      3下标（保存：左眼y）
     *
     *      4下标（保存：右眼x）‘
     *      5下标（保存：右眼y）
     *
     *      6下标（保存：鼻尖x）
     *      7下标（保存：鼻尖y）
     *
     *      7下标（保存：左边嘴角x）
     *      8下标（保存：左边嘴角y）
     *
     *      9下标（保存：右边嘴角x）
     *     10下标（保存：右边嘴角y）
     */
    public float[] landmarks;

    public int width;        // 保存人脸的框 的宽度
    public int height;       // 保存人脸的框 的高度
    public int imgWidth;    // 送去检测的所有宽 屏幕
    public int imgHeight;   // 送去检测的所有高 屏幕

    public Face(int width, int height, int imgWidth,int imgHeight, float[] landmarks) {
        this.landmarks = landmarks;
        this.width = width;
        this.height = height;
        this.imgWidth = imgWidth;
        this.imgHeight = imgHeight;
    }

    @Override
    public String toString() {
        return "Face{" +
                "landmarks=" + Arrays.toString(landmarks) +
                ", width=" + width +
                ", height=" + height +
                ", imgWidth=" + imgWidth +
                ", imgHeight=" + imgHeight +
                '}';
    }
}
