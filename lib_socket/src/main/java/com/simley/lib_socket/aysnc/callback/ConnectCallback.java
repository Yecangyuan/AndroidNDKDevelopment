package com.simley.lib_socket.aysnc.callback;


import com.simley.lib_socket.aysnc.AsyncSocket;

public interface ConnectCallback {
    void onConnectCompleted(Exception ex, AsyncSocket socket);
}
