package com.example.bitecheck.ui;

import android.os.Bundle;
import android.widget.TextView;

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
}
