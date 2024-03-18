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

package com.simley.lib_network.transform

import kotlinx.coroutines.Deferred

/**
 * 可以将[Deferred]返回结果进行转换
 * [block]在[Deferred]执行成功返回结果时执行
 */
fun <T, R> Deferred<T>.transform(block: (T) -> R): DeferredTransform<T, R> {
    return DeferredTransform(this, block)
}

data class DeferredTransform<T, R>(val deferred: Deferred<T>, val block: (T) -> R)