package com.simley.lib_socket.aysnc.callback;


import com.simley.lib_socket.aysnc.future.Continuation;

public interface ContinuationCallback {
    void onContinue(Continuation continuation, CompletedCallback next) throws Exception;
}
