package com.drake.net.interfaces

import android.view.View
import com.simley.lib_network.Net
import com.simley.lib_network.NetConfig
import com.drake.net.R
import com.drake.net.exception.*
import com.simley.lib_network.R
import com.simley.lib_network.utils.TipUtils
import java.net.UnknownHostException

interface NetErrorHandler {

    companion object DEFAULT : NetErrorHandler

    /**
     * 全局的网络错误处理
     *
     * @param e 发生的错误
     */
    fun onError(e: Throwable) {
        val message = when (e) {
            is UnknownHostException -> NetConfig.app.getString(R.string.net_host_error)
            is URLParseException -> NetConfig.app.getString(R.string.net_url_error)
            is NetConnectException -> NetConfig.app.getString(R.string.net_connect_error)
            is NetworkingException -> NetConfig.app.getString(R.string.net_networking_error)
            is NetSocketTimeoutException -> NetConfig.app.getString(
                R.string.net_connect_timeout_error,
                e.message
            )
            is DownloadFileException -> NetConfig.app.getString(R.string.net_download_error)
            is ConvertException -> NetConfig.app.getString(R.string.net_parse_error)
            is RequestParamsException -> NetConfig.app.getString(R.string.net_request_error)
            is ServerResponseException -> NetConfig.app.getString(R.string.net_server_error)
            is NullPointerException -> NetConfig.app.getString(R.string.net_null_error)
            is NoCacheException -> NetConfig.app.getString(R.string.net_no_cache_error)
            is ResponseException -> e.message
            is HttpFailureException -> NetConfig.app.getString(R.string.request_failure)
            is NetException -> NetConfig.app.getString(R.string.net_error)
            else -> NetConfig.app.getString(R.string.net_other_error)
        }

        Net.debug(e)
        TipUtils.toast(message)
    }

    /**
     * 当你使用包含缺省页功能的作用域中发生错误将回调本函数处理错误
     *
     * @param e 发生的错误
     * @param view 缺省页, StateLayout或者PageRefreshLayout
     */
    fun onStateError(e: Throwable, view: View) {
        when (e) {
            is ConvertException,
            is RequestParamsException,
            is ResponseException,
            is NullPointerException -> onError(e)
            else -> Net.debug(e)
        }
    }
}