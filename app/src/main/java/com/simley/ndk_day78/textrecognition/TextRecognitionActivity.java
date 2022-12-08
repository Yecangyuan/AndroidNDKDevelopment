package com.simley.ndk_day78.textrecognition;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.simley.ndk_day78.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class TextRecognitionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int REQUEST_CODE_ADDRESS = 101;
    private TextRecognition mTextRecognition;
    private CameraBridgeViewBase mCameraBridgeViewBase;
    private Mat mRgba;
    private Bitmap mTextBitmap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_recognition);

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                activateOpenCVCameraView();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_ADDRESS);
            }
        }
    }

    private void activateOpenCVCameraView() {
        // everything needed to start a camera preview
        mCameraBridgeViewBase = findViewById(R.id.javaCameraView);
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
