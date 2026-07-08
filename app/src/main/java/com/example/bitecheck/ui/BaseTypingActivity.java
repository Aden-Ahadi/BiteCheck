package com.example.bitecheck.ui;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bitecheck.R;

/**
 * Shell for the text-entry screens (Chat, Advisor). No bottom nav or title —
 * just a circular back button top-left so the keyboard has the whole screen.
 */
public abstract class BaseTypingActivity extends AppCompatActivity {

    @LayoutRes
    protected abstract int getContentLayoutId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        FrameLayout contentFrame = findViewById(R.id.content_frame);
        getLayoutInflater().inflate(getContentLayoutId(), contentFrame, true);

        findViewById(R.id.btn_back_circle).setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());
    }
}
