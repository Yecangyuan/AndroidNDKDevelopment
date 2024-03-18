package com.simley.lib_socket.aysnc.parser;

import com.simley.lib_socket.aysnc.DataEmitter;
import com.simley.lib_socket.aysnc.DataSink;
import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.future.Future;

import org.json.JSONArray;

import java.lang.reflect.Type;

/**
 * JSON数组解析器
 */
public class JSONArrayParser implements AsyncParser<JSONArray> {
    @Override
    public Future<JSONArray> parse(DataEmitter emitter) {
        return new StringParser().parse(emitter)
        .thenConvert(JSONArray::new);
    }

    @Override
    public void write(DataSink sink, JSONArray value, CompletedCallback completed) {
        new StringParser().write(sink, value.toString(), completed);
    }

    @Override
    public Type getType() {
        return JSONArray.class;
    }

    @Override
    public String getMime() {
        return "application/json";
    }
}
