package com.example.bitecheck.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.bitecheck.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

public class WeightDao {

    public static class Row {
        public long id;
        public double weightKg;
        public String loggedAt;
    }

    private final BiteCheckDbHelper helper;

    public WeightDao(Context context) {
        this.helper = new BiteCheckDbHelper(context.getApplicationContext());
    }

    public void insert(String userId, double weightKg) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("weight_kg", weightKg);
        values.put("logged_at", DateUtil.now());
        values.put("synced", 0);
        helper.getWritableDatabase().insert("weight_logs", null, values);
    }

    /** Latest logged weight, or -1 when none exists. */
    public double latest(String userId) {
        try (Cursor cursor = helper.getReadableDatabase().query("weight_logs",
                new String[]{"weight_kg"}, "user_id = ?", new String[]{userId},
                null, null, "logged_at DESC", "1")) {
            return cursor.moveToFirst() ? cursor.getDouble(0) : -1;
        }
    }

    public List<Row> list(String userId) {
        List<Row> rows = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("weight_logs",
                new String[]{"id", "weight_kg", "logged_at"},
                "user_id = ?", new String[]{userId},
                null, null, "logged_at DESC")) {
            while (cursor.moveToNext()) {
                rows.add(rowFrom(cursor));
            }
        }
        return rows;
    }

    public List<Row> unsynced(String userId) {
        List<Row> rows = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("weight_logs",
                new String[]{"id", "weight_kg", "logged_at"},
                "user_id = ? AND synced = 0", new String[]{userId},
                null, null, "logged_at ASC")) {
            while (cursor.moveToNext()) {
                rows.add(rowFrom(cursor));
            }
        }
        return rows;
    }

    public void markSynced(long id) {
        ContentValues values = new ContentValues();
        values.put("synced", 1);
        helper.getWritableDatabase()
                .update("weight_logs", values, "id = ?", new String[]{String.valueOf(id)});
    }

    private Row rowFrom(Cursor cursor) {
        Row row = new Row();
        row.id = cursor.getLong(0);
        row.weightKg = cursor.getDouble(1);
        row.loggedAt = cursor.getString(2);
        return row;
    }
}
