package com.simley.lib_socket.aysnc;


import com.simley.lib_socket.aysnc.callback.DataCallback;

public class DataEmitterReader implements DataCallback {
    DataCallback mPendingRead;
    int mPendingReadLength;
    ByteBufferList mPendingData = new ByteBufferList();

    public void read(int count, DataCallback callback) {
        mPendingReadLength = count;
        mPendingRead = callback;
        mPendingData.recycle();
    }

    private boolean handlePendingData(DataEmitter emitter) {
        if (mPendingReadLength > mPendingData.remaining())
            return false;

        DataCallback pendingRead = mPendingRead;
        mPendingRead = null;
        pendingRead.onDataAvailable(emitter, mPendingData);

        return true;
    }

    public DataEmitterReader() {
    }
    @Override
    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
        // if we're registered for data, we must be waiting for a read
        do {
            int need = Math.min(bb.remaining(), mPendingReadLength - mPendingData.remaining());
            bb.get(mPendingData, need);
            bb.remaining();
        }
        while (handlePendingData(emitter) && mPendingRead != null);
        bb.remaining();
    }
}
