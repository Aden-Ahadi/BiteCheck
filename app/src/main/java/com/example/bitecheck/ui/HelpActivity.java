package com.example.bitecheck.ui;

import android.os.Bundle;
import android.widget.TextView;

import com.example.bitecheck.R;

public class HelpActivity extends BaseSecondaryActivity {

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_text;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_help);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((TextView) findViewById(R.id.text_content)).setText(R.string.help_content);
    }
}
