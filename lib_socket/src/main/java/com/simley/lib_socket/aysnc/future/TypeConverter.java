package com.simley.lib_socket.aysnc.future;

public interface TypeConverter<T, F> {
    Future<T> convert(F from, String fromMime) throws Exception;
}
