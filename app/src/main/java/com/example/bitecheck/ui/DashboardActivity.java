package com.example.bitecheck.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
        refreshDashboard();
        SyncManager.sync(this);
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

    /** Time-of-day greeting with the user's first name only. */
    private String buildGreeting(String name) {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        int greetRes = hour < 12 ? R.string.greeting_morning
                : hour < 17 ? R.string.greeting_afternoon
                : R.string.greeting_evening;
        String greet = getString(greetRes);
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.greeting_no_name_format, greet);
        }
        String firstName = name.trim().split("\\s+")[0];
        return getString(R.string.greeting_name_format, greet, firstName);
    }

    /** A lively, state-aware subtitle instead of a fixed line. */
    private String buildSubtitle(int consumed, int target, int remaining) {
        int[] pool;
        if (consumed == 0) {
            pool = new int[]{R.string.sub_start_1, R.string.sub_start_2, R.string.sub_start_3};
        } else if (consumed < target) {
            pool = new int[]{R.string.sub_progress_1, R.string.sub_progress_2,
                    R.string.sub_progress_3};
        } else if (consumed <= target * 1.1) {
            pool = new int[]{R.string.sub_target_1, R.string.sub_target_2};
        } else {
            pool = new int[]{R.string.sub_over_1, R.string.sub_over_2};
        }
        int pick = pool[new java.util.Random().nextInt(pool.length)];
        // Progress lines carry the remaining-calorie number; the rest are plain.
        if (pick == R.string.sub_progress_1 || pick == R.string.sub_progress_2
                || pick == R.string.sub_progress_3) {
            return getString(pick, remaining);
        }
        return getString(pick);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        View anchor = menu.findItem(R.id.action_more).getActionView();
        if (anchor != null) {
            anchor.setContentDescription(getString(R.string.action_more));
            anchor.setOnClickListener(this::showMorePopup);
        }
        return true;
    }

    /** Rounded, anchored menu with a small pop-in — replaces the OS overflow,
     *  which OneUI renders as a flat square we can't restyle. */
    private void showMorePopup(View anchor) {
        View content = getLayoutInflater().inflate(R.layout.menu_more_popup, null);
        PopupWindow popup = new PopupWindow(content,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(getResources().getDisplayMetrics().density * 10);
        popup.setAnimationStyle(R.style.Animation_BiteCheck_Popup);
        content.findViewById(R.id.menu_settings).setOnClickListener(v -> {
            popup.dismiss();
            startActivity(new Intent(this, SettingsActivity.class));
        });
        content.findViewById(R.id.menu_logout).setOnClickListener(v -> {
            popup.dismiss();
            logout();
        });
        float density = getResources().getDisplayMetrics().density;
        // Nudge in from the screen edge and drop just below the toolbar icon.
        popup.showAsDropDown(anchor, (int) (-8 * density), (int) (4 * density),
                Gravity.END);
    }
}
