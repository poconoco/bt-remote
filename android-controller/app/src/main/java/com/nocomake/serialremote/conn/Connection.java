package com.nocomake.serialremote.conn;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public abstract class Connection {

    public abstract Disposable connect(final Runnable mOnConnected);
    public abstract void disconnect();
    public abstract void send(final byte[] packet);

    public abstract boolean isConnected();
    public abstract boolean isConnecting();
}
