package com.example.bitecheck.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bitecheck.R;
import com.google.android.material.button.MaterialButton;

/**
 * Yuka-style intro carousel: full-bleed colored slides with a mascot circle,
 * script title and short pitch, dots + a single pill button at the bottom.
 */
public class IntroActivity extends AppCompatActivity {

    public static final String PREFS_UI = "bitecheck_ui";
    public static final String KEY_INTRO_SEEN = "intro_seen";

    private static final int[] BG_COLORS = {
            R.color.bc_primary, R.color.bc_secondary, R.color.bc_primary};
    private static final int[] ICONS = {
            R.drawable.logo_bitecheck, R.drawable.ic_chat, R.drawable.ic_advisor};
    // 0 = full-color image, leave untinted; otherwise tint the line icon.
    private static final int[] ICON_TINTS = {
            0, R.color.bc_primary, R.color.bc_secondary};
    private static final int[] TITLES = {
            R.string.intro_1_title, R.string.intro_2_title, R.string.intro_3_title};
    private static final int[] BODIES = {
            R.string.intro_1_body, R.string.intro_2_body, R.string.intro_3_body};

    private View introRoot;
    private ViewPager2 pager;
    private MaterialButton nextButton;
    private LinearLayout dotsRow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_intro);
        new WindowInsetsControllerCompat(getWindow(), findViewById(R.id.intro_root))
                .setAppearanceLightStatusBars(false);

        introRoot = findViewById(R.id.intro_root);
        pager = findViewById(R.id.intro_pager);
        nextButton = findViewById(R.id.btn_intro_next);
        dotsRow = findViewById(R.id.intro_dots);

        pager.setAdapter(new IntroAdapter());
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateChrome(position);
            }
        });

        nextButton.setOnClickListener(v -> {
            if (pager.getCurrentItem() == BG_COLORS.length - 1) {
                finishIntro();
            } else {
                pager.setCurrentItem(pager.getCurrentItem() + 1, true);
            }
        });

        updateChrome(0);
    }

    private void finishIntro() {
        getSharedPreferences(PREFS_UI, MODE_PRIVATE)
                .edit().putBoolean(KEY_INTRO_SEEN, true).apply();
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }

    private void updateChrome(int position) {
        introRoot.setBackgroundColor(ContextCompat.getColor(this, BG_COLORS[position]));

        for (int i = 0; i < dotsRow.getChildCount(); i++) {
            dotsRow.getChildAt(i).setAlpha(i == position ? 1f : 0.4f);
        }

        boolean last = position == BG_COLORS.length - 1;
        nextButton.setText(last ? R.string.intro_lets_go : R.string.intro_continue);
        int buttonBg = last
                ? ContextCompat.getColor(this, R.color.bc_cta_dark)
                : 0x33FFFFFF;
        nextButton.setBackgroundTintList(ColorStateList.valueOf(buttonBg));
        nextButton.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private class IntroAdapter extends RecyclerView.Adapter<IntroAdapter.PageHolder> {

        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View page = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_intro_page, parent, false);
            return new PageHolder(page);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            holder.root.setBackgroundColor(
                    ContextCompat.getColor(IntroActivity.this, BG_COLORS[position]));
            holder.icon.setImageResource(ICONS[position]);
            if (ICON_TINTS[position] == 0) {
                holder.icon.clearColorFilter();
            } else {
                holder.icon.setColorFilter(
                        ContextCompat.getColor(IntroActivity.this, ICON_TINTS[position]));
            }
            // Set the script typeface in code: theme-level fontFamily can win
            // over XML on some devices, and this bypasses inflation quirks.
            holder.title.setTypeface(
                    ResourcesCompat.getFont(IntroActivity.this, R.font.script_bold));
            holder.title.setText(TITLES[position]);
            holder.body.setText(BODIES[position]);
        }

        @Override
        public int getItemCount() {
            return BG_COLORS.length;
        }

        class PageHolder extends RecyclerView.ViewHolder {
            final View root;
            final ImageView icon;
            final TextView title;
            final TextView body;

            PageHolder(View view) {
                super(view);
                root = view.findViewById(R.id.page_root);
                icon = view.findViewById(R.id.intro_icon);
                title = view.findViewById(R.id.intro_title);
                body = view.findViewById(R.id.intro_body);
            }
        }
    }
}
