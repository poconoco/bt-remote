package com.nocomake.serialremote;

import android.os.Bundle;

import DiyRemote.R;

public class PrefsActivity extends FullscreenActivityBase {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.prefs_activity);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, new PrefsFragment())
                .commit();
    }
}
