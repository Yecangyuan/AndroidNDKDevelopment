package com.simley.lib_socket.aysnc.callback;

import com.simley.lib_socket.aysnc.AsyncServerSocket;
import com.simley.lib_socket.aysnc.AsyncSocket;

public interface ListenCallback extends CompletedCallback {
    void onAccepted(AsyncSocket socket);
    void onListening(AsyncServerSocket socket);
}
