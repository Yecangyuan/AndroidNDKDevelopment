//
// Created by Simle Y on 2022/12/28.
//

#ifndef NDK_DAY78_YEVIDEO_H
#define NDK_DAY78_YEVIDEO_H

#include "YEPlayStatus.h"
#include "YECallJava.h"
#include "YEQueue.h"
#include "YEAudio.h"

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavutil/time.h"
#include "libavutil/imgutils.h"
#include "libswscale/swscale.h"
};

class YEVideo {
public:
    YEQueue *queue = NULL;
    int streamIndex = -1;
    AVCodecContext *avCodecContext = NULL;
    AVCodecParameters *codecParameters = NULL;
    YEPlayStatus *playStatus = NULL;
    YECallJava *callJava = NULL;
    pthread_mutex_t pthreadMutex;
    pthread_t pthreadPlay;

    double clock = 0;
    // 主要与音频的差值
    double delayTime = 0;
    // 默认休眠时间 40ms 0.04s 帧率 25帧
    double defaultDelayTime = 0.04;
    YEAudio *audio = NULL;
    AVRational timeBase;

public:
    YEVideo(YEPlayStatus *playStatus, YECallJava *callJava);

    ~YEVideo();

    void play();

    double getDelayTime(double diff);

    double getFrameDiffTime(AVFrame *avFrame);
};


#endif //NDK_DAY78_YEVIDEO_H
