package com.example.bitecheck.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bitecheck.R;
import com.example.bitecheck.util.SessionManager;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
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
        }, SPLASH_DELAY_MS);
    }
}
