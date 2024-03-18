package com.simley.lib_socket.aysnc.stream;

import com.simley.lib_socket.aysnc.ByteBufferList;
import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.callback.DataCallback;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;


public class OutputStreamDataCallback implements DataCallback, CompletedCallback {
    private OutputStream mOutput;

    public OutputStreamDataCallback(OutputStream os) {
        mOutput = os;
    }

    public OutputStream getOutputStream() {
        return mOutput;
    }

    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        try {
            while (bb.size() > 0) {
                ByteBuffer b = bb.remove();
                mOutput.write(b.array(), b.arrayOffset() + b.position(), b.remaining());
                ByteBufferList.reclaim(b);
            }
        } catch (Exception ex) {
            onCompleted(ex);
        } finally {
            bb.recycle();
        }
    }

    public void close() {
        try {
            mOutput.close();
        } catch (IOException e) {
            onCompleted(e);
        }
    }

    @Override
    public void onCompleted(Exception error) {
        error.printStackTrace();
    }
}
