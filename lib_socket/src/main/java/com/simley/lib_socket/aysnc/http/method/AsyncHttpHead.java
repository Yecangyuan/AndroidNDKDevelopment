package com.simley.lib_socket.aysnc.http.method;

import android.net.Uri;

import com.simley.lib_socket.aysnc.http.AsyncHttpRequest;

/**
 * Created by koush on 8/25/13.
 */
public class AsyncHttpHead extends AsyncHttpRequest {
    public AsyncHttpHead(Uri uri) {
        super(uri, METHOD);
    }

    @Override
    public boolean hasBody() {
        return false;
    }

    public static final String METHOD = "HEAD";
}
