package com.simley.lib_socket.aysnc.wrapper;


import com.simley.lib_socket.aysnc.AsyncSocket;

public interface AsyncSocketWrapper extends AsyncSocket, DataEmitterWrapper {
    AsyncSocket getSocket();
}
