package com.nocomake.serialremote.protocol;

import com.nocomake.serialremote.protocol.impls.BinaryProtocol;

public class ProtocolFactory {
    public static Protocol createProtocol() {
        return new BinaryProtocol();
    }
}
