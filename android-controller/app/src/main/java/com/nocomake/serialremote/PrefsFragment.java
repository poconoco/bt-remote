package com.nocomake.serialremote;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import SerialRemote.R;

public class PrefsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
