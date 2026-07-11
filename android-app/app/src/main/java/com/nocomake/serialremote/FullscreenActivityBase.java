package com.nocomake.serialremote;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import DiyRemote.R;

public class FullscreenActivityBase extends AppCompatActivity {

    // On devices using 3-button/2-button navigation (as opposed to gesture navigation),
    // system bars revealed by the user do not auto-hide on their own even with
    // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE, so we enforce our own timeout.
    private static final long AUTO_HIDE_DELAY_MS = 3000;

    private final Handler mHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable mHideRunnable = this::hideSystemBars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getSupportActionBar();
        if (bar != null)
            bar.hide();

        setUpFullscreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The system bars can be left shown (and drawn opaque) after returning from another
        // activity, since revealing/hiding them is a per-window state that isn't restored
        // automatically, so we need to re-hide them every time we come back to the front.
        hideSystemBars();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus)
            hideSystemBars();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHideHandler.removeCallbacks(mHideRunnable);
    }

    private void setUpFullscreen() {
        // Try to fill the space under the camera cutout to the same color we use for
        // background.
        // Using ContextCompat is the modern, safe way to get colors.
        final Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(ContextCompat.getColor(this, R.color.background));
        final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        getWindow().setBackgroundDrawable(bitmapDrawable);

        // Tell Android we want to draw our app edge-to-edge (under the system bars)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        hideSystemBars();

        // Whenever the system bars become visible again (e.g. the user tapped near the
        // screen edge to reveal them on a button-navigation device), schedule hiding them
        // again after a short timeout instead of relying on the system to do it.
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (v, insets) -> {
            mHideHandler.removeCallbacks(mHideRunnable);
            if (insets.isVisible(WindowInsetsCompat.Type.systemBars()))
                mHideHandler.postDelayed(mHideRunnable, AUTO_HIDE_DELAY_MS);
            return insets;
        });
    }

    private void hideSystemBars() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        // Hide both the status bar (top) and the navigation bar (bottom)
        controller.hide(WindowInsetsCompat.Type.systemBars());

        // BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE means that if the user swipes from the
        // edge of the screen, the system bars will appear semi-transparently for a few
        // seconds and then fade away automatically. (Perfect for games/remotes!)
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );
    }
}
