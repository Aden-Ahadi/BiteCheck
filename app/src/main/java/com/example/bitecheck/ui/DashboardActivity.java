package com.example.bitecheck.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.bitecheck.R;
import com.example.bitecheck.data.local.MealDao;
import com.example.bitecheck.data.local.ProfileDao;
import com.example.bitecheck.data.local.WaterDao;
import com.example.bitecheck.data.local.WeightDao;
import com.example.bitecheck.model.UserProfile;
import com.example.bitecheck.util.DateUtil;
import com.example.bitecheck.util.SessionManager;
import com.example.bitecheck.util.SyncManager;

public class DashboardActivity extends BaseNavActivity {

    private final BroadcastReceiver syncReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshDashboard();
        }
    };

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_dashboard;
    }

    @Override
    protected int getBottomNavItemId() {
        return R.id.nav_dashboard;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_dashboard);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(syncReceiver, new IntentFilter(SyncManager.ACTION_SYNC_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(syncReceiver, new IntentFilter(SyncManager.ACTION_SYNC_COMPLETE));
        }
        refreshDashboard();
        SyncManager.sync(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(syncReceiver);
    }

    private void refreshDashboard() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId() != null ? session.getUserId() : "local";
        UserProfile profile = new ProfileDao(this).get(userId);
        String today = DateUtil.today();

        int target = profile != null ? profile.calorieTarget : 2000;
        int consumed = new MealDao(this).caloriesForDay(userId, today);
        int remaining = Math.max(0, target - consumed);

        TextView greeting = findViewById(R.id.text_greeting);
        String name = profile != null && profile.name != null && !profile.name.isEmpty()
                ? profile.name : session.getDisplayName();
        greeting.setText(buildGreeting(name));
        ((TextView) findViewById(R.id.text_greeting_sub))
                .setText(buildSubtitle(consumed, target, remaining));

        TextView ringValue = findViewById(R.id.text_ring_value);
        TextView eaten = findViewById(R.id.text_calories);
        TextView targetText = findViewById(R.id.text_target);
        ringValue.setText(String.valueOf(remaining));
        eaten.setText(getString(R.string.kcal_format, consumed));
        targetText.setText(getString(R.string.kcal_format, target));
        CalorieRingView ring = findViewById(R.id.ring_calories);
        ring.setValues(consumed, target);

        int waterMl = new WaterDao(this).totalForDay(userId, today);
        TextView water = findViewById(R.id.text_water);
        water.setText(getString(R.string.water_ml_format, waterMl));

        double latestWeight = new WeightDao(this).latest(userId);
        if (latestWeight < 0 && profile != null) {
            latestWeight = profile.weightKg;
        }
        if (latestWeight > 0) {
            TextView weight = findViewById(R.id.text_weight);
            weight.setText(getString(R.string.weight_kg_format, latestWeight));
        }

        findViewById(R.id.card_water).setOnClickListener(v ->
                startActivity(new Intent(this, WaterTrackerActivity.class)));
        findViewById(R.id.card_weight).setOnClickListener(v ->
                startActivity(new Intent(this, WeightTrackerActivity.class)));
    }

    private String buildGreeting(String name) {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) greeting = getString(R.string.greeting_morning);
        else if (hour < 17) greeting = getString(R.string.greeting_afternoon);
        else greeting = getString(R.string.greeting_evening);

        if (name != null && !name.isEmpty()) {
            return getString(R.string.greeting_name_format, greeting, name.split("\\s+")[0]);
        }
        return getString(R.string.greeting_no_name_format, greeting);
    }

    private String buildSubtitle(int consumed, int target, int remaining) {
        if (consumed == 0) {
            String[] starters = {getString(R.string.sub_start_1), getString(R.string.sub_start_2), getString(R.string.sub_start_3)};
            return starters[(int) (Math.random() * starters.length)];
        }
        if (consumed <= target) {
            if (remaining == 0) return getString(R.string.sub_target_2);
            String[] progress = {getString(R.string.sub_progress_1), getString(R.string.sub_progress_2), getString(R.string.sub_progress_3)};
            return String.format(progress[(int) (Math.random() * progress.length)], remaining);
        }
        String[] over = {getString(R.string.sub_over_1), getString(R.string.sub_over_2)};
        return over[(int) (Math.random() * over.length)];
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        View more = menu.findItem(R.id.action_more).getActionView();
        if (more != null) {
            more.setOnClickListener(this::showMorePopup);
        }
        return true;
    }

    private void showMorePopup(View anchor) {
        View view = getLayoutInflater().inflate(R.layout.menu_more_popup, null);
        PopupWindow popup = new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setAnimationStyle(R.style.Animation_BiteCheck_Popup);

        view.findViewById(R.id.menu_settings).setOnClickListener(v -> {
            popup.dismiss();
            startActivity(new Intent(this, SettingsActivity.class));
        });
        view.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            popup.dismiss();
            new SessionManager(this).clear();
            startActivity(new Intent(this, WelcomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });

        int offset = (int) (8 * getResources().getDisplayMetrics().density);
        popup.showAsDropDown(anchor, -offset, offset, Gravity.END);
    }
}
