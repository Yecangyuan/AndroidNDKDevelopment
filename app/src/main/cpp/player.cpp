
//
// Created by Simle Y on 2022/12/8.
//

#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavdevice/avdevice.h"
#include "libavutil/imgutils.h"
#include "libswscale/swscale.h"
#include "libavutil/time.h"
}

#define TAG "PLAYER_TAG"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_Player_play(JNIEnv *env, jobject thiz, jstring url_,
                                              jobject surface) {
    const char *url = env->GetStringUTFChars(url_, JNI_FALSE);
    // 获取上下文对象
    AVFormatContext *avformat_context = avformat_alloc_context();

    int ret = avformat_open_input(&avformat_context, url, NULL, NULL);
    if (ret != 0) {
        LOGE("Failed to open file, return value is %d", ret);
        return;
    }

    if (avformat_find_stream_info(avformat_context, NULL) < 0) {
        LOGE("Couldn't open video");
        return;
    }

    // 获取视频流索引
    int video_index = -1;
    for (int i = 0; i < avformat_context->nb_streams; ++i) {
        if (avformat_context->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = i;
            break;
        }
    }
    if (video_index == -1) {
        LOGE("There is no video stream.");
        return;
    }

    // 解码这个媒体流的参数信息 , 包含 码率 , 宽度 , 高度 , 采样率 等参数信息
    AVCodecParameters *codecpar = avformat_context->streams[video_index]->codecpar;
    const AVCodec *avcodec = avcodec_find_decoder(codecpar->codec_id);

    // 获取编解码器的上下文
    AVCodecContext *av_codec_context = avcodec_alloc_context3(avcodec);
    // 设置 编解码器上下文 参数
    if (avcodec_parameters_to_context(av_codec_context, codecpar) < 0) {
        LOGE("设置 编解码器上下文 参数失败");
        return;
    }

    // 打开编解码器
    if (avcodec_open2(av_codec_context, avcodec, NULL) != 0) {
        LOGE("打开编解码器失败");
        return;
    }

    ANativeWindow *native_window = ANativeWindow_fromSurface(env, surface);
    if (native_window == 0) {
        LOGE("failed to convert surface to native window");
        return;
    }

    AVFrame *av_frame = av_frame_alloc();
    // AVPacket *av_packet = static_cast<AVPacket *>(malloc(sizeof(AVPacket)));
    AVPacket *av_packet = av_packet_alloc();

    AVFrame *rgb_frame = av_frame_alloc();

    int width = av_codec_context->width;
    int height = av_codec_context->height;

    int num_byte = av_image_get_buffer_size(AV_PIX_FMT_RGBA, width, height, 1);

    auto *out_buffer = static_cast<uint8_t *>(av_malloc(num_byte * sizeof(uint8_t)));

    av_image_fill_arrays(rgb_frame->data, rgb_frame->linesize, out_buffer, AV_PIX_FMT_RGBA, width,
                         height, 1);


    // 获取转换器上下文
    SwsContext *sws_context = sws_getContext(width, height, av_codec_context->pix_fmt,
                                             width, height, AV_PIX_FMT_RGBA, SWS_BICUBIC, NULL,
                                             NULL,
                                             NULL);

    if (ANativeWindow_setBuffersGeometry(native_window, width, height, WINDOW_FORMAT_RGBA_8888) !=
        0) {
        LOGE("Could not set buffer to geometry.");
        ANativeWindow_release(native_window);
        return;
    }


    // 读取视频帧，使用av_packet来作为容器
    while (av_read_frame(avformat_context, av_packet) >= 0) {
        // 解码
        // avcodec_send_packet();
        // avcodec_receive_frame();

        // 编码
        // avcodec_send_frame();
        //avcodec_receive_packet();
        if (av_packet->stream_index == video_index) {
            int ret = avcodec_send_packet(av_codec_context, av_packet);
            if (ret != 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
                LOGE("解码出错");
                return;
            }

            ret = avcodec_receive_frame(av_codec_context, av_frame);
            if (ret == AVERROR(EAGAIN)) {
                continue;
            } else if (ret < 0) {
                break;
            }

            // 未压缩数据
            sws_scale(sws_context, av_frame->data, av_frame->linesize, 0, av_codec_context->height,
                      rgb_frame->data, rgb_frame->linesize);
            ANativeWindow_Buffer windowBuffer;
            if (ANativeWindow_lock(native_window, &windowBuffer, NULL) < 0) {
                LOGE("cannot lock window");
            } else {
                // 将图像绘制到屏幕上，注意这里的rgb_frame一行的像素可能和windowBuffer的像素不一样
                // 需要转换好，否则可能花屏
                uint8_t *dst = static_cast<uint8_t *>(windowBuffer.bits);
                for (int i = 0; i < height; ++i) {
                    memcpy(dst + i * windowBuffer.stride * 4,
                           out_buffer + i * rgb_frame->linesize[0],
                           rgb_frame->linesize[0]);
                }

                switch (av_frame->pict_type) {
                    case AV_PICTURE_TYPE_I:
                        LOGE("I帧");
                        break;
                    case AV_PICTURE_TYPE_B:
                        LOGE("B帧");
                        break;
                    case AV_PICTURE_TYPE_P:
                        LOGE("P帧");
                    default:
                        break;
                }
            }
            av_usleep(1000 * 33);
            ANativeWindow_unlockAndPost(native_window);
        }
    }

    ANativeWindow_release(native_window);
    av_free(out_buffer);
    av_frame_free(&rgb_frame);
    // free(av_packet);
    av_packet_free(&av_packet);
    av_frame_free(&av_frame);
    // 释放编解码上下文对象
    avcodec_free_context(&av_codec_context);
    // 释放上下文对象
    avformat_free_context(avformat_context);
    env->ReleaseStringUTFChars(url_, url);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_Player_playSound(JNIEnv *env, jobject thiz, jstring url_) {
    const char *url = env->GetStringUTFChars(url_, JNI_FALSE);
    AVFormatContext *avformat_context = avformat_alloc_context();

    int ret = avformat_open_input(&avformat_context, url, NULL, NULL);
    if (ret != 0) {
        LOGE("Failed to open file, return value is %d", ret);
        return;
    }

    if (avformat_find_stream_info(avformat_context, NULL) < 0) {
        LOGE("Couldn't open video");
        return;
    }

    // 获取视频流索引
    int audio_index = -1;
    for (int i = 0; i < avformat_context->nb_streams; ++i) {
        if (avformat_context->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_index = i;
            break;
        }
    }
    if (audio_index == -1) {
        LOGE("There is no audio stream.");
        return;
    }

    AVCodecParameters *codecpar = avformat_context->streams[audio_index]->codecpar;
    const AVCodec *avcodec = avcodec_find_decoder(codecpar->codec_id);

    AVCodecContext *av_codec_context = avcodec_alloc_context3(avcodec);
    if (avcodec_parameters_to_context(av_codec_context, codecpar) < 0) {
        LOGE("设置 编解码器上下文 参数失败");
        return;
    }

    // 打开编解码器
    if (avcodec_open2(av_codec_context, avcodec, NULL) != 0) {
        LOGE("打开编解码器失败");
        return;
    }
    AVPacket *av_packet = av_packet_alloc();
    AVFrame *av_frame = av_frame_alloc();

    int out_channel_nb = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);

    // 通过反射的方式调用java层的代码
    jclass divide_player = env->GetObjectClass(thiz);
    jmethodID createAudio = env->GetMethodID(divide_player, "createAudioTrack", "(II)V");
    env->CallVoidMethod(thiz, createAudio, 44100, out_channel_nb);
    while (av_read_frame(avformat_context, av_packet) >= 0) {
        if (av_packet->stream_index == audio_index) {
            int ret = avcodec_send_packet(av_codec_context, av_packet);
            if (ret != 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
                LOGE("解码出错");
                return;
            }

            ret = avcodec_receive_frame(av_codec_context, av_frame);
            if (ret == AVERROR(EAGAIN)) {
                continue;
            } else if (ret < 0) {
                break;
            }

        }
    }

    av_packet_free(&av_packet);
    av_frame_free(&av_frame);
    avcodec_free_context(&av_codec_context);
    avformat_free_context(avformat_context);
    env->ReleaseStringUTFChars(url_, url);

}