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
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import com.nocomake.serialremote.conn.Connection;
import com.nocomake.serialremote.conn.ConnectionFactory;

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
        resetSwitchButtons();
    }

    private void scheduleSend() {
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                () -> {
                    if (mSerialConnection == null || ! mSerialConnection.isConnected())
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

                    mSerialConnection.send(packet.getBytes());

                    scheduleSend();
                },
                100);
    }

    private void resetKnobWhenPadReady(ImageView padView, RelativeLayout knobView, PointF pos) {
        padView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            updateKnob(padView, knobView, pos);
            attachKnobMovement(padView, knobView, pos);
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

                    final float knobW = (float) knobView.getWidth();
                    final float knobH = (float) knobView.getHeight();

                    final float x = (motionEvent.getX() - knobW / 2) / (width - knobW);
                    final float y = (motionEvent.getY() - knobH / 2) / (height - knobH);

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
        if (mSerialConnection != null && mSerialConnection.isConnected()) {
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
        @SuppressLint("UseSwitchCompatOrMaterialCode") final SwitchCompat mSwA = findViewById(R.id.switchA);
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

    private void setTerminalText(String text) {
        final TextView receiveTerminal = findViewById(R.id.textTerminal);
        receiveTerminal.setText(text);
    }

    private void allowConnection(boolean allow, String buttonLabel) {
        final Button connectBtn = findViewById(R.id.connect);
        final Spinner spinner = findViewById(R.id.btDevice);

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

    // State to be sent
    private PointF mLeftJoyPos;
    private PointF mRightJoyPos;
    private boolean mStateA;
    private boolean mStateE;

    private ArrayList<ConnectionFactory.RemoteDevice> mRemoteDevices;
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private Connection mSerialConnection;
}
