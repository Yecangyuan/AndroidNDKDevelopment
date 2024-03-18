package com.simley.lib_socket.aysnc;


import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.callback.DataCallback;

public interface DataEmitter {
    void setDataCallback(DataCallback callback);
    DataCallback getDataCallback();
    boolean isChunked();
    void pause();
    void resume();
    void close();
    boolean isPaused();
    void setEndCallback(CompletedCallback callback);
    CompletedCallback getEndCallback();
    AsyncServer getServer();
    String charset();
}
