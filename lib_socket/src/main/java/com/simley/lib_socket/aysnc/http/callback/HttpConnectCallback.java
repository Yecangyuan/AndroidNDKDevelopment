package com.simley.lib_socket.aysnc.http.callback;


import com.simley.lib_socket.aysnc.http.AsyncHttpResponse;

public interface HttpConnectCallback {
    public void onConnectCompleted(Exception ex, AsyncHttpResponse response);
}
