package com.example.bitecheck.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bitecheck.R;
import com.example.bitecheck.data.remote.SupabaseAuth;
import com.example.bitecheck.util.NetworkUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ResetPasswordActivity extends AppCompatActivity {

    private final SupabaseAuth auth = new SupabaseAuth();
    private MaterialButton sendButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        TextInputLayout emailLayout = findViewById(R.id.layout_email);
        TextInputEditText emailInput = findViewById(R.id.input_email);
        sendButton = findViewById(R.id.btn_next);
        statusText = findViewById(R.id.text_status);

        ((TextView) findViewById(R.id.text_nav_title)).setText(R.string.reset_title);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        sendButton.setOnClickListener(v -> {
            String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
            if (email.isEmpty()) {
                emailLayout.setError(getString(R.string.error_required));
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.setError(getString(R.string.error_invalid_email));
                return;
            }
            emailLayout.setError(null);

            if (!NetworkUtil.isOnline(this)) {
                Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_LONG).show();
                return;
            }

            setLoading(true);
            auth.recoverPassword(email, new SupabaseAuth.AuthCallback() {
                @Override
                public void onSuccess(SupabaseAuth.Session session) {
                    Toast.makeText(ResetPasswordActivity.this,
                            R.string.reset_link_sent, Toast.LENGTH_LONG).show();
                    finish();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    Toast.makeText(ResetPasswordActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void setLoading(boolean loading) {
        sendButton.setEnabled(!loading);
        statusText.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
