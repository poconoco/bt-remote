package com.nocomake.serialremote.conn;

import io.reactivex.disposables.Disposable;

public class TCPConnection extends Connection {
    @Override
    public Disposable connect(Runnable mOnConnected) {
        return null;
    }

    @Override
    public void disconnect() {

    }

    @Override
    public void send(byte[] packet) {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }
}
