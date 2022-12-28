//
// Created by Simle Y on 2022/12/28.
//

#include "YEVideo.h"


YEVideo::YEVideo(YEPlayStatus *play_status, YECallJava *ye_call_java) {
    this->play_status = play_status;
    this->ye_call_java = ye_call_java;
    queue = new YEQueue(play_status);
    pthread_mutex_init(&pthread_mutex, NULL);
}

YEVideo::~YEVideo() {

}

double YEVideo::get_frame_diff_time(AVFrame *av_frame) {
    // 先获取视频时间戳
//    double pts = av_frame_get_best_effort_timestamp(av_frame);
//    if (pts == AV_NOPTS_VALUE) {
//        pts = 0;
//    }
//    pts *= av_q2d(time_base);
//    if (pts > 0) {
//        clock = pts;
//    }
    double diff = audio->clock - clock;
    return diff;
}

double YEVideo::get_delay_time(double diff) {
    if (diff > 0.003) {
        // 视频休眠时间
        delay_time = delay_time * 2 / 3;
        if (delay_time < default_delay_time / 2) {
            // 用户有所察觉
            delay_time = default_delay_time * 2 / 3;
        } else if (delay_time > default_delay_time * 2) {
            delay_time = default_delay_time * 2;
        }
    } else if (diff < -0.003) {
        delay_time = delay_time * 3 / 2;
        if (delay_time < default_delay_time / 2) {
            delay_time = default_delay_time * 2 / 3;
        } else if (delay_time > default_delay_time * 2) {
            delay_time = default_delay_time * 2;
        }
    }
    // 感觉的视频加速
    if (diff >= 0.5) {
        delay_time = 0;
    } else if (diff <= -0.5) {
        delay_time = default_delay_time * 2;
    }

    if (diff >= 10) {
        queue->clear_av_packet();
        delay_time = default_delay_time;
    }

    if (diff <= -10) {
        audio->queue->clear_av_packet();
        delay_time = default_delay_time;
        LOGE("====================>视频太快了");
    }
    return delay_time;
}

void *play_video(void *data) {
    YEVideo *video = static_cast<YEVideo *>(data);
    // 死循环 轮训
    while (video->play_status != NULL && !video->play_status->exit) {
        // 解码 seek pause 队列没有数据
        if (video->play_status->seek) {
            av_usleep(1000 * 100);
            continue;
        }
        if (video->play_status->pause) {
            av_usleep(1000 * 100);
            continue;
        }
        if (video->queue->get_queue_size() == 0) {
            // 网络不佳 请慢慢等待 回调应用层
            if (!video->play_status->load) {
                video->play_status->load = true;
                video->ye_call_java->on_call_load(CHILD_THREAD, true);
                av_usleep(1000 * 100);
                continue;
            }
        }
        AVPacket *av_packet = av_packet_alloc();
        if (video->queue->get_av_packet(av_packet) != 0) {
            av_packet_free(&av_packet);
            av_free(av_packet);
            av_packet = NULL;
            continue;
        }
        // 视频解码 比较耗时 多线程环境
        pthread_mutex_lock(&video->pthread_mutex);
        if (avcodec_send_packet(video->avcodec_context, av_packet) != 0) {
            av_packet_free(&av_packet);
            av_free(av_packet);
            av_packet = NULL;
            pthread_mutex_unlock(&video->pthread_mutex);
            continue;
        }
        AVFrame *av_frame = av_frame_alloc();
        if (avcodec_receive_frame(video->avcodec_context, av_frame) != 0) {
            av_frame_free(&av_frame);
            av_free(av_frame);
            av_frame = NULL;
            av_packet_free(&av_packet);
            av_free(av_packet);
            av_packet = NULL;
            pthread_mutex_unlock(&video->pthread_mutex);
            continue;
        }
        // 解码成功
        if (av_frame->format == AV_PIX_FMT_YUV420P) {
            double diff = video->get_frame_diff_time(av_frame);
            // 通过diff计算休眠时间
            av_usleep(video->get_delay_time(diff) * 1000000);
            video->ye_call_java->on_call_render_yuv(
                    video->avcodec_context->width,
                    video->avcodec_context->height,
                    av_frame->data[0],
                    av_frame->data[1],
                    av_frame->data[2]);
            LOGI("当前视频是YUV420P格式");
        } else {
            LOGE("当前视频不是YUV420P格式");
            AVFrame *pframe_yuv_420p = av_frame_alloc();
            int num = av_image_get_buffer_size(AV_PIX_FMT_YUV420P,
                                               video->avcodec_context->width,
                                               video->avcodec_context->height,
                                               1);

            uint8_t *buffer = static_cast<uint8_t *>(av_malloc(num * sizeof(uint8_t)));
            av_image_fill_arrays(
                    pframe_yuv_420p->data,
                    pframe_yuv_420p->linesize,
                    buffer,
                    AV_PIX_FMT_YUV420P,
                    video->avcodec_context->width,
                    video->avcodec_context->height,
                    1);

            SwsContext *sws_ctx = sws_getContext(
                    video->avcodec_context->width,
                    video->avcodec_context->height,
                    video->avcodec_context->pix_fmt,
                    video->avcodec_context->width,
                    video->avcodec_context->height,
                    AV_PIX_FMT_YUV420P,
                    SWS_BICUBIC, NULL, NULL, NULL);

            if (!sws_ctx) {
                av_frame_free(&pframe_yuv_420p);
                av_free(pframe_yuv_420p);
                av_free(buffer);
                pthread_mutex_unlock(&video->pthread_mutex);
                continue;
            }
            sws_scale(
                    sws_ctx,
                    reinterpret_cast<const uint8_t *const *>(av_frame->data),
                    av_frame->linesize,
                    0,
                    av_frame->height,
                    pframe_yuv_420p->data,
                    pframe_yuv_420p->linesize);
            //渲染
            video->ye_call_java->on_call_render_yuv(
                    video->avcodec_context->width,
                    video->avcodec_context->height,
                    pframe_yuv_420p->data[0],
                    pframe_yuv_420p->data[1],
                    pframe_yuv_420p->data[2]);

            av_frame_free(&pframe_yuv_420p);
            av_free(pframe_yuv_420p);
            av_free(buffer);
            sws_freeContext(sws_ctx);
        }
        av_frame_free(&av_frame);
        av_free(av_frame);
        av_frame = NULL;
        av_packet_free(&av_packet);
        av_free(av_packet);
        av_packet = NULL;
        pthread_mutex_unlock(&video->pthread_mutex);
    }
    pthread_exit(&video->pthread_play);
}

void YEVideo::play() {
    pthread_create(&pthread_play, NULL, play_video, this);
}

