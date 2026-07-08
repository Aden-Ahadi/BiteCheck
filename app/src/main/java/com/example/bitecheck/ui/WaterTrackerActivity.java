package com.example.bitecheck.ui;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.bitecheck.R;
import com.example.bitecheck.data.local.WaterDao;
import com.example.bitecheck.util.DateUtil;
import com.example.bitecheck.util.SessionManager;

public class WaterTrackerActivity extends BaseSecondaryActivity {

    private static final int DAILY_GOAL_ML = 2000;

    private WaterDao waterDao;
    private String userId;

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_water;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_water);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        waterDao = new WaterDao(this);
        SessionManager session = new SessionManager(this);
        userId = session.getUserId() != null ? session.getUserId() : "local";

        findViewById(R.id.btn_add_250).setOnClickListener(v -> addWater(250));
        findViewById(R.id.btn_add_500).setOnClickListener(v -> addWater(500));
        refresh();
    }

    private void addWater(int amountMl) {
        waterDao.insert(userId, amountMl);
        refresh();
    }

    private void refresh() {
        int total = waterDao.totalForDay(userId, DateUtil.today());
        TextView totalText = findViewById(R.id.text_water_total);
        TextView goalText = findViewById(R.id.text_water_goal);
        ProgressBar progress = findViewById(R.id.progress_water);
        totalText.setText(getString(R.string.water_ml_format, total));
        goalText.setText(getString(R.string.water_goal_format, DAILY_GOAL_ML));
        progress.setMax(DAILY_GOAL_ML);
        progress.setProgress(Math.min(total, DAILY_GOAL_ML));
    }
}
