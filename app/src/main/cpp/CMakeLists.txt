# cmake最低版本要求
cmake_minimum_required(VERSION 3.18.1)

project("ndk_day78")
# 目的是为了让CMake找到我们的头文件在哪里，这里是在 cpp/include 这个文件夹下
include_directories(include)
include_directories(soundtouch/include)
include_directories(soundtouch/SoundTouch)
#设置系统变量，目的是为了让CMake知道在哪个目录下找到我们的libopencv_java3.so这个库
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}")

######################    Native层代码优化   ######################
# 1. 首先是开启 –gc-sections 。 –gc-sections 开启后，可以在 native 代码编译过程中移除无用的代码。
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI} -ffunction-sections -fdata-sections -Wl,--gc-sections -flto ${CMAKE_C_FLAGS} -flto")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffunction-sections -fdata-sections -Wl,--gc-sections")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--gc-sections")
# 2. 其次是开启 LTO 。 LTO 是 Link Time Optimization 的缩写，即链接阶段优化。
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -O3 -flto")

AUX_SOURCE_DIRECTORY(./ SRC_LIST) # 当前文件夹下的所有文件，不包含子文件夹
AUX_SOURCE_DIRECTORY(./soundtouch/SoundTouch SOUND_TOUCH) # 当前文件夹下的所有文件，不包含子文件夹

# 批量导入所有源文件 头文件
# file(GLOB allCPP *.c *.h *.cpp)

# add_library的作用就是将()中的所有文件打包成一个.so文件，第一个是这个.so文件的文件名libndk_day78.so
add_library(ndk_day78  # Sets the name of the library.
        SHARED
        # text_recognize.cpp
        # player.cpp
        # native-lib.cpp
        ${SRC_LIST}
        ${SOUND_TOUCH})


#add_library(libopencv_java4 SHARED IMPORTED)
#set_target_properties(
#        libopencv_java4
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}/libopencv_java4.so)
#message("cmake source: " ${CMAKE_SOURCE_DIR})


add_library(libopencv_java4 SHARED IMPORTED)
set_target_properties(
        libopencv_java4
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}/libopencv_java4.so)

#add_library(libffmepg SHARED IMPORTED)
#set_target_properties(
#        libffmepg
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}/libavfilter.so
#)
#add_library(
#        opencv_java
#        SHARED
#        IMPORTED)
#set_target_properties(
#        opencv_java
#        PROPERTIES IMPORTED_LOCATION
#        ../../main/cpp/libs/arm64-v8a/libopencv_java3.so)


# # 自动去这个路径下面找到这个so文件
# /Users/simley/Library/Android/sdk/ndk/23.1.7779620/toolchains/llvm/prebuilt/darwin-x86_64/sysroot/usr/lib/aarch64-linux-android/24/liblog.so
# 如果后面多次用到该`log`库，就需要通过find_library来定义一个变量`log-lib`，防止多处引用 重复查找
# 语法格式： find_library (<VAR> name [path1 path2 ...])
find_library( # Sets the name of the path variable.
        log-lib
        log)

target_link_libraries( ndk_day78
        -ljnigraphics
        # ffmpeg
        avfilter
        avcodec
        avdevice
        avutil
        swscale
        avformat
        swresample
        android
        # opensles
        OpenSLES
        # fmod
        fmod
        fmodL
        # opencv
        libopencv_java4
        # log
        ${log-lib})