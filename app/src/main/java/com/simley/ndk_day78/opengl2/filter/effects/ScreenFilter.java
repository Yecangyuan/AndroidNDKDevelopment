package com.simley.ndk_day78.opengl2.filter.effects;

import android.content.Context;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.opengl2.filter.BaseFilter;

// 显示到 GLSurfaceView 屏幕   === 着色器语言打交道
public class ScreenFilter extends BaseFilter {

    public ScreenFilter(Context context) {
        super(context, R.raw.base_vertex, R.raw.base_fragment); // base_vertex(没有矩阵) base_fragment(没有OES 是sampler2D)
    }
}
