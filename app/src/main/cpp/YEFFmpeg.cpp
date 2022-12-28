//
// Created by Simle Y on 2022/12/10.
//

#include "YEFFmpeg.h"
#include "ye_log.h"

extern "C" {
#include "libavutil/time.h"
}

YEFFmpeg::YEFFmpeg(YEPlayStatus *play_status, YECallJava *call_java, const char *url) {
    this->play_status = play_status;
    this->call_java = call_java;
    this->url = url;
    pthread_mutex_init(&seek_mutex, NULL);
    pthread_mutex_init(&init_mutex, NULL);
}

YEFFmpeg::~YEFFmpeg() {
    avformat_network_deinit();
    pthread_mutex_destroy(&init_mutex);
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

int YEFFmpeg::get_avcodec_context(AVCodecParameters *codecpar, AVCodecContext **av_codec_context) {
    int ret;
    const AVCodec *avcodec = avcodec_find_decoder(codecpar->codec_id);
    if (!avcodec) {
        // call_java->on_call_error(CHILD_THREAD, ret, "找不到对应的编解码器");
        exit = true;
        avformat_close_input(&av_format_context);
        avformat_free_context(av_format_context);
        av_format_context = NULL;

        pthread_mutex_unlock(&init_mutex);
        avformat_network_deinit();
        LOGE("找不到对应的编解码器");
        return -1;
    }

    *av_codec_context = avcodec_alloc_context3(avcodec);
    if (!(*av_codec_context)) {
        // call_java->on_call_error(CHILD_THREAD, ret, "分配编解码器上下文失败");

        exit = true;
        avcodec_close(*av_codec_context);
        avcodec_free_context(av_codec_context);
        *av_codec_context = NULL;

        avformat_close_input(&av_format_context);
        avformat_free_context(av_format_context);
        av_format_context = NULL;

        pthread_mutex_unlock(&init_mutex);
        avformat_network_deinit();
        LOGE("分配编解码器上下文失败");
        return -1;
    }

    ret = avcodec_parameters_to_context(*av_codec_context, codecpar);
    if (ret < 0) {
        call_java->on_call_error(CHILD_THREAD, ret, av_err2str(ret));
        exit = true;
        avcodec_close(*av_codec_context);
        avcodec_free_context(av_codec_context);
        *av_codec_context = NULL;

        avformat_close_input(&av_format_context);
        avformat_free_context(av_format_context);
        av_format_context = NULL;

        pthread_mutex_unlock(&init_mutex);
        avformat_network_deinit();
        LOGE("设置 编解码器上下文 参数失败");
        return -1;
    }

    // 打开编解码器
    ret = avcodec_open2(*av_codec_context, avcodec, NULL);
    if (ret != 0) {
        call_java->on_call_error(CHILD_THREAD, ret, av_err2str(ret));
        exit = true;
        avcodec_close(*av_codec_context);
        avcodec_free_context(av_codec_context);
        *av_codec_context = NULL;

        avformat_close_input(&av_format_context);
        avformat_free_context(av_format_context);
        av_format_context = NULL;

        pthread_mutex_unlock(&init_mutex);
        avformat_network_deinit();
        LOGE("打开编解码器失败");
        return -1;
    }
    return 0;
}

void YEFFmpeg::decode_ffmepg_thread() {
    pthread_mutex_lock(&init_mutex);
    // 初始化网络支持模块
    avformat_network_init();

    // 分配上下文
    av_format_context = avformat_alloc_context();

    int ret;
    ret = avformat_open_input(&av_format_context, url, NULL, NULL);
    if (ret != 0) {
        // 1. 回调给java层，通知打开文件失败
        call_java->on_call_error(CHILD_THREAD, ret, av_err2str(ret));
        // 2. 释放相关资源
        avformat_close_input(&av_format_context);
        avformat_free_context(av_format_context);
        av_format_context = NULL;
        avformat_network_deinit();
        LOGE("Failed to open file, the error is %s", av_err2str(ret));
        return;
    }

    ret = avformat_find_stream_info(av_format_context, NULL);
    if (ret < 0) {
        call_java->on_call_error(CHILD_THREAD, ret, av_err2str(ret));

        avformat_close_input(&av_format_context);
        avformat_free_context(av_format_context);
        av_format_context = NULL;
        avformat_network_deinit();
        LOGE("Couldn't open video, the error is %s", av_err2str(ret));
        return;
    }

    for (int i = 0; i < av_format_context->nb_streams; ++i) {
        // 得到音频流
        if (av_format_context->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            if (audio == NULL) {
                audio = new YEAudio(play_status,
                                    av_format_context->streams[i]->codecpar->sample_rate,
                                    call_java);
                audio->stream_index = i;
                // 音频时长，单位微秒，转换成秒
                audio->duration = (int) (av_format_context->duration / AV_TIME_BASE);
                audio->time_base = av_format_context->streams[i]->time_base;
                audio->codecpar = av_format_context->streams[i]->codecpar;
                duration = audio->duration;
            }
        }
            // 得到视频流
        else if (av_format_context->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            if (video == NULL) {
                video = new YEVideo(play_status, call_java);
                video->stream_index = i;
                video->codecpar = av_format_context->streams[i]->codecpar;
            }
        }
    }

    // 找不到音频流或者视频流
    if (audio->stream_index == -1) {
        call_java->on_call_error(CHILD_THREAD, ret, "There is no audio stream.");

        avformat_close_input(&av_format_context);
        avformat_free_context(av_format_context);
        av_format_context = NULL;
        avformat_network_deinit();
        LOGE("There is no audio stream.");
        // return;
    }
    if (video->stream_index == -1) {
        LOGE("There is no video stream.");
    }

    if (audio != NULL) {
        get_avcodec_context(audio->codecpar, &audio->avcodec_context);
    }

    if (video != NULL) {
        get_avcodec_context(video->codecpar, &video->avcodec_context);
    }

    call_java->on_call_prepared(CHILD_THREAD);
    pthread_mutex_unlock(&init_mutex);
}

void YEFFmpeg::start() {
    if (audio == NULL && video == NULL) {
        LOGE("audio and video are null");
        return;
    }
    if (audio != NULL) {
        audio->play();
    }
    if (video != NULL) {
        video->play();
    }

    int count = 0;
    while (play_status != NULL && !play_status->exit) {
        if (play_status->seek) {
            continue;
        }

        // 放入队列
        if (audio->queue->get_queue_size() > 40) {
            continue;
        }

        AVPacket *av_packet = av_packet_alloc();
        if (av_read_frame(av_format_context, av_packet) == 0) {
            if (av_packet->stream_index == audio->stream_index) {
                // 解码操作
                count++;
                LOGI("解码音频第 %d 帧", count);
                audio->queue->put_av_packet(av_packet);
            } else if (av_packet->stream_index == video->stream_index) {
                LOGI("解码视频第 %d 帧", count);
                video->queue->put_av_packet(av_packet);
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

void YEFFmpeg::seek(int64_t sec) {
    if (duration <= 0) {
        return;
    }

    if (sec >= 0 && sec <= duration) {
        play_status->seek = true;
        pthread_mutex_lock(&seek_mutex);
        int64_t rel = sec * AV_TIME_BASE;
        avformat_seek_file(av_format_context, -1, INT64_MIN, rel, INT64_MAX, 0);
        if (audio != NULL) {
            audio->queue->clear_av_packet();
            audio->clock = 0;
            audio->last_time = 0;
            avcodec_flush_buffers(audio->avcodec_context);
        }
        if (video != NULL) {
            video->queue->clear_av_packet();
            pthread_mutex_lock(&video->pthread_mutex);
            avcodec_flush_buffers(video->avcodec_context);
            pthread_mutex_unlock(&video->pthread_mutex);
        }
        pthread_mutex_unlock(&seek_mutex);
        play_status->seek = false;

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

void YEFFmpeg::set_mute(int mute) {
    if (audio != NULL) {
        audio->set_mute(mute);
    }
}

void YEFFmpeg::set_volume(int percent) {
    if (audio != NULL) {
        audio->set_volume(percent);
    }
}

void YEFFmpeg::set_speed(float speed) {
    if (audio != NULL) {
        audio->set_speed(speed);
    }
}

void YEFFmpeg::set_pitch(float pitch) {
    if (audio != NULL) {
        audio->set_pitch(pitch);
    }
}

void YEFFmpeg::release() {
    LOGE("开始释放YEFFmpeg");
    play_status->exit = true;
    int sleep_count = 0;
    pthread_mutex_lock(&init_mutex);
    while (!exit) {

        if (sleep_count > 1000) {
            exit = true;
        }

        LOGE("wait ffmpeg  exit %d", sleep_count);
        sleep_count++;
        av_usleep(1000 * 10);//暂停10毫秒
    }

    if (audio != NULL) {
        audio->release();
        delete (audio);
        audio = NULL;
    }

    LOGE("释放 封装格式上下文");
    if (av_format_context != NULL) {
        avformat_close_input(&av_format_context);
        avformat_free_context(av_format_context);
        av_format_context = NULL;
    }

    LOGE("释放 callJava");
    if (call_java != NULL) {
        call_java = NULL;
    }

    LOGE("释放 playstatus");
    if (play_status != NULL) {
        play_status = NULL;
    }
    pthread_mutex_unlock(&init_mutex);
}

