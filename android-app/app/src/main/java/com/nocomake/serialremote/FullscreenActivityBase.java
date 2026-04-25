package com.nocomake.serialremote;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import DiyRemote.R;

public class FullscreenActivityBase extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getSupportActionBar();
        if (bar != null)
            bar.hide();

        fixFullscreen();
    }

    private void fixFullscreen() {
        // Try to fill the space under the camera cutout to the same color we use for
        // background.
        // Using ContextCompat is the modern, safe way to get colors.
        final Bitmap bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(ContextCompat.getColor(this, R.color.background));
        final BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        getWindow().setBackgroundDrawable(bitmapDrawable);

        // 1. Tell Android we want to draw our app edge-to-edge (under the system bars)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 2. Get the insets controller to hide the actual system bars
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
