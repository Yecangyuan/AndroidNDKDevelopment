package com.simley.ndk_day78.opengl2.utils;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameteri;

/**
 * FBO(Frame Buffer Object)
 * 离屏缓冲
 * FBO纹理 配置 工具类
 */
public class TextureHelper {

    /**
     * 生成并配置纹理
     *
     * @param textures 传递纹理数组进来{有很多很多的纹理，用循环遍历中配置纹理}
     */
    public static void genTextures(int[] textures) {
        glGenTextures(textures.length, textures, 0); // offset:从第几个位置开始
        for (int texture : textures) {  // 如果你要生成多个纹理，我也支持
            // TODO 1，绑定纹理
            // 面向过程
            // 绑定后的操作就是在该纹理上进行的
            // int target, 纹理目标
            // int texture 纹理id
            glBindTexture(GL_TEXTURE_2D, texture);

            // TODO 2，配置纹理 FBO要求配置
            // 2.1 设置过滤参数，当纹理被使用到一个比它大或小的形状上时，opengl该如何处理
            // 配合使用：min与最近点，mag 与 线性采样
            // int target,   纹理目标
            // int pname,    参数名
            // int param     参数值
            // GL_TEXTURE_MAG_FILTER 放大过滤（线性过滤）
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            // GL_TEXTURE_MIN_FILTER 缩小过滤（最近点过滤）
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

            // 不是按照你设置的来，如果你不设置，就无法达到FBO纹理的要求
            // TODO 2.2 设置纹理环绕方向，纹理坐标0-1，如果超出范围的坐标，告诉opengl根据配置的参数进行处理
            // GL_TEXTURE_WRAP_S GL_TEXTURE_WRAP_T分别为纹理的x, y 方向
            // GL_REPEAT 重复拉伸（平铺）
            // GL_CLAMP_TO_EDGE 截取拉伸（边缘拉伸）

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE); // 纹理中的S--X轴
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE); // 纹理中的T--Y轴

            // TODO 3，解绑纹理（传0 表示与当前纹理解绑）
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }


}
