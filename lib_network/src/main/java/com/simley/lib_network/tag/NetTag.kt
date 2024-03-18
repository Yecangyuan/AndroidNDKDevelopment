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

package com.simley.lib_network.tag

import com.drake.net.interfaces.ProgressListener
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KType

sealed class NetTag {
    class Extras : HashMap<String, Any?>()
    class UploadListeners : ConcurrentLinkedQueue<ProgressListener>()
    class DownloadListeners : ConcurrentLinkedQueue<ProgressListener>()

    @JvmInline
    value class RequestId(val value: Any)

    @JvmInline
    value class RequestGroup(val value: Any)

    @JvmInline
    value class RequestKType(val value: KType)

    @JvmInline
    value class DownloadFileMD5Verify(val value: Boolean = true)

    @JvmInline
    value class DownloadFileNameDecode(val value: Boolean = true)

    @JvmInline
    value class DownloadTempFile(val value: Boolean = true)

    @JvmInline
    value class DownloadFileConflictRename(val value: Boolean = true)

    @JvmInline
    value class DownloadFileName(val value: String)

    @JvmInline
    value class CacheKey(val value: String)

    @JvmInline
    value class CacheValidTime(val value: Long)

    @JvmInline
    value class DownloadFileDir(val value: String) {
        constructor(fileDir: File) : this(fileDir.absolutePath)
    }
}
