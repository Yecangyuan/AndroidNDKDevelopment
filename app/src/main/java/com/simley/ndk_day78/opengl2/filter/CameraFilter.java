package com.simley.ndk_day78.opengl2.filter;

import android.content.Context;
import android.opengl.GLES11Ext;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.opengl2.utils.TextureHelper;

import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glFramebufferTexture2D;
import static android.opengl.GLES20.glGenFramebuffers;
import static android.opengl.GLES20.glTexImage2D;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

public class CameraFilter extends BaseFilter {

    private int[] mFrameBuffers; // FBO帧缓冲区
    private int[] mFrameBufferTextures; // fbo的纹理ID（和直接显示不同），最终是要返回
    private float[] matrix; // 变换矩阵

    public CameraFilter(Context context) {
        super(context, R.raw.camera_vertex, R.raw.camera_fragment);
    }

    public void onReady(int width, int height) {
       super.onReady(width,height);

        // TODO 准备工作
        // TODO 第一步：创建 FBO （看不见的离屏的屏幕）
        mFrameBuffers = new int[1];
        // 参数1：int n, fbo 个数
        // 参数2：int[] framebuffers, 用来保存 fbo id 的数组
        // 参数3：int offset 从数组中第几个id来保存,从零下标开始
        glGenFramebuffers(mFrameBuffers.length, mFrameBuffers, 0); // 实例化创建帧缓冲区，FBO缓冲区

        // TODO 第二步：创建属于 fbo 纹理(第一节课是没有配置的，但是这个是FOB纹理，所以需要配置纹理)
        // 既然上面的 FBO（看不见的离屏的屏幕），下面的目的就是要把画面显示到FBO中
        mFrameBufferTextures = new int[1]; // 记录FBO纹理的ID
        TextureHelper.genTextures(mFrameBufferTextures); // 生成并配置纹理

        // TODO 第三步：上面的 FBO缓冲区 与 FBO纹理 还没有任何关系，现在要让他们绑定起来
        glBindTexture(GL_TEXTURE_2D, mFrameBufferTextures[0]);

        // 生产2D纹理图像
        /*
            int target,         要绑定的纹理目标
            int level,          level一般都是0
            int internalformat, 纹理图像内部处理的格式是什么，rgba
            int width,          宽
            int height,         高
            int border,         边界
            int format,         纹理图像格式是什么，rgba
            int type,           无符号字节的类型
            java.nio.Buffer pixels
         */
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);

        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]); // 发生关系
         /*
            int target,     fbo的纹理目标
            int attachment, 附属到哪里
            int textarget,  要绑定的纹理目标
            int texture,    纹理
            int level       level一般都是0
         */
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mFrameBufferTextures[0], 0);

        // TODO 第四步：解绑操作
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int onDrawFrame(int textureId) {
        // super.onDrawFrame(); // 1  不可以2

        // 不能调用super，因为父类做的事情，和子类是很大区别的
        // super.onDrawFrame(textureId);
        glViewport(0, 0, mWidth, mHeight);  // 设置视窗大小

        // TODO 渲染到 FBO离线缓存中

        // 绑定FBO缓存（否则会绘制到屏幕上） 我们最终的效果是 离屏渲染
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);

        glUseProgram(mProgramId); // 必须要使用着色器程序一次

        // 画画 绘制操作

        mVertexBuffer.position(0); // 顶点坐标赋值
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, mVertexBuffer); // 传值
        glEnableVertexAttribArray(vPosition); // 激活

        mTextureBuffer.position(0); // 纹理坐标赋值
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, mTextureBuffer); // 传值
        glEnableVertexAttribArray(vCoord); // 激活

        // TODO 变换矩阵，在我们CameraFilter这里就需要处理了，后面的BaseFilter就不需要了
        glUniformMatrix4fv(vMatrix, 1, false, matrix, 0);

        // 片元 vTexture
        glActiveTexture(GL_TEXTURE0); // 激活图层
        // glBindTexture(GL_TEXTURE_2D ,textureId); // 公用的那个  1
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId); // 摄像头打交道采样器：使用额外拓展的，不能使用公用的那个  2
        glUniform1i(vTexture, 0);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); // 通知 opengl 绘制

        // 解绑 fbo
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // FBO的纹理ID，返回了
        return mFrameBufferTextures[0];
    }

    // 接收外界传递进来的 变换矩阵
    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }
}
