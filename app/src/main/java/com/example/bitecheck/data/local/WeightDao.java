package com.example.bitecheck.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.bitecheck.util.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WeightDao {

    public static class Row {
        public long id;
        public String uuid;
        public double weightKg;
        public String loggedAt;
        public boolean synced;
    }

    private final BiteCheckDbHelper helper;

    public WeightDao(Context context) {
        this.helper = new BiteCheckDbHelper(context.getApplicationContext());
    }

    public void insert(String userId, double weightKg) {
        insert(userId, null, weightKg, DateUtil.now(), false);
    }

    public void insert(String userId, String uuid, double weightKg, String loggedAt, boolean synced) {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        ContentValues values = new ContentValues();
        values.put("uuid", uuid);
        values.put("user_id", userId);
        values.put("weight_kg", weightKg);
        values.put("logged_at", loggedAt);
        values.put("synced", synced ? 1 : 0);
        helper.getWritableDatabase().insertWithOnConflict("weight_logs", null, values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
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
                null, "user_id = ?", new String[]{userId},
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
                null, "user_id = ? AND synced = 0", new String[]{userId},
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
        row.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        row.uuid = cursor.getString(cursor.getColumnIndexOrThrow("uuid"));
        row.weightKg = cursor.getDouble(cursor.getColumnIndexOrThrow("weight_kg"));
        row.loggedAt = cursor.getString(cursor.getColumnIndexOrThrow("logged_at"));
        row.synced = cursor.getInt(cursor.getColumnIndexOrThrow("synced")) == 1;
        return row;
    }
}
