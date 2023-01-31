package com.simley.ndk_day78.fmod;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.simley.ndk_day78.R;

import org.fmod.FMOD;

public class FmodActivity extends AppCompatActivity {

    private final String path = "file:///android_asset/derry.mp3";

    private static final int MODE_NORMAL = 0; // 正常
    private static final int MODE_LUOLI = 1; //
    private static final int MODE_DASHU = 2; //
    private static final int MODE_JINGSONG = 3; //
    private static final int MODE_GAOGUAI = 4; //
    private static final int MODE_KONGLING = 5; //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fmod);
        // 初始化fmod
        FMOD.init(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 销毁fmod
        FMOD.close();
    }

    public void onFix(View view) {
        switch (view.getId()) {
            case R.id.btn_normal:
                voiceChange(MODE_NORMAL, path); // 真实开发中，必须子线程  JNI线程（很多坑）
                break;
            case R.id.btn_luoli:
                voiceChange(MODE_LUOLI, path);
                break;
            case R.id.btn_dashu:
                voiceChange(MODE_DASHU, path);
                break;
            case R.id.btn_jingsong:
                voiceChange(MODE_JINGSONG, path);
                break;
            case R.id.btn_gaoguai:
                voiceChange(MODE_GAOGUAI, path);
                break;
            case R.id.btn_kongling:
                voiceChange(MODE_KONGLING, path);
                break;
        }
    }

    // 给C++调用的函数
    // JNI 调用 Java函数的时候，忽略掉 私有、公开 等
    private void playerEnd(String msg) {
        Toast.makeText(this, "" + msg, Toast.LENGTH_SHORT).show();
    }

    private void voiceChange(int modeNormal, String path) {
        new Thread(() -> voiceChangeNative(modeNormal, path)).start();
    }

    private native void voiceChangeNative(int modeNormal, String path);
}