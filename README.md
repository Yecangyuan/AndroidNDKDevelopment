# AndroidNDKDevelopment

个人学习工程（Gradle 工程名为 `NDK_Day78`），以 **Android NDK / JNI** 为主线，同时沉淀常用 Android 基础库模块。

仅供个人学习使用。

---

## 一、环境要求

| 项目 | 版本 | 说明 |
| --- | --- | --- |
| Gradle | 8.13 | 由 wrapper 管理 |
| Android Gradle Plugin | 8.11.2 | |
| Kotlin | 1.8.10 | KSP `1.8.10-1.0.9`，Hilt `2.41` |
| JDK | **17** | AGP 8.11 的最低要求；工程已统一固定 Java 工具链为 17 |
| NDK | **25.1.8937393（r25）** | 不可随意升级，见第五节说明 |
| CMake | 3.22.1 | |
| compileSdk / minSdk / targetSdk | 33 / 19 / 33 | app 模块；`lib_mlkit`、`lib_mqtt` 为 34，`lib_database_room` minSdk 为 24 |
| CPU 架构 | **仅 arm64-v8a** | 由 `config.gradle` 的 `abiFilters` 控制 |

### 构建

```bash
./gradlew assembleDebug        # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew clean assembleDebug  # 全量重建
```

Android Studio 中直接 Run 即可，无需额外调整 Gradle JDK（工程已固定 Java 工具链，JDK 17 / 21 / 22 下均可构建）。

已在 **JDK 17（Zulu）** 与 **JDK 21（Android Studio 自带 JBR）** 下验证 `clean assembleDebug` 通过，APK 约 104 MB（含 OpenCV、FFmpeg 等预编译原生库）。

---

## 二、工程结构

```
AndroidNDKDevelopment/
├── app/                      # NDK / JNI 实战示例主工程
├── lib_base/                 # 公共基础模块（其余模块的底座）
├── lib_ext/ │ lib_layout/    # Kotlin 扩展 / 布局（占位）
├── lib_mvp/ │ lib_mvi/       # MVP / MVI 架构
├── lib_database/             # SQLite（占位）
├── lib_database_room/        # Room 数据库
├── lib_network/ lib_okhttp/ lib_okio/ lib_socket/   # 网络与通信
├── lib_serialport/           # 串口通信（含 native）
├── lib_rtmp/ lib_rtsp/ lib_hls/                     # 流媒体
├── lib_glide/ lib_mlkit/     # 图片加载 / Google MLKit
├── lib_mqtt/ lib_catcher/    # MQTT / ANR 监控
├── config.gradle             # 全局配置中心（SDK 版本、abiFilters、依赖清单）
└── settings.gradle           # 模块与仓库声明
```

`config.gradle` 是全局配置的唯一入口：`android{}`（版本号）、`abiFilters`（CPU 架构）、`projectsPath`（app 聚合的模块）、`baseDeps` / `deps`（依赖清单）。新增依赖请改这里，而不是分散在各模块。

---

## 三、模块清单

### 参与构建的模块（已在 `settings.gradle` 中 `include`）

| 模块 | 功能 | 关键依赖 | 源码文件数 |
| --- | --- | --- | --- |
| `app` | NDK / JNI 实战示例主工程，详见下一节 | fmod、OpenCV、FFmpeg、SoundTouch、Tesseract | 见第四节 |
| `lib_base` | 公共基类：`BaseActivity`、`BaseFragment`；其余模块的底座 | AndroidX、Material、ViewBinding | 2 |
| `lib_ext` | Kotlin 扩展工具（占位，暂无源码） | lib_base | 0 |
| `lib_layout` | 布局相关（占位，暂无源码） | lib_base | 0 |
| `lib_mvp` | MVP 架构示例 | lib_base | 3 |
| `lib_mvi` | MVI 架构示例（占位，暂无源码） | AndroidX | 0 |
| `lib_database` | SQLite 数据库封装（占位，暂无源码） | AndroidX | 0 |
| `lib_database_room` | Room 数据库：`AppDatabase` / `UserDao` / `RoomManager` / `RoomViewModel` | Room 2.6.1、Lifecycle 2.7.0 | 5 |
| `lib_okhttp` | OkHttp 封装：`OkHttpUtils` | okhttp 4.12.0、logging-interceptor | 1 |
| `lib_okio` | Okio 封装（占位，暂无源码） | okio 3.3.0 | 0 |
| `lib_socket` | 异步 Socket / SSL 通信：`AsyncServer` 系列、`SelectorWrapper`、缓冲与文件收发 | BouncyCastle 1.70 | 169 |
| `lib_serialport` | 串口通信：`SerialPortManager` + native `SerialPort.c`；附设备搜索与列表适配 | lib_base | 13 |
| `lib_rtmp` | RTMP 推流：`librtmp`(C) + `RTMPMuxer` / `RtmpClient`(JNI) | 原生 librtmp | 21 |
| `lib_mlkit` | Google MLKit：人脸检测 / 姿态检测 | face-detection 16.1.6、pose-detection 18.0.0-beta4 | 1 |
| `lib_mqtt` | MQTT 客户端：`MqttManager`（连接 / 订阅 / 发布 / 断开） | Eclipse Paho 1.1.1、BouncyCastle 1.70 | 1 |
| `lib_catcher` | ANR 监控：`ANRWatchDog` / `ANRError` | AndroidX | 2 |

> 「源码文件数」指 `src/main` 下 `.kt` / `.java` / `.c` / `.h` 的数量，仅作规模参考。

### 暂未纳入构建的模块

以下 4 个模块**代码已存在但未写入 `settings.gradle`**，因此不参与构建（无 `build/` 产物）：

| 模块 | 功能 | 关键依赖 | 源码文件数 |
| --- | --- | --- | --- |
| `lib_glide` | Glide 图片加载 + 加载进度：`ProgressManager`、`ProgressResponseBody`、`CircleProgressView`、`YeGlideModule` | Glide 4.15.1 | 18 |
| `lib_network` | 网络请求：协程封装 `NetCoroutine`、`NetConfig`、缓存策略 | okhttp 4.10.0、startup-runtime、coroutines | 72 |
| `lib_hls` | HLS 媒体源（抽取自 ExoPlayer） | — | 34 |
| `lib_rtsp` | RTSP 媒体源（抽取自 ExoPlayer） | — | 30 |

需要启用时，在 `settings.gradle` 中补 `include ':lib_xxx'` 即可。

---

## 四、app 模块：NDK / JNI 实战

`applicationId` / `namespace`：`com.simley.ndk_day78`

### 功能包

| 包 | 内容 |
| --- | --- |
| `egl` | EGL 环境搭建：`eglGetDisplay` → `eglInitialize` → `eglChooseConfig` → `eglCreateContext` → `eglCreateWindowSurface` |
| `opengl` / `opengl2` | OpenGL ES 渲染、`GLSurfaceView`、抖音快慢速播放、大眼滤镜与人脸特效；含录制与自定义录音按钮 |
| `player` | 基于 FFmpeg 的音视频播放器：`YEPlayer`、音频服务、视频播放页 |
| `face` | 人脸检测与关键点追踪（native `FaceTrack` + `FaceAlignment`） |
| `idcard` | 身份证识别（native `idcard_recognition`） |
| `bandcard` | 银行卡识别（native `cardocr`） |
| `textrecognition` / `ocr` | 文字识别（native `text_recognize` + Tesseract） |
| `fmod` | FMOD 音频引擎接入（`native-fmod`） |
| `serialport` | 串口通信示例 |

### native 层（`app/src/main/cpp`）

- **自研源码**：`native-lib`、`EGL`、`FaceTrack`、`YEFFmpeg` / `YEAudio` / `YEVideo` / `YEQueue` / `YEPlayStatus`、`YECallJava`、`JavaVMInit`、`player`、`native-fmod`、`cardocr`、`idcard_recognition`、`text_recognize`、`bitmap_util`、`cv_helper`、`utils`
- **第三方源码**：`soundtouch/`（变声）、`FaceAlignment/`（人脸关键点，静态库 `libseeta_fa_lib.a`）
- **预编译库**（`cpp/libs/arm64-v8a`）：FFmpeg（`libavcodec` / `avdevice` / `avfilter` / `avformat` / `avutil` / `swresample` / `swscale`）、`libfmod` / `libfmodL`、`libopencv_java4`
- **本地 jar**：`app/libs/fmod.jar`
- **产出**：`libndk_day78.so`

### app 依赖

Tesseract4Android 4.3.0、Hilt 2.41、RxPermissions 0.12、RxJava3 3.1.6、RxAndroid3 3.0.0、Bugly crashreport 4.1.9、CameraX core / camera2 1.1.0、zstd-jni 1.5.5-1、LeakCanary 2.10（仅 debug）。

---

## 五、构建配置关键点

以下几点是踩坑后固化下来的约束，改动前请先阅读，否则容易把工程改回不可编译状态。

### 1. NDK 必须固定在 r25，不能跟随 AGP 默认值

`minSdk = 19`，而 **NDK 从 r26 起不再支持 API < 21**。AGP 8.11.2 的默认 NDK 是 r27，直接构建会报：

```
[CXX1110] Platform version 19 is unsupported by this NDK.
```

因此 `config.gradle` 中显式声明了 `ndkVersion = "25.1.8937393"`，并由三个含原生构建的模块引用：

```groovy
ndkVersion rootProject.android.ndkVersion
```

**`ndkVersion` 是模块级属性**，`app`、`lib_rtmp`、`lib_serialport` 三处都必须写；漏掉任何一个都会再次报错。若将来把 `minSdk` 提升到 21，才可解除该限制。

### 2. Java 工具链固定在 17

Kotlin Gradle Plugin 会从「运行 Gradle 的那个 JDK」推断 `jvmTarget`，而 **Kotlin 1.8.10 不认识 JVM target 20 / 21**（该支持从 Kotlin 1.9.20 才加入）。Android Studio 默认用自带 JBR 21 运行 Gradle，会导致配置阶段抛：

```
java.lang.IllegalArgumentException: Unknown Kotlin JVM target: 21
```

注意：模块里即使已写 `kotlinOptions { jvmTarget = "17" }` 也挡不住——KGP 仍会去解析由 Java 工具链推导出的 convention 值。

根 `build.gradle` 中已统一处理，覆盖所有 Kotlin 模块（含后续新增）：

```groovy
subprojects { subproject ->
    subproject.plugins.withId('org.jetbrains.kotlin.android') {
        def kotlinExt = subproject.extensions.findByName('kotlin')
        if (kotlinExt != null) kotlinExt.jvmToolchain(17)
    }
}
```

**根本解**是升级 Kotlin 到 1.9.20+，但需同步升级 KSP 与 Hilt，属较大改动，暂未执行。

### 3. 依赖源使用阿里云镜像

`settings.gradle` 的 `pluginManagement` 与 `dependencyResolutionManagement` 两处均以阿里云镜像优先，用于规避直连 Maven Central 时出现的 TLS 握手中断（`Remote host terminated the handshake`）。

**`https://jitpack.io` 必须保留**：阿里云未镜像 JitPack，而 `RxPermissions` 等依赖来自 JitPack。

另注：JitPack 的 artifactId **区分大小写**，必须与 GitHub 仓库名一致，例如 `com.github.tbruyelle:RxPermissions:0.12`（写成全小写会 404）。

### 4. CMake 版本与 SDK 中已安装的版本保持一致

`app` / `lib_rtmp` / `lib_serialport` 统一使用 CMake **3.22.1**。三个模块的 `CMakeLists.txt` 中 `cmake_minimum_required` 为 3.18.1，由 3.22.1 满足。若声明了本机未安装的版本，会报 `[CXX1300] CMake 'x.y.z' was not found in SDK`。

### 5. 原生链接库

`app/src/main/cpp/CMakeLists.txt` 的 `target_link_libraries` 中，使用哪个前缀的符号就要链接对应的库：

| 符号前缀 | 需要的库 |
| --- | --- |
| `egl*` | `EGL` |
| `gl*` | `GLESv3` |
| `ANativeWindow_*` / `AAsset*` | `android` |
| `__android_log_*` | `log` |
| `slCreateEngine` | `OpenSLES` |
| `AndroidBitmap_*` | `jnigraphics` |

当前已链接 `EGL`（供 `EGL.cpp` 使用）。若后续新增 `glXxx` 调用，需补 `GLESv3`。

### 6. 根任务会构建所有模块

`./gradlew assembleDebug` 是**根任务**，会构建 `settings.gradle` 中 `include` 的**全部**模块——即使某些模块并未被 app 聚合（`config.gradle` 的 `projectsPath` 只列了 6 个）。因此任一模块编译失败都会阻塞整体构建。只想构建主工程时可指定模块：

```bash
./gradlew :app:assembleDebug
```

---

## 六、已知问题与待办

1. `gradle/wrapper/gradle-wrapper.properties` 第 2–4 行残留合并冲突标记（`<<<<<<<`、`=======`、`>>>>>>>`）。实测对构建无影响（仅多余的 Properties 键），建议清理。
2. `app/build.gradle` 的 `debug` 构建类型同时设置了 `debuggable true` 与 `minifyEnabled true`，Gradle 会告警并自动禁用混淆。按实际意图二选一即可。
3. `lib_ext` / `lib_layout` / `lib_database` / `lib_mvi` / `lib_okio` 为空占位模块，仅有构建脚本、无源码。
4. `lib_glide` / `lib_hls` / `lib_network` / `lib_rtsp` 未纳入 `settings.gradle`，当前不参与构建。
5. 工程版本存在错配：AGP 8.11.2（2025）搭配 Kotlin 1.8.10（2023）。当前靠固定 Java 工具链规避，长期建议整体对齐版本。
6. `.workbuddy/`（本地工具数据目录）未被 `.gitignore` 忽略，建议补充忽略规则，避免误提交。
7. **银行卡识别的准确率仍不完整（16/19 位）**。参数已按真机矩阵实测选定（2 种模型 × 3 种引擎 × 4 种版面 × 5 种预处理）：
   `OEM_LSTM_ONLY` + `PSM_RAW_LINE` + 先按 native 侧 `co1::findCardNumberArea` 的同一比例启发式裁出卡号条带
   （`x=cols/12, y=rows/2, w=cols*5/6, h=rows/4`）+ 灰度化。注意 `PSM_SINGLE_LINE` 会把该条带判为无效而返回空。
   训练数据已由 31.8 MB 的老模型换为 `tessdata_fast`（4.1 MB）：源文件减少 26.5 MiB，因 APK 内 assets 按 Deflate
   压缩存储（约 50%），APK 净减约 14 MB。
   实测：换模型前 11/19 位（得 `10993123007`），换模型 + 灰度后 **16/19 位**（得 `2284109931240479`，
   真值 `6228481099312404479`）。放大 2x/3x 反而变差。
   剩余误差集中在首六位 `622848`——它恰好压在卡面高光水滴上，属图像固有难点。
   要完全正确，需实现 `cardocr.cpp` 里原本设计的 native 逐字切分识别（定位卡区 → 二值化 → 单字符切分 → 逐字比对）。
   该文件目前**无任何 JNI 导出**，是未实现的占位，`BankCardRecognition.cardOcr` 调用会抛 `UnsatisfiedLinkError`。
8. **`FileUtil.copyAssets2SDCard` 的另外两个调用方同样受分区存储影响**：`MyGLRenderer` 拷贝人脸模型
   (`/sdcard/haarcascade_frontalface_alt.xml`、`/sdcard/seeta_fa_v1.1.bin`)、`MusicService` 拷贝音频
   (`/storage/emulated/0/Music/...`)。实测应用 UID 对 `/storage/emulated/0` **不可写**（`mkdir` 返回
   `Permission denied`，尽管 `WRITE_EXTERNAL_STORAGE` 显示已授予），故这两处拷贝会失败（现已有错误日志）。
   涉及的功能可能因此不可用，建议一并改为写应用私有目录。银行卡识别已按此方式修复。
