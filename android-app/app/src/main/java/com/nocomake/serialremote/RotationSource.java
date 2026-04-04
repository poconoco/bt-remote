package com.nocomake.serialremote;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class RotationSource implements SensorEventListener {

    private final SensorManager mSensorManager;
    private final Sensor mRotationVectorSensor;
    private final float[] mRotationMatrix = new float[9];
    private final float[] mRemappedMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private boolean mStarted = false;
    private boolean mHasData = false;
    private float mOriginYaw = 0;
    private float mOriginPitch = 0;

    public RotationSource(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        } else {
            mRotationVectorSensor = null;
        }
    }

    public void start() {
        if (mStarted)
            return;

        if (mRotationVectorSensor != null) {
            mHasData = false;
            // SENSOR_DELAY_UI is a good balance between responsiveness and battery life.
            // Use SENSOR_DELAY_GAME if you need real-time fast updates.
            mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
            mStarted = true;
        }
    }

    public void stop() {
        if (!mStarted)
            return;

        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }

        mStarted = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
            // Remap the axes for Landscape mode
            // The top of the phone (camera) is pointing to your LEFT
            SensorManager.remapCoordinateSystem(
                mRotationMatrix,
                SensorManager.AXIS_Y,
                SensorManager.AXIS_MINUS_X,
                mRemappedMatrix
            );

            SensorManager.getOrientation(mRemappedMatrix, mOrientationAngles);

            // Save origin pitch and yaw on the first event
            if (!mHasData) {
                mOriginPitch = mOrientationAngles[1];
                mOriginYaw = mOrientationAngles[0];
            }

            mHasData = true;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nothing
    }

    public float pitch() {
        if (!mStarted || !mHasData)
            return 0;

        // Note that we need pitch/roll in landscape
        return mOrientationAngles[1] - mOriginPitch;
    }

    public float roll() {
        if (!mStarted || !mHasData)
            return 0;

        // Note that we need pitch/roll in landscape
        return mOrientationAngles[2];
    }

    public float yaw() {
        if (!mStarted || !mHasData)
            return 0;

        return mOrientationAngles[0] - mOriginYaw;
    }
}
