# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 代码混淆压缩比，在0~7之间，默认为5,一般不下需要修改
-optimizationpasses 5
# 混淆时不使用大小写混合，混淆后的类名为小写
-dontusemixedcaseclassnames
# 指定不去忽略非公共的库的类
-dontskipnonpubliclibraryclasses
# 指定不去忽略非公共的库的类的成员
-dontskipnonpubliclibraryclassmembers
# 不做预检验，Android不需要预校验，保留即可
-dontpreverify
# 是否生成混淆文件，需保留，不然release没法分析异常
-verbose
# 指定混淆时采用的算法，后面的参数是一个过滤器，这个过滤器是谷歌推荐的算法，一般不改变
-optimizations !code/simplification/artithmetic,!field/*,!class/merging/*
# 保护代码中的Annotation不被混淆，这在JSON实体映射时很重要，比如fastJson，如果被混淆了就无法使用了
-keepattributes *Annotation*
# 避免混淆泛型
-keepattributes Signature
# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable
# bugly混淆规则
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

-keepclassmembers class * { # native层 防止成员被移除或者被重命名
    native <methods>;
}
-keepclasseswithmembernames class *{ # native层 防止拥有该成员的类和成员被重命名
    native <methods>;
}

# ---------------------------------------------------------------------------
# 被 native「回调」的 Java 成员，需要单独保留。
#
# 上面两条只保住 native 方法本身；而 native 层还会用 GetMethodID / FindClass
# 按名字反查 Java 成员，这些成员在 Java 侧没有任何调用者，R8 会把它们移除或改名。
# 一旦找不到，JNI 会先抛 NoSuchMethodError / ClassNotFoundException，再因为
# 「带着未决异常继续调用 JNI 函数」而直接 abort 掉整个进程。
#
# 实测：debug 构建同样开启了 minifyEnabled，以下成员在 APK 的 dex 里全部不存在
# （onCallTimeInfo / onCallPrepared / onCallLoad / onCallRenderYUV / createAudioTrack
# 命中数均为 0），导致播放界面一点「播放」就 NoSuchMethodError 崩溃。
# ---------------------------------------------------------------------------

# YEPlayer：native 通过以下方法把播放状态、进度、解码帧回传 Java
-keepclassmembers class com.simley.ndk_day78.player.YEPlayer {
    public void onCallPrepared();
    public void onCallTimeInfo(int, int);
    public void onCallLoad(boolean);
    public void onCallRenderYUV(int, int, byte[], byte[], byte[]);
    public void onError(int, java.lang.String);
    # native 反射调用它创建 AudioTrack；JNI 不区分访问修饰符，private 也能查到
    android.media.AudioTrack createAudioTrack(int, int);
}
# native 用 FindClass + <init>(IIII[F)V 反射构造 Face 回传人脸检测结果
-keep class com.simley.ndk_day78.opengl2.face.Face {
    <init>(int, int, int, int, float[]);
}
