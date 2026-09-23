package com.simley.ndk_day78.serialport;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.simley.lib_serialport.adapter.DeviceAdapter;
import com.simley.lib_serialport.bean.Device;
import com.simley.lib_serialport.bean.T;
import com.simley.lib_serialport.utils.SerialPortSearcher;
import com.simley.ndk_day78.R;

import java.util.List;

// 这个Activity要展示，所有可能打开的串口设备列表
public class SelectSerialPortActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private DeviceAdapter mDeviceAdapter; // 显示串口列表的适配器
    private EditText mBaudRateInput;      // 波特率输入框
    private TextInputLayout mBaudRateLayout; // 波特率输入框外壳，用于内联报错

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_serial_port);

        mBaudRateInput = findViewById(R.id.btlv);
        mBaudRateLayout = findViewById(R.id.til_baud_rate);
        mBaudRateInput.addTextChangedListener(mClearErrorWatcher);

        ListView listView = findViewById(R.id.lv_devices); // 同学们：列出所有的串口
        List<Device> devices = new SerialPortSearcher().getDevices(); // 获取所有的串口

        TextView countView = findViewById(R.id.tv_device_count);
        if (countView != null) {
            countView.setText("(" + devices.size() + ")");
        }

        if (listView != null) {
            listView.setEmptyView(findViewById(R.id.tv_empty)); // 若没有串口，就显示tv_empty控件
            mDeviceAdapter = new DeviceAdapter(getApplicationContext(), devices); // 实例化适配器
            listView.setAdapter(mDeviceAdapter); // 把适配器设置到ListView中去
            listView.setOnItemClickListener(this); // 设置ListView item的监听
        }
    }

    /** 用户一开始输入就清掉上一次的报错，避免红框一直挂着 */
    private final TextWatcher mClearErrorWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mBaudRateLayout != null && mBaudRateLayout.getError() != null) {
                mBaudRateLayout.setError(null);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Device device = mDeviceAdapter.getItem(position); // 获取具体的
        Log.d(T.TAG, "SelectSerialPortActivity onItemClick: " + device.toString());
        // Device{name='ttyS3', root='serial', file=/dev/ttyS3} 这个串口文件 不可用
        // Device{name='ttyS2', root='serial', file=/dev/ttyS2} 这个串口文件 不可用
        // Device{name='ttyS1', root='serial', file=/dev/ttyS1} 这个串口文件 是可以用的，有时候要重启电脑才能正常使用，毕竟是虚拟串口的
        // Device{name='ttyS0', root='serial', file=/dev/ttyS0} 这个串口文件 表面上是可以用的，实际上是一个坑货

        String baudRate = mBaudRateInput.getText() == null
                ? "" : mBaudRateInput.getText().toString().trim();
        if (baudRate.isEmpty()) { // 用户没填波特率
            // 内联报错比 Toast 更靠近输入框，用户不用把视线移到屏幕底部
            mBaudRateLayout.setError(getString(R.string.serial_baud_rate_required));
            mBaudRateInput.requestFocus();
            return;
        }

        Intent intent = new Intent(this, SerialPortActivity.class); // SerialPortActivity串口读写操作
        intent.putExtra(SerialPortActivity.DEVICE, device); // 传递串口设备对象
        intent.putExtra(SerialPortActivity.BAUD_RATE, baudRate); // 传递波特率
        startActivity(intent); // 启动SerialPortActivity
    }
}
