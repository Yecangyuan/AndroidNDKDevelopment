package com.simley.lib_socket.aysnc.http.method;

import android.net.Uri;

import com.simley.lib_socket.aysnc.http.AsyncHttpRequest;

public class AsyncHttpDelete extends AsyncHttpRequest {
    public static final String METHOD = "DELETE";

    public AsyncHttpDelete(String uri) {
        this(Uri.parse(uri));
    }

    public AsyncHttpDelete(Uri uri) {
        super(uri, METHOD);
    }
}
