package com.simley.lib_socket.aysnc.callback;


import com.simley.lib_socket.aysnc.ByteBufferList;
import com.simley.lib_socket.aysnc.DataEmitter;

public interface DataCallback {
    class NullDataCallback implements DataCallback {
        @Override
        public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
            bb.recycle();
        }
    }

    void onDataAvailable(DataEmitter emitter, ByteBufferList bb);
}
