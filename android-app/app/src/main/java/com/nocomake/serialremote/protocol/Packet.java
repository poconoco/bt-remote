package com.nocomake.serialremote.protocol;

public class Packet {
    final public boolean[] switches = new boolean[8];
    final public byte[] axes = new byte[4];
    final public byte[] sliders = new byte[2];

    // For future upgrades like device orientation data, but to be sure older clients
    // would still work with the updated remote apps
    final public byte[] reserved = new byte[4];
}
