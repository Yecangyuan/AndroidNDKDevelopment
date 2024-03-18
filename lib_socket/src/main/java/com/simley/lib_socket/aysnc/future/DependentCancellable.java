package com.simley.lib_socket.aysnc.future;

public interface DependentCancellable extends Cancellable {
    boolean setParent(Cancellable parent);
}
