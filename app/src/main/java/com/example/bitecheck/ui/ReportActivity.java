package com.example.bitecheck.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.View;

import com.example.bitecheck.R;
import com.example.bitecheck.data.local.MealDao;
import com.example.bitecheck.data.local.ProfileDao;
import com.example.bitecheck.data.local.WaterDao;
import com.example.bitecheck.data.local.WeightDao;
import com.example.bitecheck.model.UserProfile;
import com.example.bitecheck.util.DateUtil;
import com.example.bitecheck.util.SessionManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportActivity extends BaseSecondaryActivity {

    private static final int REQUEST_SEND_SMS = 41;

    private String userId;
    private String summaryText = "";
    private String pendingPhoneNumber;

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_report;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_report);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SessionManager session = new SessionManager(this);
        userId = session.getUserId() != null ? session.getUserId() : "local";

        buildReport();
        buildCharts();
        wireTabs();
        findViewById(R.id.btn_share_sms).setOnClickListener(v -> promptPhoneNumber());
    }

    private void buildReport() {
        MealDao mealDao = new MealDao(this);
        WaterDao waterDao = new WaterDao(this);
        UserProfile profile = new ProfileDao(this).get(userId);
        int target = profile != null ? profile.calorieTarget : 2000;
        String today = DateUtil.today();

        int consumed = mealDao.caloriesForDay(userId, today);
        int mealCount = mealDao.mealsForDay(userId, today).size();
        int water = waterDao.totalForDay(userId, today);

        summaryText = String.format(Locale.US,
                "BiteCheck — %s\nCalories: %d / %d kcal\nMeals logged: %d\nWater: %d ml",
                DateUtil.displayDay(today), consumed, target, mealCount, water);
        TextView todaySummary = findViewById(R.id.text_today_summary);
        todaySummary.setText(summaryText.replaceFirst("BiteCheck — .*\n", ""));
    }

    private void wireTabs() {
        TabLayout tabs = findViewById(R.id.tabs_report);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                findViewById(R.id.section_daily)
                        .setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                findViewById(R.id.section_weekly)
                        .setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                findViewById(R.id.section_monthly)
                        .setVisibility(position == 2 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    // ---- Charts (Phase 6) ------------------------------------------------

    private void buildCharts() {
        MealDao mealDao = new MealDao(this);
        WaterDao waterDao = new WaterDao(this);
        UserProfile profile = new ProfileDao(this).get(userId);
        int target = profile != null ? profile.calorieTarget : 2000;

        buildDailyBars(findViewById(R.id.chart_calories_week), 7, "EEE",
                day -> (float) mealDao.caloriesForDay(userId, day),
                R.color.bc_primary, target);
        buildDailyBars(findViewById(R.id.chart_water_week), 7, "EEE",
                day -> (float) waterDao.totalForDay(userId, day),
                R.color.bc_water, 0);
        buildDailyBars(findViewById(R.id.chart_calories_month), 30, "d",
                day -> (float) mealDao.caloriesForDay(userId, day),
                R.color.bc_primary, target);
        buildWeightLine(findViewById(R.id.chart_weight));
    }

    private interface DayValue {
        float valueFor(String day);
    }

    /** One bar per day, oldest → newest, single brand hue, recessive axes. */
    private void buildDailyBars(BarChart chart, int days, String labelPattern,
                                DayValue source, int colorRes, int targetLine) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        float max = 0;
        for (int i = days - 1; i >= 0; i--) {
            String day = DateUtil.dayNDaysAgo(i);
            float value = source.valueFor(day);
            max = Math.max(max, value);
            entries.add(new BarEntry(days - 1 - i, value));
            labels.add(shortLabel(day, labelPattern));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(ContextCompat.getColor(this, colorRes));
        dataSet.setDrawValues(false);
        dataSet.setHighLightAlpha(60);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);
        chart.setData(data);

        styleChart(chart, labels, days > 7 ? 6 : days);

        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(0f);
        if (targetLine > 0) {
            LimitLine limit = new LimitLine(targetLine,
                    getString(R.string.report_target_label));
            limit.setLineColor(ContextCompat.getColor(this, R.color.bc_secondary));
            limit.setLineWidth(1.5f);
            limit.enableDashedLine(12f, 8f, 0f);
            limit.setTextColor(ContextCompat.getColor(this, R.color.bc_text_secondary));
            limit.setTextSize(10f);
            left.addLimitLine(limit);
            left.setDrawLimitLinesBehindData(true);
            left.setAxisMaximum(Math.max(max, targetLine) * 1.15f);
        }
        chart.animateY(400);
        chart.invalidate();
    }

    private void buildWeightLine(LineChart chart) {
        String since = DateUtil.dayNDaysAgo(90);
        List<WeightDao.Row> rows = new WeightDao(this).list(userId);
        Collections.reverse(rows); // DAO returns newest-first; charts want oldest-first
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (WeightDao.Row row : rows) {
            if (row.loggedAt.compareTo(since) < 0) {
                continue;
            }
            entries.add(new Entry(entries.size(), (float) row.weightKg));
            labels.add(shortLabel(row.loggedAt.substring(0, 10), "d MMM"));
        }

        TextView noWeight = findViewById(R.id.text_no_weight);
        if (entries.size() < 2) {
            chart.setVisibility(View.GONE);
            noWeight.setVisibility(View.VISIBLE);
            return;
        }
        chart.setVisibility(View.VISIBLE);
        noWeight.setVisibility(View.GONE);

        int orange = ContextCompat.getColor(this, R.color.bc_secondary);
        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(orange);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(orange);
        dataSet.setCircleRadius(3.5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false);

        chart.setData(new LineData(dataSet));
        styleChart(chart, labels, Math.min(labels.size(), 5));
        // Weight is change-over-time: the axis hugs the data, not zero.
        chart.getAxisLeft().setSpaceTop(20f);
        chart.getAxisLeft().setSpaceBottom(20f);
        chart.animateX(400);
        chart.invalidate();
    }

    /** Shared recessive styling: no legend/description, left axis only. */
    private void styleChart(BarLineChartBase<?> chart, List<String> labels,
                            int labelCount) {
        int textColor = ContextCompat.getColor(this, R.color.bc_text_secondary);
        int gridColor = ContextCompat.getColor(this, R.color.bc_chart_grid);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.setScaleEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setExtraBottomOffset(4f);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labelCount);
        xAxis.setTextColor(textColor);
        xAxis.setTextSize(10f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));

        YAxis left = chart.getAxisLeft();
        left.setDrawAxisLine(false);
        left.setGridColor(gridColor);
        left.setGridLineWidth(1f);
        left.setTextColor(textColor);
        left.setTextSize(10f);
        left.setLabelCount(5, false);
    }

    private String shortLabel(String day, String pattern) {
        try {
            Date date = DateUtil.DAY.parse(day);
            return date == null ? day
                    : new SimpleDateFormat(pattern, Locale.US).format(date);
        } catch (java.text.ParseException e) {
            return day;
        }
    }

    /** SMS share  ask for a number, send via SmsManager. */
    private void promptPhoneNumber() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setHint(R.string.hint_phone_number);
        new AlertDialog.Builder(this)
                .setTitle(R.string.action_share_sms)
                .setView(input)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_submit, (dialog, which) -> {
                    String number = input.getText().toString().trim();
                    if (number.isEmpty()) {
                        return;
                    }
                    pendingPhoneNumber = number;
                    sendSmsWithPermission();
                })
                .show();
    }

    private void sendSmsWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            sendSms();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SEND_SMS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSms();
            } else {
                // Permission denied: open the SMS app with the text prefilled instead.
                Intent intent = new Intent(Intent.ACTION_SENDTO,
                        Uri.parse("smsto:" + pendingPhoneNumber));
                intent.putExtra("sms_body", summaryText);
                startActivity(intent);
            }
        }
    }

    private void sendSms() {
        try {
            SmsManager smsManager = getSystemService(SmsManager.class);
            smsManager.sendMultipartTextMessage(pendingPhoneNumber, null,
                    smsManager.divideMessage(summaryText), null, null);
            Toast.makeText(this, R.string.msg_sms_sent, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.msg_sms_failed, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        }
    }
}
