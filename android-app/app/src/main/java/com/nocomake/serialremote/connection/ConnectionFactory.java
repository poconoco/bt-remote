package com.nocomake.serialremote.connection;

import java.util.ArrayList;
import android.content.Context;

import com.nocomake.serialremote.connection.impls.BluetoothConnection;
import com.nocomake.serialremote.connection.impls.TCPConnection;
import com.nocomake.serialremote.connection.impls.UDPConnection;

public class ConnectionFactory {
    public static class RemoteDevice {
        public enum Type {
            BT,
            TCP,
            UDP
        }

        public RemoteDevice(Type type_, String name_, String address_) {
            type = type_;
            name = name_;
            address = address_;
        }

        final public String name;
        final public String address;
        final public Type type;
    }

    public static ArrayList<RemoteDevice> getRemoteDevices(Context context, boolean skipBluetooth) {
        final ArrayList<RemoteDevice> result = new ArrayList<>();

        result.addAll(TCPConnection.getRemoteDevices(context));
        result.addAll(UDPConnection.getRemoteDevices(context));

        if (! skipBluetooth)
            result.addAll(BluetoothConnection.getRemoteDevices(context));

        return result;
    }

    public static Connection createConnection(
            RemoteDevice remoteDevice,
            Context context) {
        return switch (remoteDevice.type) {
            case BT -> new BluetoothConnection(remoteDevice, context);
            case TCP -> new TCPConnection(remoteDevice, context);
            case UDP -> new UDPConnection(remoteDevice, context);
        };
    }
}
