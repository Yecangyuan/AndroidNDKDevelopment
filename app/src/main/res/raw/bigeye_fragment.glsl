// TODO 大眼 片元着色器 【大眼特效的算法处理，针对纹理数据进行处理，局部放大算法】

precision mediump float;// 中精度

varying vec2 aCoord;// 纹理坐标
uniform sampler2D vTexture;// 采样器

uniform vec2 left_eye;// 左眼x/y
uniform vec2 right_eye;// 右眼x/y

// 把公式转变成代码
// r: 原来的点 距离眼睛中心点距离（半径）
// rmax: 局部放大 最大作用半径（不能无限放大，不然左眼会遮挡右眼 会重叠，所以这里是（两眼之间距离/2））（最大半径）
// a：系数，系数如果等于0，那么就等于当前的r
float fs(float r, float rmax) {
    float a = 0.4;// 放大系数
    // pow 内置函数，求平方运算的函数
    // return (1.0 - pow(r / rmax -1, 2.0) * a) * r; // 与后面 calcNewCoord 38行的  /r 抵消
    return (1.0 - pow(r / rmax - 1.0, 2.0) * a);
}

/* 放大区域外面的rgba，需要去寻找里面的rgba，来替代外面的rgba（复杂眼睛里面显示 到 放大的区域）
 * 根据需要采集的点aCoord， 来计算新的点
     oldCoord：原始的点，原来的点
     eye：     眼睛的坐标
     rmax：    (rmax两眼间距) 注意是放大后的间距
   return newCoord：左眼/右眼 放大位置的采用点
*/
vec2 calcNewCoord(vec2 oldCoord, vec2 eye, float rmax){
    vec2 newCoord = oldCoord;
    float r = distance(oldCoord, eye);// 求两点的距离
    // if(r < rmax){ // 半径之内的才放大
    if (r > 0.0 && r < rmax) { // 如果进不了if，那么还是返回原来的点
        float fsr = fs(r, rmax);
        //    新点 - 眼睛     /  老点 - 眼睛   = 新距离;
        // (newCoord - eye) / (coord - eye) = fsr;

        // 之前遗留参考如下：
        // (newCoord - eye) / (coord - eye) = fsr / r; // 上面没有*，这里就不需要除
        // newCoord - eye = fsr / r * (coord - eye)
        // newCoord = fsr / r * (coord - eye) + eye

        // newCoord新点 = 新距离 * （老点 - 眼睛） + 眼睛
        newCoord = fsr * (oldCoord - eye) + eye;
    }
    return newCoord;
}

void main(){
    // 两眼间距的一半
    float rmax = distance(left_eye, right_eye) / 2.0;// distance 求两点的距离(rmax两眼间距) 注意是放大后的间距

    // aCoord是整副图像，
    vec2 newCoord = calcNewCoord(aCoord, left_eye, rmax);// 求左眼放大位置的采样点
    newCoord = calcNewCoord(newCoord, right_eye, rmax);// 求右眼放大位置的采样点
    // 此newCoord就是大眼像素坐标值
    gl_FragColor = texture2D(vTexture, newCoord);
}