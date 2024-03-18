package com.simley.lib_socket.aysnc.http.server;


public interface HttpServerRequestCallback {
    void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response);
}
