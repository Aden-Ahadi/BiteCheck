package com.example.bitecheck.ui;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bitecheck.R;
import com.example.bitecheck.data.local.BiteCheckDbHelper;
import com.example.bitecheck.data.local.ProfileDao;
import com.example.bitecheck.data.remote.SupabaseAuth;
import com.example.bitecheck.data.remote.SupabaseDb;
import com.example.bitecheck.model.UserProfile;
import com.example.bitecheck.util.NetworkUtil;
import com.example.bitecheck.util.SessionManager;
import com.google.gson.JsonObject;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private TextView statusText;

    private final SupabaseAuth auth = new SupabaseAuth();
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        emailLayout = findViewById(R.id.layout_email);
        passwordLayout = findViewById(R.id.layout_password);
        emailInput = findViewById(R.id.input_email);
        passwordInput = findViewById(R.id.input_password);
        loginButton = findViewById(R.id.btn_next);
        statusText = findViewById(R.id.text_status);

        ((TextView) findViewById(R.id.text_nav_title)).setText(R.string.action_login);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        loginButton.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.link_forgot_password).setOnClickListener(v ->
                startActivity(new Intent(this, ResetPasswordActivity.class)));
        findViewById(R.id.link_sign_up).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    private void attemptLogin() {
        String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString();

        boolean valid = true;
        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.error_required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            valid = false;
        } else {
            emailLayout.setError(null);
        }
        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.error_required));
            valid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_short_password));
            valid = false;
        } else {
            passwordLayout.setError(null);
        }
        if (!valid) {
            return;
        }

        setLoading(true);

        // Assignment requirement: check internet + database connections before login.
        if (!NetworkUtil.isOnline(this)) {
            fail(getString(R.string.error_no_internet));
            return;
        }
        if (!localDatabaseOpens()) {
            fail(getString(R.string.error_local_db));
            return;
        }
        auth.checkHealth(reachable -> {
            if (!reachable) {
                fail(getString(R.string.error_server_unreachable));
                return;
            }
            signIn(email, password);
        });
    }

    private boolean localDatabaseOpens() {
        try (BiteCheckDbHelper helper = new BiteCheckDbHelper(this);
             SQLiteDatabase db = helper.getReadableDatabase()) {
            return db.isOpen();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void signIn(String email, String password) {
        auth.signIn(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(SupabaseAuth.Session session) {
                if (session == null) {
                    fail(getString(R.string.error_server_unreachable));
                    return;
                }
                sessionManager.saveSession(session.userId, session.email,
                        session.accessToken, session.refreshToken);
                restoreProfileAndContinue(session.userId, session.accessToken);
            }

            @Override
            public void onError(String message) {
                fail(message);
            }
        });
    }

    /**
     * A returning user shouldn't repeat profile setup: the onboarded flag only
     * lives in SharedPreferences and dies on logout. Restore the profile from
     * SQLite first, then from the Supabase profiles table, and only route to
     * OnboardingActivity when neither has it (a genuinely new account).
     */
    private void restoreProfileAndContinue(String userId, String accessToken) {
        ProfileDao dao = new ProfileDao(this);
        UserProfile local = dao.get(userId);
        if (local != null) {
            continueToApp(true, local.name);
            return;
        }
        SupabaseDb.selectOne("profiles", "user_id", userId, accessToken, row -> {
            if (row == null) {
                continueToApp(false, null);
                return;
            }
            UserProfile profile = new UserProfile();
            profile.userId = userId;
            profile.name = str(row, "name");
            profile.email = sessionManager.getEmail();
            profile.gender = str(row, "gender");
            profile.dob = str(row, "dob");
            profile.heightCm = num(row, "height_cm");
            profile.weightKg = num(row, "weight_kg");
            profile.goal = str(row, "goal");
            profile.activityLevel = (int) num(row, "activity_level");
            profile.calorieTarget = (int) num(row, "calorie_target");
            profile.synced = true;
            dao.upsert(profile);
            continueToApp(true, profile.name);
        });
    }

    private void continueToApp(boolean onboarded, String name) {
        sessionManager.setOnboarded(onboarded);
        if (name != null && !name.isEmpty()) {
            sessionManager.saveName(name);
        }
        Class<?> next = onboarded ? DashboardActivity.class : OnboardingActivity.class;
        Intent intent = new Intent(this, next);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private static String str(JsonObject row, String key) {
        return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsString() : null;
    }

    private static double num(JsonObject row, String key) {
        return row.has(key) && !row.get(key).isJsonNull() ? row.get(key).getAsDouble() : 0;
    }

    private void fail(String message) {
        setLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean loading) {
        loginButton.setEnabled(!loading);
        statusText.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
