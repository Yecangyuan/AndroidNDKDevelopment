package com.simley.lib_socket.aysnc.http.method;

import android.net.Uri;

import com.simley.lib_socket.aysnc.http.AsyncHttpRequest;

public class AsyncHttpPost extends AsyncHttpRequest {
    public static final String METHOD = "POST";
    
    public AsyncHttpPost(String uri) {
        this(Uri.parse(uri));
    }

    public AsyncHttpPost(Uri uri) {
        super(uri, METHOD);
    }
}
