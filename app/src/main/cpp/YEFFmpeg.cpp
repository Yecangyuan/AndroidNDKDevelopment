//
// Created by Simle Y on 2022/12/10.
//

#include "YEFFmpeg.h"
#include "ye_log.h"

YEFFmpeg::YEFFmpeg(YEPlayStatus *play_status, YECallJava *call_java, const char *url) {
    this->play_status = play_status;
    this->call_java = call_java;
    this->url = url;
    pthread_mutex_init(&seek_mutex, NULL);
}

YEFFmpeg::~YEFFmpeg() {
    avformat_network_deinit();
    pthread_mutex_destroy(&seek_mutex);
}

void *on_decode_ffmpeg(void *data) {
    auto *wl_ffmpeg = (YEFFmpeg *) data;
    wl_ffmpeg->decode_ffmepg_thread();
    pthread_exit(&wl_ffmpeg->decode_thread);
}

void YEFFmpeg::prepared() {
    pthread_create(&decode_thread, NULL, on_decode_ffmpeg, this);
}

void YEFFmpeg::decode_ffmepg_thread() {
    // 初始化网络支持模块
    avformat_network_init();

    // 分配上下文
    av_format_context = avformat_alloc_context();

    if (avformat_open_input(&av_format_context, url, NULL, NULL) != 0) {
        LOGE("Failed to open input");
        return;
    }

    if (avformat_find_stream_info(av_format_context, NULL) < 0) {
        LOGE("Couldn't open video");
        return;
    }

    for (int i = 0; i < av_format_context->nb_streams; ++i) {
        if (av_format_context->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) { // 得到音频流
            if (audio == NULL) {
                audio = new YEAudio(play_status,
                                    av_format_context->streams[i]->codecpar->sample_rate,
                                    call_java);
                audio->stream_index = i;
                // 音频时长，单位微秒
                audio->duration = (int) (av_format_context->duration / AV_TIME_BASE);
                audio->time_base = av_format_context->streams[i]->time_base;
                audio->codec_par = av_format_context->streams[i]->codecpar;
                duration = audio->duration;
            }
        }
    }

    const AVCodec *avcodec = avcodec_find_decoder(audio->codec_par->codec_id);
    if (!avcodec) {
        LOGE("找不到对应的编解码器");
        return;
    }

    audio->av_codec_context = avcodec_alloc_context3(avcodec);
    if (!audio->av_codec_context) {
        LOGE("分配编解码器上下文失败");
        return;
    }

    if (avcodec_parameters_to_context(audio->av_codec_context, audio->codec_par) < 0) {
        LOGE("设置 编解码器上下文 参数失败");
        return;
    }

    // 打开编解码器
    if (avcodec_open2(audio->av_codec_context, avcodec, NULL) != 0) {
        LOGE("打开编解码器失败");
        return;
    }
    call_java->on_call_prepared(CHILD_THREAD);
}

void YEFFmpeg::start() {
    if (audio == NULL) {
        LOGE("audio is null");
        return;
    }
    audio->play();

    int count = 0;
    while (play_status != NULL && !play_status->exit) {
        if (play_status->seek) {
            continue;
        }

        if (audio->queue->get_queue_size() > 40) {
            continue;
        }

        AVPacket *av_packet = av_packet_alloc();
        if (av_read_frame(av_format_context, av_packet) == 0) {
            if (av_packet->stream_index == audio->stream_index) {
                // 解码操作
                count++;
                LOGI("解码第 %d 帧", count);
                audio->queue->put_av_packet(av_packet);
            } else {
                av_packet_free(&av_packet);
                av_free(av_packet);
            }
        } else {
            av_packet_free(&av_packet);
            av_free(av_packet);
            while (play_status != NULL && !play_status->exit) {
                if (audio->queue->get_queue_size() > 0) {
                    continue;
                } else {
                    play_status->exit = true;
                    break;
                }
            }
        }
    }
    LOGI("解码完成");
}

void YEFFmpeg::seek(jint sec) {
    if (duration <= 0) {
        return;
    }
    if (sec >= 0 && sec <= duration) {
        if (audio != NULL) {
            play_status->seek = true;
            audio->queue->clear_av_packet();
            audio->clock = 0;
            audio->last_time = 0;
            pthread_mutex_lock(&seek_mutex);
            int64_t rel = sec * AV_TIME_BASE;
            avformat_seek_file(av_format_context, -1, INT64_MIN, rel, INT64_MAX, 0);
            pthread_mutex_unlock(&seek_mutex);
            play_status->seek = false;
        }
    }
}

void YEFFmpeg::resume() {
    if (audio != NULL) {
        audio->resume();
    }
}

void YEFFmpeg::pause() {
    if (audio != NULL) {
        audio->pause();
    }
}

void YEFFmpeg::set_mute(jint mute) {
    if (audio != NULL) {
        audio->set_mute(mute);
    }
}

void YEFFmpeg::set_volume(jint percent) {
    if (audio != NULL) {
        audio->set_volume(percent);
    }
}
