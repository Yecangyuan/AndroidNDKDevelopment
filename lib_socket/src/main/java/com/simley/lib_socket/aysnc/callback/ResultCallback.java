package com.simley.lib_socket.aysnc.callback;

public interface ResultCallback<S, T> {
    void onCompleted(Exception e, S source, T result);
}
