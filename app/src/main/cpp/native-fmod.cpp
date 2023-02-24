//
// Created by Simle Y on 2023/1/30.
//

#include <unistd.h>
#include "com_simley_ndk_day78_fmod_FmodActivity.h"
#include "YeLog.h"

using namespace FMOD;

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_fmod_FmodActivity_voiceChangeNative(JNIEnv *env, jobject thiz,
                                                               jint mode, jstring path) {
    char *content_ = "默认 播放完毕";
    // C认识的字符串
    const char *path_ = env->GetStringUTFChars(path, NULL);
    // Java  对象
    // C     指针
    // Linux 文件

    // 音效引擎系统 指针
    System *system = NULL;
    // 声音 指针
    Sound *sound = NULL;
    // 通道，音轨，声音在上面跑 跑道 指针
    Channel *channel = NULL;
    // DSP：digital signal process  == 数字信号处理  指针
    DSP *dsp = NULL;

    // 第一步 创建系统
    System_Create(&system);
    // 第二步 系统的初始化 参数1：最大音轨数，  参数2：系统初始化标记， 参数3：额外数据
    system->init(32, FMOD_INIT_NORMAL, 0);
    // 第三步 创建声音  参数1：路径，  参数2：声音初始化标记， 参数3：额外数据， 参数4：声音指针
    system->createSound(path_, FMOD_DEFAULT, 0, &sound);
    // 第四步：播放声音  音轨 声音
    system->playSound(sound, 0, false, &channel);
//
//    // 第五步：增加特效
    switch (mode) {
        case com_simley_ndk_day78_fmod_FmodActivity_MODE_NORMAL: // 原生
            content_ = "原生 播放完毕";
            break;
        case com_simley_ndk_day78_fmod_FmodActivity_MODE_LUOLI: // 萝莉
            content_ = "萝莉 播放完毕";
            // 音调高 -- 萝莉 2.0
            // 1.创建DSP类型的Pitch 音调条件
            system->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
            // 2.设置Pitch音调调节2.0
            dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 2.0f);
            // 3.添加音效进去 音轨
            channel->addDSP(0, dsp);
            break;
        case com_simley_ndk_day78_fmod_FmodActivity_MODE_DASHU:
            content_ = "大叔 播放完毕";

            // 音调低 -- 大叔 0.7
            // 1.创建DSP类型的Pitch 音调条件
            system->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
            // 2.设置Pitch音调调节2.0
            dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 0.7f);
            // 3.添加音效进去 音轨
            channel->addDSP(0, dsp);
            break;
        case com_simley_ndk_day78_fmod_FmodActivity_MODE_GAOGUAI: // 搞怪
            content_ = "搞怪 小黄人 播放完毕";

            // 小黄人声音 频率快

            // 从音轨拿 当前 频率
            float mFrequency;
            channel->getFrequency(&mFrequency);

            // 修改频率
            channel->setFrequency(mFrequency * 1.5f); // 频率加快  小黄人的声音
            break;
        case com_simley_ndk_day78_fmod_FmodActivity_MODE_JINGSONG: // 惊悚
            content_ = "惊悚 播放完毕";

            // 惊悚音效：特点： 很多声音的拼接

            // TODO 音调低
            // 音调低 -- 大叔 0.7
            // 1.创建DSP类型的Pitch 音调条件
            system->createDSPByType(FMOD_DSP_TYPE_PITCHSHIFT, &dsp);
            // 2.设置Pitch音调调节2.0
            dsp->setParameterFloat(FMOD_DSP_PITCHSHIFT_PITCH, 0.7f);
            // 3.添加音效进去 音轨
            channel->addDSP(0, dsp); // 第一个音轨

            // TODO 搞点回声
            // 回音 ECHO
            system->createDSPByType(FMOD_DSP_TYPE_ECHO, &dsp);
            dsp->setParameterFloat(FMOD_DSP_ECHO_DELAY, 200); // 回音 延时    to 5000.  Default = 500.
            dsp->setParameterFloat(FMOD_DSP_ECHO_FEEDBACK, 10); // 回音 衰减度 Default = 50   0 完全衰减了
            channel->addDSP(1, dsp); // 第二个音轨

            // TODO 颤抖
            // Tremolo 颤抖音 正常5    非常颤抖  20
            system->createDSPByType(FMOD_DSP_TYPE_TREMOLO, &dsp);
            dsp->setParameterFloat(FMOD_DSP_TREMOLO_FREQUENCY, 20); // 非常颤抖
            dsp->setParameterFloat(FMOD_DSP_TREMOLO_SKEW, 0.8f); // ？？？
            channel->addDSP(2, dsp); // 第三个音轨

            // 调音师：才能跳出来  同学们自己去调
            break;
        case com_simley_ndk_day78_fmod_FmodActivity_MODE_KONGLING: // 空灵  学校广播
            content_ = "空灵 播放完毕";
            // 回音 ECHO
            system->createDSPByType(FMOD_DSP_TYPE_ECHO, &dsp);
            dsp->setParameterFloat(FMOD_DSP_ECHO_DELAY, 200); // 回音 延时    to 5000.  Default = 500.
            dsp->setParameterFloat(FMOD_DSP_ECHO_FEEDBACK, 10); // 回音 衰减度 Default = 50   0 完全衰减了
            channel->addDSP(0, dsp);
            break;
    }

    // 等待播放完毕 再回收
    bool isPlaying = true; // 你用不是一级指针  我用一级指针接收你，可以修改给你
    while (isPlaying) {
        channel->isPlaying(&isPlaying);
        usleep(1000 * 1000); // 每隔一秒
    }

    sound->release();
    system->close();
    system->release();
    env->ReleaseStringUTFChars(path, path_);
    LOGD("播放完毕");


//    JNIEnv *jniEnv;
//    if (javaVm->AttachCurrentThread(&jniEnv, 0) != JNI_OK) {
//        LOGE("get child thread jnienv wrong.");
//        return;
//    }
//    jclass fmodclass = env->GetObjectClass(thiz);
//    jmethodID endMethod = env->GetMethodID(fmodclass, "playerEnd", "(Ljava/lang/String;)V");
//    jstring value = env->NewStringUTF(content_);
//    jniEnv->CallVoidMethod(thiz, endMethod, value);
//    javaVm->DetachCurrentThread();
}