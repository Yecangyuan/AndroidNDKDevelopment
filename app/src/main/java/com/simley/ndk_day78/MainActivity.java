package com.simley.ndk_day78;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.simley.ndk_day78.bandcard.BankCardRecognitionActivity;
import com.simley.ndk_day78.databinding.ActivityMainBinding;
import com.simley.ndk_day78.face.FaceDetection;
import com.simley.ndk_day78.face.FaceDetectionActivity;
import com.simley.ndk_day78.player.PlayerActivity;
import com.simley.ndk_day78.textrecognition.TextRecognitionActivity;
import com.tbruyelle.rxpermissions3.RxPermissions;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
            android.Manifest.permission.ACCESS_WIFI_STATE
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
        binding.btnTextRecognition.setOnClickListener(v -> startActivity(new Intent(this, TextRecognitionActivity.class)));
        binding.btnPlayInterface.setOnClickListener(v -> startActivity(new Intent(this, PlayerActivity.class)));
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