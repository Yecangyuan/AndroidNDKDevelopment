package com.simley.ndk_day78.opengl2.record;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Surface;
import androidx.annotation.RequiresApi;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 录制视频的工具类（内部封装了MediaCodec）
 */
public class MyMediaRecorder {

    private final int mWidth; // 宽度
    private final int mHeight; // 高度
    private final String mOutputPath; // 输出的路径
    private final EGLContext mEglContext; // EGL的上下文
    private final Context mContext; // 传递过来的上下文
    private MediaCodec mMediaCodec; // 音视频的编解码器
    private Surface mInputSurface; // MediaCodec的输入Surface画布
    private MediaMuxer mMediaMuxer; // 封装器（能够封装 xxx.mp4 视频文件）
    private Handler mHandler; // 定义Handler 为了切换到主loop
    private MyEGL mEGL; // ELG中间件【重要】
    private boolean isStart;
    private int index;
    private float mSpeed;
    private long lastTimeUs;

    public MyMediaRecorder(int width, int height, String outputPath, EGLContext eglContext, Context context) {
        mWidth = width;
        mHeight = height;
        mOutputPath = outputPath;
        mEglContext = eglContext;
        mContext = context;
    }

    /**
     * 录制 真正执行编码的函数
     * @param textureId 最终成效显示屏幕上的纹理ID
     * @param timestamp 时间戳
     */
    public void encodeFrame(final int textureId, final long timestamp) {
        if (!isStart) {
            return;
        }
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    // 画到 虚拟屏幕上
                    if (null != mEGL) {
                        mEGL.draw(textureId, timestamp);
                    }

                    // 从编码器中去除数据 编码 封装成derry_xxxx.mp4文件成果
                    getEncodedData(false);
                }
            });
        }
    }

    /**
     * 圆形红色按钮的 按住拍 的 开始录制
     * @param speed 录制的：极慢 慢 标准 快 极快
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void start(float speed) throws IOException {
        mSpeed = speed;
        /**
         * 1.创建 MediaCodec 编码器
         * type: 哪种类型的视频编码器
         * MIMETYPE_VIDEO_AVC： H.264
         */
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);

        /**
         * 2.配置编码器参数
         */
        // 视频格式
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,  mWidth, mHeight);
        // 设置码率
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1500_000);
        // 帧率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        // 颜色格式 （从Surface中自适应）
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        // 关键帧间隔
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 20);
        // 配置编码器
        mMediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        /**
         * 3.创建输入 Surface（虚拟屏幕）
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mInputSurface = mMediaCodec.createInputSurface();
        }

        /**
         * 4, 创建封装器
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }

        /**
         * 5, 配置 EGL 环境
         */
        HandlerThread handlerThread = new HandlerThread("MyMediaRecorder");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        mHandler = new Handler(looper);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mEGL = new MyEGL(mEglContext, mInputSurface, mContext, mWidth, mHeight); // 在一个新的Thread中，初始化EGL环境
                mMediaCodec.start(); // 启动编码器
                isStart = true;
            }
        });
    }

    /**
     * 圆形红色按钮的 按住拍 的 录制完成
     */
    public void stop() {
        isStart = false;
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    getEncodedData(true); // true代表：结束工作

                    if (mMediaCodec != null){ // MediaCodec 释放掉
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mMediaCodec.stop();
                            mMediaCodec.release();
                        }
                        mMediaCodec = null;
                    }

                    if (mMediaMuxer != null) { // 封装器 释放掉
                        try{
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                mMediaMuxer.stop();
                                mMediaMuxer.release();
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        mMediaMuxer = null;
                    }

                    if (mInputSurface != null) { // MediaCodec的输入画布/虚拟屏幕 释放掉
                        mInputSurface.release();
                        mInputSurface = null;
                    }

                    mEGL.release(); // EGL中间件 释放掉
                    mEGL = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        mHandler.getLooper().quitSafely();
                        mHandler = null;
                    }
                }
            });
        }
    }

    /**【MediaCodec输出缓冲区】
     * 获取编码后的数据，写入封装成 derry_xxxx.mp4
     * @param endOfStream 流结束的标记  true代表结束，false才继续工作
     */
    private void getEncodedData(boolean endOfStream) {
        if (endOfStream) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mMediaCodec.signalEndOfInputStream(); // 让MediaCodec的输入流结束
            }
        }

        // MediaCodec的输出缓冲区
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            while (true) {
                // 使输出缓冲区出列，最多阻止“timeoutUs”微秒。 // 10ms
                int status = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);

                if(status == MediaCodec.INFO_TRY_AGAIN_LATER) { // 稍后再试的意思
                    // endOfStream = true, 要录制，继续循环，继续取新的编码数据
                    if(!endOfStream){
                        break;
                    }
                } else if(status == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) { // 输出格式已更改
                    MediaFormat outputFormat = mMediaCodec.getOutputFormat();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        index = mMediaMuxer.addTrack(outputFormat);
                        mMediaMuxer.start();// 启动封装器
                    }
                } else if(status == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){ // 输出缓冲区已更改

                } else {
                    // 成功取到一个有效数据
                    ByteBuffer outputBuffer = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        outputBuffer = mMediaCodec.getOutputBuffer(status);
                    }
                    if (null == outputBuffer){
                        throw new RuntimeException("getOutputBuffer fail");
                    }

                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0){
                        bufferInfo.size = 0; // 如果是配置信息
                    }

                    // TODO 下面开始写入操作
                    if(bufferInfo.size != 0){
                        // 除以大于1的 ： 加速
                        // 小于1 的： 减速
                        bufferInfo.presentationTimeUs = (long)(bufferInfo.presentationTimeUs / mSpeed);
                        // 可能会出现类似：TimeUs < lastTimeUs xxxxxx for video Track
                        if(bufferInfo.presentationTimeUs <= lastTimeUs){
                            bufferInfo.presentationTimeUs = (long)(lastTimeUs + 1_000_000 /25/mSpeed);
                        }
                        lastTimeUs = bufferInfo.presentationTimeUs;

                        // 偏移位置
                        outputBuffer.position(bufferInfo.offset);
                        // 可读写的总长度
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        try{
                            // 写数据
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                mMediaMuxer.writeSampleData(index, outputBuffer, bufferInfo);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                    // 释放输出缓冲区
                    mMediaCodec.releaseOutputBuffer(status, false);
                    // 编码结束
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                        break;
                    }
                }
            } // while end
        }
    }
}
