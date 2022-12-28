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
    YEVideo *video = NULL;
    YEPlayStatus *play_status = NULL;

    int duration = 0;
    pthread_mutex_t seek_mutex;
    pthread_mutex_t init_mutex;
    bool exit = false;

public:
    YEFFmpeg(YEPlayStatus *play_status, YECallJava *call_java, const char *url);

    ~YEFFmpeg();

    void prepared();

    void decode_ffmepg_thread();

    void start();

    void seek(int64_t sec);

    void resume();

    void pause();

    void set_mute(int mute);

    void set_volume(int percent);

    void set_speed(float speed);

    void set_pitch(float pitch);

    void release();

    /**
     * 获取音视频编解码器的上下文对象
     * @param codecpar
     * @param av_codec_context
     * @return
     */
    int get_avcodec_context(AVCodecParameters *codecpar, AVCodecContext **av_codec_context);
};


#endif //NDK_DAY78_YEFFMPEG_H
