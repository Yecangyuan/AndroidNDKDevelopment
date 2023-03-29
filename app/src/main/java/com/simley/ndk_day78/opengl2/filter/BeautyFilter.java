package com.simley.ndk_day78.opengl2.filter;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

import android.content.Context;

import com.simley.ndk_day78.R;
import com.simley.ndk_day78.opengl2.filter.BaseFrameFilter;

/**
 * 美颜过滤器
 */
public class BeautyFilter extends BaseFrameFilter {
    private final int width; // 最新改变的宽
    private final int height; // 最新改变的高

    public BeautyFilter(Context context) {
        super(context, R.raw.base_vertex, R.raw.beauty_fragment);
        width = glGetUniformLocation(mProgramId, "width"); // 关联最新改变的宽
        height = glGetUniformLocation(mProgramId, "height"); // 关联最新改变的高
    }

    @Override
    public int onDrawFrame(int textureId) {
        // 1：设置视窗
        glViewport(0, 0, mWidth, mHeight);
        // 这里是因为要渲染到FBO缓存中，而不是直接显示到屏幕上
        glBindFramebuffer(GL_FRAMEBUFFER, mFrameBuffers[0]);
        // 渲染 传值
        // 1：顶点数据
        mVertexBuffer.position(0);
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, mVertexBuffer); // 传值
        glEnableVertexAttribArray(vPosition); // 传值后激活

        // 2：纹理坐标
        mTextureBuffer.position(0);
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, mTextureBuffer); // 传值
        glEnableVertexAttribArray(vCoord); // 传值后激活

        // 给纹理宽高 赋值  只有这个点和以前不同，其他的 全部都是模板代码 都是之前的
        glUniform1i(width, mWidth);
        glUniform1i(height, mHeight);

        // 片元 vTexture
        glActiveTexture(GL_TEXTURE0); // 激活图层
        glBindTexture(GL_TEXTURE_2D, textureId); // 绑定
        glUniform1i(vTexture, 0); // 传递参数

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4); // 通知opengl绘制

        // 解绑FBO
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        return mFrameBufferTextures[0]; // 同学们注意：返回fbo的纹理id
    }
}
