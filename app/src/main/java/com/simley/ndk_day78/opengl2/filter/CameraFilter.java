package com.simley.ndk_day78.opengl2.filter;

import android.content.Context;
import android.opengl.GLES11Ext;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.opengl2.filter.BaseFrameFilter;
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

public class CameraFilter extends BaseFrameFilter {

    private float[] matrix; // 变换矩阵

    public CameraFilter(Context context) {
        super(context, R.raw.camera_vertex, R.raw.camera_fragment);
    }

//    public void onReady(int width, int height) {
//       super.onReady(width,height);
//
//        // 准备工作
//        // 第一步：创建 FBO （看不见的离屏的屏幕）
//        mFrameBuffers = new int[1];
//        // 参数1：int n, fbo 个数
//        // 参数2：int[] framebuffers, 用来保存 fbo id 的数组
//        // 参数3：int offset 从数组中第几个id来保存,从零下标开始
//        glGenFramebuffers(mFrameBuffers.length, mFrameBuffers, 0); // 实例化创建帧缓冲区，FBO缓冲区
//
//        // 第二步：创建属于 fbo 纹理(第一节课是没有配置的，但是这个是FOB纹理，所以需要配置纹理)
//        // 既然上面的 FBO（看不见的离屏的屏幕），下面的目的就是要把画面显示到FBO中
//        mFrameBufferTextures = new int[1]; // 记录FBO纹理的ID
//        TextureHelper.genTextures(mFrameBufferTextures); // 生成并配置纹理
//
//        // 第三步：上面的 FBO缓冲区 与 FBO纹理 还没有任何关系，现在要让他们绑定起来
//        glBindTexture(GL_TEXTURE_2D, mFrameBufferTextures[0]);
//
//        // 生产2D纹理图像
//        /*
//            int target,         要绑定的纹理目标
//            int level,          level一般都是0
//            int internalformat, 纹理图像内部处理的格式是什么，rgba
//            int width,          宽
//            int height,         高
//            int border,         边界
//            int format,         纹理图像格式是什么，rgba
//            int type,           无符号字节的类型
//            java.nio.Buffer pixels
//         */
//        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
//
//        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]); // 发生关系
//         /*
//            int target,     fbo的纹理目标
//            int attachment, 附属到哪里
//            int textarget,  要绑定的纹理目标
//            int texture,    纹理
//            int level       level一般都是0
//         */
//        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
//
//        // 第四步：解绑操作
//        glBindTexture(GL_TEXTURE_2D, 0);
//        glBindFramebuffer(GL_FRAMEBUFFER, 0);
//    }

    @Override // @param textureId 这里的纹理id 是摄像头的
    public int onDrawFrame(int textureId) {
        glViewport(0, 0, mWidth, mHeight); // 设置视窗大小
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]); // 绑定（否则会绘制到屏幕上了）
        glUseProgram(mProgramId);

        mVertexBuffer.position(0); // 顶点坐标赋值
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, mVertexBuffer); // 传值
        glEnableVertexAttribArray(vPosition);  // 激活

        // 纹理坐标赋值
        mTextureBuffer.position(0);
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, mTextureBuffer); // 传值
        glEnableVertexAttribArray(vCoord); // 激活

        // TODO 变换矩阵
        glUniformMatrix4fv(vMatrix, 1, false, matrix, 0);

        // TODO 片元 vTexture
        glActiveTexture(GL_TEXTURE0); // 激活图层
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        glUniform1i(vTexture, 0);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); // 通知 opengl 绘制
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0); // 解绑 fbo
        // return textureId;
        return mFrameBufferTextures[0]; // TODO 应该返回 FBO 的纹理id
    }

    // 接收外界传递进来的 变换矩阵
    public void setMatrix(float[] matrix) {
        this.matrix = matrix;
    }


    @Override
    protected void changeTextureData() {
    }

    /*private void releaseFrameBuffers() {
        if (null != mFrameBufferTextures) {
            glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (null != mFrameBuffers) {
            glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }
    @Override
    public void release() {
        super.release();
        releaseFrameBuffers();
    }*/
}
