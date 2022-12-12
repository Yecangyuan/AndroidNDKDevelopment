//
// Created by Simle Y on 2022/12/9.
//

#ifndef NDK_DAY78_YEAUDIO_H
#define NDK_DAY78_YEAUDIO_H

#include "YEQueue.h"
#include "YEPlayStatus.h"
#include "YECallJava.h"


extern "C" {
#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"
#include "SLES/OpenSLES.h"
#include "SLES/OpenSLES_Android.h"
}

class YEAudio {
public:
    int stream_index = -1;
    AVCodecContext *av_codec_context = NULL;
    AVCodecParameters *codec_par = NULL;
    YEQueue *queue = NULL;
    YEPlayStatus *play_status = NULL;

    pthread_t pthread_play;
    AVPacket *av_packet = NULL;
    AVFrame *av_frame = NULL;
    int ret = 0;
    uint8_t *buffer = 0;
    int data_size = 0;
    int sample_rate = 0;

    // 引擎接口
    SLObjectItf engine_object = NULL;
    SLEngineItf engine_engine = NULL;

    // 混音器
    SLObjectItf output_mix_object = NULL;
    SLEnvironmentalReverbItf output_mix_environmental_reverb = NULL;
    SLEnvironmentalReverbSettings reverb_settings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

    // PCM
    SLObjectItf pcm_player_object = NULL;
    SLPlayItf pcm_player_play = NULL;
    SLVolumeItf pcm_volume_play = NULL;

    // 缓冲器队列接口
    SLAndroidSimpleBufferQueueItf pcm_buffer_queue = NULL;

    // 音频时长
    int duration = 0;
    // 时间单位 总时间/帧数  pts = 序号 * 总时间 / 帧数
    AVRational time_base;
    // 当前av_frame的时间
    double now_time;
    // 当前播放的时间准确时间
    double clock;

    YECallJava *call_java = NULL;
    // 上一次调用时间
    double last_time;
    // 立体声
    int mute = 2;
    SLMuteSoloItf pcm_mute_play = NULL;
    int volume_percent = 100;

public:
    YEAudio(YEPlayStatus *play_status, int sample_rate, YECallJava *call_java);

    ~YEAudio();

    void play();

    int resample_audio();

    void init_opensles();

    int get_current_sample_rate_for_opensles(int sample_rate);

    void pause();

    void resume();

    void set_mute(int mute);

    void set_volume(int percent);
};


#endif //NDK_DAY78_YEAUDIO_H
