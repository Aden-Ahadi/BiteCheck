package com.example.bitecheck.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.bitecheck.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

/**
 * Shared shell for the four top-level screens: toolbar + navigation drawer +
 * bottom navigation. Child activities supply their content layout, which is
 * inflated into the content frame.
 */
public abstract class BaseNavActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;

    @LayoutRes
    protected abstract int getContentLayoutId();

    protected abstract int getBottomNavItemId();

    protected abstract CharSequence getScreenTitle();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_nav);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Titles intentionally omitted — each screen's content carries its own
        // heading, so the toolbar stays minimal (just the drawer button).
        setTitle("");

        FrameLayout contentFrame = findViewById(R.id.content_frame);
        getLayoutInflater().inflate(getContentLayoutId(), contentFrame, true);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_drawer_open, R.string.nav_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navView = findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(this::onDrawerItemSelected);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(this::onBottomNavItemSelected);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Highlight the current tab without re-triggering navigation.
        bottomNav.setOnItemSelectedListener(null);
        bottomNav.setSelectedItemId(getBottomNavItemId());
        bottomNav.setOnItemSelectedListener(this::onBottomNavItemSelected);
    }

    private boolean onBottomNavItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == getBottomNavItemId()) {
            return true;
        }
        Class<?> target = null;
        if (id == R.id.nav_dashboard) {
            target = DashboardActivity.class;
        } else if (id == R.id.nav_chat) {
            target = MainActivity.class;
        } else if (id == R.id.nav_advisor) {
            target = FoodAdvisorActivity.class;
        } else if (id == R.id.nav_history) {
            target = MealHistoryActivity.class;
        }
        if (target != null) {
            Intent intent = new Intent(this, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            // Dashboard stays as the task root; other tabs finish so back
            // always returns to Dashboard instead of stacking tabs.
            if (!(this instanceof DashboardActivity)) {
                finish();
            }
        }
        return true;
    }

    private boolean onDrawerItemSelected(MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START);
        int id = item.getItemId();
        Class<?> target = null;
        if (id == R.id.nav_water) {
            target = WaterTrackerActivity.class;
        } else if (id == R.id.nav_weight) {
            target = WeightTrackerActivity.class;
        } else if (id == R.id.nav_report) {
            target = ReportActivity.class;
        } else if (id == R.id.nav_settings) {
            target = SettingsActivity.class;
        } else if (id == R.id.nav_account) {
            target = AccountActivity.class;
        } else if (id == R.id.nav_feedback) {
            target = UserFeedbackActivity.class;
        } else if (id == R.id.nav_complains) {
            target = ComplainsActivity.class;
        } else if (id == R.id.nav_help) {
            target = HelpActivity.class;
        } else if (id == R.id.nav_contact) {
            target = ContactActivity.class;
        } else if (id == R.id.nav_privacy) {
            target = PrivacyPolicyActivity.class;
        } else if (id == R.id.nav_logout) {
            logout();
            return true;
        }
        if (target != null) {
            startActivity(new Intent(this, target));
        }
        return true;
    }

    protected void logout() {
        new com.example.bitecheck.util.SessionManager(this).clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
