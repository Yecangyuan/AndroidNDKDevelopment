//
// Created by Simle Y on 2022/12/9.
//

#include "YEAudio.h"

YEAudio::YEAudio(YEPlayStatus *play_status, int sample_rate, YECallJava *call_java) {
    this->play_status = play_status;
    this->sample_rate = sample_rate;
    queue = new YEQueue(play_status);
    buffer = (uint8_t *) (av_malloc(sample_rate * 2 * 2));
    this->call_java = call_java;
    sample_buffer = static_cast<SAMPLETYPE *>(malloc(sample_rate * 2 * 2));
    init_soundtouch();
}

YEAudio::~YEAudio() {}

void YEAudio::init_soundtouch() {
    sound_touch = new SoundTouch();
    sound_touch->setSampleRate(sample_rate);
    sound_touch->setChannels(2);
    // speed  1.1   1.5  2.1
    sound_touch->setTempo(speed);
    sound_touch->setPitch(pitch);
}

void *decode_play(void *data) {
    YEAudio *wl_audio = (YEAudio *) (data);
    wl_audio->init_opensles();
    pthread_exit(&wl_audio->pthread_play);
}

void YEAudio::play() {
    pthread_create(&pthread_play, NULL, decode_play, this);
}

int YEAudio::resample_audio(void **pcmbuf) {
    while (play_status != NULL && !play_status->exit) {
        av_packet = av_packet_alloc();
        if (queue->get_av_packet(av_packet) != 0) {
            av_packet_free(&av_packet);
            av_free(av_packet);
            av_packet = NULL;
            continue;
        }
        ret = avcodec_send_packet(avcodec_context, av_packet);
        if (ret != 0) {
            av_packet_free(&av_packet);
            av_free(av_packet);
            av_packet = NULL;
            continue;
        }
        av_frame = av_frame_alloc();
        ret = avcodec_receive_frame(avcodec_context, av_frame);
        if (ret == 0) {
            if (av_frame->channels && av_frame->channel_layout == 0) {
                av_frame->channel_layout = av_get_default_channel_layout(av_frame->channels);
            } else if (av_frame->channels == 0 && av_frame->channel_layout > 0) {
                av_frame->channels = av_get_channel_layout_nb_channels(av_frame->channel_layout);
            }
            SwrContext *swr_context;
            swr_context = swr_alloc_set_opts(NULL,
                                             AV_CH_LAYOUT_STEREO,
                                             AV_SAMPLE_FMT_S16,
                                             av_frame->sample_rate,
                                             av_frame->channel_layout,
                                             (AVSampleFormat) av_frame->format,
                                             av_frame->sample_rate,
                                             NULL, NULL
            );

            if (!swr_context || swr_init(swr_context) < 0) {
                av_packet_free(&av_packet);
                av_free(av_packet);
                av_packet = NULL;
                av_frame_free(&av_frame);
                av_free(av_frame);
                av_frame = NULL;
                swr_free(&swr_context);
                continue;
            }

            nb = swr_convert(
                    swr_context,
                    &buffer,
                    av_frame->nb_samples,
                    (const uint8_t **) av_frame->data,
                    av_frame->nb_samples
            );

            int out_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
            data_size = nb * out_channels * av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);

            // av_frame->pts * time_base.num / time_base.den; 等价于下面这行代码 计算时间
            now_time = av_frame->pts * av_q2d(time_base);

            if (now_time < clock) {
                now_time = clock;
            }
            clock = now_time;

            *pcmbuf = buffer;
            LOGI("data size is %d", data_size);

            av_packet_free(&av_packet);
            av_free(av_packet);
            av_packet = NULL;
            av_frame_free(&av_frame);
            av_free(av_frame);
            av_frame = NULL;
            swr_free(&swr_context);
            break;
        } else {
            av_packet_free(&av_packet);
            av_free(av_packet);
            av_packet = NULL;
            av_frame_free(&av_frame);
            av_free(av_frame);
            av_frame = NULL;
            continue;
        }
    }
    return data_size;
}

void pcm_buffer_callback(SLAndroidSimpleBufferQueueItf bf, void *context) {
    LOGI("pcm_buffer_callback被调用");
    YEAudio *wl_audio = (YEAudio *) context;

    if (wl_audio != NULL) {
        // int buffer_size = wl_audio->resample_audio();
        int buffer_size = wl_audio->get_sound_touch_data();

        if (buffer_size > 0) {
            wl_audio->clock += buffer_size / ((double) wl_audio->sample_rate * 2 * 2);

            // 由于pcm_buffer_callback调用频率高，而且通过反射的方式调用java层的onCallTimeInfo方法会比较损耗性能
            // 所以每隔一秒才调用一次
            if (wl_audio->clock - wl_audio->last_time >= 0.1) {
                wl_audio->last_time = wl_audio->clock;
                wl_audio->call_java->on_call_time_info(CHILD_THREAD, (int) wl_audio->clock,
                                                       wl_audio->duration);
            }

            (*wl_audio->pcm_buffer_queue)->Enqueue(wl_audio->pcm_buffer_queue,
                                                   (char *) wl_audio->sample_buffer,
                                                   buffer_size * 2 * 2);
        }
    }
}

void YEAudio::init_opensles() {
    SLresult lresult;
    lresult = slCreateEngine(&engine_object, 0, 0, 0, 0, 0);
    lresult = (*engine_object)->Realize(engine_object, SL_BOOLEAN_FALSE);
    lresult = (*engine_object)->GetInterface(engine_object, SL_IID_ENGINE, &engine_engine);

    // 第二步，创建混音器
    const SLInterfaceID mids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean mreq[1] = {SL_BOOLEAN_FALSE};
    lresult = (*engine_engine)->CreateOutputMix(engine_engine, &output_mix_object, 1, mids, mreq);
    (void) lresult;
    lresult = (*output_mix_object)->Realize(output_mix_object, SL_BOOLEAN_FALSE);
    (void) lresult;
    lresult = (*output_mix_object)->GetInterface(output_mix_object, SL_IID_ENVIRONMENTALREVERB,
                                                 &output_mix_environmental_reverb);

    if (SL_RESULT_SUCCESS == lresult) {
        lresult = (*output_mix_environmental_reverb)->SetEnvironmentalReverbProperties(
                output_mix_environmental_reverb, &reverb_settings);
        (void) lresult;
    }
    SLDataLocator_OutputMix output_mix = {SL_DATALOCATOR_OUTPUTMIX, output_mix_object};
    SLDataSink audio_sink = {&output_mix, 0};

    // 第三步，配置PCM格式信息
    SLDataLocator_AndroidSimpleBufferQueue android_queue = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
                                                            2};
    SLDataFormat_PCM pcm = {
            SL_DATAFORMAT_PCM, // 播放PCM格式的数据
            2, // 2个声道（立体声）
            static_cast<SLuint32>(get_current_sample_rate_for_opensles(
                    sample_rate)), // 444100Hz的采样率
            SL_PCMSAMPLEFORMAT_FIXED_16, // 位数 16位
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, // 立体声（前左前右）
            SL_BYTEORDER_LITTLEENDIAN       // 结束标志
    };
    SLDataSource sl_datasource = {&android_queue, &pcm};

    const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_VOLUME, SL_IID_MUTESOLO};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

    (*engine_engine)->CreateAudioPlayer(engine_engine, &pcm_player_object, &sl_datasource,
                                        &audio_sink, 2, ids, req);
    // 初始化播放器
    (*pcm_player_object)->Realize(pcm_player_object, SL_BOOLEAN_FALSE);
    // 得到接口后调用， 获取Player接口
    (*pcm_player_object)->GetInterface(pcm_player_object, SL_IID_PLAY, &pcm_player_play);
    // 获取声道操作接口
    (*pcm_player_object)->GetInterface(pcm_player_object, SL_IID_MUTESOLO, &pcm_mute_play);
    // 获取播放 暂停 恢复的句柄
    (*pcm_player_object)->GetInterface(pcm_player_object, SL_IID_VOLUME, &pcm_volume_play);
    // 注册回调缓冲区 获取缓冲队列接口
    (*pcm_player_object)->GetInterface(pcm_player_object, SL_IID_BUFFERQUEUE, &pcm_buffer_queue);
    // 缓冲接口回调
    (*pcm_buffer_queue)->RegisterCallback(pcm_buffer_queue, pcm_buffer_callback, this);
    // 获取播放状态接口
    (*pcm_player_play)->SetPlayState(pcm_player_play, SL_PLAYSTATE_PLAYING);
    pcm_buffer_callback(pcm_buffer_queue, this);
}

int YEAudio::get_current_sample_rate_for_opensles(int sample_rate) {
    int rate;
    switch (sample_rate) {
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
    if (pcm_player_play != NULL) {
        (*pcm_player_play)->SetPlayState(pcm_player_play, SL_PLAYSTATE_PAUSED);
    }
}

void YEAudio::resume() {
    if (pcm_player_play != NULL) {
        (*pcm_player_play)->SetPlayState(pcm_player_play, SL_PLAYSTATE_PLAYING);
    }
}

void YEAudio::set_mute(int mute) {
    LOGE(" 声道  接口%p", pcm_mute_play);
    if (pcm_mute_play == NULL) {
        return;
    }
    this->mute = mute;
    switch (mute) {
        // it means that only left channel can hear sound.
        case 0: //right   0
            (*pcm_mute_play)->SetChannelMute(pcm_mute_play, 1, false);
            (*pcm_mute_play)->SetChannelMute(pcm_mute_play, 0, true);
            break;
        case 1: //left
            (*pcm_mute_play)->SetChannelMute(pcm_mute_play, 1, true);
            (*pcm_mute_play)->SetChannelMute(pcm_mute_play, 0, false);
            break;
        case 2: //center
            (*pcm_mute_play)->SetChannelMute(pcm_mute_play, 1, false);
            (*pcm_mute_play)->SetChannelMute(pcm_mute_play, 0, false);
        default:
            break;
    }
}

void YEAudio::set_volume(int percent) {
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

void YEAudio::set_speed(float speed) {
    this->speed = speed;
    if (sound_touch != NULL) {
        sound_touch->setTempo(speed);
    }
}

void YEAudio::set_pitch(float pitch) {
    this->pitch = pitch;
    if (sound_touch != NULL) {
        sound_touch->setPitch(pitch);
    }
}

void YEAudio::release() {

    if (queue != NULL) {
        delete queue;
        queue = NULL;
    }

    if (pcm_player_object != NULL) {
        (*pcm_player_object)->Destroy(pcm_player_object);
        pcm_player_object = NULL;
        pcm_player_play = NULL;
        pcm_buffer_queue = NULL;
    }

    if (output_mix_object != NULL) {
        (*output_mix_object)->Destroy(output_mix_object);
        output_mix_object = NULL;
        output_mix_environmental_reverb = NULL;
    }

    if (engine_object != NULL) {
        (*engine_object)->Destroy(engine_object);
        engine_object = NULL;
        engine_engine = NULL;
    }

    if (buffer != NULL) {
        free(buffer);
        buffer = NULL;
    }

    if (avcodec_context != NULL) {
        avcodec_close(avcodec_context);
        avcodec_free_context(&avcodec_context);
        avcodec_context = NULL;
    }

    if (play_status != NULL) {
        play_status = NULL;
    }

    if (call_java != NULL) {
        call_java = NULL;
    }
}

int YEAudio::get_sound_touch_data() {
    // 重新整理波形 soundtouch
    // 我们先取数据 pcm的数据 就在outbuffer
    while (play_status != NULL && !play_status->exit) {
        LOGE("------------------循环---------------------------finished %d", finished);
        out_buffer = NULL;
        if (finished) {
            finished = false;
            // 从网络流 文件 读取数据 out_buffer  字节数量  out_buffer   是一个旧波
            data_size = this->resample_audio(reinterpret_cast<void **>(&out_buffer));

            if (data_size > 0) {

                for (int i = 0; i < data_size / 2 + 1; ++i) {
                    // short 2个字节 pcm数据   ====波形
                    sample_buffer[i] = (float) (out_buffer[i * 2] | ((out_buffer[i * 2 + 1]) << 8));
                }

                // 让soundtouch对波进行整理
                sound_touch->putSamples(sample_buffer, nb);
                // 整理完成，接收一个新波，放到sample_buffer
                num = sound_touch->receiveSamples(sample_buffer, data_size / 4);
                LOGE("------------第一个num %d ", num);
            } else {
                sound_touch->flush();
            }
        }

        if (num == 0) {
            finished = true;
            continue;
        } else {

            if (out_buffer == NULL) {
                num = sound_touch->receiveSamples(sample_buffer, data_size / 4);
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
