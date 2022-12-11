package com.simley.ndk_day78.player.video;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.databinding.ActivityVideoPlayerBinding;
import com.simley.ndk_day78.player.YEPlayer;

import java.io.File;

public class VideoPlayerActivity extends AppCompatActivity {

    private YEPlayer yePlayer;
    private ActivityVideoPlayerBinding binding;
    private Surface surface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        yePlayer = new YEPlayer();

        binding.play.setOnClickListener(v -> {
            String mp4File = new File(Environment.getExternalStorageDirectory(), "/Movies/kali_hacker_video.mp4").getAbsolutePath();
            new Thread(() -> yePlayer.play(mp4File, surface)).start();
        });

        renderFrameToSurfaceView();
    }

    private void renderFrameToSurfaceView() {
        SurfaceHolder holder = binding.surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surface = holder.getSurface();
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
    }

}