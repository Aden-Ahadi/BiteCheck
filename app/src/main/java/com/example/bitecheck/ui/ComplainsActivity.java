package com.example.bitecheck.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.bitecheck.R;

public class ComplainsActivity extends BaseSecondaryActivity {

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_form;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_complains);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((TextView) findViewById(R.id.text_form_intro)).setText(R.string.complains_placeholder);
        UserFeedbackActivity.FormSubmitter.wire(this, "complaints");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == UserFeedbackActivity.FormSubmitter.REQ_SMS && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.msg_sms_permission_granted, Toast.LENGTH_SHORT).show();
        }
    }
}
