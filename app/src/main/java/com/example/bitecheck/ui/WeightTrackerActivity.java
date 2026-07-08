package com.example.bitecheck.ui;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bitecheck.R;
import com.example.bitecheck.data.local.WeightDao;
import com.example.bitecheck.util.SessionManager;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WeightTrackerActivity extends BaseSecondaryActivity {

    private WeightDao weightDao;
    private String userId;

    @Override
    protected int getContentLayoutId() {
        return R.layout.content_weight;
    }

    @Override
    protected CharSequence getScreenTitle() {
        return getString(R.string.nav_weight);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        weightDao = new WeightDao(this);
        SessionManager session = new SessionManager(this);
        userId = session.getUserId() != null ? session.getUserId() : "local";

        TextInputEditText input = findViewById(R.id.input_new_weight);
        findViewById(R.id.btn_log_weight).setOnClickListener(v -> {
            String text = input.getText() == null ? "" : input.getText().toString().trim();
            double weight;
            try {
                weight = Double.parseDouble(text);
            } catch (NumberFormatException e) {
                weight = -1;
            }
            if (weight < 20 || weight > 400) {
                Toast.makeText(this, R.string.error_required, Toast.LENGTH_SHORT).show();
                return;
            }
            weightDao.insert(userId, weight);
            input.setText("");
            refresh();
        });
        refresh();
    }

    private void refresh() {
        List<WeightDao.Row> rows = weightDao.list(userId);
        TextView latest = findViewById(R.id.text_weight_latest);
        if (rows.isEmpty()) {
            latest.setText(R.string.weight_placeholder);
        } else {
            latest.setText(getString(R.string.weight_latest_format, rows.get(0).weightKg));
        }

        List<String> lines = new ArrayList<>();
        for (WeightDao.Row row : rows) {
            lines.add(String.format(Locale.US, "%.1f kg   —   %s",
                    row.weightKg, row.loggedAt));
        }
        ListView list = findViewById(R.id.list_weight);
        list.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, lines));
    }
}
