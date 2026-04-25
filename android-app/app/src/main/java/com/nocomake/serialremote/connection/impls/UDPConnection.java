package com.nocomake.serialremote.connection.impls;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.Consumer;
import io.reactivex.rxjava3.disposables.Disposable;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.widget.Toast;
import androidx.preference.PreferenceManager;

import com.nocomake.serialremote.connection.Connection;
import com.nocomake.serialremote.connection.ConnectionFactory;

import DiyRemote.R;

public class UDPConnection implements Connection {

    public static ArrayList<ConnectionFactory.RemoteDevice> getRemoteDevices(Context context) {
        final ArrayList<ConnectionFactory.RemoteDevice> result = new ArrayList<>();

        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);

        final int defaultPort =
                context.getResources().getInteger(R.integer.defaultIpPort);
        final String defaultIpAddress =
                context.getResources().getString(R.string.defaultIpAddress);

        final String ipAddress =
                sharedPreferences.getString("ipAddress", defaultIpAddress);
        final int ipPort = Integer.parseInt(sharedPreferences.getString(
                "ipPort", Integer.toString(defaultPort)));

        final String addressWithPort = ipAddress + ":" + ipPort;

        result.add(new ConnectionFactory.RemoteDevice(
                ConnectionFactory.RemoteDevice.Type.UDP,
                "UDP: " + addressWithPort,
                addressWithPort));

        return result;
    }

    public UDPConnection(
            ConnectionFactory.RemoteDevice remoteDevice,
            Context context) {
        mConnected = false;
        mConnecting = false;
        mContext = context;
        mMainHandler = new Handler(Looper.getMainLooper());

        String[] parts = remoteDevice.address.split(":");
        if (parts.length != 2) {
            Toast.makeText(
                    context,
                    "Invalid UDP address, check preferences",
                    Toast.LENGTH_LONG).show();
            return;
        }

        mAddresss = parts[0];
        mPort = Integer.parseInt(parts[1]);
    }

    @Override
    public Disposable connect(Runnable onConnected, Runnable onError) {
        if (mPort == 0 || mAddresss == null)
            return null;

        mConnecting = true;
        mOnError = onError;

        mReceiveThread = new Thread(() -> {
            try {
                InetAddress serverAddr = InetAddress.getByName(mAddresss);
                mSocket = new DatagramSocket();

                // Connecting a UDP socket restricts it to this specific address/port.
                // It does NOT establish a real connection, so this will succeed instantly.
                mSocket.connect(serverAddr, mPort);

                mConnected = true;
                mConnecting = false;
                mMainHandler.post(onConnected);

                mSendHandlerThread = new HandlerThread("UdpSendThread");
                mSendHandlerThread.start();
                mSendHandler = new Handler(mSendHandlerThread.getLooper());

                // Buffer for incoming packets (max standard UDP payload size)
                byte[] receiveBuffer = new byte[65535];
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);

                while (mConnected) {
                    // This blocks until a packet is received
                    mSocket.receive(receivePacket);

                    String message = new String(
                            receivePacket.getData(),
                            0,
                            receivePacket.getLength(),
                            StandardCharsets.UTF_8
                    );

                    // Replicate BufferedReader.readLine() behavior by stripping trailing newlines
                    if (message.endsWith("\n")) {
                        message = message.substring(0, message.length() - 1);
                    }
                    if (message.endsWith("\r")) {
                        message = message.substring(0, message.length() - 1);
                    }

                    final String _message = message;
                    mMainHandler.post(() -> {
                        if (mOnReceived != null)
                            mOnReceived.accept(_message.replace('\t', '\n'));
                    });
                }
            } catch (SocketException e) {
                // This is expected when disconnect() calls mSocket.close()
                // We simply break out of the thread naturally.
            } catch (Exception e) {
                mConnected = false;
                mConnecting = false;
                cleanupThreads();

                if (mOnError != null)
                    mMainHandler.post(mOnError);

                mMainHandler.post(() -> {
                    Toast.makeText(
                            mContext,
                            "UDP communication error",
                            Toast.LENGTH_LONG).show();
                });
            }
        });

        mReceiveThread.start();
        return null;
    }

    @Override
    public void disconnect() {
        if (!mConnected)
            return;

        mConnected = false;

        if (mSocket != null) {
            mSocket.close(); // This interrupts the blocking mSocket.receive() loop
            mSocket = null;
        }

        try {
            if (mReceiveThread != null) {
                mReceiveThread.join(1000); // Wait up to 1s for thread to die
            }
        } catch (InterruptedException e) {
            Toast.makeText(mContext, "Error joining receive thread", Toast.LENGTH_SHORT).show();
        }

        cleanupThreads();
    }

    private void cleanupThreads() {
        if (mSendHandlerThread != null) {
            mSendHandlerThread.quitSafely();
            mSendHandlerThread = null;
            mSendHandler = null;
        }
    }

    @Override
    public void send(byte[] packet) {
        if (!mConnected || mSocket == null)
            return;

        mSendHandler.post(() -> {
            try {
                // Since we used mSocket.connect(), we don't need to specify the address here
                DatagramPacket sendPacket = new DatagramPacket(packet, packet.length);
                mSocket.send(sendPacket);
            } catch (IOException e) {
                mMainHandler.post(() -> {
                    Toast.makeText(
                            mContext,
                            "Error sending UDP packet",
                            Toast.LENGTH_LONG).show();
                    if (mOnError != null)
                        mOnError.run();
                });
            }
        });
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

    private String mAddresss;
    private int mPort;
    private volatile boolean mConnected; // Volatile because it's checked across threads
    private boolean mConnecting;
    private Runnable mOnError;
    private Consumer<String> mOnReceived;
    private final Context mContext;
    private final Handler mMainHandler;
    private HandlerThread mSendHandlerThread;
    private Handler mSendHandler;
    private Thread mReceiveThread;
    private DatagramSocket mSocket;
}
