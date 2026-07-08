package com.example.bitecheck.ui;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.bitecheck.R;

/**
 * Shared shell for secondary screens reached from the drawer: toolbar with
 * Up navigation + a content frame the child activity fills.
 */
public abstract class BaseSecondaryActivity extends AppCompatActivity {

    @LayoutRes
    protected abstract int getContentLayoutId();

    protected abstract CharSequence getScreenTitle();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        FrameLayout contentFrame = findViewById(R.id.content_frame);
        getLayoutInflater().inflate(getContentLayoutId(), contentFrame, true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
