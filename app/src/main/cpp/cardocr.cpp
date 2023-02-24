//
// Created by hcDarren on 2018/7/28.
//

#include "cardocr.h"
#include <vector>
#include <android/log.h>
#include "iostream"
#include "dirent.h"

using namespace std;

// 语法基础
int co1::findCardArea(const Mat &mat, Rect &area) {
    // 首先降噪
    Mat blur;
    GaussianBlur(mat, blur, Size(5, 5), BORDER_DEFAULT, BORDER_DEFAULT);

    // 边缘梯度增强（保存图片）x,y 增强
    Mat grad_x, grad_Y;
    Scharr(blur, grad_x, CV_32F, 1, 0);
    Scharr(blur, grad_Y, CV_32F, 0, 1);
    Mat abs_gard_x, abs_gard_y;
    convertScaleAbs(grad_x, abs_gard_x);
    convertScaleAbs(grad_Y, abs_gard_y);
    Mat grad;
    addWeighted(abs_gard_x, 0.5, abs_gard_y, 0.5, 0, grad);

    // 灰度图
    Mat gray;
    cvtColor(grad, gray, COLOR_BGRA2GRAY);
    // 二值化，筛选轮廓
    Mat binary;
    threshold(gray, binary, 40, 255, THRESH_BINARY);
//    GaussianBlur(gray, gray, Size(GAUSSIANBLUE, GAUSSIANBLUR),0, 0, BORDER_DEFAULT);

    vector<vector<Point> > contours;
    findContours(binary, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
    for (auto &contour: contours) {
        Rect rect = boundingRect(contour);
        // 过滤轮廓
        if (rect.width > mat.cols / 2 && rect.width != mat.cols && rect.height > mat.rows / 2) {
            // 银行卡区域的宽高必须大于图片的一半
            area = rect;
            break;
        }
    }

    // 没有对返回值成功失败做处理

    // 释放资源 有没有动态开辟内存，有没有 new 对象
    // mat 数据类提供了释放函数，一般要调用
    blur.release();
    grad_x.release();
    grad_Y.release();
    abs_gard_x.release();
    abs_gard_y.release();
    grad.release();
    binary.release();

    return 0;
}

int co1::findCardNumberArea(const Mat &mat, Rect &area) {
    // 有两种方式，一种是精确截取，找到银联区域通过大小比例精确的截取 （70%）
    // 粗略的截取，截取高度 1/2 - 3/4 ， 宽度 1/12  11/12    90%  （采用）
    // 万一找不到 ，可以手动的输入和修改
    area.x = mat.cols / 12;
    area.y = mat.rows / 2;
    area.width = mat.cols * 5 / 6;
    area.height = mat.rows / 4;
    return 0;
}

int co1::findCardNumbers(const Mat &src_img, std::vector<Mat> &ret) {
    Mat gray_img;
    // 转成灰度图
    cvtColor(src_img, gray_img, COLOR_BGRA2GRAY);
    // THRESH_OTSU THRESH_TRIANGLE自己去找合适的值 ，这一步非常关键
    Mat binary;
    // 二值化，也就是转成黑白图
    threshold(gray_img, binary, 39, 255, THRESH_BINARY);
    imwrite("/storage/emulated/0/ocr/card_number_binary_n.jpg", binary);
    // 降噪过滤，创建一个 3*3的滤波器（卷积核）
    Mat kernel = getStructuringElement(MORPH_RECT, Size(7, 7));
    // 开运算（先腐蚀，再膨胀）
    // 腐蚀
    morphologyEx(binary, binary, MORPH_CLOSE, kernel);
    // 膨胀
//    dilate(binary, binary, kernel);
    // 去掉干扰过滤填充，找数字就是轮廓查询 （card.io 16 位 ，19 位）
    // 查找轮廓 白色轮廓 binary  黑色的背景，白色的数字
    // 取反 白黑 -> 黑白
    Mat binary_not = binary.clone();
    bitwise_not(binary_not, binary_not);
    imwrite("/storage/emulated/0/ocr/card_number_binary_not_n.jpg", binary_not);
    vector<vector<Point> > contours;
    findContours(binary_not, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
    int mat_area = src_img.rows * src_img.cols;
    int min_h = src_img.rows / 4;
    for (int i = 0; i < contours.size(); i++) {
        Rect rect = boundingRect(contours[i]);
        // 多个条件，面积太小的过滤 1/100
        int area = rect.width * rect.height;
        if (area < mat_area / 200) {
            // 小面积填充为 白色背景
            drawContours(binary, contours, i, Scalar(255), -1);
        } else if (rect.height < min_h) {
            drawContours(binary, contours, i, Scalar(255), -1);
        }
    }

    imwrite("/storage/emulated/0/ocr/card_number_binary_noise_n.jpg", binary);

    // 截取每个数字的轮廓 binary(没噪音) 不行，binary_not(有噪音)
    binary.copyTo(binary_not);
    bitwise_not(binary_not, binary_not);// 没有噪音的 binary_not

    // 先要把 Rect 存起来 , 查找有可能出现循序错乱，还有可能出现粘连字符
    contours.clear();
    findContours(binary_not, contours, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);
    Rect rects[contours.size()];
    // 白色的图片，单颜色
    Mat contours_mat(binary.size(), CV_8UC1, Scalar(255));
    // 判断粘连字符
    int min_w = src_img.cols;
    for (int i = 0; i < contours.size(); i++) {
        rects[i] = boundingRect(contours[i]);
        drawContours(contours_mat, contours, i, Scalar(0), 1);
        min_w = min(rects[i].width, min_w);
    }


    imwrite("/storage/emulated/0/ocr/card_number_contours_mat_n.jpg", contours_mat);

    // 进行排序 冒泡（最简单） 自己搞些数据做演示，在自己图板多画画
    for (int i = 0; i < contours.size() - 1; ++i) {
        for (int j = 0; j < contours.size() - i - 1; ++j) {
            if (rects[j].x > rects[j + 1].x) {
                swap(rects[j], rects[j + 1]);
            }
        }
    }

    // 裁剪，裁剪 src_img(彩色图) 裁剪，数据量太大
    ret.clear();
    for (int i = 0; i < contours.size(); i++) {
        // 最小宽度的两倍
        if (rects[i].width >= min_h * 2) {
            // 处理粘连字符
            Mat _mat(contours_mat, rects[i]);
            int cols_pos = co1::findSplitColsPos(_mat);
            // 左右两个数字都存进去
            Rect rect_left(0, 0, cols_pos - 1, _mat.rows);
            ret.emplace_back(_mat, rect_left);
            Rect rect_right(cols_pos, 0, _mat.cols, _mat.rows);
            ret.emplace_back(_mat, rect_right);
        } else {
            Mat number(contours_mat, rects[i]);
            ret.push_back(number);
            // 保存数字图片
            char name[50];
            sprintf(name, "/storage/emulated/0/ocr/card_number_%d.jpg", i);
            imwrite(name, number);
        }
    }

    gray_img.release();
    binary.release();
    binary_not.release();
    contours_mat.release();

    return 0;
}

// 反复看一看敲一敲
int co1::findSplitColsPos(Mat mat) {
    // 怎么处理粘连 ，对中心位置的左右 1/4 扫描，记录最少的黑色像素点的这一列的位置，
    // 就当做我们的字符串的粘连位置
    int mx = mat.cols / 2;
    int height = mat.rows;
    // 围绕中心左右扫描 1/4
    int start_x = mx - mx / 2;
    int end_x = mx + mx / 2;
    // 字符的粘连位置
    int cols_pos = mx;
    // 获取像素子 0 ，255
    int c = 0;
    // 最小的像素值
    int min_h_p = mat.rows;

    for (int col = start_x; col < end_x; ++col) {
        int total = 0;
        for (int row = 0; row < height; ++row) {
            // 获取像素点
            c = mat.at<Vec3b>(row, col)[0];// 单通道
            if (c == 0) {
                total++;
            }
        }
        if (total < min_h_p) {
            min_h_p = total;
            cols_pos = col;
        }
    }
    return cols_pos;
}