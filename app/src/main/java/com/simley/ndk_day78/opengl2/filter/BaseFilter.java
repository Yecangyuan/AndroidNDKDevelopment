package com.simley.ndk_day78.opengl2.filter;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

import android.content.Context;

import com.simley.ndk_day78.opengl2.utils.BufferHelper;
import com.simley.ndk_day78.opengl2.utils.ShaderHelper;
import com.simley.ndk_day78.opengl2.utils.TextResourceReader;

import java.nio.FloatBuffer;

// Filter的基类 -- 简单的OpenGL绘制即可
public class BaseFilter {

    private int mVertexSourceId; // 子类传递过来的顶点着色器代码ID
    private int mFragmentSourceId; // 子类传递过来的片元着色器代码ID

    protected FloatBuffer mVertexBuffer; // 顶点坐标数据缓冲区
    protected FloatBuffer mTextureBuffer; // 纹理坐标数据缓冲区

    protected int mProgramId; // 着色器程序

    protected int vPosition;  // 顶点着色器：顶点位置
    protected int vCoord; // 顶点着色器：纹理坐标
    protected int vMatrix; // 顶点着色器：变换矩阵

    protected int vTexture; // 片元着色器：采样器

    protected int mWidth; // 宽度
    protected int mHeight; // 高度

    public BaseFilter(Context context, int vertexSourceId, int fragmentSourceId) {
        this.mVertexSourceId = vertexSourceId; // 子类传递过来的顶点着色器代码ID
        this.mFragmentSourceId = fragmentSourceId; // 子类传递过来的片元着色器代码ID

        // 顶点相关 坐标系
        float[] VERTEX = {
                -1.0f, -1.0f,
                1.0f, -1.0f,
                -1.0f, 1.0f,
                1.0f, 1.0f,};
        mVertexBuffer = BufferHelper.getFloatBuffer(VERTEX); // 保存到 顶点坐标数据缓冲区

        // 纹理相关 坐标系
        float[] TEXTURE = {
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,};
        mTextureBuffer = BufferHelper.getFloatBuffer(TEXTURE); // 保存到 纹理坐标数据缓冲区

        init(context);
    }

    private void init(Context context) {
        String vertexSource = TextResourceReader.readTextFileFromResource(context, mVertexSourceId); // 顶点着色器代码字符串
        String fragmentSource = TextResourceReader.readTextFileFromResource(context, mFragmentSourceId); // 片元着色器代码字符串

        int vertexShaderId = ShaderHelper.compileVertexShader(vertexSource); // 编译顶点着色器代码字符串
        int fragmentShaderId = ShaderHelper.compileFragmentShader(fragmentSource); // 编译片元着色器代码字符串

        mProgramId = ShaderHelper.linkProgram(vertexShaderId, fragmentShaderId); // 链接 顶点着色器ID，片元着色器ID 最终输出着色器程序

        // 删除 顶点 片元 着色器ID
        glDeleteShader(vertexShaderId);
        glDeleteShader(fragmentShaderId);

        vPosition = glGetAttribLocation(mProgramId, "vPosition"); // 顶点着色器：的索引值
        vCoord = glGetAttribLocation(mProgramId, "vCoord"); // 顶点着色器：纹理坐标，采样器采样图片的坐标 的索引值
        vMatrix = glGetUniformLocation(mProgramId, "vMatrix"); // 顶点着色器：变换矩阵 的索引值

        // 片元着色器里面的如下：
        vTexture = glGetUniformLocation(mProgramId, "vTexture"); // 片元着色器：采样器
    }

    // 让子类去 更新 宽度 高度
    public void onReady(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    /**
     * 让子类去 绘制操作
     * @param textureId 画布 纹理ID
     */
    public int onDrawFrame(int textureId) {
        // 设置视窗大小
        glViewport(0, 0, mWidth, mHeight);
        glUseProgram(mProgramId); // 必须要使用着色器程序一次

        // TODO 画画，绘制 等工作

        // TODO 顶点坐标赋值
        mVertexBuffer.position(0);
        // 传值
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, mVertexBuffer);
        // 激活
        glEnableVertexAttribArray(vPosition);

        // TODO 纹理坐标赋值
        mTextureBuffer.position(0);
        // 传值
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, mTextureBuffer);
        // 激活
        glEnableVertexAttribArray(vCoord);

        // 只需要把OpenGL的纹理ID，渲染到屏幕上就可以了，不需要矩阵数据传递给顶点着色器了
        // 变换矩阵 把mtx矩阵数据 传递到 vMatrix
        // glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);

        // TODO 片元 vTexture
        glActiveTexture(GL_TEXTURE0); // 激活图层

        // 不需要关心摄像头 和 矩阵
        // 绑定图层，为什么不需要GL_TEXTURE_EXTERNAL_OES？答：目前拿到的textureId已经是纹理ID了，不是摄像头直接采集到的纹理ID
        glBindTexture(GL_TEXTURE_2D ,textureId);

        // 因为CameraFilter已经做过了，我就直接显示，我用OepnGL 2D GL_TEXTURE_2D 显示就行了
        // glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId); // 由于这种方式并不是通用的，所以先去除

        glUniform1i(vTexture, 0); // 传递采样器
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); // 通知 opengl 绘制

        return textureId; // 返回纹理ID，可以告诉下一个过滤器
    }

}
