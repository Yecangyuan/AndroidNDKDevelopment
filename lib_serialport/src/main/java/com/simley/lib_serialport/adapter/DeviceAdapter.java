package com.simley.lib_serialport.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.simley.lib_serialport.R;
import com.simley.lib_serialport.bean.Device;
import com.simley.lib_serialport.bean.T;

import java.io.File;
import java.util.List;


/**
 * 同学们：这个是串口列表适配器
 * <p>
 * 每个列表项分三行展示：设备名 + 驱动名标签 / 设备文件路径 / 权限标签。
 * 权限标签直接反映「这个串口现在能不能打开」——已授予绿色、未授予灰色，
 * 三项都没有时收敛成单个「需 root 授权」标签（本模块靠 chmod777 提权后打开串口）。
 * 原实现把设备名、驱动、路径、三项权限全挤在一行纯文本里，一眼看不出重点。
 * <p>
 * 标签文案见 lib_serialport/src/main/res/values/strings.xml，不要在代码里硬编码。
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
            convertView = mInflater.inflate(R.layout.item_device, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Device device = devices.get(position);
        File file = device.getFile();
        boolean canRead = file.canRead();
        boolean canWrite = file.canWrite();
        boolean canExecute = file.canExecute();

        holder.name.setText(device.getName());
        holder.driver.setText(device.getRoot());
        holder.path.setText(file.getAbsolutePath());
        bindPermissions(holder, canRead, canWrite, canExecute);

        // 1|root@android:/dev # ls ttyS 【目前一共提供的串口文件如下】
        // ttyS0 ttyS1 ttyS2 ttyS3
        Log.d(T.TAG, String.format(
                "DeviceAdapter getView: %s [%s] (%s) 权限[读=%b 写=%b 执行=%b]",
                device.getName(), device.getRoot(), file.getAbsolutePath(),
                canRead, canWrite, canExecute));
        // Device{name='ttyS1', root='serial', file=/dev/ttyS1} 权限[读=true 写=true 执行=false]
        return convertView;
    }

    /**
     * 权限展示策略：
     * - 三项全部未授予 → 只显示一个灰色「需 root 授权」标签。实测多数设备的 /dev/ttyS* 是
     *   root-only（0600），而本模块正是靠 chmod777（su + chmod 777）提权后打开的，
     *   此时每行堆三个灰标签只会把重点淹没；
     * - 有任意一项已授予 → 三项都列出来，已授予绿色、未授予灰色，便于定位究竟缺哪一项。
     * <p>
     * 注意 ViewHolder 会被复用，标签的可见性必须每次都显式设置。
     */
    private static void bindPermissions(ViewHolder holder, boolean canRead, boolean canWrite, boolean canExecute) {
        if (!canRead && !canWrite && !canExecute) {
            holder.read.setText(R.string.serial_perm_need_root);
            applyChipStyle(holder.read, false);
            holder.write.setVisibility(View.GONE);
            holder.execute.setVisibility(View.GONE);
            return;
        }
        holder.write.setVisibility(View.VISIBLE);
        holder.execute.setVisibility(View.VISIBLE);
        setChip(holder.read, canRead, R.string.serial_perm_read, R.string.serial_perm_no_read);
        setChip(holder.write, canWrite, R.string.serial_perm_write, R.string.serial_perm_no_write);
        setChip(holder.execute, canExecute, R.string.serial_perm_exec, R.string.serial_perm_no_exec);
    }

    /** 按权限是否授予切换标签文案与配色 */
    private static void setChip(TextView chip, boolean granted, int grantedTextRes, int deniedTextRes) {
        chip.setText(granted ? grantedTextRes : deniedTextRes);
        applyChipStyle(chip, granted);
    }

    private static void applyChipStyle(TextView chip, boolean granted) {
        chip.setBackgroundResource(granted
                ? R.drawable.serial_bg_chip_ok
                : R.drawable.serial_bg_chip_no);
        chip.setTextColor(ContextCompat.getColor(chip.getContext(), granted
                ? R.color.serial_chip_ok_text
                : R.color.serial_chip_no_text));
    }

    private static class ViewHolder {
        final TextView name;
        final TextView driver;
        final TextView path;
        final TextView read;
        final TextView write;
        final TextView execute;

        ViewHolder(View root) {
            name = root.findViewById(R.id.tv_device);
            driver = root.findViewById(R.id.tv_driver);
            path = root.findViewById(R.id.tv_path);
            read = root.findViewById(R.id.tv_perm_read);
            write = root.findViewById(R.id.tv_perm_write);
            execute = root.findViewById(R.id.tv_perm_exec);
        }
    }
}
