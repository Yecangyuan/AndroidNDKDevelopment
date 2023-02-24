package com.simley.lib_serialport.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.simley.lib_serialport.R;
import com.simley.lib_serialport.bean.Device;
import com.simley.lib_serialport.bean.T;

import java.io.File;
import java.util.List;


/**
 * 同学们：这个是串口列表适配器
 */
public class DeviceAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final List<Device> devices;

    public DeviceAdapter(Context context, List<Device> devices) {
        this.mInflater = LayoutInflater.from(context);
        this.devices = devices;
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Device getItem(int position) {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (null == convertView) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.item_device, null);
            holder.device = convertView.findViewById(R.id.tv_device);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String deviceName = devices.get(position).getName();
        String driverName = devices.get(position).getRoot();
        File file = devices.get(position).getFile();
        boolean canRead = file.canRead();
        boolean canWrite = file.canWrite();
        boolean canExecute = file.canExecute();
        String path = file.getAbsolutePath();

        String permission = "\t权限[" +
                (canRead ? " 可读 " : " 不可读 ") +
                (canWrite ? " 可写 " : " 不可写 ") +
                (canExecute ? " 可执行 " : " 不可执行 ") +
                "]";

        // 1|root@android:/dev # ls ttyS 【目前一共提供的串口文件如下】
        // ttyS0 ttyS1 ttyS2 ttyS3
        String result = String.format("%s [%s] (%s)  %s", deviceName, driverName, path, permission);
        Log.d(T.TAG, "DeviceAdapter getView: result:" + result);
        // result:ttyS3 [serial] (/dev/ttyS3)  	权限[ 可读  可写  不可执行 ]
        // result:ttyS2 [serial] (/dev/ttyS2)  	权限[ 可读  可写  不可执行 ]
        // result:ttyS1 [serial] (/dev/ttyS1)  	权限[ 可读  可写  不可执行 ]
        // result:ttyS0 [serial] (/dev/ttyS0)  	权限[ 可读  可写  不可执行 ]

        holder.device.setText(result);
        return convertView;
    }

    private static class ViewHolder {
        TextView device;
    }
}
