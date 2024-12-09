package com.poconoco.tests.btserialremote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
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

import BtSerialRemote.R;

public class MainActivity extends AppCompatActivity {

    private static final int BT_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();

        setContentView(R.layout.main_activity);

        fixFullscreen();

        mStatus = findViewById(R.id.status);
        mDeviceSelection = findViewById(R.id.btDevice);

        mLeftJoyPos = new PointF(0.5f, 0.5f);
        mRightJoyPos = new PointF(0.5f, 0.5f);

        mDeviceSelection.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final TextView textView = ((TextView) parent.getChildAt(0));
                if (textView != null)
                    textView.setTextColor(Color.WHITE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        resetKnobWhenPadReady(findViewById(R.id.leftJoystick), findViewById(R.id.leftKnob), mLeftJoyPos);
        resetKnobWhenPadReady(findViewById(R.id.rightJoystick), findViewById(R.id.rightKnob), mRightJoyPos);

        initSettingsButton();
        applyPreferences();

        mBluetoothManager = BluetoothManager.getInstance();
        if (mBluetoothManager == null) {
            // Bluetooth unavailable on this device :( tell the user
            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show(); // Replace context with your context instance.
            mStatus.setText("Bluetooth not available");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.BLUETOOTH_CONNECT },
                    BT_PERMISSION_REQUEST);
        } else {
            populatePairedDevices();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        disconnect(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyPreferences();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BT_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populatePairedDevices();
                populatePairedDevices();
            } else {
                mStatus.setText("Check BT permission");
            }
        }
    }

    private void fixFullscreen() {
        // Try to fill the space under the camera cutout to the same color we use for
        // background
        final Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(getResources().getColor(R.color.background, null));
        final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        getWindow().setBackgroundDrawable(bitmapDrawable);

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    private void applyPreferences() {
        final SharedPreferences sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(this);

        setViewName(R.id.switchA, sharedPreferences.getString("switchA", ""), "A");
        setViewName(R.id.switchB, sharedPreferences.getString("switchB", ""), "B");
        setViewName(R.id.switchC, sharedPreferences.getString("switchC", ""), "C");
        setViewName(R.id.switchD, sharedPreferences.getString("switchD", ""), "D");

        setViewName(R.id.buttonE, sharedPreferences.getString("buttonE", ""), "E");
        setViewName(R.id.buttonF, sharedPreferences.getString("buttonF", ""), "F");
        setViewName(R.id.buttonG, sharedPreferences.getString("buttonG", ""), "G");
        setViewName(R.id.buttonH, sharedPreferences.getString("buttonH", ""), "H");

        setViewName(R.id.sliderLText, sharedPreferences.getString("sliderL", ""), "L");
        setViewName(R.id.sliderRText, sharedPreferences.getString("sliderR", ""), "R");

        setViewVisibility(R.id.sliderLContainer, sharedPreferences.getBoolean("showSliderL", true));
        setViewVisibility(R.id.sliderRContainer, sharedPreferences.getBoolean("showSliderR", true));

        setViewVisibility(R.id.leftJoystick, sharedPreferences.getBoolean("showJoyL", true));
        setViewVisibility(R.id.rightJoystick, sharedPreferences.getBoolean("showJoyR", true));
        setViewVisibility(R.id.leftKnob, sharedPreferences.getBoolean("showJoyL", true));
        setViewVisibility(R.id.rightKnob, sharedPreferences.getBoolean("showJoyR", true));

    }

    private void setViewName(int resourceId, String name, String defValue) {
        if (name == null || name.isEmpty())
            name = defValue;

        final TextView switchView = findViewById(resourceId);
        switchView.setText(name);
    }

    private void setViewVisibility(int resourceId, boolean visible) {
        final View view = findViewById(resourceId);
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void initSettingsButton() {
        final ImageButton settingsButton = findViewById(R.id.buttonSettings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(MainActivity.this, PrefsActivity.class);
                MainActivity.this.startActivity(settingsIntent);
            }
        });
    }

    private void populatePairedDevices() {
        final Collection<BluetoothDevice> pairedDevices = mBluetoothManager.getPairedDevicesList();
        ArrayList<String> pairedNames = new ArrayList<>();
        mPairedMACs = new ArrayList<>();

        try {
            for (final BluetoothDevice device : pairedDevices) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pairedNames.add(device.getAlias());
                } else {
                    pairedNames.add(device.getName());
                }
                mPairedMACs.add(device.getAddress());
            }
        } catch (SecurityException e) {
            mStatus.setText("Check BT permission");
        }

        final ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item,
                        pairedNames);

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDeviceSelection.setAdapter(spinnerAdapter);

        resetConnectButton();
        resetSwitchButtons();
    }

    private void scheduleSend() {
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                () -> {
                    if (!mConnected)
                        return;

                    int x1 = Math.round(mLeftJoyPos.x * 100);
                    int y1 = 100 - Math.round(mLeftJoyPos.y * 100);

                    int x2 = Math.round(mRightJoyPos.x * 100);
                    int y2 = 100 - Math.round(mRightJoyPos.y * 100);

                    String packet = String.format(
                        "MX%03dY%03dA%dB%d", x1, y1,
                        mStateA ? 1 : 0,
                        mStateE ? 1 : 0);
                    mStatus.setText(packet);

                    mDeviceInterface.sendMessage(packet);
                    scheduleSend();
                },
                100);
    }

    private void resetKnobWhenPadReady(ImageView padView, RelativeLayout knobView, PointF pos) {
        padView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateKnob(padView, knobView, pos);
                attachKnobMovement(padView, knobView, pos);
            }
        });
    }

    private void updateKnob(ImageView padView, RelativeLayout knobView, PointF pos) {
        int maxX = padView.getWidth() - knobView.getWidth();
        int maxY = padView.getHeight() - knobView.getHeight();

        knobView.setX(padView.getX() + pos.x * maxX);
        knobView.setY(padView.getY() + pos.y * maxY);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachKnobMovement(ImageView padView, RelativeLayout knobView, PointF output) {

        final MainActivity that = this;
        padView.setOnTouchListener((view1, motionEvent) -> {
            final int action = motionEvent.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    int[] pos = new int[2];
                    view1.getLocationOnScreen(pos);

                    final float width = view1.getWidth();
                    final float height = view1.getHeight();

                    final float knobW = (float)knobView.getWidth();
                    final float knobH = (float)knobView.getHeight();

                    final float x = (motionEvent.getX() - knobW/2) / (width - knobW);
                    final float y = (motionEvent.getY() - knobH/2) / (height - knobH);

                    output.x = clamp(x, 0, 1);
                    output.y = clamp(y, 0, 1);

                    that.updateKnob(padView, knobView, output);
                    return true;

                case MotionEvent.ACTION_UP:
                    output.x = 0.5f;
                    output.y = 0.5f;

                    that.updateKnob(padView, knobView, output);
                    return true;
            }

            return false;
        });
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
        final SwitchCompat mSwA = findViewById(R.id.switchA);
        final Button mBtnE = findViewById(R.id.buttonE);

        mSwA.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mStateA = isChecked;
            }
        });

        mBtnE.setOnTouchListener((view1, motionEvent) -> {
            final int action = motionEvent.getActionMasked();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mStateE = true;
                    break;
                case MotionEvent.ACTION_UP:
                    mStateE = false;
                    break;
            }

            return false;
        });
    }

    private void connect() {
        final int selectedPos = mDeviceSelection.getSelectedItemPosition();

        if (selectedPos < 0) {
            resetConnectButton();
            mStatus.setText("No BT connection selected");
            return;
        }

        final String mac = mPairedMACs.get(selectedPos);

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
        disconnect("Connection error");
    }

    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private TextView mStatus;
    private Spinner mDeviceSelection;

    // State to be sent
    private PointF mLeftJoyPos;
    private PointF mRightJoyPos;
    private boolean mStateA;
    private boolean mStateE;

    private BluetoothManager mBluetoothManager;
    private SimpleBluetoothDeviceInterface mDeviceInterface;
    private boolean mConnected;
    private ArrayList<String> mPairedMACs;
}