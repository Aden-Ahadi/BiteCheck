package com.example.bitecheck.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.bitecheck.util.DateUtil;

import java.util.ArrayList;
import java.util.List;

public class WaterDao {

    /** id + amount for unsynced rows. */
    public static class Row {
        public long id;
        public int amountMl;
        public String loggedAt;
    }

    private final BiteCheckDbHelper helper;

    public WaterDao(Context context) {
        this.helper = new BiteCheckDbHelper(context.getApplicationContext());
    }

    public void insert(String userId, int amountMl) {
        ContentValues values = new ContentValues();
        values.put("user_id", userId);
        values.put("amount_ml", amountMl);
        values.put("logged_at", DateUtil.now());
        values.put("synced", 0);
        helper.getWritableDatabase().insert("water_logs", null, values);
    }

    public int totalForDay(String userId, String day) {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount_ml), 0) FROM water_logs " +
                        "WHERE user_id = ? AND logged_at LIKE ?",
                new String[]{userId, day + "%"})) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    public List<Row> unsynced(String userId) {
        List<Row> rows = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("water_logs",
                new String[]{"id", "amount_ml", "logged_at"},
                "user_id = ? AND synced = 0", new String[]{userId},
                null, null, "logged_at ASC")) {
            while (cursor.moveToNext()) {
                Row row = new Row();
                row.id = cursor.getLong(0);
                row.amountMl = cursor.getInt(1);
                row.loggedAt = cursor.getString(2);
                rows.add(row);
            }
        }
        return rows;
    }

    public void markSynced(long id) {
        ContentValues values = new ContentValues();
        values.put("synced", 1);
        helper.getWritableDatabase()
                .update("water_logs", values, "id = ?", new String[]{String.valueOf(id)});
    }
}
