package com.simley.ndk_day78.player

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import com.simley.ndk_day78.opengl.YEGLSurfaceView
import com.simley.ndk_day78.utils.ThreadPool

/**
 * 音视频播放器
 */
class YEPlayer {
    private var mSource: String? = null // 数据源
    private var iPlayerListener: IPlayerListener? = null // 音视频播放回调接口

    // 回调函数，当数据准备好的时候native层会回调该接口的onPrepared()方法
    private var wlOnPreparedListener: WlOnPreparedListener? = null
    private var yeglSurfaceView: YEGLSurfaceView? = null
    private val playState = PlayState.STARTED
    var duration = 0 // 播放时长
        private set

    /**
     * 开始播放
     *
     * @param url
     * @param surface
     */
    external fun play(url: String?, surface: Surface?)

    /**
     * 获取视频宽度
     *
     * @return
     */
    private external fun getVideoWidth(): Int

    /**
     * 获取视频高度
     *
     * @return
     */
    private external fun getVideoHeight(): Int
    //    public native void playSound(String url);
    /**
     * 准备播放
     *
     * @param source
     */
    private external fun nativePrepared(source: String?)

    /**
     * 开始播放
     */
    private external fun nativeStart()

    /**
     * 释放资源
     */
    private external fun nativeRelease()

    /**
     * 跳转到 指定播放位置
     *
     * @param sec
     */
    private external fun nativeSeek(sec: Int)

    /**
     * 继续播放
     */
    private external fun nativeResume()

    /**
     * 暂停播放
     */
    private external fun nativePause()

    /**
     * 设置静音
     *
     * @param mute
     */
    private external fun nativeMute(mute: Int)

    /**
     * 设置 循环播放
     *
     * @param looping
     */
    external fun nativeSetLooping(looping: Boolean)

    /**
     * 设置音量
     *
     * @param percent
     */
    private external fun nativeVolume(percent: Int)

    /**
     * 设置播放速度
     *
     * @param speed
     */
    private external fun nativeSpeed(speed: Float)

    /**
     * 设置音调
     *
     * @param pitch
     */
    private external fun nativePitch(pitch: Float)

    /**
     * 停止播放
     */
    private external fun nativeStop()

    /**
     * 是否循环播放
     *
     * @return
     */
    private external fun nativeIsLooping(): Boolean

    /**
     * 是否正在播放
     *
     * @return
     */
    val isPlaying: Boolean get() = playState == PlayState.PLAYING

    /**
     * 音频变速
     *
     * @param speed
     */
    fun setSpeed(speed: Float) {
        nativeSpeed(speed)
    }

    /**
     * 设置 音高
     *
     * @param pitch
     */
    fun setPitch(pitch: Float) {
        nativePitch(pitch)
    }
    /**
     * 是否循环播放中
     *
     * @return
     */
    /**
     * 设置循环播放
     *
     * @param looping
     */
    var isLooping: Boolean
        get() = nativeIsLooping()
        set(looping) {
            nativeSetLooping(looping)
        }

    /**
     * 停止播放
     */
    fun stop() {
        MediaPlayer().start()
        ThreadPool.getInstance().execute { nativeStop() }
    }

    fun prepare() {
        if (TextUtils.isEmpty(mSource)) {
            return
        }
        ThreadPool.getInstance().execute { nativePrepared(mSource) }
    }

    fun onCallLoad(load: Boolean) {}
    fun onCallRenderYUV(width: Int, height: Int, y: ByteArray?, u: ByteArray?, v: ByteArray?) {
        if (yeglSurfaceView != null) {
            yeglSurfaceView!!.setYUVData(width, height, y, u, v)
        }
    }

    /**
     * 开始播放
     */
    fun start() {
        require(!TextUtils.isEmpty(mSource)) { "datasource is null, please set the datasource by calling setDataSource method." }
        ThreadPool.getInstance().execute { nativeStart() }
    }

    fun setDataSource(dataSource: String?) {
        mSource = dataSource
    }

    fun onCallPrepared() {
        if (wlOnPreparedListener != null) {
            wlOnPreparedListener!!.onPrepared()
        }
    }

    fun setPlayerListener(iPlayerListener: IPlayerListener?) {
        this.iPlayerListener = iPlayerListener
    }

    fun onCallTimeInfo(currentTime: Int, totalTime: Int) {
        if (iPlayerListener != null) {
            duration = totalTime
            iPlayerListener!!.onCurrentTime(currentTime, totalTime)
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        nativeRelease()
    }

    fun onError(errorCode: Int, errorMsg: String?) {
        if (iPlayerListener != null) {
            iPlayerListener!!.onError(errorCode, errorMsg)
        }
    }

    fun setYeglSurfaceView(yeglSurfaceView: YEGLSurfaceView?) {
        this.yeglSurfaceView = yeglSurfaceView
    }

    /**
     * 设置准备接口回调
     *
     * @param wlOnPreparedListener
     */
    fun setWlOnPreparedListener(wlOnPreparedListener: WlOnPreparedListener?) {
        this.wlOnPreparedListener = wlOnPreparedListener
    }

    /**
     * 创建一个AudioTrack对象，用来播放音频
     * native层回调
     *
     * @param sampleRateInHz
     * @param nbChannels
     */
    private fun createAudioTrack(sampleRateInHz: Int, nbChannels: Int): AudioTrack {
        Log.d(YEPlayer::class.java.simpleName, "初始化播放器成功")
        val channelsConfig: Int // 通道数
        channelsConfig = when (nbChannels) {
            1 -> AudioFormat.CHANNEL_OUT_MONO
            2 -> AudioFormat.CHANNEL_OUT_STEREO
            else -> AudioFormat.CHANNEL_OUT_MONO
        }
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRateInHz,
            channelsConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )
        return AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRateInHz,
            channelsConfig,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize,
            AudioTrack.MODE_STREAM
        )
    }

    /**
     * 跳转到指定的 播放 时间点
     *
     * @param sec
     */
    fun seekTo(sec: Int) {
        nativeSeek(sec)
    }

    fun setVolume(leftVolume: Int, rightVolume: Int) {}
    //    public void setVolume(int volume) {
    //        setVolume(volume, volume);
    //    }
    /**
     * 设置音量
     *
     * @param percent
     */
    fun setVolume(percent: Int) {
        if (percent >= 0 && percent <= 100) {
            nativeVolume(percent)
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        nativePause()
    }

    /**
     * 继续播放
     */
    fun resume() {
        nativeResume()
    }

    /**
     * 静音
     * 左声道 右声道 立体声
     *
     * @param mute
     */
    fun setMute(mute: Int) {
        nativeMute(mute)
    }

    /**
     * 播放状态
     */
    internal enum class PlayState {
        STARTED, PLAYING, PAUSED, STOPPED
    }

    interface WlOnPreparedListener {
        fun onPrepared()
    }

    interface IPlayerListener {
        fun onLoad(load: Boolean)
        fun onCurrentTime(currentTime: Int, totalTime: Int)
        fun onError(code: Int, msg: String?)
        fun onPause(pause: Boolean)
        fun onDbValue(db: Int)
        fun onComplete()
        fun onNext(): String?
    }
}