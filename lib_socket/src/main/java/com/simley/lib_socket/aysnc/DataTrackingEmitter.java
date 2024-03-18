package com.simley.lib_socket.aysnc;

/**
 * Created by koush on 5/28/13.
 */
public interface DataTrackingEmitter extends DataEmitter {
    interface DataTracker {
        void onData(int totalBytesRead);
    }
    void setDataTracker(DataTracker tracker);
    DataTracker getDataTracker();
    int getBytesRead();
    void setDataEmitter(DataEmitter emitter);
}
