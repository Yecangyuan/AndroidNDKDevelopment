//
// Created by Simle Y on 2022/12/9.
//

#ifndef NDK_DAY78_YEAUDIO_H
#define NDK_DAY78_YEAUDIO_H

#include "YEQueue.h"
#include "YEPlayStatus.h"
#include "YECallJava.h"
#include "SoundTouch.h"
#include "ye_log.h"

extern "C" {
#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"
#include "SLES/OpenSLES.h"
#include "SLES/OpenSLES_Android.h"
}

using namespace soundtouch;

class YEAudio {
public:
    int stream_index = -1;
    AVCodecContext *avcodec_context = NULL;
    AVCodecParameters *codecpar = NULL;
    YEQueue *queue = NULL;
    YEPlayStatus *play_status = NULL;

    pthread_t pthread_play;
    AVPacket *av_packet = NULL;
    AVFrame *av_frame = NULL;
    int ret = 0;
    uint8_t *buffer = NULL;
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
    // 当前播放的时间 准确时间
    double clock;
    // 新的缓冲区
    SAMPLETYPE *sample_buffer = NULL;
    uint8_t *out_buffer = NULL;
    // 音高
    float pitch = 1.0f;
    // 倍速
    float speed = 1.0f;
    bool finished = true;
    // 新波的实际个数
    int nb = 0;
    int num = 0;

    YECallJava *call_java = NULL;
    SoundTouch *sound_touch = NULL;
    // 上一次调用时间
    double last_time;
    // 立体声
    int mute = 2;
    SLMuteSoloItf pcm_mute_play = NULL;
    int volume_percent = 100;

public:
    YEAudio(YEPlayStatus *play_status, int sample_rate, YECallJava *call_java);

    ~YEAudio();

    /**
     * 开始播放
     */
    void play();

    /**
     * 对音频重采样
     * @param pcmbuf
     * @return
     */
    int resample_audio(void **pcmbuf);

    /**
     * 初始化opensles
     */
    void init_opensles();

    /**
     * 初始化soundtouch
     */
    void init_soundtouch();

    int get_current_sample_rate_for_opensles(int sample_rate);

    /**
     * 暂停播放
     */
    void pause();

    /**
     * 继续播放
     */
    void resume();

    /**
     * 设置声道
     * @param mute
     */
    void set_mute(int mute);

    /**
     * 设置播放音量
     * @param percent
     */
    void set_volume(int percent);

    /**
     * 设置播放速度
     * @param speed
     */
    void set_speed(float speed);

    /**
     * 设置音高 音调
     * @param pitch
     */
    void set_pitch(float pitch);

    /**
     * 释放相关资源
     */
    void release();

    int get_sound_touch_data();
};


#endif //NDK_DAY78_YEAUDIO_H
