package com.nocomake.serialremote;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference;

import DiyRemote.R;

public class PrefsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        setNumericValidator("sendPeriod", "Period", 20, 1000);
    }

    private void setNumericValidator(String prefName, String name, int min, int max) {
        EditTextPreference numericPreference = findPreference(prefName);
        if (numericPreference != null) {
            numericPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                    try {
                        int value = Integer.parseInt(newValue.toString());
                        if (value >= min && value <= max) {
                            return true;
                        } else {
                            Toast.makeText(
                                    getContext(),
                                    name+" must be between "+min+" and "+max,
                                    Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(
                                getContext(),
                                "Invalid number for "+name,
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
            });
        }
    }
}
