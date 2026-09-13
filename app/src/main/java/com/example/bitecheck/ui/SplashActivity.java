package com.example.bitecheck.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bitecheck.R;
import com.example.bitecheck.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1800;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable nextAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Handle Deep Links (Password Reset)
        if (getIntent() != null && getIntent().getData() != null) {
            Uri data = getIntent().getData();
            String fragment = data.getFragment();
            String query = data.getQuery();
            
            String token = null;
            if (fragment != null && fragment.contains("access_token=")) {
                token = fragment.split("access_token=")[1].split("&")[0];
            } else if (query != null && query.contains("access_token=")) {
                token = data.getQueryParameter("access_token");
            }

            if (token != null) {
                Intent intent = new Intent(this, UpdatePasswordActivity.class);
                intent.putExtra("access_token", token);
                startActivity(intent);
                finish();
                return; // STOP HERE - don't start the timer
            }
        }

        // Standard launch flow
        nextAction = () -> {
            SessionManager session = new SessionManager(this);
            Class<?> next;
            if (session.isLoggedIn() && session.isOnboarded()) {
                next = DashboardActivity.class;
            } else if (session.isLoggedIn()) {
                next = OnboardingActivity.class;
            } else {
                boolean introSeen = getSharedPreferences(IntroActivity.PREFS_UI, MODE_PRIVATE)
                        .getBoolean(IntroActivity.KEY_INTRO_SEEN, false);
                next = introSeen ? WelcomeActivity.class : IntroActivity.class;
            }
            startActivity(new Intent(this, next));
            finish();
        };
        handler.postDelayed(nextAction, SPLASH_DELAY_MS);
    }

    @Override
    protected void onDestroy() {
        if (nextAction != null) {
            handler.removeCallbacks(nextAction);
        }
        super.onDestroy();
    }
}
