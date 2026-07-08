package com.example.bitecheck.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bitecheck.R;
import com.example.bitecheck.data.local.ProfileDao;
import com.example.bitecheck.data.remote.SupabaseDb;
import com.example.bitecheck.model.UserProfile;
import com.example.bitecheck.util.CalorieCalculator;
import com.example.bitecheck.util.NetworkUtil;
import com.example.bitecheck.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;

import java.util.Calendar;
import java.util.Locale;

/**
 * Yuka-style setup wizard: one question per screen with a progress bar,
 * a circular back button and a single Continue pill. All the data the old
 * single-form screen collected, revealed step by step.
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final int TOTAL_STEPS = 6;
    private static final int STEP_ACTIVITY = 4;
    private static final int STEP_RESULT = 5;

    public static final String EXTRA_EDIT = "edit_mode";

    private int step = 0;
    private boolean editMode;

    // Collected answers
    private String goal;
    private String gender;
    private String dob;
    private int activityLevel = -1;
    private int target;

    private View[] stepViews;
    private LinearProgressIndicator progress;
    private MaterialButton continueButton;

    private LinearLayout[] goalOpts;
    private LinearLayout[] genderOpts;
    private LinearLayout[] activityOpts;
    private final String[] goalValues = {"lose", "maintain", "gain"};
    private final String[] genderValues = {"Male", "Female"};

    private TextInputLayout dobLayout;
    private TextInputLayout heightLayout;
    private TextInputLayout weightLayout;
    private TextInputEditText dobInput;
    private TextInputEditText heightInput;
    private TextInputEditText weightInput;
    private android.widget.TextView targetValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        progress = findViewById(R.id.progress_steps);
        progress.setMax(TOTAL_STEPS);
        continueButton = findViewById(R.id.btn_continue);
        targetValue = findViewById(R.id.text_target_value);

        stepViews = new View[]{
                findViewById(R.id.step_goal),
                findViewById(R.id.step_gender),
                findViewById(R.id.step_dob),
                findViewById(R.id.step_body),
                findViewById(R.id.step_activity),
                findViewById(R.id.step_result)};

        goalOpts = new LinearLayout[]{
                findViewById(R.id.opt_goal_lose),
                findViewById(R.id.opt_goal_maintain),
                findViewById(R.id.opt_goal_gain)};
        genderOpts = new LinearLayout[]{
                findViewById(R.id.opt_gender_male),
                findViewById(R.id.opt_gender_female)};
        activityOpts = new LinearLayout[]{
                findViewById(R.id.opt_act_0),
                findViewById(R.id.opt_act_1),
                findViewById(R.id.opt_act_2),
                findViewById(R.id.opt_act_3),
                findViewById(R.id.opt_act_4)};

        for (int i = 0; i < goalOpts.length; i++) {
            final int index = i;
            goalOpts[i].setOnClickListener(v -> {
                goal = goalValues[index];
                highlight(goalOpts, index);
            });
        }
        for (int i = 0; i < genderOpts.length; i++) {
            final int index = i;
            genderOpts[i].setOnClickListener(v -> {
                gender = genderValues[index];
                highlight(genderOpts, index);
            });
        }
        for (int i = 0; i < activityOpts.length; i++) {
            final int index = i;
            activityOpts[i].setOnClickListener(v -> {
                activityLevel = index;
                highlight(activityOpts, index);
            });
        }

        dobLayout = findViewById(R.id.layout_dob);
        heightLayout = findViewById(R.id.layout_height);
        weightLayout = findViewById(R.id.layout_weight);
        dobInput = findViewById(R.id.input_dob);
        heightInput = findViewById(R.id.input_height);
        weightInput = findViewById(R.id.input_weight);
        dobInput.setOnClickListener(v -> showDatePicker());
        dobLayout.setEndIconOnClickListener(v -> showDatePicker());

        findViewById(R.id.btn_back).setOnClickListener(v -> goBack());
        continueButton.setOnClickListener(v -> goNext());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBack();
            }
        });

        editMode = getIntent().getBooleanExtra(EXTRA_EDIT, false);
        if (editMode) {
            prefillFromProfile();
        }
        updateStep();
    }

    /** Editing existing details: preselect every answer so only changes matter. */
    private void prefillFromProfile() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId() != null ? session.getUserId() : "local";
        UserProfile profile = new ProfileDao(this).get(userId);
        if (profile == null) {
            return;
        }
        for (int i = 0; i < goalValues.length; i++) {
            if (goalValues[i].equals(profile.goal)) {
                goal = profile.goal;
                highlight(goalOpts, i);
            }
        }
        for (int i = 0; i < genderValues.length; i++) {
            if (genderValues[i].equalsIgnoreCase(profile.gender)) {
                gender = genderValues[i];
                highlight(genderOpts, i);
            }
        }
        if (profile.dob != null && !profile.dob.isEmpty()) {
            dob = profile.dob;
            dobInput.setText(dob);
        }
        heightInput.setText(trimNumber(profile.heightCm));
        weightInput.setText(trimNumber(profile.weightKg));
        if (profile.activityLevel >= 0 && profile.activityLevel < activityOpts.length) {
            activityLevel = profile.activityLevel;
            highlight(activityOpts, activityLevel);
        }
    }

    private String trimNumber(double value) {
        if (value <= 0) {
            return "";
        }
        return value == Math.floor(value)
                ? String.valueOf((long) value) : String.valueOf(value);
    }

    private void highlight(LinearLayout[] group, int selectedIndex) {
        for (int i = 0; i < group.length; i++) {
            group[i].setActivated(i == selectedIndex);
        }
    }

    private void goBack() {
        if (step == 0) {
            finish();
        } else {
            step--;
            updateStep();
        }
    }

    private void goNext() {
        if (!validateCurrentStep()) {
            return;
        }
        if (step == STEP_RESULT) {
            finishSetup();
            return;
        }
        step++;
        updateStep();
    }

    private void updateStep() {
        for (int i = 0; i < stepViews.length; i++) {
            stepViews[i].setVisibility(i == step ? View.VISIBLE : View.GONE);
        }
        progress.setProgressCompat(step + 1, true);
        continueButton.setText(step == STEP_RESULT
                ? (editMode ? R.string.action_save : R.string.onboarding_get_started)
                : R.string.onboarding_continue);
        if (step == STEP_RESULT) {
            target = computeTarget();
            targetValue.setText(String.valueOf(target));
        }
    }

    private boolean validateCurrentStep() {
        switch (step) {
            case 0:
                return require(goal != null, R.string.onboarding_goal_title);
            case 1:
                return require(gender != null, R.string.onboarding_gender_title);
            case 2:
                boolean okDob = dob != null && CalorieCalculator.ageFromDob(dob) >= 5;
                dobLayout.setError(okDob ? null : getString(R.string.error_required));
                return okDob;
            case STEP_ACTIVITY:
                return require(activityLevel >= 0, R.string.onboarding_activity_title);
            case 3:
                double h = parseDouble(heightInput);
                double w = parseDouble(weightInput);
                boolean okH = h >= 80 && h <= 250;
                boolean okW = w >= 20 && w <= 400;
                heightLayout.setError(okH ? null : getString(R.string.error_required));
                weightLayout.setError(okW ? null : getString(R.string.error_required));
                return okH && okW;
            default:
                return true;
        }
    }

    private boolean require(boolean condition, int promptRes) {
        if (!condition) {
            Toast.makeText(this, promptRes, Toast.LENGTH_SHORT).show();
        }
        return condition;
    }

    private int computeTarget() {
        int age = CalorieCalculator.ageFromDob(dob);
        double height = parseDouble(heightInput);
        double weight = parseDouble(weightInput);
        return CalorieCalculator.dailyTarget(gender, age, height, weight, activityLevel, goal);
    }

    private void finishSetup() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId() != null ? session.getUserId() : "local";
        UserProfile existing = new ProfileDao(this).get(userId);
        UserProfile profile = new UserProfile();
        profile.userId = userId;
        // Keep any real name we already have — re-onboarding shouldn't wipe it.
        profile.name = session.getName();
        if ((profile.name == null || profile.name.isEmpty()) && existing != null) {
            profile.name = existing.name;
        }
        profile.email = session.getEmail();
        profile.gender = gender;
        profile.dob = dob;
        profile.heightCm = parseDouble(heightInput);
        profile.weightKg = parseDouble(weightInput);
        profile.goal = goal;
        profile.activityLevel = activityLevel;
        profile.calorieTarget = target;
        profile.synced = false;

        new ProfileDao(this).upsert(profile);
        session.setOnboarded(true);
        pushToCloud(profile, session);

        if (editMode) {
            // Return to Account, which refreshes with the new values on resume.
            finish();
        } else {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    /** Best-effort cloud sync; the profile stays local (synced=0) if it fails. */
    private void pushToCloud(UserProfile profile, SessionManager session) {
        if (!NetworkUtil.isOnline(this) || session.getAccessToken() == null
                || session.getUserId() == null) {
            return;
        }
        JsonObject row = new JsonObject();
        row.addProperty("user_id", profile.userId);
        row.addProperty("name", profile.name);
        row.addProperty("gender", profile.gender);
        row.addProperty("dob", profile.dob);
        row.addProperty("height_cm", profile.heightCm);
        row.addProperty("weight_kg", profile.weightKg);
        row.addProperty("goal", profile.goal);
        row.addProperty("activity_level", profile.activityLevel);
        row.addProperty("calorie_target", profile.calorieTarget);
        String userId = profile.userId;
        SupabaseDb.upsert("profiles", "user_id", row, session.getAccessToken(),
                (success, error) -> {
                    if (success) {
                        new ProfileDao(OnboardingActivity.this).markSynced(userId);
                    }
                });
    }

    private double parseDouble(TextInputEditText input) {
        try {
            return Double.parseDouble(input.getText() == null
                    ? "" : input.getText().toString().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void showDatePicker() {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            dob = String.format(Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year);
            dobInput.setText(dob);
            dobLayout.setError(null);
        }, now.get(Calendar.YEAR) - 20, now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)).show();
    }
}
