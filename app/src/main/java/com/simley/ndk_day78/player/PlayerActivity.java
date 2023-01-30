package com.simley.ndk_day78.player;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.simley.ndk_day78.databinding.ActivityPlayerBinding;
import com.simley.ndk_day78.player.audio.AudioPlayerActivity;
import com.simley.ndk_day78.player.video.VideoPlayerActivity;
import com.simley.ndk_day78.player.video.VideoPlayerActivity2;
import com.tbruyelle.rxpermissions3.RxPermissions;

public class PlayerActivity extends AppCompatActivity {

    private ActivityPlayerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.gotoAudioInterface.setOnClickListener(v -> startActivity(new Intent(this, AudioPlayerActivity.class)));
//        binding.gotoVideoInterface.setOnClickListener(v -> startActivity(new Intent(this, VideoPlayerActivity.class)));
        binding.gotoVideoInterface.setOnClickListener(v -> startActivity(new Intent(this, VideoPlayerActivity2.class)));
    }


}