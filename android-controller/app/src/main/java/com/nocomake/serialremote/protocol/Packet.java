package com.nocomake.serialremote.protocol;

public class Packet {
    public boolean[] switches = new boolean[8];
    public byte[] axes = new byte[4];
    public byte[] sliders = new byte[2];

    // For future upgrades like device orientation data, but to be sure older clients
    // would still work with the updated remote apps
    public byte[] reserved = new byte[4];
}
