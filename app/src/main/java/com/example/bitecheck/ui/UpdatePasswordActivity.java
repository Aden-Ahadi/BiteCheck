package com.example.bitecheck.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.bitecheck.R;
import com.example.bitecheck.data.remote.SupabaseAuth;
import com.example.bitecheck.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class UpdatePasswordActivity extends BaseTypingActivity {

    private String accessToken;
    private final SupabaseAuth auth = new SupabaseAuth();

    @Override
    protected int getContentLayoutId() {
        return R.layout.activity_update_password;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        accessToken = getIntent().getStringExtra("access_token");
        if (accessToken == null) {
            Toast.makeText(this, "Session expired. Try again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        TextInputLayout passLayout = findViewById(R.id.layout_password);
        TextInputEditText passInput = findViewById(R.id.input_password);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> {
            String pass = passInput.getText() == null ? "" : passInput.getText().toString().trim();
            if (pass.length() < 6) {
                passLayout.setError(getString(R.string.error_short_password));
                return;
            }
            passLayout.setError(null);

            btnSave.setEnabled(false);
            auth.updatePassword(accessToken, pass, (success, error) -> {
                btnSave.setEnabled(true);
                if (success) {
                    Toast.makeText(this, R.string.msg_password_updated, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, WelcomeActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                } else {
                    Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
