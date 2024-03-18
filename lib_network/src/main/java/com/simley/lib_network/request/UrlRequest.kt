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

package com.simley.lib_network.request

open class UrlRequest : BaseRequest() {

    override fun param(name: String, value: String?) {
        value ?: return
        httpUrl.setQueryParameter(name, value)
    }

    override fun param(name: String, value: String?, encoded: Boolean) {
        value ?: return
        if (encoded) {
            httpUrl.setEncodedQueryParameter(name, value)
        } else {
            httpUrl.setQueryParameter(name, value)
        }
    }

    override fun param(name: String, value: Number?) {
        value ?: return
        httpUrl.setQueryParameter(name, value.toString())
    }

    override fun param(name: String, value: Boolean?) {
        value ?: return
        httpUrl.setQueryParameter(name, value.toString())
    }
}