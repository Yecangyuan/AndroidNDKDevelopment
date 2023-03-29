// TODO 顶点着色器 位置规划的

attribute vec4 vPosition; // 顶点坐标，相当于：相机的四个点位置排版

attribute vec4 vCoord; // 纹理坐标，用来图形上色的

uniform mat4 vMatrix; // 变换矩阵，4*4的格式的

varying vec2 aCoord; // 把这个最终的计算成果，给片元着色器 【不需要Java传递，他是计算出来的】

void main() {
    gl_Position = vPosition; // 确定好位置排版

    // 下面的方式，是为了兼容所有设备
    aCoord = (vMatrix * vCoord).xy;

    // 部分机型有问题（早期都是这样干）
    // aCoord = vCoord.xy;
}
