// TODO  Base 的 片元着色器  上色的

// float 数据的精度 （precision lowp = 低精度） （precision mediump = 中精度） （precision highp = 高精度）
precision mediump float;

// 根据上面的数据的精度，写下面的 采样器 相机的数据
uniform sampler2D vTexture;// 由于我们用的是 安卓的相机，就不能用他(用OpenGL 2D显示就行了，不需要摄像头了，sampler2D)

varying vec2 aCoord;// 把这个最终的计算成果，给片元着色器，拿到最终的成果，我才能上色

void main() {
    // texture2D (采样器, 坐标)  gl_FragColor OpenGL着色器语言内置的变量
    // gl_FragColor = texture2D(vTexture, aCoord); // 直接上色，你直接上色，是上camera_fragment.glsl的底片效果

    // 在网上Copy的公式：底片效果
    vec4 rgba = texture2D(vTexture, aCoord);// rgba
    gl_FragColor = vec4(1.-rgba.r, 1.-rgba.g, 1.-rgba.b, rgba.a);
}

