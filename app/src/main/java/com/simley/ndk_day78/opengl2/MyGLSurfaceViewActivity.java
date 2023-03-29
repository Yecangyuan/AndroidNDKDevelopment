package com.simley.ndk_day78.opengl2;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraCharacteristics;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.opengl2.listener.MyGestureListener;

import org.opencv.core.Size;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class MyGLSurfaceViewActivity extends AppCompatActivity implements MyGestureListener.SimpleGestureListener {

    protected static final int SHADER_NUM = 34;
    protected static final int LUT_A_SHADER_INDEX = 19;
    protected static final int LUT_B_SHADER_INDEX = 20;
    protected static final int LUT_C_SHADER_INDEX = 21;
    protected static final int LUT_D_SHADER_INDEX = 22;
    protected static final int ASCII_SHADER_INDEX = 29;
    protected MyGLRenderer myGLRenderer;
    protected MyGLSurfaceView mGLSurfaceView;
    protected MyGestureListener mGestureDetector;
    //    protected int mCurrentShaderIndex = SHADER_NUM - 1;
    protected int mCurrentShaderIndex = 23;
    protected Size mRootViewSize, mScreenSize;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_glsurface_view);
        mGLSurfaceView = findViewById(R.id.myglsurfaceview);
        mGestureDetector = new MyGestureListener(this, this);
        myGLRenderer = new MyGLRenderer(mGLSurfaceView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(this, "左滑或右滑切换滤镜", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSwipe(MyGestureListener.SwipeDirection direction) {

    }

    public void updateTransformMatrix(String cameraId) {
//        if (Integer.valueOf(cameraId) == CameraCharacteristics.LENS_FACING_FRONT) {
//            mByteFlowRender.setTransformMatrix(90, 0);
//        } else {
//            mByteFlowRender.setTransformMatrix(90, 1);
//        }

    }

    public void updateGLSurfaceViewSize(Size previewSize) {
        Size fitSize = null;
//        fitSize = CameraUtil.getFitInScreenSize(previewSize.getWidth(), previewSize.getHeight(), getScreenSize().getWidth(), getScreenSize().getHeight());
//        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mGLSurfaceView
//                .getLayoutParams();
//        params.width = fitSize.getWidth();
//        params.height = fitSize.getHeight();
//        params.addRule(RelativeLayout.ALIGN_PARENT_TOP | RelativeLayout.CENTER_HORIZONTAL);

//        mGLSurfaceView.setLayoutParams(params);
    }

    public Size getScreenSize() {
        if (mScreenSize == null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            mScreenSize = new Size(displayMetrics.widthPixels, displayMetrics.heightPixels);
        }
        return mScreenSize;
    }

    public void loadRGBAImage(int resId, int index) {
        InputStream is = this.getResources().openRawResource(resId);
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(is);
            if (bitmap != null) {
                int bytes = bitmap.getByteCount();
                ByteBuffer buf = ByteBuffer.allocate(bytes);
                bitmap.copyPixelsToBuffer(buf);
                byte[] byteArray = buf.array();
//                mByteFlowRender.loadLutImage(index, IMAGE_FORMAT_RGBA, bitmap.getWidth(), bitmap.getHeight(), byteArray);
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}