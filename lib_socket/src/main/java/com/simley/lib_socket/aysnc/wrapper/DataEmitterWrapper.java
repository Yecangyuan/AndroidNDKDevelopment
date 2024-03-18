package com.simley.lib_socket.aysnc.wrapper;

import com.simley.lib_socket.aysnc.DataEmitter;

public interface DataEmitterWrapper extends DataEmitter {
    DataEmitter getDataEmitter();
}
