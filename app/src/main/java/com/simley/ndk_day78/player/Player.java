package com.simley.ndk_day78.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.view.Surface;

import java.util.List;

public class Player {

    private AudioTrack audioTrack;
    private String mSource;
    private Listener mListener;

    public native void play(String url, Surface surface);

    public native void playSound(String url);


    /**
     * 设置 数据源
     *
     * @param mSource 数据源
     */
    public void setSource(String mSource) {
        this.mSource = mSource;
    }

    public void setListener(Listener mListener) {
        this.mListener = mListener;
    }

    /**
     * 创建一个AudioTrack对象，用来播放音频
     * native层回调
     *
     * @param sampleRateInHz
     * @param nbChannels
     */
    private AudioTrack createAudioTrack(int sampleRateInHz, int nbChannels) {
        Log.d("player", "初始化播放器成功");
        int channelsConfig; // 通道数
        if (nbChannels == 1) { // 单声道
            channelsConfig = AudioFormat.CHANNEL_OUT_MONO;
        } else if (nbChannels == 2) { // 立体声
            channelsConfig = AudioFormat.CHANNEL_OUT_STEREO;
        } else {
            channelsConfig = AudioFormat.CHANNEL_OUT_MONO;
        }
        int minBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelsConfig, AudioFormat.ENCODING_PCM_16BIT);
        return new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelsConfig, AudioFormat.ENCODING_PCM_16BIT,
                minBufferSize, AudioTrack.MODE_STREAM);
    }


}
