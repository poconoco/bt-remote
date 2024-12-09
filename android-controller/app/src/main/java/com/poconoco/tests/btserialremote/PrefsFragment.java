package com.poconoco.tests.btserialremote;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

import BtSerialRemote.R;

public class PrefsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);
    }
}
