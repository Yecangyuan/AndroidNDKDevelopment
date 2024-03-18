package com.simley.lib_socket.aysnc.http.method;

import android.net.Uri;

import com.simley.lib_socket.aysnc.http.AsyncHttpRequest;

public class AsyncHttpGet extends AsyncHttpRequest {
    public static final String METHOD = "GET";
    
    public AsyncHttpGet(String uri) {
        super(Uri.parse(uri), METHOD);
    }

    public AsyncHttpGet(Uri uri) {
        super(uri, METHOD);
    }
}
