package com.simley.lib_network.model

/**
 * 基础的响应类
 */
class BaseResponse<T>(
    val code: Int,
    val msg: String,
    val data: T
)
