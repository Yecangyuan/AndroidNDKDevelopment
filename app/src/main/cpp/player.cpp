
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

#define AUDIO_SAMPLE_RATE 44100 // 采样率

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_play(JNIEnv *env, jobject thiz, jstring url_,
                                                jobject surface) {
    const char *url = env->GetStringUTFChars(url_, JNI_FALSE);
    // 获取上下文对象
    AVFormatContext *avformat_context = avformat_alloc_context();

    // Open an input stream and read the header
    if (avformat_open_input(&avformat_context, url, NULL, NULL)) {

        LOGE("打开文件失败，文件可能不存在");
        return;
    }

    // Read packets of a media file to get stream information
    if (avformat_find_stream_info(avformat_context, NULL) < 0) {
        LOGE("读取媒体文件的流信息失败");
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

jobject init_audio_track(JNIEnv *env, jobject thiz, int out_channel_nb) {
    // 通过反射的方式调用java层的代码 去创建音频轨对象
    jclass divide_player = env->GetObjectClass(thiz);
    jmethodID createAudio = env->GetMethodID(divide_player, "createAudioTrack",
                                             "(II)Landroid/media/AudioTrack;");
    jobject audio_track = env->CallObjectMethod(thiz, createAudio, AUDIO_SAMPLE_RATE,
                                                out_channel_nb);
    return audio_track;
}

jmethodID get_audio_track_write(JNIEnv *env, jobject audio_track) {
    jclass audio_track_class = env->GetObjectClass(audio_track);
    jmethodID audio_track_play = env->GetMethodID(audio_track_class, "play", "()V");
    env->CallVoidMethod(audio_track, audio_track_play);

    jmethodID audio_track_write = env->GetMethodID(audio_track_class, "write", "([BII)I");
    return audio_track_write;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_playSound(JNIEnv *env, jobject thiz, jstring url_) {
    const char *url = env->GetStringUTFChars(url_, JNI_FALSE);

    AVFormatContext *avformat_context = avformat_alloc_context();

    // 初始化网络支持模块
    avformat_network_init();


    int ret;
    ret = avformat_open_input(&avformat_context, url, NULL, NULL);
    if (ret != 0) {
        // 1. 回调给java层，通知打开文件失败
//        jclass divide_player = env->GetObjectClass(thiz);
//        jmethodID onError = env->GetMethodID(divide_player, "onError", "(ILjava/lang/String;)V");
//        char *a = av_err2str(ret);
//        env->CallVoidMethod(thiz, onError, ret, av_err2str(ret));
        // 2. 释放相关资源
        avformat_close_input(&avformat_context);
        avformat_free_context(avformat_context);
        avformat_context = NULL;
        avformat_network_deinit();
        LOGE("Failed to open file, the error is %s", av_err2str(ret));
        return;
    }

    ret = avformat_find_stream_info(avformat_context, NULL);
    if (ret < 0) {
        avformat_close_input(&avformat_context);
        avformat_free_context(avformat_context);
        avformat_context = NULL;
        avformat_network_deinit();
        LOGE("Couldn't open video, the error is %s", av_err2str(ret));
        return;
    }

    // int audio_index = av_find_best_stream(avformat_context, AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0);
    // 获取音频流索引
    int audio_index = -1;
    int video_index = -1;
    for (int i = 0; i < avformat_context->nb_streams; ++i) {
        // 获取到音频索引
        if (avformat_context->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_index = i;
            break;
        }
            // TODO 获取到视频流索引
        else if (avformat_context->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_index = i;
        }
    }
    if (audio_index == -1) {
        avformat_close_input(&avformat_context);
        avformat_free_context(avformat_context);
        avformat_context = NULL;
        avformat_network_deinit();
        LOGE("There is no audio stream.");
        return;
    }

    AVCodecParameters *codecpar = avformat_context->streams[audio_index]->codecpar;
    const AVCodec *avcodec = avcodec_find_decoder(codecpar->codec_id);
    AVCodecContext *av_codec_context = avcodec_alloc_context3(avcodec);
    if (avcodec_parameters_to_context(av_codec_context, codecpar) < 0) {
        avcodec_close(av_codec_context);
        avcodec_free_context(&av_codec_context);
        av_codec_context = NULL;

        avformat_close_input(&avformat_context);
        avformat_free_context(avformat_context);
        avformat_context = NULL;

        avformat_network_deinit();
        LOGE("设置 编解码器上下文 参数失败");
        return;
    }

    // 打开编解码器
    if (avcodec_open2(av_codec_context, avcodec, NULL) != 0) {
        avcodec_close(av_codec_context);
        avcodec_free_context(&av_codec_context);
        av_codec_context = NULL;

        avformat_close_input(&avformat_context);
        avformat_free_context(avformat_context);
        avformat_context = NULL;

        avformat_network_deinit();
        LOGE("打开编解码器失败");
        return;
    }

    AVPacket *av_packet = av_packet_alloc();
    // 分配av_frame对象，作为解码后数据的容器
    AVFrame *av_frame = av_frame_alloc();

    int out_channel_nb = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);

    SwrContext *swr_context = swr_alloc();
    uint64_t out_ch_layout = AV_CH_LAYOUT_STEREO;
    enum AVSampleFormat out_format = AV_SAMPLE_FMT_S16;
    int out_sample_rate = av_codec_context->sample_rate;
    // 转换器的代码
    swr_alloc_set_opts(swr_context, out_ch_layout, out_format, out_sample_rate,
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
//                            in_channel_layout, avcodec_context->sample_fmt,
//                            avcodec_context->sample_rate, 0, NULL) != 0) {
//        LOGE("swr_alloc_set_opts2 failed.");
//        return;
//    }

    // 初始化转换上下文
    swr_init(swr_context);
    // 1s的pcm个数
    auto *out_buffer = (uint8_t *) av_malloc(44100 * 2);

    // 初始化audio_track
    jobject audio_track = init_audio_track(env, thiz, out_channel_nb);
    jmethodID audio_track_write = get_audio_track_write(env, audio_track);

    int size = av_samples_get_buffer_size(NULL, out_channel_nb, av_frame->nb_samples,
                                          av_codec_context->sample_fmt, 1);
    // java的字节数组
    jbyteArray audio_sample_array = env->NewByteArray(size);

    // 每次调用av_read_frame方法都会 读取流数据的下一帧并返回，读不到就退出
    while (av_read_frame(avformat_context, av_packet) >= 0) {
        // 音频
        if (av_packet->stream_index == audio_index) {
            // 1.调用avcodec_send_packet()方法发送一个未解码的packet
            ret = avcodec_send_packet(av_codec_context, av_packet);
            // LOGE("解码成功%d", ret);
            if (ret < 0 && ret != AVERROR(EAGAIN) && ret != AVERROR_EOF) {
                LOGE("解码出错");
                break;
            }
            // 2.调用avcodec_receive_frame()方法接收一个解码后的frame
            ret = avcodec_receive_frame(av_codec_context, av_frame);
            if (ret < 0 && ret != AVERROR_EOF) {
                LOGE("读取出错");
                break;
            }
            if (ret >= 0) {
                swr_convert(swr_context, &out_buffer, 44100 * 2,
                            (const uint8_t **) (av_frame->data), av_frame->nb_samples);

                // 1s的pcm个数
                // jbyte *out_buffer = env->GetByteArrayElements(audio_sample_array, NULL);
                // memcpy(out_buffer, av_frame->data, size);
                env->SetByteArrayRegion(audio_sample_array, 0, size,
                                        reinterpret_cast<const jbyte *>(out_buffer));

//                env->ReleaseByteArrayElements(audio_sample_array,
//                                              reinterpret_cast<jbyte *>(out_buffer), JNI_COMMIT);

                env->CallIntMethod(audio_track, audio_track_write, audio_sample_array, 0, size);
                usleep(1000 * 16);
            }
        }
            // TODO 视频
        else if (av_packet->stream_index == video_index) {

        }
        // 解引用
        av_packet_unref(av_packet);
        av_frame_unref(av_frame);

    }
    LOGI("音频播放完毕，开始释放相关资源");

    env->DeleteLocalRef(audio_track);
    env->DeleteLocalRef(audio_sample_array);

    if (out_buffer != NULL) {
        av_free(out_buffer);
    }

    if (swr_context != NULL) {
        swr_free(&swr_context);
    }

    if (av_packet != NULL) {
        // 1.解引用data   2.销毁av_packet结构体 3.av_packet = NULL
        av_packet_free(&av_packet);
    }

    if (av_frame != NULL) {
        av_frame_free(&av_frame);
    }

    avcodec_free_context(&av_codec_context);

    if (avformat_context != NULL) {
        avformat_free_context(avformat_context);
    }

    avformat_network_deinit();
}

/*
 * 释放相关资源
 */
//void ReleaseRes() {
//
//    env->ReleaseStringUTFChars(url_, url);
//
//    if (av_forma != NULL) {
//
//    }
//    if () {
//
//    }
//}

_JavaVM *java_vm = NULL;
YECallJava *call_java = NULL;
YEFFmpeg *ffmpeg = NULL;
YEPlayStatus *play_status = NULL;

/*
 * 这个JNI_OnLoad()函数就相当于Java层的构造方法，会在Java层调用loadLibrary()方法的时候被调用
 * 如果不重写JNI_OnLoad()方法，则
 */
extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *javaVm, void *reserved) {
    ::java_vm = javaVm;

    JNIEnv *env = NULL;
    jint result = ::java_vm->GetEnv((void **) &env, JNI_VERSION_1_6);
    if (result != JNI_OK) {
        LOGD("加载JNIEnv失败");
        return result;
    }
    LOGD("加载JNIEnv成功");
    return JNI_VERSION_1_6; // 在AS的JDK在JNI默认最高是1.6    在Java的JDK在JNI可以是1.8或者更高
}

extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1prepared(JNIEnv *env, jobject thiz, jstring source_) {
    const char *source = env->GetStringUTFChars(source_, JNI_FALSE);

    if (ffmpeg == NULL) {

        if (call_java == NULL) {
            /*
             * JNIEnv *env：env传递不能跨越线程，否则崩溃。可以跨越函数，该env对象是绑定于线程的，所以只能在当前线程中使用
             *      【解决方法】：在子线程中创建一个新的JNIEnv *jniEnv，并调用JavaVM的AttachCurrentThread()方法
             *              将这个jniEnv绑定到当前的线程
             * jobject thiz: thiz传递不能跨越线程，不能跨越函数，否则崩溃。
             *      【解决方法】：该变量默认是局部变量，将其提升为全局变量即可解决问题
             * _JavaVM *java_vm：java_vm传递可以跨线程和函数，因为JavaVM是Java虚拟机，是全局的，一个进程（应用）只有一个JavaVM对象
             */
            call_java = new YECallJava(java_vm, env, &thiz);
        }

        play_status = new YEPlayStatus();
        ffmpeg = new YEFFmpeg(play_status, call_java, source);
        ffmpeg->prepared();
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
    if (ffmpeg != NULL) {
        ffmpeg->set_speed(speed);
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1pitch(JNIEnv *env, jobject thiz, jfloat pitch) {
    if (ffmpeg != NULL) {
        ffmpeg->set_pitch(pitch);
    }
}

bool n_exit = true;
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1stop(JNIEnv *env, jobject thiz) {
    if (!n_exit) {
        return;
    }
    n_exit = false;
    if (ffmpeg != NULL) {
        ffmpeg->release();
        delete ffmpeg;
        if (call_java != NULL) {
            delete call_java;
            call_java = NULL;
        }
        if (play_status != NULL) {
            delete play_status;
            play_status = NULL;
        }
    }
    n_exit = true;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1set_1looping(JNIEnv *env, jobject thiz,
                                                           jboolean looping) {

}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_simley_ndk_1day78_player_YEPlayer_n_1is_1looping(JNIEnv *env, jobject thiz) {

}