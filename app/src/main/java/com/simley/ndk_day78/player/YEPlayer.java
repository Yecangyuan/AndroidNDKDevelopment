package com.simley.ndk_day78.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;

import com.simley.ndk_day78.player.audio.listener.IPlayerListener;
import com.simley.ndk_day78.player.audio.listener.WlOnPreparedListener;
import com.simley.ndk_day78.player.audio.listener.YEPlayListener;

public class YEPlayer {

    private AudioTrack audioTrack;
    private String mSource;
    private YEPlayListener mYEPlayListener;
    private IPlayerListener iPlayerListener;
    private WlOnPreparedListener wlOnPreparedListener;

    public native void play(String url, Surface surface);

    public native void playSound(String url);

    private native void n_prepared(String source);

    private native void n_start();

    private native void n_seek(int secds);

    private native void n_resume();

    private native void n_pause();

    private native void n_mute(int mute);

    private native void n_volume(int percent);

    private native void n_speed(float speed);

    private native void n_pitch(float pitch);

    private native void n_stop();

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

    public void stop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                n_stop();
            }
        }).start();
    }

    public void prepare() {
        if (TextUtils.isEmpty(mSource)) {
            return;
        }
        new Thread(() -> n_prepared(mSource)).start();
    }

    public void start() {
        if (TextUtils.isEmpty(mSource)) {
            return;
        }
        new Thread(this::n_start).start();
    }

    public void setSource(String mSource) {
        this.mSource = mSource;
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
            iPlayerListener.onCurrentTime(currentTime, totalTime);
        }
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
    public void seek(int secds) {
        n_seek(secds);
    }

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

}
