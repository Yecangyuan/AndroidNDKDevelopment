package com.simley.ndk_day78.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.simley.ndk_day78.opengl.YEGLSurfaceView;
import com.simley.ndk_day78.player.audio.listener.IPlayerListener;
import com.simley.ndk_day78.player.audio.listener.WlOnPreparedListener;
import com.simley.ndk_day78.player.audio.listener.YEPlayListener;
import com.simley.ndk_day78.utils.ThreadPool;

/**
 * 音视频播放器
 */
public class YEPlayer {

    private AudioTrack audioTrack;
    private String mSource;
    private YEPlayListener mYEPlayListener;
    private IPlayerListener iPlayerListener;
    private WlOnPreparedListener wlOnPreparedListener;

    private YEGLSurfaceView yeglSurfaceView;

    private final PlayState playState = PlayState.STARTED;
    private int duration;

    public native void play(String url, Surface surface);

    public native void playSound(String url);

    private native void n_prepared(String source);

    private native void n_start();

    private native void n_seek(int sec);

    private native void n_resume();

    private native void n_pause();

    private native void n_mute(int mute);

    public native void n_set_looping(boolean looping);

    private native void n_volume(int percent);

    private native void n_speed(float speed);

    private native void n_pitch(float pitch);

    private native void n_stop();

    private native boolean n_is_looping();

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
        n_speed(speed);
    }

    /**
     * 设置 音高
     *
     * @param pitch
     */
    public void setPitch(float pitch) {
        n_pitch(pitch);
    }

    /**
     * 设置循环播放
     *
     * @param looping
     */
    public void setLooping(boolean looping) {
        n_set_looping(looping);
    }

    /**
     * 是否循环播放中
     *
     * @return
     */
    public boolean isLooping() {
        return n_is_looping();
    }

    /**
     * 停止播放
     */
    public void stop() {
        new MediaPlayer().start();
        ThreadPool.getInstance().execute(this::n_stop);
    }

    public void prepare() {
        if (TextUtils.isEmpty(mSource)) {
            return;
        }
        ThreadPool.getInstance().execute(() -> n_prepared(mSource));
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
        ThreadPool.getInstance().execute(this::n_start);
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
     * @param secds
     */
    public void seekTo(int secds) {
        n_seek(secds);
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
            n_volume(percent);
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        n_pause();
    }

    /**
     * 继续播放
     */
    public void resume() {
        n_resume();
    }

    /**
     * 静音
     * 左声道 右声道 立体声
     *
     * @param mute
     */
    public void setMute(int mute) {
        n_mute(mute);
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

}
