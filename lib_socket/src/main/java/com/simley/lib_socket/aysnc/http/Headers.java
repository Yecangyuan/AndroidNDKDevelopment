package com.simley.lib_socket.aysnc.http;


import android.text.TextUtils;

import com.simley.lib_socket.aysnc.util.TaggedList;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by koush on 7/21/14.
 */
public class Headers {
    public Headers() {
    }

    public Headers(Map<String, List<String>> mm) {
        for (String key : mm.keySet()) {
            addAll(key, mm.get(key));
        }
    }

    final Multimap map = new Multimap() {
        @Override
        protected List<String> newList() {
            return new TaggedList<>();
        }
    };

    public Multimap getMultiMap() {
        return map;
    }

    public List<String> getAll(String header) {
        return map.get(header.toLowerCase(Locale.US));
    }

    public String get(String header) {
        return map.getString(header.toLowerCase(Locale.US));
    }

    public Headers set(String header, String value) {
        if (value != null && (value.contains("\n") || value.contains("\r")))
            throw new IllegalArgumentException("value must not contain a new line or line feed");
        String lc = header.toLowerCase(Locale.US);
        map.put(lc, value);
        TaggedList<String> list = (TaggedList<String>) map.get(lc);
        list.tagNull(header);
        return this;
    }

    public Headers add(String header, String value) {
        String lc = header.toLowerCase(Locale.US);
        map.add(lc, value);
        TaggedList<String> list = (TaggedList<String>) map.get(lc);
        list.tagNull(header);
        return this;
    }

    public Headers addLine(String line) {
        if (line != null) {
            line = line.trim();
            String[] parts = line.split(":", 2);
            if (parts.length == 2)
                add(parts[0].trim(), parts[1].trim());
            else
                add(parts[0].trim(), "");
        }
        return this;
    }

    public Headers addAll(String header, List<String> values) {
        for (String v : values) {
            add(header, v);
        }
        return this;
    }

    public Headers addAll(Map<String, List<String>> m) {
        for (String key : m.keySet()) {
            for (String value : m.get(key)) {
                add(key, value);
            }
        }
        return this;
    }

    public Headers addAllMap(Map<String, String> m) {
        for (String key : m.keySet()) {
            add(key, m.get(key));
        }
        return this;
    }

    public Headers addAll(Headers headers) {
        // safe to addall since this is another Headers object
        map.putAll(headers.map);
        return this;
    }

    public List<String> removeAll(String header) {
        return map.remove(header.toLowerCase(Locale.US));
    }

    public String remove(String header) {
        List<String> r = removeAll(header.toLowerCase(Locale.US));
        if (r == null || r.size() == 0)
            return null;
        return r.get(0);
    }

    public Headers removeAll(Collection<String> headers) {
        for (String header : headers) {
            remove(header);
        }
        return this;
    }

    public StringBuilder toStringBuilder() {
        StringBuilder result = new StringBuilder(256);
        for (String key : map.keySet()) {
            TaggedList<String> list = (TaggedList<String>) map.get(key);
            for (String v : list) {
                result.append((String) list.tag())
                        .append(": ")
                        .append(v)
                        .append("\r\n");
            }
        }
        result.append("\r\n");
        return result;
    }

    @Override
    public String toString() {
        return toStringBuilder().toString();
    }

    public String toPrefixString(String prefix) {
        return
                toStringBuilder()
                        .insert(0, prefix + "\r\n")
                        .toString();
    }

    public static Headers parse(String payload) {
        String[] lines = payload.split("\n");

        Headers headers = new Headers();
        for (String line : lines) {
            line = line.trim();
            if (TextUtils.isEmpty(line))
                continue;

            headers.addLine(line);
        }
        return headers;
    }
}
