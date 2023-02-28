//
// Created by Simle Y on 2022/12/9.
//

#ifndef NDK_DAY78_YEAUDIO_H
#define NDK_DAY78_YEAUDIO_H

#define PCM_CHANNELS_NUM 2
#include "YEQueue.h"
#include "YEPlayStatus.h"
#include "YECallJava.h"
#include "SoundTouch.h"
#include "YeLog.h"

extern "C" {
#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"
#include "SLES/OpenSLES.h"
#include "SLES/OpenSLES_Android.h"
}

using namespace soundtouch;

class YEAudio {
public:
    int streamIndex = -1;
    AVCodecContext *avcodecContext = NULL;
    AVCodecParameters *codecParameters = NULL;
    YEQueue *queue = NULL;
    YEPlayStatus *playStatus = NULL;

    pthread_t pthreadPlay;
    AVPacket *avPacket = NULL;
    AVFrame *avFrame = NULL;
    int ret = 0;
    uint8_t *buffer = NULL;
    int dataSize = 0;
    int sampleRate = 0;

    // 引擎接口
    SLObjectItf engineObject = NULL;
    SLEngineItf engineEngine = NULL;

    // 混音器
    SLObjectItf outputMixObject = NULL;
    SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;
    SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

    // PCM
    SLObjectItf pcmPlayerObject = NULL;
    SLPlayItf pcmPlayerPlay = NULL;
    SLVolumeItf pcmVolumePlay = NULL;

    // 缓冲器队列接口
    SLAndroidSimpleBufferQueueItf pcmBufferQueue = NULL;

    // 音频时长
    int duration = 0;
    // 时间单位 总时间/帧数  pts = 序号 * 总时间 / 帧数
    AVRational timeBase;
    // 当前av_frame的时间
    int64_t nowTime;
    // 当前播放的时间 准确时间
    int64_t clock;
    // 新的缓冲区
    SAMPLETYPE *sampleBuffer = NULL;
    uint8_t *outputBuffer = NULL;
    // 音高
    float pitch = 1.0f;
    // 倍速
    float speed = 1.0f;
    bool finished = true;
    // 新波的实际个数
    int nb = 0;
    int num = 0;

    YECallJava *callJava = NULL;
    SoundTouch *soundTouch = NULL;
    // 上一次调用时间
    double lastTime;
    // 立体声
    int mute = 2;
    SLMuteSoloItf pcmMutePlay = NULL;
    int volumePercent = 100;

public:
    YEAudio(YEPlayStatus *playStatus, int sampleRate, YECallJava *callJava);

    ~YEAudio();

    /**
     * 开始播放
     */
    void play();

    /**
     * 对音频重采样
     * @param pcmBuffer
     * @return
     */
    int resampleAudio(void **pcmBuffer);

    /**
     * 初始化opensles
     */
    void initOpenSLES();

//    /**
//     * 初始化soundtouch
//     */
//    void initSoundTouch();

    /**
     * 获取当前的采样率
     * @param _sampleRate
     * @return
     */
    int getCurrentSampleRateForOpenSLES(int _sampleRate);

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
    void setMute(int mute);

    /**
     * 设置播放音量
     * @param percent
     */
    void setVolume(int percent);

    /**
     * 设置播放速度
     * @param speed
     */
    void setSpeed(float speed);

    /**
     * 设置音高 音调
     * @param pitch
     */
    void setPitch(float pitch);

    /**
     * 释放相关资源
     */
    void release();

    int getSoundTouchData();
};


#endif //NDK_DAY78_YEAUDIO_H
