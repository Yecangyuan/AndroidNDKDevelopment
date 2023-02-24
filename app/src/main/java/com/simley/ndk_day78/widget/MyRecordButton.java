package com.simley.ndk_day78.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

// 自定义控件
public class MyRecordButton extends AppCompatTextView {

    private OnRecordListener mListener;

    public MyRecordButton(Context context) {
        this(context, null);
    }

    public MyRecordButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyRecordButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mListener == null) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 1. 可以执行动画效果
                setPressed(true);
                mListener.onStartRecording();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 2. 可以执行动画效果
                setPressed(false);
                mListener.onStopRecording();
                break;
        }
        return true;
    }

    public void setOnRecordListener(OnRecordListener mListener) {
        this.mListener = mListener;
    }

    public interface OnRecordListener {
        void onStartRecording();

        void onStopRecording();
    }
}
