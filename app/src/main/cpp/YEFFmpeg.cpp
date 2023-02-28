//
// Created by Simle Y on 2022/12/10.
//

#include "YEFFmpeg.h"


YEFFmpeg::YEFFmpeg(YEPlayStatus *playStatus, YECallJava *callJava, const char *url) {
    this->playStatus = playStatus;
    this->callJava = callJava;
    this->url = new char[strlen(url) + 1];
    strcpy(this->url, url);


    pthread_mutex_init(&seekMutex, NULL);
    pthread_mutex_init(&initMutex, NULL);
}

YEFFmpeg::~YEFFmpeg() {
    delete this->url;
    avformat_network_deinit();
    pthread_mutex_destroy(&initMutex);
    pthread_mutex_destroy(&seekMutex);
}

void *onDecodeFfmpeg(void *data) {
    auto *wlFfmpeg = (YEFFmpeg *) data;
    wlFfmpeg->decodeFfmepgThread();
    pthread_exit(&wlFfmpeg->decodeThread);
}

void YEFFmpeg::prepared() {
    pthread_create(&decodeThread, NULL, onDecodeFfmpeg, this);
}

int
YEFFmpeg::getAvcodecContext(AVCodecParameters *codecParameters, AVCodecContext **avCodecContext) {
    int ret;
    const AVCodec *avcodec = avcodec_find_decoder(codecParameters->codec_id);
    if (!avcodec) {
        // callJava->onCallError(CHILD_THREAD, ret, "找不到对应的编解码器");
        exit = true;
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;

        pthread_mutex_unlock(&initMutex);
        avformat_network_deinit();
        LOGE("找不到对应的编解码器");
        return -1;
    }

    *avCodecContext = avcodec_alloc_context3(avcodec);
    if (!(*avCodecContext)) {
        // callJava->onCallError(CHILD_THREAD, ret, "分配编解码器上下文失败");

        exit = true;
        avcodec_close(*avCodecContext);
        avcodec_free_context(avCodecContext);
        *avCodecContext = NULL;

        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;

        pthread_mutex_unlock(&initMutex);
        avformat_network_deinit();
        LOGE("分配编解码器上下文失败");
        return -1;
    }

    ret = avcodec_parameters_to_context(*avCodecContext, codecParameters);
    if (ret < 0) {
        callJava->onCallError(CHILD_THREAD, ret, av_err2str(ret));
        exit = true;
        avcodec_close(*avCodecContext);
        avcodec_free_context(avCodecContext);
        *avCodecContext = NULL;

        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;

        pthread_mutex_unlock(&initMutex);
        avformat_network_deinit();
        LOGE("设置 编解码器上下文 参数失败");
        return -1;
    }

    // 打开编解码器
    ret = avcodec_open2(*avCodecContext, avcodec, NULL);
    if (ret != 0) {
        callJava->onCallError(CHILD_THREAD, ret, av_err2str(ret));
        exit = true;
        avcodec_close(*avCodecContext);
        avcodec_free_context(avCodecContext);
        *avCodecContext = NULL;

        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;

        pthread_mutex_unlock(&initMutex);
        avformat_network_deinit();
        LOGE("打开编解码器失败");
        return -1;
    }
    return 0;
}

void YEFFmpeg::decodeFfmepgThread() {
    pthread_mutex_lock(&initMutex);
    // 初始化网络支持模块
    avformat_network_init();

    // 分配上下文
    avFormatContext = avformat_alloc_context();

    AVDictionary *avDictionary = NULL;
    av_dict_set(&avDictionary, "timeout", "5000000", 0);

    int ret;
    ret = avformat_open_input(&avFormatContext, url, NULL, &avDictionary);
    if (ret != 0) {
        // 1. 回调给java层，通知打开文件失败
        callJava->onCallError(CHILD_THREAD, ret, av_err2str(ret));
        // 2. 释放相关资源
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
        avformat_network_deinit();
        LOGE("Failed to open file, the error is %s", av_err2str(ret));
        return;
    }

    ret = avformat_find_stream_info(avFormatContext, NULL);
    if (ret < 0) {
        callJava->onCallError(CHILD_THREAD, ret, av_err2str(ret));

        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
        avformat_network_deinit();
        LOGE("Couldn't open video, the error is %s", av_err2str(ret));
        return;
    }

    for (int streamIndex = 0; streamIndex < avFormatContext->nb_streams; ++streamIndex) {
        // 得到音频流
        if (avFormatContext->streams[streamIndex]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            if (audio == NULL) {
                audio = new YEAudio(playStatus,
                                    avFormatContext->streams[streamIndex]->codecpar->sample_rate,
                                    callJava);
                audio->streamIndex = streamIndex;
                // 音频时长，单位微秒，转换成秒
                audio->duration = avFormatContext->duration / AV_TIME_BASE;
                audio->timeBase = avFormatContext->streams[streamIndex]->time_base;
                audio->codecParameters = avFormatContext->streams[streamIndex]->codecpar;
                duration = audio->duration;
            }
        } else if (avFormatContext->streams[streamIndex]->codecpar->codec_type ==
                   AVMEDIA_TYPE_VIDEO) { // 得到视频流
            if (video == NULL) {
                video = new YEVideo(playStatus, callJava);
                video->streamIndex = streamIndex;
                video->codecParameters = avFormatContext->streams[streamIndex]->codecpar;
                video->timeBase = avFormatContext->streams[streamIndex]->time_base;
                // 默认  40ms  帧率   25   40ms    读取帧率     25帧    1       50  /2
                int num = avFormatContext->streams[streamIndex]->avg_frame_rate.num; // 分子(numerator)
                int den = avFormatContext->streams[streamIndex]->avg_frame_rate.den; // 分母(denominator)
                // 25帧    平均下来  40ms     60帧    16,.66ms      3  defaultDelayTime   默认
                if (num != 0 && den != 0) {
                    int fps = num / den;//[25 / 1]
                    video->defaultDelayTime = 1.0 / fps;//秒
                }
            }
        }
    }

    // 找不到音频流
//    if (audio->streamIndex == -1) {
//        callJava->onCallError(CHILD_THREAD, ret, "There is no audio stream.");
//
//        avformat_close_input(&avFormatContext);
//        avformat_free_context(avFormatContext);
//        avFormatContext = NULL;
//        avformat_network_deinit();
//        LOGE("There is no audio stream.");
//        // return;
//    }
//    // 找不到视频流
//    if (video->streamIndex == -1) {
//        LOGE("There is no video stream.");
//    }

    if (audio != NULL) {
        getAvcodecContext(audio->codecParameters, &audio->avcodecContext);
    }

    if (video != NULL) {
        getAvcodecContext(video->codecParameters, &video->avCodecContext);
    }

    callJava->onCallPrepared(CHILD_THREAD);
    pthread_mutex_unlock(&initMutex);
}

void YEFFmpeg::start() {
    if (audio == NULL) {
        LOGE("audio is null");
        return;
    }
    audio->play();
    video->play();
    video->audio = audio;

    int count = 0;
    while (playStatus != NULL && !playStatus->exit) {
        if (playStatus->seek) {
            continue;
        }

        // 放入队列
        if (audio->queue->getQueueSize() > 40) {
            continue;
        }

        AVPacket *avPacket = av_packet_alloc();
        if (av_read_frame(avFormatContext, avPacket) == 0) {
            if (avPacket->stream_index == audio->streamIndex) {
                // 解码操作
                count++;
                LOGI("解码音频第 %d 帧", count);
                audio->queue->putAvPacket(avPacket);
            } else if (avPacket->stream_index == video->streamIndex) {
                LOGI("解码视频第 %d 帧", count);
                video->queue->putAvPacket(avPacket);
            } else {
                av_packet_free(&avPacket);
                av_free(avPacket);
            }
        } else {
            av_packet_free(&avPacket);
            av_free(avPacket);
            while (playStatus != NULL && !playStatus->exit) {
                if (audio->queue->getQueueSize() > 0) {
                    continue;
                } else {
                    playStatus->exit = true;
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
        playStatus->seek = true;
        pthread_mutex_lock(&seekMutex);
        int64_t rel = sec * AV_TIME_BASE;
        LOGE("rel time %ld", sec);
        avformat_seek_file(avFormatContext, -1, INT64_MIN, rel, INT64_MAX, 0);
        if (audio != NULL) {
            audio->queue->clearAvPacket();
            audio->clock = 0;
            audio->lastTime = 0;
            avcodec_flush_buffers(audio->avcodecContext);
        }
        if (video != NULL) {
            video->queue->clearAvPacket();
            pthread_mutex_lock(&video->pthreadMutex);
            avcodec_flush_buffers(video->avCodecContext);
            pthread_mutex_unlock(&video->pthreadMutex);
        }
        pthread_mutex_unlock(&seekMutex);
        playStatus->seek = false;

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

void YEFFmpeg::setMute(int mute) {
    if (audio != NULL) {
        audio->setMute(mute);
    }
}

void YEFFmpeg::setVolume(int percent) {
    if (audio != NULL) {
        audio->setVolume(percent);
    }
}

void YEFFmpeg::setSpeed(float speed) {
    if (audio != NULL) {
        audio->setSpeed(speed);
    }
}

void YEFFmpeg::setPitch(float pitch) {
    if (audio != NULL) {
        audio->setPitch(pitch);
    }
}

void YEFFmpeg::release() {
    LOGE("开始释放YEFFmpeg");
    playStatus->exit = true;
    int sleep_count = 0;
    pthread_mutex_lock(&initMutex);
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
    if (avFormatContext != NULL) {
        avformat_close_input(&avFormatContext);
        avformat_free_context(avFormatContext);
        avFormatContext = NULL;
    }

    LOGE("释放 callJava");
    if (callJava != NULL) {
        callJava = NULL;
    }

    LOGE("释放 playstatus");
    if (playStatus != NULL) {
        playStatus = NULL;
    }
    pthread_mutex_unlock(&initMutex);
}

