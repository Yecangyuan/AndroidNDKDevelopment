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
    int stream_index = -1;
    AVCodecContext *avcodec_context = NULL;
    AVCodecParameters *codecpar = NULL;
    YEPlayStatus *play_status = NULL;
    YECallJava *ye_call_java = NULL;
    pthread_mutex_t pthread_mutex;
    pthread_t pthread_play;

    double clock = 0;
    // 主要与音频的差值
    double delay_time = 0;
    // 默认休眠时间 40ms 0.04s 帧率 25帧
    double default_delay_time = 0.04;
    YEAudio *audio = NULL;
    AVRational time_base;

public:
    YEVideo(YEPlayStatus *play_status, YECallJava *ye_call_java);

    ~YEVideo();

    void play();

    double get_delay_time(double diff);

    double get_frame_diff_time(AVFrame *av_frame);
};


#endif //NDK_DAY78_YEVIDEO_H
