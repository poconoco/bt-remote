package com.poconoco.tests.btserialremote;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.harrysoft.androidbluetoothserial.BluetoothManager;
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice;
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface;

import java.util.ArrayList;
import java.util.Collection;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        setContentView(R.layout.main_activity);

        mStatus = findViewById(R.id.status);
        mDeviceSelection = findViewById(R.id.btDevice);
        mLeftJoystick = findViewById(R.id.leftJoystick);
        mLeftKnob = findViewById(R.id.leftKnob);

        mLeftJoystickPos = new PointF(0.5f, 0.5f);

        mBluetoothManager = BluetoothManager.getInstance();
        if (mBluetoothManager == null) {
            // Bluetooth unavailable on this device :( tell the user
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show(); // Replace context with your context instance.
            mStatus.setText("Bluetooth not available");
            return;
        }

        final Collection<BluetoothDevice> pairedDevices = mBluetoothManager.getPairedDevicesList();
        ArrayList<String> pairedNames = new ArrayList<>();
        mPairedMACs = new ArrayList<>();

        for (final BluetoothDevice device : pairedDevices) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pairedNames.add(device.getAlias());
            } else {
                pairedNames.add(device.getName());
            }
            mPairedMACs.add(device.getAddress());
        }

        final ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<String>(this,
                                         android.R.layout.simple_spinner_item,
                                         pairedNames);

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDeviceSelection.setAdapter(spinnerAdapter);

        resetConnectButton();
        resetSwitchButtons();

        attachViewJoystick(mLeftJoystick, mLeftJoystickPos);

        // FIXME: This has no effect, the knob is not placed in the center at the start
        this.updateKnob();
    }

    @Override
    protected void onPause() {
        super.onPause();

        disconnect(null);
    }

    private void scheduleSend() {
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                () -> {
                    if (!mConnected)
                        return;

                    int x = Math.round(mLeftJoystickPos.x * 100);
                    int y = 100 - Math.round(mLeftJoystickPos.y * 100);

                    String packet = String.format("MX%03dY%03dA%dB%d", x, y,
                                                  mSwitchA ? 1 : 0,
                                                  mSwitchB ? 1 : 0);
                    mStatus.setText(packet);

                    mDeviceInterface.sendMessage(packet);
                    scheduleSend();
                },
                100);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachViewJoystick(ImageView view, PointF output) {

        final MainActivity that = this;
        view.setOnTouchListener((view1, motionEvent) -> {
            if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN
                    || motionEvent.getActionMasked() == MotionEvent.ACTION_MOVE) {
                int[] pos = new int[2];
                view1.getLocationOnScreen(pos);
                //view.getLocationInWindow(locations);

                float width = view1.getWidth();
                float height = view1.getHeight();
                float x = motionEvent.getX() / width;
                float y = motionEvent.getY() / height;

                output.x = clamp(x, 0, 1);
                output.y = clamp(y, 0, 1);

                that.updateKnob();

                return true;
            }

            if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                output.x = 0.5f;
                output.y = 0.5f;

                that.updateKnob();

                return true;
            }

            return false;
        });
    }

    private void updateKnob() {
        int allowedOver = 40;

        int maxX = mLeftJoystick.getWidth() - mLeftKnob.getWidth() + allowedOver * 2;
        int maxY = mLeftJoystick.getHeight() - mLeftKnob.getHeight() + allowedOver * 2;

        mLeftKnob.setX(mLeftJoystickPos.x * maxX - allowedOver);
        mLeftKnob.setY(mLeftJoystickPos.y * maxY - allowedOver);
    }

    private void resetConnectButton() {
        final Button mConnect = findViewById(R.id.connect);

        mConnect.setEnabled(true);
        if (mConnected) {
            mConnect.setText("Disconnect");
            mConnect.setOnClickListener(view -> disconnect(null));
        } else {
            mConnect.setText("Connect");
            mConnect.setOnClickListener(view -> {
                mStatus.setText("Connecting...");
                mConnect.setEnabled(false);
                connect();
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void resetSwitchButtons() {
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        final Switch mLight = findViewById(R.id.switchA);
        final Button mHonk = findViewById(R.id.buttonHonk);

        mLight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mSwitchA = isChecked;
            }
        });

        mHonk.setOnTouchListener((view1, motionEvent) -> {
            if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
                mSwitchB = true;
                return true;
            }

            if (motionEvent.getActionMasked() == MotionEvent.ACTION_UP) {
                mSwitchB = false;
                return true;
            }

            return false;
        });
    }

    private void connect() {
        final String mac = mPairedMACs.get(mDeviceSelection.getSelectedItemPosition());

        mBluetoothManager.openSerialDevice(mac)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnected, this::onError);
    }

    private void disconnect(final String message) {
        if (mDeviceInterface != null)
            mBluetoothManager.closeDevice(mDeviceInterface);
        mBluetoothManager.close();

        mDeviceInterface = null;

        mConnected = false;
        resetConnectButton();

        if (message != null)
            mStatus.setText(message);
        else
            mStatus.setText("Disconnected");
    }

    private void onConnected(BluetoothSerialDevice connectedDevice) {
        // You are now connected to this device!
        // Here you may want to retain an instance to your device:
        mDeviceInterface = connectedDevice.toSimpleDeviceInterface();

        // Listen to bluetooth events
        mDeviceInterface.setListeners(this::onMessageReceived, this::onMessageSent, this::onError);

        // Let's send a message:
        //mDeviceInterface.sendMessage("Hello world!");

        mStatus.setText("Connected");

        mConnected = true;
        resetConnectButton();

        scheduleSend();
    }

    private void onMessageSent(String message) {}

    private void onMessageReceived(String message) {
        mStatus.setText("Received: " + message);
    }

    private void onError(Throwable error) {
        disconnect("Error: "+error.getLocalizedMessage());
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private TextView mStatus;
    private Spinner mDeviceSelection;
    private ImageView mLeftJoystick;

    private RelativeLayout mLeftKnob;

    // State to be sent
    private PointF mLeftJoystickPos;
    private boolean mSwitchA;
    private boolean mSwitchB;

    private BluetoothManager mBluetoothManager;
    private SimpleBluetoothDeviceInterface mDeviceInterface;
    private boolean mConnected;
    private ArrayList<String> mPairedMACs;
}