package com.simley.ndk_day78.player;

import android.os.Bundle;
import android.os.Environment;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.databinding.ActivityPlayerBinding;
import com.tbruyelle.rxpermissions3.RxPermissions;

import java.io.File;

public class PlayerActivity extends AppCompatActivity {

    private Surface surface;
    private final Player player = new Player();
    private ActivityPlayerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        copyNecessaryFilesToSdcard();
        requestPermissionWithRx();
        handlePlayVideo();
        handlePlaySound();
        renderFrameToSurfaceView();
    }

    private void copyNecessaryFilesToSdcard() {
//        getAssets().list()
    }

    private void handlePlaySound() {
        binding.playSound.setOnClickListener(v -> {
            String mp3File = new File(Environment.getExternalStorageDirectory(), "/Music/琵琶语-林海.mp3").getAbsolutePath();
            new Thread(() -> player.playSound(mp3File)).start();
        });
    }

    private void requestPermissionWithRx() {
        new RxPermissions(this).request(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
        ).subscribe(granted -> {
            if (granted) { // Always true pre-M
//                println("权限获取成功");
            } else {
//                println("权限获取失败");
            }
        });
    }


    private void handlePlayVideo() {
        binding.play.setOnClickListener(v -> {
            String mp4File = new File(Environment.getExternalStorageDirectory(), "/Movies/kali_hacker_video.mp4").getAbsolutePath();
            new Thread(() -> player.play(mp4File, surface)).start();
        });
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