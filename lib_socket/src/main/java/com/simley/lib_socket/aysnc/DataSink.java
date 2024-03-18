package com.simley.lib_socket.aysnc;

import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.callback.WritableCallback;

public interface DataSink {
    public void write(ByteBufferList bb);
    public void setWriteableCallback(WritableCallback handler);
    public WritableCallback getWriteableCallback();
    
    public boolean isOpen();
    public void end();
    public void setClosedCallback(CompletedCallback handler);
    public CompletedCallback getClosedCallback();
    public AsyncServer getServer();
}
