package com.simley.lib_socket.aysnc.http.server;

import android.text.TextUtils;

import com.simley.lib_socket.aysnc.AsyncServer;
import com.simley.lib_socket.aysnc.AsyncSocket;
import com.simley.lib_socket.aysnc.ByteBufferList;
import com.simley.lib_socket.aysnc.DataSink;
import com.simley.lib_socket.aysnc.Util;
import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.callback.DataCallback;
import com.simley.lib_socket.aysnc.callback.WritableCallback;
import com.simley.lib_socket.aysnc.http.method.AsyncHttpHead;
import com.simley.lib_socket.aysnc.http.AsyncHttpResponse;
import com.simley.lib_socket.aysnc.http.Headers;
import com.simley.lib_socket.aysnc.http.HttpUtil;
import com.simley.lib_socket.aysnc.http.Protocol;
import com.simley.lib_socket.aysnc.http.filter.ChunkedOutputFilter;
import com.simley.lib_socket.aysnc.parser.AsyncParser;
import com.simley.lib_socket.aysnc.util.StreamUtility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Locale;

public class AsyncHttpServerResponseImpl implements AsyncHttpServerResponse {
    private final Headers mRawHeaders = new Headers();
    private long mContentLength = -1;

    @Override
    public Headers getHeaders() {
        return mRawHeaders;
    }

    public AsyncSocket getSocket() {
        return mSocket;
    }

    @Override
    public void setSocket(AsyncSocket socket) {
        mSocket = socket;
    }

    AsyncSocket mSocket;
    AsyncHttpServerRequestImpl mRequest;

    AsyncHttpServerResponseImpl(AsyncSocket socket, AsyncHttpServerRequestImpl req) {
        mSocket = socket;
        mRequest = req;
        if (HttpUtil.isKeepAlive(Protocol.HTTP_1_1, req.getHeaders()))
            mRawHeaders.set("Connection", "Keep-Alive");
    }

    @Override
    public AsyncHttpServerRequest getRequest() {
        return mRequest;
    }

    @Override
    public void write(ByteBufferList bb) {
        // order is important here...
        // do the header write... this will call onWritable, which may be reentrant
        if (!headWritten)
            initFirstWrite();

        // now check to see if the list is empty. reentrancy may cause it to empty itself.
        if (bb.remaining() == 0)
            return;

        // null sink means that the header has not finished writing
        if (mSink == null)
            return;

        // can successfully write!
        mSink.write(bb);
    }

    boolean headWritten = false;
    DataSink mSink;

    void initFirstWrite() {
        if (headWritten)
            return;

        headWritten = true;

        final boolean isChunked;
        String currentEncoding = mRawHeaders.get("Transfer-Encoding");
        if ("".equals(currentEncoding))
            mRawHeaders.removeAll("Transfer-Encoding");
        boolean canUseChunked = ("Chunked".equalsIgnoreCase(currentEncoding) || currentEncoding == null)
                && !"close".equalsIgnoreCase(mRawHeaders.get("Connection"));
        if (mContentLength < 0) {
            String contentLength = mRawHeaders.get("Content-Length");
            if (!TextUtils.isEmpty(contentLength))
                mContentLength = Long.parseLong(contentLength);
        }
        if (mContentLength < 0 && canUseChunked) {
            mRawHeaders.set("Transfer-Encoding", "Chunked");
            isChunked = true;
        } else {
            isChunked = false;
        }

        String statusLine = String.format(Locale.ENGLISH, "%s %s %s", httpVersion, code, AsyncHttpServer.getResponseCodeDescription(code));
        String rh = mRawHeaders.toPrefixString(statusLine);

        Util.writeAll(mSocket, rh.getBytes(), ex -> {
            if (ex != null) {
                report(ex);
                return;
            }
            if (isChunked) {
                ChunkedOutputFilter chunked = new ChunkedOutputFilter(mSocket);
                chunked.setMaxBuffer(0);
                mSink = chunked;
            } else {
                mSink = mSocket;
            }

            mSink.setClosedCallback(closedCallback);
            closedCallback = null;
            mSink.setWriteableCallback(writable);
            writable = null;
            if (ended) {
                // the response ended while headers were written
                end();
                return;
            }
            getServer().post(() -> {
                WritableCallback wb = getWriteableCallback();
                if (wb != null)
                    wb.onWriteable();
            });
        });
    }

    WritableCallback writable;

    @Override
    public void setWriteableCallback(WritableCallback handler) {
        if (mSink != null)
            mSink.setWriteableCallback(handler);
        else
            writable = handler;
    }

    @Override
    public WritableCallback getWriteableCallback() {
        if (mSink != null)
            return mSink.getWriteableCallback();
        return writable;
    }

    boolean ended;

    @Override
    public void end() {
        if (ended)
            return;
        ended = true;
        if (headWritten && mSink == null) {
            // header is in the process of being written... bail out.
            // end will be called again after finished.
            return;
        }
        if (!headWritten) {
            // end was called, and no head or body was yet written,
            // so strip the transfer encoding as that is superfluous.
            mRawHeaders.remove("Transfer-Encoding");
        }
        if (mSink instanceof ChunkedOutputFilter) {
            // this filter won't close the socket underneath.
            mSink.end();
        } else if (!headWritten) {
            if (!mRequest.getMethod().equalsIgnoreCase(AsyncHttpHead.METHOD))
                send("text/html", "");
            else {
                writeHead();
                onEnd();
            }
        } else {
            onEnd();
        }
    }

    @Override
    public void writeHead() {
        initFirstWrite();
    }

    @Override
    public void setContentType(String contentType) {
        mRawHeaders.set("Content-Type", contentType);
    }

    @Override
    public void send(final String contentType, final byte[] bytes) {
        send(contentType, new ByteBufferList(bytes));
    }

    @Override
    public <T> void sendBody(AsyncParser<T> body, T value) {
        mRawHeaders.set("Content-Type", body.getMime());
        body.write(this, value, ex -> end());
    }

    @Override
    public void send(String contentType, ByteBuffer bb) {
        send(contentType, new ByteBufferList(bb));
    }

    @Override
    public void send(String contentType, ByteBufferList bb) {
        getServer().post(() -> {
            mContentLength = bb.remaining();
            mRawHeaders.set("Content-Length", Long.toString(mContentLength));
            if (contentType != null)
                mRawHeaders.set("Content-Type", contentType);

            Util.writeAll(AsyncHttpServerResponseImpl.this, bb, ex -> onEnd());
        });
    }

    @Override
    public void send(String contentType, final String string) {
        try {
            send(contentType, string.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    boolean mEnded;

    protected void onEnd() {
        mEnded = true;
    }

    protected void report(Exception e) {
    }


    @Override
    public void send(String string) {
        String contentType = mRawHeaders.get("Content-Type");
        if (contentType == null)
            contentType = "text/html; charset=utf-8";
        send(contentType, string);
    }

    @Override
    public void send(JSONObject json) {
        send("application/json; charset=utf-8", json.toString());
    }

    @Override
    public void send(JSONArray jsonArray) {
        send("application/json; charset=utf-8", jsonArray.toString());
    }

    @Override
    public void sendStream(final InputStream inputStream, long totalLength) {
        long start = 0;
        long end = totalLength - 1;

        String range = mRequest.getHeaders().get("Range");
        if (range != null) {
            String[] parts = range.split("=");
            if (parts.length != 2 || !"bytes".equals(parts[0])) {
                // Requested range not satisfiable
                code(416);
                end();
                return;
            }

            parts = parts[1].split("-");
            try {
                if (parts.length > 2)
                    throw new MalformedRangeException();
                if (!TextUtils.isEmpty(parts[0]))
                    start = Long.parseLong(parts[0]);
                if (parts.length == 2 && !TextUtils.isEmpty(parts[1]))
                    end = Long.parseLong(parts[1]);
                else
                    end = totalLength - 1;

                code(206);
                getHeaders().set("Content-Range", String.format(Locale.ENGLISH, "bytes %d-%d/%d", start, end, totalLength));
            } catch (Exception e) {
                code(416);
                end();
                return;
            }
        }
        try {
            if (start != inputStream.skip(start))
                throw new StreamSkipException("skip failed to skip requested amount");
            mContentLength = end - start + 1;
            mRawHeaders.set("Content-Length", String.valueOf(mContentLength));
            mRawHeaders.set("Accept-Ranges", "bytes");
            if (mRequest.getMethod().equals(AsyncHttpHead.METHOD)) {
                writeHead();
                onEnd();
                return;
            }
            if (mContentLength == 0) {
                writeHead();
                StreamUtility.closeQuietly(inputStream);
                onEnd();
                return;
            }
            getServer().post(() ->
                    Util.pump(inputStream, mContentLength, AsyncHttpServerResponseImpl.this,
                            ex -> {
                                StreamUtility.closeQuietly(inputStream);
                                onEnd();
                            }));
        } catch (Exception e) {
            code(500);
            end();
        }
    }

    @Override
    public void sendFile(File file) {
        try {
            if (mRawHeaders.get("Content-Type") == null)
                mRawHeaders.set("Content-Type", AsyncHttpServer.getContentType(file.getAbsolutePath()));
            FileInputStream fin = new FileInputStream(file);
            sendStream(new BufferedInputStream(fin, 64000), file.length());
        } catch (FileNotFoundException e) {
            code(404);
            end();
        }
    }

    @Override
    public void proxy(final AsyncHttpResponse remoteResponse) {
        code(remoteResponse.code());
        remoteResponse.headers().removeAll("Transfer-Encoding");
        remoteResponse.headers().removeAll("Content-Encoding");
        remoteResponse.headers().removeAll("Connection");
        getHeaders().addAll(remoteResponse.headers());
        // TODO: remove?
        remoteResponse.headers().set("Connection", "close");
        Util.pump(remoteResponse, this, ex -> {
            remoteResponse.setEndCallback(new NullCompletedCallback());
            remoteResponse.setDataCallback(new DataCallback.NullDataCallback());
            end();
        });
    }

    int code = 200;

    @Override
    public AsyncHttpServerResponse code(int code) {
        this.code = code;
        return this;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public void redirect(String location) {
        code(302);
        mRawHeaders.set("Location", location);
        end();
    }

    String httpVersion = "HTTP/1.1";

    @Override
    public String getHttpVersion() {
        return httpVersion;
    }

    @Override
    public void setHttpVersion(String httpVersion) {
        this.httpVersion = httpVersion;
    }

    @Override
    public void onCompleted(Exception ex) {
        end();
    }

    @Override
    public boolean isOpen() {
        if (mSink != null)
            return mSink.isOpen();
        return mSocket.isOpen();
    }

    CompletedCallback closedCallback;

    @Override
    public void setClosedCallback(CompletedCallback handler) {
        if (mSink != null)
            mSink.setClosedCallback(handler);
        else
            closedCallback = handler;
    }

    @Override
    public CompletedCallback getClosedCallback() {
        if (mSink != null)
            return mSink.getClosedCallback();
        return closedCallback;
    }

    @Override
    public AsyncServer getServer() {
        return mSocket.getServer();
    }

    @Override
    public String toString() {
        if (mRawHeaders == null)
            return super.toString();
        String statusLine = String.format(Locale.ENGLISH, "%s %s %s", httpVersion, code, AsyncHttpServer.getResponseCodeDescription(code));
        return mRawHeaders.toPrefixString(statusLine);
    }
}
