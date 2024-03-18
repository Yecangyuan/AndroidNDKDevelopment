package com.simley.lib_socket.aysnc.future;

public interface DoneCallback<T> {
    void done(Exception e, T result) throws Exception;
}
