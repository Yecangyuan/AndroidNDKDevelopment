package com.simley.lib_socket.aysnc.http;

import android.text.TextUtils;

import com.simley.lib_socket.aysnc.AsyncSocket;
import com.simley.lib_socket.aysnc.BufferedDataSink;
import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.DataSink;
import com.simley.lib_socket.aysnc.LineEmitter;
import com.simley.lib_socket.aysnc.Util;
import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.http.body.AsyncHttpRequestBody;
import com.simley.lib_socket.aysnc.http.filter.ChunkedOutputFilter;

import java.io.IOException;

/**
 * Created by koush on 7/24/14.
 */
public class HttpTransportMiddleware extends SimpleMiddleware {
    @Override
    public boolean exchangeHeaders(final OnExchangeHeaderData data) {
        Protocol p = Protocol.get(data.protocol);
        if (p != null && p != Protocol.HTTP_1_0 && p != Protocol.HTTP_1_1)
            return super.exchangeHeaders(data);

        AsyncHttpRequest request = data.request;
        AsyncHttpRequestBody requestBody = data.request.getBody();

        if (requestBody != null) {
            if (requestBody.length() >= 0) {
                request.getHeaders().set("Content-Length", String.valueOf(requestBody.length()));
                data.response.sink(data.socket);
            }
            else if ("close".equals(request.getHeaders().get("Connection"))) {
                data.response.sink(data.socket);
            }
            else {
                request.getHeaders().set("Transfer-Encoding", "Chunked");
                data.response.sink(new ChunkedOutputFilter(data.socket));
            }
        }

        String rl = request.getRequestLine().toString();
        String rs = request.getHeaders().toPrefixString(rl);

        byte[] rsBytes = rs.getBytes();

        // try to get the request body in the same packet as the request headers... if it will fit
        // in the max MTU (1540 or whatever).
        final boolean waitForBody = requestBody != null && requestBody.length() >= 0 && requestBody.length() + rsBytes.length < 1024;
        final BufferedDataSink bsink;
        final DataSink headerSink;
        if (waitForBody) {
            // force buffering of headers
            bsink = new BufferedDataSink(data.response.sink());
            bsink.forceBuffering(true);
            data.response.sink(bsink);
            headerSink = bsink;
        }
        else {
            bsink = null;
            headerSink = data.socket;
        }

        request.logv("\n" + rs);

        final CompletedCallback sentCallback = data.sendHeadersCallback;
        Util.writeAll(headerSink, rsBytes, new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                Util.end(sentCallback, ex);
                // flush headers and any request body that was written by the callback
                if (bsink != null) {
                    bsink.forceBuffering(false);
                    bsink.setMaxBuffer(0);
                }
            }
        });

        LineEmitter.StringCallback headerCallback = new LineEmitter.StringCallback() {
            Headers mRawHeaders = new Headers();
            String statusLine;

            @Override
            public void onStringAvailable(String s) {
                try {
                    s = s.trim();
                    if (statusLine == null) {
                        statusLine = s;
                    }
                    else if (!TextUtils.isEmpty(s)) {
                        mRawHeaders.addLine(s);
                    }
                    else {
                        String[] parts = statusLine.split(" ", 3);
                        if (parts.length < 2)
                            throw new Exception(new IOException("Not HTTP"));

                        data.response.headers(mRawHeaders);
                        String protocol = parts[0];
                        data.response.protocol(protocol);
                        data.response.code(Integer.parseInt(parts[1]));
                        data.response.message(parts.length == 3 ? parts[2] : "");
                        data.receiveHeadersCallback.onCompleted(null);

                        // socket may get detached after headers (websocket)
                        AsyncSocket socket = data.response.socket();
                        if (socket == null)
                            return;
                        DataEmitter emitter;
                        // HEAD requests must not return any data. They still may
                        // return content length, etc, which will confuse the body decoder
                        if (!data.request.hasBody()) {
                            emitter = HttpUtil.EndEmitter.create(socket.getServer(), null);
                        }
                        else if (responseIsEmpty(data.response.code())) {
                            emitter = HttpUtil.EndEmitter.create(socket.getServer(), null);
                        }
                        else {
                            emitter = HttpUtil.getBodyDecoder(socket, Protocol.get(protocol), mRawHeaders, false);
                        }
                        data.response.emitter(emitter);
                    }
                }
                catch (Exception ex) {
                    data.receiveHeadersCallback.onCompleted(ex);
                }
            }
        };

        LineEmitter liner = new LineEmitter();
        data.socket.setDataCallback(liner);
        liner.setLineCallback(headerCallback);
        return true;
    }

    static boolean responseIsEmpty(int code) {
        return (code >= 100 && code <= 199) || code == 204 || code == 304;
    }

    @Override
    public void onRequestSent(OnRequestSentData data) {
        Protocol p = Protocol.get(data.protocol);
        if (p != null && p != Protocol.HTTP_1_0 && p != Protocol.HTTP_1_1)
            return;

        if (data.response.sink() instanceof ChunkedOutputFilter)
            data.response.sink().end();
    }
}
