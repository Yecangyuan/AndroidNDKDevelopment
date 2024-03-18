/*
 * Copyright (C) 2018 Drake, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simley.lib_network.scope

import android.app.Dialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.simley.lib_network.NetConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 自动加载对话框网络请求
 *
 *
 * 开始: 显示对话框
 * 错误: 提示错误信息, 关闭对话框
 * 完全: 关闭对话框
 *
 * @param activity 对话框跟随生命周期的FragmentActivity
 * @param dialog 不使用默认的加载对话框而指定对话框
 * @param cancelable 是否允许用户取消对话框
 */
@Suppress("DEPRECATION")
class DialogCoroutineScope(
    val activity: FragmentActivity,
    var dialog: Dialog? = null,
    val cancelable: Boolean? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.Main
) : NetCoroutineScope(dispatcher = dispatcher), LifecycleObserver {

    init {
        activity.lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    dialog?.cancel()
                }
            }
        })
    }

    override fun start() {
        activity.runOnUiThread {
            val dialog = dialog ?: NetConfig.dialogFactory.onCreate(activity)
            this.dialog = dialog
            cancelable?.let { dialog.setCancelable(it) }
            dialog.setOnCancelListener {
                cancel()
            }
            if (!activity.isFinishing) {
                dialog.show()
            }
        }
    }

    override fun previewFinish(succeed: Boolean) {
        super.previewFinish(succeed)
        if (succeed && previewBreakLoading) {
            dialog?.dismiss()
        }
    }

    override fun finally(e: Throwable?) {
        super.finally(e)
        dialog?.dismiss()
    }

}