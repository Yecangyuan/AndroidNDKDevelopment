cmake_minimum_required(VERSION 3.22.1)

project("lib_serialport")

add_library(
        SerialPort
        SHARED
        SerialPort.h
        SerialPort.c)

find_library(
        log-lib
        log)

target_link_libraries(
        SerialPort
        ${log-lib})
