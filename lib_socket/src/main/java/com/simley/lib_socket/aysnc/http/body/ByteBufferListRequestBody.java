package com.simley.lib_socket.aysnc.http.body;


import com.simley.lib_socket.aysnc.ByteBufferList;
import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.DataSink;
import com.simley.lib_socket.aysnc.Util;
import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.http.AsyncHttpRequest;
import com.simley.lib_socket.aysnc.parser.ByteBufferListParser;

public class ByteBufferListRequestBody implements AsyncHttpRequestBody<ByteBufferList> {
    public ByteBufferListRequestBody() {
    }

    ByteBufferList bb;
    public ByteBufferListRequestBody(ByteBufferList bb) {
        this.bb = bb;
    }
    @Override
    public void write(AsyncHttpRequest request, DataSink sink, CompletedCallback completed) {
        Util.writeAll(sink, bb, completed);
    }

    @Override
    public void parse(DataEmitter emitter, CompletedCallback completed) {
        new ByteBufferListParser().parse(emitter).setCallback((e, result) -> {
            bb = result;
            completed.onCompleted(e);
        });
    }

    public static String CONTENT_TYPE = "application/binary";

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        return bb.remaining();
    }

    @Override
    public ByteBufferList get() {
        return bb;
    }
}
