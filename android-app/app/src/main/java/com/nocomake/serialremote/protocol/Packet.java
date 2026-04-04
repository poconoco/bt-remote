package com.nocomake.serialremote.protocol;

public class Packet {
    final public boolean[] switches = new boolean[8];
    final public byte[] axes = new byte[4];
    final public byte[] sliders = new byte[2];
    final public byte[] orientation = new byte[3];

    final public byte reserved = 0;
}
