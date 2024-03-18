package com.simley.lib_socket.aysnc.http.body;

import android.text.TextUtils;

import com.simley.lib_socket.aysnc.ByteBufferList;
import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.DataSink;
import com.simley.lib_socket.aysnc.LineEmitter;
import com.simley.lib_socket.aysnc.Util;
import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.callback.ContinuationCallback;
import com.simley.lib_socket.aysnc.future.Continuation;
import com.simley.lib_socket.aysnc.http.AsyncHttpRequest;
import com.simley.lib_socket.aysnc.http.Headers;
import com.simley.lib_socket.aysnc.http.Multimap;
import com.simley.lib_socket.aysnc.http.server.BoundaryEmitter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MultipartFormDataBody extends BoundaryEmitter implements AsyncHttpRequestBody<Multimap> {
    LineEmitter liner;
    Headers formData;
    ByteBufferList lastData;
    Part lastPart;

    public interface MultipartCallback {
        public void onPart(Part part);
    }

    @Override
    public void parse(DataEmitter emitter, final CompletedCallback completed) {
        setDataEmitter(emitter);
        setEndCallback(completed);
    }

    void handleLast() {
        if (lastData == null)
            return;
        
        if (formData == null)
            formData = new Headers();

        String value = lastData.peekString();
        String name = TextUtils.isEmpty(lastPart.getName()) ? "unnamed" : lastPart.getName();
        StringPart part = new StringPart(name, value);
        part.mHeaders = lastPart.mHeaders;
        addPart(part);

        formData.add(name, value);

        lastPart = null;
        lastData = null;
    }
    
    public String getField(String name) {
        if (formData == null)
            return null;
        return formData.get(name);
    }
    
    @Override
    protected void onBoundaryEnd() {
        super.onBoundaryEnd();
        handleLast();
    }

    @Override
    protected void onBoundaryStart() {
        final Headers headers = new Headers();
        liner = new LineEmitter();
        liner.setLineCallback(s -> {
            if (!"\r".equals(s)){
                headers.addLine(s);
            }
            else {
                handleLast();

                liner = null;
                setDataCallback(null);
                Part part = new Part(headers);
                if (mCallback != null)
                    mCallback.onPart(part);
                if (getDataCallback() == null) {
//                        if (part.isFile()) {
//                            setDataCallback(new NullDataCallback());
//                            return;
//                        }

                    lastPart = part;
                    lastData = new ByteBufferList();
                    setDataCallback((emitter, bb) -> bb.get(lastData));
                }
            }
        });
        setDataCallback(liner);
    }

    public static final String PRIMARY_TYPE = "multipart/";
    public static final String CONTENT_TYPE = PRIMARY_TYPE + "form-data";
    String contentType = CONTENT_TYPE;
    public MultipartFormDataBody(String contentType) {
        Multimap map = Multimap.parseSemicolonDelimited(contentType);
        String boundary = map.getString("boundary");
        if (boundary == null)
            report(new Exception ("No boundary found for multipart/form-data"));
        else
            setBoundary(boundary);
    }

    MultipartCallback mCallback;
    public void setMultipartCallback(MultipartCallback callback) {
        mCallback = callback;
    }
    
    public MultipartCallback getMultipartCallback() {
        return mCallback;
    }

    int written;
    @Override
    public void write(AsyncHttpRequest request, final DataSink sink, final CompletedCallback completed) {
        if (mParts == null)
            return;

        Continuation c = new Continuation(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                completed.onCompleted(ex);
//                if (ex == null)
//                    sink.end();
//                else
//                    sink.close();
            }
        });

        for (final Part part: mParts) {
            c.add(new ContinuationCallback() {
                @Override
                public void onContinue(Continuation continuation, CompletedCallback next) throws Exception {
                    byte[] bytes = part.getRawHeaders().toPrefixString(getBoundaryStart()).getBytes();
                    Util.writeAll(sink, bytes, next);
                    written += bytes.length;
                }
            })
            .add(new ContinuationCallback() {
                @Override
                public void onContinue(Continuation continuation, CompletedCallback next) throws Exception {
                    long partLength = part.length();
                    if (partLength >= 0)
                        written += partLength;
                    part.write(sink, next);
                }
            })
            .add(new ContinuationCallback() {
                @Override
                public void onContinue(Continuation continuation, CompletedCallback next) throws Exception {
                    byte[] bytes = "\r\n".getBytes();
                    Util.writeAll(sink, bytes, next);
                    written += bytes.length;
                }
            });
        }
        c.add(new ContinuationCallback() {
            @Override
            public void onContinue(Continuation continuation, CompletedCallback next) throws Exception {
                byte[] bytes = (getBoundaryEnd()).getBytes();
                Util.writeAll(sink, bytes, next);
                written += bytes.length;
            }
        });
        c.start();
    }

    @Override
    public String getContentType() {
        if (getBoundary() == null) {
            setBoundary("----------------------------" + UUID.randomUUID().toString().replace("-", ""));
        }
        return contentType + "; boundary=" + getBoundary();
    }

    @Override
    public boolean readFullyOnRequest() {
        return false;
    }

    int totalToWrite;
    @Override
    public int length() {
        if (getBoundary() == null) {
            setBoundary("----------------------------" + UUID.randomUUID().toString().replace("-", ""));
        }

        int length = 0;
        for (final Part part: mParts) {
            String partHeader = part.getRawHeaders().toPrefixString(getBoundaryStart());
            if (part.length() == -1)
                return -1;
            length += part.length() + partHeader.getBytes().length + "\r\n".length();
        }
        length += (getBoundaryEnd()).getBytes().length;
        return totalToWrite = length;
    }
    
    public MultipartFormDataBody() {
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public List<Part> getParts() {
        if (mParts == null)
            return null;
        return new ArrayList<>(mParts);
    }

    public void addFilePart(String name, File file) {
        addPart(new FilePart(name, file));
    }
    
    public void addStringPart(String name, String value) {
        addPart(new StringPart(name, value));
    }
    
    private ArrayList<Part> mParts;
    public void addPart(Part part) {
        if (mParts == null)
            mParts = new ArrayList<Part>();
        mParts.add(part);
    }

    @Override
    public Multimap get() {
        return new Multimap(formData.getMultiMap());
    }

    @Override
    public String toString() {
        for (Part part: getParts()) {
            return part.toString();
        }
        return "multipart content is empty";
    }
}
