package com.simley.lib_socket.aysnc.parser;


import com.simley.lib_socket.aysnc.ByteBufferList;
import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.DataSink;
import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.future.Future;

import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * 字符串解析器
 */
public class StringParser implements AsyncParser<String> {

    Charset forcedCharset;

    public StringParser() {
    }

    public StringParser(Charset charset) {
        this.forcedCharset = charset;
    }

    @Override
    public Future<String> parse(DataEmitter emitter) {
        final String charset = emitter.charset();
        return new ByteBufferListParser().parse(emitter)
                .thenConvert(from -> {
                    Charset charsetToUse = forcedCharset;
                    if (charsetToUse == null && charset != null)
                        charsetToUse = Charset.forName(charset);
                    return from.readString(charsetToUse);
                });
    }

    @Override
    public void write(DataSink sink, String value, CompletedCallback completed) {
        new ByteBufferListParser().write(sink, new ByteBufferList(value.getBytes()), completed);
    }

    @Override
    public Type getType() {
        return String.class;
    }

    @Override
    public String getMime() {
        return null;
    }
}
