// TODO 美颜的片元着色器代码
// 既然是美颜，肯定是美颜整个屏幕，所以不需要顶点着色器 直接用base的顶点着色器，只需要美颜的片元着色器

precision mediump float;// 中精度

varying vec2 aCoord;// 纹理坐标

uniform sampler2D vTexture;// 采样器

uniform int width;// 纹理的宽

uniform int height;// 纹理的高

vec2 blurCoordinates[20];

// 大问题思考：我们做美颜，难道要自己写吗？ 不需要自己写美颜功能
// 美颜功能是基础功能-很复杂的   小米手机  华为手机  xxx手机

// 美颜三部曲：1.高斯模糊，  2.高反差     3.磨皮
// 官方源码 高斯模糊：采样点 绿色 基本上就可以做高斯模糊了，  我们等下要高rgb，我们会更丰富

void main() {
    // TODO 1: 高斯模糊处理（参考代码是对绿色通道做了高斯模糊， 而我们这里对所有通道做了高斯模糊）
    vec2 singleStepOffset = vec2(1.0/float(width), 1.0/float(height));

    /*
    高斯模糊原理：所谓"模糊"，可以理解成每一个像素都取周边像素的平均值
         1 中间像素点 是根据周边像素的平均值来进行高斯模糊的
         2 360度 / 20度 = 18度 每隔18度就取像素值，然后再去映射下面的坐标值
    */
    // 下面是为了去 采集20个点 （对green通道进行高斯模糊） 我们等下使用rgb通道进行高斯模糊经验值（取周边 平均值）
    blurCoordinates[0] = aCoord.xy + singleStepOffset * vec2(0.0, -10.0);// 向量的偏移运算(都是基础功能美颜，固定的模板代码了)
    blurCoordinates[1] = aCoord.xy + singleStepOffset * vec2(0.0, 10.0);
    blurCoordinates[2] = aCoord.xy + singleStepOffset * vec2(-10.0, 0.0);
    blurCoordinates[3] = aCoord.xy + singleStepOffset * vec2(10.0, 0.0);
    blurCoordinates[4] = aCoord.xy + singleStepOffset * vec2(5.0, -8.0);
    blurCoordinates[5] = aCoord.xy + singleStepOffset * vec2(5.00, 8.0);
    blurCoordinates[7] = aCoord.xy + singleStepOffset * vec2(-5.0, 8.0);
    blurCoordinates[6] = aCoord.xy + singleStepOffset * vec2(-5., -8.0);
    blurCoordinates[8] = aCoord.xy + singleStepOffset * vec2(8.0, -5.0);
    blurCoordinates[9] = aCoord.xy + singleStepOffset * vec2(8.0, 5.0);
    blurCoordinates[10] = aCoord.xy + singleStepOffset * vec2(-8.0, 5.0);
    blurCoordinates[11] = aCoord.xy + singleStepOffset * vec2(-8.0, -5.0);
    blurCoordinates[12] = aCoord.xy + singleStepOffset * vec2(0.0, -6.0);
    blurCoordinates[13] = aCoord.xy + singleStepOffset * vec2(0.0, 6.0);
    blurCoordinates[14] = aCoord.xy + singleStepOffset * vec2(6.0, 0.0);
    blurCoordinates[15] = aCoord.xy + singleStepOffset * vec2(-6.0, 0.0);
    blurCoordinates[16] = aCoord.xy + singleStepOffset * vec2(-4.0, -4.0);
    blurCoordinates[17] = aCoord.xy + singleStepOffset * vec2(-4.0, 4.0);
    blurCoordinates[18] = aCoord.xy + singleStepOffset * vec2(4.0, -4.0);
    blurCoordinates[19] = aCoord.xy + singleStepOffset * vec2(4.0, 4.0);

    vec4 currentColor = texture2D(vTexture, aCoord);// 当前采样点的像素值 vec4代表rgba
    vec3 rgb = currentColor.rgb;

    for (int i = 0; i < 20; i++){ // 计算坐标颜色值的总和
        rgb += texture2D(vTexture, blurCoordinates[i].xy).rgb;// 采集20个点的像素
    }

    // rgb： 21个点的总和
    vec4 blur = vec4(rgb *1.0/21.0, currentColor.a);

    // gl_FragColor = blur; // TODO 1: 高斯模糊处理 看看效果 【画面模糊了，看不清楚细节了】

    // TODO 2：高反差
    vec4 highPassColor = currentColor - blur;// 高反差公式：原图 - 高斯模糊 = 高反差
    // 强度系数 - TODO 强光模式(手电筒照上去)
    // clamp（夹具函数） glsl着色器语音的内置函数 ：取三个参数的中间值
    highPassColor.r = clamp(2.0*highPassColor.r*highPassColor.r * 24.0, 0.0, 1.0);// 【同学们注意：需要反复去调】
    highPassColor.g = clamp(2.0*highPassColor.g*highPassColor.g * 24.0, 0.0, 1.0);
    highPassColor.b = clamp(2.0*highPassColor.b*highPassColor.b * 24.0, 0.0, 1.0);

    vec4 highPassBlur = vec4(highPassColor.rgb, 1.0);// 可以去痘印，疤痕 等
    // highPassBlur == rgba 经过了 高斯模糊 高反差 强光模式
    gl_FragColor = highPassBlur;// TODO 2：高反差，先不磨皮，先运行看看效果了【只能看到五官轮廓黑边边了】

    // TODO 3：磨皮
    // 蓝色分量  min glsl着色器语音的内置函数：取两个参数的最小值
    float blue = min(currentColor.b, blur.b);// 取两个参数的最小值
    float value = clamp((blue - 0.2) * 5.0, 0.0, 1.0);// 【同学们注意：需要反复去调】 经验值  最开始做美颜功能前辈

    // 取r, g, b 三个分量中最大的值   max glsl着色器语音的内置函数：取三个参数的最大值
    float maxChannelColor = max(max(highPassColor.r, highPassColor.g), highPassColor.b);
    float intensity = 1.0;// 磨皮强度，磨皮强度 不能太强，例如:3.0 那就看起来有斑纹了
    float currentIntensity = (1.0 - maxChannelColor / (maxChannelColor + 0.2)) * value * intensity;// 【同学们注意：需要反复去调】 经验值  最开始做美颜功能前辈

    // 线性混合
    // mix: 返回线性混合的x和y，如：x⋅(1−a)+y⋅a
    vec3 rgb_value = mix(currentColor.rgb, blur.rgb, currentIntensity);

    // 把rgb的vec3 变成 vec4，所以增加1.0 透明通道， 因为gl_FragColor内置变量就是rgba的vec4类型
    gl_FragColor = vec4(rgb_value, 1.0);// gl_FragColor: rgb不支持 yuv   TODO 3：磨皮，看看效果

    // gl_FragColor 必须是接收 rgba，所以没有办法，手动组装  1.0 == a 透明值

    // 美白的代码：color.r=max(min(color.r, 1.0), 0.0);
    // 改装后代码：clamp(color.r, 1.0, 0.0);
}
