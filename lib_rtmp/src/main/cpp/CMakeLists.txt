cmake_minimum_required(VERSION 3.22.1)
project("lib_rtmp")

#file(GLOB allCPP *.c *.h *.cpp)
#
#include_directories(librtmp)
#include_directories(flvmuxer)
#
#AUX_SOURCE_DIRECTORY(LIB_RTMP ./librtmp)
#AUX_SOURCE_DIRECTORY(LIB_FLV_MUXER ./flvmuxer)
include_directories(./librtmp)

add_library(lib_rtmp
        SHARED
        # Provides a relative path to your source file(s).
        lib_rtmp.c
        rtmpmuxer.c   # 复用器
        ./librtmp/amf.c
        ./librtmp/amf.h
        ./librtmp/bytes.h
        ./librtmp/dh.h
        ./librtmp/dhgroups.h
        ./librtmp/handshake.h
        ./librtmp/hashswf.c
        ./librtmp/http.h
        ./librtmp/log.c
        ./librtmp/log.h
        ./librtmp/parseurl.c
        ./librtmp/rtmp.c
        ./librtmp/rtmp.h
        ./librtmp/rtmp_sys.h
        ./flvmuxer/xiecc_rtmp.c
        ./flvmuxer/xiecc_rtmp.h
        )

add_definitions(-DNO_CRYPTO)

target_link_libraries(
        lib_rtmp
        log)