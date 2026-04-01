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

import static com.drnoob.datamonitor.Common.UTCToLocal;
import static com.drnoob.datamonitor.Common.cancelDataPlanNotification;
import static com.drnoob.datamonitor.Common.dismissOnClick;
import static com.drnoob.datamonitor.Common.localToUTC;
import static com.drnoob.datamonitor.Common.setBoldSpan;
import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_UNIT_BINARY;
import static com.drnoob.datamonitor.core.Values.TIME_FORMAT_24H;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_RESTART;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_RECURRING;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_START_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_TYPE;
import static com.drnoob.datamonitor.core.Values.LIMIT;
import static com.drnoob.datamonitor.core.Values.UNLIMITED_TIME_SLOT_ENABLED;
import static com.drnoob.datamonitor.core.Values.UNLIMITED_TIME_SLOT_START_HOUR;
import static com.drnoob.datamonitor.core.Values.UNLIMITED_TIME_SLOT_START_MIN;
import static com.drnoob.datamonitor.core.Values.UNLIMITED_TIME_SLOT_END_HOUR;
import static com.drnoob.datamonitor.core.Values.UNLIMITED_TIME_SLOT_END_MIN;
import static com.drnoob.datamonitor.core.Values.UNLIMITED_TIME_SLOT_START_ENABLED;
import static com.drnoob.datamonitor.core.Values.UNLIMITED_TIME_SLOT_END_ENABLED;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.databinding.FragmentDataPlanBinding;
import com.drnoob.datamonitor.utils.DataUsageMonitor;
import com.drnoob.datamonitor.utils.NotificationService;
import com.drnoob.datamonitor.utils.VibrationUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.elevation.SurfaceColors;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

public class DataPlanFragment extends Fragment {
    public static final String TAG = DataPlanFragment.class.getSimpleName();

    private static final int TYPE_PLAN_START = 0;
    private static final int TYPE_PLAN_END = 1;

    FragmentDataPlanBinding binding;

    private Long planStartDateMillis, planEndDateMillis,
            originalPlanStartDateMillis, originalPlanEndDateMillis;
    private int startHour, startMinute, endHour, endMinute;
    private long startMillis, endMillis; // Absolute start and end time in millis
    private boolean is12HourView, isRecurring;
    
    // Unlimited time slot variables
    private boolean isUnlimitedTimeSlotEnabled;
    private boolean isUnlimitedStartTimeEnabled;
    private boolean isUnlimitedEndTimeEnabled;
    private int unlimitedStartHour, unlimitedStartMinute, unlimitedEndHour, unlimitedEndMinute;
    private static final int TYPE_UNLIMITED_START = 2;
    private static final int TYPE_UNLIMITED_END = 3;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDataPlanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.containerToolbar);
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setTitle(requireContext().getString(R.string.title_add_data_plan));
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setDisplayHomeAsUpEnabled(true);
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                .setDisplayShowHomeEnabled(true);
        binding.containerToolbar.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        binding.toolbarSave.setVisibility(View.VISIBLE);

        isRecurring = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(DATA_RESET_CUSTOM_RECURRING, false);
        binding.recurringSwitch.setChecked(isRecurring);

        binding.recurringSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isRecurring = isChecked;
            if (isChecked) {
                updateDatesForRecurring();
            } else {
                restoreOriginalDates();
            }
        });

        Calendar calendar = Calendar.getInstance();
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Read time format preference (true = 24h, false = 12h)
        boolean use24h = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(TIME_FORMAT_24H, true);
        is12HourView = !use24h;

        try {
            planStartDateMillis = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getLong(DATA_RESET_CUSTOM_DATE_START, UTCToLocal(MaterialDatePicker.todayInUtcMilliseconds()));
            planEndDateMillis = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getLong(DATA_RESET_CUSTOM_DATE_END, UTCToLocal(MaterialDatePicker.todayInUtcMilliseconds()));
        }
        catch (ClassCastException e) {
            int planStartIntValue = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt(DATA_RESET_CUSTOM_DATE_START, -1);
            int planEndIntValue = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt(DATA_RESET_CUSTOM_DATE_END, -1);
            planStartDateMillis = ((Number) planStartIntValue).longValue();
            planEndDateMillis = ((Number) planEndIntValue).longValue();
        }

        originalPlanStartDateMillis = planStartDateMillis;
        originalPlanEndDateMillis = planEndDateMillis;

        startHour = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(DATA_RESET_CUSTOM_DATE_START_HOUR, -1);
        startMinute = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(DATA_RESET_CUSTOM_DATE_START_MIN, -1);
        endHour = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(DATA_RESET_CUSTOM_DATE_END_HOUR, -1);
        endMinute = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(DATA_RESET_CUSTOM_DATE_END_MIN, -1);

        if (startHour < 0 || startMinute < 0 || endHour < 0 || endMinute < 0) {
            startHour = 0;
            startMinute = 0;
            endHour = 23;
            endMinute = 59;
        }

        // Initialize unlimited time slot settings
        isUnlimitedTimeSlotEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean(UNLIMITED_TIME_SLOT_ENABLED, false);
        isUnlimitedStartTimeEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean(UNLIMITED_TIME_SLOT_START_ENABLED, false);
        isUnlimitedEndTimeEnabled = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getBoolean(UNLIMITED_TIME_SLOT_END_ENABLED, false);
        unlimitedStartHour = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(UNLIMITED_TIME_SLOT_START_HOUR, 0);
        unlimitedStartMinute = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(UNLIMITED_TIME_SLOT_START_MIN, 0);
        unlimitedEndHour = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(UNLIMITED_TIME_SLOT_END_HOUR, 23);
        unlimitedEndMinute = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getInt(UNLIMITED_TIME_SLOT_END_MIN, 59);

        updateDateViews();

        binding.customStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.setTimeInMillis(new Date().getTime());
                calendar.add(Calendar.YEAR, -2);
                long startYear = calendar.getTimeInMillis();
                calendar.add(Calendar.YEAR, 2);
                long endYear = calendar.getTimeInMillis();

                CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                        .setStart(startYear)
                        .setEnd(endYear)
                        .setValidator(DateValidatorPointBackward.now());

                MaterialDatePicker<Long> startDatePicker =
                        MaterialDatePicker.Builder.datePicker()
                                .setSelection(localToUTC(planStartDateMillis))
                                .setTitleText(getContext().getString(R.string.label_select_start_date))
                                .setCalendarConstraints(constraintsBuilder.build())
                                .build();


                startDatePicker.addOnPositiveButtonClickListener(selection -> {
                    long selectionMillis = UTCToLocal(selection);
                    originalPlanStartDateMillis = selectionMillis;
                    planStartDateMillis = selectionMillis;

                    if (isRecurring) {
                        updateDatesForRecurring();
                    } else {
                        updateDateViews();
                    }
                });

                startDatePicker.show(getChildFragmentManager(), startDatePicker.toString());
            }
        });

        binding.customEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.setTimeInMillis(new Date().getTime());
//                            calendar.add(Calendar.YEAR, -2);
                long startYear = calendar.getTimeInMillis();
                calendar.add(Calendar.YEAR, 2);
                long endYear = calendar.getTimeInMillis();

                CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                        .setStart(startYear)
                        .setEnd(endYear)
                        .setValidator(DateValidatorPointForward.from(originalPlanStartDateMillis));

                MaterialDatePicker<Long> endDatePicker =
                        MaterialDatePicker.Builder.datePicker()
                                .setSelection(localToUTC(planEndDateMillis))
                                .setTitleText(getContext().getString(R.string.label_plan_end_date))
                                .setCalendarConstraints(constraintsBuilder.build())
                                .build();

                endDatePicker.addOnPositiveButtonClickListener(selection -> {
                    long selectionMillis = UTCToLocal(selection);
                    originalPlanEndDateMillis = selectionMillis;
                    planEndDateMillis = selectionMillis;

                    if (isRecurring) {
                        updateDatesForRecurring();
                    } else {
                        updateDateViews();
                    }
                });

                endDatePicker.show(getChildFragmentManager(), endDatePicker.toString());
            }
        });

        binding.customStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(TYPE_PLAN_START);
            }
        });

        binding.customEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(TYPE_PLAN_END);
            }
        });

        binding.dataTypeSwitcher.selectTab(binding.dataTypeSwitcher.getTabAt(PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .getInt(DATA_TYPE, 0)));

        binding.dataTypeSwitcher.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (!PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getBoolean("disable_haptics", false)) {
                    VibrationUtils.hapticMinor(getContext());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        Float dataLimit = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getFloat(DATA_LIMIT, -1);
        if (dataLimit > 0) {
            int dataType = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(DATA_TYPE, 0);
            float divisor = PreferenceManager.getDefaultSharedPreferences(getContext())
                    .getBoolean(DATA_UNIT_BINARY, true) ? 1024f : 1000f;
            if (dataType == 1) { // Is GB
                String data = String.format(Locale.US, "%.2f", dataLimit / divisor);
                if (data.endsWith(".00")) {
                    data = data.substring(0, data.length() - 3);
                } else if (data.endsWith("0") && data.contains(".")) {
                    data = data.substring(0, data.length() - 1);
                }
                binding.dataLimit.setText(data);
            } else { // Is MB
                binding.dataLimit.setText(String.valueOf(dataLimit.intValue()));
            }
        }

        switch (PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(DATA_RESET, "")) {
            case DATA_RESET_MONTHLY:
                binding.dataReset.check(R.id.monthly);
                binding.customDateView.setVisibility(View.GONE);
                break;
            case DATA_RESET_DAILY:
                binding.dataReset.check(R.id.daily);
                binding.customDateView.setVisibility(View.GONE);
                break;
            case DATA_RESET_CUSTOM:
                binding.dataReset.check(R.id.custom_reset);
                binding.customDateView.setAlpha(1f);
                binding.customDateView.setVisibility(View.VISIBLE);
                break;
        }

        binding.dataReset.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.custom_reset) {
                    binding.customDateView.setAlpha(0f);
                    binding.customDateView.setVisibility(View.VISIBLE);
                    binding.customDateView.animate()
                            .alpha(1f)
                            .setDuration(350)
                            .start();
                }
                else {
                    binding.customDateView.animate()
                            .alpha(0f)
                            .setDuration(350)
                            .start();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            binding.customDateView.setVisibility(View.GONE);
                        }
                    }, 150);

                }
            }
        });

        // Initialize unlimited time slot section visibility based on data reset type
        if (binding.dataReset.getCheckedRadioButtonId() == R.id.daily) {
            binding.unlimitedTimeSlotView.setVisibility(View.VISIBLE);
        } else {
            binding.unlimitedTimeSlotView.setVisibility(View.GONE);
        }

        // Update unlimited time slot section visibility when data reset type changes
        binding.dataReset.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.custom_reset) {
                    binding.customDateView.setAlpha(0f);
                    binding.customDateView.setVisibility(View.VISIBLE);
                    binding.customDateView.animate()
                            .alpha(1f)
                            .setDuration(350)
                            .start();
                    binding.unlimitedTimeSlotView.setVisibility(View.GONE);
                }
                else {
                    binding.customDateView.animate()
                            .alpha(0f)
                            .setDuration(350)
                            .start();
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            binding.customDateView.setVisibility(View.GONE);
                        }
                    }, 150);

                    // Show unlimited time slot view only for daily data plan
                    if (i == R.id.daily) {
                        binding.unlimitedTimeSlotView.setVisibility(View.VISIBLE);
                    } else {
                        binding.unlimitedTimeSlotView.setVisibility(View.GONE);
                    }
                }
            }
        });

        // Initialize unlimited time slot switch
        binding.unlimitedTimeSlotSwitch.setChecked(isUnlimitedTimeSlotEnabled);
        binding.unlimitedTimeSlotSettings.setVisibility(isUnlimitedTimeSlotEnabled ? View.VISIBLE : View.GONE);

        binding.unlimitedTimeSlotSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isUnlimitedTimeSlotEnabled = isChecked;
            binding.unlimitedTimeSlotSettings.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getBoolean("disable_haptics", false)) {
                VibrationUtils.hapticMinor(getContext());
            }
        });

        // Initialize unlimited start time switch
        binding.unlimitedStartTimeSwitch.setChecked(isUnlimitedStartTimeEnabled);
        binding.unlimitedStartTime.setEnabled(isUnlimitedStartTimeEnabled);
        updateUnlimitedStartTimeView();

        binding.unlimitedStartTimeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isUnlimitedStartTimeEnabled = isChecked;
            binding.unlimitedStartTime.setEnabled(isChecked);
            if (!PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getBoolean("disable_haptics", false)) {
                VibrationUtils.hapticMinor(getContext());
            }
        });

        // Initialize unlimited end time switch
        binding.unlimitedEndTimeSwitch.setChecked(isUnlimitedEndTimeEnabled);
        binding.unlimitedEndTime.setEnabled(isUnlimitedEndTimeEnabled);
        updateUnlimitedEndTimeView();

        binding.unlimitedEndTimeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isUnlimitedEndTimeEnabled = isChecked;
            binding.unlimitedEndTime.setEnabled(isChecked);
            if (!PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getBoolean("disable_haptics", false)) {
                VibrationUtils.hapticMinor(getContext());
            }
        });

        // Set click listeners for unlimited time slot time pickers
        binding.unlimitedStartTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(TYPE_UNLIMITED_START);
            }
        });

        binding.unlimitedEndTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(TYPE_UNLIMITED_END);
            }
        });

        binding.toolbarSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String previousPlanType = PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getString(DATA_RESET, "null");
                // Data limit is now optional - if empty, treat as unlimited
                String dataLimitText = binding.dataLimit.getText().toString().trim();
                boolean isUnlimited = dataLimitText.isEmpty();
                
                // The planStartDateMillis and planEndDateMillis are already correctly set by the interactive logic.
                // We just need to get the absolute start/end millis for the final validation check.
                String startDate = new SimpleDateFormat("yyyy/MM/dd").format(planStartDateMillis);
                String endDate = new SimpleDateFormat("yyyy/MM/dd").format(planEndDateMillis);
                String start = startDate + " " + startHour + ":" + startMinute + ":00";
                String end = endDate + " " + endHour + ":" + endMinute + ":00";

                Date date;
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                try {
                    date = dateFormat.parse(start);
                    startMillis = date.getTime();
                    date = dateFormat.parse(end);
                    endMillis = date.getTime();
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }

                // Validation: If not recurring, end date must not be in the past.
                // Skip this validation for unlimited plans
                if (!isUnlimited && binding.dataReset.getCheckedRadioButtonId() == R.id.custom_reset &&
                        !binding.recurringSwitch.isChecked() &&
                        endMillis < System.currentTimeMillis()) {
                    Snackbar snackbar = Snackbar.make(binding.getRoot(),
                            requireContext().getString(R.string.error_invalid_plan_duration),
                            Snackbar.LENGTH_SHORT);
                    dismissOnClick(snackbar);
                    snackbar.show();
                }
                else {
                    Float dataLimit = 0f;
                    int dataType = 0;
                    
                    if (!isUnlimited) {
                        if (dataLimitText.contains(",")) {
                            dataLimitText = dataLimitText.replace(",", ".");
                        }
                        if (dataLimitText.contains("٫")) {
                            dataLimitText = dataLimitText.replace("٫", ".");
                        }
                        dataLimit = Float.parseFloat(dataLimitText);
                        float divisor = PreferenceManager.getDefaultSharedPreferences(getContext())
                                .getBoolean(DATA_UNIT_BINARY, true) ? 1024f : 1000f;
                        if (binding.dataTypeSwitcher.getTabAt(0).isSelected()) {
                            if (dataLimit >= divisor) {
                                dataType = 1;
                            } else {
                                dataLimit = dataLimit;
                                dataType = binding.dataTypeSwitcher.getSelectedTabPosition();
                            }
                        }
                        else {
                            dataLimit = dataLimit * divisor;
                            dataType = binding.dataTypeSwitcher.getSelectedTabPosition();
                        }
                    }
                    if (binding.dataReset.getCheckedRadioButtonId() == R.id.daily) {
                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                .putString(DATA_RESET, DATA_RESET_DAILY).apply();
                    }
                    else if (binding.dataReset.getCheckedRadioButtonId() == R.id.monthly) {
                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                .putString(DATA_RESET, DATA_RESET_MONTHLY).apply();
                    }
                    else if (binding.dataReset.getCheckedRadioButtonId() == R.id.custom_reset) {
                        calendar.setTimeInMillis(planEndDateMillis);
                        calendar.add(Calendar.DATE, 1);

                        PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                                .putString(DATA_RESET, DATA_RESET_CUSTOM)
                                .putLong(DATA_RESET_CUSTOM_DATE_START, planStartDateMillis)
                                .putLong(DATA_RESET_CUSTOM_DATE_END, planEndDateMillis)
                                .putLong(DATA_RESET_CUSTOM_DATE_RESTART, calendar.getTimeInMillis())
                                .putBoolean(DATA_RESET_CUSTOM_RECURRING, binding.recurringSwitch.isChecked())
                                .apply();
                    }

                    PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                            .putFloat(DATA_LIMIT, isUnlimited ? 0f : dataLimit)
                            .putString(LIMIT, isUnlimited ? "" : binding.dataLimit.getText().toString())
                            .putInt(DATA_TYPE, dataType)
                            .putInt(DATA_RESET_CUSTOM_DATE_START_HOUR, startHour)
                            .putInt(DATA_RESET_CUSTOM_DATE_START_MIN, startMinute)
                            .putInt(DATA_RESET_CUSTOM_DATE_END_HOUR, endHour)
                            .putInt(DATA_RESET_CUSTOM_DATE_END_MIN, endMinute)
                            .putBoolean(UNLIMITED_TIME_SLOT_ENABLED, isUnlimitedTimeSlotEnabled)
                            .putBoolean(UNLIMITED_TIME_SLOT_START_ENABLED, isUnlimitedStartTimeEnabled)
                            .putBoolean(UNLIMITED_TIME_SLOT_END_ENABLED, isUnlimitedEndTimeEnabled)
                            .putInt(UNLIMITED_TIME_SLOT_START_HOUR, unlimitedStartHour)
                            .putInt(UNLIMITED_TIME_SLOT_START_MIN, unlimitedStartMinute)
                            .putInt(UNLIMITED_TIME_SLOT_END_HOUR, unlimitedEndHour)
                            .putInt(UNLIMITED_TIME_SLOT_END_MIN, unlimitedEndMinute)
                            .apply();

                    if (previousPlanType.equals(DATA_RESET_CUSTOM)) {
                        Log.d(TAG, "onClick: Previously set custom plan found, cancelling refresh alarm" );
                        cancelDataPlanNotification(requireContext());
                    }
                    if (PreferenceManager.getDefaultSharedPreferences(requireContext())
                            .getBoolean("data_usage_alert", false)) {
                        DataUsageMonitor.updateServiceRestart(requireContext());
                    }

                    Intent resultData = new Intent();
                    requireActivity().setResult(Activity.RESULT_OK, resultData);
                    requireActivity().finish();
                }
            }
        });

        binding.dataLimit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Clear error when user starts typing (data limit is now optional)
                binding.dataLimitView.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @SuppressLint("SimpleDateFormat")
    private void updateDatesForRecurring() {
        calculateNewDates();
        updateDateViews();
    }

    @SuppressLint("SimpleDateFormat")
    private void restoreOriginalDates() {
        planStartDateMillis = originalPlanStartDateMillis;
        planEndDateMillis = originalPlanEndDateMillis;
        updateDateViews();
    }

    @SuppressLint("SimpleDateFormat")
    private void calculateNewDates() {
        // Use temporary variables for calculation, starting from the user's chosen original dates
        long tempStartDateMillis = originalPlanStartDateMillis;
        long tempEndDateMillis = originalPlanEndDateMillis;

        // Combine date with time for accurate comparison
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(tempEndDateMillis);
        endCal.set(Calendar.HOUR_OF_DAY, endHour);
        endCal.set(Calendar.MINUTE, endMinute);
        endCal.set(Calendar.SECOND, 59);

        // Check if the calculated end time is in the past
        if (endCal.getTimeInMillis() < System.currentTimeMillis()) {
            long planDuration = originalPlanEndDateMillis - originalPlanStartDateMillis;

            // Ensure duration is positive to prevent infinite loops
            if (planDuration <= 0) {
                // If duration is invalid, just use the original dates
                planStartDateMillis = originalPlanStartDateMillis;
                planEndDateMillis = originalPlanEndDateMillis;
                return;
            }

            // Loop until the end date is in the future
            while (endCal.getTimeInMillis() < System.currentTimeMillis()) {
                tempStartDateMillis += planDuration;
                tempEndDateMillis += planDuration;

                // Update calendar with the new end date for the next loop check
                endCal.setTimeInMillis(tempEndDateMillis);
                endCal.set(Calendar.HOUR_OF_DAY, endHour);
                endCal.set(Calendar.MINUTE, endMinute);
                endCal.set(Calendar.SECOND, 59);
            }
        }

        // Update the actual plan dates that will be displayed and saved
        planStartDateMillis = tempStartDateMillis;
        planEndDateMillis = tempEndDateMillis;
    }

    @SuppressLint("SimpleDateFormat")
    private void updateDateViews() {
        String planStart = new SimpleDateFormat("dd/MM/yyyy").format(planStartDateMillis);
        String planEnd = new SimpleDateFormat("dd/MM/yyyy").format(planEndDateMillis);
        String startTime = getTime(startHour, startMinute, is12HourView);
        String endTime = getTime(endHour, endMinute, is12HourView);

        String startDateToday = getContext().getString(R.string.label_custom_start_date, planStart);
        String endDateToday = getContext().getString(R.string.label_custom_end_date, planEnd);
        String startTimeString = getContext().getString(R.string.label_custom_start_time, startTime);
        String endTimeString = getContext().getString(R.string.label_custom_end_time, endTime);

        binding.customStartDate.setText(setBoldSpan(startDateToday, planStart));
        binding.customEndDate.setText(setBoldSpan(endDateToday, planEnd));
        binding.customStartTime.setText(setBoldSpan(startTimeString, startTime));
        binding.customEndTime.setText(setBoldSpan(endTimeString, endTime));
        
        // Update unlimited time slot views
        updateUnlimitedStartTimeView();
        updateUnlimitedEndTimeView();
    }

    private void updateUnlimitedStartTimeView() {
        if (isUnlimitedStartTimeEnabled) {
            String startTime = getTime(unlimitedStartHour, unlimitedStartMinute, is12HourView);
            String startTimeString = getContext().getString(R.string.label_unlimited_start_time_set, startTime);
            binding.unlimitedStartTime.setText(setBoldSpan(startTimeString, startTime));
        } else {
            binding.unlimitedStartTime.setText(R.string.label_set_start_time);
        }
    }

    private void updateUnlimitedEndTimeView() {
        if (isUnlimitedEndTimeEnabled) {
            String endTime = getTime(unlimitedEndHour, unlimitedEndMinute, is12HourView);
            String endTimeString = getContext().getString(R.string.label_unlimited_end_time_set, endTime);
            binding.unlimitedEndTime.setText(setBoldSpan(endTimeString, endTime));
        } else {
            binding.unlimitedEndTime.setText(R.string.label_set_end_time);
        }
    }

    private void showTimePicker(int type) {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheet);
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.layout_time_picker, null);

        TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.reset_time_picker);
        ConstraintLayout footer = dialogView.findViewById(R.id.footer);
        TextView cancel = footer.findViewById(R.id.cancel);
        TextView ok = footer.findViewById(R.id.ok);

        // Set time picker to 24h mode if preference is set
        boolean use24h = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(TIME_FORMAT_24H, true);
        timePicker.setIs24HourView(use24h);

        (((LinearLayout) ((LinearLayout) timePicker.getChildAt(0)).getChildAt(0)).getChildAt(0)).setVerticalScrollBarEnabled(false);
        (((LinearLayout) ((LinearLayout) timePicker.getChildAt(0)).getChildAt(0)).getChildAt(2)).setVerticalScrollBarEnabled(false);

        if (type == TYPE_PLAN_START) {
            timePicker.setHour(startHour);
            timePicker.setMinute(startMinute);
        }
        else if (type == TYPE_PLAN_END) {
            timePicker.setHour(endHour);
            timePicker.setMinute(endMinute);
        }
        else if (type == TYPE_UNLIMITED_START) {
            timePicker.setHour(unlimitedStartHour);
            timePicker.setMinute(unlimitedStartMinute);
        }
        else if (type == TYPE_UNLIMITED_END) {
            timePicker.setHour(unlimitedEndHour);
            timePicker.setMinute(unlimitedEndMinute);
        }

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker timePicker, int i, int i1) {
                if (!PreferenceManager.getDefaultSharedPreferences(getContext())
                        .getBoolean("disable_haptics", false)) {
                    VibrationUtils.hapticMinor(getContext());
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (type == TYPE_PLAN_START) {
                    startHour = timePicker.getHour();
                    startMinute = timePicker.getMinute();
                }
                else if (type == TYPE_PLAN_END) {
                    endHour = timePicker.getHour();
                    endMinute = timePicker.getMinute();
                }
                else if (type == TYPE_UNLIMITED_START) {
                    unlimitedStartHour = timePicker.getHour();
                    unlimitedStartMinute = timePicker.getMinute();
                }
                else if (type == TYPE_UNLIMITED_END) {
                    unlimitedEndHour = timePicker.getHour();
                    unlimitedEndMinute = timePicker.getMinute();
                }

                if (isRecurring) {
                    updateDatesForRecurring();
                } else {
                    updateDateViews();
                }

                dialog.dismiss();
            }
        });

        dialog.setContentView(dialogView);
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
                FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                BottomSheetBehavior.from(bottomSheet).setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        dialog.show();
    }

    public static String getTime(int hour, int minute, boolean is12HourView) {
        String time;
        int hourOfDay;
        if (!is12HourView) {
            String formattedHour, formattedMinute;
            if (hour < 10) {
                formattedHour = "0" + hour;
            }
            else {
                formattedHour = "" + hour;
            }
            if (minute < 10) {
                formattedMinute = "0" + minute;
            }
            else {
                formattedMinute = "" + minute;
            }
            time = formattedHour + ":" + formattedMinute;
        }
        else {
            if (hour >= 12) {
                if (hour == 12) {
                    hourOfDay = 12;
                }
                else {
                    hourOfDay = (hour - 12);
                }
                if (minute < 10) {
                    time = hourOfDay + ":0" + minute + " pm";
                }
                else {
                    time = hourOfDay + ":" + minute + " pm";
                }
            }
            else {
                if (hour == 0) {
                    hourOfDay = 12;
                }
                else if (hour < 10) {
                    hourOfDay = hour;
                    if (minute < 10) {
                        time = "0" + hourOfDay + ":0" + minute + " pm";
                    }
                    else {
                        time = "0" + hourOfDay + ":" + minute + " pm";
                    }
                }
                else {
                    hourOfDay = hour;
                }
                if (hourOfDay < 10) {
                    time = "0" + hourOfDay + ":" + minute + " am";
                }
                else {
                    time = hourOfDay + ":" + minute + " am";
                }
                if (minute < 10) {
                    time = hourOfDay + ":0" + minute + " am";
                }
                else {
                    time = hourOfDay + ":" + minute + " am";
                }
            }
        }
        return time;
    }
}
