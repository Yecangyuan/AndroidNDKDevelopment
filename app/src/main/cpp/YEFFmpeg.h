//
// Created by Simle Y on 2022/12/10.
//

#ifndef NDK_DAY78_YEFFMPEG_H
#define NDK_DAY78_YEFFMPEG_H

#include "YECallJava.h"
#include "pthread.h"
#include "YEAudio.h"
#include "YEPlayStatus.h"

extern "C" {
#include "libavformat/avformat.h"
};

class YEFFmpeg {
public:
    YECallJava *call_java = NULL;
    const char *url = NULL;
    pthread_t decode_thread;
    AVFormatContext *av_format_context = NULL;
    YEAudio *audio = NULL;
    YEPlayStatus *play_status = NULL;

    int duration = 0;
    pthread_mutex_t seek_mutex;

public:
    YEFFmpeg(YEPlayStatus *play_status, YECallJava *call_java, const char *url);

    ~YEFFmpeg();

    void prepared();

    void decode_ffmepg_thread();

    void start();

    void seek(jint sec);

    void resume();

    void pause();

    void set_mute(jint mute);

    void set_volume(jint percent);
};


#endif //NDK_DAY78_YEFFMPEG_H
