package com.nocomake.serialremote;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import com.google.common.primitives.Booleans;
import com.nocomake.serialremote.connection.Connection;
import com.nocomake.serialremote.connection.ConnectionFactory;

import com.nocomake.serialremote.protocol.Packet;
import com.nocomake.serialremote.protocol.Protocol;
import com.nocomake.serialremote.protocol.ProtocolFactory;



import SerialRemote.R;

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
        mProtocol = ProtocolFactory.createProtocol();

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                         != PackageManager.PERMISSION_GRANTED ||
                 ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                         != PackageManager.PERMISSION_GRANTED)) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN},
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
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BT_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
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

        // An attempt to remove the black bar at the bottom with close swipe handle,
        // but also affects status bar, so disable for now, to reconsider later

        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        //                      WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    private void applyPreferences() {
        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        setViewText(R.id.switchA, sharedPreferences.getString("switchA", ""), "A");
        setViewText(R.id.switchB, sharedPreferences.getString("switchB", ""), "B");
        setViewText(R.id.switchC, sharedPreferences.getString("switchC", ""), "C");
        setViewText(R.id.switchD, sharedPreferences.getString("switchD", ""), "D");

        setViewText(R.id.buttonE, sharedPreferences.getString("buttonE", ""), "E");
        setViewText(R.id.buttonF, sharedPreferences.getString("buttonF", ""), "F");
        setViewText(R.id.buttonG, sharedPreferences.getString("buttonG", ""), "G");
        setViewText(R.id.buttonH, sharedPreferences.getString("buttonH", ""), "H");

        setViewText(R.id.sliderLText, sharedPreferences.getString("sliderL", ""), "L");
        setViewText(R.id.sliderRText, sharedPreferences.getString("sliderR", ""), "R");

        setViewVisibility(R.id.sliderLContainer, sharedPreferences.getBoolean("showSliderL", true));
        setViewVisibility(R.id.sliderRContainer, sharedPreferences.getBoolean("showSliderR", true));

        setViewVisibility(R.id.leftJoystick, sharedPreferences.getBoolean("showJoyL", true));
        setViewVisibility(R.id.rightJoystick, sharedPreferences.getBoolean("showJoyR", true));
        setViewVisibility(R.id.leftKnob, sharedPreferences.getBoolean("showJoyL", true));
        setViewVisibility(R.id.rightKnob, sharedPreferences.getBoolean("showJoyR", true));

        final int defaultSendPeriod = getResources().getInteger(R.integer.defaultSendPeriod);
        try {
            mSendPeriod = Integer.parseInt(
                    sharedPreferences.getString(
                            "sendPeriod", Integer.toString(defaultSendPeriod)));
        } catch (NumberFormatException e) {
            Toast.makeText(
                    this,
                    "Invalid preference value for send period",
                    Toast.LENGTH_SHORT).show();
            mSendPeriod = defaultSendPeriod;
        }
    }

    private void setViewText(int resourceId, String name, String defValue) {
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
        settingsButton.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(MainActivity.this, PrefsActivity.class);
            MainActivity.this.startActivity(settingsIntent);
        });
    }

    private void populatePairedDevices() {
        mRemoteDevices = ConnectionFactory.getRemoteDevices(this, this::onConnectionError);
        final List<String> names = Arrays.asList(mRemoteDevices
                .stream()
                .map(remoteDevice -> remoteDevice.name)
                .toArray(String[]::new));

        final ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item,
                        names);

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDeviceSelection.setAdapter(spinnerAdapter);

        resetConnectButton();
        resetOtherInputs();
    }

    private void scheduleSend() {
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                () -> {
                    if (mSerialConnection == null || ! mSerialConnection.isConnected())
                        return;

                    final Packet packet = new Packet();
                    final boolean[] allSwitchesState = Booleans.concat(mSwitchesState, mButtonsState);
                    assert packet.switches.length == allSwitchesState.length;
                    assert packet.sliders.length == mSliderPositions.length;
                    System.arraycopy(
                            allSwitchesState, 0, packet.switches, 0, packet.switches.length);
                    System.arraycopy(
                            mSliderPositions, 0, packet.sliders, 0, packet.sliders.length);

                    // For axes we don't have normalized to byte range values yet, so normalize here
                    packet.axes[0] = (byte)Math.round(mLeftJoyPos.x * 255 - 128);
                    packet.axes[1] = (byte)Math.round(mLeftJoyPos.y * 255 - 128);
                    packet.axes[2] = (byte)Math.round(mRightJoyPos.x * 255 - 128);
                    packet.axes[3] = (byte)Math.round(mRightJoyPos.y * 255 - 128);

                    mSerialConnection.send(mProtocol.serialize(packet));
                    scheduleSend();
                },
                mSendPeriod); // Approximately, we do not compensate for the execution time, etc
    }

    private void resetKnobWhenPadReady(ImageView padView, RelativeLayout knobView, PointF pos) {
        // We need to position knob knowing the pad width and height, but they are not
        // available during onCreate nor onResume, so we have to listen to the layout
        // listener to reset initial positions of knobs
        padView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            updateKnob(padView, knobView, pos);
            attachKnobMovement(padView, knobView, pos);
        });
    }

    private void updateKnob(ImageView padView, RelativeLayout knobView, PointF pos) {
        int maxX = padView.getWidth() - knobView.getWidth();
        int maxY = padView.getHeight() - knobView.getHeight();

        knobView.setX(padView.getX() + pos.x * maxX);
        knobView.setY(padView.getY() + (1 - pos.y) * maxY);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachKnobMovement(ImageView padView, RelativeLayout knobView, PointF output) {
        final MainActivity that = this;
        padView.setOnTouchListener((view1, motionEvent) -> {
            final int action = motionEvent.getActionMasked();
            final float width = view1.getWidth();
            final float height = view1.getHeight();

            final float knobW = (float) knobView.getWidth();
            final float knobH = (float) knobView.getHeight();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Distance from tap to center should be less than knob radius
                    final float xDist = width / 2 - motionEvent.getX();
                    final float yDist = height / 2 - motionEvent.getY();
                    if (Math.pow(xDist, 2) + Math.pow(yDist, 2) > Math.pow(knobW / 2, 2))
                        return true;

                    grabbedPads.add(padView);
                case MotionEvent.ACTION_MOVE:
                    if (! grabbedPads.contains(padView))
                        return true;

                    int[] pos = new int[2];
                    view1.getLocationOnScreen(pos);

                    // You need to tap into a knob to grab it, otherwise discard the tap down action
                    if (action == MotionEvent.ACTION_DOWN) {
                    }

                    final float x = (motionEvent.getX() - knobW / 2) / (width - knobW);
                    final float y = (motionEvent.getY() - knobH / 2) / (height - knobH);

                    output.x = clamp(x, 0, 1);
                    output.y = 1 - clamp(y, 0, 1);

                    that.updateKnob(padView, knobView, output);
                    return true;

                case MotionEvent.ACTION_UP:
                    grabbedPads.remove(padView);
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
        if (mSerialConnection != null && mSerialConnection.isConnected()) {
            allowConnection(true, "Disconnect");
            mConnect.setOnClickListener(view -> disconnect(null));
        } else {
            allowConnection(true, "Connect");
            mConnect.setOnClickListener(view -> {
                allowConnection(false, null);
                mStatus.setText("Connecting...");
                mConnect.setEnabled(false);
                connect();
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void resetOtherInputs() {
        final LinkedList<SwitchCompat> switches = new LinkedList<>();
        final LinkedList<Button> buttons = new LinkedList<>();
        final LinkedList<SeekBar> sliders = new LinkedList<>();

        switches.add(findViewById(R.id.switchA));
        switches.add(findViewById(R.id.switchB));
        switches.add(findViewById(R.id.switchC));
        switches.add(findViewById(R.id.switchD));

        buttons.add(findViewById(R.id.buttonE));
        buttons.add(findViewById(R.id.buttonF));
        buttons.add(findViewById(R.id.buttonG));
        buttons.add(findViewById(R.id.buttonH));

        sliders.add(findViewById(R.id.sliderL));
        sliders.add(findViewById(R.id.sliderR));

        mSwitchesState = new boolean[switches.size()];
        mButtonsState = new boolean[buttons.size()];
        mSliderPositions = new byte[sliders.size()];

        for (int i = 0; i < switches.size(); i++) {
            int i_ = i;
            switches.get(i).setOnCheckedChangeListener(
                    (view, isChecked) -> mSwitchesState[i_] = isChecked);
        }

        for (int i = 0; i < buttons.size(); i++) {
            int i_ = i;
            buttons.get(i).setOnTouchListener((view1, motionEvent) -> {
                final int action = motionEvent.getActionMasked();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        mButtonsState[i_] = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        mButtonsState[i_] = false;
                        break;
                }

                return false;
            });
        }

        for (int i = 0; i < sliders.size(); i++) {
            // Shift 0..255 slider range to segned byte range -128..127
            mSliderPositions[i] = (byte)(sliders.get(i).getProgress() - 128);
        }
    }

    private void setTerminalText(String text) {
        text = text.replace('\t', '\n');
        final TextView receiveTerminal = findViewById(R.id.textTerminal);
        receiveTerminal.setText(text);
    }

    private void allowConnection(boolean allow, String buttonLabel) {
        final Button connectBtn = findViewById(R.id.connect);
        final Spinner spinner = findViewById(R.id.btDevice);

        if (buttonLabel != null)
            connectBtn.setText(buttonLabel);
        connectBtn.setEnabled(allow);
        spinner.setEnabled(allow);
    }

    private void connect() {
        if (mSerialConnection != null) {
            if (mSerialConnection.isConnected()) {
                allowConnection(true, "Disconnect");
                return;
            }

            mSerialConnection = null;
        }

        final int selectedPos = mDeviceSelection.getSelectedItemPosition();

        if (selectedPos < 0) {
            resetConnectButton();
            allowConnection(true, null);
            mStatus.setText("No BT connection selected");
            return;
        }

        final ConnectionFactory.RemoteDevice selectedDevice = mRemoteDevices.get(selectedPos);
        mSerialConnection = ConnectionFactory.createConnection(
                selectedDevice,
                this::onPacketSent,
                this::onMessageReceived,
                this::onConnectionError,
                this);

        final Disposable d = mSerialConnection.connect(this::onConnected);
        mCompositeDisposable.add(d);
    }

    private void disconnect(final String message) {
        if (mSerialConnection != null) {
            if (mSerialConnection.isConnecting())
                return;

            if (mSerialConnection.isConnected())
                mSerialConnection.disconnect();

            mSerialConnection = null;
        }

        resetConnectButton();

        if (message != null) {
            mStatus.setText(message);
        } else {
            mStatus.setText("Disconnected");
            setTerminalText("");
        }
    }

    private void onConnected() {
        mStatus.setText("Connected");
        setTerminalText("");
        resetConnectButton();
        scheduleSend();
    }

    private void onPacketSent() {
        // Do nothing
    }

    private void onMessageReceived(String message) {
        setTerminalText(message);
    }

    private void onConnectionError() {
        disconnect("Connection error");
    }


    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private TextView mStatus;
    private Spinner mDeviceSelection;
    final Set<View> grabbedPads = new HashSet<>();

    private int mSendPeriod;

    // State to be sent
    private PointF mLeftJoyPos;
    private PointF mRightJoyPos;
    private boolean[] mSwitchesState;
    private boolean[] mButtonsState;
    private byte[] mSliderPositions;

    private ArrayList<ConnectionFactory.RemoteDevice> mRemoteDevices;
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private Connection mSerialConnection;
    Protocol mProtocol;
}
