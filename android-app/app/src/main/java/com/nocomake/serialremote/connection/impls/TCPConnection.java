package com.nocomake.serialremote.connection.impls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.function.Consumer;
import io.reactivex.disposables.Disposable;

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

public class TCPConnection implements Connection {

    public static ArrayList<ConnectionFactory.RemoteDevice> getRemoteDevices(Context context) {
        final ArrayList<ConnectionFactory.RemoteDevice> result = new ArrayList<>();

        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);

        final int defaultPort =
                context.getResources().getInteger(R.integer.defaultTcpPort);
        final String defaultIpAddress =
                context.getResources().getString(R.string.defaultTcpAddress);
        final String ipAddress =
                sharedPreferences.getString("ipAddress", defaultIpAddress);
        final int ipPort = Integer.parseInt(sharedPreferences.getString(
                "ipPort", Integer.toString(defaultPort)));

        final String addressWithPort = ipAddress+":"+ipPort;

        result.add(new ConnectionFactory.RemoteDevice(
                ConnectionFactory.RemoteDevice.Type.TCP,
                "TCP: "+addressWithPort,
                addressWithPort));

        return result;
    }

    public TCPConnection(
            ConnectionFactory.RemoteDevice remoteDevice,
            Consumer<String> onReceived,
            Runnable onError,
            Context context) {
        mConnected = false;
        mConnecting = false;
        mOnReceived = onReceived;
        mOnError = onError;
        mContext = context;
        mMainHandler = new Handler(Looper.getMainLooper());

        String[] parts = remoteDevice.address.split(":");
        if (parts.length != 2) {
            Toast.makeText(
                    context,
                    "Invalid TCP address, check preferences",
                    Toast.LENGTH_LONG).show();
            return;
        }

        mAddresss = parts[0];
        mPort = Integer.parseInt(parts[1]);
    }

    @Override
    public Disposable connect(Runnable onConnected) {
        if (mPort == 0 || mAddresss == null)
            return null;

        mConnecting = true;

         mReceiveThread = new Thread(() -> {
            try {
                // Create a socket and connect to the server
                SocketAddress socketAddress = new InetSocketAddress(mAddresss, mPort);
                mSocket = new Socket();
                mSocket.connect(socketAddress, 2000);

                mConnected = true;
                mConnecting = false;
                mMainHandler.post(onConnected);
                mOutputStream = mSocket.getOutputStream();
                final BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(mSocket.getInputStream()));

                mSendHandlerThread = new HandlerThread("SendThread");
                mSendHandlerThread.start();
                mSendHandler = new Handler(mSendHandlerThread.getLooper());

                String message;
                while ((message = reader.readLine()) != null) {
                    final String _message = message;
                    mMainHandler.post(() -> {
                        mOnReceived.accept(_message);
                    });
                }
            } catch (Exception e) {
                mConnected = false;
                mConnecting = false;
                if (mSendHandlerThread != null) {
                    mSendHandlerThread.quitSafely();
                    mSendHandlerThread = null;
                }

                mMainHandler.post(mOnError);
                mMainHandler.post(() -> {
                    Toast.makeText(
                            mContext,
                            "TCP communication error",
                            Toast.LENGTH_LONG).show();
                });
            }
        });

        mReceiveThread.start();

        return null;
    }

    @Override
    public void disconnect() {
        if (! mConnected)
            return;

        try {
            mSocket.shutdownInput();
            mReceiveThread.join();

            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }

            if (mSendHandlerThread != null) {
                mSendHandlerThread.quitSafely();
                mSendHandlerThread = null;
            }
        } catch (IOException e) {
            Toast.makeText(
                    mContext,
                    "Error closing socket",
                    Toast.LENGTH_LONG).show();
            mOnError.run();
        } catch (InterruptedException e) {
            Toast.makeText(
                    mContext,
                    "Error joining receive thread",
                    Toast.LENGTH_LONG).show();
            mOnError.run();
        } finally {
            mConnected = false;
        }
    }

    @Override
    public void send(byte[] packet) {
        if (! mConnected)
            return;

        mSendHandler.post(() -> {
            try {
                mOutputStream.write(packet);
                mOutputStream.flush();
            } catch (IOException e) {
                mMainHandler.post(() -> {
                    mConnected = false;
                    Toast.makeText(
                            mContext,
                            "Communication error",
                            Toast.LENGTH_LONG).show();
                    mOnError.run();
                });
            }
        });
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
    private boolean mConnected;
    private boolean mConnecting;
    private final Runnable mOnError;
    private final Consumer<String> mOnReceived;
    private final Context mContext;
    private final Handler mMainHandler;
    private HandlerThread mSendHandlerThread;
    private Handler mSendHandler;
    private Thread mReceiveThread;
    private Socket mSocket;
    private OutputStream mOutputStream;
}
