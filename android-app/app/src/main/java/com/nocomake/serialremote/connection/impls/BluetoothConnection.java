package com.nocomake.serialremote.connection.impls;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;
import com.nocomake.serialremote.connection.Connection;
import com.nocomake.serialremote.connection.ConnectionFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class BluetoothConnection implements Connection {

    public BluetoothConnection(
            ConnectionFactory.RemoteDevice remoteDevice,
            Runnable onSent,
            Consumer<String> onReceived,
            Runnable onError,
            Context context) {
        mConnected = false;
        mConnecting = false;
        mRemoteDevice = remoteDevice;
        mOnSent = onSent;
        mOnReceived = onReceived;
        mOnError = onError;
        mBluetoothManager = BluetoothManager.getInstance();
        if (mBluetoothManager == null) {
            Toast.makeText(context, "Bluetooth not available", Toast.LENGTH_LONG).show();
        }

    }

    public ArrayList<ConnectionFactory.RemoteDevice> getRemoteDevices(Context context) {
        final Collection<BluetoothDevice> pairedDevices = mBluetoothManager.getPairedDevices();
        final ArrayList<ConnectionFactory.RemoteDevice> result = new ArrayList<>();

        try {
            for (final BluetoothDevice device : pairedDevices) {
                final String name = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        ? device.getAlias()
                        : device.getName();

                result.add(new ConnectionFactory.RemoteDevice(name, device.getAddress()));
            }
        } catch (SecurityException e) {
            Toast.makeText(context, "Bluetooth permission error", Toast.LENGTH_LONG).show();
        }

        return result;
    }

    @Override
    public Disposable connect(Runnable onConnected) {
        mConnecting = true;
        mOnConnected = onConnected;
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
        mOnSent.run();
    }

    private void onMessageReceived(String message) {
        mOnReceived.accept(message);
    }

    private void onError(Throwable error) {
        mConnecting = false;
        mOnError.run();
    }

    private BluetoothManager mBluetoothManager;
    private SimpleBluetoothDeviceInterface mDeviceInterface;
    private ConnectionFactory.RemoteDevice mRemoteDevice;
    private boolean mConnected;
    private boolean mConnecting;
    private Runnable mOnConnected;
    private final Runnable mOnSent;
    private final Runnable mOnError;
    private final Consumer<String> mOnReceived;

}
