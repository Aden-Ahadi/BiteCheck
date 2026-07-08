package com.example.bitecheck.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.example.bitecheck.R;

public class ContactActivity extends BaseSecondaryActivity {

    private static final String SUPPORT_PHONE = "+15551234567";
    private static final String SUPPORT_EMAIL = "support@bitecheck.app";

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_contact;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_contact);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        findViewById(R.id.btn_call).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + SUPPORT_PHONE));
            startActivity(intent);
        });

        findViewById(R.id.btn_email).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            startActivity(intent);
        });
    }
}
