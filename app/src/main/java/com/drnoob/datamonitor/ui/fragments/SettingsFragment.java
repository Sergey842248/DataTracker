/*
 * Copyright (C) 2021 Dr.NooB
 *
 * This file is a part of Data Tracker <https://github.com/Sergey842248/DataTracker>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.drnoob.datamonitor.ui.fragments;

import static com.drnoob.datamonitor.core.Values.ABOUT_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.APP_LANGUAGE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.APP_THEME;
import static com.drnoob.datamonitor.core.Values.APP_THEME_SUMMARY;
import static com.drnoob.datamonitor.core.Values.CONTRIBUTORS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DIAGNOSTICS_SETTINGS_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DONATE_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DATA_UNIT_BINARY;
import static com.drnoob.datamonitor.core.Values.TIME_FORMAT_24H;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.core.base.Preference;
import com.drnoob.datamonitor.core.base.SwitchPreferenceCompat;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = SettingsFragment.class.getSimpleName();
    private Preference mAppThemePicker, mLanguagePicker, mDiagnosticsSettings,
            mAbout, mContributors, mDonate;
    private SwitchPreferenceCompat mDisableHaptics, mDataUnitBinary, mTimeFormat24h;
    private Snackbar snackbar;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.app_settings, rootKey);

        mAppThemePicker = (Preference) findPreference("app_theme");
        mLanguagePicker = (Preference) findPreference("language_picker");
        mDiagnosticsSettings = (Preference) findPreference("network_diagnostics");
        mDisableHaptics = (SwitchPreferenceCompat) findPreference("disable_haptics");
        mDataUnitBinary = (SwitchPreferenceCompat) findPreference("data_unit_binary");
        mTimeFormat24h = (SwitchPreferenceCompat) findPreference("time_format_24h");
        mAbout = (Preference) findPreference("about");
        mContributors = (Preference) findPreference("contributors");
        mDonate = (Preference) findPreference("donate");

        if (mAppThemePicker != null) {
            String themeSummary = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getString(APP_THEME_SUMMARY, getString(R.string.system_theme_summary));
            mAppThemePicker.setSummary(themeSummary);

            mAppThemePicker.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_app_theme, null);

                    RadioGroup themeGroup = dialogView.findViewById(R.id.themes_group);
                    ConstraintLayout footer = dialogView.findViewById(R.id.footer);
                    TextView cancel = footer.findViewById(R.id.cancel);
                    TextView ok = footer.findViewById(R.id.ok);

                    String theme = PreferenceManager.getDefaultSharedPreferences(getContext())
                            .getString(APP_THEME, "system");
                    if (theme.equals("dark")) {
                        themeGroup.check(R.id.theme_dark);
                    } else if (theme.equals("light")) {
                        themeGroup.check(R.id.theme_light);
                    } else {
                        themeGroup.check(R.id.theme_system);
                    }

                    cancel.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    ok.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            String theme;
                            String summary;
                            int checkedId = themeGroup.getCheckedRadioButtonId();
                            if (checkedId == R.id.theme_light) {
                                theme = "light";
                                summary = getString(R.string.light_theme_summary);
                            } else if (checkedId == R.id.theme_dark) {
                                theme = "dark";
                                summary = getString(R.string.dark_theme_summary);
                            } else {
                                theme = "system";
                                summary = getString(R.string.system_theme_summary);
                            }
                            PreferenceManager.getDefaultSharedPreferences(getContext())
                                    .edit()
                                    .putString(APP_THEME, theme)
                                    .putString(APP_THEME_SUMMARY, summary)
                                    .apply();

                            if (theme.equals("dark")) {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            } else if (theme.equals("light")) {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            } else {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                            }
                            mAppThemePicker.setSummary(summary);
                            dialog.dismiss();
                        }
                    });

                    dialog.setContentView(dialogView);
                    dialog.show();
                    return false;
                }
            });
        }

        if (mLanguagePicker != null) {
            mLanguagePicker.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    startActivity(new Intent(getContext(), ContainerActivity.class)
                            .putExtra(GENERAL_FRAGMENT_ID, APP_LANGUAGE_FRAGMENT));
                    return false;
                }
            });
        }

        if (mDiagnosticsSettings != null) {
            mDiagnosticsSettings.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    startActivity(new Intent(getContext(), ContainerActivity.class)
                            .putExtra(GENERAL_FRAGMENT_ID, DIAGNOSTICS_SETTINGS_FRAGMENT));
                    return false;
                }
            });
        }

        if (mDisableHaptics != null) {
            mDisableHaptics.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(@NonNull androidx.preference.Preference preference) {
                    boolean isChecked = PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("disable_haptics", false);
                    if (isChecked) {
                        snackbar = Snackbar.make(getView(), "Haptic feedback disabled", Snackbar.LENGTH_SHORT);
                    } else {
                        snackbar = Snackbar.make(getView(), "Haptic feedback enabled", Snackbar.LENGTH_SHORT);
                    }
                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                            .putBoolean("disable_haptics", isChecked).apply();
                    snackbar.show();
                    return false;
                }
            });
        }

        if (mDataUnitBinary != null) {
            // Set initial summary based on current state
            boolean useBinary = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getBoolean(DATA_UNIT_BINARY, true);
            mDataUnitBinary.setSummary(useBinary ?
                    getString(R.string.data_unit_binary_summary) :
                    getString(R.string.data_unit_decimal_summary));

            mDataUnitBinary.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isBinary = (Boolean) newValue;
                mDataUnitBinary.setSummary(isBinary ?
                        getString(R.string.data_unit_binary_summary) :
                        getString(R.string.data_unit_decimal_summary));

                // Convert data limit when unit changes
                float currentDataLimit = PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getFloat(DATA_LIMIT, -1);
                if (currentDataLimit > 0) {
                    float convertedDataLimit;
                    if (isBinary) {
                        // Switching to binary: convert from decimal to binary
                        convertedDataLimit = currentDataLimit * (float) Math.pow(1000.0 / 1024.0, 2);
                    } else {
                        // Switching to decimal: convert from binary to decimal
                        convertedDataLimit = currentDataLimit * (float) Math.pow(1024.0 / 1000.0, 2);
                    }
                    PreferenceManager.getDefaultSharedPreferences(getContext())
                            .edit()
                            .putFloat(DATA_LIMIT, convertedDataLimit)
                            .apply();
                }

                return true;
            });
        }

        if (mTimeFormat24h != null) {
            // Set initial summary based on current state
            boolean use24h = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getBoolean(TIME_FORMAT_24H, true);
            mTimeFormat24h.setSummary(use24h ?
                    getString(R.string.time_format_24h_summary) :
                    getString(R.string.time_format_12h_summary));

            mTimeFormat24h.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean is24h = (Boolean) newValue;
                mTimeFormat24h.setSummary(is24h ?
                        getString(R.string.time_format_24h_summary) :
                        getString(R.string.time_format_12h_summary));
                return true;
            });
        }

        if (mAbout != null) {
            mAbout.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    startActivity(new Intent(getContext(), ContainerActivity.class)
                            .putExtra(GENERAL_FRAGMENT_ID, ABOUT_FRAGMENT));
                    return false;
                }
            });
        }

        if (mContributors != null) {
            mContributors.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    startActivity(new Intent(getContext(), ContainerActivity.class)
                            .putExtra(GENERAL_FRAGMENT_ID, CONTRIBUTORS_FRAGMENT));
                    return false;
                }
            });
        }

        if (mDonate != null) {
            mDonate.setOnPreferenceClickListener(new androidx.preference.Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(androidx.preference.Preference preference) {
                    startActivity(new Intent(getContext(), ContainerActivity.class)
                            .putExtra(GENERAL_FRAGMENT_ID, DONATE_FRAGMENT));
                    return false;
                }
            });
        }


    }


    @Override
    public void onPause() {
        super.onPause();
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }
}
