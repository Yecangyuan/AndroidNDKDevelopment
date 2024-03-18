package com.simley.lib_socket.aysnc.future;

import java.util.concurrent.ExecutorService;

/**
 * Created by koush on 12/22/13.
 */
public class FutureThread<T> extends SimpleFuture<T> {
    public FutureThread(final FutureRunnable<T> runnable) {
        this(runnable, "FutureThread");
    }

    public FutureThread(final ExecutorService pool, final FutureRunnable<T> runnable) {
        pool.submit(() -> {
            try {
                setComplete(runnable.run());
            }
            catch (Exception e) {
                setComplete(e);
            }
        });
    }

    public FutureThread(final FutureRunnable<T> runnable, String name) {
        new Thread(() -> {
            try {
                setComplete(runnable.run());
            }
            catch (Exception e) {
                setComplete(e);
            }
        }, name).start();
    }
}
