package com.nocomake.serialremote.connection;

import java.util.function.Consumer;

import io.reactivex.rxjava3.disposables.Disposable;

public interface Connection {
    Disposable connect(final Runnable mOnConnected, final Runnable onError);
    void disconnect();
    void send(final byte[] packet);
    void setOnReceivedListener(Consumer<String> onReceived);
    boolean isConnected();
    boolean isConnecting();
}
