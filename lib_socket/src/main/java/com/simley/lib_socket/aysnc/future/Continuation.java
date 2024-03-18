package com.simley.lib_socket.aysnc.future;


import com.simley.lib_socket.aysnc.callback.CompletedCallback;
import com.simley.lib_socket.aysnc.callback.ContinuationCallback;

import java.util.LinkedList;

public class Continuation extends SimpleCancellable implements ContinuationCallback, Runnable, Cancellable {
    CompletedCallback callback;
    Runnable cancelCallback;
    
    public CompletedCallback getCallback() {
        return callback;
    }
    public void setCallback(CompletedCallback callback) {
        this.callback = callback;
    }
    
    public Runnable getCancelCallback() {
        return cancelCallback;
    }
    public void setCancelCallback(Runnable cancelCallback) {
        this.cancelCallback = cancelCallback;
    }
    public void setCancelCallback(final Cancellable cancel) {
        if (cancel == null) {
            this.cancelCallback = null;
            return;
        }
        this.cancelCallback = cancel::cancel;
    }
    
    public Continuation() {
        this(null);
    }
    public Continuation(CompletedCallback callback) {
        this(callback, null);
    }
    public Continuation(CompletedCallback callback, Runnable cancelCallback) {
        this.cancelCallback = cancelCallback;
        this.callback = callback;
    }
    
    private CompletedCallback wrap() {
        return new CompletedCallback() {
            boolean mThisCompleted;
            @Override
            public void onCompleted(Exception ex) {
                // onCompleted may be called more than once... buggy code.
                // only accept the first (timeouts, etc)
                if (mThisCompleted)
                    return;
                mThisCompleted = true;
                waiting = false;
                if (ex == null) {
                    next();
                    return;
                }
                reportCompleted(ex);
            }
        };
    }
    
    void reportCompleted(Exception ex) {
        if (!setComplete())
            return;
        if (callback != null)
            callback.onCompleted(ex);        
    }
    
    LinkedList<ContinuationCallback> mCallbacks = new LinkedList<>();
    
    private ContinuationCallback hook(ContinuationCallback callback) {
        if (callback instanceof DependentCancellable) {
            DependentCancellable child = (DependentCancellable)callback;
            child.setParent(this);
        }
        return callback;
    }
    
    public Continuation add(ContinuationCallback callback) {
        mCallbacks.add(hook(callback));
        return this;
    }
    
    public Continuation insert(ContinuationCallback callback) {
        mCallbacks.add(0, hook(callback));
        return this;
    }
   
    public Continuation add(final DependentFuture future) {
        future.setParent(this);
        add((continuation, next) -> {
            future.get();
            next.onCompleted(null);
        });
        return this;
    }
    
    private boolean inNext;
    private boolean waiting;
    private void next() {
        if (inNext)
            return;
        while (mCallbacks.size() > 0 && !waiting && !isDone() && !isCancelled()) {
            ContinuationCallback cb = mCallbacks.remove();
            try {
                inNext = true;
                waiting = true;
                cb.onContinue(this, wrap());
            }
            catch (Exception e) {
                reportCompleted(e);
            }
            finally {
                inNext = false;
            }
        }
        if (waiting)
            return;
        if (isDone())
            return;
        if (isCancelled())
            return;

        reportCompleted(null);
    }

    @Override
    public boolean cancel() {
        if (!super.cancel())
            return false;
        
        if (cancelCallback != null)
            cancelCallback.run();
        
        return true;
    }
    
    boolean started;
    public Continuation start() {
        if (started)
            throw new IllegalStateException("already started");
        started = true;
        next();
        return this;
    }

    @Override
    public void onContinue(Continuation continuation, CompletedCallback next) throws Exception {
        setCallback(next);
        start();
    }

    @Override
    public void run() {
        start();
    }
}
