package com.simley.ndk_day78.player.video

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import com.simley.lib_base.BaseActivity
import com.simley.ndk_day78.databinding.ActivityVideoPlayerBinding
import com.simley.ndk_day78.player.YEPlayer
import com.simley.ndk_day78.utils.ThreadPool
import java.io.File

class VideoPlayerActivity : BaseActivity<ActivityVideoPlayerBinding>(), View.OnClickListener {

    private val yePlayer: YEPlayer = YEPlayer()

    private var surface: Surface? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.play.setOnClickListener(this)
        renderFrameToSurfaceView()
    }


    private fun renderFrameToSurfaceView() {
        val holder = binding.surfaceView.holder
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surface = holder.surface
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {}
        })
    }

    override fun onClick(p0: View?) {
        val mp4File = File(
            Environment.getExternalStorageDirectory(),
            "/Movies/kali_hacker_video.mp4"
        ).absolutePath
        ThreadPool.getInstance().execute { yePlayer.play(mp4File, surface) }
    }

    override fun getViewBinding(layoutInflater: LayoutInflater): ActivityVideoPlayerBinding =
        ActivityVideoPlayerBinding.inflate(layoutInflater, null, false)

}