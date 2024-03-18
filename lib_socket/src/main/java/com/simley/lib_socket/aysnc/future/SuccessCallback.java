package com.simley.lib_socket.aysnc.future;

public interface SuccessCallback<T> {
    void success(T value) throws Exception;
}
