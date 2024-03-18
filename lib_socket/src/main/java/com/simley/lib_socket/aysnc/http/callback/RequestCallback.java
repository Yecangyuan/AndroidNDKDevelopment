package com.simley.lib_socket.aysnc.http.callback;

import com.simley.lib_socket.aysnc.callback.ResultCallback;
import com.simley.lib_socket.aysnc.http.AsyncHttpResponse;

public interface RequestCallback<T> extends ResultCallback<AsyncHttpResponse, T> {
    public void onConnect(AsyncHttpResponse response);
    public void onProgress(AsyncHttpResponse response, long downloaded, long total);
}
