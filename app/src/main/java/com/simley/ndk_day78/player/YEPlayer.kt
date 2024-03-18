package com.simley.ndk_day78.player

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.text.TextUtils
import android.util.Log
import android.view.Surface
import com.simley.ndk_day78.opengl.YEGLSurfaceView
import com.simley.ndk_day78.utils.ThreadPoolManager

/**
 * 音视频播放器
 */
class YEPlayer {

    private var mSource: String? = null // 数据源
    private var iPlayerListener: IPlayerListener? = null // 音视频播放回调接口

    // 回调函数，当数据准备好的时候native层会回调该接口的onPrepared()方法
    private var iOnPreparedListener: IOnPreparedListener? = null
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
        ThreadPoolManager.getInstance().execute { nativeStop() }
    }

    fun prepare() {
        if (TextUtils.isEmpty(mSource)) {
            return
        }
        ThreadPoolManager.getInstance().execute { nativePrepared(mSource) }
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
        ThreadPoolManager.getInstance().execute { nativeStart() }
    }

    fun setDataSource(dataSource: String?) {
        mSource = dataSource
    }

    fun onCallPrepared() {
        if (iOnPreparedListener != null) {
            iOnPreparedListener!!.onPrepared()
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
     * @param iOnPreparedListener
     */
    fun setWlOnPreparedListener(iOnPreparedListener: IOnPreparedListener?) {
        this.iOnPreparedListener = iOnPreparedListener
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
        // 通道数
        val channelsConfig: Int = when (nbChannels) {
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
        if (percent in 0..100) {
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


//    internal class TrackInfo() : Parcelable {
//        constructor(parcel: Parcel) : this() {
//        }
//
//        override fun describeContents(): Int {
//        }
//
//        override fun writeToParcel(p0: Parcel, p1: Int) {
//
//        }
//
//        companion object CREATOR : Parcelable.Creator<MediaPlayer.TrackInfo> {
//            override fun createFromParcel(parcel: Parcel): MediaPlayer.TrackInfo {
//                return TrackInfo(parcel)
//            }
//
//            override fun newArray(size: Int): Array<TrackInfo?> {
//                return arrayOfNulls(size)
//            }
//        }
//
//    }

    /**
     * 播放状态
     */
    internal enum class PlayState {
        STARTED, PLAYING, PAUSED, STOPPED
    }

    /**
     * 准备播放接口
     */
    interface IOnPreparedListener {
        fun onPrepared()
    }

    /**
     * 播放器接口
     */
    interface IPlayerListener {

        /**
         * 加载
         */
        fun onLoad(load: Boolean)

        /**
         * 回调当前时间
         * @param currentTime 当前播放时间
         * @param totalTime 总时长
         */
        fun onCurrentTime(currentTime: Int, totalTime: Int)

        /**
         * 错误
         */
        fun onError(code: Int, msg: String?)

        /**
         * 暂停
         */
        fun onPause(pause: Boolean)

        fun onDbValue(db: Int)

        /**
         * 播放完成
         */
        fun onComplete()

        /**
         * 播放下一个
         */
        fun onNext(): String?
    }

    companion object {
        const val MEDIA_ERROR_IO = -1004
        const val MEDIA_ERROR_MALFORMED = -1007
        const val MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200
        const val MEDIA_ERROR_SERVER_DIED = 100
        const val MEDIA_ERROR_TIMED_OUT = -110
        const val MEDIA_ERROR_UNKNOWN = 1
        const val MEDIA_ERROR_UNSUPPORTED = -1010
        const val MEDIA_INFO_AUDIO_NOT_PLAYING = 804
        const val MEDIA_INFO_BAD_INTERLEAVING = 800
        const val MEDIA_INFO_BUFFERING_END = 702
        const val MEDIA_INFO_BUFFERING_START = 701
        const val MEDIA_INFO_METADATA_UPDATE = 802
        const val MEDIA_INFO_NOT_SEEKABLE = 801
        const val MEDIA_INFO_STARTED_AS_NEXT = 2
        const val MEDIA_INFO_SUBTITLE_TIMED_OUT = 902
        const val MEDIA_INFO_UNKNOWN = 1
        const val MEDIA_INFO_UNSUPPORTED_SUBTITLE = 901
        const val MEDIA_INFO_VIDEO_NOT_PLAYING = 805
        const val MEDIA_INFO_VIDEO_RENDERING_START = 3
        const val MEDIA_INFO_VIDEO_TRACK_LAGGING = 700
        const val MEDIA_MIMETYPE_TEXT_SUBRIP = "application/x-subrip"
        const val PREPARE_DRM_STATUS_PREPARATION_ERROR = 3
        const val PREPARE_DRM_STATUS_PROVISIONING_NETWORK_ERROR = 1
        const val PREPARE_DRM_STATUS_PROVISIONING_SERVER_ERROR = 2
        const val PREPARE_DRM_STATUS_SUCCESS = 0
        const val SEEK_CLOSEST = 3
        const val SEEK_CLOSEST_SYNC = 2
        const val SEEK_NEXT_SYNC = 1
        const val SEEK_PREVIOUS_SYNC = 0
        const val VIDEO_SCALING_MODE_SCALE_TO_FIT = 1
        const val VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING = 2
    }
}