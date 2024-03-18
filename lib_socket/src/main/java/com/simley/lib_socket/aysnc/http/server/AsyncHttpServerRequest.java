package com.simley.lib_socket.aysnc.http.server;

import com.simley.lib_socket.aysnc.AsyncSocket;
import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.http.Headers;
import com.simley.lib_socket.aysnc.http.Multimap;
import com.simley.lib_socket.aysnc.http.body.AsyncHttpRequestBody;

import java.util.Map;
import java.util.regex.Matcher;

public interface AsyncHttpServerRequest extends DataEmitter {
    Headers getHeaders();
    Matcher getMatcher();
    void setMatcher(Matcher matcher);
    <T extends AsyncHttpRequestBody> T getBody();
    AsyncSocket getSocket();
    String getPath();
    Multimap getQuery();
    String getMethod();
    String getUrl();

    String get(String name);
    Map<String, Object> getState();
}
