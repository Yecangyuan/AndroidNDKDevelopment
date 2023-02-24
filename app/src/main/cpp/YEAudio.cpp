//
// Created by Simle Y on 2022/12/9.
//

#include "YEAudio.h"

#define PCM_CHANNELS_NUM 2

YEAudio::YEAudio(YEPlayStatus *playStatus, int sampleRate, YECallJava *callJava) {
    this->playStatus = playStatus;
    this->sampleRate = sampleRate;
    queue = new YEQueue(playStatus);
    buffer = (uint8_t *) (av_malloc(sampleRate * 2 * 2));
    this->callJava = callJava;
    sampleBuffer = static_cast<SAMPLETYPE *>(malloc(sampleRate * 2 * 2));
    soundTouch = new SoundTouch();
    soundTouch->setSampleRate(sampleRate);
    soundTouch->setChannels(2);
    // speed  1.1   1.5  2.1
    soundTouch->setTempo(speed);
    soundTouch->setPitch(pitch);
}

YEAudio::~YEAudio() {}

void *decode_play(void *data) {
    auto *wlAudio = (YEAudio *) (data);
    wlAudio->initOpenSLES();
    pthread_exit(&wlAudio->pthreadPlay);
}

void YEAudio::play() {
    pthread_create(&pthreadPlay, NULL, decode_play, this);
}

int YEAudio::resampleAudio(void **pcmBuffer) {
    while (playStatus != NULL && !playStatus->exit) {
        avPacket = av_packet_alloc();
        if (queue->getAvPacket(avPacket) != 0) {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            continue;
        }
        ret = avcodec_send_packet(avcodecContext, avPacket);
        if (ret != 0) {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            continue;
        }
        avFrame = av_frame_alloc();
        ret = avcodec_receive_frame(avcodecContext, avFrame);
        if (ret == 0) {
            if (avFrame->channels && avFrame->channel_layout == 0) {
                avFrame->channel_layout = av_get_default_channel_layout(avFrame->channels);
            } else if (avFrame->channels == 0 && avFrame->channel_layout > 0) {
                avFrame->channels = av_get_channel_layout_nb_channels(avFrame->channel_layout);
            }
            SwrContext *swrContext;

            swrContext = swr_alloc_set_opts(
                    NULL,
                    AV_CH_LAYOUT_STEREO,
                    AV_SAMPLE_FMT_S16,
                    avFrame->sample_rate,
                    avFrame->channel_layout,
                    (AVSampleFormat) avFrame->format,
                    avFrame->sample_rate,
                    NULL, NULL
            );

            if (!swrContext || swr_init(swrContext) < 0) {
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;

                av_frame_free(&avFrame);
                av_free(avFrame);
                avFrame = NULL;
                swr_free(&swrContext);
                continue;
            }

            nb = swr_convert(
                    swrContext,
                    &buffer,
                    avFrame->nb_samples,
                    (const uint8_t **) avFrame->data,
                    avFrame->nb_samples
            );
            LOGE("nb:%d", nb);

            int out_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
            dataSize = nb * out_channels * av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);
            nowTime = avFrame->pts * av_q2d(timeBase);

            if (nowTime < clock) {
                nowTime = clock;
            }
            clock = nowTime;
//
            *pcmBuffer = buffer;

            LOGE("dataSize is %d", dataSize);

            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            swr_free(&swrContext);
            break;
        } else {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            continue;
        }
    }
    return dataSize;
}

void pcmBufferCallback(SLAndroidSimpleBufferQueueItf bf, void *context) {
    LOGI("pcmBufferCallback被调用");
    auto *wlAudio = (YEAudio *) context;
    if (wlAudio != NULL) {
        int bufferSize = wlAudio->getSoundTouchData();

        if (bufferSize > 0) {
            wlAudio->clock += bufferSize / ((double) wlAudio->sampleRate * 2 * 2);

            // 由于pcm_buffer_callback调用频率高，而且通过反射的方式调用java层的onCallTimeInfo方法会比较损耗性能
            // 所以每隔一秒才调用一次
            if (wlAudio->clock - wlAudio->lastTime >= 0.1) {
                wlAudio->lastTime = wlAudio->clock;
                wlAudio->callJava->onCallTimeInfo(CHILD_THREAD, (int) wlAudio->clock,
                                                  wlAudio->duration);
            }

            (*wlAudio->pcmBufferQueue)->Enqueue(wlAudio->pcmBufferQueue,
                                                (char *) wlAudio->sampleBuffer,
                                                bufferSize * 2 * 2);
        }
    }
}

void YEAudio::initOpenSLES() {
    SLresult sLresult;
    sLresult = slCreateEngine(&engineObject, 0, 0, 0, 0, 0);
    sLresult = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    sLresult = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);

    // 第二步，创建混音器
    const SLInterfaceID mids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean mreq[1] = {SL_BOOLEAN_FALSE};
    sLresult = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 1, mids, mreq);
    (void) sLresult;
    sLresult = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    (void) sLresult;
    sLresult = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                                                &outputMixEnvironmentalReverb);

    if (SL_RESULT_SUCCESS == sLresult) {
        sLresult = (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(
                outputMixEnvironmentalReverb, &reverb_settings);
        (void) sLresult;
    }
    SLDataLocator_OutputMix output_mix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audio_sink = {&output_mix, 0};

    // 第三步，配置PCM格式信息
    SLDataLocator_AndroidSimpleBufferQueue android_queue = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
                                                            2};
    SLDataFormat_PCM pcm = {
            SL_DATAFORMAT_PCM, // 播放PCM格式的数据
            PCM_CHANNELS_NUM, // 2个声道（立体声）
            static_cast<SLuint32>(getCurrentSampleRateForOpenSLES(sampleRate)), // 444100Hz的采样率
            SL_PCMSAMPLEFORMAT_FIXED_16, // 位数 16位
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, // 立体声（前左前右）
            SL_BYTEORDER_LITTLEENDIAN       // 结束标志
    };
    SLDataSource sl_datasource = {&android_queue, &pcm};

    const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME, SL_IID_MUTESOLO};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

    (*engineEngine)->CreateAudioPlayer(engineEngine, &pcmPlayerObject, &sl_datasource,
                                       &audio_sink, 2, ids, req);
    // 初始化播放器
    (*pcmPlayerObject)->Realize(pcmPlayerObject, SL_BOOLEAN_FALSE);
    // 得到接口后调用， 获取Player接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_PLAY, &pcmPlayerPlay);
    // 获取声道操作接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_MUTESOLO, &pcmMutePlay);
    // 获取播放 暂停 恢复的句柄
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_VOLUME, &pcm_volume_play);
    // 注册回调缓冲区 获取缓冲队列接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_BUFFERQUEUE, &pcmBufferQueue);
    // 缓冲接口回调
    (*pcmBufferQueue)->RegisterCallback(pcmBufferQueue, pcmBufferCallback, this);
    // 获取播放状态接口
    (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PLAYING);
    pcmBufferCallback(pcmBufferQueue, this);
}

int YEAudio::getCurrentSampleRateForOpenSLES(int sampleRate) {
    int rate;
    switch (sampleRate) {
        case 8000:
            rate = SL_SAMPLINGRATE_8;
            break;
        case 11025:
            rate = SL_SAMPLINGRATE_11_025;
            break;
        case 12000:
            rate = SL_SAMPLINGRATE_12;
            break;
        case 16000:
            rate = SL_SAMPLINGRATE_16;
            break;
        case 22050:
            rate = SL_SAMPLINGRATE_22_05;
            break;
        case 24000:
            rate = SL_SAMPLINGRATE_24;
            break;
        case 32000:
            rate = SL_SAMPLINGRATE_32;
            break;
        case 44100:
            rate = SL_SAMPLINGRATE_44_1;
            break;
        case 48000:
            rate = SL_SAMPLINGRATE_48;
            break;
        case 64000:
            rate = SL_SAMPLINGRATE_64;
            break;
        case 88200:
            rate = SL_SAMPLINGRATE_88_2;
            break;
        case 96000:
            rate = SL_SAMPLINGRATE_96;
            break;
        case 192000:
            rate = SL_SAMPLINGRATE_192;
            break;
        default:
            rate = SL_SAMPLINGRATE_44_1;
    }
    return rate;
}


void YEAudio::pause() {
    if (pcmPlayerPlay != NULL) {
        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PAUSED);
    }
}

void YEAudio::resume() {
    if (pcmPlayerPlay != NULL) {
        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PLAYING);
    }
}

void YEAudio::setMute(int mute) {
    LOGE(" 声道  接口%p", pcmMutePlay);
    if (pcmMutePlay == NULL) {
        return;
    }
    this->mute = mute;
    switch (mute) {
        // it means that only left channel can hear sound.
        case 0: //right   0
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, false);
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, true);
            break;
        case 1: //left
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, true);
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, false);
            break;
        case 2: //center
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, false);
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, false);
        default:
            break;
    }
}

void YEAudio::setVolume(int percent) {
    if (pcm_volume_play != NULL) {
        if (percent > 30) {
            (*pcm_volume_play)->SetVolumeLevel(pcm_volume_play, (100 - percent) * -20);
        } else if (percent > 25) {
            (*pcm_volume_play)->SetVolumeLevel(pcm_volume_play, (100 - percent) * -22);
        } else if (percent > 20) {
            (*pcm_volume_play)->SetVolumeLevel(pcm_volume_play, (100 - percent) * -25);
        } else if (percent > 15) {
            (*pcm_volume_play)->SetVolumeLevel(pcm_volume_play, (100 - percent) * -28);
        } else if (percent > 10) {
            (*pcm_volume_play)->SetVolumeLevel(pcm_volume_play, (100 - percent) * -30);
        } else if (percent > 5) {
            (*pcm_volume_play)->SetVolumeLevel(pcm_volume_play, (100 - percent) * -34);
        } else if (percent > 3) {
            (*pcm_volume_play)->SetVolumeLevel(pcm_volume_play, (100 - percent) * -37);
        } else if (percent > 0) {
            (*pcm_volume_play)->SetVolumeLevel(pcm_volume_play, (100 - percent) * -40);
        } else {
            (*pcm_volume_play)->SetVolumeLevel(pcm_volume_play, (100 - percent) * -100);
        }
    }
}

void YEAudio::setSpeed(float speed) {
    this->speed = speed;
    if (soundTouch != NULL) {
        soundTouch->setTempo(speed);
    }
}

void YEAudio::setPitch(float pitch) {
    this->pitch = pitch;
    if (soundTouch != NULL) {
        soundTouch->setPitch(pitch);
    }
}

void YEAudio::release() {

    if (queue != NULL) {
        delete queue;
        queue = NULL;
    }

    if (pcmPlayerObject != NULL) {
        (*pcmPlayerObject)->Destroy(pcmPlayerObject);
        pcmPlayerObject = NULL;
        pcmPlayerPlay = NULL;
        pcmBufferQueue = NULL;
    }

    if (outputMixObject != NULL) {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
        outputMixEnvironmentalReverb = NULL;
    }

    if (engineObject != NULL) {
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        engineEngine = NULL;
    }

    if (buffer != NULL) {
        free(buffer);
        buffer = NULL;
    }

    if (avcodecContext != NULL) {
        avcodec_close(avcodecContext);
        avcodec_free_context(&avcodecContext);
        avcodecContext = NULL;
    }

    if (playStatus != NULL) {
        playStatus = NULL;
    }
    if (callJava != NULL) {
        callJava = NULL;
    }
}

int YEAudio::getSoundTouchData() {
    // 重新整理波形 soundtouch
    // 我们先取数据 pcm的数据 就在outbuffer
    while (playStatus != NULL && !playStatus->exit) {
        LOGE("------------------循环---------------------------finished %d", finished);
        outputBuffer = NULL;
        if (finished) {
            finished = false;
            // 从网络流 文件 读取数据 outputBuffer  字节数量  outputBuffer   是一个旧波
            dataSize = this->resampleAudio(reinterpret_cast<void **>(&outputBuffer));

            if (dataSize > 0) {

                for (int i = 0; i < dataSize / 2 + 1; ++i) {
                    // short 2个字节 pcm数据   ====波形
                    sampleBuffer[i] = (outputBuffer[i * 2] | ((outputBuffer[i * 2 + 1]) << 8));
                }

                // 让soundtouch对波进行整理
                soundTouch->putSamples(sampleBuffer, nb);
                // 整理完成，接收一个新波，放到sample_buffer
                num = soundTouch->receiveSamples(sampleBuffer, dataSize / 4);
                LOGE("------------第一个num %d ", num);
            } else {
                soundTouch->flush();
            }
        }

        if (num == 0) {
            finished = true;
            continue;
        } else {
            if (outputBuffer == NULL) {
                num = soundTouch->receiveSamples(sampleBuffer, dataSize / 4);
                LOGE("------------第二个num %d ", num);

                if (num == 0) {
                    finished = true;
                    continue;
                }
            }
            LOGE("---------------- 结束1 -----------------------");
            return num;
        }
    }
    LOGE("---------------- 结束2 -----------------------");
    return 0;
}
