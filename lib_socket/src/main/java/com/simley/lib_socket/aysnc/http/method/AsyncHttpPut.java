package com.simley.lib_socket.aysnc.http.method;

import android.net.Uri;

import com.simley.lib_socket.aysnc.http.AsyncHttpRequest;

public class AsyncHttpPut extends AsyncHttpRequest {
    public static final String METHOD = "PUT";
    
    public AsyncHttpPut(String uri) {
        this(Uri.parse(uri));
    }

    public AsyncHttpPut(Uri uri) {
        super(uri, METHOD);
    }
}
