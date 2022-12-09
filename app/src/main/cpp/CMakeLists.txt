
cmake_minimum_required(VERSION 3.18.1)

project("ndk_day78")
# 目的是为了让CMake找到我们的头文件在哪里，这里是在 cpp/include 这个文件夹下
include_directories(include)
#设置系统变量，目的是为了让CMake知道在哪个目录下找到我们的libopencv_java3.so这个库
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/libs/${CMAKE_ANDROID_ARCH_ABI}")

AUX_SOURCE_DIRECTORY(./ SRC_LIST) # 当前文件夹下的所有文件，不包含子文件夹
# add_library的作用就是将()中的所有文件打包成一个.so文件，第一个是这个.so文件的文件名libndk_day78.so
add_library(ndk_day78  # Sets the name of the library.
        SHARED
#        text_recognize.cpp
#        player.cpp
#        native-lib.cpp
        ${SRC_LIST})


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

find_library( # Sets the name of the path variable.
        log-lib
        log)

target_link_libraries( # Specifies the target library.
        ndk_day78
        -ljnigraphics
        avfilter
        avcodec
        avdevice
        avutil
        swscale
        android
        avformat
        swresample
        libopencv_java4
        ${log-lib})