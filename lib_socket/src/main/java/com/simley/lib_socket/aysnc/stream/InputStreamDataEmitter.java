package com.simley.lib_socket.aysnc.stream;

import com.simley.lib_socket.aysnc.AsyncServer;
import com.simley.lib_socket.aysnc.ByteBufferList;
import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.Util;
import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.callback.DataCallback;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by koush on 5/22/13.
 */
public class InputStreamDataEmitter implements DataEmitter {
    AsyncServer server;
    InputStream inputStream;
    public InputStreamDataEmitter(AsyncServer server, InputStream inputStream) {
        this.server = server;
        this.inputStream = inputStream;
        doResume();
    }

    DataCallback callback;
    @Override
    public void setDataCallback(DataCallback callback) {
        this.callback = callback;
    }

    @Override
    public DataCallback getDataCallback() {
        return callback;
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    boolean paused;
    @Override
    public void pause() {
        paused = true;
    }

    @Override
    public void resume() {
        paused = false;
        doResume();
    }

    private void report(final Exception e) {
        getServer().post(() -> {
            Exception ex = e;
            try {
                inputStream.close();
            }
            catch (Exception e1) {
                ex = e1;
            }
            if (endCallback != null)
                endCallback.onCompleted(ex);
        });
    }

    int mToAlloc = 0;
    ByteBufferList pending = new ByteBufferList();
    Runnable pumper = new Runnable() {
        @Override
        public void run() {
            try {
                if (!pending.isEmpty()) {
                    getServer().run(() -> Util.emitAllData(InputStreamDataEmitter.this, pending));
                    if (!pending.isEmpty())
                        return;
                }
                ByteBuffer b;
                do {
                    b = ByteBufferList.obtain(Math.min(Math.max(mToAlloc, 2 << 11), 256 * 1024));
                    int read;
                    if (-1 == (read = inputStream.read(b.array()))) {
                        report(null);
                        return;
                    }
                    mToAlloc = read * 2;
                    b.limit(read);
                    pending.add(b);
                    getServer().run(() -> Util.emitAllData(InputStreamDataEmitter.this, pending));
                }
                while (pending.remaining() == 0 && !isPaused());
            }
            catch (Exception e) {
                report(e);
            }
        }
    };

    private void doResume() {
        new Thread(pumper).start();
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    CompletedCallback endCallback;
    @Override
    public void setEndCallback(CompletedCallback callback) {
        endCallback = callback;
    }

    @Override
    public CompletedCallback getEndCallback() {
        return endCallback;
    }

    @Override
    public AsyncServer getServer() {
        return server;
    }

    @Override
    public void close() {
        report(null);
        try {
            inputStream.close();
        }
        catch (Exception ignored) {
        }
    }

    @Override
    public String charset() {
        return null;
    }
}
