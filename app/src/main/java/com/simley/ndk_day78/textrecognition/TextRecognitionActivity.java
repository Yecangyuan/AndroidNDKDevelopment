package com.simley.ndk_day78.textrecognition;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.databinding.ActivityMainBinding;
import com.simley.ndk_day78.databinding.ActivityTextRecognitionBinding;
import com.tbruyelle.rxpermissions3.RxPermissions;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TextRecognitionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private TextRecognition mTextRecognition;
    private CameraBridgeViewBase mCameraBridgeViewBase;
    private Mat mRgba;
    private Bitmap mTextBitmap;
    private ActivityTextRecognitionBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTextRecognitionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mTextRecognition = new TextRecognition();
        Button mBtnCapture = findViewById(R.id.btn_capture);
        ImageView image = findViewById(R.id.image);

        mTextBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ticket);
        mBtnCapture.setOnClickListener(v -> {
            Bitmap result = mTextRecognition.recognize(mTextBitmap);
//            image.setImageBitmap(result);
//            if (mRgba != null && !mRgba.empty()) {
//                Bitmap bitmap = mTextRecognition.recognize(mRgba);
//                if (bitmap != null) {
//                    Intent intent = new Intent(this, DisplayResultActivity.class);
//                    intent.setData(Uri.parse(saveBitmap(bitmap)));
//                    startActivity(intent);
//                }
//            }
        });
//        requestPermission();
    }

    public String saveBitmap(Bitmap mBitmap) {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath());   //FILE_DIR自定义
        if (!file.exists()) {
            file.mkdir();
        }
        File tmpf = new File(file, "bitmap.jpg");
        try {
            tmpf.createNewFile();
            FileOutputStream fOut;
            fOut = new FileOutputStream(tmpf);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String image_file_url = tmpf.getAbsolutePath();
        Log.i("image_file_url", image_file_url);
        return image_file_url;
    }


    private void requestPermission() {
        new RxPermissions(this).request(Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        ).subscribe(granted -> {
            if (granted) {
                activateOpenCVCameraView();
            }
        });
    }

    private void activateOpenCVCameraView() {
        // everything needed to start a camera preview
        mCameraBridgeViewBase = binding.javaCameraView;
//        mCameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mCameraBridgeViewBase.setCameraPermissionGranted();
        mCameraBridgeViewBase.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mCameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        mCameraBridgeViewBase.setCvCameraViewListener(this);
        // 屏幕上显示fps
        mCameraBridgeViewBase.enableFpsMeter();
        mCameraBridgeViewBase.enableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
//        mRgba = new Mat(width, height, CvType.CV_8UC4);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mCameraBridgeViewBase.enableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mCameraBridgeViewBase.disableView();
    }

    @Override
    public void onCameraViewStopped() {
//        if (mRgba != null) {
//            mRgba.release();
//        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        mRgba = inputFrame.rgba();
        return inputFrame.rgba();
    }
}
