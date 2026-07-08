package com.example.bitecheck.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bitecheck.R;
import com.example.bitecheck.data.remote.SupabaseAuth;
import com.example.bitecheck.util.NetworkUtil;
import com.example.bitecheck.util.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignUpActivity extends AppCompatActivity {

    private final SupabaseAuth auth = new SupabaseAuth();
    private FloatingActionButton signUpButton;
    private TextView statusText;

    private TextInputLayout nameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmLayout;
    private TextInputEditText nameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        nameLayout = findViewById(R.id.layout_name);
        emailLayout = findViewById(R.id.layout_email);
        passwordLayout = findViewById(R.id.layout_password);
        confirmLayout = findViewById(R.id.layout_confirm);
        nameInput = findViewById(R.id.input_name);
        emailInput = findViewById(R.id.input_email);
        passwordInput = findViewById(R.id.input_password);
        confirmInput = findViewById(R.id.input_confirm);

        signUpButton = findViewById(R.id.btn_sign_up);
        statusText = findViewById(R.id.text_status);
        signUpButton.setOnClickListener(v -> attemptSignUp());

        ((TextView) findViewById(R.id.text_nav_title)).setText(R.string.signup_title);
        findViewById(R.id.btn_next).setVisibility(View.GONE);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void attemptSignUp() {
        String name = text(nameInput);
        String email = text(emailInput);
        String password = text(passwordInput);
        String confirm = text(confirmInput);

        boolean valid = true;
        if (name.isEmpty()) {
            nameLayout.setError(getString(R.string.error_required));
            valid = false;
        } else {
            nameLayout.setError(null);
        }
        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.error_required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            valid = false;
        } else {
            emailLayout.setError(null);
        }
        if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.error_short_password));
            valid = false;
        } else {
            passwordLayout.setError(null);
        }
        if (!password.equals(confirm)) {
            confirmLayout.setError(getString(R.string.error_password_mismatch));
            valid = false;
        } else {
            confirmLayout.setError(null);
        }
        if (!valid) {
            return;
        }

        if (!NetworkUtil.isOnline(this)) {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        auth.signUp(email, password, name, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(SupabaseAuth.Session session) {
                if (session == null) {
                    // Email confirmation is enabled on the Supabase project:
                    // account exists but the user must confirm before logging in.
                    Toast.makeText(SignUpActivity.this,
                            R.string.msg_check_email, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                SessionManager sessionManager = new SessionManager(SignUpActivity.this);
                sessionManager.saveSession(session.userId, session.email,
                        session.accessToken, session.refreshToken);
                sessionManager.saveName(name);
                Intent intent = new Intent(SignUpActivity.this, OnboardingActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(SignUpActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        signUpButton.setEnabled(!loading);
        statusText.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }
}
