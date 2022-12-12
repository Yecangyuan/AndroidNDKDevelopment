
//
// Created by Simle Y on 2022/12/8.
//

#include <jni.h>
#include "ye_log.h"
#include <android/native_window_jni.h>
#include <unistd.h>
#include "YECallJava.h"
#include "YEPlayStatus.h"
#include "YEFFmpeg.h"

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavdevice/avdevice.h"
#include "libavutil/imgutils.h"
#include "libswscale/swscale.h"
#include "libavutil/time.h"
#include "libswresample/swresample.h"
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_play(JNIEnv *env, jobject thiz, jstring url_,
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
Java_com_simley_ndk_1day78_player_YEPlayer_playSound(JNIEnv *env, jobject thiz, jstring url_) {
    const char *url = env->GetStringUTFChars(url_, JNI_FALSE);

    // 初始化网络支持模块
    avformat_network_init();

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
    // 分配av_frame对象，作为解码后数据的容器
    AVFrame *av_frame = av_frame_alloc();

    int out_channel_nb = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);

    SwrContext *swr_context = swr_alloc();
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    enum AVSampleFormat out_formart = AV_SAMPLE_FMT_S16;
    int out_sample_rate = av_codec_context->sample_rate;
//    转换器的代码
    swr_alloc_set_opts(swr_context, out_ch_layout, out_formart, out_sample_rate,
//            输出的
                       av_codec_context->channel_layout, av_codec_context->sample_fmt,
                       av_codec_context->sample_rate, 0, NULL
    );

//    AVChannelLayout *out_channel_layout;
//    AVChannelLayout *in_channel_layout;
//
////    out_channel_layout->nb_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
////    in_channel_layout->nb_channels = c->ch_layout->nb_channels;
//
//    if (swr_alloc_set_opts2(&swr_context, out_channel_layout, AV_SAMPLE_FMT_S16, out_sample_rate,
//                            in_channel_layout, av_codec_context->sample_fmt,
//                            av_codec_context->sample_rate, 0, NULL) != 0) {
//        LOGE("swr_alloc_set_opts2 failed.");
//        return;
//    }

    // 初始化转换上下文
    swr_init(swr_context);
    // 1s的pcm个数
    auto *out_buffer = (uint8_t *) av_malloc(44100 * 2);

    // 通过反射的方式调用java层的代码
    jclass divide_player = env->GetObjectClass(thiz);
    jmethodID createAudio = env->GetMethodID(divide_player, "createAudioTrack",
                                             "(II)Landroid/media/AudioTrack;");
    jobject audio_track = env->CallObjectMethod(thiz, createAudio, 44100, out_channel_nb);

    jclass audio_track_class = env->GetObjectClass(audio_track);
    jmethodID audio_track_play = env->GetMethodID(audio_track_class, "play", "()V");
    env->CallVoidMethod(audio_track, audio_track_play);

    jmethodID audio_track_write = env->GetMethodID(audio_track_class, "write", "([BII)I");

    while (av_read_frame(avformat_context, av_packet) >= 0) {
        if (av_packet->stream_index == audio_index) {
            ret = avcodec_send_packet(av_codec_context, av_packet);
            // LOGE("解码成功%d", ret);
            if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
                LOGE("解码出错");
                break;
            }
            ret = avcodec_receive_frame(av_codec_context, av_frame);
            if (ret < 0 && ret != AVERROR_EOF) {
                LOGE("读取出错");
                break;
            }
            if (ret >= 0) {
                swr_convert(swr_context, &out_buffer, 44100 * 2,
                            (const uint8_t **) (av_frame->data), av_frame->nb_samples);

                // 解码了
                int size = av_samples_get_buffer_size(NULL, out_channel_nb, av_frame->nb_samples,
                                                      AV_SAMPLE_FMT_S16, 1);
                // java的字节数组
                jbyteArray audio_sample_array = env->NewByteArray(size);
                env->SetByteArrayRegion(audio_sample_array, 0, size,
                                        reinterpret_cast<const jbyte *>(out_buffer));
                env->CallIntMethod(audio_track, audio_track_write, audio_sample_array, 0, size);
                env->DeleteLocalRef(audio_sample_array);
                usleep(1000 * 16);
            }
        }
    }
    LOGI("音频播放完毕");

    // 以下释放所有相关的资源
    av_free(out_buffer);
    swr_free(&swr_context);
    av_packet_free(&av_packet);
    av_frame_free(&av_frame);
    avcodec_free_context(&av_codec_context);
    avformat_free_context(avformat_context);
    avformat_network_deinit();
    env->ReleaseStringUTFChars(url_, url);
}


_JavaVM *java_vm = NULL;
YECallJava *call_java = NULL;
YEFFmpeg *ffmpeg = NULL;
YEPlayStatus *play_status = NULL;

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    jint result = -1;
    java_vm = vm;
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return result;
    }
    return JNI_VERSION_1_6;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1prepared(JNIEnv *env, jobject thiz, jstring source_) {
    const char *source = env->GetStringUTFChars(source_, JNI_FALSE);

    if (ffmpeg == NULL) {
        if (call_java == NULL) {
            call_java = new YECallJava(java_vm, env, &thiz);
        }
        play_status = new YEPlayStatus();
        ffmpeg = new YEFFmpeg(play_status, call_java, source);
        ffmpeg->prepared();
    } else {

    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1start(JNIEnv *env, jobject thiz) {
    if (ffmpeg != NULL) {
        ffmpeg->start();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1volume(JNIEnv *env, jobject thiz, jint percent) {
    if (ffmpeg != NULL) {
        ffmpeg->set_volume(percent);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1mute(JNIEnv *env, jobject thiz, jint mute) {
    if (ffmpeg != NULL) {
        ffmpeg->set_mute(mute);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1pause(JNIEnv *env, jobject thiz) {
    if (ffmpeg != NULL) {
        ffmpeg->pause();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1resume(JNIEnv *env, jobject thiz) {
    if (ffmpeg != NULL) {
        ffmpeg->resume();
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1seek(JNIEnv *env, jobject thiz, jint secds) {
    if (ffmpeg != NULL) {
        ffmpeg->seek(secds);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1speed(JNIEnv *env, jobject thiz, jfloat speed) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1pitch(JNIEnv *env, jobject thiz, jfloat pitch) {

}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1stop(JNIEnv *env, jobject thiz) {

}