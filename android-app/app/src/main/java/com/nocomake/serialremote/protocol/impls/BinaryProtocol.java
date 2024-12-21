package com.nocomake.serialremote.protocol.impls;

import com.nocomake.serialremote.protocol.Packet;
import com.nocomake.serialremote.protocol.Protocol;

public class BinaryProtocol implements Protocol {
    @Override
    public byte[] serialize(Packet packet) {
        final byte[] result = new byte[15];

        // packet pointer
        int p = 0;

        // 3 bytes - header
        result[p++] = 'N';
        result[p++] = 'O';
        result[p++] = 'C';

        // 1 byte - bitmask for 8 binary switches
        byte bitSwitches = 0;
        assert packet.switches.length == 8;
        for (int i = 0; i < packet.switches.length; i++)
            bitSwitches |= (byte)(packet.switches[i] ? (1 << i) : 0);
        result[p++] = bitSwitches;

        // 4 bytes - axes
        for (byte value : packet.axes)
            result[p++] = value;

        // 2 bytes - slider
        for (byte value : packet.sliders)
            result[p++] = value;

        // 4 bytes - reserved
        for (byte value : packet.reserved)
            result[p++] = value;

        // Calculate XOR checksum
        byte checksum = 0;
        for (byte value : result)
            checksum ^= value;

        // 1 byte - checksum
        result[p++] = checksum;

        // The whole packet expected to be 15 bytes
        assert p == 15;

        return result;
    }
}
