//
// Created by Simle Y on 2022/10/25.
//

#include "utils.h"

void mosaicFace(Mat mat) {
    // 获取图片高度
    int src_width = mat.cols;
    int src_height = mat.rows;

    // 省略人脸识别
    int rows_s = src_height / 6;
    int cols_s = src_width / 6;
    int rows_e = src_height * 5 / 6;
    int cols_e = src_width * 5 / 6;
    int size = 15;
    for (int rows = rows_s; rows < rows_e; rows += size) {
        for (int cols = cols_s; cols < cols_e; cols += size) {
            int pixels = mat.at<int>(rows, cols);
            for (int m_rows = 1; m_rows < size; ++m_rows) {
                for (int m_cols = 1; m_cols < size; ++m_cols) {
                    mat.at<int>(rows + m_rows, cols + m_cols) = pixels;
                }
            }
        }
    }
}

vector<RotatedRect> findTextAreas(const Mat &input) {
    //Dilate the image
    auto kernel1 = getStructuringElement(MORPH_RECT, Size(30, 9));
    auto kernel2 = getStructuringElement(MORPH_RECT, Size(24, 6));
    Mat dilated1;
    dilate(input, dilated1, kernel2, cv::Point(-1, -1), 1);
    imwrite("/storage/emulated/0/ocr/dilated1.jpg", dilated1);
    Mat eroded;
    cv::erode(dilated1, eroded, kernel1, cv::Point(-1, -1), 1);
    imwrite("/storage/emulated/0/ocr/eroded.jpg", eroded);
    Mat dilated2;
    dilate(eroded, dilated2, kernel2, cv::Point(-1, -1), 3);
    imwrite("/storage/emulated/0/ocr/dilated2.jpg", eroded);

    //Find all image contours
    vector<vector<Point>> contours;
    findContours(dilated2, contours, RETR_TREE, CHAIN_APPROX_SIMPLE);
    drawContours(dilated2, contours, 0, Scalar(0, 0, 255), 2);
    imwrite("/storage/emulated/0/ocr/drawContours.jpg", input);
    // For each contour
    vector<RotatedRect> areas;
    double area;
    double epsilon;
    Mat approx;
    for (const auto &contour: contours) {
        // 计算该轮廓的面积
        area = contourArea(contour);
        // 面积小的都过滤掉
        if (area < 1000) {
            continue;
        }
        // 轮廓近似，作用很小
        epsilon = 0.001 * arcLength(contour, true);
        approxPolyDP(contour, approx, epsilon, true);

        //Find it's rotated rect
        auto box = minAreaRect(contour);
        // box是四个点的坐标
//        Mat box;
//        cv::boxPoints(rect, box);
//        box = np.int0(box)
        // Discard very small boxes
        if (box.size.width < 20 || box.size.height < 20)
            continue;

        //Discard squares shaped boxes and boxes
        //higher than larger
        double proportion;
        if (box.angle < -45.0) {
            proportion = box.size.height / box.size.width;
        } else {
            proportion = box.size.width / box.size.height;
        }

        if (proportion < 2)
            continue;

        //Add the box
        areas.push_back(box);
    }
    return areas;
}

Mat binarize(Mat input) {
    //Uses otsu to threshold the input image
    Mat binaryImage;
    cvtColor(input, input, COLOR_BGR2GRAY);
    threshold(input, binaryImage, 0, 255, THRESH_OTSU + THRESH_BINARY);

    //Count the number of black and white pixels
    int white = countNonZero(binaryImage);
    int black = binaryImage.size().area() - white;

    //If the image is mostly white (white background), invert it
    return white < black ? binaryImage : ~binaryImage;
}

Mat deskewAndCrop(Mat input, const RotatedRect &box) {
    double angle = box.angle;
    auto size = box.size;

    // Adjust the box angle
    if (angle < -45.0) {
        angle += 90.0;
        std::swap(size.width, size.height);
    }

    // Rotate the text according to the angle
    auto transform = getRotationMatrix2D(box.center, angle, 1.0);
    Mat rotated;
    warpAffine(input, rotated, transform, input.size(), INTER_CUBIC);

    //Crop the result
    Mat cropped;
    getRectSubPix(rotated, size, box.center, cropped);
    copyMakeBorder(cropped, cropped, 10, 10, 10, 10, BORDER_CONSTANT, Scalar(0));
    return cropped;
}