package com.simley.ndk_day78.player.audio.ui.model

import java.io.Serializable

/**
 * Created by AchillesL on 2016/11/15.
 */
data class MusicData(/*音乐资源id*/
    val musicRes: Int, /*专辑图片id*/
    val musicPicRes: Int, /*音乐名称*/
    val musicName: String, /*作者*/
    val musicAuthor: String
) : Serializable