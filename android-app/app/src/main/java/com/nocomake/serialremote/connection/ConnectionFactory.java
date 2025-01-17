package com.nocomake.serialremote.connection;

import java.util.ArrayList;
import java.util.function.Consumer;

import android.content.Context;
import android.widget.Toast;

import com.nocomake.serialremote.connection.impls.BluetoothConnection;
import com.nocomake.serialremote.connection.impls.TCPConnection;

public class ConnectionFactory {
    public static class RemoteDevice {
        public enum Type {
            BT,
            TCP
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
        final ArrayList<RemoteDevice> result =
                TCPConnection.getRemoteDevices(context);

        if (! skipBluetooth)
            result.addAll(BluetoothConnection.getRemoteDevices(context));

        return result;
    }

    public static Connection createConnection(
            RemoteDevice remoteDevice,
            Context context) {
        switch (remoteDevice.type) {
            case BT:
                return new BluetoothConnection(remoteDevice, context);
            case TCP:
                return new TCPConnection(remoteDevice, context);
            default:
                Toast.makeText(
                        context,
                        "Unknown remote type: "+remoteDevice.type,
                        Toast.LENGTH_LONG).show();
                return null;
        }
    }
}
