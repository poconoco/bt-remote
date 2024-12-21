package com.nocomake.serialremote.connection;

import io.reactivex.disposables.Disposable;

public interface Connection {
    Disposable connect(final Runnable mOnConnected);
    void disconnect();
    void send(final byte[] packet);
    boolean isConnected();
    boolean isConnecting();
}
