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
package com.drake.net.body

import android.os.SystemClock
import com.drake.net.component.Progress
import com.drake.net.interfaces.ProgressListener
import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue

class NetResponseBody(
    private val body: ResponseBody,
    private val progressListeners: ConcurrentLinkedQueue<ProgressListener>? = null,
    private val complete: (() -> Unit)? = null
) : ResponseBody() {

    private val progress = Progress()
    private val bufferedSource by lazy { body.source().toProgress().buffer() }
    private val contentLength by lazy { body.contentLength() }

    override fun contentType(): MediaType? {
        return body.contentType()
    }

    override fun contentLength(): Long {
        return contentLength
    }

    override fun source(): BufferedSource {
        return bufferedSource
    }

    /**
     * 复制一段指定长度的字符串内容
     * @param byteCount 复制的字节长度, 允许超过实际长度, 如果-1则返回完整的字符串内容
     */
    fun peekBytes(byteCount: Long = 1024 * 1024 * 4): ByteString {
        val peeked = body.source().peek()
        peeked.request(byteCount)
        val maxSize = if (byteCount < 0) peeked.buffer.size else minOf(byteCount, peeked.buffer.size)
        return peeked.readByteString(maxSize)
    }

    private fun Source.toProgress() = object : ForwardingSource(this) {
        var readByteCount: Long = 0

        @Throws(IOException::class)
        override fun read(sink: Buffer, byteCount: Long): Long {
            try {
                val bytesRead = super.read(sink, byteCount)
                readByteCount += if (bytesRead != -1L) bytesRead else 0
                if (progressListeners != null) {
                    val currentElapsedTime = SystemClock.elapsedRealtime()
                    progressListeners.forEach { progressListener ->
                        progressListener.intervalByteCount += if (bytesRead != -1L) bytesRead else 0
                        val currentInterval = currentElapsedTime - progressListener.elapsedTime
                        if (!progress.finish && (readByteCount == contentLength || bytesRead == -1L || currentInterval >= progressListener.interval)) {
                            if (readByteCount == contentLength || bytesRead == -1L) {
                                progress.finish = true
                            }
                            progressListener.onProgress(
                                progress.apply {
                                    currentByteCount = readByteCount
                                    totalByteCount = contentLength
                                    intervalByteCount = progressListener.intervalByteCount
                                    intervalTime = currentInterval
                                }
                            )
                            progressListener.elapsedTime = currentElapsedTime
                            progressListener.intervalByteCount = 0L
                        }
                    }
                }
                if (bytesRead == -1L) complete?.invoke()
                return bytesRead
            } catch (e: Exception) {
                complete?.invoke()
                throw e
            }
        }
    }
}