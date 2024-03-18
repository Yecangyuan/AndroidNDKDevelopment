package com.simley.lib_socket.aysnc;


public interface AsyncSocket extends DataEmitter, DataSink {
    AsyncServer getServer();
}
