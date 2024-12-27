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

    public static ArrayList<RemoteDevice> getRemoteDevices(Context context) {
        final ArrayList<RemoteDevice> tcpDevices =
                TCPConnection.getRemoteDevices(context);

        final ArrayList<RemoteDevice> bluetoothDevices =
                BluetoothConnection.getRemoteDevices(context);

        final ArrayList<RemoteDevice> result = new ArrayList<>();
        result.addAll(tcpDevices);
        result.addAll(bluetoothDevices);

        return result;
    }

    public static Connection createConnection(
            RemoteDevice remoteDevice,
            Consumer<String> onReceived,
            Runnable onError,
            Context context) {
        switch (remoteDevice.type) {
            case BT:
                return new BluetoothConnection(remoteDevice, onReceived, onError, context);
            case TCP:
                return new TCPConnection(remoteDevice, onReceived, onError, context);
            default:
                Toast.makeText(
                        context,
                        "Unknown remote type: "+remoteDevice.type,
                        Toast.LENGTH_LONG).show();
                return null;
        }
    }
}
