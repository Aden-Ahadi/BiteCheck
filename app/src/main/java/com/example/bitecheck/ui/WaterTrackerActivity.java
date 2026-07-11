package com.example.bitecheck.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
        findViewById(R.id.btn_undo_water).setOnClickListener(v -> undoWater());
        refresh();
    }

    private void addWater(int amountMl) {
        waterDao.insert(userId, amountMl);
        refresh();
    }

    private void undoWater() {
        boolean removed = waterDao.deleteLast(userId, DateUtil.today());
        Toast.makeText(this,
                removed ? R.string.msg_water_undone : R.string.msg_water_nothing,
                Toast.LENGTH_SHORT).show();
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
        // Only offer "undo" when there's actually something logged to remove.
        findViewById(R.id.btn_undo_water).setVisibility(total > 0 ? View.VISIBLE : View.GONE);
    }
}
