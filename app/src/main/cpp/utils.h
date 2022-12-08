//
// Created by Simle Y on 2022/10/25.
//

#ifndef NDK_DAY78_UTILS_H
#define NDK_DAY78_UTILS_H

#include "opencv2/core/mat.hpp"
#include "opencv2/opencv.hpp"
#include "vector"

using namespace cv;
using namespace std;

/**
 * 给人脸打上马赛克
 * @param mat
 */
void mosaicFace(Mat mat);

/**
 * 对图像进行二值化
 * 也就是将图像转成黑白两色的图像
 * @param input
 * @return
 */
Mat binarize(Mat input);

Mat deskewAndCrop(Mat input, const RotatedRect &box);

/**
 * 找出文本区域
 * @param input
 * @return
 */
vector<RotatedRect> findTextAreas(const Mat &input);


#endif //NDK_DAY78_UTILS_H
