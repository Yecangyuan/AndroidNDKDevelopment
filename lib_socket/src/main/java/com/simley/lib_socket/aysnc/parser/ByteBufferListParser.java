package com.simley.lib_socket.aysnc.parser;


import com.simley.lib_socket.aysnc.ByteBufferList;
import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.DataSink;
import com.simley.lib_socket.aysnc.Util;
import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.future.Future;
import com.simley.lib_socket.aysnc.future.SimpleFuture;

import java.lang.reflect.Type;

/**
 * ByteBufferList解析器
 */
public class ByteBufferListParser implements AsyncParser<ByteBufferList> {
    @Override
    public Future<ByteBufferList> parse(final DataEmitter emitter) {
        final ByteBufferList bb = new ByteBufferList();
        final SimpleFuture<ByteBufferList> ret = new SimpleFuture<ByteBufferList>() {
            @Override
            protected void cancelCleanup() {
                emitter.close();
            }
        };
        emitter.setDataCallback((emitter1, data) -> data.get(bb));

        emitter.setEndCallback(ex -> {
            if (ex != null) {
                ret.setComplete(ex);
                return;
            }

            try {
                ret.setComplete(bb);
            }
            catch (Exception e) {
                ret.setComplete(e);
            }
        });

        return ret;
    }

    @Override
    public void write(DataSink sink, ByteBufferList value, CompletedCallback completed) {
        Util.writeAll(sink, value, completed);
    }

    @Override
    public Type getType() {
        return ByteBufferList.class;
    }

    @Override
    public String getMime() {
        return null;
    }
}
