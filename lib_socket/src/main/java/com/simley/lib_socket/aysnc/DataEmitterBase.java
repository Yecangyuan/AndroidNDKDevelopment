package com.simley.lib_socket.aysnc;


import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.callback.DataCallback;

/**
 * Created by koush on 5/27/13.
 */
public abstract class DataEmitterBase implements DataEmitter {
    private boolean ended;
    protected void report(Exception e) {
        if (ended)
            return;
        ended = true;
        if (getEndCallback() != null)
            getEndCallback().onCompleted(e);
    }

    @Override
    public final void setEndCallback(CompletedCallback callback) {
        endCallback = callback;
    }

    CompletedCallback endCallback;
    @Override
    public final CompletedCallback getEndCallback() {
        return endCallback;
    }


    DataCallback mDataCallback;
    @Override
    public void setDataCallback(DataCallback callback) {
        mDataCallback = callback;
    }

    @Override
    public DataCallback getDataCallback() {
        return mDataCallback;
    }

    @Override
    public String charset() {
        return null;
    }
}
