package com.nocomake.serialremote.connection;

import android.content.Context;

import com.nocomake.serialremote.connection.impls.BluetoothConnection;
import com.nocomake.serialremote.connection.impls.TCPConnection;

import java.util.ArrayList;
import java.util.function.Consumer;

public class ConnectionFactory {
    public static class RemoteDevice {
        public RemoteDevice(String name_, String address_) {
            name = name_;
            address = address_;
        }

        public String name;
        public String address;
    }

    public static ArrayList<RemoteDevice> getRemoteDevices(Context context, Runnable onError) {
        final ArrayList<RemoteDevice> bluetoothDevices =
                (new BluetoothConnection(null, null, null, onError, context))
                        .getRemoteDevices(context);

        // TODO: Also fetch TCP devices

        return bluetoothDevices;
    }

    public static Connection createConnection(
            RemoteDevice remoteDevice,
            Runnable onSent,
            Consumer<String> onReceived,
            Runnable onError,
            Context context) {
        // TODO: determine if remoteDevice needs a BT or TCP connection
        return new BluetoothConnection(remoteDevice, onSent, onReceived, onError, context);
    }
}
