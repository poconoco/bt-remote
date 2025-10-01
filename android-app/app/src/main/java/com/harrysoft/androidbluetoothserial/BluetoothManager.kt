package com.harrysoft.androidbluetoothserial

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import io.reactivex.rxjava3.core.Single
import java.nio.charset.Charset
import android.bluetooth.BluetoothManager as SystemBluetoothManager // Alias the Android system class

interface BluetoothManager : AutoCloseable {
    /**
     * A collection of paired Bluetooth devices, not restricted to serial devices.
     */
    val pairedDevices: Collection<BluetoothDevice>

    /**
     * A collection of paired Bluetooth devices, not restricted to serial devices.
     */
    @Deprecated("Use pairedDevices instead", replaceWith = ReplaceWith("pairedDevices"))
    val pairedDevicesList: List<BluetoothDevice> get() = pairedDevices.toList()

    /**
     * @param mac The MAC address of the device
     * you are trying to connect to
     * @return An RxJava Single, that will either emit
     * a BluetoothSerialDevice or a BluetoothConnectException
     */
    fun openSerialDevice(mac: String): Single<BluetoothSerialDevice>

    /**
     * @param mac The MAC address of the device
     * you are trying to connect to
     * @param charset The Charset to use for input/output streams
     * @return An RxJava Single, that will either emit
     * a BluetoothSerialDevice or a BluetoothConnectException
     */
    fun openSerialDevice(mac: String, charset: Charset): Single<BluetoothSerialDevice>

    /**
     * Closes the connection to a device. After calling,
     * you should probably set your instance to null
     * to avoid trying to read/write from it.
     *
     * @param mac The MAC Address of the device you are
     * trying to close the connection to
     */
    fun closeDevice(mac: String)

    /**
     * Closes the connection to a device. After calling,
     * you should probably set your instance to null
     * to avoid trying to read/write from it.
     *
     * @param device The instance of the device you are
     * trying to close the connection to
     */
    fun closeDevice(device: BluetoothSerialDevice)

    /**
     * Closes the connection to a device. After calling,
     * you should probably set your instance to null
     * to avoid trying to read/write from it.
     *
     * @param deviceInterface The interface accessing the device
     * you are trying to close the connection to
     */
    fun closeDevice(deviceInterface: SimpleBluetoothDeviceInterface)

    /**
     * Closes all connected devices
     */
    override fun close()

    companion object {
        /**
         * Creates and returns a BluetoothManager instance if the device
         * has Bluetooth, or null otherwise.
         *
         * @param context The application context is required to access system services.
         * @return A BluetoothManager instance or null.
         */
        @JvmStatic
        fun getInstance(context: Context): BluetoothManager? {
            // 1. Get the BluetoothManager system service
            val systemBluetoothManager =
                context.getSystemService(Context.BLUETOOTH_SERVICE)
                        as? SystemBluetoothManager

            // 2. Use the BluetoothManager to get the BluetoothAdapter
            val bluetoothAdapter = systemBluetoothManager?.adapter

            // 3. Initialize your class if the adapter is available
            return if (bluetoothAdapter != null) {
                // Assuming BluetoothManagerImpl is your concrete implementation
                BluetoothManagerImpl(bluetoothAdapter)
            } else null
        }
    }
}
