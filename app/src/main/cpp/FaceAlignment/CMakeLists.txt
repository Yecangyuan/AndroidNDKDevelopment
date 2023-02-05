# Use C++11 特性
#set(CMAKE_CXX_STANDARD 11)
#set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11") # 他需要用到 C++11特下

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2") # 设置他需要的参数

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -msse4.1") # 设置他需要的参数

include_directories(include) # 导入头文件

set(src_files # CPP源码关联
    src/cfan.cpp
    src/face_alignment.cpp
    src/sift.cpp
    )

add_library(seeta_fa_lib STATIC ${src_files}) # 生成静态库