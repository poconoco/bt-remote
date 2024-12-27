package com.nocomake.serialremote;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import android.os.Bundle;
import android.widget.Toast;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import DiyRemote.R;

public class PrefsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        setNumericValidator("sendPeriod", "Period", 20, 1000);
        setNumericValidator("ipPort", "IP port", 1, 65535);
        setStringValidator(
                "ipAddress",
                "IP address",
                Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$"));

        setStringValidator(
                "videoStreamURL",
                "stream URL",
                Pattern.compile("^(http|https|rtsp|rtmp)://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)$"));
    }

    private void setNumericValidator(String prefName, String name, int min, int max) {
        EditTextPreference numericPreference = findPreference(prefName);
        if (numericPreference != null) {
            numericPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    int value = Integer.parseInt(newValue.toString());
                    if (value >= min && value <= max) {
                        return true;
                    } else {
                        Toast.makeText(
                                getContext(),
                                name+" must be between "+min+" and "+max,
                                Toast.LENGTH_LONG).show();
                        return false;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(
                            getContext(),
                            "Invalid number for "+name,
                            Toast.LENGTH_LONG).show();
                    return false;
                }
            });
        }
    }

    private void setStringValidator(String prefName, String name, Pattern pattern) {
        EditTextPreference numericPreference = findPreference(prefName);
        if (numericPreference != null) {
            numericPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                Matcher matcher = pattern.matcher(newValue.toString());
                if (matcher.matches()) {
                    return true;
                } else {
                    Toast.makeText(
                            getContext(),
                            "Invalid value for "+name,
                            Toast.LENGTH_LONG).show();
                    return false;
                }
            });
        }
    }
}
