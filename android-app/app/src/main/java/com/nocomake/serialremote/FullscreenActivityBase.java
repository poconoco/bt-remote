package com.nocomake.serialremote;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import DiyRemote.R;

public class FullscreenActivityBase extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        fixFullscreen();
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

}
