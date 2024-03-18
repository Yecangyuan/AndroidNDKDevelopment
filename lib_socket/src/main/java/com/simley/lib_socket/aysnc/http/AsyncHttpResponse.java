package com.simley.lib_socket.aysnc.http;


import com.simley.lib_socket.aysnc.AsyncSocket;
import com.simley.lib_socket.aysnc.DataEmitter;

public interface AsyncHttpResponse extends DataEmitter {
    String protocol();
    String message();
    int code();
    Headers headers();
    AsyncSocket detachSocket();
    AsyncHttpRequest getRequest();
}
