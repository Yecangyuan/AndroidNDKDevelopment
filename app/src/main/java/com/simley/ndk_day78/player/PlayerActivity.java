package com.simley.ndk_day78.player;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.simley.ndk_day78.databinding.ActivityPlayerBinding;
import com.simley.ndk_day78.player.audio.AudioPlayerActivity;
import com.simley.ndk_day78.player.video.VideoPlayerActivity;
import com.tbruyelle.rxpermissions3.RxPermissions;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        requestPermissionWithRx();
        binding.gotoAudioInterface.setOnClickListener(v -> startActivity(new Intent(this, AudioPlayerActivity.class)));
        binding.gotoVideoInterface.setOnClickListener(v -> startActivity(new Intent(this, VideoPlayerActivity.class)));
    }


    private void requestPermissionWithRx() {
        new RxPermissions(this).request(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                android.Manifest.permission.MOUNT_FORMAT_FILESYSTEMS
        ).subscribe(granted -> {
            if (granted) { // Always true pre-M
//                println("权限获取成功");
            } else {
//                println("权限获取失败");
            }
        });
    }


}