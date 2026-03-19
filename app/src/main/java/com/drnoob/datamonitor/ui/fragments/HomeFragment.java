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

import static com.drnoob.datamonitor.Common.dismissOnClick;
import static com.drnoob.datamonitor.Common.formatOrdinalNumber;
import static com.drnoob.datamonitor.Common.getCurrentLocale;
import static com.drnoob.datamonitor.Common.setDataPlanNotification;
import static com.drnoob.datamonitor.Common.setRefreshAlarm;
import static com.drnoob.datamonitor.core.Values.DAILY_DATA_HOME_ACTION;
import static com.drnoob.datamonitor.core.Values.DATA_LIMIT;
import static com.drnoob.datamonitor.core.Values.DATA_PLAN_FRAGMENT;
import static com.drnoob.datamonitor.core.Values.DATA_QUOTA;
import static com.drnoob.datamonitor.core.Values.DATA_QUOTA_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_QUOTA_PERFORMED_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_QUOTA_SCHEDULED_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_QUOTA_WARNING_SHOWN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_HOUR;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_CUSTOM_DATE_END_MIN;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DAILY;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_DATE;
import static com.drnoob.datamonitor.core.Values.DATA_RESET_MONTHLY;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_SESSION;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TODAY;
import static com.drnoob.datamonitor.core.Values.DATA_USAGE_TYPE;
import static com.drnoob.datamonitor.core.Values.GENERAL_FRAGMENT_ID;
import static com.drnoob.datamonitor.core.Values.SESSION_CUSTOM;
import static com.drnoob.datamonitor.core.Values.SESSION_MONTHLY;
import static com.drnoob.datamonitor.core.Values.SESSION_TODAY;
import static com.drnoob.datamonitor.core.Values.SHOW_ADD_PLAN_BANNER;
import static com.drnoob.datamonitor.core.Values.TYPE_MOBILE_DATA;
import static com.drnoob.datamonitor.core.Values.TYPE_WIFI;
import static com.drnoob.datamonitor.ui.activities.MainActivity.setRefreshAppDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.convertGBToBytes;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.formatData;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDataLimitBytes;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDataUnitDivisor;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceMobileDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.getDeviceWifiDataUsage;
import static com.drnoob.datamonitor.utils.NetworkStatsHelper.updateOverview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.drnoob.datamonitor.R;
import com.drnoob.datamonitor.Widget.DataUsageWidget;
import com.drnoob.datamonitor.adapters.data.OverviewModel;
import com.drnoob.datamonitor.ui.activities.ContainerActivity;
import com.drnoob.datamonitor.utils.NotificationService;
import com.drnoob.datamonitor.utils.SmartDataAllocationService;
import com.drnoob.datamonitor.utils.VibrationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.skydoves.progressview.ProgressView;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class HomeFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private static final int MODE_LOAD_OVERVIEW = 0;
    private static final int MODE_REFRESH_OVERVIEW = 1;

    private LinearLayout mSetupDataPlan;
    private ConstraintLayout mGraphView;
    private MaterialButton mDismissPlanBanner, mAddDataPlan;
    private TextView mMobileDataUsage,
            mMobileDataSent,
            mMobileDataReceived,
            mWifiDataUsage,
            mWifiDataSent,
            mWifiFataReceived;
    private Long[] mobile, wifi;
    private Snackbar snackbar;
    private LinearLayout mMobileDataUsageToday, mWifiUsageToday;
    private static ProgressView mMobileMon, mMobileTue, mMobileWed, mMobileThurs, mMobileFri, mMobileSat, mMobileSun,
            mWifiMon, mWifiTue, mWifiWed, mWifiThurs, mWifiFri, mWifiSat, mWifiSun;
    private LinearLayout mMonView, mTueView, mWedView, mThursView, mFriView, mSatView, mSunView;
    private static ConstraintLayout mOverview;
    private static ConstraintLayout mOverviewLoading;
    private static ImageView mRefreshOverview;
    private static Context mContext;
    private static List<OverviewModel> mList = new ArrayList<>();
    private boolean openQuickView = false;
    private TextView mDataRemaining;
    private Long planStartDateMillis, planEndDateMillis;
    private ActivityResultLauncher<Intent> dataPlanLauncher;
    private ConstraintLayout mPlanDetailsView;
    private TextView mPlanDetailsTitle, mPlanUsage, mPlanValidity, mDailyQuota;

    private SharedPreferences preferences;

    public HomeFragment() {
        // Required empty public constructor
    }

    public boolean isOpenQuickView() {
        return openQuickView;
    }

    public void setOpenQuickView(boolean openQuickView) {
        this.openQuickView = openQuickView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

        dataPlanLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            snackbar = Snackbar.make(getActivity().findViewById(R.id.main_root),
                                            getString(R.string.label_data_plan_saved), Snackbar.LENGTH_SHORT)
                                    .setAnchorView(getActivity().findViewById(R.id.bottomNavigationView));
                            if (preferences.getString(DATA_RESET, "null").equals(DATA_RESET_CUSTOM)) {
                                if (preferences.getBoolean("auto_update_data_plan", false)) {
                                    setRefreshAlarm(requireContext());
                                } else {
                                    setDataPlanNotification(requireContext());
                                }
                            }
                            mSetupDataPlan.setVisibility(View.GONE);
                            updateDataBalance();
                            dismissOnClick(snackbar);
                            snackbar.show();
                            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getContext());
                            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getContext(), DataUsageWidget.class));
                            Intent intent = new Intent(getContext(), DataUsageWidget.class);
                            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                            boolean updateNotification = preferences.getBoolean("setup_notification", false);
                            if (updateNotification) {
                                Intent notificationIntent = new Intent(getContext(), NotificationService.NotificationUpdater.class);
                                getContext().sendBroadcast(notificationIntent);
                            }

                            preferences.edit().putBoolean(DATA_QUOTA_WARNING_SHOWN, false).apply();

                            getContext().sendBroadcast(intent);
                        }
                    }
                }
        );
    }

    @Override
    public void onAttach(@NonNull @NotNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mSetupDataPlan = view.findViewById(R.id.setup_data_plan);
        mGraphView = view.findViewById(R.id.graph_view);
        mDismissPlanBanner = view.findViewById(R.id.dismiss_add_plan_banner);
        mAddDataPlan = view.findViewById(R.id.add_data_plan);
        mMobileDataUsage = view.findViewById(R.id.mobile_data_usage);
        mMobileDataSent = view.findViewById(R.id.mobile_data_sent);
        mMobileDataReceived = view.findViewById(R.id.mobile_data_received);
        mWifiDataUsage = view.findViewById(R.id.wifi_data_usage);
        mWifiDataSent = view.findViewById(R.id.wifi_data_sent);
        mWifiFataReceived = view.findViewById(R.id.wifi_data_received);
        mMobileDataUsageToday = view.findViewById(R.id.data_usage_mobile_today);
        mWifiUsageToday = view.findViewById(R.id.data_usage_wifi_today);
        mDataRemaining = view.findViewById(R.id.home_data_remaining);
        mPlanDetailsView = view.findViewById(R.id.plan_details_view);
        mPlanDetailsTitle = view.findViewById(R.id.plan_details_title);
        mPlanUsage = view.findViewById(R.id.plan_usage_details);
        mPlanValidity = view.findViewById(R.id.plan_validity_details);
        mDailyQuota = view.findViewById(R.id.daily_quota);

        mOverview = view.findViewById(R.id.overview);
        mOverviewLoading = view.findViewById(R.id.overview_loading);

        mMobileMon = mOverview.findViewById(R.id.progress_mobile_mon);
        mMobileTue = mOverview.findViewById(R.id.progress_mobile_tue);
        mMobileWed = mOverview.findViewById(R.id.progress_mobile_wed);
        mMobileThurs = mOverview.findViewById(R.id.progress_mobile_thurs);
        mMobileFri = mOverview.findViewById(R.id.progress_mobile_fri);
        mMobileSat = mOverview.findViewById(R.id.progress_mobile_sat);
        mMobileSun = mOverview.findViewById(R.id.progress_mobile_sun);

        mWifiMon = mOverview.findViewById(R.id.progress_wifi_mon);
        mWifiTue = mOverview.findViewById(R.id.progress_wifi_tue);
        mWifiWed = mOverview.findViewById(R.id.progress_wifi_wed);
        mWifiThurs = mOverview.findViewById(R.id.progress_wifi_thurs);
        mWifiFri = mOverview.findViewById(R.id.progress_wifi_fri);
        mWifiSat = mOverview.findViewById(R.id.progress_wifi_sat);
        mWifiSun = mOverview.findViewById(R.id.progress_wifi_sun);

        mMonView = view.findViewById(R.id.view_mon);
        mTueView = view.findViewById(R.id.view_tue);
        mWedView = view.findViewById(R.id.view_wed);
        mThursView = view.findViewById(R.id.view_thurs);
        mFriView = view.findViewById(R.id.view_fri);
        mSatView = view.findViewById(R.id.view_sat);
        mSunView = view.findViewById(R.id.view_sun);

        mMonView.setOnClickListener(this);
        mTueView.setOnClickListener(this);
        mWedView.setOnClickListener(this);
        mThursView.setOnClickListener(this);
        mFriView.setOnClickListener(this);
        mSatView.setOnClickListener(this);
        mSunView.setOnClickListener(this);

        mRefreshOverview = view.findViewById(R.id.overview_refresh);

        updateData();
        updateDataBalance();
        refreshOverview();
        checkDataQuota();

        mMobileDataUsage.setSelected(true);
        mWifiDataUsage.setSelected(true);

        boolean showPlanBanner = preferences.getBoolean(SHOW_ADD_PLAN_BANNER, true);

        if (preferences.getFloat(DATA_LIMIT, -1) > 0 || !showPlanBanner) {
            mSetupDataPlan.setVisibility(View.GONE);
        } else {
            mSetupDataPlan.setVisibility(View.VISIBLE);
        }

        mRefreshOverview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRefreshOverview.animate().rotation(720).setDuration(1000)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                mRefreshOverview.setRotation(0);
                            }
                        });
//                refreshOverview();
                UpdateOverview updateOverview = new UpdateOverview(MODE_REFRESH_OVERVIEW);
                updateOverview.execute();
            }
        });


//        if (mobile != null && mobile.length > 0) {
//
//        }
//        else {
//            Log.d(TAG, "onCreateView: refreshing data");
//            updateData();
//        }

        mAddDataPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), ContainerActivity.class);
                intent.putExtra(GENERAL_FRAGMENT_ID, DATA_PLAN_FRAGMENT);
                dataPlanLauncher.launch(intent);
            }
        });

        mDismissPlanBanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSetupDataPlan.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .scaleY(0.8f)
                        .scaleX(0.8f)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                super.onAnimationStart(animation);
                                float total = mSetupDataPlan.getHeight() + 28; // just to adjust a bit of distance issue
                                mGraphView.animate()
                                        .translationY((total * -1))
                                        .setDuration(300)
                                        .setStartDelay(80)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);

                                                mGraphView.animate()
                                                        .translationY(0)
                                                        .setDuration(0)
                                                        .setListener(new AnimatorListenerAdapter() {
                                                            @Override
                                                            public void onAnimationStart(Animator animation) {
                                                                super.onAnimationStart(animation);
                                                                mSetupDataPlan.setVisibility(View.GONE);
                                                            }
                                                        })
                                                        .start();
                                            }
                                        })
                                        .start();
                            }
                        })
                        .start();

                preferences.edit()
                        .putBoolean(SHOW_ADD_PLAN_BANNER, false)
                        .apply();
            }
        });

        mMobileDataUsageToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, ContainerActivity.class);
                intent.putExtra(GENERAL_FRAGMENT_ID, DATA_USAGE_TODAY);
                intent.putExtra(DATA_USAGE_SESSION, SESSION_TODAY);
                intent.putExtra(DATA_USAGE_TYPE, TYPE_MOBILE_DATA);
                intent.putExtra(DAILY_DATA_HOME_ACTION, true);
                setRefreshAppDataUsage(true);
                startActivity(intent);
            }
        });

        mWifiUsageToday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, ContainerActivity.class);
                intent.putExtra(GENERAL_FRAGMENT_ID, DATA_USAGE_TODAY);
                intent.putExtra(DATA_USAGE_SESSION, SESSION_TODAY);
                intent.putExtra(DATA_USAGE_TYPE, TYPE_WIFI);
                intent.putExtra(DAILY_DATA_HOME_ACTION, true);
                setRefreshAppDataUsage(true);
                startActivity(intent);
            }
        });


        return view;
    }

    private void updateDataBalance() {
        Long[] mobileData = null;
        int date = preferences.getInt(DATA_RESET_DATE, 1);
        String planDetailsTitle = requireContext().getString(R.string.label_plan_details_title_unknown);
        boolean isSmartAllocationEnabled = preferences.getBoolean("smart_data_allocation", false);

        try {
            if (preferences.getString(DATA_RESET, "null")
                    .equals(DATA_RESET_MONTHLY)) {
                mobileData = getDeviceMobileDataUsage(getContext(), SESSION_MONTHLY, date);
                planDetailsTitle = requireContext().getString(R.string.label_plan_details_title_monthly);
            } else if (preferences.getString(DATA_RESET, "null")
                    .equals(DATA_RESET_DAILY)) {
                mobileData = getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1);
                planDetailsTitle = requireContext().getString(R.string.label_plan_details_title_daily);
            } else {
                mobileData = getDeviceMobileDataUsage(getContext(), SESSION_CUSTOM, -1);
                planDetailsTitle = requireContext().getString(R.string.label_plan_details_title_custom);
            }

        } catch (ParseException | RemoteException e) {
            e.printStackTrace();
        }

        Float dataLimit = preferences.getFloat(DATA_LIMIT, -1);
        if (dataLimit > 0) {
            if (preferences.getString(DATA_RESET, null)
                    .equals(DATA_RESET_DAILY)) {
//                Long total = (mobileData[2]);
//                Long limit = dataLimit.longValue() * 1048576;
//                Long remaining;
//                String remainingData;
//                if (limit > total) {
//                    remaining= limit - total;
//                    remainingData = formatData(remaining / 2, remaining / 2)[2];
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining, remainingData));
//                }
//                else {
//                    remaining= total - limit;
//                    remainingData = formatData(remaining / 2, remaining / 2)[2];
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining_used_excess, remainingData));
//                }

                Long total = (mobileData[2]);
                Long limit = convertGBToBytes(getContext(), dataLimit);
                Long remaining;
                String remainingData;
                mPlanValidity.setVisibility(View.GONE);

                if (isSmartAllocationEnabled) {
                    Float quota = preferences.getFloat(DATA_QUOTA, 0F);
                    long remainingDataBytes = 0L;
                    if (dataLimit > 0 && mobileData != null) {
                        long totalUsage = mobileData[2];
                        long limitBytes = convertGBToBytes(getContext(), dataLimit);
                        if (limitBytes > totalUsage) {
                            remainingDataBytes = limitBytes - totalUsage;
                        }
                    }

                    long dailyQuotaBytes = (long) (quota * 1024 * 1024);
                    long finalQuota = Math.min(dailyQuotaBytes, remainingDataBytes);

                    String dailyQuota = formatData(getContext(), 0L, finalQuota)[2];
                    mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                    mDailyQuota.setVisibility(View.VISIBLE);
                } else {
                    // Check if custom daily quota is set
                    float customQuota = preferences.getFloat(DATA_QUOTA_CUSTOM, -1f);
                    if (customQuota > 0) {
                        // Get today's mobile data usage
                        Long[] todayData = null;
                        try {
                            todayData = getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1);
                        } catch (ParseException | RemoteException e) {
                            e.printStackTrace();
                        }

                        if (todayData != null) {
                            long todayUsage = todayData[2]; // Total mobile data used today in bytes
                            long customQuotaBytes = (long) (customQuota * 1024 * 1024); // Custom quota in bytes

                            // Available quota = custom quota - today's usage (minimum 0)
                            long availableQuota = Math.max(0, customQuotaBytes - todayUsage);

                            String dailyQuota = formatData(getContext(), 0L, availableQuota)[2];
                            mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                            mDailyQuota.setVisibility(View.VISIBLE);
                        } else {
                            // Fallback: show the full custom quota
                            long customQuotaBytes = (long) (customQuota * 1024 * 1024);
                            String dailyQuota = formatData(getContext(), 0L, customQuotaBytes)[2];
                            mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                            mDailyQuota.setVisibility(View.VISIBLE);
                        }
                    } else {
                        mDailyQuota.setVisibility(View.GONE);
                    }
                }

                if (limit > total) {
                    remaining = limit - total;
                    remainingData = formatData(getContext(), remaining / 2, remaining / 2)[2];
                    mPlanUsage.setText(getContext().getString(R.string.label_data_remaining, remainingData));
                } else {
                    remaining = total - limit;
                    remainingData = formatData(getContext(), remaining / 2, remaining / 2)[2];
                    mPlanUsage.setText(getContext().getString(R.string.label_data_remaining_used_excess, remainingData));
                }


            } else if (preferences.getString(DATA_RESET, null)
                    .equals(DATA_RESET_MONTHLY)) {
                String validity = getPlanValidity(SESSION_MONTHLY);
                mPlanValidity.setText(validity);
                if (isSmartAllocationEnabled) {
                    Float quota = preferences.getFloat(DATA_QUOTA, 0F);
                    long remainingDataBytes = 0L;
                    if (dataLimit > 0 && mobileData != null) {
                        long totalUsage = mobileData[2];
                        long limitBytes = convertGBToBytes(getContext(), dataLimit);
                        if (limitBytes > totalUsage) {
                            remainingDataBytes = limitBytes - totalUsage;
                        }
                    }

                    long dailyQuotaBytes = (long) (quota * 1024 * 1024);
                    long finalQuota = Math.min(dailyQuotaBytes, remainingDataBytes);

                    String dailyQuota = formatData(getContext(), 0L, finalQuota)[2];
                    mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                    mDailyQuota.setVisibility(View.VISIBLE);
                } else {
                    // Check if custom daily quota is set
                    float customQuota = preferences.getFloat(DATA_QUOTA_CUSTOM, -1f);
                    if (customQuota > 0) {
                        // Get today's mobile data usage
                        Long[] todayData = null;
                        try {
                            todayData = getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1);
                        } catch (ParseException | RemoteException e) {
                            e.printStackTrace();
                        }

                        if (todayData != null) {
                            long todayUsage = todayData[2]; // Total mobile data used today in bytes
                            float divisor = getDataUnitDivisor(getContext());
                            long customQuotaBytes = (long) (customQuota * divisor * divisor); // Custom quota in bytes

                            // Available quota = custom quota - today's usage (minimum 0)
                            long availableQuota = Math.max(0, customQuotaBytes - todayUsage);

                            String dailyQuota = formatData(getContext(), 0L, availableQuota)[2];
                            mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                            mDailyQuota.setVisibility(View.VISIBLE);
                        } else {
                            // Fallback: show the full custom quota
                            float divisor = getDataUnitDivisor(getContext());
                            long customQuotaBytes = (long) (customQuota * divisor * divisor);
                            String dailyQuota = formatData(getContext(), 0L, customQuotaBytes)[2];
                            mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                            mDailyQuota.setVisibility(View.VISIBLE);
                        }
                    } else {
                        mDailyQuota.setVisibility(View.GONE);
                    }
                }
//                Long total = getDeviceMobileDataUsage(getContext(), SESSION_MONTHLY, date)[2];
                Long total = mobileData[2];
                Long limit = convertGBToBytes(getContext(), dataLimit);
                Long remaining;
                String remainingData;
                String used = formatData(0l, total)[2];
                if (limit > total) {
                    remaining = limit - total;
                    remainingData = requireContext().getString(R.string.label_data_remaining,
                            formatData(getContext(), remaining / 2, remaining / 2)[2]);
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining, remainingData));
                } else {
                    remaining = total - limit;
                    remainingData = requireContext().getString(R.string.label_data_remaining_used_excess,
                            formatData(getContext(), remaining / 2, remaining / 2)[2]);
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining_used_excess, remainingData));
                }

                String usageDetails = requireContext().getString(R.string.home_plan_usage_details, used, remainingData);
                mPlanUsage.setText(usageDetails);
            } else {
                String validity = getPlanValidity(SESSION_CUSTOM);
                mPlanValidity.setText(validity);
                if (isSmartAllocationEnabled) {
                    Float quota = preferences.getFloat(DATA_QUOTA, 0F);
                    long remainingDataBytes = 0L;
                    if (dataLimit > 0 && mobileData != null) {
                        long totalUsage = mobileData[2];
                        long limitBytes = convertGBToBytes(getContext(), dataLimit);
                        if (limitBytes > totalUsage) {
                            remainingDataBytes = limitBytes - totalUsage;
                        }
                    }

                    float divisor = getDataUnitDivisor(getContext());
                    long dailyQuotaBytes = (long) (quota * divisor * divisor);
                    long finalQuota = Math.min(dailyQuotaBytes, remainingDataBytes);

                    String dailyQuota = formatData(getContext(), 0L, finalQuota)[2];
                    mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                    mDailyQuota.setVisibility(View.VISIBLE);
                } else {
                    // Check if custom daily quota is set
                    float customQuota = preferences.getFloat(DATA_QUOTA_CUSTOM, -1f);
                    if (customQuota > 0) {
                        // Get today's mobile data usage
                        Long[] todayData = null;
                        try {
                            todayData = getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1);
                        } catch (ParseException | RemoteException e) {
                            e.printStackTrace();
                        }

                        if (todayData != null) {
                            long todayUsage = todayData[2]; // Total mobile data used today in bytes
                            float divisor = getDataUnitDivisor(getContext());
                            long customQuotaBytes = (long) (customQuota * divisor * divisor); // Custom quota in bytes

                            // Available quota = custom quota - today's usage (minimum 0)
                            long availableQuota = Math.max(0, customQuotaBytes - todayUsage);

                            String dailyQuota = formatData(getContext(), 0L, availableQuota)[2];
                            mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                            mDailyQuota.setVisibility(View.VISIBLE);
                        } else {
                            // Fallback: show the full custom quota
                            float divisor = getDataUnitDivisor(getContext());
                            long customQuotaBytes = (long) (customQuota * divisor * divisor);
                            String dailyQuota = formatData(getContext(), 0L, customQuotaBytes)[2];
                            mDailyQuota.setText(getString(R.string.label_daily_quota, dailyQuota));
                            mDailyQuota.setVisibility(View.VISIBLE);
                        }
                    } else {
                        mDailyQuota.setVisibility(View.GONE);
                    }
                }
                Long total = (mobileData[2]);
                Long limit = convertGBToBytes(getContext(), dataLimit);
                Long remaining;
                String remainingData;
                String used = formatData(0l, total)[2];
                if (limit > total) {
                    remaining = limit - total;
                    remainingData = requireContext().getString(R.string.label_data_remaining,
                            formatData(getContext(), remaining / 2, remaining / 2)[2]);
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining, remainingData));
                } else {
                    remaining = total - limit;
                    remainingData = requireContext().getString(R.string.label_data_remaining_used_excess,
                            formatData(getContext(), remaining / 2, remaining / 2)[2]);
//                    mDataRemaining.setText(getContext().getString(R.string.label_data_remaining_used_excess, remainingData));
                }
                String usageDetails = requireContext().getString(R.string.home_plan_usage_details, used, remainingData);
                mPlanUsage.setText(usageDetails);
            }

            mDataRemaining.setVisibility(View.GONE);
            mPlanDetailsView.setVisibility(View.VISIBLE);
            mPlanDetailsTitle.setText(planDetailsTitle);
        } else {
            // No data plan is set. Hide mDataRemaining view.
            mDataRemaining.setVisibility(View.GONE);
            mPlanDetailsView.setVisibility(View.GONE);
        }
    }

    /**
     * Calculates the plan validity for Monthly/Custom data plans.
     *
     * @param session The plan session. One of SESSION_MONTHLY or SESSION_CUSTOM.
     *
     * @return Plan reset date and the number of days remaining as a formatted string.
     */
    @SuppressLint("StringFormatMatches")
    private String getPlanValidity(int session) {
        String validity;
        Calendar calendar = Calendar.getInstance();
        String month, ordinal, end;
        int endDate;
        long currentTimeMillis = System.currentTimeMillis();
        long endTimeMillis;

        if (session == SESSION_MONTHLY) {
            int planReset = preferences.getInt(DATA_RESET_DATE, 1);
            calendar.set(Calendar.DAY_OF_MONTH, planReset);
            if (calendar.getTimeInMillis() < currentTimeMillis) {
                calendar.add(Calendar.MONTH, 1);
            }
            endTimeMillis = calendar.getTimeInMillis();
            month = new SimpleDateFormat("MMMM", getCurrentLocale(requireContext())).format(calendar.getTime());
            endDate = planReset;

            // Check if today is the reset date - if so, show full month duration
            Calendar todayCalendar = Calendar.getInstance();
            if (todayCalendar.get(Calendar.DAY_OF_MONTH) == planReset &&
                todayCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                todayCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
                // Today is the reset date, so this is the start of a new cycle
                // Calculate days remaining as a full month (typically 31 days)
                calendar.add(Calendar.MONTH, 1);
                endTimeMillis = calendar.getTimeInMillis();
            }
        }
        else {
            long planEndDateMillis;
            try {
                planEndDateMillis = preferences.getLong(DATA_RESET_CUSTOM_DATE_END, -1);
            }
            catch (ClassCastException e) {
                int planEndIntValue = preferences.getInt(DATA_RESET_CUSTOM_DATE_END, -1);
                planEndDateMillis = ((Number) planEndIntValue).longValue();
            }

            int planEndHour = preferences.getInt(DATA_RESET_CUSTOM_DATE_END_HOUR, 0);
            int planEndMin = preferences.getInt(DATA_RESET_CUSTOM_DATE_END_MIN, 0);

            calendar.setTimeInMillis(planEndDateMillis);
            calendar.set(Calendar.HOUR_OF_DAY, planEndHour);
            calendar.set(Calendar.MINUTE, planEndMin);

            endTimeMillis = calendar.getTimeInMillis();
            month = new SimpleDateFormat("MMMM", getCurrentLocale(requireContext())).format(calendar.getTime());
            endDate = calendar.get(Calendar.DAY_OF_MONTH);
        }

        long remainingMillis = endTimeMillis - currentTimeMillis;
        int daysRemaining = (int) Math.round((remainingMillis / (24 * 60 * 60 * 1000.0)));

        ordinal = formatOrdinalNumber(endDate, requireContext());
        end = ordinal + " " + month;
        if (daysRemaining < 0) {
            daysRemaining = 0;
        }
        String remaining;
        if (daysRemaining < 1 && remainingMillis > 0) {
            int hoursRemaining = (int) Math.round(remainingMillis / (60 * 60 * 1000.0));
            remaining = requireContext().getString(R.string.label_hours_remaining, Integer.toString(hoursRemaining));
        } else {
            remaining = requireContext().getString(R.string.label_days_remaining, Integer.toString(daysRemaining));
        }
        validity = requireContext().getString(R.string.label_plan_validity, end, remaining);
        return validity;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateData();
        updateDataBalance();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    private void updateData() {
        try {
            mobile = getDeviceMobileDataUsage(getContext(), SESSION_TODAY, 1);
            wifi = getDeviceWifiDataUsage(getContext(), SESSION_TODAY);

            String[] mobileData = formatData(mobile[0], mobile[1]);
            String[] wifiData = formatData(wifi[0], wifi[1]);
            mMobileDataUsage.setText(mobileData[2]);
            mWifiDataUsage.setText(wifiData[2]);

            String mobileDataSent = getResources().getString(R.string.home_mobile_data_sent,
                    mobileData[0]);
            String mobileDataReceived = getResources().getString(R.string.home_mobile_data_received,
                    mobileData[1]);
            String wifiDataSent = getResources().getString(R.string.home_wifi_data_sent,
                    wifiData[0]);
            String wifiDataReceived = getResources().getString(R.string.home_wifi_data_received,
                    wifiData[1]);

            mMobileDataSent.setText(mobileDataSent);
            mMobileDataReceived.setText(mobileDataReceived);
            mWifiDataSent.setText(wifiDataSent);
            mWifiFataReceived.setText(wifiDataReceived);


        } catch (ParseException | RemoteException e) {
            e.printStackTrace();
        }
    }

    private static void resetOverview() {
        mOverviewLoading.setAlpha(0.0f);
        mOverview.setAlpha(1.0f);

        OverviewModel model = null;

        for (int i = 0; i < mList.size(); i++) {
            model = mList.get(i);
            long mobileSent = (model.getTotalMobile() / 2l) * 1048576;
            long mobileReceived = mobileSent;
            long wifiSent = (model.getTotalWifi() / 2l) * 1048576;
            long wifiReceived = wifiSent;
            String data = formatData(mContext, mobileSent, mobileReceived)[2];
            String wifi = formatData(mContext, wifiSent, wifiReceived)[2];
            if (i == 0) {
                mMobileMon.setProgress((model.getTotalMobile() / 25) + 2);  // 500 MB is 20 in the progressBar, so divided by 25. Added 2 to fix margin issue
                mWifiMon.setProgress((model.getTotalWifi() / 25) + 2);
                mMobileMon.setLabelText("");
                mWifiMon.setLabelText("");
            } else if (i == 1) {
                mMobileTue.setProgress((model.getTotalMobile() / 25) + 2);
                mWifiTue.setProgress((model.getTotalWifi() / 25) + 2);
                mMobileTue.setLabelText("");
                mWifiTue.setLabelText("");
            } else if (i == 2) {
                mMobileWed.setProgress((model.getTotalMobile() / 25) + 2);
                mWifiWed.setProgress((model.getTotalWifi() / 25) + 2);
                mMobileWed.setLabelText("");
                mWifiWed.setLabelText("");
            } else if (i == 3) {
                mMobileThurs.setProgress((model.getTotalMobile() / 25) + 2);
                mWifiThurs.setProgress((model.getTotalWifi() / 25) + 2);
                mMobileThurs.setLabelText("");
                mWifiThurs.setLabelText("");
            } else if (i == 4) {
                mMobileFri.setProgress((model.getTotalMobile() / 25) + 2);
                mWifiFri.setProgress((model.getTotalWifi() / 25) + 2);
                mMobileFri.setLabelText("");
                mWifiFri.setLabelText("");
            } else if (i == 5) {
                mMobileSat.setProgress((model.getTotalMobile() / 25) + 2);
                mWifiSat.setProgress((model.getTotalWifi() / 25) + 2);
                mMobileSat.setLabelText("");
                mWifiSat.setLabelText("");
            } else if (i == 6) {
                mMobileSun.setProgress((model.getTotalMobile() / 25) + 2);
                mWifiSun.setProgress((model.getTotalWifi() / 25) + 2);
                mMobileSun.setLabelText("");
                mWifiSun.setLabelText("");
            }
        }
    }

    private static void refreshOverview() {
        if (isOverviewAvailable()) {
            mOverviewLoading.setAlpha(0.0f);
            mOverview.setAlpha(1.0f);

            OverviewModel model = null;

            // Calculate dynamic maximum value from all days for proper scaling
            long maxMobileValue = 0;
            long maxWifiValue = 0;
            
            for (int i = 0; i < mList.size(); i++) {
                model = mList.get(i);
                maxMobileValue = Math.max(maxMobileValue, model.getTotalMobile());
                maxWifiValue = Math.max(maxWifiValue, model.getTotalWifi());
            }
            
            // Use the maximum of both mobile and wifi to set scale, ensure minimum of 2000 (2GB) for consistency
            long maxValue = Math.max(Math.max(maxMobileValue, maxWifiValue), 2000L);
            
            // Round maxValue to a "nice" number for better Y-axis labels
            long roundedMaxValue = roundToNiceValue(maxValue);
            
            // Set max values for ProgressView elements to ensure proper scaling
            setProgressViewMaxValues(roundedMaxValue);
            
            // Update Y-axis labels dynamically based on roundedMaxValue
            updateYAxisLabels(roundedMaxValue);

            for (int i = 0; i < mList.size(); i++) {
                model = mList.get(i);
                long mobileSent = (model.getTotalMobile() / 2l) * 1048576;
                long mobileReceived = mobileSent;
                long wifiSent = (model.getTotalWifi() / 2l) * 1048576;
                long wifiReceived = wifiSent;
                String data = formatData(mContext, mobileSent, mobileReceived)[2];
                String wifi = formatData(mContext, wifiSent, wifiReceived)[2];
                
                // Dynamic scaling calculation using rounded max value
                float mobileProgress = calculateProgress(model.getTotalMobile(), roundedMaxValue);
                float wifiProgress = calculateProgress(model.getTotalWifi(), roundedMaxValue);
                
                if (i == 0) {
                    mMobileMon.setProgress(mobileProgress);
                    mWifiMon.setProgress(wifiProgress);
                    mMobileMon.setLabelText("");
                    mWifiMon.setLabelText("");
                } else if (i == 1) {
                    mMobileTue.setProgress(mobileProgress);
                    mWifiTue.setProgress(wifiProgress);
                    mMobileTue.setLabelText("");
                    mWifiTue.setLabelText("");
                } else if (i == 2) {
                    mMobileWed.setProgress(mobileProgress);
                    mWifiWed.setProgress(wifiProgress);
                    mMobileWed.setLabelText("");
                    mWifiWed.setLabelText("");
                } else if (i == 3) {
                    mMobileThurs.setProgress(mobileProgress);
                    mWifiThurs.setProgress(wifiProgress);
                    mMobileThurs.setLabelText("");
                    mWifiThurs.setLabelText("");
                } else if (i == 4) {
                    mMobileFri.setProgress(mobileProgress);
                    mWifiFri.setProgress(wifiProgress);
                    mMobileFri.setLabelText("");
                    mWifiFri.setLabelText("");
                } else if (i == 5) {
                    mMobileSat.setProgress(mobileProgress);
                    mWifiSat.setProgress(wifiProgress);
                    mMobileSat.setLabelText("");
                    mWifiSat.setLabelText("");
                } else if (i == 6) {
                    mMobileSun.setProgress(mobileProgress);
                    mWifiSun.setProgress(wifiProgress);
                    mMobileSun.setLabelText("");
                    mWifiSun.setLabelText("");
                }
            }
        } else {
            UpdateOverview updateOverview = new UpdateOverview(MODE_LOAD_OVERVIEW);
            updateOverview.execute();
        }
    }

    /**
     * Calculate progress for ProgressView based on value and maximum
     * @param value Current value
     * @param maxValue Maximum value for scaling
     * @return Progress value for ProgressView (0-100 range + margin)
     */
    private static float calculateProgress(long value, long maxValue) {
        if (maxValue <= 0) {
            return 2f; // Default margin
        }
        // Scale to 0-100 range, then add 2 for margin like original implementation
        float progress = (value / (float) maxValue) * 100f;
        return Math.max(progress + 2f, 2f); // Ensure minimum 2 for margin
    }

    /**
     * Round a value in MB to a "nice" number for better Y-axis display
     * Examples: 4970 MB -> 5120 MB (5GB), 3200 MB -> 4096 MB (4GB), 7500 MB -> 8192 MB (8GB)
     * @param valueInMB Value in MB to round
     * @return Rounded value in MB
     */
    private static long roundToNiceValue(long valueInMB) {
        // Convert to GB for easier calculation
        double valueInGB = valueInMB / 1024.0;
        
        // Round up to nice values
        double roundedGB;
        if (valueInGB <= 1) {
            // Round to nearest 0.5 GB
            roundedGB = Math.ceil(valueInGB * 2) / 2.0;
        } else if (valueInGB <= 5) {
            // Round to nearest 1 GB
            roundedGB = Math.ceil(valueInGB);
        } else if (valueInGB <= 10) {
            // Round to nearest 2 GB
            roundedGB = Math.ceil(valueInGB / 2.0) * 2.0;
        } else if (valueInGB <= 50) {
            // Round to nearest 5 GB
            roundedGB = Math.ceil(valueInGB / 5.0) * 5.0;
        } else if (valueInGB <= 100) {
            // Round to nearest 10 GB
            roundedGB = Math.ceil(valueInGB / 10.0) * 10.0;
        } else if (valueInGB <= 500) {
            // Round to nearest 20 GB
            roundedGB = Math.ceil(valueInGB / 20.0) * 20.0;
        } else {
            // Round to nearest 50 GB
            roundedGB = Math.ceil(valueInGB / 50.0) * 50.0;
        }
        
        // Convert back to MB
        return (long) (roundedGB * 1024);
    }

    /**
     * Set maximum values for all ProgressView elements
     * @param maxValue Maximum value to set for ProgressViews
     */
    private static void setProgressViewMaxValues(long maxValue) {
        try {
            // For ProgressView library, we work with percentages (0-100)
            // The actual max value is managed through scaling logic in calculateProgress
            // We set all ProgressView max to 100 since we work with percentage-based scaling
            int maxProgress = 100;
            
            // Set max value for all ProgressView elements to 100 for percentage calculation
            mMobileMon.setMax(maxProgress);
            mWifiMon.setMax(maxProgress);
            mMobileTue.setMax(maxProgress);
            mWifiTue.setMax(maxProgress);
            mMobileWed.setMax(maxProgress);
            mWifiWed.setMax(maxProgress);
            mMobileThurs.setMax(maxProgress);
            mWifiThurs.setMax(maxProgress);
            mMobileFri.setMax(maxProgress);
            mWifiFri.setMax(maxProgress);
            mMobileSat.setMax(maxProgress);
            mWifiSat.setMax(maxProgress);
            mMobileSun.setMax(maxProgress);
            mWifiSun.setMax(maxProgress);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting ProgressView max values: " + e.getMessage());
        }
    }

    /**
     * Update Y-axis labels dynamically based on the maximum value
     * @param maxValue Maximum value in MB for scaling
     */
    private static void updateYAxisLabels(long maxValue) {
        try {
            // Calculate appropriate GB values for Y-axis labels
            double maxGB = maxValue / 1024.0; // Convert MB to GB
            
            // Find appropriate scale intervals
            double[] intervals = {0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000};
            double selectedInterval = intervals[0];
            
            for (double interval : intervals) {
                if (maxGB <= interval * 6) { // Ensure we have at least 6 intervals
                    selectedInterval = interval;
                    break;
                }
            }
            
            // Ensure we have a reasonable minimum
            if (selectedInterval < 0.5) {
                selectedInterval = 0.5;
            }
            
            // Update the TextViews for Y-axis labels
            updateYAxisLabelTexts(selectedInterval, maxGB);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating Y-axis labels: " + e.getMessage());
        }
    }

    /**
     * Update the actual Y-axis label text views
     * @param interval Interval value in GB
     * @param maxGB Maximum value in GB
     */
    private static void updateYAxisLabelTexts(double interval, double maxGB) {
        try {
            // Get the overview view to find text views
            View overviewView = mOverview;
            if (overviewView == null) return;
            
            // Find Y-axis label text views (this assumes specific IDs, may need adjustment)
            TextView topLabel = overviewView.findViewById(R.id.label_top);
            TextView secondLabel = overviewView.findViewById(R.id.label_second); 
            TextView thirdLabel = overviewView.findViewById(R.id.label_third);
            TextView fourthLabel = overviewView.findViewById(R.id.label_fourth);
            TextView fifthLabel = overviewView.findViewById(R.id.label_fifth);
            TextView bottomLabel = overviewView.findViewById(R.id.label_bottom);
            
            // Format labels based on interval
            String topText = formatDataLabel(maxGB, true);
            String secondText = formatDataLabel(maxGB * 0.8, false);
            String thirdText = formatDataLabel(maxGB * 0.6, false);
            String fourthText = formatDataLabel(maxGB * 0.4, false);
            String fifthText = formatDataLabel(maxGB * 0.2, false);
            String bottomText = "0 GB";
            
            if (topLabel != null) topLabel.setText(topText);
            if (secondLabel != null) secondLabel.setText(secondText);
            if (thirdLabel != null) thirdLabel.setText(thirdText);
            if (fourthLabel != null) fourthLabel.setText(fourthText);
            if (fifthLabel != null) fifthLabel.setText(fifthText);
            if (bottomLabel != null) bottomLabel.setText(bottomText);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating Y-axis label texts: " + e.getMessage());
        }
    }

    /**
     * Format data label for Y-axis
     * @param gbValue Value in GB
     * @param isMax Whether this is the maximum value
     * @return Formatted string
     */
    private static String formatDataLabel(double gbValue, boolean isMax) {
        if (gbValue >= 1000) {
            return String.format("%.0f TB", gbValue / 1000);
        } else if (gbValue >= 100) {
            return String.format("%.0f GB", gbValue);
        } else if (gbValue >= 10) {
            // Show one decimal only if it's not a whole number
            if (gbValue % 1 == 0) {
                return String.format("%.0f GB", gbValue);
            } else {
                return String.format("%.1f GB", gbValue);
            }
        } else if (gbValue >= 1) {
            // For values 1-10 GB, show one decimal only if needed
            if (gbValue % 1 == 0) {
                return String.format("%.0f GB", gbValue);
            } else if ((gbValue * 10) % 1 == 0) {
                return String.format("%.1f GB", gbValue);
            } else {
                return String.format("%.2f GB", gbValue);
            }
        } else {
            // For values < 1 GB, always show decimals
            if (gbValue % 0.1 == 0) {
                return String.format("%.1f GB", gbValue);
            } else {
                return String.format("%.2f GB", gbValue);
            }
        }
    }

    private static boolean isOverviewAvailable() {
        return mList.size() > 0;
    }

    @Override
    public void onClick(View v) {
        showPopupWindow(v);
    }

    private void showPopupWindow(View anchorView) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.layout_overview_popup, null);
        TextView overviewDay = popupView.findViewById(R.id.overview_day);
        TextView overviewMobile = popupView.findViewById(R.id.overview_mobile_data);
        TextView overviewWifi = popupView.findViewById(R.id.overview_wifi);

        if (isOverviewAvailable()) {
            try {
                String[] dataUsage = getDataUsage(anchorView);
                overviewDay.setText(dataUsage[2]);
                overviewMobile.setText("Mobile: " + dataUsage[0]);
                overviewWifi.setText("WiFi: " + dataUsage[1]);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        popupView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupWidth = popupView.getMeasuredWidth();
        int popupHeight = popupView.getMeasuredHeight();

        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        int xOffset = (anchorView.getWidth() - popupWidth) / 2;
        int yOffset = -anchorView.getHeight() - popupHeight;

        popupWindow.showAsDropDown(anchorView, xOffset, yOffset);
    }

    private String[] getDataUsage(View view) {
        String mobile, wifi, day;
        int viewId = view.getId();
        int dayIndex = -1;

        if (viewId == R.id.view_mon) {
            dayIndex = 0;
            day = getString(R.string.overview_monday);
        } else if (viewId == R.id.view_tue) {
            dayIndex = 1;
            day = getString(R.string.overview_tuesday);
        } else if (viewId == R.id.view_wed) {
            dayIndex = 2;
            day = getString(R.string.overview_wednesday);
        } else if (viewId == R.id.view_thurs) {
            dayIndex = 3;
            day = getString(R.string.overview_thursday);
        } else if (viewId == R.id.view_fri) {
            dayIndex = 4;
            day = getString(R.string.overview_friday);
        } else if (viewId == R.id.view_sat) {
            dayIndex = 5;
            day = getString(R.string.overview_saturday);
        } else if (viewId == R.id.view_sun) {
            dayIndex = 6;
            day = getString(R.string.overview_sunday);
        } else {
            day = "";
        }

        if (dayIndex != -1 && mList.size() > dayIndex) {
            OverviewModel model = mList.get(dayIndex);
            long mobileSent = (model.getTotalMobile() / 2L) * 1048576;
            long mobileReceived = mobileSent;
            long wifiSent = (model.getTotalWifi() / 2L) * 1048576;
            long wifiReceived = wifiSent;
            mobile = formatData(mContext, mobileSent, mobileReceived)[2];
            wifi = formatData(mContext, wifiSent, wifiReceived)[2];
        } else {
            mobile = getString(R.string.app_data_usage_placeholder);
            wifi = getString(R.string.app_data_usage_placeholder);
        }
        return new String[]{mobile, wifi, day};
    }

    private static class UpdateOverview extends AsyncTask<Object, Object, List<OverviewModel>> {
        private int mode;

        public UpdateOverview(int mode) {
            this.mode = mode;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: update overview");
            if (mode == MODE_LOAD_OVERVIEW) {
                mOverview.animate().alpha(0.0f);
                mOverviewLoading.setAlpha(1.0f);
            } else if (mode == MODE_REFRESH_OVERVIEW) {
                mOverviewLoading.setAlpha(0.0f);
                mOverview.setAlpha(1.0f);
            }
        }

        @Override
        protected List<OverviewModel> doInBackground(Object[] objects) {
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_WEEK);
            int[] days;
            List<OverviewModel> list = null;
            if (day == Calendar.MONDAY) {
                days = new int[]{Calendar.MONDAY};
            } else if (day == Calendar.TUESDAY) {
                days = new int[]{Calendar.MONDAY, Calendar.TUESDAY};
            } else if (day == Calendar.WEDNESDAY) {
                days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY};
            } else if (day == Calendar.THURSDAY) {
                days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY};
            } else if (day == Calendar.FRIDAY) {
                days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY};
            } else if (day == Calendar.SATURDAY) {
                days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};
            } else if (day == Calendar.SUNDAY) {
                days = new int[]{Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY};
            } else {
                days = new int[]{0};
            }

            try {
                list = updateOverview(mContext, days);
                mList = list;

                Log.d(TAG, "doInBackground: " + mList.size());

            } catch (ParseException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            return list;
        }

        @Override
        protected void onPostExecute(List<OverviewModel> list) {
            super.onPostExecute(list);
            mOverview.animate().alpha(1.0f);
            mOverviewLoading.animate().alpha(0.0f);
            refreshOverview();
        }
    }

    private void checkDataQuota() {
        if (preferences.getBoolean("smart_data_allocation", false)) {
            long scheduledTime = preferences.getLong(DATA_QUOTA_SCHEDULED_RESET, 0);
            long performedTime = preferences.getLong(DATA_QUOTA_PERFORMED_RESET, 0);

            if (scheduledTime > 0 &&
                    scheduledTime < System.currentTimeMillis() &&
                    performedTime < scheduledTime) {
                Log.d(TAG, "checkDataQuota: Performing a quota reset");

                WorkManager workManager = WorkManager.getInstance(requireContext());
                workManager.cancelUniqueWork("smart_data_allocation");
                workManager.cancelUniqueWork("data_rollover");
                OneTimeWorkRequest smartDataAllocationWorkRequest = new OneTimeWorkRequest
                        .Builder(SmartDataAllocationService.class)
                        .build();

                workManager.enqueueUniqueWork(
                        "smart_data_allocation",
                        ExistingWorkPolicy.KEEP,
                        smartDataAllocationWorkRequest
                );
            }
        }
    }
}
