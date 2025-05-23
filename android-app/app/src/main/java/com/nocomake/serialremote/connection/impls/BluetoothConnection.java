package com.nocomake.serialremote.connection.impls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;
import com.nocomake.serialremote.connection.Connection;
import com.nocomake.serialremote.connection.ConnectionFactory;

public class BluetoothConnection implements Connection {
    public static ArrayList<ConnectionFactory.RemoteDevice> getRemoteDevices(Context context) {
        final ArrayList<ConnectionFactory.RemoteDevice> result = new ArrayList<>();
        BluetoothManager bluetoothManager = BluetoothManager.getInstance();
        if (bluetoothManager == null) {
            Toast.makeText(
                    context,
                    "Failed to initialize Bluetooth manager",
                    Toast.LENGTH_LONG).show();
            return result;
        }

        final Collection<BluetoothDevice> pairedDevices = bluetoothManager.getPairedDevices();
        try {
            for (final BluetoothDevice device : pairedDevices) {
                final String name = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        ? device.getAlias()
                        : device.getName();

                result.add(new ConnectionFactory.RemoteDevice(
                        ConnectionFactory.RemoteDevice.Type.BT,
                        "BT: "+name,
                        device.getAddress()));
            }
        } catch (SecurityException e) {
            Toast.makeText(context, "Bluetooth permission error", Toast.LENGTH_LONG).show();
        }

        return result;
    }

    public BluetoothConnection(
            ConnectionFactory.RemoteDevice remoteDevice,
            Context context) {
        mConnected = false;
        mConnecting = false;
        mRemoteDevice = remoteDevice;
        mBluetoothManager = BluetoothManager.getInstance();
        if (mBluetoothManager == null) {
            Toast.makeText(context, "Bluetooth not available", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public Disposable connect(Runnable onConnected, Runnable onError) {
        mConnecting = true;
        mOnConnected = onConnected;
        mOnError = onError;
        return mBluetoothManager.openSerialDevice(mRemoteDevice.address)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnected, this::onError);
    }

    @Override
    public void disconnect() {
        if (mDeviceInterface != null)
            mBluetoothManager.closeDevice(mDeviceInterface);

        mBluetoothManager.close();
        mDeviceInterface = null;
        mConnected = false;
    }

    @Override
    public void send(final byte[] packet) {
        if (!mConnected)
            return;

        mDeviceInterface.sendBytes(packet);
    }

    @Override
    public void setOnReceivedListener(Consumer<String> onReceived) {
        mOnReceived = onReceived;
    }

    @Override
    public boolean isConnected() {
        return mConnected;
    }

    @Override
    public boolean isConnecting() {
        return mConnecting;
    }

    private void onConnected(BluetoothSerialDevice connectedDevice) {
        // You are now connected to this device!
        // Here you may want to retain an instance to your device:
        mDeviceInterface = connectedDevice.toSimpleDeviceInterface();

        // Listen to bluetooth events
        mDeviceInterface.setListeners(this::onMessageReceived, this::onMessageSent, this::onError);
        mConnected = true;
        mConnecting = false;

        mOnConnected.run();
    }

    private void onMessageSent(String message) {
        // do nothing
    }

    private void onMessageReceived(String message) {
        // Expect remote to encode all \n as \t
        if (mOnReceived != null)
            mOnReceived.accept(message.replace('\t', '\n'));
    }

    private void onError(Throwable error) {
        mConnecting = false;
        if (mOnError != null)
            mOnError.run();
    }

    private final BluetoothManager mBluetoothManager;
    private SimpleBluetoothDeviceInterface mDeviceInterface;
    private final ConnectionFactory.RemoteDevice mRemoteDevice;
    private boolean mConnected;
    private boolean mConnecting;
    private Runnable mOnConnected;
    private Runnable mOnError;
    private Consumer<String> mOnReceived;

}
