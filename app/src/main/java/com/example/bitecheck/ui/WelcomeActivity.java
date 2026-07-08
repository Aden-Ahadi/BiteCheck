package com.example.bitecheck.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bitecheck.R;

/**
 * Logged-out hub after the intro carousel: mascot + "Sign in with email"
 * and a create-account link, matching the Yuka welcome screen.
 */
public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        findViewById(R.id.btn_signin_email).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
        findViewById(R.id.link_create_account).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }
}
