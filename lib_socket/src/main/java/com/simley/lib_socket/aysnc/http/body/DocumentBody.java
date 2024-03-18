package com.simley.lib_socket.aysnc.http.body;


import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.DataSink;
import com.simley.lib_socket.aysnc.Util;
import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.future.FutureCallback;
import com.simley.lib_socket.aysnc.http.AsyncHttpRequest;
import com.simley.lib_socket.aysnc.parser.DocumentParser;

import org.w3c.dom.Document;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by koush on 8/30/13.
 */
public class DocumentBody implements AsyncHttpRequestBody<Document> {
    public DocumentBody() {
        this(null);
    }

    public DocumentBody(Document document) {
        this.document = document;
    }

    ByteArrayOutputStream bout;
    private void prepare() {
        if (bout != null)
            return;

        try {
            DOMSource source = new DOMSource(document);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            bout = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(bout, StandardCharsets.UTF_8);
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
            writer.flush();
        }
        catch (Exception ignored) {
        }
    }

    @Override
    public void write(AsyncHttpRequest request, DataSink sink, CompletedCallback completed) {
        prepare();
        byte[] bytes = bout.toByteArray();
        Util.writeAll(sink, bytes, completed);
    }

    @Override
    public void parse(DataEmitter emitter, final CompletedCallback completed) {
        new DocumentParser().parse(emitter).setCallback(new FutureCallback<Document>() {
            @Override
            public void onCompleted(Exception e, Document result) {
                document = result;
                completed.onCompleted(e);
            }
        });
    }

    public static final String CONTENT_TYPE = "application/xml";

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public boolean readFullyOnRequest() {
        return true;
    }

    @Override
    public int length() {
        prepare();
        return bout.size();
    }

    Document document;
    @Override
    public Document get() {
        return document;
    }
}
