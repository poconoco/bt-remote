package com.nocomake.serialremote;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import SerialRemote.R;

public class PrefsActivity extends AppCompatActivity {
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
