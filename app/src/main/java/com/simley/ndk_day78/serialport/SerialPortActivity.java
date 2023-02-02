package com.simley.ndk_day78.serialport;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.serialport.handler.SerialPortManager;
import com.simley.ndk_day78.serialport.listener.OnOpenSerialPortListener;

import java.io.File;

public class SerialPortActivity extends AppCompatActivity implements OnOpenSerialPortListener {

    public static final String DEVICE = "device"; // 接收串口文件的标识
    public static final String BOTELV = "botelv"; // 接收波特率的标识
    int botelv = -1; // 波特率

    private SerialPortManager mSerialPortManager; // 打开串口，关闭串口，发生串口数据 需用的关联类


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial_port);
    }


    /**
     * 打开串口 调用 Toast的工具函数
     *
     * @param content content
     */
    private void showToast(String content) {
        if (null == mToast) {
            mToast = Toast.makeText(getApplicationContext(), null, Toast.LENGTH_SHORT);
        }
        mToast.setText(content);
        mToast.show();
    }

    @Override
    public void onSuccess(File device) {

    }

    @Override
    public void onFail(File device, Status status) {

    }
}