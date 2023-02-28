//
// Created by Simle Y on 2022/12/10.
//

#ifndef NDK_DAY78_YEFFMPEG_H
#define NDK_DAY78_YEFFMPEG_H

#include "YECallJava.h"
#include "pthread.h"
#include "YEAudio.h"
#include "YEVideo.h"
#include "YEPlayStatus.h"
#include "cstring"
#include "YeLog.h"

extern "C" {
#include "libavutil/time.h"
#include "libavformat/avformat.h"
};

class YEFFmpeg {
public:
    YECallJava *callJava = NULL;
    AVFormatContext *avFormatContext = NULL;
    char *url = NULL;
    YEAudio *audio = NULL;
    YEVideo *video = NULL;
    YEPlayStatus *playStatus = NULL;

    int duration = 0;
    bool exit = false;

    pthread_t decodeThread;
    pthread_mutex_t seekMutex;
    pthread_mutex_t initMutex;

public:
    YEFFmpeg(YEPlayStatus *playStatus, YECallJava *callJava, const char *url);

    ~YEFFmpeg();

    void prepared();

    void decodeFfmepgThread();

    void start();

    void seek(int64_t sec);

    void resume();

    void pause();

    void setMute(int mute);

    void setVolume(int percent);

    void setSpeed(float speed);

    void setPitch(float pitch);

    void release();

    /**
     * 获取音视频编解码器的上下文对象
     * @param codecParameters
     * @param avCodecContext
     * @return
     */
    int getAvcodecContext(AVCodecParameters *codecParameters, AVCodecContext **avCodecContext);
};


#endif //NDK_DAY78_YEFFMPEG_H
