//
// Created by Simle Y on 2022/12/28.
//

#include "YEVideo.h"


YEVideo::YEVideo(YEPlayStatus *playStatus, YECallJava *callJava) {
    this->playStatus = playStatus;
    this->callJava = callJava;
    queue = new YEQueue(playStatus);
    pthread_mutex_init(&pthreadMutex, NULL);
}

YEVideo::~YEVideo() {}

double YEVideo::getFrameDiffTime(AVFrame *avFrame) {
    // 先获取视频时间戳
    double pts = avFrame->best_effort_timestamp;
    if (pts == AV_NOPTS_VALUE) {
        pts = 0;
    }
    pts *= timeBase.num / timeBase.den;
    if (pts > 0) {
        clock = pts;
    }
    // diff = 音频的当前时间 - 视频的当前时间
    double diff = audio->clock - clock;
    // diff > 0 表示 音频超前
    return diff;
}

double YEVideo::getDelayTime(double diff) {
    // 音频和视频之差大于 3ms到500ms，则进行干预
    // 音频和视频之间的差距大于 3ms 所以进行干预，小于3ms则不进行干预
    if (diff > 0.003) {
        // 视频休眠时间
        delayTime = delayTime * 2 / 3;
        if (delayTime < defaultDelayTime / 2) {
            // 用户有所察觉
            delayTime = defaultDelayTime * 2 / 3;
        } else if (delayTime > defaultDelayTime * 2) {
            delayTime = defaultDelayTime * 2;
        }
    }
        // 视频比较快，加大视频休眠时间， 让音频赶上来
    else if (diff < -0.003) {
        delayTime = delayTime * 3 / 2;
        if (delayTime < defaultDelayTime / 2) {
            delayTime = defaultDelayTime * 2 / 3;
        } else if (delayTime > defaultDelayTime * 2) {
            delayTime = defaultDelayTime * 2;
        }
    }
    // 感觉的视频加速
    if (diff >= 0.5) {
        delayTime = 0;
    } else if (diff <= -0.5) {
        delayTime = defaultDelayTime * 2;
    }

    // 音频和视频之差 大于10s，则直接让视频跳到音频播放的位置
    if (diff >= 10) {
        queue->clearAvPacket();
        delayTime = defaultDelayTime;
    }

    if (diff <= -10) {
        audio->queue->clearAvPacket();
        delayTime = defaultDelayTime;
        LOGE("====================>视频太快了");
    }
    return delayTime;
}

void *playVideo(void *data) {
    auto *video = static_cast<YEVideo *>(data);
    // 死循环 轮训
    while (video->playStatus != NULL && !video->playStatus->exit) {
        // 解码 seek pause 队列没有数据
        if (video->playStatus->seek) {
            av_usleep(1000 * 100);
            continue;
        }
        if (video->playStatus->pause) {
            av_usleep(1000 * 100);
            continue;
        }
        if (video->queue->getQueueSize() == 0) {
            // 网络不佳 请慢慢等待 回调应用层
            if (!video->playStatus->load) {
                video->playStatus->load = true;
                video->callJava->onCallLoad(CHILD_THREAD, true);
                av_usleep(1000 * 100);
                continue;
            }
        }
        AVPacket *avPacket = av_packet_alloc();
        if (video->queue->getAvPacket(avPacket) != 0) {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            continue;
        }
        // 视频解码 比较耗时 多线程环境
        pthread_mutex_lock(&video->pthreadMutex);
        if (avcodec_send_packet(video->avCodecContext, avPacket) != 0) {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            pthread_mutex_unlock(&video->pthreadMutex);
            continue;
        }

        AVFrame *avFrame = av_frame_alloc();
        if (avcodec_receive_frame(video->avCodecContext, avFrame) != 0) {
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            pthread_mutex_unlock(&video->pthreadMutex);
            continue;
        }
        // 解码成功
        if (avFrame->format == AV_PIX_FMT_YUV420P) {
            double diff = video->getFrameDiffTime(avFrame);

            // 通过diff计算休眠时间
            av_usleep(video->getDelayTime(diff) * 1000000);

            video->callJava->onCallRenderYuv(
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    avFrame->data[0],
                    avFrame->data[1],
                    avFrame->data[2]);
            LOGI("当前视频是YUV420P格式");
        } else {
            LOGE("当前视频不是YUV420P格式");
            AVFrame *pFrameYuv420P = av_frame_alloc();
            int num = av_image_get_buffer_size(AV_PIX_FMT_YUV420P,
                                               video->avCodecContext->width,
                                               video->avCodecContext->height,
                                               1);

            auto *buffer = static_cast<uint8_t *>(av_malloc(num * sizeof(uint8_t)));
            av_image_fill_arrays(
                    pFrameYuv420P->data,
                    pFrameYuv420P->linesize,
                    buffer,
                    AV_PIX_FMT_YUV420P,
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    1);

            SwsContext *sws_ctx = sws_getContext(
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    video->avCodecContext->pix_fmt,
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    AV_PIX_FMT_YUV420P,
                    SWS_BICUBIC, NULL, NULL, NULL);

            if (!sws_ctx) {
                av_frame_free(&pFrameYuv420P);
                av_free(pFrameYuv420P);
                av_free(buffer);
                pthread_mutex_unlock(&video->pthreadMutex);
                continue;
            }
            sws_scale(
                    sws_ctx,
                    reinterpret_cast<const uint8_t *const *>(avFrame->data),
                    avFrame->linesize,
                    0,
                    avFrame->height,
                    pFrameYuv420P->data,
                    pFrameYuv420P->linesize);
            //渲染
            video->callJava->onCallRenderYuv(
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    pFrameYuv420P->data[0],
                    pFrameYuv420P->data[1],
                    pFrameYuv420P->data[2]);

            av_frame_free(&pFrameYuv420P);
            av_free(pFrameYuv420P);
            av_free(buffer);
            sws_freeContext(sws_ctx);
        }
        av_frame_free(&avFrame);
        av_free(avFrame);
        avFrame = NULL;
        av_packet_free(&avPacket);
        av_free(avPacket);
        avPacket = NULL;
        pthread_mutex_unlock(&video->pthreadMutex);
    }
    pthread_exit(&video->pthreadPlay);
}

void YEVideo::play() {
    pthread_create(&pthreadPlay, NULL, playVideo, this);
}

