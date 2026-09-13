package com.example.bitecheck.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.bitecheck.util.DateUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WaterDao {

    /** id + amount for unsynced rows. */
    public static class Row {
        public long id;
        public String uuid;
        public int amountMl;
        public String loggedAt;
        public boolean synced;
    }

    private final BiteCheckDbHelper helper;

    public WaterDao(Context context) {
        this.helper = new BiteCheckDbHelper(context.getApplicationContext());
    }

    public void insert(String userId, int amountMl) {
        insert(userId, null, amountMl, DateUtil.now(), false);
    }

    public void insert(String userId, String uuid, int amountMl, String loggedAt, boolean synced) {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        ContentValues values = new ContentValues();
        values.put("uuid", uuid);
        values.put("user_id", userId);
        values.put("amount_ml", amountMl);
        values.put("logged_at", loggedAt);
        values.put("synced", synced ? 1 : 0);
        helper.getWritableDatabase().insertWithOnConflict("water_logs", null, values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);
    }

    /** Removes the most recent entry for the day (an "undo" for a mistaken tap). */
    public boolean deleteLast(String userId, String day) {
        return helper.getWritableDatabase().delete("water_logs",
                "id = (SELECT id FROM water_logs WHERE user_id = ? AND logged_at LIKE ? " +
                        "ORDER BY id DESC LIMIT 1)",
                new String[]{userId, day + "%"}) > 0;
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
                new String[]{"id", "uuid", "amount_ml", "logged_at"},
                "user_id = ? AND synced = 0", new String[]{userId},
                null, null, "logged_at ASC")) {
            while (cursor.moveToNext()) {
                Row row = new Row();
                row.id = cursor.getLong(0);
                row.uuid = cursor.getString(1);
                row.amountMl = cursor.getInt(2);
                row.loggedAt = cursor.getString(3);
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
