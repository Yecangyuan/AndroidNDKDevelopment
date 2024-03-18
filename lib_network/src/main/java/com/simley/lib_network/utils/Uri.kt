package com.simley.lib_network.utils

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.simley.lib_network.NetConfig
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

fun Uri.fileName(): String? {
    return DocumentFile.fromSingleUri(NetConfig.app, this)?.name
}

fun Uri.mediaType(): MediaType? {
    val fileName = DocumentFile.fromSingleUri(NetConfig.app, this)?.name
    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)?.toMediaTypeOrNull()
}

fun Uri.toRequestBody(): RequestBody {
    val document = DocumentFile.fromSingleUri(NetConfig.app, this)
    val length = document?.length() ?: 0
    return object : RequestBody() {
        override fun contentType(): MediaType? {
            return mediaType()
        }

        override fun contentLength() = length

        override fun writeTo(sink: BufferedSink) {
            NetConfig.app.contentResolver.openInputStream(this@toRequestBody)?.source()?.use {
                sink.writeAll(it)
            }
        }
    }
}