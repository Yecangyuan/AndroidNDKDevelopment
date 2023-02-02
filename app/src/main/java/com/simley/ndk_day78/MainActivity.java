package com.simley.ndk_day78;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.simley.ndk_day78.bandcard.BankCardRecognitionActivity;
import com.simley.ndk_day78.databinding.ActivityMainBinding;
import com.simley.ndk_day78.face.FaceDetectionActivity;
import com.simley.ndk_day78.fmod.FmodActivity;
import com.simley.ndk_day78.idcard.IDCardRecognitionActivity;
import com.simley.ndk_day78.opengl2.MyGLSurfaceViewActivity;
import com.simley.ndk_day78.player.PlayerActivity;
import com.simley.ndk_day78.textrecognition.TextRecognitionActivity;
import com.tbruyelle.rxpermissions3.RxPermissions;

import io.reactivex.rxjava3.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
            android.Manifest.permission.MOUNT_FORMAT_FILESYSTEMS,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE,
            android.Manifest.permission.READ_PHONE_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        requestPermissionWithRx();
        initListener();
    }

    /**
     * 初始化监听器
     */
    private void initListener() {
        // 人脸识别
        binding.btnFaceDetection.setOnClickListener(v -> startActivity(new Intent(this, FaceDetectionActivity.class)));
        // 银行卡识别
        binding.btnBankCardOrganize.setOnClickListener(v -> startActivity(new Intent(this, BankCardRecognitionActivity.class)));
        // 身份证识别
        binding.btnIdcardReconition.setOnClickListener(v -> startActivity(new Intent(this, IDCardRecognitionActivity.class)));
        // 文本识别
        binding.btnTextRecognition.setOnClickListener(v -> startActivity(new Intent(this, TextRecognitionActivity.class)));
        // 音视频播放器
        binding.btnPlayInterface.setOnClickListener(v -> startActivity(new Intent(this, PlayerActivity.class)));
        // fmod
        binding.btnFmodInterface.setOnClickListener(v -> startActivity(new Intent(this, FmodActivity.class)));
        // opengl
        binding.btnOpenglInterface.setOnClickListener(v -> startActivity(new Intent(this, MyGLSurfaceViewActivity.class)));
    }

    /**
     * 请求所有相关权限
     */
    private void requestPermissionWithRx() {
        Disposable disposable = new RxPermissions(this)
                .request(PERMISSIONS).subscribe(granted -> {
                    if (granted) {
                        Log.d(MainActivity.class.getSimpleName(), "相关权限获取成功");
                    } else {
                        Log.d(MainActivity.class.getSimpleName(), "相关权限获取失败");
                    }
                });
        disposable.dispose();
    }


}