package com.simley.ndk_day78.opengl2;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.simley.ndk_day78.MainActivity;
import com.simley.ndk_day78.R;
import com.simley.ndk_day78.widget.MyRecordButton;

public class TiktokVideoFastSlowActivity extends AppCompatActivity {
    // 布局 ---> MainActivity.java ---> MyGLSurfaceView ---> MyGlRenderer ---> MyMediaRecorder ---> MyEGL --->
    private MyGLSurfaceView mGLSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiktok_video_fast_slow);
        mGLSurfaceView = findViewById(R.id.glSurfaceView);

        // 圆形红色按钮的 按住拍/录制完成
        ((MyRecordButton) findViewById(R.id.btn_record)).setOnRecordListener(
                new MyRecordButton.OnRecordListener() {

                    @Override
                    public void onStartRecording() { // 开始录制
                        mGLSurfaceView.startRecording();
                    }

                    @Override
                    public void onStopRecording() { // 停止录制
                        mGLSurfaceView.stopRecording();
                        Toast.makeText(TiktokVideoFastSlowActivity.this, "录制完成！", Toast.LENGTH_SHORT).show();
                    }
                });

        // 极慢 慢 标准 快 级快
        ((RadioGroup) findViewById(R.id.group_record_speed)).setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    /**
                     * 选择录制模式
                     * @param group
                     * @param checkedId
                     */
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (checkedId) {
                            case R.id.rbtn_record_speed_extra_slow: // 极慢
                                mGLSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_EXTRA_SLOW);
                                break;
                            case R.id.rbtn_record_speed_slow:   // 慢
                                mGLSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_SLOW);
                                break;
                            case R.id.rbtn_record_speed_normal: // 正常 标准
                                mGLSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_NORMAL);
                                break;
                            case R.id.rbtn_record_speed_fast:   // 快
                                mGLSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_FAST);
                                break;
                            case R.id.rbtn_record_speed_extra_fast: // 极快
                                mGLSurfaceView.setSpeed(MyGLSurfaceView.Speed.MODE_EXTRA_FAST);
                                break;
                        }
                    }
                });

        ((CheckBox)findViewById(R.id.chk_bigeye)).setOnCheckedChangeListener((buttonView, isChecked) -> mGLSurfaceView.enableBigEye(isChecked));
    }
}