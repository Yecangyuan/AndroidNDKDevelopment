package com.simley.lib_socket.aysnc.util;

import java.util.Hashtable;

public class UntypedHashtable {
    private final Hashtable<String, Object> hash = new Hashtable<String, Object>();

    public void put(String key, Object value) {
        hash.put(key, value);
    }

    public void remove(String key) {
        hash.remove(key);
    }

    public <T> T get(String key, T defaultValue) {
        T ret = get(key);
        if (ret == null)
            return defaultValue;
        return ret;
    }

    public <T> T get(String key) {
        return (T) hash.get(key);
    }
}