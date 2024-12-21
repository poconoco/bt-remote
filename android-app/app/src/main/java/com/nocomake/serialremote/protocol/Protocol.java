package com.nocomake.serialremote.protocol;

public interface Protocol {
    byte[] serialize(final Packet packet);
}
