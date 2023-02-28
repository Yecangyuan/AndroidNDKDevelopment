package com.simley.ndk_day78.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.simley.ndk_day78.opengl.YEGLSurfaceView;
import com.simley.ndk_day78.utils.ThreadPool;

/**
 * 音视频播放器
 */
public class YEPlayer {

    private String mSource; // 数据源
    private IPlayerListener iPlayerListener;  // 音视频播放回调接口

    private WlOnPreparedListener wlOnPreparedListener; // 回调函数，当数据准备好的时候native层会回调该接口的onPrepared()方法

    private YEGLSurfaceView yeglSurfaceView;

    private final PlayState playState = PlayState.STARTED;
    private int duration; // 播放时长

    /**
     * 开始播放
     *
     * @param url
     * @param surface
     */
    public native void play(String url, Surface surface);

    /**
     * 获取视频宽度
     *
     * @return
     */
    public native int getVideoWidth();

    /**
     * 获取视频高度
     *
     * @return
     */
    public native int getVideoHeight();

//    public native void playSound(String url);

    /**
     * 准备播放
     *
     * @param source
     */
    private native void nativePrepared(String source);

    /**
     * 开始播放
     */
    private native void nativeStart();

    /**
     * 释放资源
     */
    private native void nativeRelease();

    /**
     * 跳转到 指定播放位置
     *
     * @param sec
     */
    private native void nativeSeek(int sec);

    /**
     * 继续播放
     */
    private native void nativeResume();

    /**
     * 暂停播放
     */
    private native void nativePause();

    /**
     * 设置静音
     *
     * @param mute
     */
    private native void nativeMute(int mute);

    /**
     * 设置 循环播放
     *
     * @param looping
     */
    public native void nativeSetLooping(boolean looping);

    /**
     * 设置音量
     *
     * @param percent
     */
    private native void nativeVolume(int percent);

    /**
     * 设置播放速度
     *
     * @param speed
     */
    private native void nativeSpeed(float speed);

    /**
     * 设置音调
     *
     * @param pitch
     */
    private native void nativePitch(float pitch);

    /**
     * 停止播放
     */
    private native void nativeStop();

    /**
     * 是否循环播放
     *
     * @return
     */
    private native boolean nativeIsLooping();

    /**
     * 是否正在播放
     *
     * @return
     */
    public boolean isPlaying() {
        return playState == PlayState.PLAYING;
    }

    /**
     * 音频变速
     *
     * @param speed
     */
    public void setSpeed(float speed) {
        nativeSpeed(speed);
    }

    /**
     * 设置 音高
     *
     * @param pitch
     */
    public void setPitch(float pitch) {
        nativePitch(pitch);
    }

    /**
     * 设置循环播放
     *
     * @param looping
     */
    public void setLooping(boolean looping) {
        nativeSetLooping(looping);
    }

    /**
     * 是否循环播放中
     *
     * @return
     */
    public boolean isLooping() {
        return nativeIsLooping();
    }

    /**
     * 停止播放
     */
    public void stop() {
        new MediaPlayer().start();
        ThreadPool.getInstance().execute(this::nativeStop);
    }

    public void prepare() {
        if (TextUtils.isEmpty(mSource)) {
            return;
        }
        ThreadPool.getInstance().execute(() -> nativePrepared(mSource));
    }

    public void onCallLoad(boolean load) {

    }

    public void onCallRenderYUV(int width, int height, byte[] y, byte[] u, byte[] v) {
        if (yeglSurfaceView != null) {
            yeglSurfaceView.setYUVData(width, height, y, u, v);
        }
    }

    public int getDuration() {
        return duration;
    }


    /**
     * 开始播放
     */
    public void start() {
        if (TextUtils.isEmpty(mSource)) {
            throw new IllegalArgumentException("datasource is null, please set the datasource by calling setDataSource method.");
        }
        ThreadPool.getInstance().execute(this::nativeStart);
    }

    public void setDataSource(String dataSource) {
        this.mSource = dataSource;
    }

    public void onCallPrepared() {
        if (wlOnPreparedListener != null) {
            wlOnPreparedListener.onPrepared();
        }
    }

    public void setPlayerListener(IPlayerListener iPlayerListener) {
        this.iPlayerListener = iPlayerListener;
    }

    public void onCallTimeInfo(int currentTime, int totalTime) {
        if (iPlayerListener != null) {
            duration = totalTime;
            iPlayerListener.onCurrentTime(currentTime, totalTime);
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        nativeRelease();
    }


    public void onError(int errorCode, String errorMsg) {
        if (iPlayerListener != null) {
            iPlayerListener.onError(errorCode, errorMsg);
        }
    }

    public void setYeglSurfaceView(YEGLSurfaceView yeglSurfaceView) {
        this.yeglSurfaceView = yeglSurfaceView;
    }

    /**
     * 设置准备接口回调
     *
     * @param wlOnPreparedListener
     */
    public void setWlOnPreparedListener(WlOnPreparedListener wlOnPreparedListener) {
        this.wlOnPreparedListener = wlOnPreparedListener;
    }

    /**
     * 创建一个AudioTrack对象，用来播放音频
     * native层回调
     *
     * @param sampleRateInHz
     * @param nbChannels
     */
    private AudioTrack createAudioTrack(int sampleRateInHz, int nbChannels) {
        Log.d(YEPlayer.class.getSimpleName(), "初始化播放器成功");
        int channelsConfig; // 通道数
        switch (nbChannels) {
            case 1: // 单声道
                channelsConfig = AudioFormat.CHANNEL_OUT_MONO;
                break;
            case 2: // 立体声
                channelsConfig = AudioFormat.CHANNEL_OUT_STEREO;
                break;
            default:
                channelsConfig = AudioFormat.CHANNEL_OUT_MONO;
                break;
        }
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelsConfig, AudioFormat.ENCODING_PCM_16BIT);
        return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelsConfig, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize, AudioTrack.MODE_STREAM);
    }


    /**
     * 跳转到指定的 播放 时间点
     *
     * @param sec
     */
    public void seekTo(int sec) {
        nativeSeek(sec);
    }

    public void setVolume(int leftVolume, int rightVolume) {

    }

//    public void setVolume(int volume) {
//        setVolume(volume, volume);
//    }

    /**
     * 设置音量
     *
     * @param percent
     */
    public void setVolume(int percent) {
        if (percent >= 0 && percent <= 100) {
            nativeVolume(percent);
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        nativePause();
    }

    /**
     * 继续播放
     */
    public void resume() {
        nativeResume();
    }

    /**
     * 静音
     * 左声道 右声道 立体声
     *
     * @param mute
     */
    public void setMute(int mute) {
        nativeMute(mute);
    }


    /**
     * 播放状态
     */
    enum PlayState {
        STARTED,
        PLAYING,
        PAUSED,
        STOPPED,
    }

    public interface WlOnPreparedListener {
        void onPrepared();
    }

    public interface IPlayerListener {
        void onLoad(boolean load);

        void onCurrentTime(int currentTime, int totalTime);

        void onError(int code, String msg);

        void onPause(boolean pause);

        void onDbValue(int db);

        void onComplete();

        String onNext();
    }


}
