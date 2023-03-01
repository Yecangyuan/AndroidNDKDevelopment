package com.simley.ndk_day78.face;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.databinding.ActivityFaceDetectionBinding;
import com.simley.ndk_day78.utils.FileUtil;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class FaceDetectionActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int REQUEST_CODE_ADDRESS = 101;
    private FaceDetection mFaceDetection;
    private File mFaceCascadeFile, mEyeCascadeFile;
    private int cameraId;

    private Mat mIntermediateMat;
    private CameraBridgeViewBase cameraView;

    private ActivityFaceDetectionBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        binding = ActivityFaceDetectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        requestPermission();

        binding.sampleText.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        cameraId = CameraBridgeViewBase.CAMERA_ID_FRONT;
        binding.sampleText.setCvCameraViewListener(this);

        mFaceDetection = new FaceDetection();
        // 加在Haar级联分类器
//        loadHaarCascade();
        // 加载DNN模型
        loadDNNModel();
        loadDNNRecognitionModel();

        // 开始训练样本------->DNN
        new Thread(() -> {
            mFaceDetection.trainingDNNPattern();
        }).start();

        // 开始训练样本
//        mFaceDetection.trainingPattern();
        // 加载训练样本
//        mFaceDetection.loadPattern("/storage/emulated/0/face_simley_pattern.xml");
//        File file = new File(Environment.getExternalStorageDirectory(), "1.png");
//        Log.e("TAG", file.getAbsolutePath());
    }

    /**
     * 加载DNN 人脸识别模型
     */
    private void loadDNNRecognitionModel() {
        String faceModel = FileUtil.copyRawFileToSDCard(this, "dnn", "openface_nn4_small2_v1_t7", R.raw.openface_nn4_small2_v1_t7);
        mFaceDetection.loadDNNFaceRecognition(faceModel);
    }

    /**
     * 加载DNN 人脸检测模型
     */
    private void loadDNNModel() {
        String modelDesc = FileUtil.copyRawFileToSDCard(this, "dnn", "opencv_face_detector.pbtxt", R.raw.opencv_face_detector);
        String modelBin = FileUtil.copyRawFileToSDCard(this, "dnn", "opencv_face_detector_uint8.pb", R.raw.opencv_face_detector_uint8);
        mFaceDetection.loadDNNFaceDetector(modelBin, modelDesc);
    }

    /**
     * 加载Haar级联分类器
     */
    private void loadHaarCascade() {
        // 加载级联分类器 CascadeClassifier
        String eyeCascadeFile = FileUtil.copyRawFileToSDCard(this, "cascade", "haarcascade_eye_tree_eyeglasses.xml", R.raw.haarcascade_eye_tree_eyeglasses);
        String faceCascadeFile = FileUtil.copyRawFileToSDCard(this, "cascade", "haarcascade_frontalface_alt.xml", R.raw.haarcascade_frontalface_alt);
        mFaceDetection.loadFaceCascade(faceCascadeFile);
        mFaceDetection.loadEyeCascade(eyeCascadeFile);
    }


    @Override
    protected void onResume() {
        super.onResume();
        binding.sampleText.enableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.sampleText.disableView();
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


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_ADDRESS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted 授予权限
                //处理授权之后逻辑
                activateOpenCVCameraView();
            } else {
                // Permission Denied 权限被拒绝
                Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void activateOpenCVCameraView() {
        // everything needed to start a camera preview
        cameraView = binding.sampleText;
        cameraView.setCameraPermissionGranted();
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_ANY);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
        // 屏幕上显示fps
        cameraView.enableFpsMeter();
        cameraView.enableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mIntermediateMat.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();

//        Display display = ((WindowManager)getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

//        if(display.getRotation() == Surface.ROTATION_0) {
//            parameters.setPreviewSize(height, width);
//            mCamera.setDisplayOrientation(90);
//
//        }
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


//        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
//            switch (cameraId) {
//                case CameraBridgeViewBase.CAMERA_ID_FRONT:
//                    Core.rotate(rgba, rgba, Core.ROTATE_90_COUNTERCLOCKWISE);
//                    Core.flip(rgba, rgba, 1);
//                    break;
//                case CameraBridgeViewBase.CAMERA_ID_BACK:
//                    Core.rotate(rgba, rgba, Core.ROTATE_90_CLOCKWISE);
//                    break;
//            }
//        }
        mFaceDetection.faceDetectionDNN(rgba.nativeObj);
//        mFaceDetection.faceDetection(rgba);
        Size size = new Size(cameraView.getWidth(), cameraView.getHeight());
        Imgproc.resize(rgba, rgba, size);
        return rgba;
    }


    /**
     * 切换摄像头
     *
     * @return 切换摄像头是否成功
     */
    public boolean switchCamera() {
        // 摄像头总数
        int numberOfCameras = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {
            numberOfCameras = Camera.getNumberOfCameras();
        }
        // 2个及以上摄像头
        // 设备没有摄像头
        //            int index = ++mCameraSwitchCount % numberOfCameras;
        //            disableView();
        //            setCameraIndex(index);
        //            enableView();
        return 1 < numberOfCameras;
    }
}