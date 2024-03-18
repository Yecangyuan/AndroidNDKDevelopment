package com.simley.lib_socket.aysnc.http.server;

import com.simley.lib_socket.aysnc.http.Headers;
import com.simley.lib_socket.aysnc.http.body.AsyncHttpRequestBody;
public interface AsyncHttpRequestBodyProvider {
    AsyncHttpRequestBody getBody(Headers headers);
}
