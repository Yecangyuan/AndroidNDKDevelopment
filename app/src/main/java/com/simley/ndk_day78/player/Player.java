package com.simley.ndk_day78.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;
import android.view.Surface;

public class Player {

    private AudioTrack audioTrack;

    public native void play(String url, Surface surface);


    public native void playSound(String url);


    /**
     * native层回调
     *
     * @param sampleRateInHz
     * @param nbChannels
     */
    private void createAudioTrack(int sampleRateInHz, int nbChannels) {
        new Thread(() -> {
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
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelsConfig, AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize, AudioTrack.MODE_STREAM);
            audioTrack.play();
        }).start();

    }

    /**
     * native层回调
     * pcm 数据内容 pcm实际长度 喇叭就能播放
     */
    private void playSound(byte[] buffer, int length) {
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.write(buffer, 0, length);
        }
    }
}
