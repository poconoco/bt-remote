package com.nocomake.serialremote;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
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

import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;
import com.google.common.primitives.Booleans;

import com.nocomake.serialremote.connection.Connection;
import com.nocomake.serialremote.connection.ConnectionFactory;
import com.nocomake.serialremote.protocol.Packet;
import com.nocomake.serialremote.protocol.Protocol;
import com.nocomake.serialremote.protocol.ProtocolFactory;

import DiyRemote.R;

public class MainActivity extends FullscreenActivityBase {

    private static final int BT_PERMISSION_REQUEST = 100;
    private static final String SELECTED_DEVICE_KEY = "SELECTED_DEVICE_KEY";
    private static final String SELECTED_DEVICE_TYPE_KEY = "SELECTED_DEVICE_TYPE_KEY";
    private static final String SLIDER_KEY_PREFIX = "SLIDER_KEY_PREFIX_";
    private static final String SWITCH_KEY_PREFIX = "SWITCH_KEY_PREFIX_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        mBackgroundExecutor = Executors.newSingleThreadExecutor();
        mStatus = findViewById(R.id.status);
        mRemoteStats = findViewById(R.id.remoteStats);
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

        attachJoystickWhenPadReady(findViewById(R.id.leftJoystick), findViewById(R.id.leftKnob), mLeftJoyPos);
        attachJoystickWhenPadReady(findViewById(R.id.rightJoystick), findViewById(R.id.rightKnob), mRightJoyPos);

        attachControlSwitches();
        attachControlButtons();
        attachSliders();

        initAuxButtons();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnect(null);
        saveState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyConfigurablePreferences();

        if (mSerialConnection == null ||
                (! mSerialConnection.isConnected() && ! mSerialConnection.isConnecting())) {
            checkPermissionsAndPopulateRemoteDevices();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BT_PERMISSION_REQUEST) {
            mNoPermission = false;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED)
                    mNoPermission = true;
            }

            if (mNoPermission)
                showNoBtPermissionToast();

            populateRemoteDevices();
        }
    }

    private void showNoBtPermissionToast() {
        Toast.makeText(
                this,
                "Bluetooth permission denied, BT connections disabled",
                Toast.LENGTH_LONG).show();
    }

    private void checkPermissionsAndPopulateRemoteDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                                != PackageManager.PERMISSION_GRANTED)) {

            boolean showRationale =
                    ActivityCompat.shouldShowRequestPermissionRationale(
                            this, Manifest.permission.BLUETOOTH_CONNECT);

            if (!showRationale) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN},
                        BT_PERMISSION_REQUEST);
            } else {
                mNoPermission = true;
                showNoBtPermissionToast();
                populateRemoteDevices();
            }
        } else {
            mNoPermission = false;
            populateRemoteDevices();
        }
    }

    private void saveState() {
        final ConnectionFactory.RemoteDevice remoteDevice = getSelectedRemote();
        if (remoteDevice == null)
            return;

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        int i = 0;
        for (SeekBar slider : getSliders())
            editor.putInt(SLIDER_KEY_PREFIX+i++, slider.getProgress());

        i = 0;
        for (SwitchCompat switchCtl : getControlSwitches())
            editor.putBoolean(SWITCH_KEY_PREFIX+i++, switchCtl.isChecked());

        editor.putString(SELECTED_DEVICE_KEY, remoteDevice.address);
        editor.putString(SELECTED_DEVICE_TYPE_KEY, remoteDevice.type.name());

        editor.apply();
    }

    private void loadState() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        int i = 0;
        for (SeekBar slider : getSliders())
            slider.setProgress(sharedPreferences.getInt(SLIDER_KEY_PREFIX+i++, 0));

        i = 0;
        for (SwitchCompat switchCtl : getControlSwitches())
            switchCtl.setChecked(sharedPreferences.getBoolean(SWITCH_KEY_PREFIX+i++, false));

        final String selectedDeviceType = sharedPreferences.getString(SELECTED_DEVICE_TYPE_KEY, null);
        if (selectedDeviceType != null) {
            final String selectedDeviceAddress = sharedPreferences.getString(SELECTED_DEVICE_KEY, null);

            if (selectedDeviceAddress != null) {
                for (i = 0; i < mRemoteDevices.size(); i++) {
                    final ConnectionFactory.RemoteDevice device = mRemoteDevices.get(i);
                    if ((device.type == ConnectionFactory.RemoteDevice.Type.TCP
                            && device.type.name().equals(selectedDeviceType))
                            || device.address.equals(selectedDeviceAddress)) {
                        mDeviceSelection.setSelection(i);
                        break;
                    }
                }
            }
        }
    }

    private void applyConfigurablePreferences() {
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

        final String defaultStreamURL = getResources().getString(R.string.defaultVideoStreamURL);
        mVideoStreamURL = sharedPreferences.getString("videoStreamURL", defaultStreamURL);
        mVideoStreamMJPG = sharedPreferences.getBoolean("videoStreamIsMJPEG", true);
        mVideoStreamEnabled = sharedPreferences.getBoolean("videoStreamEnabled", false)
                && mVideoStreamURL != null && !mVideoStreamURL.isEmpty();

        final boolean remoteStatsOnTop = sharedPreferences.getBoolean("remoteStatsOnTopVideo", true);

        setViewVisibility(R.id.remoteStats, remoteStatsOnTop || !mVideoStreamEnabled);
        setViewVisibility(R.id.videoPlayerStatus, mVideoStreamEnabled);

        final int defaultSendPeriod = getResources().getInteger(R.integer.defaultSendPeriod);
        mSendPeriod = Integer.parseInt(sharedPreferences.getString(
                "sendPeriod", Integer.toString(defaultSendPeriod)));

        final int defaultStatsFontSize = getResources().getInteger(R.integer.defaultStatsFontSize);
        final int statsFontSize = Integer.parseInt(sharedPreferences.getString(
                "remoteStatsFontSize", Integer.toString(defaultStatsFontSize)));

        mRemoteStats.setTextSize(TypedValue.COMPLEX_UNIT_SP, statsFontSize);
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

    private void initAuxButtons() {
        final ImageButton settingsButton = findViewById(R.id.buttonSettings);
        settingsButton.setOnClickListener(v -> {
            Intent settingsIntent = new Intent(MainActivity.this, PrefsActivity.class);
            MainActivity.this.startActivity(settingsIntent);
        });

        final ImageButton helpButton = findViewById(R.id.buttonHelp);
        helpButton.setOnClickListener(v -> {
            Intent helpIntent = new Intent(MainActivity.this, HelpActivity.class);
            MainActivity.this.startActivity(helpIntent);
        });

    }

    private void setRemoteStatus(String status) {
        mRemoteStats.setText(status);
    }

    private void populateRemoteDevices() {
        mRemoteDevices = ConnectionFactory.getRemoteDevices(this, mNoPermission);
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

        // We can load state only after populating the remote devices
        loadState();
        resetConnectButton();
    }

    private void scheduleSend() {
        new android.os.Handler(Looper.getMainLooper()).postDelayed(
                () -> {
                    if (mSerialConnection == null || ! mSerialConnection.isConnected())
                        return;

                    final Packet packet = new Packet();

                    // Ensure packet accepts the same number of variables we have
                    assert packet.axes.length == 4;
                    assert packet.switches.length == mSwitchesState.length + mButtonsState.length;
                    assert packet.sliders.length == mSliderPositions.length;

                    // Pack switches together with buttons, they are all booleans
                    final boolean[] allSwitchesState = Booleans.concat(mSwitchesState, mButtonsState);
                    System.arraycopy(
                            allSwitchesState, 0, packet.switches, 0, packet.switches.length);

                    // Normalize sliders positions which are 0..255 to signed byte
                    // which is -128..127 with center in 0
                    for (int i = 0; i < mSliderPositions.length; i++)
                        packet.sliders[i] = (byte)(mSliderPositions[i] - 128);

                    // Normalize joystick positions as well
                    packet.axes[0] = (byte)Math.round(mLeftJoyPos.x * 255 - 128);
                    packet.axes[1] = (byte)Math.round(mLeftJoyPos.y * 255 - 128);
                    packet.axes[2] = (byte)Math.round(mRightJoyPos.x * 255 - 128);
                    packet.axes[3] = (byte)Math.round(mRightJoyPos.y * 255 - 128);

                    mSerialConnection.send(mProtocol.serialize(packet));
                    scheduleSend();
                },
                mSendPeriod); // Approximately, we do not compensate for the execution time, etc
    }

    private void attachJoystickWhenPadReady(ImageView padView, RelativeLayout knobView, PointF pos) {
        // We need to position knob knowing the pad width and height, but they are not
        // available during onCreate nor onResume, so we have to listen to the layout
        // listener to reset initial positions of knobs
        padView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            updateKnob(padView, knobView, pos);
            attachJoysticks(padView, knobView, pos);
        });
    }

    private void updateKnob(ImageView padView, RelativeLayout knobView, PointF pos) {
        int maxX = padView.getWidth() - knobView.getWidth();
        int maxY = padView.getHeight() - knobView.getHeight();

        knobView.setX(padView.getX() + pos.x * maxX);
        knobView.setY(padView.getY() + (1 - pos.y) * maxY);
    }

    private void resetConnectButton() {
        final Button mConnect = findViewById(R.id.connect);

        mConnect.setEnabled(true);
        if (mSerialConnection != null && mSerialConnection.isConnected()) {
            allowConnection(true, "Disconnect");
            mConnect.setOnClickListener(view -> disconnect(null));
            allowSettings(false);
        } else {
            allowConnection(true, "Connect");
            allowSettings(true);
            mConnect.setOnClickListener(view -> {
                allowConnection(false, null);
                connectVideoStream();
                mStatus.setText("Connecting...");
                mConnect.setEnabled(false);
                allowSettings(false);
                connect();
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachJoysticks(ImageView padView, RelativeLayout knobView, PointF output) {
        final MainActivity that = this;
        padView.setOnTouchListener((view1, motionEvent) -> {
            final int action = motionEvent.getActionMasked();
            final float width = view1.getWidth();
            final float height = view1.getHeight();

            final float knobW = (float) knobView.getWidth();
            final float knobH = (float) knobView.getHeight();

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // When finger is up, user needs to grab the knob by start a tap
                    // on it, not elsewhere in the joystick area

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

    private LinkedList<SwitchCompat> getControlSwitches() {
        final LinkedList<SwitchCompat> switches = new LinkedList<>();

        switches.add(findViewById(R.id.switchA));
        switches.add(findViewById(R.id.switchB));
        switches.add(findViewById(R.id.switchC));
        switches.add(findViewById(R.id.switchD));

        return switches;
    }

    private void attachControlSwitches() {
        final LinkedList<SwitchCompat> switches = getControlSwitches();
        mSwitchesState = new boolean[switches.size()];

        for (int i = 0; i < switches.size(); i++) {
            int i_ = i;
            switches.get(i).setOnCheckedChangeListener(
                    (view, isChecked) -> mSwitchesState[i_] = isChecked);
        }
    }

    private LinkedList<Button> getControlButtons() {
        final LinkedList<Button> buttons = new LinkedList<>();

        buttons.add(findViewById(R.id.buttonE));
        buttons.add(findViewById(R.id.buttonF));
        buttons.add(findViewById(R.id.buttonG));
        buttons.add(findViewById(R.id.buttonH));

        return buttons;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachControlButtons() {
        final LinkedList<Button> buttons = getControlButtons();
        mButtonsState = new boolean[buttons.size()];

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
    }

    private LinkedList<SeekBar> getSliders() {
        final LinkedList<SeekBar> sliders = new LinkedList<>();

        sliders.add(findViewById(R.id.sliderL));
        sliders.add(findViewById(R.id.sliderR));

        return sliders;
    }

    private void attachSliders() {
        final LinkedList<SeekBar> sliders = getSliders();
        mSliderPositions = new byte[sliders.size()];

        for (int i = 0; i < sliders.size(); i++) {
            final SeekBar slider = sliders.get(i);
            int i_ = i;

            // Reset initial position
            mSliderPositions[i_] = (byte)(slider.getProgress());
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar slider, int progress, boolean fromUser) {
                    // Update position when slider moves
                    mSliderPositions[i_] = (byte)(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    private void allowConnection(boolean allow, String buttonLabel) {
        final Button connectBtn = findViewById(R.id.connect);
        final Spinner spinner = findViewById(R.id.btDevice);

        if (buttonLabel != null)
            connectBtn.setText(buttonLabel);
        connectBtn.setEnabled(allow);
        spinner.setEnabled(allow);
    }

    private void allowSettings(boolean allow) {
        final ImageButton settingsButton = findViewById(R.id.buttonSettings);
        settingsButton.setVisibility(allow ? View.VISIBLE : View.INVISIBLE);
        mDeviceSelection.setEnabled(allow);
    }

    private ConnectionFactory.RemoteDevice getSelectedRemote() {
        final int selectedPos = mDeviceSelection.getSelectedItemPosition();

        if (selectedPos < 0) {
            resetConnectButton();
            allowConnection(true, null);
            mStatus.setText("No remote device selected");
            return null;
        }

        return mRemoteDevices.get(selectedPos);
    }

    private void connect() {
        if (mSerialConnection != null) {
            if (mSerialConnection.isConnected()) {
                allowConnection(true, "Disconnect");
                return;
            }

            mSerialConnection = null;
        }

        final ConnectionFactory.RemoteDevice selectedDevice = getSelectedRemote();
        if (selectedDevice == null)
            return;

        mSerialConnection = ConnectionFactory.createConnection(
                selectedDevice,
                this::onMessageReceived,
                this::onConnectionError,
                this);

        if (mSerialConnection == null) {
            onConnectionError();
            return;
        }

        final Disposable d = mSerialConnection.connect(this::onConnected);
        if (d != null)
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

        if (message != null) {
            mStatus.setText(message);
        } else {
            mStatus.setText("Disconnected");
        }

        setRemoteStatus("");
        disconnectVideoStream(this::resetConnectButton);
    }

    private void onConnected() {
        mStatus.setText("Connected");
        setRemoteStatus("");
        resetConnectButton();
        scheduleSend();
    }

    private void connectVideoStream() {
        if (! mVideoStreamEnabled)
            return;

        if (mVideoStreamMJPG)
            connectViewStreamMjpeg();
        else
            connectVideoStreamExoPlayer();

    }

    private void connectViewStreamMjpeg() {
        final TextView status = findViewById(R.id.videoPlayerStatus);
        mMjpegPlayerView = findViewById(R.id.videoPlayerMJPEG);
        mMjpegPlayerView.getSurfaceView().setVisibility(View.VISIBLE);
        final int timeout = 5;  // Seconds

        status.setText("Connecting...");
        Mjpeg.newInstance()
                .open(mVideoStreamURL, timeout)
                .subscribe(inputStream -> {
                    status.setText("");
                    mMjpegPlayerView.setSource(inputStream);
                    mMjpegPlayerView.setDisplayMode(DisplayMode.BEST_FIT);
                },
                error -> {
                    status.setText("Stream connection error: "+error);
                    if (mMjpegPlayerView != null)
                        mMjpegPlayerView.getSurfaceView().setVisibility(View.GONE);
                });
    }

    private void connectVideoStreamExoPlayer() {
        final TextView status = findViewById(R.id.videoPlayerStatus);
        status.setText("Connecting...");

        mExoPlayer = new ExoPlayer.Builder(this).build();
        mExoPlayerView = findViewById(R.id.videoPlayerView);
        mExoPlayerView.setPlayer(mExoPlayer);
        mExoPlayerView.setVisibility(View.VISIBLE);

        // Hide UI buttons inside the player
        mExoPlayerView.setUseController(false);

        mExoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_READY:
                        status.setText("");
                        break;
                    case Player.STATE_BUFFERING:
                        status.setText("Buffering...");
                        break;
                    case Player.STATE_ENDED:
                        status.setText("Stream ended");
                        break;
                    case Player.STATE_IDLE:
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                mExoPlayerView.setVisibility(View.GONE);
                status.setText("Stream connection error: "+error);
            }
        });

        MediaItem mediaItem = MediaItem.fromUri(mVideoStreamURL);
        mExoPlayer.setMediaItem(mediaItem);
        mExoPlayer.prepare();
        mExoPlayer.play();
    }

    private void disconnectVideoStream(Runnable completion) {
        if (! mVideoStreamEnabled) {
            completion.run();
            return;
        }

        if (mExoPlayer != null) {
            mExoPlayer.stop();
            mExoPlayer.release();
            mExoPlayer = null;
            mExoPlayerView.setPlayer(null);
            mExoPlayerView.setVisibility(View.GONE);
            completion.run();
            return;
        }

        final TextView status = findViewById(R.id.videoPlayerStatus);
        if (mMjpegPlayerView != null) {
            status.setText("Disconnecting...");
            mBackgroundExecutor.execute(() -> {
                if (mMjpegPlayerView != null) {
                    try {
                        mMjpegPlayerView.stopPlayback();
                        mMjpegPlayerView.clearStream();
                    } catch (Exception e) {
                        Log.e(TAG, "error stopping playback", e);
                    }
                }

                runOnUiThread(() -> {
                    if (mMjpegPlayerView != null) {
                        mMjpegPlayerView.getSurfaceView().setVisibility(View.GONE);
                        mMjpegPlayerView = null;
                    }
                    status.setText("");
                    completion.run();
                });
            });
        }
    }

    private void onMessageReceived(String message) {
        if (mSerialConnection != null && mSerialConnection.isConnected())
            setRemoteStatus(message);
    }

    private void onConnectionError() {
        disconnect("Connection error");
    }


    private float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    private boolean mNoPermission;
    private TextView mStatus;
    private TextView mRemoteStats;
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
    private Protocol mProtocol;

    private boolean mVideoStreamEnabled;
    private boolean mVideoStreamMJPG;
    private String mVideoStreamURL;
    MjpegView mMjpegPlayerView;
    private PlayerView mExoPlayerView;
    private ExoPlayer mExoPlayer;
    ExecutorService mBackgroundExecutor;
    private static final String TAG = MainActivity.class.getSimpleName();

}
