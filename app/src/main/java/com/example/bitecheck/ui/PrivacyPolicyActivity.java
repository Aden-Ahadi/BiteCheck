package com.example.bitecheck.ui;

import android.os.Bundle;
import android.widget.TextView;

import com.example.bitecheck.R;

public class PrivacyPolicyActivity extends BaseSecondaryActivity {

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_text;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_privacy);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((TextView) findViewById(R.id.text_content)).setText(R.string.privacy_content);
    }
}
